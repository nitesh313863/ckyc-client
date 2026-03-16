package com.example.ckyc.serviceImpl;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.constant.ApplicationConstant;
import com.example.ckyc.dto.CkycApiResponsePayload;
import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.dto.CkycUpdateResponseDto;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.dto.CkycResponseDto;
import com.example.ckyc.exception.CkycEncryptionException;
import com.example.ckyc.exception.CkycException;
import com.example.ckyc.exception.CkycSignatureException;
import com.example.ckyc.exception.CkycUpstreamException;
import com.example.ckyc.exception.CkycValidationException;
import com.example.ckyc.model.ApiResponse;
import com.example.ckyc.service.CkycControllerService;
import com.example.ckyc.service.DownloadService;
import com.example.ckyc.service.UpdateService;
import com.example.ckyc.service.UploadService;
import com.example.ckyc.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CkycControllerServiceImpl implements CkycControllerService {

    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("<REQUEST_ID>([^<]+)</REQUEST_ID>");

    private final CkycService ckycService;
    private final CkycResponseService responseService;
    private final CkycApiClient ckycApiClient;
    private final CkycProperties ckycProperties;
    private final DownloadService downloadService;
    private final UploadService uploadService;
    private final UpdateService updateService;
    private final MaskingUtil maskingUtil;

    @Override
    public CkycApiResponsePayload search(String idType, String idNo) {
        log.info("Received CKYC search request for idType={}", idType);
        String xmlRequest = ckycService.prepareSearchRequest(idType, idNo);

        String requestId = extractRequestId(xmlRequest);
        String rawXmlResponse = ckycApiClient.postXml(ckycProperties.getUrl(), xmlRequest, requestId, "CKYC_SEARCH");
        if (rawXmlResponse == null || rawXmlResponse.isBlank()) {
            log.warn("CKYC upstream response body was empty");
            throw new CkycException("Received empty response from CKYC");
        }

        CkycResponseDto processed = responseService.processResponse(rawXmlResponse, requestId, "CKYC_SEARCH");

        return CkycApiResponsePayload.builder()
                .requestXml(xmlRequest)
                .rawResponseXml(rawXmlResponse)
                .response(processed)
                .build();
    }

    @Override
    public ApiResponse<CkycApiResponsePayload> download(CkycDownloadRequest request) {
        log.info(
                "Received CKYC download request ckycNo={} authFactorType={}",
                maskingUtil.maskSensitive(request.getCkycNo()),
                request.getAuthFactorType()
        );
        return downloadService.download(request);
    }

    @Override
    public ApiResponse<CkycApiResponsePayload> validateOtp(CkycValidateOtpRequest request) {
        log.info("Received CKYC validate-otp request ckycNo={}", maskingUtil.maskSensitive(request.getCkycNo()));
        return downloadService.validateOtp(request);
    }

    @Override
    public ApiResponse<CkycApiResponsePayload> upload(CkycUploadRequest request) {
        int imageCount = request.getImageDetails() == null ? 0 : request.getImageDetails().size();
        log.info("Received CKYC upload request identityCount={} addressCount={} imageCount={}",
                request.getIdentityDetails() == null ? 0 : request.getIdentityDetails().size(),
                request.getAddressDetails() == null ? 0 : request.getAddressDetails().size(),
                imageCount);
        return uploadService.upload(request);
    }

    @Override
    public ResponseEntity<CkycUpdateResponseDto> update(CkycUpdateRequestDto request, BindingResult bindingResult) {
        log.info(
                "Received CKYC update request ckycNo={} updateType={}",
                maskingUtil.maskSensitive(request.getCkycNo()),
                request.getUpdateType()
        );
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().isEmpty()
                    ? ApplicationConstant.Error.CKYC_VALIDATION_ERROR_MESSAGE
                    : bindingResult.getFieldErrors().get(0).getDefaultMessage();
            log.warn("CKYC update validation failed ckycNo={} message={}", maskingUtil.maskSensitive(request.getCkycNo()), message);
            return ResponseEntity.badRequest().body(errorResponse(
                    request,
                    "FAILED",
                    message,
                    ApplicationConstant.Error.CKYC_VALIDATION_ERROR_CODE
            ));
        }
        try {
            return ResponseEntity.ok(updateService.update(request));
        } catch (CkycValidationException ex) {
            log.warn(
                    "CKYC update business validation failed ckycNo={} message={}",
                    maskingUtil.maskSensitive(request.getCkycNo()),
                    ex.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse(
                    request,
                    "FAILED",
                    ex.getMessage(),
                    ApplicationConstant.Error.CKYC_VALIDATION_ERROR_CODE
            ));
        } catch (CkycEncryptionException ex) {
            log.error("CKYC update encryption error ckycNo={}", maskingUtil.maskSensitive(request.getCkycNo()), ex);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse(
                    request,
                    "FAILED",
                    ex.getMessage(),
                    ApplicationConstant.Error.CKYC_ENCRYPTION_ERROR_CODE
            ));
        } catch (CkycSignatureException ex) {
            log.error("CKYC update signature error ckycNo={}", maskingUtil.maskSensitive(request.getCkycNo()), ex);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse(
                    request,
                    "FAILED",
                    ex.getMessage(),
                    ApplicationConstant.Error.CKYC_SIGNATURE_ERROR_CODE
            ));
        } catch (CkycUpstreamException ex) {
            log.error(
                    "CKYC update upstream error ckycNo={} status={}",
                    maskingUtil.maskSensitive(request.getCkycNo()),
                    ex.getUpstreamStatusCode(),
                    ex
            );
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse(
                    request,
                    "FAILED",
                    ex.getMessage(),
                    ApplicationConstant.Error.CKYC_UPSTREAM_ERROR_CODE
            ));
        }
    }

    private CkycUpdateResponseDto errorResponse(
            CkycUpdateRequestDto request,
            String status,
            String message,
            String errorCode
    ) {
        return CkycUpdateResponseDto.builder()
                .status(status)
                .message(message)
                .ckycNo(request == null ? null : request.getCkycNo())
                .updateType(request == null ? null : request.getUpdateType())
                .errorCode(errorCode)
                .build();
    }

    private String extractRequestId(String xml) {
        Matcher matcher = REQUEST_ID_PATTERN.matcher(xml);
        return matcher.find() ? matcher.group(1) : "NA";
    }
}
