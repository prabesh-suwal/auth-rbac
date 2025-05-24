//package com.sb.authenticationrbac.services;
//
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//// Service layer with method-level security
//@Service
//@Transactional
//public class LoanService {
//
//    private final LoanRepository loanRepository;
//    private final PermissionEvaluationService permissionService;
//
//    public LoanService(LoanRepository loanRepository,
//                      PermissionEvaluationService permissionService) {
//        this.loanRepository = loanRepository;
//        this.permissionService = permissionService;
//    }
//
//    // Only users with LOAN_VIEW permission and branch access can view
//    @PreAuthorize("hasPermission(#loanId, 'LOAN', 'VIEW')")
//    public Loan getLoan(Long loanId) {
//        return loanRepository.findById(loanId)
//            .orElseThrow(() -> new LoanNotFoundException("Loan not found"));
//    }
//
//    // Custom authorization with amount check
//    @PreAuthorize("hasPermission(#loan, 'LOAN_APPROVE') and @loanService.canApproveAmount(#loan.amount)")
//    public Loan approveLoan(Loan loan) {
//        loan.setStatus(LoanStatus.APPROVED);
//        return loanRepository.save(loan);
//    }
//
//    // Get loans that user can view (filtered by branch and permissions)
//    public List<Loan> getUserAccessibleLoans(Authentication authentication) {
//        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
//        Long userId = userDetails.getUserId();
//        Branch userBranch = userDetails.getBranch();
//
//        // Get all loans from user's branch
//        List<Loan> branchLoans = loanRepository.findByBranch(userBranch);
//
//        // Filter based on user's specific permissions
//        return branchLoans.stream()
//            .filter(loan -> permissionService.canAccessResource(userId, loan))
//            .collect(Collectors.toList());
//    }
//
//    // Check if current user can approve the given amount
//    public boolean canApproveAmount(BigDecimal amount) {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
//
//        BigDecimal limit = permissionService.getAmountLimit(userDetails.getUserId(), "LOAN_APPROVE");
//        return amount.compareTo(limit) <= 0;
//    }
//}