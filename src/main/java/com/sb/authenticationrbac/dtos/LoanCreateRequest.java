package com.sb.authenticationrbac.dtos;

import lombok.Data;

@Data
public class LoanCreateRequest {
    private String loanId;
    private String branchId;
}
