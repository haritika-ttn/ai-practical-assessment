package com.supporttickets.core.exception;

/**
 * Raised when an unexpected repository or infrastructure error occurs.
 */
public class InternalServiceException extends RuntimeException {

    public InternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
