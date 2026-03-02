package com.example.ckyc.serviceImpl;

import com.example.ckyc.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final MaskingUtil maskingUtil;

    public void record(String action, String requestId, String status, String details) {
        log.info(
                "AUDIT action={} requestId={} status={} details={}",
                action,
                requestId,
                status,
                maskingUtil.maskSensitive(details)
        );
    }
}
