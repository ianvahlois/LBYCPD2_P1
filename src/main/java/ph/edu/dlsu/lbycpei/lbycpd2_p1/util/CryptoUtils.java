package ph.edu.dlsu.lbycpei.lbycpd2_p1.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Minimal AES‑GCM encryption utilities used to protect exported payroll CSV
 * files on disk.
 *
 * Format of the encrypted payload (all UTF‑8 text):
 *
 * <pre>
 * PAYROLL_ENC_V1
 * base64(salt)
 * base64(iv)
 * base64(cipherText)
 * </pre>
 *
 * The same login password is used as the basis for the encryption key via
 * PBKDF2WithHmacSHA256.
 */
public final class CryptoUtils {

    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12; // recommended for GCM
    private static final int KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 65_536;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CryptoUtils() { }

    /**
     * Encrypts the given plain text using AES‑GCM and the provided password.
     * @return UTF‑8 encoded encrypted payload following the format in the class javadoc.
     */
    public static byte[] encryptToBytes(String plainText, char[] password) {
        if (plainText == null) throw new IllegalArgumentException("plainText must not be null");
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("password must not be empty for encryption");
        }
        try {
            byte[] salt = new byte[SALT_BYTES];
            RANDOM.nextBytes(salt);
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            Base64.Encoder enc = Base64.getEncoder();
            StringBuilder sb = new StringBuilder();
            sb.append("PAYROLL_ENC_V1").append('\n');
            sb.append(enc.encodeToString(salt)).append('\n');
            sb.append(enc.encodeToString(iv)).append('\n');
            sb.append(enc.encodeToString(cipherText)).append('\n');
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt payroll CSV", e);
        }
    }
}

