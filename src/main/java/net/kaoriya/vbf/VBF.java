package net.kaoriya.vbf;

import java.io.IOException;

public interface VBF {
    void put(byte[] d) throws IOException;
    boolean check(byte[] d, int bias) throws IOException;
    void subtract(int delta) throws IOException;
}
