package uk.ac.cam.cl.lm649.bonjourtesting.util;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class PhoneNumUtilTest {

    private static final String[] dirtyPhoneNumbers = new String[] {
            "06 70 123 4567",
            "36 30/234-2345",
            "0036302342345",
            "07-123-456-789",
            "01223 247209",
            "020 33266391",
            "0//-2-0 **3.3.266|   |3?91",
    };

    @Test
    public void testFormatPhoneNumber() throws Exception {
        String[] inputPhoneNumberOfThisDevice = new String[] {
                "+36204445555",
                "+36204445555",
                "+36204445555",
                "+447555666777",
                "+447555666777",
                "+447555666777",
                "+447555666777",
        };
        String[] output = new String[] {
                "+36701234567",
                "+36302342345",
                "+36302342345",
                "+447123456789",
                "+441223247209",
                "+442033266391",
                "+442033266391",
        };

        Method method = PhoneNumUtil.class.getDeclaredMethod("_formatPhoneNumber", String.class, String.class);
        method.setAccessible(true);

        for (int i = 0; i < dirtyPhoneNumbers.length; i++) {
            assertEquals(
                    output[i],
                    method.invoke(null, dirtyPhoneNumbers[i], inputPhoneNumberOfThisDevice[i])
            );
        }
    }

    @Test
    public void testNaiveSanitizePhoneNumber() throws Exception {
        String[] output = new String[] {
                "06701234567",
                "36302342345",
                "0036302342345",
                "07123456789",
                "01223247209",
                "02033266391",
                "02033266391",
        };

        for (int i = 0; i < dirtyPhoneNumbers.length; i++) {
            assertEquals(
                    output[i], PhoneNumUtil.naiveSanitizePhoneNumber(dirtyPhoneNumbers[i])
            );
        }
    }

}