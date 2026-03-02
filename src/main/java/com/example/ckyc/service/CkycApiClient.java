package com.example.ckyc.service;

import com.example.ckyc.exception.CkycUpstreamException;
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

    public String postXml(String url, String xmlRequest, String requestId, String operation) {
        return ckycRetryTemplate.execute(context -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_XML);
                headers.set("X-Request-Id", requestId);
                HttpEntity<String> requestEntity = new HttpEntity<>(xmlRequest, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                if (response.getBody() == null || response.getBody().isBlank()) {
                    throw new CkycUpstreamException("Received empty response from CKYC", 502, false, null);
                }
                auditService.record(operation, requestId, "UPSTREAM_SUCCESS", "status=" + response.getStatusCode().value());
                return response.getBody();
            } catch (RestClientResponseException ex) {
                boolean retryable = ex.getRawStatusCode() >= 500;
                log.error(
                        "CKYC upstream response exception operation={} requestId={} status={} retryable={}",
                        operation, requestId, ex.getRawStatusCode(), retryable
                );
                throw new CkycUpstreamException(
                        "CKYC upstream returned error status",
                        ex,
                        ex.getRawStatusCode(),
                        retryable,
                        ex.getResponseBodyAsString()
                );
            } catch (ResourceAccessException ex) {
                log.error("CKYC upstream connectivity failure operation={} requestId={}", operation, requestId, ex);
                throw new CkycUpstreamException("Unable to connect CKYC endpoint", ex, 504, true, null);
            }
        }, context -> {
            Throwable last = context.getLastThrowable();
            if (last instanceof CkycUpstreamException upstreamException) {
                auditService.record(operation, requestId, "UPSTREAM_FAILED", "status=" + upstreamException.getUpstreamStatusCode());
                throw upstreamException;
            }
            throw new CkycUpstreamException("CKYC call failed", last, 502, false, null);
        });
    }
}
