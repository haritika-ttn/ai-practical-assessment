package com.supporttickets.core.util;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * UTC timestamp formatting for JCR property values.
 */
public final class DateTimeUtil {

    private DateTimeUtil() {
    }

    public static String nowUtc() {
        return Instant.now().toString();
    }

    /**
     * Returns a UTC timestamp strictly after {@code previous} when updates occur in the same clock tick.
     */
    public static String nowUtcAfter(String previous) {
        Instant next = Instant.now();
        if (previous == null || previous.isBlank()) {
            return next.toString();
        }

        try {
            Instant previousInstant = Instant.parse(previous);
            if (!next.isAfter(previousInstant)) {
                next = previousInstant.plusNanos(1);
            }
        } catch (DateTimeParseException ex) {
            return next.toString();
        }

        return next.toString();
    }

    /**
     * Returns a UTC timestamp strictly after all supplied instants (and the current clock).
     */
    public static String maxUtcAfter(String... candidates) {
        Instant latest = Instant.EPOCH;
        if (candidates != null) {
            for (String candidate : candidates) {
                if (candidate == null || candidate.isBlank()) {
                    continue;
                }
                try {
                    Instant parsed = Instant.parse(candidate);
                    if (parsed.isAfter(latest)) {
                        latest = parsed;
                    }
                } catch (DateTimeParseException ex) {
                    // ignore unparsable values
                }
            }
        }

        Instant next = Instant.now();
        if (!next.isAfter(latest)) {
            next = latest.plusNanos(1);
        }
        return next.toString();
    }
}
