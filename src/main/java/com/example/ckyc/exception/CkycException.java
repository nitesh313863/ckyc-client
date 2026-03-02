package com.example.ckyc.exception;

public class CkycException extends RuntimeException {
    public CkycException(String message) { super(message); }
    public CkycException(String message, Throwable cause) { super(message, cause); }
}
