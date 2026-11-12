package com.platform.module.email.provider;

import java.util.Map;

public interface EmailProvider {

    String getProviderName();

    EmailDeliveryResult sendEmail(String recipient, String cc, String bcc, String subject, String body, boolean isHtml, Map<String, Object> metadata);

    record EmailDeliveryResult(boolean success, String providerMessageId, String errorCode, String errorMessage) {}
}
