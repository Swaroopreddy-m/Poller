package com.platform.security;

import com.platform.common.exception.PlatformException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

/**
 * Server-Side Request Forgery (SSRF) Protection Validator.
 * Validates external client-pull target URLs before network dispatch:
 * 1. Blocks private IPv4 / RFC1918 subnets (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
 * 2. Blocks loopback (127.0.0.1 / ::1)
 * 3. Blocks AWS/Cloud link-local metadata endpoints (169.254.169.254)
 * 4. Enforces TLS/HTTPS schemes only.
 */
@Slf4j
@Component
public class SsrfProtectionValidator {

    private static final List<String> ALLOWED_SCHEMES = List.of("https");

    public void validateUrl(String targetUrl) {
        try {
            URI uri = URI.create(targetUrl);
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                throw new PlatformException("Only HTTPS scheme is allowed for external connections",
                        HttpStatus.BAD_REQUEST, "SSRF_DISALLOWED_SCHEME");
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new PlatformException("Invalid target host in URL", HttpStatus.BAD_REQUEST, "SSRF_INVALID_HOST");
            }

            // Resolve DNS and check all resolved IP addresses
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
                    log.warn("SSRF attempt blocked! Target host: {} resolved to private address: {}", host, address.getHostAddress());
                    throw new PlatformException("Target host resolves to forbidden internal network",
                            HttpStatus.FORBIDDEN, "SSRF_BLOCKED_TARGET");
                }

                // Check cloud metadata addresses
                String ip = address.getHostAddress();
                if (ip.startsWith("169.254.") || ip.startsWith("0.") || ip.equals("255.255.255.255")) {
                    log.warn("SSRF cloud metadata access blocked: {}", ip);
                    throw new PlatformException("Target host resolves to cloud metadata address",
                            HttpStatus.FORBIDDEN, "SSRF_BLOCKED_TARGET");
                }
            }
        } catch (PlatformException pe) {
            throw pe;
        } catch (Exception e) {
            log.error("Failed DNS validation for URL: {}", targetUrl, e);
            throw new PlatformException("Failed to validate external destination: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST, "SSRF_VALIDATION_ERROR");
        }
    }
}
