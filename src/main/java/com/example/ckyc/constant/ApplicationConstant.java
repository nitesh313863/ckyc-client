package com.example.ckyc.constant;

public final class ApplicationConstant {

    private ApplicationConstant() {
    }

    public static final class Error {
        private Error() {
        }

        public static final String CKYC_ERROR_CODE = "CKYC_1001";
        public static final String CKYC_ERROR_MESSAGE = "CKYC request validation failed";

        public static final String MISSING_PARAMETER_CODE = "CKYC_1002";
        public static final String MISSING_PARAMETER_MESSAGE = "Missing required request parameter";

        public static final String INVALID_PARAMETER_CODE = "CKYC_1003";
        public static final String INVALID_PARAMETER_MESSAGE = "Invalid request parameter";

        public static final String MALFORMED_REQUEST_CODE = "CKYC_1004";
        public static final String MALFORMED_REQUEST_MESSAGE = "Malformed request payload";

        public static final String METHOD_NOT_ALLOWED_CODE = "CKYC_1005";
        public static final String METHOD_NOT_ALLOWED_MESSAGE = "HTTP method not allowed";

        public static final String UNSUPPORTED_MEDIA_TYPE_CODE = "CKYC_1006";
        public static final String UNSUPPORTED_MEDIA_TYPE_MESSAGE = "Unsupported content type";

        public static final String CKYC_UPSTREAM_ERROR_CODE = "CKYC_1007";
        public static final String CKYC_UPSTREAM_ERROR_MESSAGE = "CKYC endpoint returned an error response";

        public static final String CKYC_CONNECTIVITY_ERROR_CODE = "CKYC_1008";
        public static final String CKYC_CONNECTIVITY_ERROR_MESSAGE = "Unable to connect CKYC endpoint";

        public static final String UNEXPECTED_ERROR_CODE = "CKYC_1009";
        public static final String UNEXPECTED_ERROR_MESSAGE = "Request could not be processed";

        public static final String CKYC_VALIDATION_ERROR_CODE = "CKYC_1010";
        public static final String CKYC_VALIDATION_ERROR_MESSAGE = "Request validation failed";

        public static final String CKYC_ENCRYPTION_ERROR_CODE = "CKYC_1011";
        public static final String CKYC_ENCRYPTION_ERROR_MESSAGE = "Failed to encrypt/decrypt CKYC payload";

        public static final String CKYC_SIGNATURE_ERROR_CODE = "CKYC_1012";
        public static final String CKYC_SIGNATURE_ERROR_MESSAGE = "CKYC signature processing failed";
    }
}
