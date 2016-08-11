package uk.ac.cam.cl.lm649.bonjourtesting.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Symmetric {

    public static final int BLOCK_SIZE_IN_BYTES = 128 / 8;
    public static final int KEY_LENGTH_IN_BYTES = 256 / 8;

    private static byte[] _encryptBytes(boolean isEncrypting, byte[] data, byte[] key) throws DataLengthException, InvalidCipherTextException {
        BlockCipher engine = new AESFastEngine();
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(engine), new PKCS7Padding());
        CipherParameters keyParam = new KeyParameter(key);
        cipher.init(isEncrypting, keyParam);
        byte[] out = new byte[cipher.getOutputSize(data.length)];
        int len = cipher.processBytes(data, 0, data.length, out, 0);
        try {
            len += cipher.doFinal(out, len);
        } catch (IllegalStateException e){
            // this should never happen
            throw new RuntimeException();
        }
        if (isEncrypting) {
            return out;
        } else {
            // remove PKCS7 padding
            byte[] out2 = new byte[len];
            System.arraycopy(out, 0, out2, 0, len);
            return out2;
        }
    }

    /**
     * @param key - needs to be 128/192/256 bits in size,
     * 					so the hexKey.length should be 32/48/64
     */
    public static byte[] encryptBytes(byte[] data, byte[] key) throws DataLengthException, InvalidCipherTextException {
        return _encryptBytes(true, data, key);
    }

    /**
     * @param key - needs to be 128/192/256 bits in size,
     * 					so the hexKey.length should be 32/48/64
     */
    public static byte[] decryptBytes(byte[] data, byte[] key) throws DataLengthException, InvalidCipherTextException {
        return _encryptBytes(false, data, key);
    }

    /**
     * @return a random 256 bit key to use with AES encryption
     */
    public static byte[] generateKey() {
        SecureRandom randomNumberGenerator = new SecureRandom();
        byte[] bytes = new byte[KEY_LENGTH_IN_BYTES];
        randomNumberGenerator.nextBytes(bytes);
        return bytes;
    }

    public static Cipher getInitialisedCipher(int opMode, byte[] keyBytes, byte[] ivBytes)
            throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        final IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        final Cipher cipher;
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(opMode, keySpec, ivSpec);
        return cipher;
    }

    public static Cipher getInitialisedCipher(int opMode, byte[] keyBytes)
            throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        byte[] zeroIv = new byte[Symmetric.BLOCK_SIZE_IN_BYTES];
        return getInitialisedCipher(opMode, keyBytes, zeroIv);
    }

}

