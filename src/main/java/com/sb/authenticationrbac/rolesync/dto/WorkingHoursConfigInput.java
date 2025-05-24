package com.sb.authenticationrbac.rolesync.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkingHoursConfigInput {
    private boolean enabled;
    private String startTime;
    private String endTime;
    private List<String> workingDays;
}
