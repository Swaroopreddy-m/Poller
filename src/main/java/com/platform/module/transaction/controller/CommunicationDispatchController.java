package com.platform.module.transaction.controller;

import com.platform.common.response.ApiResponse;
import com.platform.module.transaction.dto.CommunicationDispatchRequest;
import com.platform.module.transaction.dto.TransactionStatusResponse;
import com.platform.module.transaction.service.TransactionDispatchService;
import com.platform.security.ClientPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/communications")
@RequiredArgsConstructor
@Tag(name = "Communication Dispatch", description = "Composite communication dispatching (Email, SMS, Documents)")
public class CommunicationDispatchController {

    private final TransactionDispatchService dispatchService;

    @PostMapping("/dispatch")
    @Operation(summary = "Dispatch composite communication request")
    public ResponseEntity<ApiResponse<TransactionStatusResponse>> dispatch(
            @Valid @RequestBody CommunicationDispatchRequest request,
            @RequestHeader(value = "X-Processing-Mode", defaultValue = "SYNC") String processingMode,
            @AuthenticationPrincipal ClientPrincipal principal) {

        TransactionStatusResponse response = dispatchService.dispatchTransaction(request, principal, processingMode);
        return ResponseEntity.ok(ApiResponse.success(response, "Communication transaction staged and dispatched successfully", response.getRefNo()));
    }
}
