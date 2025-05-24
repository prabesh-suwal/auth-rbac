package com.sb.authenticationrbac.permissionsync.notification;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyncResult {
    private int added;
    private int updated;
    private int total;
    private long timeTaken;
} 