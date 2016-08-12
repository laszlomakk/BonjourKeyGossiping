package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.Context;
import android.content.SharedPreferences;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;

import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Asymmetric;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.SaveData;

public class SaveIdentityData extends SaveData {

    private static final String TAG = "SaveBadgeData";

    private static SaveIdentityData INSTANCE = null;

    private static final String SAVE_LOCATION_FOR_OWN_BADGE_CUSTOM_NAME = "my_badge_custom_name";
    private static final String SAVE_LOCATION_FOR_OWN_PRIVATE_KEY = "my_private_key";
    private static final String SAVE_LOCATION_FOR_OWN_PUBLIC_KEY = "my_public_key";
    private static final String SAVE_LOCATION_FOR_OWN_PHONE_NUMBER = "phone_number";

    private SaveIdentityData(Context context) {
        super(context, context.getString(R.string.identity_save_location));
    }

    public static synchronized SaveIdentityData getInstance(Context context) {
        if (null == INSTANCE) {
            INSTANCE = new SaveIdentityData(context);
        }
        return INSTANCE;
    }

    public void saveMyCustomName(String customName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SAVE_LOCATION_FOR_OWN_BADGE_CUSTOM_NAME, customName);
        editor.apply();
    }

    public String getMyCustomName() {
        String customName = sharedPreferences.getString(SAVE_LOCATION_FOR_OWN_BADGE_CUSTOM_NAME, "");
        if ("".equals(customName)) { // completely disallow empty names
            customName = "anon";
        }
        return customName;
    }

    // TODO if we end up regenerating our key pair at arbitrary times, locking will be needed.
    //      e.g. what if the private key is retrieved, we call regenerate, and then we get the
    //           new public key instead of the old one (so there's a mismatch)
    /**
     * Generates a new public-private key pair and persists it to disk.
     * If we already have a key pair, does nothing, unless you force re-generation.
     *
     * @param forceReGen if you want to generate a key pair even if we already have one
     */
    public synchronized void generateAndSaveMyKeypair(boolean forceReGen) {
        FLogger.i(TAG, "generateAndSaveMyKeypair() called.");
        if (!doWeHaveAKeypair() || forceReGen) {
            FLogger.i(TAG, "generateAndSaveMyKeypair() decided to generate a new key pair.");
            AsymmetricCipherKeyPair keyPair = generateKeypairWithTiming();
            saveMyPublicKey(Asymmetric.keyToStringKey(keyPair.getPublic()));
            saveMyPrivateKey(Asymmetric.keyToStringKey(keyPair.getPrivate()));
        } else {
            FLogger.i(TAG, "generateAndSaveMyKeypair() decided to use already existing key pair.");
        }
    }

    private static AsymmetricCipherKeyPair generateKeypairWithTiming() {
        long time1 = android.os.SystemClock.elapsedRealtime();
        AsymmetricCipherKeyPair keyPair = Asymmetric.generateNewKeyPair();
        long time2 = android.os.SystemClock.elapsedRealtime();
        long timeTaken = time2 - time1;
        FLogger.i(TAG, "generateAndSaveMyKeypair(). time taken: " + timeTaken + " ms");
        return keyPair;
    }

    private void saveMyPublicKey(String publicKey) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SAVE_LOCATION_FOR_OWN_PUBLIC_KEY, publicKey);
        editor.apply();
    }

    public String getMyPublicKey() {
        String publicKey = sharedPreferences.getString(SAVE_LOCATION_FOR_OWN_PUBLIC_KEY, "");
        if ("".equals(publicKey)) {
            FLogger.i(TAG, "getMyPublicKey(). generating new key pair on demand");
            generateAndSaveMyKeypair(false);
            return getMyPublicKey();
        }
        return publicKey;
    }

    private void saveMyPrivateKey(String privateKey) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SAVE_LOCATION_FOR_OWN_PRIVATE_KEY, privateKey);
        editor.apply();
    }

    public String getMyPrivateKey() {
        String privateKey = sharedPreferences.getString(SAVE_LOCATION_FOR_OWN_PRIVATE_KEY, "");
        if ("".equals(privateKey)) {
            FLogger.i(TAG, "getMyPublicKey(). generating new key pair on demand");
            generateAndSaveMyKeypair(false);
            return getMyPrivateKey();
        }
        return privateKey;
    }

    private boolean doWeHaveAKeypair() {
        String privateKey = sharedPreferences.getString(SAVE_LOCATION_FOR_OWN_PRIVATE_KEY, "");
        String publicKey = sharedPreferences.getString(SAVE_LOCATION_FOR_OWN_PUBLIC_KEY, "");
        return !"".equals(privateKey) && !"".equals(publicKey);
    }

    public void savePhoneNumber(String str) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SAVE_LOCATION_FOR_OWN_PHONE_NUMBER, str);
        editor.apply();
    }

    public String getPhoneNumber(){
        return sharedPreferences.getString(SAVE_LOCATION_FOR_OWN_PHONE_NUMBER, "+441234567890");
    }

}
