package com.adorsys.webank.config.logging;

/**
 * Utility class for logging operations, including methods to mask
 * sensitive information like PII, passwords, and other secure data.
 */
public final class LoggingUtils {
    
    private LoggingUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Masks sensitive information like email addresses, phone numbers, or IDs.
     * 
     * @param value The value to mask
     * @return The masked value
     */
    public static String maskSensitiveInfo(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // Email masking: show only first character and domain
        if (value.contains("@")) {
            int atIndex = value.indexOf('@');
            if (atIndex > 0) {
                String firstChar = value.substring(0, 1);
                String domain = value.substring(atIndex);
                return firstChar + "****" + domain;
            }
        }
        
        // Phone number masking: show only last 4 digits
        if (value.matches("\\d{10,15}")) {
            return "******" + value.substring(Math.max(0, value.length() - 4));
        }
        
        // ID/Document number masking: show only first and last 2 characters
        if (value.length() > 4) {
            return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
        }
        
        // Default masking for other sensitive data
        if (value.length() > 2) {
            return value.substring(0, 1) + "****" + value.substring(value.length() - 1);
        }
        
        return "****";
    }
    
    /**
     * Completely masks a value (e.g., for passwords)
     * 
     * @param value The value to mask
     * @return The masked value
     */
    public static String maskCompletely(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return "********";
    }
} 