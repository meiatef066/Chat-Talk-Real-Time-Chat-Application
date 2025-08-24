package com.system.chattalkdesktop.utils;

/**
 * Configuration class for managing notification settings
 * This helps reduce excessive notifications and provides centralized control
 */
public class NotificationConfig {
    
    // Connection notifications (WebSocket connection status)
    private static boolean showConnectionNotifications = false;
    
    // Debug notifications (detailed logging and debug info)
    private static boolean showDebugNotifications = false;
    
    // Chat message notifications
    private static boolean showChatNotifications = true;
    
    // Friend request notifications
    private static boolean showFriendRequestNotifications = true;
    
    // System notifications (errors, warnings)
    private static boolean showSystemNotifications = true;
    
    // Success notifications
    private static boolean showSuccessNotifications = false;
    
    public static boolean isShowConnectionNotifications() {
        return showConnectionNotifications;
    }
    
    public static void setShowConnectionNotifications(boolean show) {
        showConnectionNotifications = show;
    }
    
    public static boolean isShowDebugNotifications() {
        return showDebugNotifications;
    }
    
    public static void setShowDebugNotifications(boolean show) {
        showDebugNotifications = show;
    }
    
    public static boolean isShowChatNotifications() {
        return showChatNotifications;
    }
    
    public static void setShowChatNotifications(boolean show) {
        showChatNotifications = show;
    }
    
    public static boolean isShowFriendRequestNotifications() {
        return showFriendRequestNotifications;
    }
    
    public static void setShowFriendRequestNotifications(boolean show) {
        showFriendRequestNotifications = show;
    }
    
    public static boolean isShowSystemNotifications() {
        return showSystemNotifications;
    }
    
    public static void setShowSystemNotifications(boolean show) {
        showSystemNotifications = show;
    }
    
    public static boolean isShowSuccessNotifications() {
        return showSuccessNotifications;
    }
    
    public static void setShowSuccessNotifications(boolean show) {
        showSuccessNotifications = show;
    }
    
    /**
     * Set all notification types to minimal (only essential notifications)
     */
    public static void setMinimalNotifications() {
        showConnectionNotifications = false;
        showDebugNotifications = false;
        showChatNotifications = true;
        showFriendRequestNotifications = true;
        showSystemNotifications = true;
        showSuccessNotifications = false;
    }
    
    /**
     * Set all notification types to verbose (all notifications enabled)
     */
    public static void setVerboseNotifications() {
        showConnectionNotifications = true;
        showDebugNotifications = true;
        showChatNotifications = true;
        showFriendRequestNotifications = true;
        showSystemNotifications = true;
        showSuccessNotifications = true;
    }
    
    /**
     * Set default notification settings (balanced approach)
     */
    public static void setDefaultNotifications() {
        showConnectionNotifications = false;
        showDebugNotifications = false;
        showChatNotifications = true;
        showFriendRequestNotifications = true;
        showSystemNotifications = true;
        showSuccessNotifications = false;
    }
}
