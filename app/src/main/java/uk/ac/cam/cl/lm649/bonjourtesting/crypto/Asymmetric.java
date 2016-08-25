package uk.ac.cam.cl.lm649.bonjourtesting.crypto;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

public class Asymmetric {

    private static final int RSAKeySize = 4096;

    private static final byte KEY_IS_PUBLIC_FLAG = 0;
    private static final byte KEY_IS_PRIVATE_FLAG = 1;

    private static byte[] _encryptBytes(boolean isEncrypting, byte[] data, AsymmetricKeyParameter key) throws InvalidCipherTextException {
        RSAEngine engine = new RSAEngine();
        OAEPEncoding cipher = new OAEPEncoding(engine);
        cipher.init(isEncrypting, key);
        return cipher.processBlock(data, 0, data.length);
    }

    public static byte[] encryptBytes(byte[] data, AsymmetricKeyParameter key) throws InvalidCipherTextException, DataSizeException {
        if (data.length >= (RSAKeySize - 384 ) / 8 + 7) {
            throw new DataSizeException("data too long to encrypt with RSA, use symmetric encryption instead");
        }
        return _encryptBytes(true, data, key);
    }

    public static byte[] decryptBytes(byte[] data, AsymmetricKeyParameter key) throws InvalidCipherTextException {
        return _encryptBytes(false, data, key);
    }

    public static String encryptHexString(String hexString, AsymmetricKeyParameter key) throws InvalidCipherTextException, DataSizeException {
        byte[] data = Hex.decode(hexString);
        data = encryptBytes(data,key);
        return Hex.toHexString(data);
    }

    public static String decryptHexString(String hexString, AsymmetricKeyParameter key) throws InvalidCipherTextException {
        byte[] data = Hex.decode(hexString);
        data = decryptBytes(data,key);
        return Hex.toHexString(data);
    }

    public static String encryptBase64String(String base64String, AsymmetricKeyParameter key) throws InvalidCipherTextException, DataSizeException {
        byte[] data = Base64.decode(base64String);
        data = encryptBytes(data,key);
        return Base64.toBase64String(data);
    }

    public static String decryptBase64String(String base64String, AsymmetricKeyParameter key) throws InvalidCipherTextException {
        byte[] data = Base64.decode(base64String);
        data = decryptBytes(data,key);
        return Base64.toBase64String(data);
    }

    /**
     * Generates a strong new RSA key pair.
     * Takes cca. 5-10 seconds.
     */
    public static AsymmetricCipherKeyPair generateNewKeyPair() {

        SecureRandom randomNumberGenerator = new SecureRandom();

        RSAKeyGenerationParameters keyParams = new RSAKeyGenerationParameters(
                new BigInteger("10001", 16), // publicExponent
                randomNumberGenerator, // prng
                RSAKeySize, // strength
                80 // certainty
        );

        RSAKeyPairGenerator keyPairGenerator = new RSAKeyPairGenerator();
        keyPairGenerator.init(keyParams);

        AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair;
    }

    public static AsymmetricCipherKeyPair getKeyPairFromKeys(AsymmetricKeyParameter publicKey, AsymmetricKeyParameter privateKey) {
        return new AsymmetricCipherKeyPair(publicKey, privateKey);
    }

    public static AsymmetricCipherKeyPair getKeyPairFromStringKeys(String publicKey, String privateKey) throws KeyDecodingException {
        return new AsymmetricCipherKeyPair(stringKeyToKey(publicKey), stringKeyToKey(privateKey));
    }

    public static AsymmetricCipherKeyPair getKeyPairFromByteKeys(byte[] publicKey, byte[] privateKey) throws KeyDecodingException {
        return new AsymmetricCipherKeyPair(byteKeyToKey(publicKey), byteKeyToKey(privateKey));
    }

