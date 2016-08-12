package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Symmetric;

public class SessionKey {

    public final byte[] secretKeyBytes;
    public final byte[] ivBytes;

    public SessionKey(byte[] secretKeyBytes, byte[] ivBytes)
            throws InvalidSessionKeySizeException {
        if (null == secretKeyBytes || null == ivBytes) {
            throw new InvalidSessionKeySizeException("secret key or ivBytes is null");
        }
        if (!MsgClient.isSecretKeyLengthValid(secretKeyBytes)) {
            throw new InvalidSessionKeySizeException("secret key length: " + secretKeyBytes.length);
        }
        if (Symmetric.BLOCK_SIZE_IN_BYTES != ivBytes.length) {
            throw new InvalidSessionKeySizeException("iv length: " + ivBytes.length);
        }

        this.secretKeyBytes = secretKeyBytes;
        this.ivBytes = ivBytes;
    }

    public SessionKey(byte[] secretKeyBytes) throws InvalidSessionKeySizeException {
        this(secretKeyBytes, new byte[Symmetric.BLOCK_SIZE_IN_BYTES]);
    }

    public class InvalidSessionKeySizeException extends Exception {
        public InvalidSessionKeySizeException() {
            super();
        }
        public InvalidSessionKeySizeException(String msg) {
            super(msg);
        }
    }

}
