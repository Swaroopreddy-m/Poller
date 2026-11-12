package com.platform.module.email.provider;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:contact@zenvoc.com}")
    private String fromAddress;

    @Override
    public String getProviderName() {
        return "GMAIL_SMTP";
    }

    @Override
    public EmailDeliveryResult sendEmail(
            String recipient,
            String cc,
            String bcc,
            String subject,
            String body,
            boolean isHtml,
            Map<String, Object> metadata) {

        log.info("[EMAIL] Sending via Gmail SMTP | to={} | subject={}", recipient, subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(recipient);

            if (cc != null && !cc.isBlank()) {
                helper.setCc(cc.split(","));
            }
            if (bcc != null && !bcc.isBlank()) {
                helper.setBcc(bcc.split(","));
            }

            helper.setSubject(subject != null ? subject : "(no subject)");
            helper.setText(body != null ? body : "", isHtml);

            mailSender.send(message);

            String messageId = message.getMessageID();
            log.info("[EMAIL] Sent successfully | to={} | messageId={}", recipient, messageId);
            return new EmailDeliveryResult(true, messageId, null, null);

        } catch (Exception ex) {
            log.error("[EMAIL] Send failed | to={} | error={}", recipient, ex.getMessage(), ex);
            return new EmailDeliveryResult(false, null, "SMTP_ERROR", ex.getMessage());
        }
    }
}
