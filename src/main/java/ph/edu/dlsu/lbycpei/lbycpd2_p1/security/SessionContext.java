package ph.edu.dlsu.lbycpei.lbycpd2_p1.security;

/**
 * Simple in-memory session state shared across controllers.
 * Keeps track of the authenticated user and the (temporary) encryption password.
 *
 * In a larger application this would likely be replaced by a proper
 * session / user management component.
 */
public final class SessionContext {

    private static volatile boolean authenticated = false;
    private static volatile String currentUsername = null;
    /**
     * We keep a char[] instead of String so the password could be wiped later
     * if needed. For this small desktop app it stays in-memory only.
     */
    private static volatile char[] encryptionPassword = null;

    private SessionContext() { }

    public static void startSession(String username, char[] passwordForEncryption) {
        authenticated = true;
        currentUsername = username;
        // Make a defensive copy so callers can clear their own array
        if (passwordForEncryption != null) {
            encryptionPassword = passwordForEncryption.clone();
        } else {
            encryptionPassword = null;
        }
    }

    public static void endSession() {
        authenticated = false;
        currentUsername = null;
        if (encryptionPassword != null) {
            for (int i = 0; i < encryptionPassword.length; i++) {
                encryptionPassword[i] = 0;
            }
        }
        encryptionPassword = null;
    }

    public static boolean isAuthenticated() {
        return authenticated;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static char[] getEncryptionPassword() {
        return encryptionPassword;
    }
}

