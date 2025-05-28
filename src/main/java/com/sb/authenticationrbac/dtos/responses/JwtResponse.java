package com.sb.authenticationrbac.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private String id;
    private String username;
    private String email;
    private String branchId;
    private List<String> roles;

    public JwtResponse(String accessToken, String refreshToken, String id, String username, 
                      String email, String branchId, List<String> roles) {
        this.token = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.branchId = branchId;
        this.roles = roles;
    }
} 