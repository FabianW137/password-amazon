// src/main/java/com/example/alexahttps/GlobalErrorHandler.java
package com.example.alexahttps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String,Object> handleSecurity(SecurityException ex){
        log.warn("Alexa security check failed: {}", ex.getMessage(), ex);
        return Map.of("ok", false, "error", "security", "message", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String,Object> handleGeneric(Exception ex){
        log.error("Unhandled error in /alexa", ex);
        return Map.of("ok", false, "error", "internal");
    }
}
