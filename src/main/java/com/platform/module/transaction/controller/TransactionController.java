package com.platform.module.transaction.controller;

import com.platform.common.exception.PlatformException;
import com.platform.common.response.ApiResponse;
import com.platform.module.error.entity.OutMessageError;
import com.platform.module.error.repository.OutMessageErrorRepository;
import com.platform.module.transaction.dto.TransactionStatusResponse;
import com.platform.module.transaction.entity.OutMessage;
import com.platform.module.transaction.repository.OutMessageRepository;
import com.platform.security.ClientPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Tracking", description = "Central transaction tracking and status verification APIs")
public class TransactionController {

    private final OutMessageRepository outMessageRepository;
    private final OutMessageErrorRepository errorRepository;

    @GetMapping("/{refNo}")
    @Operation(summary = "Get master transaction summary by REF_NO")
    public ResponseEntity<ApiResponse<TransactionStatusResponse>> getTransaction(
            @PathVariable String refNo,
            @AuthenticationPrincipal ClientPrincipal principal) {

        // Strict Client Isolation: authenticated client can only view their own transactions
        OutMessage msg = outMessageRepository.findByRefNoAndClientId(refNo, principal.getClientId())
                .orElseThrow(() -> new PlatformException("Transaction not found for REF_NO: " + refNo, HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", refNo));

        Map<String, String> links = new HashMap<>();
        links.put("sms", "/api/v1/transactions/" + refNo + "/sms");
        links.put("emails", "/api/v1/transactions/" + refNo + "/emails");
        links.put("documents", "/api/v1/transactions/" + refNo + "/documents");
        links.put("reports", "/api/v1/transactions/" + refNo + "/reports");
        links.put("errors", "/api/v1/transactions/" + refNo + "/errors");

        TransactionStatusResponse responseData = TransactionStatusResponse.builder()
                .refNo(msg.getRefNo())
                .clientId(msg.getClientId())
                .requestType(msg.getRequestType())
                .processingMode(msg.getProcessingMode())
                .overallStatus(msg.getOverallStatus())
                .completionOutcome(msg.getCompletionOutcome())
                .serviceCounts(TransactionStatusResponse.ServiceCounts.builder()
                        .total(msg.getTotalServiceCount())
                        .sms(msg.getSmsCount())
                        .email(msg.getEmailCount())
                        .pdf(msg.getPdfCount())
                        .excel(msg.getExcelCount())
                        .csv(msg.getCsvCount())
                        .report(msg.getReportCount())
                        .build())
                .metrics(TransactionStatusResponse.ProcessingMetrics.builder()
                        .success(msg.getSuccessCount())
                        .failed(msg.getFailedCount())
                        .inProgress(msg.getInProgressCount())
                        .completed(msg.getCompletedCount())
                        .expired(msg.getExpiredCount())
                        .paused(msg.getPausedCount())
                        .build())
                .timestamps(TransactionStatusResponse.TemporalMilestones.builder()
                        .createdAt(msg.getCreatedAt())
                        .startedAt(msg.getStartedAt())
                        .completedAt(msg.getCompletedAt())
                        .lastUpdatedAt(msg.getLastUpdatedAt())
                        .expiresAt(msg.getExpiresAt())
                        .retentionUntil(msg.getRetentionUntil())
                        .build())
                .links(links)
                .build();

        return ResponseEntity.ok(ApiResponse.success(responseData, "Transaction retrieved successfully", refNo));
    }

    @GetMapping("/{refNo}/errors")
    @Operation(summary = "Get transaction error breakdown by REF_NO")
    public ResponseEntity<ApiResponse<List<OutMessageError>>> getTransactionErrors(
            @PathVariable String refNo,
            @AuthenticationPrincipal ClientPrincipal principal) {

        // Enforce ownership
        outMessageRepository.findByRefNoAndClientId(refNo, principal.getClientId())
                .orElseThrow(() -> new PlatformException("Transaction not found", HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", refNo));

        List<OutMessageError> errors = errorRepository.findByRefNo(refNo);
        return ResponseEntity.ok(ApiResponse.success(errors, "Errors retrieved successfully", refNo));
    }
}
