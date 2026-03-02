package com.example.ckyc.exception;

public class CkycSignatureException extends CkycException {
    public CkycSignatureException(String message) {
        super(message);
    }

    public CkycSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
