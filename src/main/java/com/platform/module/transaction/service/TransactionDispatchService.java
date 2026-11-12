package com.platform.module.transaction.service;

import com.platform.common.id.TsidGenerator;
import com.platform.module.document.entity.OutMessageDocument;
import com.platform.module.document.repository.OutMessageDocumentRepository;
import com.platform.module.email.entity.OutMessageEmail;
import com.platform.module.email.repository.OutMessageEmailRepository;
import com.platform.module.sms.entity.OutMessageSms;
import com.platform.module.sms.repository.OutMessageSmsRepository;
import com.platform.module.transaction.dto.CommunicationDispatchRequest;
import com.platform.module.transaction.dto.TransactionStatusResponse;
import com.platform.module.transaction.entity.OutMessage;
import com.platform.module.transaction.repository.OutMessageRepository;
import com.platform.security.ClientPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionDispatchService {

    private final OutMessageRepository outMessageRepository;
    private final OutMessageEmailRepository emailRepository;
    private final OutMessageSmsRepository smsRepository;
    private final OutMessageDocumentRepository documentRepository;
    private final TsidGenerator tsidGenerator;

    @Transactional
    public TransactionStatusResponse dispatchTransaction(CommunicationDispatchRequest request, ClientPrincipal principal, String processingMode) {
        String refNo = tsidGenerator.generateRefNo();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(request.getTtlSeconds(), ChronoUnit.SECONDS);
        Instant retentionUntil = now.plus(365, ChronoUnit.DAYS);

        int emailCount = request.getOperations() != null && request.getOperations().getEmail() != null ? request.getOperations().getEmail().size() : 0;
        int smsCount = request.getOperations() != null && request.getOperations().getSms() != null ? request.getOperations().getSms().size() : 0;
        int docCount = request.getOperations() != null && request.getOperations().getDocuments() != null ? request.getOperations().getDocuments().size() : 0;
        int totalServices = emailCount + smsCount + docCount;

        // Persist Central OUT_MESSAGE
        OutMessage outMessage = OutMessage.builder()
                .createdAt(now)
                .refNo(refNo)
                .clientId(principal.getClientId())
                .requestType(request.getRequestType())
                .requestSource("REST_API")
                .idempotencyKey(request.getIdempotencyKey())
                .processingMode(processingMode)
                .priority(request.getPriority())
                .emailCount(emailCount)
                .smsCount(smsCount)
                .pdfCount(docCount)
                .totalServiceCount(totalServices)
                .overallStatus("IP")
                .startedAt(now)
                .expiresAt(expiresAt)
                .retentionUntil(retentionUntil)
                .build();

        outMessage = outMessageRepository.save(outMessage);

        // Stage Child Operations
        if (request.getOperations() != null) {
            if (request.getOperations().getEmail() != null) {
                for (var emailOp : request.getOperations().getEmail()) {
                    OutMessageEmail email = OutMessageEmail.builder()
                            .createdAt(now)
                            .outMessageId(outMessage.getId())
                            .refNo(refNo)
                            .clientId(principal.getClientId())
                            .recipientEmail(emailOp.getRecipient())
                            .ccList(emailOp.getCc())
                            .bccList(emailOp.getBcc())
                            .subject(emailOp.getSubject() != null ? emailOp.getSubject() : "Notification")
                            .renderedBody(emailOp.getContent() != null ? emailOp.getContent() : "Content")
                            .status("IP")
                            .retentionUntil(retentionUntil)
                            .build();
                    emailRepository.save(email);
                }
            }

            if (request.getOperations().getSms() != null) {
                for (var smsOp : request.getOperations().getSms()) {
                    OutMessageSms sms = OutMessageSms.builder()
                            .createdAt(now)
                            .outMessageId(outMessage.getId())
                            .refNo(refNo)
                            .clientId(principal.getClientId())
                            .recipientPhone(smsOp.getRecipientPhone())
                            .renderedContent(smsOp.getContent() != null ? smsOp.getContent() : "Alert")
                            .status("IP")
                            .retentionUntil(retentionUntil)
                            .build();
                    smsRepository.save(sms);
                }
            }

            if (request.getOperations().getDocuments() != null) {
                for (var docOp : request.getOperations().getDocuments()) {
                    OutMessageDocument doc = OutMessageDocument.builder()
                            .outMessageId(outMessage.getId())
                            .refNo(refNo)
                            .clientId(principal.getClientId())
                            .documentType(docOp.getDocumentType() != null ? docOp.getDocumentType() : "PDF")
                            .fileName(docOp.getFileName() != null ? docOp.getFileName() : "Document.pdf")
                            .storageBucket("platform-documents")
                            .storagePath("docs/" + refNo + "/" + (docOp.getFileName() != null ? docOp.getFileName() : "doc.pdf"))
                            .mimeType("application/pdf")
                            .status("IP")
                            .retentionUntil(retentionUntil)
                            .build();
                    documentRepository.save(doc);
                }
            }
        }

        // For Synchronous mode mock/dispatch execution
        if ("SYNC".equalsIgnoreCase(processingMode)) {
            // Mark operations executed successfully in synchronous pipeline
            outMessage.setSuccessCount(totalServices);
            outMessage.setCompletedCount(totalServices);
            outMessage.setInProgressCount(0);
            outMessage.setOverallStatus("C");
            outMessage.setCompletionOutcome("FULL_SUCCESS");
            outMessage.setCompletedAt(Instant.now());
            outMessageRepository.save(outMessage);
        }

        return TransactionStatusResponse.builder()
                .refNo(refNo)
                .clientId(principal.getClientId())
                .requestType(outMessage.getRequestType())
                .processingMode(outMessage.getProcessingMode())
                .overallStatus(outMessage.getOverallStatus())
                .completionOutcome(outMessage.getCompletionOutcome())
                .serviceCounts(TransactionStatusResponse.ServiceCounts.builder()
                        .total(outMessage.getTotalServiceCount())
                        .email(outMessage.getEmailCount())
                        .sms(outMessage.getSmsCount())
                        .pdf(outMessage.getPdfCount())
                        .build())
                .metrics(TransactionStatusResponse.ProcessingMetrics.builder()
                        .success(outMessage.getSuccessCount())
                        .failed(outMessage.getFailedCount())
                        .inProgress(outMessage.getInProgressCount())
                        .completed(outMessage.getCompletedCount())
                        .expired(outMessage.getExpiredCount())
                        .paused(outMessage.getPausedCount())
                        .build())
                .timestamps(TransactionStatusResponse.TemporalMilestones.builder()
                        .createdAt(outMessage.getCreatedAt())
                        .startedAt(outMessage.getStartedAt())
                        .completedAt(outMessage.getCompletedAt())
                        .lastUpdatedAt(outMessage.getLastUpdatedAt())
                        .expiresAt(outMessage.getExpiresAt())
                        .retentionUntil(outMessage.getRetentionUntil())
                        .build())
                .build();
    }
}
