package com.rohan.service.imp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sendinblue.ApiClient;
import sendinblue.ApiException;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sendinblue.api.TransactionalEmailsApi;
import sendinblue.model.SendSmtpEmail;
import sendinblue.model.SendSmtpEmailSender;
import sendinblue.model.SendSmtpEmailTo;
import sendinblue.model.SendSmtpEmailReplyTo;

import java.util.Collections;

@Service
@Slf4j
public class BrevoEmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    private TransactionalEmailsApi getTransactionalEmailsApi() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        
        // Configure API key authorization: api-key
        ApiKeyAuth apiKey = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
        apiKey.setApiKey(brevoApiKey);
        
        return new TransactionalEmailsApi(defaultClient);
    }

    /**
     * Send OTP email using Brevo API
     * @param toEmail Recipient email address
     * @param otp One-Time Password to send
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendOtpEmail(String toEmail, String otp) {
        try {
            log.info("📧 Sending OTP email via Brevo API to: {}", toEmail);
            
            TransactionalEmailsApi apiInstance = getTransactionalEmailsApi();
            
            // Create sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setName("SastaaBazaar");
            sender.setEmail("noreply@sastabazaar.com");
            
            // Create recipient
            SendSmtpEmailTo to = new SendSmtpEmailTo();
            to.setEmail(toEmail);
            
            // Create reply-to
            SendSmtpEmailReplyTo replyTo = new SendSmtpEmailReplyTo();
            replyTo.setEmail("noreply@sastabazaar.com");
            
            // Build email content
            String emailContent = buildEmailContent(otp);
            
            // Create email
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            sendSmtpEmail.setSender(sender);
            sendSmtpEmail.setTo(Collections.singletonList(to));
            sendSmtpEmail.setReplyTo(replyTo);
            sendSmtpEmail.setSubject("SastaaBazaar OTP Verification");
            sendSmtpEmail.setHtmlContent(emailContent);
            
            // Send email
            apiInstance.sendTransacEmail(sendSmtpEmail);
            
            log.info("✅ OTP email sent successfully via Brevo to: {}", toEmail);
            return true;
            
        } catch (ApiException e) {
            log.error("❌ Brevo API Error - Code: {}, Body: {}", e.getCode(), e.getResponseBody(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ Failed to send OTP email via Brevo to: {} - Error: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Build email content with OTP
     */
    private String buildEmailContent(String otp) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>" +
               "<body style='margin:0;padding:0;font-family:Arial,Helvetica,sans-serif;background-color:#f4f4f7;'>" +
               
               // Main container
               "<div style='max-width:520px;margin:30px auto;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,0.1);'>" +
               
               // Header
               "<div style='background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:36px 24px;text-align:center;'>" +
               "<h1 style='color:#ffffff;margin:0;font-size:30px;font-weight:800;letter-spacing:1px;'>🛍️ SastaaBazaar</h1>" +
               "<p style='color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:13px;letter-spacing:0.5px;'>Your Smart Shopping Companion</p>" +
               "</div>" +
               
               // OTP Section
               "<div style='text-align:center;padding:36px 24px 20px;'>" +
               "<div style='width:64px;height:64px;background:linear-gradient(135deg,#e8f5e9,#c8e6c9);border-radius:50%;margin:0 auto 20px;line-height:64px;font-size:28px;'>🔐</div>" +
               "<h2 style='color:#1a1a2e;margin:0 0 8px;font-size:22px;font-weight:700;'>Verification Code</h2>" +
               "<p style='color:#666;margin:0 0 28px;font-size:14px;line-height:1.5;'>Aapka One-Time Password (OTP) neeche diya gaya hai.<br/>Ise kisi ke saath share na karein.</p>" +
               
               // OTP Display
               "<div style='display:inline-block;background:#f8f9fa;border:2px dashed #667eea;border-radius:12px;padding:18px 28px;'>" +
               "<span style='font-size:36px;font-weight:800;color:#1a1a2e;letter-spacing:14px;font-family:monospace;'>" +
               otp +
               "</span>" +
               "</div>" +
               
               "<p style='color:#999;margin:20px 0 0;font-size:12px;'>⏱️ Yeh OTP <strong>5 minutes</strong> ke liye valid hai</p>" +
               "</div>" +
               
               // Security Tips
               "<div style='padding:0 24px 24px;'>" +
               "<table style='width:100%;background:linear-gradient(135deg,#fff3e0,#fce4ec);border-radius:10px;border:1px solid #ffe0b2;' cellpadding='16' cellspacing='0'><tr><td>" +
               "<p style='margin:0 0 10px;font-size:13px;font-weight:700;color:#e65100;'>⚠️ Security Tips</p>" +
               "<p style='margin:3px 0;font-size:12px;color:#555;'>🔒 Yeh OTP sirf aapke liye hai, kisi ko na batayein</p>" +
               "<p style='margin:3px 0;font-size:12px;color:#555;'>📞 SastaaBazaar kabhi phone par OTP nahi maangta</p>" +
               "<p style='margin:3px 0;font-size:12px;color:#555;'>🚫 Agar aapne yeh request nahi ki, toh ignore karein</p>" +
               "</td></tr></table>" +
               "</div>" +
               
               // Footer
               "<div style='background:#f8f9fa;padding:20px 24px;text-align:center;border-top:1px solid #eee;'>" +
               "<p style='margin:0 0 6px;font-size:13px;color:#888;'>Thank you for choosing <strong>SastaaBazaar</strong>! 🛒</p>" +
               "<p style='margin:0 0 6px;font-size:11px;color:#bbb;'>Smart shopping, sasta prices — yahi hai hamara vaada</p>" +
               "<p style='margin:0;font-size:10px;color:#ccc;'>This is an automated email. Please do not reply.</p>" +
               "</div>" +
               
               "</div></body></html>";
    }
}
