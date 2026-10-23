package services;
import java.security.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int ITERATIONS = 210000;
    private PasswordHasher() {}
    public static String hash(String password) {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }
    public static boolean verify(String password, String encoded) {
        try {
            String[] p = encoded.split(":"); int rounds = Integer.parseInt(p[0]);
            if (rounds < 10000 || rounds > 1000000) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(p[2]),
                    derive(password, Base64.getDecoder().decode(p[1]), rounds));
        } catch (RuntimeException e) { return false; }
    }
    private static byte[] derive(String password, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
}
