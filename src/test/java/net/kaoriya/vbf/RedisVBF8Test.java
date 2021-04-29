package net.kaoriya.vbf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

class RedisVBF8Test {
    static JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost");

    static Jedis getJedis() throws Exception {
        return pool.getResource();
    }

    static void checkFPRate(Jedis jedis, String key, int m, int k, int num, double hitRate) throws Exception {
        RedisVBF8 vbf = new RedisVBF8(jedis, key, m, k);
        vbf.delete();
        try {
            TestUtils.checkFPRate(vbf, num, hitRate);
        } finally {
            vbf.delete();
        }
    }
    @Test
    void falsePositiveRates() throws Exception {
        try (Jedis jedis = getJedis()) {
            checkFPRate(jedis, "net.kaoriya.vbf.radis8-FPR-200-0.1", 1000, 7, 200, 0.1);
            checkFPRate(jedis, "net.kaoriya.vbf.radis8-FPR-200-0.5", 1000, 7, 200, 0.5);
            checkFPRate(jedis, "net.kaoriya.vbf.radis8-FPR-200-0.9", 1000, 7, 200, 0.9);
            checkFPRate(jedis, "net.kaoriya.vbf.radis8-FPR-400-0.1", 1000, 7, 400, 0.1);
            checkFPRate(jedis, "net.kaoriya.vbf.radis8-FPR-700-0.1", 1000, 7, 700, 0.1);
            checkFPRate(jedis, "net.kaoriya.vbf.radis8-FPR-1000-0.1", 1000, 7, 1000, 0.1);
        }
    }

    @Test
    void subtract() throws Exception {
        final String key = "net.kaoriya.vbf.radis8-SUB";
        try (Jedis jedis = getJedis()) {
            RedisVBF8 vbf = new RedisVBF8(jedis, key, 2048, 8);
            vbf.delete();
            try {
                TestUtils.checkSubtract8(vbf);
            } finally {
                vbf.delete();
            }
        }
    }

    @Test
    void subtractOverflow() throws Exception {
        final String key = "net.kaoriya.vbf.radis8-SUBOVER";
        try (Jedis jedis = getJedis()) {
            RedisVBF8 vbf = new RedisVBF8(jedis, key, 2048, 8);
            vbf.delete();
            try {
                TestUtils.checkSubtractOverflow8(vbf);
            } finally {
                vbf.delete();
            }
        }
    }
}
