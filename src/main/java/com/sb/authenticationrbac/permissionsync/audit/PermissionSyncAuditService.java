package com.sb.authenticationrbac.permissionsync.audit;

import com.sb.authenticationrbac.permissionsync.notification.SyncResult;
import com.sb.authenticationrbac.permissionsync.validation.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PermissionSyncAuditService {

    private final MongoTemplate mongoTemplate;

    public PermissionSyncAuditService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void logSuccessfulSync(SyncResult result, String triggeredBy, String triggeredByUser) {
        PermissionSyncAudit audit = PermissionSyncAudit.builder()
            .timestamp(LocalDateTime.now())
            .status("SUCCESS")
            .permissionsAdded(result.getAdded())
            .permissionsUpdated(result.getUpdated())
            .permissionsTotal(result.getTotal())
            .executionTimeMs(result.getTimeTaken())
            .triggeredBy(triggeredBy)
            .triggeredByUser(triggeredByUser)
            .metadata(createMetadata(result))
            .build();

        mongoTemplate.save(audit);
        log.info("Logged successful permission sync: added={}, updated={}, total={}, time={}ms",
            result.getAdded(), result.getUpdated(), result.getTotal(), result.getTimeTaken());
    }

    public void logValidationError(List<ValidationError> errors, String triggeredBy, String triggeredByUser) {
        List<String> errorMessages = errors.stream()
            .map(error -> error.getField() + ": " + error.getMessage())
            .collect(Collectors.toList());

        PermissionSyncAudit audit = PermissionSyncAudit.builder()
            .timestamp(LocalDateTime.now())
            .status("VALIDATION_ERROR")
            .validationErrors(errorMessages)
            .triggeredBy(triggeredBy)
            .triggeredByUser(triggeredByUser)
            .build();

        mongoTemplate.save(audit);
        log.error("Logged permission sync validation errors: {}", errorMessages);
    }

    public void logSystemError(Exception e, String triggeredBy, String triggeredByUser) {
        PermissionSyncAudit audit = PermissionSyncAudit.builder()
            .timestamp(LocalDateTime.now())
            .status("SYSTEM_ERROR")
            .errorMessage(e.getMessage())
            .triggeredBy(triggeredBy)
            .triggeredByUser(triggeredByUser)
            .metadata(Map.of("stackTrace", getStackTraceAsString(e)))
            .build();

        mongoTemplate.save(audit);
        log.error("Logged permission sync system error", e);
    }

    public Page<PermissionSyncAudit> getAuditLogs(
            LocalDateTime from,
            LocalDateTime to,
            String status,
            Pageable pageable) {
        
        Query query = new Query();
        
        if (from != null) {
            query.addCriteria(Criteria.where("timestamp").gte(from));
        }
        if (to != null) {
            query.addCriteria(Criteria.where("timestamp").lte(to));
        }
        if (status != null && !status.isEmpty()) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        
        long total = mongoTemplate.count(query, PermissionSyncAudit.class);
        
        query.with(pageable);
        List<PermissionSyncAudit> audits = mongoTemplate.find(query, PermissionSyncAudit.class);
        
        return new org.springframework.data.domain.PageImpl<>(audits, pageable, total);
    }

    private Map<String, Object> createMetadata(SyncResult result) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("executionTimeMs", result.getTimeTaken());
        metadata.put("timestamp", LocalDateTime.now());
        return metadata;
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