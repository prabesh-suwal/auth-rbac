package com.sb.authenticationrbac.dtos.requests;

public class AmountLimitRequest {
    private String permission;
    private Double limit;
    
    // getters and setters
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public Double getLimit() { return limit; }
    public void setLimit(Double limit) { this.limit = limit; }
}