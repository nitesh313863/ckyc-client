package com.example.ckyc.exception;

public class CkycValidationException extends CkycException {
    public CkycValidationException(String message) {
        super(message);
    }

    public CkycValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
