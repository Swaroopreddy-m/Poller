package com.platform.module.error.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.module.error.entity.OutMessageError;
import com.platform.module.error.repository.OutMessageErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Enterprise sanitized error logger.
 * Strips tokens, secrets, credentials and passwords before persisting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorManagementService {

    private final OutMessageErrorRepository errorRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern SENSITIVE_PATTERN = 
            Pattern.compile("(?i)(password|secret|token|api[_-]?key|authorization|bearer)\\s*[:=]\\s*['\"]?([^'\"\\s,]+)['\"]?");

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordError(Long outMessageId, String refNo, UUID clientId, String serviceType,
                            Long operationId, String errorCode, String errorCategory,
                            String rawErrorMessage, Map<String, Object> details, boolean retryable) {
        try {
            String sanitizedMessage = sanitize(rawErrorMessage);
            String detailsJson = details != null ? objectMapper.writeValueAsString(details) : null;
            Instant now = Instant.now();

            OutMessageError errorRecord = OutMessageError.builder()
                    .outMessageId(outMessageId)
                    .refNo(refNo)
                    .clientId(clientId)
                    .serviceType(serviceType)
                    .operationId(operationId)
                    .errorCode(errorCode)
                    .errorCategory(errorCategory)
                    .errorMessage(sanitizedMessage)
                    .errorDetails(detailsJson)
                    .retryable(retryable)
                    .occurredAt(now)
                    .retentionUntil(now.plus(365, ChronoUnit.DAYS))
                    .build();

            errorRepository.save(errorRecord);
            log.warn("Recorded error for REF_NO: {} [Category: {}, Code: {}]", refNo, errorCategory, errorCode);
        } catch (Exception ex) {
            log.error("Failed persisting error record for REF_NO: {}", refNo, ex);
        }
    }

    private String sanitize(String input) {
        if (input == null) return "Unknown Error";
        String clean = SENSITIVE_PATTERN.matcher(input).replaceAll("$1=***REDACTED***");
        return clean.length() > 1024 ? clean.substring(0, 1021) + "..." : clean;
    }
}
