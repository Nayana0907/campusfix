package com.anurag.campusfix.exception;

public class VerificationConflictException extends RuntimeException {

    public VerificationConflictException(String message) {
        super(message);
    }

    public VerificationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
