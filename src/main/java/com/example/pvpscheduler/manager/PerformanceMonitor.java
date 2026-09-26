package com.example.pvpscheduler.manager;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PerformanceMonitor - Theo dõi performance của plugin
 */
public class PerformanceMonitor {
    private final JavaPlugin plugin;
    private final Map<String, TaskPerformance> taskPerformance = new ConcurrentHashMap<>();
    private final Map<String, Long> methodExecutionTimes = new ConcurrentHashMap<>();
    private final List<PerformanceMetric> recentMetrics = new ArrayList<>();
    
    private long lastMemoryCheck = 0;
    private static final int MAX_METRICS = 1000;
    private static final long MEMORY_CHECK_INTERVAL = 60000; // 1 minute
    
    public PerformanceMonitor(JavaPlugin plugin) {
        this.plugin = plugin;
        startMonitoring();
    }
    
    private void startMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    collectMetrics();
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in performance monitoring: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Every minute
    }
    
    public void recordMethodExecution(String methodName, long executionTime) {
        if (methodName == null) return;
        
        methodExecutionTimes.put(methodName, executionTime);
        
        // Keep only recent metrics
        if (recentMetrics.size() >= MAX_METRICS) {
            recentMetrics.remove(0);
        }
        
        recentMetrics.add(new PerformanceMetric(methodName, executionTime, System.currentTimeMillis()));
    }
    
    public void recordTaskExecution(String taskName, long executionTime, boolean success) {
        if (taskName == null) return;
        
        TaskPerformance perf = taskPerformance.computeIfAbsent(taskName, 
            k -> new TaskPerformance(taskName));
        perf.recordExecution(executionTime, success);
    }
    
    private void collectMetrics() {
        long currentTime = System.currentTimeMillis();
        
        // Check memory usage
        if (currentTime - lastMemoryCheck > MEMORY_CHECK_INTERVAL) {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            if (memoryUsagePercent > 80) {
                plugin.getLogger().warning(String.format(
                    "[PERFORMANCE] High memory usage: %.2f%% (Used: %d MB / Max: %d MB)",
                    memoryUsagePercent, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024));
            }
            
            lastMemoryCheck = currentTime;
        }
        
        // Check for slow tasks
        for (TaskPerformance perf : taskPerformance.values()) {
            if (perf.getAverageExecutionTime() > 1000) { // > 1 second
                plugin.getLogger().warning(String.format(
                    "[PERFORMANCE] Slow task detected: %s (Avg: %d ms)",
                    perf.getTaskName(), perf.getAverageExecutionTime()));
            }
        }
    }
    
    public PerformanceReport generateReport() {
        PerformanceReport report = new PerformanceReport();
        
        // Memory info
        Runtime runtime = Runtime.getRuntime();
        report.setTotalMemory(runtime.totalMemory());
        report.setFreeMemory(runtime.freeMemory());
        report.setUsedMemory(runtime.totalMemory() - runtime.freeMemory());
        report.setMaxMemory(runtime.maxMemory());
        
        // Task performance
        report.setTaskPerformance(new HashMap<>(taskPerformance));
        
        // Method execution times
        report.setMethodExecutionTimes(new HashMap<>(methodExecutionTimes));
        
        // Recent metrics
        report.setRecentMetrics(new ArrayList<>(recentMetrics));
        
        return report;
    }
    
    public List<String> getSlowestMethods(int count) {
        return methodExecutionTimes.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(count)
            .map(e -> e.getKey() + ": " + e.getValue() + "ms")
            .collect(java.util.stream.Collectors.toList());
    }
    
    public void cleanup() {
        taskPerformance.clear();
        methodExecutionTimes.clear();
        recentMetrics.clear();
    }
    
    // Inner classes
    public static class TaskPerformance {
        private final String taskName;
        private long totalExecutions = 0;
        private long totalExecutionTime = 0;
        private long successfulExecutions = 0;
        private long failedExecutions = 0;
        private long maxExecutionTime = 0;
        private long minExecutionTime = Long.MAX_VALUE;
        
        public TaskPerformance(String taskName) {
            this.taskName = taskName;
        }
        
        public void recordExecution(long executionTime, boolean success) {
            totalExecutions++;
            totalExecutionTime += executionTime;
            
            if (success) {
                successfulExecutions++;
            } else {
                failedExecutions++;
            }
            
            if (executionTime > maxExecutionTime) {
                maxExecutionTime = executionTime;
            }
            if (executionTime < minExecutionTime) {
                minExecutionTime = executionTime;
            }
        }
        
        public long getAverageExecutionTime() {
            return totalExecutions > 0 ? totalExecutionTime / totalExecutions : 0;
        }
        
        public double getSuccessRate() {
            return totalExecutions > 0 ? (double) successfulExecutions / totalExecutions * 100 : 0;
        }
        
        // Getters
        public String getTaskName() { return taskName; }
        public long getTotalExecutions() { return totalExecutions; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        public long getSuccessfulExecutions() { return successfulExecutions; }
        public long getFailedExecutions() { return failedExecutions; }
        public long getMaxExecutionTime() { return maxExecutionTime; }
        public long getMinExecutionTime() { return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime; }
    }
    
    public static class PerformanceMetric {
        private final String methodName;
        private final long executionTime;
        private final long timestamp;
        
        public PerformanceMetric(String methodName, long executionTime, long timestamp) {
            this.methodName = methodName;
            this.executionTime = executionTime;
            this.timestamp = timestamp;
        }
        
        public String getMethodName() { return methodName; }
        public long getExecutionTime() { return executionTime; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class PerformanceReport {
        private long totalMemory;
        private long freeMemory;
        private long usedMemory;
        private long maxMemory;
        private Map<String, TaskPerformance> taskPerformance;
        private Map<String, Long> methodExecutionTimes;
        private List<PerformanceMetric> recentMetrics;
        
        // Getters and Setters
        public long getTotalMemory() { return totalMemory; }
        public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }
        public long getFreeMemory() { return freeMemory; }
        public void setFreeMemory(long freeMemory) { this.freeMemory = freeMemory; }
        public long getUsedMemory() { return usedMemory; }
        public void setUsedMemory(long usedMemory) { this.usedMemory = usedMemory; }
        public long getMaxMemory() { return maxMemory; }
        public void setMaxMemory(long maxMemory) { this.maxMemory = maxMemory; }
        public Map<String, TaskPerformance> getTaskPerformance() { return taskPerformance; }
        public void setTaskPerformance(Map<String, TaskPerformance> taskPerformance) { this.taskPerformance = taskPerformance; }
        public Map<String, Long> getMethodExecutionTimes() { return methodExecutionTimes; }
        public void setMethodExecutionTimes(Map<String, Long> methodExecutionTimes) { this.methodExecutionTimes = methodExecutionTimes; }
        public List<PerformanceMetric> getRecentMetrics() { return recentMetrics; }
        public void setRecentMetrics(List<PerformanceMetric> recentMetrics) { this.recentMetrics = recentMetrics; }
    }
}

