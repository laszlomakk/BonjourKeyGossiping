package uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Asymmetric;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.AsymmetricTest;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.DataSizeException;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.KeyDecodingException;

import static org.junit.Assert.*;

public class MsgMyPublicKeyTest {

    private static final String[] PRIVATE_KEYS = new String[] {
            AsymmetricTest.HARDCODED_PRIVATE_KEY_1,
            AsymmetricTest.HARDCODED_PRIVATE_KEY_1,
            AsymmetricTest.HARDCODED_PRIVATE_KEY_2,
            AsymmetricTest.HARDCODED_PRIVATE_KEY_2,
    };
    private static final String[] PUBLIC_KEYS = new String[] {
            AsymmetricTest.HARDCODED_PUBLIC_KEY_1,
            AsymmetricTest.HARDCODED_PUBLIC_KEY_1,
            AsymmetricTest.HARDCODED_PUBLIC_KEY_2,
            AsymmetricTest.HARDCODED_PUBLIC_KEY_2,
    };
    private static final long[] TIMESTAMPS = new long[] {
            1349333576093L,
            1349333576094L,
            1349333576095L,
            1349333576096L,
    };
    private static final String[] PHONE_NUMBERS = new String[] {
            "+441223123456",
            "911",
            "+36203334444",
            "+18005553246",
    };
    private static final String[] HEX_OF_HASH = new String[] {
            "063bb567094f1c65a8a2f6aa623558a17c1dd5e0262dc8f34f1f6b3091482607",
            "7b0fd0dc4545e6a93c1fa7b985069670f5d6938c11e4668d2446ac171aa59acd",
            "c6729847cd281422b2d04e8e3a3cd7148bbc31329e7b26c4f86d2f748166583f",
            "ecc54397be0d7adb233efb4825b1def713842f0e463fe494d31b2f90b9c6540d",
    };
    private static final String[] HEX_OF_SIGNED_HASH = new String[] {
            "8ded99b26cd176384054701c2b306731e2eea819ba0ce37a7977aef37d0bd01e834da41e278a68c5400316f3cf04266be95942dcaa04b1e78442dc2e22ebab813c9722522c0a886fcc5e43e1ee63dec31dd5b00ecec7f80206b25fc9861705eaf503e0d85bd10cecca27f5ea255263d185f907d8d7766000235ac7d7761641393b9e180904c6b88944fbc556b9689a75c86c3881576c8e72c0f58fa160cac6cece8532ed96e06db0e3281299b6cef99d0c15971abc51b271a32a04daa78f63cf94939ef31c6849ac29cb852535199603f12a88509955d98dc6f8dc9aede3034a365d5fa11067d905b5d6c3c093b27f381d11372dce7c8c99ad88d6a65b56642a408dcc4a267cbceacb6621cec5af82ec06a24db237a641ebfcf54124516cfe75af6f81da85b5e01d5be3b89385fc6a8cc011e73838c850f28a8a9404b8023c49d4f95b73f2f1e50f3ef11a9e90568c199d4097b95aa53edcc7ee78f75a03bd58552c35a6f780ca2852ae6430ee9c3ab070c2b33a3345e619b34f2dc5d04ffea5d95113fd1995271ceb22309ae926280454643538c3c3851a172ffd633c1e14c486fc9d47eeab7beda7348e3279c6d3a84fdc2544041d51aaed2fd30319e3e9abf053d2daa787b2a480f168ea4645a2b513cfd3222e229de732ac16b2d4fce71217a962af0d2ff09dfb7b9977a3da8e13bb21e68a61de08908828cca61b560395",
            "407d5cd1153247e5157ee992df354b2c57dadc8eeadfd149a513ea767b12c7168c2f3db182e8f47ebb379e974636dd4f3b5927a556384405edee49664d42adbe5eb01bcdfd5714819201e0104c2deb0d99eede95f786763f597c27ea9bf440bcc2de162a6bfa355f682933e3a740aad58d6cc4da207211aee1fb1770d21da64565af849b38856c73ee0c95784b605874de22aee89c7a33912a8d628bc321736b2837093374b90e587065cc9d76e3f7350d6f423b79050a01a31d74ea5aa105f22dd627bd0024a432344937fa3eb6d880ddf865c816dceaca3b08ed20dab3ae994dbd5827f3be0bcdb6e6dc6b3ac3c2c746ff87831fef49e128b7f60df9d7d6147c8a0927e583572ebb0754637691ea526cbaca2c7f2cdad213ba803720d69abb78bd45717feff16bdb636aac7badb3343ac12ec491cda8a659ee703de3557cc1ead766b7d2f00935a8baa2e473c8c0c6a369a9a3c7d83b069225ad9e62262e9af66a344ee6c97a5d6ab6af3c5d2743568b48bf0b0a4f4343ec9e7de81dc2085153cdcc5205c49afa5817a5f317275f25271d84b7d7fd8ad96a506ffda651aad6f01ffa06ea46db94168d316841f652fd087293f8243a412de33463b63debed1f91cd9e9d9cd82c306e668d9e11a0df84bb6c4fd0876eb7f222900c032f127b63fa4a020e0d84afd0836d7a97c3db907f6879a5485cf2e45dbc34d15520c399b7",
            "13012620c276cfbe143b0232e1d0a3119a34ddc8c05eb944c682846391cd2d9af2335896223db20829cd13bcf2aaef1bd4203f1b53996b408ba85473d1d276706fdb1b24ec57336f063f744f139f9482f85aa6b566b023e341e41255ff629496eafe09d70fa215a55a3facfe83155a989ef18b5904301eb00ccaf5c74a00216b49b2c186a59dba491aa6cb5da876f5be96f5d1a336cf905861ae1e784fe96ae86b623c4878460d2bacdf79aae8348c78f8b7623ef92e1646936b2fe0ef5dc813234a04af6a97d282fac822f0420616bea9dcd6780038761be88b17c01e8291aa5ed53bf3274d47cf98f5db7db789ab11990c38990ff362cd96e6c3f08f9dafe50d26cb562f25f1e0e8683654e4b556827db0ebc1f43050f7fd47cd46b3ffd3816bbc4628968e29402d507cca3b660849a1e4601e1e9980492ac2c8382590d5713e3d66fa4ea846bb2a2a2d3c4786e826a6a91ff9eb5f4502a82ff285adfd868a53ade8fb727b8e085cbefb3baf352c4b4bf78adb63467c3811be3e29194f9ee170a0de79d11dc22f1e64472cbc641bdf48e8f77a3c9420b363ab9c0683f0855201f1c8da4900e7467e079a73d9827936f16b1fbb7770169b161c057040d173aa483c95585e2f42bd54183fe219a400294463477b1339231f700945fad01b5bdcf3b246de28626a29eb05081f91e613af800b117b46b074552fe109679134b7df",
            "23194307aad5bdabec09359e1ab074e4e296bd379cf10a1c2bf5fc0226354182f13584e0f9ec2a3904723d2f4b392d3eef6961aa00bd4d8aa345b0346661e88ef3ef6bd03d9d5dc3db5df580a2c7dcb067b74d594031985f9d0fcbd3df7aa9de82e10eb6f8150efdd2dac43b42355aad2e9eb591c62c7dc32ac526ced16f197ab9c5b87b522bc228a3bab482468c451c17b18630b0919af8e10a543a738e3b98c62a8e5e078f055472feab286ec87455f0072e6205bcf3797b6a54f0875dcc279ea55cea83145a66b77ad17012837fad45ce5fce9df6711954521ab539ca908402cb163187cbbd11d73765f05bad6e6d4967c08e8c6c593c40fb96cab6aa19f71fdfdfb635ce2798defc89379bc2462acc8d447f3126c6e894bfef960d72b760ab81f521b5180e37b325dfa108bf8d50887bd546e8b79818bbd890b4a7214f6af2cdffbf9539e28b3259f8fe779cd983b3147fad2753e20b102e9317783de26949f7d4f883acec497263e4a19d8c985f829fb66f3cd096a0974f7a894157e2f20f9f9f64870a771ca6d90c1ee38768f3c612af062334b2993a03e9249a1478debec4c7cc71fb8464a801d47df8ee117b5b01d03493b286fc7fb61fb598861b79b2c47c4954f21735cf610656e7970b83ba01ae7f05ef521e533b587617ba2bc4082519068885743de54d2b4b1308cffd2bec830a3309b607b6d3546de6ab1376",
    };

