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
    void falsePositiveRates_0200_01() throws Exception {
        try (Jedis jedis = getJedis()) {
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-200-0.1", 1000, 7, 200, 0.1);
        }
    }

    @Test
    void falsePositiveRates_0200_05() throws Exception {
        try (Jedis jedis = getJedis()) {
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-200-0.5", 1000, 7, 200, 0.5);
        }
    }

    @Test
    void falsePositiveRates_0200_09() throws Exception {
        try (Jedis jedis = getJedis()) {
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-200-0.9", 1000, 7, 200, 0.9);
        }
    }

    @Test
    void falsePositiveRates_0400_01() throws Exception {
        try (Jedis jedis = getJedis()) {
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-400-0.1", 1000, 7, 400, 0.1);
        }
    }

    @Test
    void falsePositiveRates_0700_01() throws Exception {
        try (Jedis jedis = getJedis()) {
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-700-0.1", 1000, 7, 700, 0.1);
        }
    }

    @Test
    void falsePositiveRates_1000_01() throws Exception {
        try (Jedis jedis = getJedis()) {
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-FPR-1000-0.1", 1000, 7, 1000, 0.1);
        }
    }

    @Test
    void largeBasic() throws Exception {
        try (Jedis jedis = getJedis()) {
            checkFPRate(jedis, "net.kaoriya.vbf3.redis-4G-1000-0.1", 4294967296L, 7, 1000, 0.1);
        }
    }

    static void checkFPRate(Jedis jedis, String key, long m, int k, int num, double hitRate) throws Exception {
        final short maxLife = 10;
        //RedisVBF3.drop(jedis, key);
        RedisVBF3 vbf = RedisVBF3.open(jedis, key, m, (short)k, maxLife);
        try {
            checkFPRate(vbf, num, hitRate);
        } finally {
            vbf.drop();
        }
    }

    static void checkFPRate(RedisVBF3 vbf, int num, double hitRate) throws Exception {
        int mid = (int)((double)num * hitRate + 0.5);

        byte[][] vals = new byte[mid - 0][];
        for (int i = 0; i < mid; i++) {
            vals[i] =  int2bytes(i);
        }
        vbf.put((short)1, vals);
        vals = null;

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

    static class item {
        String data;
        short life;
        item(String data, short life) {
            this.data = data;
            this.life = life;
        }
    }

    @Test
    void life() throws Exception {
        final String key = "net.kaoriya.vbf3.RedisVBF3Test.life";
        final item[] items = new item[]{
            new item("foo", (short)1),
            new item("bar", (short)2),
            new item("baz", (short)3),
        };
        try (Jedis jedis = getJedis()) {
            RedisVBF3.drop(jedis, key);
            RedisVBF3 rf = RedisVBF3.open(jedis, key, 100, (short)7, (short)10);
            try {
                for (item v : items) {
                    rf.put(v.data, v.life);
                }

                for (int i = 0; i < 2; i++) {
                    for (item v : items) {
                        boolean want = v.life > i;
                        boolean got = rf.check(v.data);
                        if (got != want) {
                            assertEquals(got, want);
                        }
                    }
                    rf.advanceGeneration((short)1);
                }
            } finally {
                rf.drop();
            }
        }
    }

    void assertTopBottom(RedisVBF3 rf, short bottom, short top) throws Exception {
        var gen = RedisVBF3.Gen.get(rf.jedis, rf.key);
        assertEquals(bottom, gen.bottom);
        assertEquals(top, gen.top);
    }

    @Test
    void advanceGeneration() throws Exception {
        final String key = "net.kaoriya.vbf3.RedisVBF3Test.advanceGeneration";
        try (Jedis jedis = getJedis()) {
            RedisVBF3.drop(jedis, key);
            RedisVBF3 rf = RedisVBF3.open(jedis, key, 256, (short)1, (short)1);
            try {
                assertTopBottom(rf, (short)1, (short)1);
                for (int i = 2; i <= 255; i++) {
                    rf.advanceGeneration((short)1);
                    short want = (short)i;
                    assertTopBottom(rf, want, want);
                }
            } finally {
                rf.drop();
            }
        }
    }

    @Test
    void checkHolding() throws Exception {
        try (Jedis jedis = getJedis()) {
            checkHolding(jedis, 10_000L, 0.001);
            checkHolding(jedis, 100_000L, 0.001);
            checkHolding(jedis, 1000_000L, 0.001);
            checkHolding(jedis, 10_000_000L, 0.001);
            checkHolding(jedis, 20_000_000L, 0.001);
            checkHolding(jedis, 30_000_000L, 0.001);
            checkHolding(jedis, 40_000_000L, 0.001);
            checkHolding(jedis, 50_000_000L, 0.001);
            checkHolding(jedis, 60_000_000L, 0.001);
            checkHolding(jedis, 70_000_000L, 0.001);
            checkHolding(jedis, 80_000_000L, 0.001);
            checkHolding(jedis, 100_000_000L, 0.001);
        }
    }

    static void checkHolding(Jedis jedis, long n, double p) throws Exception {
        String key = String.format("checkHolding_n%d_p%g", n, p);
        double m = Math.ceil((double)n * Math.log(p) / Math.log(1 / Math.pow(2, Math.log(2))));
        double k = Math.round((m / (double)n) * Math.log(2));
        //System.out.println(String.format("n=%d p=%g m=%g k=%g key=%s", n, p, m, k, key));
        RedisVBF3 vbf = RedisVBF3.open(jedis, key, (long)m, (short)k, (short)10);
        try {
            final int Q = 100;
            byte[][] vals = new byte[Q][];
            for (int i = 0; i < Q; i++) {
                vals[i] = String.format("value%04d", i).getBytes(StandardCharsets.UTF_8);
            }
            vbf.put((short)1, vals);
            // check holding values
            for (int i = 0; i < Q; i++) {
                boolean has = vbf.check(vals[i]);
                assertTrue(has, String.format("value not held: n=%d p=%g i=%d", n, p, i));
            }
        } finally {
            vbf.drop();
        }
    }

    @Test
    void checkMultiple() throws Exception {
        try (Jedis jedis = getJedis()) {
            var vbf = RedisVBF3.open(jedis, "checkMultiple", 1000L, (short)7, (short)1);
            try {
                vbf.put((short)1, "foo", "bar", "baz");

                assertArrayEquals(new boolean[]{ true, true, true }, vbf.check("foo", "bar", "baz"));
                assertArrayEquals(new boolean[]{ true, true }, vbf.check("foo", "bar"));
                assertArrayEquals(new boolean[]{ true, true }, vbf.check("bar", "baz"));
                assertArrayEquals(new boolean[]{ true, true }, vbf.check("baz", "foo"));

                assertArrayEquals(new boolean[]{ false, false, false }, vbf.check("qux", "xyz", "non"));
                assertArrayEquals(new boolean[]{ false, false }, vbf.check("qux", "xyz"));
                assertArrayEquals(new boolean[]{ false, false }, vbf.check("xyz", "non"));
                assertArrayEquals(new boolean[]{ false, false }, vbf.check("non", "qux"));

                assertArrayEquals(new boolean[]{ true, false, false }, vbf.check("foo", "xyz", "non"));
                assertArrayEquals(new boolean[]{ false, true, false }, vbf.check("qux", "bar", "non"));
                assertArrayEquals(new boolean[]{ false, false, true }, vbf.check("qux", "xyz", "baz"));
            } finally {
                vbf.drop();
            }
        }
    }
}
