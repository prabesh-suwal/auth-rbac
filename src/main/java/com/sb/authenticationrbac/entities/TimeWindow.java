package com.sb.authenticationrbac.entities;

import lombok.Data;

@Data
public class TimeWindow {
    private String startTime; // HH:mm format
    private String endTime; // HH:mm format
    
    // constructors, getters, setters
}