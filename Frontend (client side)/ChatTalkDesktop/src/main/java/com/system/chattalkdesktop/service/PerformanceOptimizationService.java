package com.system.chattalkdesktop.service;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance optimization service that coordinates multiple polling mechanisms
 * to prevent conflicts and improve overall application performance
 */
@Slf4j
public class PerformanceOptimizationService {
    private static volatile PerformanceOptimizationService instance;
    
    // Centralized thread pool for all polling operations
    private final ScheduledExecutorService pollingExecutor;
    private final ExecutorService backgroundExecutor;
    
    // Polling coordination
    private final AtomicBoolean isPollingActive = new AtomicBoolean(false);
    private final AtomicInteger activePollingTasks = new AtomicInteger(0);
    
    // Polling intervals (configurable)
    private volatile int notificationPollInterval = 60000; // 60 seconds
    private volatile int sidebarPollInterval = 30000; // 30 seconds
    private volatile int chatPollInterval = 2000; // 2 seconds
    private volatile int statusPollInterval = 5000; // 5 seconds
    
    // Task tracking
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();
    
    private PerformanceOptimizationService() {
        // Create thread pool with appropriate sizing
        this.pollingExecutor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "PollingThread-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        this.backgroundExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "BackgroundThread-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        log.info("PerformanceOptimizationService initialized");
    }
    
    public static PerformanceOptimizationService getInstance() {
        if (instance == null) {
            synchronized (PerformanceOptimizationService.class) {
                if (instance == null) {
                    instance = new PerformanceOptimizationService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Schedule a polling task with conflict prevention
     */
    public ScheduledFuture<?> schedulePollingTask(String taskName, Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (taskName == null || task == null) {
            log.warn("Cannot schedule polling task with null name or task");
            return null;
        }
        
        // Cancel existing task if present
        cancelPollingTask(taskName);
        
        // Create coordinated task
        Runnable coordinatedTask = () -> {
            if (!isPollingActive.get()) {
                log.debug("Polling paused, skipping task: {}", taskName);
                return;
            }
            
            activePollingTasks.incrementAndGet();
            try {
                task.run();
            } catch (Exception e) {
                log.error("Error in polling task {}: {}", taskName, e.getMessage(), e);
            } finally {
                activePollingTasks.decrementAndGet();
            }
        };
        
        ScheduledFuture<?> future = pollingExecutor.scheduleAtFixedRate(coordinatedTask, initialDelay, period, unit);
        activeTasks.put(taskName, future);
        
        log.debug("Scheduled polling task: {} with interval: {} {}", taskName, period, unit);
        return future;
    }
    
    /**
     * Cancel a specific polling task
     */
    public void cancelPollingTask(String taskName) {
        ScheduledFuture<?> task = activeTasks.remove(taskName);
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
            log.debug("Cancelled polling task: {}", taskName);
        }
    }
    
    /**
     * Execute background task
     */
    public CompletableFuture<Void> executeBackgroundTask(Runnable task) {
        if (task == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(task, backgroundExecutor);
    }
    
    /**
     * Start all polling operations
     */
    public void startPolling() {
        if (isPollingActive.compareAndSet(false, true)) {
            log.info("Starting coordinated polling operations");
        }
    }
    
    /**
     * Stop all polling operations
     */
    public void stopPolling() {
        if (isPollingActive.compareAndSet(true, false)) {
            log.info("Stopping coordinated polling operations");
            
            // Cancel all active tasks
            activeTasks.forEach((name, task) -> {
                if (!task.isCancelled()) {
                    task.cancel(false);
                    log.debug("Cancelled polling task: {}", name);
                }
            });
            activeTasks.clear();
        }
    }
    
    /**
     * Pause polling temporarily
     */
    public void pausePolling() {
        isPollingActive.set(false);
        log.info("Polling operations paused");
    }
    
    /**
     * Resume polling
     */
    public void resumePolling() {
        isPollingActive.set(true);
        log.info("Polling operations resumed");
    }
    
    /**
     * Get current polling status
     */
    public boolean isPollingActive() {
        return isPollingActive.get();
    }
    
    /**
     * Get number of active polling tasks
     */
    public int getActivePollingTaskCount() {
        return activePollingTasks.get();
    }
    
    /**
     * Update polling intervals
     */
    public void updatePollingIntervals(int notificationInterval, int sidebarInterval, int chatInterval, int statusInterval) {
        this.notificationPollInterval = notificationInterval;
        this.sidebarPollInterval = sidebarInterval;
        this.chatPollInterval = chatInterval;
        this.statusPollInterval = statusInterval;
        
        log.info("Updated polling intervals - Notifications: {}ms, Sidebar: {}ms, Chat: {}ms, Status: {}ms",
                notificationInterval, sidebarInterval, chatInterval, statusInterval);
    }
    
    /**
     * Get current polling intervals
     */
    public PollingIntervals getPollingIntervals() {
        return new PollingIntervals(notificationPollInterval, sidebarPollInterval, chatPollInterval, statusPollInterval);
    }
    
    /**
     * Shutdown the service
     */
    public void shutdown() {
        stopPolling();
        
        if (!pollingExecutor.isShutdown()) {
            pollingExecutor.shutdown();
            try {
                if (!pollingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    pollingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                pollingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (!backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
            try {
                if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    backgroundExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                backgroundExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("PerformanceOptimizationService shutdown complete");
    }
    
    /**
     * Data class for polling intervals
     */
    public static class PollingIntervals {
        public final int notificationInterval;
        public final int sidebarInterval;
        public final int chatInterval;
        public final int statusInterval;
        
        public PollingIntervals(int notificationInterval, int sidebarInterval, int chatInterval, int statusInterval) {
            this.notificationInterval = notificationInterval;
            this.sidebarInterval = sidebarInterval;
            this.chatInterval = chatInterval;
            this.statusInterval = statusInterval;
        }
    }
}

