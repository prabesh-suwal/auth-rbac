package com.sb.authenticationrbac.permissionsync.notification;

import com.sb.authenticationrbac.permissionsync.validation.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PermissionSyncNotifier {
    
    private final JavaMailSender mailSender;
    
    @Value("${permission.sync.notification.emails}")
    private List<String> notificationEmails;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    public PermissionSyncNotifier(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void notifySuccess(SyncResult result) {
        String subject = "Permission Sync Completed Successfully";
        String body = String.format("""
            Permission synchronization completed successfully.
            
            Summary:
            - Added: %d permissions
            - Updated: %d permissions
            - Total: %d permissions
            
            Time taken: %d ms
            """,
            result.getAdded(),
            result.getUpdated(),
            result.getTotal(),
            result.getTimeTaken()
        );
        
        sendEmail(subject, body);
        log.info("Sent success notification for permission sync");
    }

    public void notifyValidationErrors(List<ValidationError> errors) {
        String subject = "Permission Sync Failed - Validation Errors";
        String errorList = errors.stream()
            .map(error -> String.format("- %s: %s", error.getField(), error.getMessage()))
            .collect(Collectors.joining("\n"));
            
        String body = String.format("""
            Permission synchronization failed due to validation errors.
            
            Validation Errors:
            %s
            
            Please fix these errors and try again.
            """,
            errorList
        );
        
        sendEmail(subject, body);
        log.error("Sent validation error notification for permission sync");
    }

    public void notifySyncError(Exception e) {
        String subject = "Permission Sync Failed - System Error";
        String body = String.format("""
            Permission synchronization failed due to a system error.
            
            Error:
            %s
            
            Stack Trace:
            %s
            
            Please check the system logs for more details.
            """,
            e.getMessage(),
            getStackTraceAsString(e)
        );
        
        sendEmail(subject, body);
        log.error("Sent system error notification for permission sync", e);
    }

    private void sendEmail(String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(notificationEmails.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send notification email", e);
        }
    }

    private String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 1000) { // Limit stack trace length
                sb.append("...");
                break;
            }
        }
        return sb.toString();
    }
} 