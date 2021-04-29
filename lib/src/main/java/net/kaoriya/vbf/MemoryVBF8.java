package net.kaoriya.vbf;

import java.io.IOException;

public final class MemoryVBF8 implements VBF {
    final Indexer indexer;
    final byte[] data;

    public MemoryVBF8(int m, int k) {
        this.indexer = new Indexer(m, k);
        this.data = new byte[m];
    }

    byte maxValue() {
        return (byte)0xff;
    }

    public void put(byte[] d) throws IOException {
        for (int i = 0; i < indexer.k; i++) {
            int x = indexer.index(i, d);
            data[x] = maxValue();
        }
    }

    public boolean check(byte[] d, int bias) throws IOException {
        if (bias < 0 || bias > 255) {
            throw new IllegalArgumentException("bias should be between 0 and 255");
        }
        for (int i = 0; i < indexer.k; i++) {
            int x = indexer.index(i, d);
            int v = ((int)data[x]) & 0xff;
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
        for (int i = 0; i < data.length; i++) {
            int v = ((int)data[i]) & 0xff;
            if (v > delta) {
                data[i] -= (byte)delta;
            } else {
                data[i] = 0;
            }
        }
    }
}
