package com.sb.authenticationrbac.dtos.requests;

import lombok.Data;

import java.util.Map;

@Data
public class CreateBranchRequest {
    private String name;
    private String code;
    private String location;
    private String parentBranchId;
    private Map<String, Object> metadata;
    
    // constructors, getters, setters
}