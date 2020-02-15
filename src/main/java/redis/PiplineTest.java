package redis;

import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Author wenxuan.hao
 * @create 2020-02-14 20:54
 */
public class PiplineTest {

    @Test
    public void pipeCompare() {
        Jedis redis = new Jedis("localhost", 6379);
        redis.flushDB();
        Map<String, String> data = new HashMap<String, String>();

        // 不使用pipline, hmset
        long start = System.currentTimeMillis();
        // 直接hmset
        for (int i = 0; i < 10000; i++) {
            data.clear();  //清空map
            data.put("k_" + i, "v_" + i);
            redis.hmset("key_" + i, data); //循环执行10000条数据插入redis
        }
        long end = System.currentTimeMillis();
        System.out.println("    共插入:[" + redis.dbSize() + "]条 .. ");
        System.out.println("1,未使用PIPE批量设值耗时" + (end - start));

        redis.flushDB();

        // 使用pipeline hmset
        Pipeline pipe = redis.pipelined();
        start = System.currentTimeMillis();

        for (int i = 0; i < 10000; i++) {
            data.clear();
            data.put("k_" + i, "v_" + i);
            pipe.hmset("key_" + i, data); //将值封装到PIPE对象，此时并未执行，还停留在客户端
        }
        pipe.sync(); //将封装后的PIPE一次性发给redis
        end = System.currentTimeMillis();
        System.out.println("    PIPE共插入:[" + redis.dbSize() + "]条 .. ");
        System.out.println("2,使用PIPE批量设值耗时" + (end - start) );
//--------------------------------------------------------------------------------------------------
        // hmget
        Set<String> keys = redis.keys("key_*"); //将上面设值所有结果键查询出来
        // 直接使用Jedis hgetall
        start = System.currentTimeMillis();
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        for (String key : keys) {
            //此处keys根据以上的设值结果，共有10000个，循环10000次
            result.put(key, redis.hgetAll(key)); //使用redis对象根据键值去取值，将结果放入result对象
        }
        end = System.currentTimeMillis();
        System.out.println("    共取值:[" + redis.dbSize() + "]条 .. ");
        System.out.println("3,未使用PIPE批量取值耗时 " + (end - start));

        // 使用pipeline hgetall
        result.clear();
        start = System.currentTimeMillis();
        for (String key : keys) {
            pipe.hgetAll(key); //使用PIPE封装需要取值的key,此时还停留在客户端，并未真正执行查询请求
        }
        pipe.sync();  //提交到redis进行查询

        end = System.currentTimeMillis();
        System.out.println("    PIPE共取值:[" + redis.dbSize() + "]条 .. ");
        System.out.println("4,使用PIPE批量取值耗时" + (end - start));

        redis.disconnect();
    }

}
