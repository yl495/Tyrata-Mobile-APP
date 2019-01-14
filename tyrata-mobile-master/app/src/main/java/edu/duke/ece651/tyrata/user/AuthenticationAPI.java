package edu.duke.ece651.tyrata.user;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import edu.duke.ece651.tyrata.Common;


/**
 * This class has Authentication API
 * @author Saeed Alrahma
 * Created by Saeed on 24/6/2018.
 */

class AuthenticationAPI {
    static byte[] hashPass(String pw, byte salt[]) {
        byte hashedBytes[] = null;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Combine password and salt
            byte pwBytes[] = pw.getBytes();
            byte rawBytes[] = new byte[pwBytes.length + salt.length];
            for (int i=0; i<rawBytes.length; i++) {
                rawBytes[i] = i < pwBytes.length ? pwBytes[i] : salt[i - pwBytes.length];
            }

            StringBuilder sb_salt = new StringBuilder();
            for (byte saltByte : salt) {
                sb_salt.append(Integer.toString((saltByte & 0xff) + 0x100, 16).substring(1));
            }

            Log.d(Common.LOG_TAG_AUTHENTICATION_API, "salt hex format: " + sb_salt.toString());

            // Hash password + salt
            md.update(rawBytes);
            hashedBytes = md.digest();

            //@TODO remove debugging
            StringBuilder sb = new StringBuilder();
            for (byte hashedByte : hashedBytes) {
                sb.append(Integer.toString((hashedByte & 0xff) + 0x100, 16).substring(1));
            }

            Log.d(Common.LOG_TAG_AUTHENTICATION_API, "Password hex format: " + sb.toString());
            // [End of debugging]
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return hashedBytes;
    }

    static byte[] generateSalt() {
        // Generate salt (secure random number generator)
        SecureRandom random = new SecureRandom();
        byte saltBytes[] = new byte[8];
        random.nextBytes(saltBytes);

        return saltBytes;
    }
}
