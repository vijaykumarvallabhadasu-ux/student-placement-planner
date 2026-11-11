package com.placementplanner.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility for SHA-256 password hashing.
 */
public final class PasswordUtil {

    private PasswordUtil() {
        // Utility class
    }

    /**
     * Hashes a plaintext password using SHA-256 and returns a lowercase hex string.
     */
    public static String hash(String value) {
        if (value == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}