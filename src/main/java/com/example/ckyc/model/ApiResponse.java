package com.example.ckyc.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String requestId,
        String status,
        String message,
        T data,
        String timestamp
) {
    public static <T> ApiResponse<T> of(String requestId, String status, String message, T data) {
        return new ApiResponse<>(
                requestId,
                status,
                message,
                data,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
