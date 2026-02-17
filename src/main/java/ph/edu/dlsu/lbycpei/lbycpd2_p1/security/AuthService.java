package ph.edu.dlsu.lbycpei.lbycpd2_p1.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Very small authentication helper using a single built‑in account.
 *
 * The password is never stored or compared in plain text – instead we store
 * the SHA‑256 hash of the password and compare the hash of the user input.
 *
 * This is intentionally simple for a desktop exercise; in a real system you
 * would likely use a database and a stronger password storage scheme (PBKDF2,
 * bcrypt, scrypt, Argon2, etc.).
 */
public final class AuthService {

    private static final String USERNAME = "admin";

    /**
     * SHA‑256("admin123") in lowercase hex.
     * Calculated once and stored here so we never keep the clear‑text password
     * on disk.
     */
    private static final String PASSWORD_HASH_HEX =
            "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";

    private AuthService() { }

    /**
     * Verifies the provided credentials.
     *
     * @param username username (currently only "admin" is supported)
     * @param password password as a char array
     * @return true if credentials are valid
     */
    public static boolean authenticate(String username, char[] password) {
        if (username == null || password == null) return false;
        if (!USERNAME.equals(username.trim())) return false;
        String calculated = sha256(new String(password));
        return PASSWORD_HASH_HEX.equalsIgnoreCase(calculated);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

