package com.anurag.campusfix.exception;

public class FileUploadValidationException extends RuntimeException {

    public FileUploadValidationException(String message) {
        super(message);
    }

    public FileUploadValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
