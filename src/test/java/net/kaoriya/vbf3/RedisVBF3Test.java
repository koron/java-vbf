package net.kaoriya.vbf3;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

class RedisVBF3Test {

    static JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");

    static Jedis getJedis() throws Exception {
        return pool.getResource();
    }

    @Test
    void falsePositiveRates() throws Exception {
        try (Jedis jedis = getJedis()) {
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-200-0.1", 1000, 7, 200, 0.1);
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-200-0.5", 1000, 7, 200, 0.5);
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-200-0.9", 1000, 7, 200, 0.9);
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-400-0.1", 1000, 7, 400, 0.1);
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-700-0.1", 1000, 7, 700, 0.1);
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-1000-0.1", 1000, 7, 1000, 0.1);
        }
    }

    static void checkFPRate(Jedis jedis, String key, long m, int k, int num, double hitRate) throws Exception {
        final short maxLife = 10;
        RedisVBF3.drop(jedis, key);
        RedisVBF3 vbf = RedisVBF3.open(jedis, key, m, (short)k, maxLife);
        try {
            checkFPRate(vbf, num, hitRate, maxLife);
        } finally {
            vbf.drop();
        }
    }

    static void checkFPRate(RedisVBF3 vbf, int num, double hitRate, short maxLife) throws Exception {
        int mid = (int)((double)num * hitRate + 0.5);
        for (int i = 0; i < mid; i++) {
            String s = String.valueOf(i);
            vbf.put(int2bytes(i), maxLife);
        }

	// check no false negative entries.
        for (int i = 0; i < mid; i++) {
            boolean has = vbf.check(int2bytes(i));
            assertTrue(has);
        }

        // check false positive rate is less than 1%
        int falsePositive = 0;
        for (int i = mid; i < num; i++) {
            boolean has = vbf.check(int2bytes(i));
            if (has) {
                //System.out.println(String.format("  err for %d", i));
                falsePositive++;
            }
        }
        double errRate = (double)falsePositive / (double)num * 100;
        //System.out.println(String.format("errRate=%.2f%% false_positive=%d num=%d hitRate=%f", errRate, falsePositive, num, hitRate));
        assertFalse(errRate > 1, String.format("too big error rate: %.2f%% false_positive=%d num=%d hitRate=%f", errRate, falsePositive, num, hitRate));
    }

    static byte[] int2bytes(int n) throws Exception {
        String s = String.valueOf(n);
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
