package uk.ac.cam.cl.lm649.bonjourtesting.database.tables.publickeys;

import java.util.Locale;

public class PublicKeyEntryWithName extends PublicKeyEntry {

    protected String customName = null;

    public String getCustomName() {
        return customName;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "nick: %s\n%s",
                customName,
                super.toString());
    }

}
