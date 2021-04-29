package net.kaoriya.vbf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.Jedis;

public final class RedisVBF8 implements VBF {
    final Jedis jedis;
    final String key;
    final Indexer indexer;

    public RedisVBF8(Jedis jedis, String key, int m, int k) {
        this.jedis = jedis;
        this.key = key;
        this.indexer = new Indexer(m, k);
    }

    byte maxValue() {
        return (byte)0xff;
    }

    public void put(byte[] d) throws IOException {
        ArrayList<String> args = new ArrayList<>(2 + 4 * indexer.k);
        args.add("OVERFLOW");
        args.add("SAT");
        for (int i = 0; i < indexer.k; i++) {
            int x = indexer.index(i, d);
            args.add("INCRBY");
            args.add("u8");
            args.add(String.valueOf(x * 8));
            args.add(String.valueOf((int)maxValue() & 0xff));
        }
        jedis.bitfield(key, args.toArray(new String[0]));
    }

    public boolean check(byte[] d, int bias) throws IOException {
        if (bias < 0 || bias > 255) {
            throw new IllegalArgumentException("bias should be between 0 and 255");
        }
        ArrayList<String> args = new ArrayList<>(3 * indexer.m);
        for (int i = 0; i < indexer.k; i++) {
            int x = indexer.index(i, d);
            args.add("GET");
            args.add("u8");
            args.add(String.valueOf(x * 8));
        }
        List<Long> res = jedis.bitfield(key, args.toArray(new String[0]));
        for (long v : res) {
            if (v <= bias) {
                return false;
            }
        }
        return true;
    }

    public void subtract(int delta) throws IOException {
        if (delta < 0 || delta > 255) {
            throw new IllegalArgumentException("delta should be between 0 and 255");
        }
        if (delta == 0) {
            return;
        }

        final String val = String.valueOf(-delta);
        final int bulk = 256;

        ArrayList<String> args = new ArrayList<>(2 + 4 * bulk);

        for (int i = 0; i < indexer.m; i += bulk) {
            final int start = i;
            final int end = Integer.min(i + bulk, indexer.m);
            args.clear();
            args.add("OVERFLOW");
            args.add("SAT");
            for (int j = start; j < end; j++) {
                args.add("INCRBY");
                args.add("u8");
                args.add(String.valueOf(j * 8));
                args.add(val);
            }
            jedis.bitfield(key, args.toArray(new String[0]));
        }
    }

    public void delete() {
        jedis.del(key);
    }
}
