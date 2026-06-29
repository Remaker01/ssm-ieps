package com.ieps.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {
    
    @Test
    void hashPasswordShouldUseVersionedRandomFormat() {
        String firstHash = PasswordUtil.hashPassword("Secret123");
        String secondHash = PasswordUtil.hashPassword("Secret123");
        
        assertTrue(firstHash.startsWith("PBKDF2$310000$"));
        assertTrue(secondHash.startsWith("PBKDF2$310000$"));
        assertNotEquals(firstHash, secondHash);
    }
    
    @Test
    void matchesShouldValidateCorrectAndIncorrectPasswords() {
        String hash = PasswordUtil.hashPassword("Secret123");
        
        assertTrue(PasswordUtil.matches("Secret123", hash));
        assertFalse(PasswordUtil.matches("Wrong123", hash));
    }
    
    @Test
    void shouldRejectInvalidAndLegacyFormats() {
        String legacyPassword = EncryptUtil.AESencode("Secret123", "123456");
        
        assertFalse(PasswordUtil.isPasswordHash(null));
        assertFalse(PasswordUtil.isPasswordHash("invalid-format"));
        assertFalse(PasswordUtil.isPasswordHash(legacyPassword));
        assertFalse(PasswordUtil.matches("Secret123", legacyPassword));
    }

    @Test
    void validatePasswordPolicyShouldEnforceLengthAndCategoryRules() {
        assertNull(PasswordUtil.validatePasswordPolicy("Password1"));
        assertNull(PasswordUtil.validatePasswordPolicy("lowercase@"));
        assertNull(PasswordUtil.validatePasswordPolicy("UPPERCASE9"));

        assertFalse(PasswordUtil.isPasswordPolicyValid("abcdefg"));
        assertFalse(PasswordUtil.isPasswordPolicyValid("abcdefgh"));
        assertFalse(PasswordUtil.isPasswordPolicyValid("ABCDEFGH"));
        assertFalse(PasswordUtil.isPasswordPolicyValid("12345678"));
        assertFalse(PasswordUtil.isPasswordPolicyValid("abcd 1234"));
        assertFalse(PasswordUtil.isPasswordPolicyValid("Ab1!Ab1!Ab1!Ab1!Ab1!Ab1!Ab1!Ab1"));
    }
}
