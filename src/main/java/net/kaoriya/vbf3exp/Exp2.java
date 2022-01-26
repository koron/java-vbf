package net.kaoriya.vbf3exp;

import java.io.IOException;
import java.io.PrintWriter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Exp2 {
    public static void main(String[] args) throws IOException {
        testWithRedisson();
    }

    private static void testWithRedisson() throws IOException {
        var pool = new JedisPool(new JedisPoolConfig(), "localhost");
        try (var jedis = pool.getResource()) {
            System.out.println("delete old keys");
            String[] delKeys = new String[32];
            for (int i = 0; i < 32; i++) {
                delKeys[i] = "foo" + Integer.toString(i);
            }
            jedis.del(delKeys);

            for (int i = 0; i < 64; i++) {
                System.out.println(String.format("write #%d hank", i));
                var k = "foo" + Integer.toString(i);
                var v = String.format("%02x", i).repeat(512*1024*1024/2);
                System.out.println("  writing");
                jedis.set(k, v);
            }
        }
    }
}
