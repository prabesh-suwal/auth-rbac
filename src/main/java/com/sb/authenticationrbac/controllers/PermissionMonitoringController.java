package com.sb.authenticationrbac.controllers;

import com.sb.authenticationrbac.aop.CheckPermission;
import com.sb.authenticationrbac.services.BranchHierarchyService;
import com.sb.authenticationrbac.services.PermissionPerformanceMonitoringService;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/monitoring")
public class PermissionMonitoringController {

    private final PermissionPerformanceMonitoringService performanceService;
    private final BranchHierarchyService branchHierarchyService;
    private final CacheManager cacheManager;

    public PermissionMonitoringController(PermissionPerformanceMonitoringService performanceService,
                                         BranchHierarchyService branchHierarchyService,
                                         CacheManager cacheManager) {
        this.performanceService = performanceService;
        this.branchHierarchyService = branchHierarchyService;
        this.cacheManager = cacheManager;
    }

    @GetMapping("/performance")
    @CheckPermission(
        value = "SYSTEM_MONITOR",
        operation = "VIEW",
        message = "Access denied to performance monitoring"
    )
    public ResponseEntity<PermissionPerformanceMonitoringService.PerformanceReport> getPerformanceReport() {
        PermissionPerformanceMonitoringService.PerformanceReport report = performanceService.getPerformanceReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/performance/permission/{permissionName}")
    @CheckPermission(
        value = "SYSTEM_MONITOR",
        operation = "VIEW",
        message = "Access denied to permission metrics"
    )
    public ResponseEntity<PermissionPerformanceMonitoringService.PermissionMetrics> getPermissionMetrics(
            @PathVariable String permissionName) {
        PermissionPerformanceMonitoringService.PermissionMetrics metrics = 
            performanceService.getPermissionMetrics(permissionName);
        
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/performance/user/{userId}")
    @CheckPermission(
        value = "SYSTEM_MONITOR",
        operation = "VIEW",
        message = "Access denied to user metrics"
    )
    public ResponseEntity<PermissionPerformanceMonitoringService.UserMetrics> getUserMetrics(
            @PathVariable String userId) {
        PermissionPerformanceMonitoringService.UserMetrics metrics = 
            performanceService.getUserMetrics(userId);
        
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(metrics);
    }

    @PostMapping("/performance/reset")
    @CheckPermission(
        value = "SYSTEM_ADMIN",
        operation = "RESET",
        message = "Access denied to reset performance metrics"
    )
    public ResponseEntity<Map<String, String>> resetPerformanceMetrics() {
        performanceService.resetMetrics();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Performance metrics reset successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cache/status")
    @CheckPermission(
        value = "SYSTEM_MONITOR",
        operation = "VIEW",
        message = "Access denied to cache monitoring"
    )
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        Map<String, Object> cacheStatus = new HashMap<>();
        
        // Get cache names and their statistics
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("name", cacheName);
                cacheInfo.put("nativeCache", cache.getNativeCache().getClass().getSimpleName());
                
                // For ConcurrentMapCache, we can get some basic info
                if (cache.getNativeCache() instanceof java.util.concurrent.ConcurrentMap) {
                    java.util.concurrent.ConcurrentMap<?, ?> nativeCache = 
                        (java.util.concurrent.ConcurrentMap<?, ?>) cache.getNativeCache();
                    cacheInfo.put("size", nativeCache.size());
                    cacheInfo.put("isEmpty", nativeCache.isEmpty());
                }
                
                cacheStatus.put(cacheName, cacheInfo);
            }
        });
        
        return ResponseEntity.ok(cacheStatus);
    }

    @PostMapping("/cache/clear")
    @CheckPermission(
        value = "SYSTEM_ADMIN",
        operation = "CLEAR",
        message = "Access denied to clear caches"
    )
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All caches cleared successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cache/{cacheName}/clear")
    @CheckPermission(
        value = "SYSTEM_ADMIN",
        operation = "CLEAR",
        message = "Access denied to clear specific cache"
    )
    public ResponseEntity<Map<String, String>> clearSpecificCache(@PathVariable String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        
        if (cache == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Cache not found: " + cacheName);
            return ResponseEntity.notFound().build();
        }
        
        cache.clear();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache '" + cacheName + "' cleared successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/branch-hierarchy/validate")
    @CheckPermission(
        value = "SYSTEM_MONITOR",
        operation = "VALIDATE",
        message = "Access denied to branch hierarchy validation"
    )
    public ResponseEntity<BranchHierarchyService.BranchHierarchyValidationResult> validateBranchHierarchy() {
        BranchHierarchyService.BranchHierarchyValidationResult result = 
            branchHierarchyService.validateHierarchy();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/branch-hierarchy/tree")
    @CheckPermission(
        value = "SYSTEM_MONITOR",
        operation = "VIEW",
        message = "Access denied to branch hierarchy tree"
    )
    public ResponseEntity<Map<String, BranchHierarchyService.BranchNode>> getBranchHierarchyTree() {
        Map<String, BranchHierarchyService.BranchNode> tree = branchHierarchyService.getBranchHierarchyTree();
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/branch-hierarchy/roots")
    @CheckPermission(
        value = "SYSTEM_MONITOR",
        operation = "VIEW",
        message = "Access denied to root branches"
    )
    public ResponseEntity<java.util.List<BranchHierarchyService.BranchNode>> getRootBranches() {
        java.util.List<BranchHierarchyService.BranchNode> roots = branchHierarchyService.getRootBranches();
        return ResponseEntity.ok(roots);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Basic system health
        health.put("status", "UP");
        health.put("timestamp", java.time.LocalDateTime.now());
        
        // Cache health
        Map<String, Object> cacheHealth = new HashMap<>();
        cacheHealth.put("totalCaches", cacheManager.getCacheNames().size());
        cacheHealth.put("cacheNames", cacheManager.getCacheNames());
        health.put("cache", cacheHealth);
        
        // Performance metrics summary
        PermissionPerformanceMonitoringService.PerformanceReport report = 
            performanceService.getPerformanceReport();
        Map<String, Object> performanceHealth = new HashMap<>();
        performanceHealth.put("totalEvaluations", report.getTotalEvaluations());
        performanceHealth.put("averageExecutionTime", report.getAverageEvaluationTimeMs());
        performanceHealth.put("cacheHitRate", report.getCacheHitRate());
        health.put("performance", performanceHealth);
        
        // Branch hierarchy health
        BranchHierarchyService.BranchHierarchyValidationResult hierarchyValidation = 
            branchHierarchyService.validateHierarchy();
        Map<String, Object> hierarchyHealth = new HashMap<>();
        hierarchyHealth.put("valid", hierarchyValidation.isValid());
        hierarchyHealth.put("orphanedBranches", hierarchyValidation.getOrphanedBranches().size());
        hierarchyHealth.put("cyclicBranches", hierarchyValidation.getCyclicBranches().size());
        hierarchyHealth.put("inconsistentReferences", hierarchyValidation.getInconsistentReferences().size());
        health.put("branchHierarchy", hierarchyHealth);
        
        return ResponseEntity.ok(health);
    }
} 