package com.platform.module.sms.provider;

import java.util.Map;

public interface SmsProvider {

    String getProviderName();

    SmsDeliveryResult sendSms(String recipientPhone, String messageContent, Map<String, Object> metadata);

    record SmsDeliveryResult(boolean success, String providerMessageId, String errorCode, String errorMessage) {}
}
