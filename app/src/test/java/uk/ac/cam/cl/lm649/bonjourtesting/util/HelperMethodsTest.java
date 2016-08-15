package uk.ac.cam.cl.lm649.bonjourtesting.util;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class HelperMethodsTest {

    @Test
    public void testGetNLowBitsOfByteArray() throws Exception {
        byte[][] input1 = new byte[][] {
                new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255},
                new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255},
                new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255},
                new byte[] {(byte) 37, (byte) 73, (byte) 168},
                new byte[] {(byte) 37, (byte) 73, (byte) 168},
        };
        int[] input2 = new int[] {
                -5,
                99999,
                10,
                22,
                16
        };
        byte[][] output = new byte[][] {
                new byte[0],
                new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255},
                new byte[] {(byte) 255, (byte) 3},
                new byte[] {(byte) 37, (byte) 73, (byte) 40},
                new byte[] {(byte) 37, (byte) 73},
        };
        for (int i = 0; i < input1.length; i++) {
            assertEquals(
                    Hex.toHexString(output[i]),
                    Hex.toHexString(HelperMethods.getNLowBitsOfByteArray(input1[i], input2[i]))
            );
        }
    }
}