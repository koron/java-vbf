package net.kaoriya.vbf;

import static org.junit.jupiter.api.Assertions.*;

class TestUtils {

    static byte[] int2bytes(int n) throws Exception {
        String s = String.valueOf(n);
        return s.getBytes("UTF-8");
    }

    static void checkFPRate(VBF vbf, int num, double hitRate) throws Exception {
        int mid = (int)((double)num * hitRate + 0.5);
        for (int i = 0; i < mid; i++) {
            String s = String.valueOf(i);
            vbf.put(int2bytes(i));
        }

	// check no false negative entries.
        for (int i = 0; i < mid; i++) {
            boolean has = vbf.check(int2bytes(i), 0);
            assertTrue(has);
        }

        // check false positive rate is less than 1%
        int falsePositive = 0;
        for (int i = mid; i < num; i++) {
            boolean has = vbf.check(int2bytes(i), 0);
            if (has) {
                //System.out.println(String.format("  err for %d", i));
                falsePositive++;
            }
        }
        double errRate = (double)falsePositive / (double)num * 100;
        //System.out.println(String.format("errRate=%.2f%% false_positive=%d num=%d hitRate=%f", errRate, falsePositive, num, hitRate));
        assertFalse(errRate > 1, String.format("too big error rate: %.2f%% false_positive=%d num=%d hitRate=%f", errRate, falsePositive, num, hitRate));
    }

    static void checkSubtract8(VBF vbf) throws Exception {
        for (int i = 1; i <= 255 ; i++) {
            vbf.subtract(1);
            vbf.put(int2bytes(i));
        }
        int failure = 0, total = 0;
	for (int bias = 0; bias <= 255; bias++) {
            for (int i = 1; i <= 255; i++) {
                total++;
                boolean want = i > bias;
                boolean got = vbf.check(int2bytes(i), bias);
                if (got != want) {
                    assertTrue(got, String.format("false negative on: bias=%d i=%d", bias, i));
                    failure++;
                }
            }
        }
        double rate = (double)failure / (double)total * 100;
        //System.out.println(String.format("errRate=%.2f%% failure=%d total=%d", rate, failure, total));
        assertFalse(rate > 1, String.format("too big error rate: %.2f%% failure=%d total=%d", rate, failure, total));
    }

    static void checkSubtractOverflow8(VBF vbf) throws Exception {
        final int tail = 256 + 127;
        for (int i = 1; i <= tail; i++) {
            vbf.subtract(1);
            vbf.put(int2bytes(i));
        }
        int failure = 0, total = 0;
	for (int bias = 0; bias <= 255; bias++) {
            for (int i = 1; i <= 511; i++) {
                total++;
                boolean want = i > (tail - 255 + bias) && i <= tail;
                boolean got = vbf.check(int2bytes(i), bias);
                if (got != want) {
                    assertTrue(got, String.format("false negative on: bias=%d i=%d", bias, i));
                    failure++;
                }
            }
        }
        double rate = (double)failure / (double)total * 100;
        //System.out.println(String.format("errRate=%.2f%% failure=%d total=%d", rate, failure, total));
        assertFalse(rate > 1, String.format("too big error rate: %.2f%% failure=%d total=%d", rate, failure, total));
    }

}
