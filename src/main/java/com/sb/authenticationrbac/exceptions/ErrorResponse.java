package com.sb.authenticationrbac.exceptions;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ErrorResponse {
    private String code;
    private String message;
    private int status;
    private Map<String, Object> details;
    private LocalDateTime timestamp;
    
    public ErrorResponse(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(String code, String message, int status, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
    
    // getters and setters
}