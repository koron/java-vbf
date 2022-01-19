package net.kaoriya.vbf3exp;

import java.io.IOException;
import java.io.PrintWriter;

import org.redisson.config.Config;
import org.redisson.Redisson;

public class Exp1 {
    public static void main(String[] args) throws IOException {
        testWithRedisson();
    }

    private static void testWithRedisson() throws IOException {
        var config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        var client = Redisson.create(config);
        try {
            System.out.println("delete old keys");
            /*
            var keys = client.getKeys();
            for (int i = 0; i < 32; i++) {
                var k = "foo" + Integer.toString(i);
                keys.delete(k);
            }
            */

            for (int i = 32; i < 64; i++) {
                System.out.println(String.format("write #%d hank", i));
                var k = "foo" + Integer.toString(i);
                var s = client.getBinaryStream(k);
                var d = String.format("%02x", i).repeat(512*1024*1024/2).getBytes();
                System.out.println("  writing");
                try (var o = s.getOutputStream()) {
                    o.write(d);
                }
            }
        } finally {
            client.shutdown();
        }
    }
}
