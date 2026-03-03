package com.example.ckyc.controller;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.exception.CkycException;
import com.example.ckyc.model.ApiResponse;
import com.example.ckyc.serviceImpl.CkycApiClient;
import com.example.ckyc.serviceImpl.CkycResponseService;
import com.example.ckyc.serviceImpl.CkycService;
import com.example.ckyc.service.DownloadService;
import com.example.ckyc.service.UploadService;
import com.example.ckyc.util.MaskingUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/ckyc")
@Slf4j
@RequiredArgsConstructor
public class CkycController {

    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("<REQUEST_ID>([^<]+)</REQUEST_ID>");

    private final CkycService service;
    private final CkycResponseService responseService;
    private final CkycApiClient ckycApiClient;
    private final CkycProperties ckycProperties;
    private final DownloadService downloadService;
    private final UploadService uploadService;
    private final MaskingUtil maskingUtil;

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam String idType,
                                                      @RequestParam String idNo) {
        log.info("Received CKYC search request for idType={}", idType);
        String xmlRequest = service.prepareSearchRequest(idType, idNo);

        String requestId = extractRequestId(xmlRequest);
        String rawXmlResponse = ckycApiClient.postXml(ckycProperties.getUrl(), xmlRequest, requestId, "CKYC_SEARCH");
        if (rawXmlResponse == null || rawXmlResponse.isBlank()) {
            log.warn("CKYC upstream response body was empty");
            throw new CkycException("Received empty response from CKYC");
        }

        Map<String, Object> processed = responseService.processResponse(rawXmlResponse, requestId, "CKYC_SEARCH");

        Map<String, Object> apiResponse = new LinkedHashMap<>();
        apiResponse.put("requestId", requestId);
        apiResponse.put("requestXml", xmlRequest);
        apiResponse.put("rawResponseXml", rawXmlResponse);
        apiResponse.putAll(processed);

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/download")
    public ResponseEntity<ApiResponse<Map<String, Object>>> download(@Valid @RequestBody CkycDownloadRequest request) {
        log.info(
                "Received CKYC download request ckycNo={} authFactorType={}",
                maskingUtil.maskSensitive(request.getCkycNo()),
                request.getAuthFactorType()
        );
        return ResponseEntity.ok(downloadService.download(request));
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateOtp(
            @Valid @RequestBody CkycValidateOtpRequest request
    ) {
        log.info("Received CKYC validate-otp request ckycNo={}", maskingUtil.maskSensitive(request.getCkycNo()));
        return ResponseEntity.ok(downloadService.validateOtp(request));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(@Valid @RequestBody CkycUploadRequest request) {
        int imageCount = request.getImageDetails() == null ? 0 : request.getImageDetails().size();
        log.info("Received CKYC upload request identityCount={} addressCount={} imageCount={}",
                request.getIdentityDetails() == null ? 0 : request.getIdentityDetails().size(),
                request.getAddressDetails() == null ? 0 : request.getAddressDetails().size(),
                imageCount);
        return ResponseEntity.ok(uploadService.upload(request));
    }

    private String extractRequestId(String xml) {
        Matcher matcher = REQUEST_ID_PATTERN.matcher(xml);
        return matcher.find() ? matcher.group(1) : "NA";
    }
}
