package com.example.ckyc.config;

import com.example.ckyc.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class CkycLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("<REQUEST_ID>([^<]+)</REQUEST_ID>");
    private final MaskingUtil maskingUtil;
    private final CkycProperties ckycProperties;

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        String requestBody = new String(body, StandardCharsets.UTF_8);
        String requestId = extractRequestId(requestBody, request.getHeaders().getFirst("X-Request-Id"));
        log.info(
                "CKYC outbound request method={} uri={} requestId={} payload={}",
                request.getMethod(),
                request.getURI(),
                requestId,
                truncate(maskingUtil.maskSensitive(requestBody))
        );

        ClientHttpResponse response = execution.execute(request, body);
        InputStream responseStream = response.getBody();
        byte[] responseBytes = responseStream == null ? new byte[0] : responseStream.readAllBytes();
        String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
        log.info(
                "CKYC inbound response uri={} requestId={} status={} payload={}",
                request.getURI(),
                requestId,
                response.getStatusCode().value(),
                truncate(maskingUtil.maskSensitive(responseBody))
        );
        return new CachedBodyClientHttpResponse(response, responseBytes);
    }

    private String extractRequestId(String requestBody, String requestHeaderId) {
        if (requestHeaderId != null && !requestHeaderId.isBlank()) {
            return requestHeaderId;
        }
        Matcher matcher = REQUEST_ID_PATTERN.matcher(requestBody);
        return matcher.find() ? matcher.group(1) : "NA";
    }

    private String truncate(String payload) {
        if (payload == null) {
            return null;
        }
        int limit = Math.max(100, ckycProperties.getLogging().getMaxPayloadLength());
        if (payload.length() <= limit) {
            return payload;
        }
        return payload.substring(0, limit) + "...(truncated)";
    }
}
