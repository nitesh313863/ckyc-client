package com.example.ckyc.exception;

public class CkycEncryptionException extends CkycException {
    public CkycEncryptionException(String message) {
        super(message);
    }

    public CkycEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
