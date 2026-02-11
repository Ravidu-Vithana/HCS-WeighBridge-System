package com.hcs.weighbridge.util;

import io.github.cdimascio.dotenv.Dotenv;
import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.apache.logging.log4j.Logger;

/**
 * Utility class for security operations.
 * Uses AES/GCM/NoPadding for authenticated encryption.
 */
public class SecurityUtil {

    private static final Logger logger = LogUtil.getLogger(SecurityUtil.class);
    private static Dotenv dotenv;

    private static String ALGORITHM;
    private static byte[] KEY_BYTES;

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static boolean INITIALIZED = false;

    static {

    }

    private static String getRequiredEnv(String key) {
        String value = dotenv.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }

    private static void validateKeyLength(int length) {
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalStateException(
                    "Invalid AES key length: " + length + " bytes. Must be 16, 24, or 32 bytes.");
        }
    }

    public static void initialize() {
        try {
            // Get directory where JAR/EXE is located
            File appDir = new File(
                    SecurityUtil.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParentFile();

            // Build path to keys folder
            File keysDir = new File(appDir, "keys");

            dotenv = Dotenv.configure()
                    .directory(keysDir.getAbsolutePath())
                    .ignoreIfMissing()
                    .load();

            logger.info("Loading .env from: {}", keysDir.getAbsolutePath());

        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to locate application directory", e);
        }

        ALGORITHM = getRequiredEnv("SECURITY_ALGORITHM");
        String key = getRequiredEnv("SECURITY_KEY");

        KEY_BYTES = key.getBytes(StandardCharsets.UTF_8);
        validateKeyLength(KEY_BYTES.length);
        INITIALIZED = true;

        logger.info("Security configuration loaded successfully.");
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
    public static String encrypt(String value) throws Exception {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if(!INITIALIZED) {
            initialize();
        }
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        SecretKeySpec keySpec = new SecretKeySpec(KEY_BYTES, "AES");

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

        // Combine IV + ciphertext
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);

        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    public static String decrypt(String encrypted) throws Exception {
        if (encrypted == null || encrypted.isEmpty()) {
            return encrypted;
        }

        if(!INITIALIZED) {
            initialize();
        }
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);

        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        SecretKeySpec keySpec = new SecretKeySpec(KEY_BYTES, "AES");

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] plainText = cipher.doFinal(cipherText);

        return new String(plainText, StandardCharsets.UTF_8);

    }
}
