package com.ieps.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {
    
    private static final String HASH_PREFIX = "PBKDF2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 310000;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 30;
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final String PART_SEPARATOR = "\\$";
    private static final String PASSWORD_POLICY_MESSAGE = "密码需为8-30位，且至少包含数字、大写字母、小写字母、特殊符号中的2类，且不能包含空格！";
    
    private PasswordUtil() {
    }
    
    public static String hashPassword(String rawPassword) {
        try {
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(salt);
            byte[] hash = pbkdf2(rawPassword, salt, ITERATIONS, HASH_LENGTH);
            return HASH_PREFIX + "$" + ITERATIONS + "$" + base64(salt) + "$" + base64(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("密码加密失败", e);
        }
    }
    
    public static boolean matches(String rawPassword, String storedPassword) {
        if (!isPasswordHash(storedPassword) || rawPassword == null) {
            return false;
        }
        
        String[] parts = storedPassword.split(PART_SEPARATOR);
        if (parts.length != 4) {
            return false;
        }
        
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = pbkdf2(rawPassword, salt, iterations, expectedHash.length);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }
    
    public static boolean isPasswordHash(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        String[] parts = password.split(PART_SEPARATOR);
        if (parts.length != 4 || !HASH_PREFIX.equals(parts[0])) {
            return false;
        }
        
        try {
            Integer.parseInt(parts[1]);
            Base64.getDecoder().decode(parts[2]);
            Base64.getDecoder().decode(parts[3]);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String validatePasswordPolicy(String rawPassword) {
        if (rawPassword == null) {
            return PASSWORD_POLICY_MESSAGE;
        }

        if (rawPassword.length() < MIN_PASSWORD_LENGTH || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            return PASSWORD_POLICY_MESSAGE;
        }

        boolean hasDigit = false;
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasSpecial = false;

        for (char current : rawPassword.toCharArray()) {
            if (Character.isWhitespace(current)) {
                return PASSWORD_POLICY_MESSAGE;
            }
            if (Character.isDigit(current)) {
                hasDigit = true;
            } else if (Character.isUpperCase(current)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(current)) {
                hasLowercase = true;
            } else {
                hasSpecial = true;
            }
        }

        int categoryCount = 0;
        categoryCount += hasDigit ? 1 : 0;
        categoryCount += hasUppercase ? 1 : 0;
        categoryCount += hasLowercase ? 1 : 0;
        categoryCount += hasSpecial ? 1 : 0;
        return categoryCount >= 2 ? null : PASSWORD_POLICY_MESSAGE;
    }

    public static boolean isPasswordPolicyValid(String rawPassword) {
        return validatePasswordPolicy(rawPassword) == null;
    }

    public static String getPasswordPolicyMessage() {
        return PASSWORD_POLICY_MESSAGE;
    }
    
    private static byte[] pbkdf2(String rawPassword, byte[] salt, int iterations, int hashLength) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, hashLength * 8);
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
    }
    
    private static String base64(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }
}
