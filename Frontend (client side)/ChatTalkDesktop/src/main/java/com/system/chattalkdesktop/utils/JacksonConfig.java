package com.system.chattalkdesktop.utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class to configure Jackson ObjectMapper with proper modules
 * for handling Java 8 date/time types like LocalDateTime
 */
public class JacksonConfig {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Register JSR310 module for Java 8 date/time support
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Get the configured ObjectMapper instance
     * @return configured ObjectMapper with JSR310 support
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Create a new configured ObjectMapper instance
     * @return new configured ObjectMapper with JSR310 support
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