    /**
     * Convert AsymmetricKeyParameter to string in PKCS#8 format.
     * Returned string can be saved on disk with .der extension.
     */
    public static String keyToStringKey(AsymmetricKeyParameter key) {
        String codePrefix;
        String codePostfix;
        String keyMultiLine;
        try {
            if (key.isPrivate()) {
                PrivateKeyInfo privKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(key);
                byte[] keyBytes = privKeyInfo.getEncoded(); // to get PKCS#8
                // byte[] key_bytes = privKeyInfo.parsePrivateKey().toASN1Primitive().getEncoded(); // to get PKCS#1
                keyMultiLine = convertKeyToMultiLineFromBytes(keyBytes);
                codePrefix = "-----BEGIN RSA PRIVATE KEY-----\n";
                codePostfix = "\n-----END RSA PRIVATE KEY-----";
            } else {
                SubjectPublicKeyInfo pubKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key);
                byte[] keyBytes = pubKeyInfo.getEncoded(); // to get PKCS#8
                // byte[] key_bytes = pubKeyInfo.parsePublicKey().toASN1Primitive().getEncoded(); // to get PKCS#1
                keyMultiLine = convertKeyToMultiLineFromBytes(keyBytes);
                codePrefix = "-----BEGIN RSA PUBLIC KEY-----\n";
                codePostfix = "\n-----END RSA PUBLIC KEY-----";
            }
        } catch (IOException e) {
            // should never happen
            throw new RuntimeException();
        }
        return (codePrefix + keyMultiLine + codePostfix);
    }

    private static String cutStringKeyHeaders(String key) throws KeyDecodingException {
        int cutStart = 0;
        int cutEnd = 0;
        if (key.startsWith("-----BEGIN RSA PRIVATE KEY-----\n")) {
            cutStart = "-----BEGIN RSA PRIVATE KEY-----\n".length();
            cutEnd = "\n-----END RSA PRIVATE KEY-----".length();
        } else if (key.startsWith("-----BEGIN RSA PUBLIC KEY-----\n")) {
            cutStart = "-----BEGIN RSA PUBLIC KEY-----\n".length();
            cutEnd = "\n-----END RSA PUBLIC KEY-----".length();
        } else {
            throw new KeyDecodingException("invalid key headers");
        }
        return key.substring(cutStart, key.length()-cutEnd);
    }

    private static String convertKeyToMultiLineFromBytes(byte[] key_bytes) {
        return convertKeyToMultiLineFromBytes(key_bytes, 0, key_bytes.length);
    }

    private static String convertKeyToMultiLineFromBytes(byte[] key_bytes, int offset, int length) {
        String keySingleLine = Base64.toBase64String(key_bytes, offset, length);
        String keyMultiLine = "";
        for (int c = 0; c < keySingleLine.length(); c += 64) {
            int endIndex = Math.min(c + 64, keySingleLine.length());
            keyMultiLine += keySingleLine.substring(c, endIndex) + "\n";
        }
        return keyMultiLine.substring(0, keyMultiLine.length() - 1);
    }

    public static AsymmetricKeyParameter stringKeyToKey(String key) throws KeyDecodingException{
        try {
            if (key.startsWith("-----BEGIN RSA PRIVATE KEY-----\n")) {
                return PrivateKeyFactory.createKey(	Base64.decode(cutStringKeyHeaders(key)) );
            } else if (key.startsWith("-----BEGIN RSA PUBLIC KEY-----\n")) {
                return PublicKeyFactory.createKey(	Base64.decode(cutStringKeyHeaders(key)) );
            } else {
                throw new KeyDecodingException("invalid key headers: " + key.substring(0,10) + "...");
            }
        } catch (IOException e) {
            throw new KeyDecodingException("IOE: " + e);
        }
    }

    public static byte[] stringKeyToByteKey(String key) throws KeyDecodingException {
        return keyToByteKey(stringKeyToKey(key));
    }

    public static String byteKeyToStringKey(byte[] key) throws KeyDecodingException {
        return keyToStringKey(byteKeyToKey(key));
    }

    public static byte[] keyToByteKey(AsymmetricKeyParameter key) {
        byte[] ret;
        try {
            if (key.isPrivate()) {
                PrivateKeyInfo privKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(key);
                byte[] key_bytes = privKeyInfo.getEncoded(); // to get PKCS#8
                // byte[] key_bytes = privKeyInfo.parsePrivateKey().toASN1Primitive().getEncoded(); // to get PKCS#1
                ret = new byte[1 + key_bytes.length];
                ret[0] = KEY_IS_PRIVATE_FLAG;
                System.arraycopy(key_bytes, 0, ret, 1, key_bytes.length);
            } else {
                SubjectPublicKeyInfo pubKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key);
                byte[] key_bytes = pubKeyInfo.getEncoded(); // to get PKCS#8
                // byte[] key_bytes = pubKeyInfo.parsePublicKey().toASN1Primitive().getEncoded(); // to get PKCS#1
                ret = new byte[1 + key_bytes.length];
                ret[0] = KEY_IS_PUBLIC_FLAG;
                System.arraycopy(key_bytes, 0, ret, 1, key_bytes.length);
            }
        } catch (IOException e) {
            // should never happen
            throw new RuntimeException();
        }
        return ret;
    }

    public static AsymmetricKeyParameter byteKeyToKey(byte[] key) throws KeyDecodingException {
        try {
            if (key[0] == KEY_IS_PRIVATE_FLAG) {
                return PrivateKeyFactory.createKey(	Arrays.copyOfRange(key, 1, key.length) );
            } else if (key[0] == KEY_IS_PUBLIC_FLAG) {
                return PublicKeyFactory.createKey(	Arrays.copyOfRange(key, 1, key.length) );
            } else {
                throw new KeyDecodingException("Invalid byteKey. First byte is corrupted.");
            }
        } catch (IOException e) {
            throw new KeyDecodingException("IOE: " + e);
        }
    }

    public static boolean isValidKey(String key) {
        try {
            stringKeyToKey(key);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isValidKey(byte[] key) {
        try {
            byteKeyToKey(key);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static String getFingerprint(String key) {
        return Hash.hashStringToString(key);
    }

    public static String getFingerprint(byte[] key) {
        String fingerprint;
        try {
            fingerprint = getFingerprint(byteKeyToStringKey(key));
        } catch (KeyDecodingException e) {
            fingerprint = "error";
        }
        return fingerprint;
    }

    public static String getFingerprint(AsymmetricKeyParameter key) {
        return getFingerprint(keyToStringKey(key));
    }

    public static void main(String args[]) throws KeyDecodingException {
        AsymmetricCipherKeyPair keyPair = generateNewKeyPair();
        System.out.println( keyToStringKey(keyPair.getPrivate()) );
        System.out.println( keyToStringKey(keyPair.getPublic()) );

        System.out.println( "priv len: " + keyToByteKey(keyPair.getPrivate()).length + " bytes" );
        System.out.println( "pub len: "  + keyToByteKey(keyPair.getPublic()).length  + " bytes" );
    }

}
