package com.sb.authenticationrbac.dtos.requests;

import lombok.Data;

import java.util.List;

@Data
// Supporting DTOs and Request classes
public class BulkApprovalRequest {
    private List<String> loanIds;
    private Double totalAmount;
    private String reason;
    private boolean override;
    
    // getters and setters
    public List<String> getLoanIds() { return loanIds; }
    public void setLoanIds(List<String> loanIds) { this.loanIds = loanIds; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isOverride() { return override; }
    public void setOverride(boolean override) { this.override = override; }
}
