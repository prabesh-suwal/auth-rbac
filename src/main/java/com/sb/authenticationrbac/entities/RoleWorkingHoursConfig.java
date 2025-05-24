package com.sb.authenticationrbac.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleWorkingHoursConfig {
    private boolean enabled;

    @Field("start_time")
    private String startTime;

    @Field("end_time")
    private String endTime;

    @Field("working_days")
    private List<String> workingDays;
}
