package com.example.ckyc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "ckyc")
public class CkycProperties {

    private String url;
    private String downloadUrl;
    private String validateOtpUrl;
    private String uploadUrl;
    private String updateUrl;

    private String version;
    private String fiCode;

    private String cersaiCert;
    private String p12Path;
    private String p12Password;
    private String p12Alias;
    private boolean allowNonStandardUploadUpdateApi = false;

    private Timeout timeout = new Timeout();
    private Retry retry = new Retry();
    private Download download = new Download();
    private Upload upload = new Upload();
    private Logging logging = new Logging();

    @Data
    public static class Timeout {
        private int connectMs = 10_000;
        private int readMs = 30_000;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long initialBackoffMs = 500L;
        private long maxBackoffMs = 5_000L;
        private double multiplier = 2.0d;
    }

    @Data
    public static class Download {
        private List<String> otpTriggerKeywords = new ArrayList<>(List.of("OTP", "ONE TIME PASSWORD"));
    }

    @Data
    public static class Upload {
        private List<String> duplicateKeywords = new ArrayList<>(List.of("DUPLICATE", "ALREADY EXISTS"));
        private int maxImageBytes = 1_048_576;
    }

    @Data
    public static class Logging {
        private int maxPayloadLength = 2_000;
    }
}
