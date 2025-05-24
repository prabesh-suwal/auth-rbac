package com.sb.authenticationrbac.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Document
public class Loan {
    @Id
    private String id;
    
    private BigDecimal amount;
    private Branch branch;

    private LoanStatus status;
    
    // other loan fields
    
    // constructors, getters, setters
}
