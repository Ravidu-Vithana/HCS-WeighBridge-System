package com.hcs.weighbridge.util;

import io.github.cdimascio.dotenv.Dotenv;
import org.mindrot.jbcrypt.BCrypt;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for security operations.
 */
public class SecurityUtil {
    private static final Logger logger = LogUtil.getLogger(SecurityUtil.class);
    private static final Dotenv dotenv;

    private static final String ALGORITHM;
    private static final String KEY;
    private static final String IV;

    static {
        // Load .env from project root
        dotenv = Dotenv.configure().load();
        
        ALGORITHM = getRequiredEnv("SECURITY_ALGORITHM");
        KEY = getRequiredEnv("SECURITY_KEY");
        IV = getRequiredEnv("SECURITY_IV");

        logger.info("Security configuration loaded from .env");
    }

    private static String getRequiredEnv(String key) {
        String value = dotenv.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }

    public static void initialize() {
        logger.debug("SecurityUtil initialized with algorithm: {}", ALGORITHM);
    }

    /**
     * Hashes a password using BCrypt.
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Checks if a plaintext password matches a hashed version.
     */
    public static boolean checkPassword(String password, String hashed) {
        try {
            return BCrypt.checkpw(password, hashed);
        } catch (Exception e) {
            logger.error("Error checking password hash", e);
            return false;
        }
    }

    /**
     * Encrypts a string using AES-256.
     */
    public static String encrypt(String value) {
        if (value == null || value.isEmpty())
            return value;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            logger.error("Error during encryption", ex);
            throw new RuntimeException("Encryption failed", ex);
        }
    }

    /**
     * Decrypts an encrypted string using AES-256.
     */
    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty())
            return encrypted;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);

            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            logger.debug("Failed to decrypt value: {}", encrypted);
            throw new RuntimeException("Decryption failed", ex);
        }
    }
}
