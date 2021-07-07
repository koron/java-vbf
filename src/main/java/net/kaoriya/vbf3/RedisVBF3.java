package net.kaoriya.vbf3;

import redis.clients.jedis.Jedis;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbProperty;

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

    static class Props {
        long  m;
        short k;

        @JsonbProperty(value = "max_life")
        short maxLife;

        @JsonbProperty(value = "seed_base")
        long  seedBase; // for future use

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
    }

    static class Gen {
        short bottom;
        short top;

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

    public void put(byte[] d, short life) throws VBF3Exception {
        // TODO:
    }

    public boolean check(byte[] d) throws VBF3Exception {
        // TODO:
        return false;
    }

    public void advanceGeneration(short generations) throws VBF3Exception {
        // TODO:
    }

    public void sweep() throws VBF3Exception {
        // TODO:
    }

    public void drop() throws VBF3Exception {
        // TODO:
        RedisVBF3.drop(jedis, key.base);
    }

    public static void drop(Jedis jedis, String name) throws  VBF3Exception {
        // TODO:
    }
}
