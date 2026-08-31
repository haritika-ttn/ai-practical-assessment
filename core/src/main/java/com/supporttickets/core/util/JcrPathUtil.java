package com.supporttickets.core.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Path and identifier helpers for JCR operations.
 */
public final class JcrPathUtil {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private JcrPathUtil() {
    }

    public static boolean isValidUuid(String value) {
        return value != null && UUID_PATTERN.matcher(value).matches();
    }

    public static String newUuid() {
        return UUID.randomUUID().toString();
    }

    public static boolean isUnderSupportUserBase(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.trim();
        if (normalized.contains("..")) {
            return false;
        }
        return normalized.startsWith("/home/users/support/")
                || normalized.equals("/home/users/support");
    }
}
