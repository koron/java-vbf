package net.kaoriya.vbf3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbProperty;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import util.hash.MetroHash;

public final class RedisVBF3 {

    static class Key {
        String base;
        Key(String base) {
            this.base = base;
        }
        String data(int n) {
            return this.base + "_" + String.valueOf(n);
        }
        String props() {
            return this.base + "_props";
        }
        String gen() {
            return this.base + "_gen";
        }
    }

    static class Pos {
        int page;
        long index;
        Pos(long x) {
            page = (int)(x / PAGE_SIZE);
            index = (x % PAGE_SIZE) * 8;
        }
    }

    public static class Props {
        public long  m;
        public short k;

        @JsonbProperty(value = "max_life")
        public short maxLife;

        @JsonbProperty(value = "seed_base")
        public long  seedBase; // for future use

        public Props() {}

        Props(long m, short k, short maxLife) {
            this.m = m;
            this.k = k;
            this.maxLife = maxLife;
            this.seedBase = 0;
        }

        static Props get(Jedis jedis, Key key) {
            String s = jedis.get(key.props());
            if (s == null) {
                return null;
            }
            Jsonb jsonb = JsonbBuilder.create();
            return jsonb.fromJson(s, Props.class);
        }

        void put(Jedis jedis, Key key) {
            String s = JsonbBuilder.create().toJson(this);
            jedis.set(key.props(), s);
        }

        void check(Props want) throws VBF3Exception {
            if (this.m != want.m || this.k != want.k || this.maxLife != want.maxLife) {
                throw new VBF3Exception.ParameterMismatch(want, this);
            }
        }

        Pos[] hash(byte[] d) {
            Pos[] pp = new Pos[k];
            for (int i = 0; i < k; i++) {
                long x = MetroHash.hash64((long)i, d).get() % m;
                if (x < 0) {
                    x = -x;
                }
                pp[i] = new Pos(x);
            }
            return pp;
        }
    }

    public static class Gen {
        public short bottom;
        public short top;

        public Gen(){}

        Gen(short maxLife) {
            bottom = 1;
            top = maxLife;
        }

        static Gen get(Jedis jedis, Key key) throws VBF3Exception {
            String s = jedis.get(key.gen());
            if (s == null) {
                throw new VBF3Exception.NoGenerationInfo();
            }
            Jsonb jsonb = JsonbBuilder.create();
            return jsonb.fromJson(s, Gen.class);
        }

        void put(Jedis jedis, Key key) throws VBF3Exception {
            String s = JsonbBuilder.create().toJson(this);
            jedis.set(key.gen(), s);
        }

        boolean isValid(byte d) {
            short n = (short)(d & 0xff);
            return isValid(n);
        }

        boolean isValid(short n) {
            boolean a = bottom <= n;
            boolean b = top >= n;
            if (bottom <= top) {
                return a && b;
            }
            return a || b;
        }

        void advance(short d) {
            bottom = m255p1add(bottom, d);
            top = m255p1add(top, d);
        }
    }

    public static RedisVBF3 open(Jedis jedis, String name, long m, short k, short maxLife) throws VBF3Exception {
        Key key = new Key(name);
        boolean newProps = false;
        Props required = new Props(m, k, maxLife);
        Props props = Props.get(jedis, key);
        if (props == null) {
            props = required;
            newProps = true;
        } else {
            props.check(required);
        }
        if (newProps) {
            Gen gen = new Gen(maxLife);
            props.put(jedis, key);
            gen.put(jedis, key);
        }
        return new RedisVBF3(jedis, key, props, (int)(m / PAGE_SIZE + 1));
    }

    public final static long PAGE_SIZE = 512 * 1024 * 1024;

    Jedis jedis;
    Key   key;
    Props props;
    int   pageNum;

    RedisVBF3(Jedis jedis, Key key, Props props, int pageNum) {
        this.jedis = jedis;
        this.key = key;
        this.props = props;
        this.pageNum = pageNum;
    }

    List<List<Long>> get(Pos[] pp) {
        int[] pages = new int[pageNum];
        ArrayList<String> args = new ArrayList<>(pp.length * 3);
        for (Pos p : pp) {
            pages[p.page]++;
            args.add("GET");
            args.add("u8");
            args.add(Long.toString(p.index));
        }

        ArrayList<Response<List<Long>>> rr1 = new ArrayList<>(pageNum);
        int x = 0;
        Transaction tx = jedis.multi();
        for (int i = 0; i < pages.length; i++) {
            int n = pages[i];
            if (n == 0) {
                continue;
            }
            String[] subArgs = args.subList(x, x + n * 3).toArray(new String[0]);
            x += n * 3;
            rr1.add(tx.bitfield(key.data(i), subArgs));
        }
        tx.exec();

        ArrayList<List<Long>> rr2 = new ArrayList<>(pageNum);
        for (Response<List<Long>> r : rr1) {
            rr2.add(r.get());
        }
        return rr2;
    }

