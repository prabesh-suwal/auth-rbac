package com.sb.authenticationrbac.controllers;

import com.sb.authenticationrbac.aop.CheckPermission;
import com.sb.authenticationrbac.aop.CheckPermissions;
import com.sb.authenticationrbac.dtos.LoanCreateRequest;
import com.sb.authenticationrbac.dtos.LoanUpdateRequest;
import com.sb.authenticationrbac.entities.Loan;
import com.sb.authenticationrbac.exceptions.DynamicPermissionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final Logger logger = LoggerFactory.getLogger(LoanController.class);

    // Simple dynamic permission check - all logic defined in database
    @GetMapping("/{id}")
    @CheckPermission(
        value = "LOAN_VIEW",
        resource = "LOAN",
        resourceIdParam = "id",
        operation = "READ",
        message = "You don't have permission to view this loan"
    )
    public ResponseEntity<Loan> getLoan(@PathVariable String id) {
        return ResponseEntity.ok(new Loan());
    }

    // Loan approval - amount limits, branch access, time restrictions all from config
    @PostMapping("/{id}/approve")
    @CheckPermission(
        value = "LOAN_APPROVE",
        resource = "LOAN",
        resourceIdParam = "id",
        operation = "APPROVE",
        contextParams = {"approvalComments", "urgentFlag"},
        message = "Loan approval not authorized"
    )
    public ResponseEntity<Loan> approveLoan(@PathVariable String id,
                                          @RequestParam(required = false) String approvalComments,
                                          @RequestParam(defaultValue = "false") boolean urgentFlag) {
//        Loan loan = loanService.approveLoan(id, approvalComments, urgentFlag);
        return ResponseEntity.ok(new Loan());
    }

    // Multiple permissions with OR logic
    @PutMapping("/{id}")
    @CheckPermissions(
        value = {
            @CheckPermission(value = "LOAN_UPDATE", resource = "LOAN", resourceIdParam = "id", operation = "UPDATE"),
            @CheckPermission(value = "LOAN_ADMIN", operation = "ADMIN")
        },
        logic = "OR",
        message = "Insufficient permissions to update loan"
    )
    public ResponseEntity<Loan> updateLoan(@PathVariable String id, @RequestBody LoanUpdateRequest request) {
//        Loan loan = loanService.updateLoan(id, request);
        return ResponseEntity.ok(new Loan());
    }

    // Create loan - branch access determined dynamically
    @PostMapping
    @CheckPermission(
        value = "LOAN_CREATE",
        operation = "CREATE",
        resourceParam = "loanRequest", // Pass the request object for validation
        contextParams = {"priority", "customerType"}
    )
    public ResponseEntity<Loan> createLoan(@RequestBody LoanCreateRequest loanRequest,
                                         @RequestParam(defaultValue = "NORMAL") String priority,
                                         @RequestParam(defaultValue = "REGULAR") String customerType) {

        logger.info("::: Loan creation api called :::");
//        Loan loan = loanService.createLoan(loanRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Loan());
    }

    // Bulk operations with special permissions
//    @PostMapping("/bulk-approve")
//    @CheckPermission(
//        value = "LOAN_BULK_APPROVE",
//        operation = "BULK_APPROVE",
//        contextParams = {"loanIds", "totalAmount"}
//    )
//    public ResponseEntity<BulkApprovalResult> bulkApproveLoan(@RequestBody BulkApprovalRequest request) {
//        List<String> loanIds = request.getLoanIds();
//        Double totalAmount = request.getTotalAmount();
//
//        BulkApprovalResult result = loanService.bulkApprove(loanIds);
//        return ResponseEntity.ok(result);
//    }

    // Get user's accessible loans - no annotation needed, handled in service
//    @GetMapping("/accessible")
//    public ResponseEntity<List<Loan>> getAccessibleLoans(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            Authentication authentication) {
//        List<Loan> loans = loanService.getUserAccessibleLoans(authentication, page, size);
//        return ResponseEntity.ok(loans);
//    }
}