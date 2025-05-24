//package com.sb.authenticationrbac.controllers;
//import com.sb.authenticationrbac.aop.CheckPermission;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDate;
//
//
//@RestController
//@RequestMapping("/api/reports")
//public class ReportController {
//
//    private final ReportService reportService;
//
//    public ReportController(ReportService reportService) {
//        this.reportService = reportService;
//    }
//
//    // Branch-specific reports - access controlled by stored configuration
//    @GetMapping("/loans/branch/{branchId}")
//    @CheckPermission(
//        value = "REPORT_BRANCH_LOANS",
//        operation = "VIEW",
//        contextParams = {"branchId", "startDate", "endDate", "reportType"}
//    )
//    public ResponseEntity<LoanReport> getBranchLoanReport(@PathVariable String branchId,
//                                                        @RequestParam LocalDate startDate,
//                                                        @RequestParam LocalDate endDate,
//                                                        @RequestParam(defaultValue = "SUMMARY") String reportType) {
//        LoanReport report = reportService.generateBranchLoanReport(branchId, startDate, endDate, reportType);
//        return ResponseEntity.ok(report);
//    }
//
//    // Consolidated reports with complex dynamic conditions
//    @GetMapping("/loans/consolidated")
//    @CheckPermission(
//        value = "REPORT_CONSOLIDATED",
//        operation = "VIEW",
//        contextParams = {"startDate", "endDate", "includeSubBranches", "reportLevel"}
//    )
//    public ResponseEntity<ConsolidatedReport> getConsolidatedReport(@RequestParam LocalDate startDate,
//                                                                  @RequestParam LocalDate endDate,
//                                                                  @RequestParam(defaultValue = "false") boolean includeSubBranches,
//                                                                  @RequestParam(defaultValue = "STANDARD") String reportLevel) {
//        ConsolidatedReport report = reportService.generateConsolidatedReport(startDate, endDate, includeSubBranches, reportLevel);
//        return ResponseEntity.ok(report);
//    }
//
//    // Financial reports with amount-based access control
//    @GetMapping("/financial/{reportType}")
//    @CheckPermission(
//        value = "REPORT_FINANCIAL",
//        operation = "VIEW",
//        contextParams = {"reportType", "amountThreshold", "confidentialityLevel"}
//    )
//    public ResponseEntity<FinancialReport> getFinancialReport(@PathVariable String reportType,
//                                                            @RequestParam(required = false) Double amountThreshold,
//                                                            @RequestParam(defaultValue = "STANDARD") String confidentialityLevel) {
//        FinancialReport report = reportService.generateFinancialReport(reportType, amountThreshold, confidentialityLevel);
//        return ResponseEntity.ok(report);
//    }
//}