package com.sb.authenticationrbac.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAuditFeaturesConfig {
    @Field("can_view_deleted_records")
    private boolean canViewDeletedRecords;

    @Field("can_view_audit_history")
    private boolean canViewAuditHistory;
}
