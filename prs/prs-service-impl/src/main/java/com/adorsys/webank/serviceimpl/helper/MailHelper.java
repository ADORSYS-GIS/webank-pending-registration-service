package com.adorsys.webank.serviceimpl.helper;

import com.adorsys.error.FailedToSendOTPException;
import com.adorsys.webank.properties.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Helper class for sending emails.
 * Handles the email sending logic for OTP and other notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailHelper {
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    /**
     * Sends an OTP email to the specified recipient.
     *
     * @param toEmail The recipient's email address
     * @param otp The OTP to send
     * @throws FailedToSendOTPException if there's an error sending the email
     */
    public void sendOtpEmail(String toEmail, String otp) {
        String correlationId = MDC.get("correlationId");
        log.info("Sending Email OTP to: {} [correlationId={}]", maskEmail(toEmail), correlationId);
        
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(mailProperties.getUsername());
            helper.setTo(toEmail);
            helper.setSubject("Webank Verification Code");
            helper.setText(String.format("Your Webank OTP is: %s (valid for 5 minutes)", otp));

            mailSender.send(message);
            log.info("Email OTP sent successfully to: {} [correlationId={}]", 
                    maskEmail(toEmail), correlationId);
        } catch (MessagingException e) {
            log.error("Failed to send Email OTP to: {} [correlationId={}]", 
                    maskEmail(toEmail), correlationId, e);
            throw new FailedToSendOTPException("Failed to send Webank email: " + e.getMessage());
        }
    }

    /**
     * Masks an email address for logging purposes.
     * Shows only first character and domain.
     */
    public String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "********";
        }
        if (email.contains("@")) {
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                String firstChar = email.substring(0, 1);
                String domain = email.substring(atIndex);
                return firstChar + "****" + domain;
            }
        }
        return email.charAt(0) + "********";
    }

    /**
     * Masks an account ID for logging purposes
     * Shows only first 2 and last 2 characters
     */
    public String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 5) {
            return "********";
        }
        return accountId.substring(0, 2) + "****" + accountId.substring(accountId.length() - 2);
    }
}
