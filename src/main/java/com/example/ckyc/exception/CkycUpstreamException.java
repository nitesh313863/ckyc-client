package com.example.ckyc.exception;

import lombok.Getter;

@Getter
public class CkycUpstreamException extends CkycException {

    private final int upstreamStatusCode;
    private final boolean retryable;
    private final String upstreamResponseBody;

    public CkycUpstreamException(String message, int upstreamStatusCode, boolean retryable, String upstreamResponseBody) {
        super(message);
        this.upstreamStatusCode = upstreamStatusCode;
        this.retryable = retryable;
        this.upstreamResponseBody = upstreamResponseBody;
    }

    public CkycUpstreamException(
            String message,
            Throwable cause,
            int upstreamStatusCode,
            boolean retryable,
            String upstreamResponseBody
    ) {
        super(message, cause);
        this.upstreamStatusCode = upstreamStatusCode;
        this.retryable = retryable;
        this.upstreamResponseBody = upstreamResponseBody;
    }
}