    private static Method method_verifySignedHash;

    @BeforeClass
    public static void setUpClass() throws Exception {
        method_verifySignedHash = MsgMyPublicKey.class.getDeclaredMethod("_verifySignedHash",
                byte[].class, String.class, long.class, String.class);
        method_verifySignedHash.setAccessible(true);
    }

    @Test
    public void testCalcHashOfContents() throws Exception {
        Method method = MsgMyPublicKey.class.getDeclaredMethod("calcHashOfContents", String.class, long.class, String.class);
        method.setAccessible(true);

        for (int i = 0; i < HEX_OF_HASH.length; i++) {
            assertEquals(
                    HEX_OF_HASH[i],
                    Hex.toHexString((byte[]) method.invoke(
                            null, PUBLIC_KEYS[i], TIMESTAMPS[i], PHONE_NUMBERS[i]))
            );
        }
    }

    @Test
    public void testSignedHashCreationAndVerification() throws Exception {
        for (int i = 0; i < 10; i++) {
            int input = i % HEX_OF_HASH.length;
            AsymmetricKeyParameter pubKey = Asymmetric.stringKeyToKey(
                    PUBLIC_KEYS[input]);
            AsymmetricKeyParameter privKey = Asymmetric.stringKeyToKey(
                    PRIVATE_KEYS[input]);
            byte[] signedHash = Asymmetric.encryptBytes(
                    Hex.decode(HEX_OF_HASH[input]),
                    privKey);
            byte[] decryptedSignedHash = Asymmetric.decryptBytes(
                    signedHash,
                    pubKey);
            assertEquals(
                    HEX_OF_HASH[input],
                    Hex.toHexString(decryptedSignedHash)
            );
        }
    }

