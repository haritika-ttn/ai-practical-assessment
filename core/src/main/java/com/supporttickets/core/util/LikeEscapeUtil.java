package com.supporttickets.core.util;

/**
 * Escapes characters with special meaning in Oak QueryBuilder LIKE predicates.
 */
public final class LikeEscapeUtil {

    private LikeEscapeUtil() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("'", "''");
    }
}
