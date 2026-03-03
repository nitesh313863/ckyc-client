package com.example.ckyc.serviceImpl;

import com.example.ckyc.exception.CkycUpstreamException;
import com.example.ckyc.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class CkycApiClient {

    private final RestTemplate restTemplate;
    private final RetryTemplate ckycRetryTemplate;
    private final AuditService auditService;
    private final MaskingUtil maskingUtil;

    public String postXml(String url, String xmlRequest, String requestId, String operation) {
        return ckycRetryTemplate.execute(context -> {
            long startNanos = System.nanoTime();
            int attempt = context.getRetryCount() + 1;
            log.info("CKYC upstream call start operation={} requestId={} attempt={} url={}", operation, requestId, attempt, url);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_XML);
                headers.set("X-Request-Id", requestId);
                HttpEntity<String> requestEntity = new HttpEntity<>(xmlRequest, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                if (response.getBody() == null || response.getBody().isBlank()) {
                    throw new CkycUpstreamException("Received empty response from CKYC", 502, false, null);
                }
                long elapsedMs = elapsedMs(startNanos);
                log.info(
                        "CKYC upstream call success operation={} requestId={} attempt={} status={} elapsedMs={}",
                        operation,
                        requestId,
                        attempt,
                        response.getStatusCode().value(),
                        elapsedMs
                );
                auditService.record(operation, requestId, "UPSTREAM_SUCCESS", "status=" + response.getStatusCode().value());
                return response.getBody();
            } catch (RestClientResponseException ex) {
                boolean retryable = ex.getRawStatusCode() >= 500;
                long elapsedMs = elapsedMs(startNanos);
                log.error(
                        "CKYC upstream response exception operation={} requestId={} attempt={} status={} retryable={} elapsedMs={} response={}",
                        operation,
                        requestId,
                        attempt,
                        ex.getRawStatusCode(),
                        retryable,
                        elapsedMs,
                        truncate(maskingUtil.maskSensitive(ex.getResponseBodyAsString()))
                );
                throw new CkycUpstreamException(
                        "CKYC upstream returned error status",
                        ex,
                        ex.getRawStatusCode(),
                        retryable,
                        ex.getResponseBodyAsString()
                );
            } catch (ResourceAccessException ex) {
                long elapsedMs = elapsedMs(startNanos);
                log.error(
                        "CKYC upstream connectivity failure operation={} requestId={} attempt={} elapsedMs={}",
                        operation,
                        requestId,
                        attempt,
                        elapsedMs,
                        ex
                );
                throw new CkycUpstreamException("Unable to connect CKYC endpoint", ex, 504, true, null);
            }
        }, context -> {
            Throwable last = context.getLastThrowable();
            if (last instanceof CkycUpstreamException upstreamException) {
                auditService.record(operation, requestId, "UPSTREAM_FAILED", "status=" + upstreamException.getUpstreamStatusCode());
                log.error(
                        "CKYC upstream call failed after retries operation={} requestId={} attempts={} status={} retryable={}",
                        operation,
                        requestId,
                        context.getRetryCount() + 1,
                        upstreamException.getUpstreamStatusCode(),
                        upstreamException.isRetryable()
                );
                throw upstreamException;
            }
            log.error(
                    "CKYC upstream call failed with unexpected exception operation={} requestId={} attempts={}",
                    operation,
                    requestId,
                    context.getRetryCount() + 1,
                    last
            );
            throw new CkycUpstreamException("CKYC call failed", last, 502, false, null);
        });
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int maxLen = 500;
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...(truncated)";
    }
}
