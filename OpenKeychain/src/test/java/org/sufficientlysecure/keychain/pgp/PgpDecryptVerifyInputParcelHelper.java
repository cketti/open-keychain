package org.sufficientlysecure.keychain.pgp;

public class PgpDecryptVerifyInputParcelHelper {
    public static boolean isAllowSymmetricDecryption(PgpDecryptVerifyInputParcel parcel) {
        return parcel.isAllowSymmetricDecryption();
    }
}
