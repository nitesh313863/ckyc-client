package com.example.ckyc.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String status,
        int statusCode,
        String errorCode,
        String message,
        String path,
        String timestamp,
        Object details
) {
}
