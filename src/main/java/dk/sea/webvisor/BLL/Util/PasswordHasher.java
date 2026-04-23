package dk.sea.webvisor.BLL.Util;

// Java Imports
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher
{
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public static String hashPassword(String plainPassword)
    {
        if (plainPassword == null || plainPassword.isBlank())
        {
            throw new IllegalArgumentException("Password must not be empty.");
        }

        try {
            byte[] salt = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            byte[] hash = pbkdf2(plainPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            String saltText = Base64.getEncoder().encodeToString(salt);
            String hashText = Base64.getEncoder().encodeToString(hash);

            return "pbkdf2$" + ITERATIONS + "$" + saltText + "$" + hashText;
        } catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("Could not hash password.", e);
        }
    }

    public static boolean verifyPassword(String plainPassword, String storedPassword)
    {
        if (plainPassword == null || storedPassword == null || storedPassword.isBlank())
        {
            return false;
        }

        if (!storedPassword.startsWith("pbkdf2$"))
        {
            return false;
        }

        try {
            String[] parts = storedPassword.split("\\$");
            if (parts.length != 4)
            {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            byte[] actualHash = pbkdf2(plainPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
            return slowEquals(actualHash, expectedHash);
        }
        catch (IllegalArgumentException | GeneralSecurityException e)
        {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) throws GeneralSecurityException
    {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    private static boolean slowEquals(byte[] left, byte[] right)
    {
        if (left.length != right.length)
        {
            return false;
        }

        int diff = 0;
        for (int i = 0; i < left.length; i++)
        {
            diff |= left[i] ^ right[i];
        }
        return diff == 0;
    }

}