    @Test
    public void testVerifySignedHash_happyPaths() throws Exception {
        for (int i = 0; i < HEX_OF_HASH.length; i++) {
            assertTrue(
                    (boolean) method_verifySignedHash.invoke(null,
                            Hex.decode(HEX_OF_SIGNED_HASH[i]),
                            PUBLIC_KEYS[i],
                            TIMESTAMPS[i],
                            PHONE_NUMBERS[i]
                    )
            );
        }
    }

    @Test
    public void testVerifySignedHash_error_corruptedHash() throws Exception {
        for (int i = 0; i < HEX_OF_HASH.length; i++) {
            try {
                assertFalse(
                        (boolean) method_verifySignedHash.invoke(null,
                                Hex.decode(HEX_OF_SIGNED_HASH[i].replace('0','1')),
                                PUBLIC_KEYS[i],
                                TIMESTAMPS[i],
                                PHONE_NUMBERS[i]
                        )
                );
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof InvalidCipherTextException)) {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testVerifySignedHash_error_IncorrectTimestamp() throws Exception {
        for (int i = 0; i < HEX_OF_HASH.length; i++) {
            try {
                assertFalse(
                        (boolean) method_verifySignedHash.invoke(null,
                                Hex.decode(HEX_OF_SIGNED_HASH[i]),
                                PUBLIC_KEYS[i],
                                TIMESTAMPS[i] + 100,
                                PHONE_NUMBERS[i]
                        )
                );
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof InvalidCipherTextException)) {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testVerifySignedHash_error_CorruptedPublicKey() throws Exception {
        for (int i = 0; i < HEX_OF_HASH.length; i++) {
            try {
                assertFalse(
                        (boolean) method_verifySignedHash.invoke(null,
                                Hex.decode(HEX_OF_SIGNED_HASH[i]),
                                PUBLIC_KEYS[i].replace('9','x'),
                                TIMESTAMPS[i],
                                PHONE_NUMBERS[i]
                        )
                );
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof KeyDecodingException)) {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testVerifySignedHash_error_InvalidPhoneNumber() throws Exception {
        for (int i = 0; i < HEX_OF_HASH.length; i++) {
            try {
                assertFalse(
                        (boolean) method_verifySignedHash.invoke(null,
                                Hex.decode(HEX_OF_SIGNED_HASH[i]),
                                PUBLIC_KEYS[i],
                                TIMESTAMPS[i],
                                PHONE_NUMBERS[i] + "2"
                        )
                );
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof InvalidCipherTextException)) {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testVerifySignedHash_error_OverlongHash() throws Exception {
        for (int i = 0; i < HEX_OF_HASH.length; i++) {
            try {
                assertFalse(
                        (boolean) method_verifySignedHash.invoke(null,
                                Hex.decode(HEX_OF_SIGNED_HASH[i] + "1a"),
                                PUBLIC_KEYS[i],
                                TIMESTAMPS[i],
                                PHONE_NUMBERS[i]
                        )
                );
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof DataSizeException)) {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testVerifySignedHash_error_DifferentPublicKey() throws Exception {
        for (int i = 0; i < HEX_OF_HASH.length; i++) {
            String publicKey = PUBLIC_KEYS[i].equals(AsymmetricTest.HARDCODED_PUBLIC_KEY_1) ?
                    AsymmetricTest.HARDCODED_PUBLIC_KEY_2 : AsymmetricTest.HARDCODED_PUBLIC_KEY_1;
            try {
                assertFalse(
                        (boolean) method_verifySignedHash.invoke(null,
                                Hex.decode(HEX_OF_SIGNED_HASH[i]),
                                publicKey,
                                TIMESTAMPS[i],
                                PHONE_NUMBERS[i]
                        )
                );
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof InvalidCipherTextException)) {
                    throw e;
                }
            }
        }
    }

}