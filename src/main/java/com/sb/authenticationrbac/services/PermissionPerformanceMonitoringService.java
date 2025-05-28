package com.sb.authenticationrbac.services;

import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class PermissionPerformanceMonitoringService {

    private final Map<String, PermissionMetrics> permissionMetrics = new ConcurrentHashMap<>();
    private final Map<String, UserMetrics> userMetrics = new ConcurrentHashMap<>();
    private final LongAdder totalEvaluations = new LongAdder();
    private final LongAdder totalEvaluationTime = new LongAdder();
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();

    public void recordPermissionEvaluation(String permissionName, String userId, long executionTimeMs, boolean allowed, boolean fromCache) {
        // Update global metrics
        totalEvaluations.increment();
        totalEvaluationTime.add(executionTimeMs);
        
        if (fromCache) {
            cacheHits.increment();
        } else {
            cacheMisses.increment();
        }

        // Update permission-specific metrics
        permissionMetrics.computeIfAbsent(permissionName, k -> new PermissionMetrics(k))
                .recordEvaluation(executionTimeMs, allowed, fromCache);

        // Update user-specific metrics
        userMetrics.computeIfAbsent(userId, k -> new UserMetrics(k))
                .recordEvaluation(permissionName, executionTimeMs, allowed, fromCache);
    }

    public PerformanceReport getPerformanceReport() {
        PerformanceReport report = new PerformanceReport();
        
        // Global metrics
        long totalEvals = totalEvaluations.sum();
        report.setTotalEvaluations(totalEvals);
        report.setTotalEvaluationTimeMs(totalEvaluationTime.sum());
        report.setAverageEvaluationTimeMs(totalEvals > 0 ? (double) totalEvaluationTime.sum() / totalEvals : 0);
        
        // Cache metrics
        long hits = cacheHits.sum();
        long misses = cacheMisses.sum();
        long totalCacheRequests = hits + misses;
        report.setCacheHitRate(totalCacheRequests > 0 ? (double) hits / totalCacheRequests : 0);
        report.setCacheHits(hits);
        report.setCacheMisses(misses);

        // Top slow permissions
        report.setTopSlowPermissions(
            permissionMetrics.values().stream()
                .sorted((a, b) -> Double.compare(b.getAverageExecutionTime(), a.getAverageExecutionTime()))
                .limit(10)
                .map(this::toPermissionSummary)
                .collect(Collectors.toList())
        );

        // Most frequently evaluated permissions
        report.setMostFrequentPermissions(
            permissionMetrics.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotalEvaluations(), a.getTotalEvaluations()))
                .limit(10)
                .map(this::toPermissionSummary)
                .collect(Collectors.toList())
        );

        // Users with most evaluations
        report.setTopActiveUsers(
            userMetrics.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotalEvaluations(), a.getTotalEvaluations()))
                .limit(10)
                .map(this::toUserSummary)
                .collect(Collectors.toList())
        );

        report.setGeneratedAt(LocalDateTime.now());
        return report;
    }

    public PermissionMetrics getPermissionMetrics(String permissionName) {
        return permissionMetrics.get(permissionName);
    }

    public UserMetrics getUserMetrics(String userId) {
        return userMetrics.get(userId);
    }

    public void resetMetrics() {
        permissionMetrics.clear();
        userMetrics.clear();
        totalEvaluations.reset();
        totalEvaluationTime.reset();
        cacheHits.reset();
        cacheMisses.reset();
    }

    private PermissionSummary toPermissionSummary(PermissionMetrics metrics) {
        PermissionSummary summary = new PermissionSummary();
        summary.setPermissionName(metrics.getPermissionName());
        summary.setTotalEvaluations(metrics.getTotalEvaluations());
        summary.setAverageExecutionTime(metrics.getAverageExecutionTime());
        summary.setAllowedCount(metrics.getAllowedCount());
        summary.setDeniedCount(metrics.getDeniedCount());
        summary.setCacheHitRate(metrics.getCacheHitRate());
        return summary;
    }

    private UserSummary toUserSummary(UserMetrics metrics) {
        UserSummary summary = new UserSummary();
        summary.setUserId(metrics.getUserId());
        summary.setTotalEvaluations(metrics.getTotalEvaluations());
        summary.setAverageExecutionTime(metrics.getAverageExecutionTime());
        summary.setUniquePermissions(metrics.getUniquePermissions());
        summary.setAllowedCount(metrics.getAllowedCount());
        summary.setDeniedCount(metrics.getDeniedCount());
        return summary;
    }

    // Inner classes for metrics
    public static class PermissionMetrics {
        private final String permissionName;
        private final AtomicLong totalEvaluations = new AtomicLong();
        private final LongAdder totalExecutionTime = new LongAdder();
        private final AtomicLong allowedCount = new AtomicLong();
        private final AtomicLong deniedCount = new AtomicLong();
        private final AtomicLong cacheHits = new AtomicLong();
        private final AtomicLong cacheMisses = new AtomicLong();
        private volatile LocalDateTime lastEvaluated;

        public PermissionMetrics(String permissionName) {
            this.permissionName = permissionName;
        }

        public void recordEvaluation(long executionTimeMs, boolean allowed, boolean fromCache) {
            totalEvaluations.incrementAndGet();
            totalExecutionTime.add(executionTimeMs);
            
            if (allowed) {
                allowedCount.incrementAndGet();
            } else {
                deniedCount.incrementAndGet();
            }
            
            if (fromCache) {
                cacheHits.incrementAndGet();
            } else {
                cacheMisses.incrementAndGet();
            }
            
            lastEvaluated = LocalDateTime.now();
        }

        // Getters
        public String getPermissionName() { return permissionName; }
        public long getTotalEvaluations() { return totalEvaluations.get(); }
        public double getAverageExecutionTime() { 
            long total = totalEvaluations.get();
            return total > 0 ? (double) totalExecutionTime.sum() / total : 0;
        }
        public long getAllowedCount() { return allowedCount.get(); }
        public long getDeniedCount() { return deniedCount.get(); }
        public double getCacheHitRate() {
            long hits = cacheHits.get();
            long misses = cacheMisses.get();
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0;
        }
        public LocalDateTime getLastEvaluated() { return lastEvaluated; }
    }

    public static class UserMetrics {
        private final String userId;
        private final AtomicLong totalEvaluations = new AtomicLong();
        private final LongAdder totalExecutionTime = new LongAdder();
        private final AtomicLong allowedCount = new AtomicLong();
        private final AtomicLong deniedCount = new AtomicLong();
        private final Map<String, AtomicLong> permissionCounts = new ConcurrentHashMap<>();
        private volatile LocalDateTime lastEvaluated;

        public UserMetrics(String userId) {
            this.userId = userId;
        }

        public void recordEvaluation(String permissionName, long executionTimeMs, boolean allowed, boolean fromCache) {
            totalEvaluations.incrementAndGet();
            totalExecutionTime.add(executionTimeMs);
            
            if (allowed) {
                allowedCount.incrementAndGet();
            } else {
                deniedCount.incrementAndGet();
            }
            
            permissionCounts.computeIfAbsent(permissionName, k -> new AtomicLong()).incrementAndGet();
            lastEvaluated = LocalDateTime.now();
        }

        // Getters
        public String getUserId() { return userId; }
        public long getTotalEvaluations() { return totalEvaluations.get(); }
        public double getAverageExecutionTime() { 
            long total = totalEvaluations.get();
            return total > 0 ? (double) totalExecutionTime.sum() / total : 0;
        }
        public long getAllowedCount() { return allowedCount.get(); }
        public long getDeniedCount() { return deniedCount.get(); }
        public int getUniquePermissions() { return permissionCounts.size(); }
        public Map<String, Long> getPermissionCounts() {
            return permissionCounts.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
        }
        public LocalDateTime getLastEvaluated() { return lastEvaluated; }
    }

    // Report classes
    public static class PerformanceReport {
        private long totalEvaluations;
        private long totalEvaluationTimeMs;
        private double averageEvaluationTimeMs;
        private double cacheHitRate;
        private long cacheHits;
        private long cacheMisses;
        private List<PermissionSummary> topSlowPermissions = new ArrayList<>();
        private List<PermissionSummary> mostFrequentPermissions = new ArrayList<>();
        private List<UserSummary> topActiveUsers = new ArrayList<>();
        private LocalDateTime generatedAt;

        // Getters and setters
        public long getTotalEvaluations() { return totalEvaluations; }
        public void setTotalEvaluations(long totalEvaluations) { this.totalEvaluations = totalEvaluations; }
        public long getTotalEvaluationTimeMs() { return totalEvaluationTimeMs; }
        public void setTotalEvaluationTimeMs(long totalEvaluationTimeMs) { this.totalEvaluationTimeMs = totalEvaluationTimeMs; }
        public double getAverageEvaluationTimeMs() { return averageEvaluationTimeMs; }
        public void setAverageEvaluationTimeMs(double averageEvaluationTimeMs) { this.averageEvaluationTimeMs = averageEvaluationTimeMs; }
        public double getCacheHitRate() { return cacheHitRate; }
        public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
        public long getCacheHits() { return cacheHits; }
        public void setCacheHits(long cacheHits) { this.cacheHits = cacheHits; }
        public long getCacheMisses() { return cacheMisses; }
        public void setCacheMisses(long cacheMisses) { this.cacheMisses = cacheMisses; }
        public List<PermissionSummary> getTopSlowPermissions() { return topSlowPermissions; }
        public void setTopSlowPermissions(List<PermissionSummary> topSlowPermissions) { this.topSlowPermissions = topSlowPermissions; }
        public List<PermissionSummary> getMostFrequentPermissions() { return mostFrequentPermissions; }
        public void setMostFrequentPermissions(List<PermissionSummary> mostFrequentPermissions) { this.mostFrequentPermissions = mostFrequentPermissions; }
        public List<UserSummary> getTopActiveUsers() { return topActiveUsers; }
        public void setTopActiveUsers(List<UserSummary> topActiveUsers) { this.topActiveUsers = topActiveUsers; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    }

    public static class PermissionSummary {
        private String permissionName;
        private long totalEvaluations;
        private double averageExecutionTime;
        private long allowedCount;
        private long deniedCount;
        private double cacheHitRate;

        // Getters and setters
        public String getPermissionName() { return permissionName; }
        public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
        public long getTotalEvaluations() { return totalEvaluations; }
        public void setTotalEvaluations(long totalEvaluations) { this.totalEvaluations = totalEvaluations; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(double averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
        public long getAllowedCount() { return allowedCount; }
        public void setAllowedCount(long allowedCount) { this.allowedCount = allowedCount; }
        public long getDeniedCount() { return deniedCount; }
        public void setDeniedCount(long deniedCount) { this.deniedCount = deniedCount; }
        public double getCacheHitRate() { return cacheHitRate; }
        public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
    }

    public static class UserSummary {
        private String userId;
        private long totalEvaluations;
        private double averageExecutionTime;
        private int uniquePermissions;
        private long allowedCount;
        private long deniedCount;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public long getTotalEvaluations() { return totalEvaluations; }
        public void setTotalEvaluations(long totalEvaluations) { this.totalEvaluations = totalEvaluations; }
        public double getAverageExecutionTime() { return averageExecutionTime; }
        public void setAverageExecutionTime(double averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
        public int getUniquePermissions() { return uniquePermissions; }
        public void setUniquePermissions(int uniquePermissions) { this.uniquePermissions = uniquePermissions; }
        public long getAllowedCount() { return allowedCount; }
        public void setAllowedCount(long allowedCount) { this.allowedCount = allowedCount; }
        public long getDeniedCount() { return deniedCount; }
        public void setDeniedCount(long deniedCount) { this.deniedCount = deniedCount; }
    }
} 