package com.supporttickets.core.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Raised when request input fails field validation.
 */
public class ValidationException extends RuntimeException {

    private final List<String> fields;
    private final Map<String, String> fieldMessages;

    public ValidationException(String message) {
        this(message, Collections.emptyList(), Collections.emptyMap());
    }

    public ValidationException(String message, List<String> fields) {
        this(message, fields, Collections.emptyMap());
    }

    public ValidationException(String message, List<String> fields, Map<String, String> fieldMessages) {
        super(message);
        this.fields = fields == null ? Collections.emptyList() : List.copyOf(fields);
        this.fieldMessages = fieldMessages == null ? Collections.emptyMap() : Map.copyOf(fieldMessages);
    }

    public List<String> getFields() {
        return fields;
    }

    public Map<String, String> getFieldMessages() {
        return fieldMessages;
    }

    public static ValidationException withFieldMessages(String message, Map<String, String> fieldMessages) {
        return new ValidationException(message, List.copyOf(fieldMessages.keySet()), fieldMessages);
    }
}
