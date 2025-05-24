package com.sb.authenticationrbac.entities;

import lombok.Data;

import java.util.List;

@Data
public class TimeAccessConfig {
    private boolean enabled;
    private List<String> allowedDays;
    private List<TimeWindow> allowedTimeWindows;
    private String timezone;
}