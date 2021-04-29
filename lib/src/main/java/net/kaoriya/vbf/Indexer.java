package net.kaoriya.vbf;

import util.hash.MetroHash;

final class Indexer {
    final int m;
    final int k;

    Indexer(int m, int k) {
        this.m = m;
        this.k = k;
    }

    int index(int n, byte[] d) {
        long x = MetroHash.hash64((long)n, d).get() % (long)m;
        if (x < 0) {
            x = -x;
        }
        return (int)x;
    }
}
