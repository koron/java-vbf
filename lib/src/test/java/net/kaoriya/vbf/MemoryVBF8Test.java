package net.kaoriya.vbf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemoryVBF8Test {
    @Test
    void falsePositiveRates() throws Exception {
        TestUtils.checkFPRate(new MemoryVBF8(1000, 7), 200, 0.1);
        TestUtils.checkFPRate(new MemoryVBF8(1000, 7), 200, 0.5);
        TestUtils.checkFPRate(new MemoryVBF8(1000, 7), 200, 0.9);
        TestUtils.checkFPRate(new MemoryVBF8(1000, 7), 400, 0.1);
        TestUtils.checkFPRate(new MemoryVBF8(1000, 7), 700, 0.1);
        TestUtils.checkFPRate(new MemoryVBF8(1000, 7), 1000, 0.1);
    }

    @Test
    void subtract() throws Exception {
        TestUtils.checkSubtract8(new MemoryVBF8(2048, 8));
    }

    @Test
    void subtractOverflow() throws Exception {
        TestUtils.checkSubtractOverflow8(new MemoryVBF8(2048, 8));
    }
}
