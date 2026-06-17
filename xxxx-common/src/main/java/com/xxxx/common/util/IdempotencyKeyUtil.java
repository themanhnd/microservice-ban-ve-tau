package com.xxxx.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.StringJoiner;

/**
 * Utility class for generating and parsing idempotency keys.
 * Idempotency keys ensure that the same operation is not executed multiple times.
 */
public final class IdempotencyKeyUtil {

    private static final String SEPARATOR = ":";

    private IdempotencyKeyUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Tạo idempotency key ổn định từ prefix và các thành phần đầu vào, giúp nhận diện request lặp.
     * The key is formed as: prefix:hash(parts joined by ':')
     *
     * @param prefix the key prefix (e.g., "order", "payment")
     * @param parts  the parts to create a deterministic hash from
     * @return a deterministic idempotency key
     */
    public static String generate(String prefix, String... parts) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix must not be null or blank");
        }
        if (parts == null || parts.length == 0) {
            throw new IllegalArgumentException("At least one part is required");
        }

        StringJoiner joiner = new StringJoiner(SEPARATOR);
        for (String part : parts) {
            joiner.add(part != null ? part : "");
        }

        String hash = sha256(joiner.toString());
        return prefix + SEPARATOR + hash;
    }

    /**
     * Extract the prefix from an idempotency key.
     *
     * @param key the idempotency key
     * @return the prefix portion of the key
     */
    public static String extractPrefix(String key) {
        if (key == null || !key.contains(SEPARATOR)) {
            return key;
        }
        return key.substring(0, key.indexOf(SEPARATOR));
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
