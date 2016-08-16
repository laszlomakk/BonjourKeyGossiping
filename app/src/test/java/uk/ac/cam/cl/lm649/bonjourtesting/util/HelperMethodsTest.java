package uk.ac.cam.cl.lm649.bonjourtesting.util;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class HelperMethodsTest {

    @Test
    public void testGetNLowBitsOfByteArray() throws Exception {
        byte[][] inputByteArr = new byte[][] {
                new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255},
                new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255},
                new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255},
                new byte[] {(byte) 37, (byte) 73, (byte) 168},
                new byte[] {(byte) 37, (byte) 73, (byte) 168},
        };
        int[] inputNBits = new int[] {
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
        for (int i = 0; i < inputByteArr.length; i++) {
            assertEquals(
                    Hex.toHexString(output[i]),
                    Hex.toHexString(
                            HelperMethods.getNLowBitsOfByteArray(inputByteArr[i], inputNBits[i]))
            );
        }
    }
}