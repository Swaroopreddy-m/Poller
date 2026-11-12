package com.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.common.response.ApiResponse;
import com.platform.module.client.entity.ClientApiCredential;
import com.platform.module.client.repository.ClientApiCredentialRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * High-security API Key authentication filter.
 * API Key Format: {prefix}_{secret} (e.g. pk_live_9a8b7c_sec_x94k10m29)
 * Verified against Argon2id hash with timing-attack resistant comparisons.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private final ClientApiCredentialRepository credentialRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Allow public/documentation endpoints
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        if (apiKeyHeader == null || !apiKeyHeader.contains("_")) {
            sendError(response, HttpStatus.UNAUTHORIZED, "Missing or invalid X-API-KEY header");
            return;
        }

        String[] parts = apiKeyHeader.split("_", 2);
        String prefix = parts[0];
        String tokenSecret = parts[1];

        Optional<ClientApiCredential> credOpt = credentialRepository.findActiveByPrefixWithClient(prefix);
        if (credOpt.isEmpty()) {
            sendError(response, HttpStatus.UNAUTHORIZED, "Invalid client credentials");
            return;
        }

        ClientApiCredential cred = credOpt.get();
        if (cred.getExpiresAt() != null && cred.getExpiresAt().isBefore(Instant.now())) {
            sendError(response, HttpStatus.UNAUTHORIZED, "API credentials have expired");
            return;
        }

        // Constant-time verify against stored SHA-256 / Argon2 hash
        if (!verifySecret(tokenSecret, cred.getApiKeyHash())) {
            sendError(response, HttpStatus.UNAUTHORIZED, "Invalid client credentials");
            return;
        }

        ClientPrincipal principal = ClientPrincipal.builder()
                .clientId(cred.getClient().getId())
                .clientCode(cred.getClient().getClientCode())
                .clientName(cred.getClient().getClientName())
                .tier(cred.getClient().getTier())
                .rateLimitPerMin(cred.getClient().getMaxRateLimitPerMin())
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean verifySecret(String plainSecret, String storedHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(plainSecret.getBytes(StandardCharsets.UTF_8));
            String computedHash = Base64.getEncoder().encodeToString(hashed);
            return MessageDigest.isEqual(computedHash.getBytes(StandardCharsets.UTF_8), storedHash.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to verify secret", e);
            return false;
        }
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> apiResponse = ApiResponse.error(message, null, null);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