    public void put(byte[] d, short life) throws VBF3Exception {
        if (life > props.maxLife) {
            throw new VBF3Exception.TooBigLife(props.maxLife);
        }
        Gen gen = Gen.get(jedis, key);

        Pos[] pp = props.hash(d);

	// get current values by hashed `d` keys
        List<List<Long>> rr = get(pp);

	// detect updates

        int updateIndex = 0;
        int[] updatePages = new int[pageNum];
        ArrayList<Pos> updates = new ArrayList<>(pp.length);
        for (List<Long> r : rr) {
            if (r == null || r.size() == 0) {
                continue;
            }
            for (Long vlong : r) {
                short v = vlong.shortValue();
                short curr = 0;
                if (gen.isValid(v)) {
                    curr = (short)(v - gen.bottom + 1);
                    if (v < gen.bottom) {
                        curr--;
                    }
                }
                if (curr == 0 || life > curr) {
                    updatePages[pp[updateIndex].page]++;
                    updates.add(pp[updateIndex]);
                }
                updateIndex++;
            }
        }
        if (updates.size() == 0) {
            return;
        }

	// apply updates

        final String nv = Short.toString(m255p1add(gen.bottom, (short)(life - 1)));
        int base = 0;
        Transaction tx = jedis.multi();
        for (int i = 0; i < updatePages.length; i++) {
            int n = updatePages[i];
            if (n == 0) {
                continue;
            }
            ArrayList<String> args2 = new ArrayList<>(n * 4);
            for (int j = 0; j < n; j++) {
                args2.add("SET");
                args2.add("u8");
                args2.add(Long.toString(updates.get(base + j).index));
                args2.add(nv);
            }
            base += n;
            tx.bitfield(key.data(i), args2.toArray(new String[0]));
        }
        tx.exec();
    }

    public boolean check(byte[] d) throws VBF3Exception {
        Gen gen = Gen.get(jedis, key);

        Pos[] pp = props.hash(d);

	// get current values by hashed `d` keys
        List<List<Long>> rr = get(pp);

	// detect invalids

        boolean validAll = true;
        int invalidIndex = 0;
        int[] invalidPages = new int[pageNum];
        ArrayList<Pos> invalids = new ArrayList<>(pp.length);
        for (List<Long> r : rr) {
            if (r == null || r.size() == 0) {
                continue;
            }
            for (Long vlong : r) {
                short v = vlong.shortValue();
                if (!gen.isValid(v)) {
                    validAll = false;
                    if (v != 0) {
                        invalidPages[pp[invalidIndex].page]++;
                        invalids.add(pp[invalidIndex]);
                    }
                }
                invalidIndex++;
            }
        }

        if (validAll) {
            return true;
        }
        if (invalids.size() == 0) {
            return false;
        }

        // clear invalids

        int base = 0;
        Transaction tx = jedis.multi();
        for (int i = 0; i < invalidPages.length; i++) {
            int n = invalidPages[i];
            if (n == 0) {
                continue;
            }
            ArrayList<String> args2 = new ArrayList<>(n * 4);
            for (int j = 0; j < n; j++) {
                args2.add("SET");
                args2.add("u8");
                args2.add(Long.toString(invalids.get(base + j).index));
                args2.add("0");
            }
            base += n;
            tx.bitfield(key.data(i), args2.toArray(new String[0]));
        }
        tx.exec();

        return false;
    }

    public void advanceGeneration(short generations) throws VBF3Exception {
        // FIXME: use watch/retry transaction.
        Gen gen = Gen.get(jedis, key);
        gen.advance(generations);
        gen.put(jedis, key);
    }

    public void sweep() throws VBF3Exception {
        // FIXME: use watch/retry transaction.
        Gen gen = Gen.get(jedis, key);
        for (int pn = 0; pn < pageNum; pn++) {
            byte[] dataKey = key.data(pn).getBytes(StandardCharsets.UTF_8);
            jedis.watch(dataKey);
            byte[] b = jedis.get(dataKey);
            if (b == null) {
                jedis.unwatch();
                continue;
            }
            boolean modified = false;
            for (int i = 0; i < b.length; i++) {
                if (b[i] != 0 && !gen.isValid(b[i])) {
                    b[i] = 0;
                    modified = true;
                }
            }
            if (modified) {
                jedis.unwatch();
                continue;
            }
            Transaction tx = jedis.multi();
            tx.set(dataKey, b);
            tx.exec();
        }
    }

    public void drop() throws VBF3Exception {
        // FIXME: rewrite without RedisVBF3.drop()
        RedisVBF3.drop(jedis, key.base);
    }

    public static void drop(Jedis jedis, String name) throws  VBF3Exception {
        Set<String> keys = jedis.keys(name + "_*");
        if (keys.size() == 0) {
            return;
        }
        jedis.del(keys.toArray(new String[0]));
    }

    static short m255p1add(short a, short b) {
        int v = a + b;
        while (v > 255) {
            v -= 255;
        }
        return (short)v;
    }
}
