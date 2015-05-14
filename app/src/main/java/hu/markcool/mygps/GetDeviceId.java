package hu.markcool.mygps;


import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class GetDeviceId {

    /*
     * get device unique id, save unique
     */
    public String getDeviceId(Context context) {

        // The IMEI: Only valid only for Android phone
        // Requires READ_PHONE_STATE
        TelephonyManager TelephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        String szImei = TelephonyMgr.getDeviceId();

        // Pseudo-Unique ID, This is valid for any Android phone
        String m_szDevIDShort = "AND" + (Build.BOARD.length() % 10)
                + (Build.BRAND.length() % 10) // + (Build.CPU_ABI.length() % 10)
                + (Build.DEVICE.length() % 10) + (Build.DISPLAY.length() % 10)
                + (Build.HOST.length() % 10) + (Build.ID.length() % 10)
                + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10)
                + (Build.PRODUCT.length() % 10) + (Build.TAGS.length() % 10)
                + (Build.TYPE.length() % 10) + (Build.USER.length() % 10); // 13
        // digits

        String deviceId = szImei + m_szDevIDShort;
//        deviceId = encodeMd5(deviceId);
//        deviceId = "SOON_SOURCE_And_" + encodeMd5(deviceId);
        deviceId = "MARKCOOL_A_" + encodeMd5(deviceId);


        return deviceId;
    }


    // md5 encryption method
    private String encodeMd5(String encodeString) {
        // MessageDigest specifically for encryption

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] result = messageDigest.digest(encodeString.getBytes()); // Get the encrypted byte array

            StringBuffer sb;
            sb = new StringBuffer();

            for (byte b : result) {
                int num = b & 0xff; // Here is to be byte-type change int-type, so that the original negative into a positive
                String hex = Integer.toHexString(num); // int-type convert hex-type
                // Hexadecimal length may be 1, in which case, you need to fill in front of 0
                if (hex.length() == 1) {
                    sb.append(0);
                }
                sb.append(hex);
            }

            return sb.toString().toUpperCase();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }


}
