package redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.io.IOException;

/**
 * @Author wenxuan.hao
 * @create 2020-02-14 20:49
 *
 * 简单限流:
 *  目的: 对用一个用户的同一种行为, 在规定时间内进行操作次数的限制
 *  方法: 利用zset结构. 一个用户的一种行为对应一个zset, 每次操作时都会向该zset中存入操作时间(score值, value无所谓, 保证唯一即可. ); 并清除超过指定时间范围的记录.
 *  最后将zset中的记录个数与规定上限次数进行对比, 即可得到是否允许本次操作
 */
public class SimpleRateLimiter {
    private Jedis jedis;

    public SimpleRateLimiter(Jedis jedis) {
        this.jedis = jedis;
    }

    public boolean isActionAllowed(String userId, String actionKey, int period, int maxCount) throws IOException {
        String key = String.format("hist:%s:%s", userId, actionKey);
        long nowTs = System.currentTimeMillis();

        // 因为一下几个操作都是针对同一个key, 所以使用pipline可以提升效率
        Pipeline pipe = jedis.pipelined();
        pipe.multi();

        pipe.zadd(key, nowTs, "" + nowTs);
        pipe.zremrangeByScore(key, 0, nowTs - period * 1000);  // 移除指定区间内的所有成员, 即 移除60s之前的所有成员
        Response<Long> count = pipe.zcard(key);  // 得到key set 集合的大小
        pipe.expire(key, period + 1);

        pipe.exec();
        pipe.close();
        return count.get() <= maxCount; // 当前集合大小若大于等于limit, 该操作不被允许
    }

    public static void main(String[] args) {
        Jedis jedis = new Jedis();
        SimpleRateLimiter limiter = new SimpleRateLimiter(jedis);
        for(int i=0;i<20;i++) {
            try {
                System.out.println(limiter.isActionAllowed("laoqian", "reply", 60, 5));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
