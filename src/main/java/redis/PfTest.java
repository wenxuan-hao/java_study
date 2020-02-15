package redis;

import redis.clients.jedis.Jedis;

/**
 * @Author wenxuan.hao
 * @create 2020-02-14 20:05
 * HyperLogLog: 用法与set相似, 常用于精度不高的统计需求.
 */
public class PfTest {

    public static void main(String[] args) {

        Jedis jedis = new Jedis("127.0.0.1", 6379);
        for (int i = 0; i<10000; i++){
            jedis.pfadd("uv_count", "user"+i);

        }
        long count = jedis.pfcount("uv_count");
        System.out.println(count);

        jedis.flushAll(); //清空存储的批量数据
        jedis.close();
    }



}
