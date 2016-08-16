package uk.ac.cam.cl.lm649.bonjourtesting.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

public final class PhoneNumUtil {

    private static final String TAG = "PhoneNumUtil";

    private PhoneNumUtil() {}

    public static String naiveSanitizePhoneNumber(String phoneNumber) {
        if (null == phoneNumber) {
            return null;
        }
        return phoneNumber.replaceAll("[^0-9+]", "");
    }

    private static String _formatPhoneNumber(String targetPhoneNumber, String phoneNumberOfLocalDevice) throws NumberParseException {
        if (null == targetPhoneNumber) {
            return null;
        }

        targetPhoneNumber = naiveSanitizePhoneNumber(targetPhoneNumber);

        if (targetPhoneNumber.length() == 0 || targetPhoneNumber.charAt(0) == '+') {
            return targetPhoneNumber;
        }

        PhoneNumberUtil util          = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber localNumberObject = util.parse(phoneNumberOfLocalDevice, null);

        String localCountryCode       = util.getRegionCodeForNumber(localNumberObject);

        Phonenumber.PhoneNumber numberObject      = util.parse(targetPhoneNumber, localCountryCode);
        return util.format(numberObject, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    public static String formatPhoneNumber(String phoneNumber, String phoneNumberOfLocalDevice) {
        try {
            return _formatPhoneNumber(phoneNumber, phoneNumberOfLocalDevice);
        } catch (NumberParseException e) {
            FLogger.w(TAG, e);
            return phoneNumber;
        }
    }

}
