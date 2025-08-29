package com.system.chattalkdesktop.service;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Event Bus implementation for frontend decoupled communication
 * Implements the Observer pattern for real-time updates
 */
@Slf4j
public class EventBus {

    private static volatile EventBus instance;
    private final Map<Class<?>, List<Consumer<?>>> handlers = new ConcurrentHashMap<>();
    private final AtomicInteger eventCounter = new AtomicInteger(0);
    private static final int MAX_EVENTS_PER_BATCH = 10;

    private EventBus() {}

    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }

    /**
     * Publish an event to all registered handlers
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(T event) {
        if (event == null) {
            log.warn("Attempted to publish null event");
            return;
        }

        List<Consumer<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null && !eventHandlers.isEmpty()) {
            log.debug("Publishing event {} to {} handlers", event.getClass().getSimpleName(), eventHandlers.size());
            
            // Check if we're already on JavaFX thread to avoid unnecessary Platform.runLater calls
            boolean isOnFxThread = Platform.isFxApplicationThread();
            
            eventHandlers.forEach(handler -> {
                try {
                    if (isOnFxThread) {
                        // Direct execution if already on JavaFX thread
                        ((Consumer<T>) handler).accept(event);
                    } else {
                        // Batch UI updates to reduce thread switching overhead
                        if (eventCounter.incrementAndGet() <= MAX_EVENTS_PER_BATCH) {
                            Platform.runLater(() -> {
                                try {
                                    ((Consumer<T>) handler).accept(event);
                                } catch (Exception e) {
                                    log.error("Error handling event {} on JavaFX thread: {}", 
                                        event.getClass().getSimpleName(), e.getMessage(), e);
                                }
                            });
                        } else {
                            // Reset counter and execute immediately for high-frequency events
                            eventCounter.set(0);
                            ((Consumer<T>) handler).accept(event);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error handling event {}: {}", event.getClass().getSimpleName(), e.getMessage(), e);
                }
            });
        }
    }

    /**
     * Subscribe to events of a specific type
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        if (eventType == null || handler == null) {
            log.warn("Cannot subscribe with null eventType or handler");
            return;
        }

        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        log.debug("Subscribed to events of type: {}", eventType.getSimpleName());
    }

    /**
     * Unsubscribe from events of a specific type
     */
    public <T> void unsubscribe(Class<T> eventType, Consumer<T> handler) {
        if (eventType == null || handler == null) {
            return;
        }

        List<Consumer<?>> eventHandlers = handlers.get(eventType);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
            log.debug("Unsubscribed from events of type: {}", eventType.getSimpleName());
        }
    }

    /**
     * Get the number of handlers for a specific event type
     */
    public int getHandlerCount(Class<?> eventType) {
        List<Consumer<?>> eventHandlers = handlers.get(eventType);
        return eventHandlers != null ? eventHandlers.size() : 0;
    }

    /**
     * Clear all handlers
     */
    public void clear() {
        handlers.clear();
        eventCounter.set(0);
        log.info("EventBus cleared all handlers");
    }

    /**
     * Get total number of event types registered
     */
    public int getEventTypeCount() {
        return handlers.size();
    }
}


