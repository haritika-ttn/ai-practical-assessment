package com.supporttickets.core.api.error;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON error envelope per api-contract.md section 4.
 */
public class ApiErrorResponse {

    private String code;
    private String message;
    private Map<String, String> fields;
    private Map<String, Object> details;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(ErrorCode code, String message) {
        this.code = code.name();
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public void addField(String name, String errorMessage) {
        if (fields == null) {
            fields = new LinkedHashMap<>();
        }
        fields.put(name, errorMessage);
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
