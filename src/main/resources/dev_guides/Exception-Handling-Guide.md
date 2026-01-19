# 🛡️ State-of-the-Art Exception Handling in Spring Boot

> **Goal:** Master exception handling to build robust, maintainable, and user-friendly APIs  
> **Stack:** Java 25, Spring Boot 4, Spring Framework 7, Hibernate 7  
> **Philosophy:** Exceptions should be predictable, informative, and never leak implementation details

---

## 📋 Table of Contents

1. [Understanding the Exception Flow](#1-understanding-the-exception-flow)
2. [The Exception Hierarchy Philosophy](#2-the-exception-hierarchy-philosophy)
3. [Building Your Exception Foundation](#3-building-your-exception-foundation)
4. [Response DTOs - The Contract with Your Frontend](#4-response-dtos---the-contract-with-your-frontend)
5. [The Global Exception Handler](#5-the-global-exception-handler)
6. [Handling Validation Exceptions](#6-handling-validation-exceptions)
7. [Security Exception Handling](#7-security-exception-handling)
8. [Database & JPA Exceptions](#8-database--jpa-exceptions)
9. [Problem Details (RFC 9457) - The Modern Standard](#9-problem-details-rfc-9457---the-modern-standard)
10. [Testing Exception Handling](#10-testing-exception-handling)
11. [Logging Strategy](#11-logging-strategy)
12. [Best Practices & Anti-Patterns](#12-best-practices--anti-patterns)
13. [Complete Implementation Reference](#13-complete-implementation-reference)

---

## 1. Understanding the Exception Flow

Before writing any code, let's understand how exceptions flow through a Spring application. This mental model is crucial!

### The Journey of an Exception

```
Client Request
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│                    FILTER CHAIN                             │
│  (Security Filters, Custom Filters)                         │
│  ⚠️ Exceptions here need special handling!                  │
└─────────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│                 DISPATCHER SERVLET                          │
│  Routes request to appropriate controller                   │
└─────────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│                    CONTROLLER                               │
│  @Valid triggers validation → MethodArgumentNotValidException│
│  Business logic exceptions bubble up from Service layer     │
└─────────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│                     SERVICE                                 │
│  Business validation → BusinessException                    │
│  Not found → ResourceNotFoundException                      │
│  Duplicates → DuplicateResourceException                    │
└─────────────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────────────┐
│                   REPOSITORY                                │
│  DataIntegrityViolationException (constraint violations)    │
│  EntityNotFoundException                                    │
│  OptimisticLockException                                    │
└─────────────────────────────────────────────────────────────┘
     │
     │ Exception thrown!
     ▼
┌─────────────────────────────────────────────────────────────┐
│              @RestControllerAdvice                          │
│         (GlobalExceptionHandler)                            │
│                                                             │
│  Catches exception → Transforms to ErrorResponse → Returns  │
└─────────────────────────────────────────────────────────────┘
     │
     ▼
   Client receives structured error response
```

### Key Insight: Exception Handling Layers

| Layer | Exceptions | Handler |
|-------|-----------|---------|
| **Filters** | Security, CORS | Custom `AuthenticationEntryPoint`, `AccessDeniedHandler` |
| **Controller** | Validation, Binding | `@RestControllerAdvice` |
| **Service** | Business logic | `@RestControllerAdvice` |
| **Repository** | Data access | `@RestControllerAdvice` |

---

## 2. The Exception Hierarchy Philosophy

### Why Build a Custom Hierarchy?

Think of exceptions like a classification system:

```
RuntimeException (Java)
    │
    └── BusinessException (Your Base)
            │
            ├── ResourceNotFoundException   → 404
            ├── DuplicateResourceException  → 409
            ├── BusinessValidationException → 400
            ├── InvalidCredentialsException → 401
            └── AccessDeniedException       → 403
```

**Benefits:**
- **Single catch** - Handle all business exceptions uniformly
- **Error codes** - Programmatic identification for frontends
- **HTTP status mapping** - Each type maps to a specific status
- **Consistent logging** - Know severity by exception type

### Checked vs Unchecked: The Decision

**Use Unchecked (RuntimeException) because:**

1. **Clean code** - No try-catch boilerplate throughout service layer
2. **Spring convention** - Spring uses unchecked exceptions
3. **Transaction handling** - Spring rolls back on unchecked exceptions by default
4. **Modern practice** - Most frameworks have moved to unchecked

```java
// ❌ Checked exception - forces try-catch everywhere
public User findUser(Long id) throws UserNotFoundException {
    return repo.findById(id).orElseThrow(() -> new UserNotFoundException(id));
}

// ✅ Unchecked exception - clean and Spring-friendly
public User findUser(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", id));
}
```

---

## 3. Building Your Exception Foundation

### Step 1: The Abstract Base Exception

```java
package com.yourapp.exception;

import lombok.Getter;

/**
 * Base exception for all business-related errors in the application.
 * 
 * Design decisions:
 * - Extends RuntimeException: No forced try-catch, Spring-friendly
 * - Contains errorCode: Machine-readable identifier for frontend
 * - Abstract: Forces creation of specific exception types
 * 
 * @see ResourceNotFoundException
 * @see DuplicateResourceException
 * @see BusinessValidationException
 */
@Getter
public abstract class BusinessException extends RuntimeException {
    
    /**
     * Machine-readable error code for frontend consumption.
     * Examples: "RESOURCE_NOT_FOUND", "DUPLICATE_EMAIL", "INVALID_STATE"
     */
    private final String errorCode;
    
    /**
     * Additional context data (optional).
     * Can be used for field-specific errors or extra details.
     */
    private final transient Object details;
    
    protected BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }
    
    protected BusinessException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    protected BusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }
}
```

### Step 2: Specific Exception Types

Each exception type represents a category of errors:

```java
package com.yourapp.exception;

/**
 * Thrown when a requested resource cannot be found.
 * 
 * HTTP Status: 404 Not Found
 * 
 * Usage:
 * - Entity not found by ID
 * - Resource doesn't exist
 * - Soft-deleted resource access attempt
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND");
    }
    
    /**
     * Convenience constructor with resource type and identifier.
     * Produces message like: "User not found with id: 42"
     */
    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(
            String.format("%s not found with id: %s", resourceType, identifier),
            "RESOURCE_NOT_FOUND"
        );
    }
    
    /**
     * For custom field lookups.
     * Produces message like: "User not found with email: john@example.com"
     */
    public ResourceNotFoundException(String resourceType, String field, Object value) {
        super(
            String.format("%s not found with %s: %s", resourceType, field, value),
            "RESOURCE_NOT_FOUND"
        );
    }
}
```

```java
package com.yourapp.exception;

/**
 * Thrown when attempting to create a resource that already exists.
 * 
 * HTTP Status: 409 Conflict
 * 
 * Usage:
 * - Duplicate email registration
 * - Unique constraint violation
 * - Resource already exists
 */
public class DuplicateResourceException extends BusinessException {
    
    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE");
    }
    
    /**
     * For field-specific duplicates.
     * Produces message like: "User with email 'john@example.com' already exists"
     */
    public DuplicateResourceException(String resourceType, String field, Object value) {
        super(
            String.format("%s with %s '%s' already exists", resourceType, field, value),
            "DUPLICATE_RESOURCE"
        );
    }
}
```

```java
package com.yourapp.exception;

import java.util.Map;

/**
 * Thrown when business rule validation fails.
 * 
 * HTTP Status: 400 Bad Request (or 422 Unprocessable Entity)
 * 
 * Usage:
 * - Invalid state transition
 * - Business rule violation
 * - Complex validation that can't be done with annotations
 */
public class BusinessValidationException extends BusinessException {
    
    public BusinessValidationException(String message) {
        super(message, "BUSINESS_VALIDATION_FAILED");
    }
    
    /**
     * For multiple field validation errors.
     */
    public BusinessValidationException(String message, Map<String, String> fieldErrors) {
        super(message, "BUSINESS_VALIDATION_FAILED", fieldErrors);
    }
    
    /**
     * For domain-specific error codes.
     */
    public BusinessValidationException(String message, String errorCode) {
        super(message, errorCode);
    }
}
```

```java
package com.yourapp.exception;

/**
 * Thrown when authentication credentials are invalid.
 * 
 * HTTP Status: 401 Unauthorized
 * 
 * Usage:
 * - Invalid username/password
 * - Expired credentials
 * - Account locked/disabled
 */
public class InvalidCredentialsException extends BusinessException {
    
    public InvalidCredentialsException(String message) {
        super(message, "INVALID_CREDENTIALS");
    }
    
    public InvalidCredentialsException() {
        super("Invalid email or password", "INVALID_CREDENTIALS");
    }
}
```

```java
package com.yourapp.exception;

/**
 * Thrown when user lacks permission to access a resource.
 * 
 * HTTP Status: 403 Forbidden
 * 
 * Usage:
 * - Accessing another user's resources
 * - Insufficient role/permissions
 * - Resource ownership violation
 */
public class AccessDeniedException extends BusinessException {
    
    public AccessDeniedException(String message) {
        super(message, "ACCESS_DENIED");
    }
    
    public AccessDeniedException() {
        super("You don't have permission to access this resource", "ACCESS_DENIED");
    }
}
```

```java
package com.yourapp.exception;

/**
 * Thrown when an operation violates the current state of a resource.
 * 
 * HTTP Status: 409 Conflict
 * 
 * Usage:
 * - Invalid state transition (e.g., canceling an already completed order)
 * - Concurrent modification conflict
 * - Resource in use
 */
public class InvalidStateException extends BusinessException {
    
    public InvalidStateException(String message) {
        super(message, "INVALID_STATE");
    }
    
    public InvalidStateException(String resource, String currentState, String attemptedAction) {
        super(
            String.format("Cannot %s %s in '%s' state", attemptedAction, resource, currentState),
            "INVALID_STATE"
        );
    }
}
```

### Directory Structure

```
src/main/java/com/yourapp/exception/
├── BusinessException.java              # Abstract base
├── ResourceNotFoundException.java      # 404
├── DuplicateResourceException.java     # 409
├── BusinessValidationException.java    # 400
├── InvalidCredentialsException.java    # 401
├── AccessDeniedException.java          # 403
├── InvalidStateException.java          # 409
├── dto/
│   ├── ErrorResponse.java              # Standard error response
│   ├── ValidationErrorResponse.java    # Validation errors
│   └── ProblemDetail.java              # RFC 9457 (optional)
└── handler/
    └── GlobalExceptionHandler.java     # @RestControllerAdvice
```

---

## 4. Response DTOs - The Contract with Your Frontend

### The Standard Error Response

```java
package com.yourapp.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Standard error response format for all API errors.
 * 
 * This is the contract between backend and frontend.
 * Frontend developers should be able to rely on this structure.
 * 
 * Example JSON:
 * {
 *     "status": 404,
 *     "errorCode": "RESOURCE_NOT_FOUND",
 *     "message": "User not found with id: 42",
 *     "path": "/api/users/42",
 *     "timestamp": "2025-01-19T10:30:00Z",
 *     "traceId": "abc123"
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response")
public record ErrorResponse(
    
    @Schema(description = "HTTP status code", example = "404")
    int status,
    
    @Schema(description = "Machine-readable error code", example = "RESOURCE_NOT_FOUND")
    String errorCode,
    
    @Schema(description = "Human-readable error message", example = "User not found with id: 42")
    String message,
    
    @Schema(description = "Request path that caused the error", example = "/api/users/42")
    String path,
    
    @Schema(description = "ISO-8601 timestamp", example = "2025-01-19T10:30:00Z")
    Instant timestamp,
    
    @Schema(description = "Trace ID for debugging (optional)", example = "abc123def456")
    String traceId
    
) {
    /**
     * Factory method for creating error responses.
     */
    public static ErrorResponse of(int status, String errorCode, String message, String path) {
        return new ErrorResponse(
            status,
            errorCode,
            message,
            path,
            Instant.now(),
            null  // traceId can be added via MDC in production
        );
    }
    
    /**
     * Factory method with trace ID for debugging.
     */
    public static ErrorResponse of(int status, String errorCode, String message, String path, String traceId) {
        return new ErrorResponse(status, errorCode, message, path, Instant.now(), traceId);
    }
}
```

### Validation Error Response

```java
package com.yourapp.exception.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Error response specifically for validation failures.
 * 
 * Provides field-level error details for frontend form handling.
 * 
 * Example JSON:
 * {
 *     "status": 400,
 *     "errorCode": "VALIDATION_FAILED",
 *     "message": "Validation failed for 3 fields",
 *     "path": "/api/users",
 *     "timestamp": "2025-01-19T10:30:00Z",
 *     "fieldErrors": [
 *         { "field": "email", "message": "must be a valid email", "rejectedValue": "invalid-email" },
 *         { "field": "password", "message": "must be at least 8 characters", "rejectedValue": null }
 *     ]
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Validation error response with field details")
public record ValidationErrorResponse(
    
    @Schema(description = "HTTP status code", example = "400")
    int status,
    
    @Schema(description = "Error code", example = "VALIDATION_FAILED")
    String errorCode,
    
    @Schema(description = "Summary message")
    String message,
    
    @Schema(description = "Request path")
    String path,
    
    @Schema(description = "Timestamp")
    Instant timestamp,
    
    @Schema(description = "Field-level validation errors")
    List<FieldError> fieldErrors
    
) {
    /**
     * Represents a single field validation error.
     */
    @Schema(description = "Field-level error detail")
    public record FieldError(
        
        @Schema(description = "Field name", example = "email")
        String field,
        
        @Schema(description = "Error message", example = "must be a valid email")
        String message,
        
        @Schema(description = "The rejected value (may be null for security)")
        Object rejectedValue
    ) {}
    
    /**
     * Factory method from field error list.
     */
    public static ValidationErrorResponse of(List<FieldError> fieldErrors, String path) {
        return new ValidationErrorResponse(
            400,
            "VALIDATION_FAILED",
            String.format("Validation failed for %d field(s)", fieldErrors.size()),
            path,
            Instant.now(),
            fieldErrors
        );
    }
    
    /**
     * Factory method from simple map (backward compatibility).
     */
    public static ValidationErrorResponse of(Map<String, String> errors, String path) {
        List<FieldError> fieldErrors = errors.entrySet().stream()
            .map(e -> new FieldError(e.getKey(), e.getValue(), null))
            .toList();
        return of(fieldErrors, path);
    }
}
```

---

## 5. The Global Exception Handler

This is the heart of your exception handling system:

```java
package com.yourapp.exception.handler;

import com.yourapp.exception.*;
import com.yourapp.exception.dto.ErrorResponse;
import com.yourapp.exception.dto.ValidationErrorResponse;
import com.yourapp.exception.dto.ValidationErrorResponse.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the entire application.
 * 
 * Design Philosophy:
 * 1. SPECIFIC exceptions handled FIRST (order matters!)
 * 2. NEVER expose internal details (stack traces, SQL, etc.)
 * 3. ALWAYS log enough for debugging
 * 4. CONSISTENT response format for frontend
 * 
 * Handler Organization:
 * 1. Business Exceptions (your custom exceptions)
 * 2. Validation Exceptions (Spring validation)
 * 3. Security Exceptions (authentication/authorization)
 * 4. Data Access Exceptions (JPA/Hibernate)
 * 5. HTTP/Web Exceptions (Spring MVC)
 * 6. Catch-All (unexpected errors)
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler {

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 1: BUSINESS EXCEPTIONS (Your Custom Exceptions)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Handles ResourceNotFoundException.
     * 
     * When: Entity not found by ID, resource doesn't exist
     * Status: 404 Not Found
     * Log Level: DEBUG (expected in normal operation)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        log.debug("Resource not found: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex, request);
    }
    
    /**
     * Handles DuplicateResourceException.
     * 
     * When: Duplicate email, unique constraint violation
     * Status: 409 Conflict
     * Log Level: WARN (indicates possible duplicate request or bad UX)
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {
        
        log.warn("Duplicate resource attempt: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        return buildErrorResponse(HttpStatus.CONFLICT, ex, request);
    }
    
    /**
     * Handles BusinessValidationException.
     * 
     * When: Business rule violation, invalid state transition
     * Status: 400 Bad Request (or 422 if you prefer)
     * Log Level: DEBUG (expected when invalid input)
     */
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidation(
            BusinessValidationException ex,
            HttpServletRequest request) {
        
        log.debug("Business validation failed: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex, request);
    }
    
    /**
     * Handles InvalidCredentialsException.
     * 
     * When: Wrong password, invalid login attempt
     * Status: 401 Unauthorized
     * Log Level: WARN (potential security concern - track for brute force)
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {
        
        // Don't log the actual credentials! Just the attempt
        log.warn("Invalid credentials attempt - Path: {} - IP: {}", 
            request.getRequestURI(), 
            getClientIP(request));
        
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex, request);
    }
    
    /**
     * Handles custom AccessDeniedException.
     * 
     * When: User tries to access another user's resource
     * Status: 403 Forbidden
     * Log Level: WARN (potential security concern)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        log.warn("Access denied: {} - Path: {} - IP: {}", 
            ex.getMessage(), 
            request.getRequestURI(),
            getClientIP(request));
        
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex, request);
    }
    
    /**
     * Handles InvalidStateException.
     * 
     * When: Invalid state transition, concurrent modification
     * Status: 409 Conflict
     * Log Level: WARN (indicates possible race condition or bad UX)
     */
    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(
            InvalidStateException ex,
            HttpServletRequest request) {
        
        log.warn("Invalid state: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        return buildErrorResponse(HttpStatus.CONFLICT, ex, request);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 2: VALIDATION EXCEPTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Handles @Valid annotation validation failures on @RequestBody.
     * 
     * When: Request body fails bean validation (@NotBlank, @Email, etc.)
     * Status: 400 Bad Request
     * Returns: ValidationErrorResponse with field-level details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        log.debug("Validation failed with {} errors - Path: {}", 
            ex.getErrorCount(), 
            request.getRequestURI());
        
        List<FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldError(
                error.getField(),
                error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                maskSensitiveValue(error.getField(), error.getRejectedValue())
            ))
            .toList();
        
        ValidationErrorResponse response = ValidationErrorResponse.of(fieldErrors, request.getRequestURI());
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handles @Validated constraint violations on path variables and params.
     * 
     * When: @PathVariable or @RequestParam fails validation
     * Status: 400 Bad Request
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        
        log.debug("Constraint violation - Path: {}", request.getRequestURI());
        
        List<FieldError> fieldErrors = ex.getConstraintViolations()
            .stream()
            .map(violation -> {
                String field = extractFieldName(violation.getPropertyPath().toString());
                return new FieldError(field, violation.getMessage(), null);
            })
            .toList();
        
        ValidationErrorResponse response = ValidationErrorResponse.of(fieldErrors, request.getRequestURI());
        
        return ResponseEntity.badRequest().body(response);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 3: SECURITY EXCEPTIONS (Spring Security)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Handles Spring Security AccessDeniedException.
     * 
     * When: @PreAuthorize fails, insufficient role
     * Status: 403 Forbidden
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringSecurityAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {
        
        log.warn("Spring Security access denied - Path: {} - IP: {}", 
            request.getRequestURI(), 
            getClientIP(request));
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "ACCESS_DENIED",
            "You don't have permission to access this resource",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 4: DATA ACCESS EXCEPTIONS (JPA/Hibernate)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Handles database constraint violations.
     * 
     * When: Unique constraint, foreign key violation
     * Status: 409 Conflict
     * 
     * IMPORTANT: Never expose SQL details to client!
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        
        // Log full details for debugging
        log.error("Data integrity violation - Path: {} - Cause: {}", 
            request.getRequestURI(), 
            ex.getMostSpecificCause().getMessage());
        
        // Determine user-friendly message based on exception content
        String userMessage = determineDataIntegrityMessage(ex);
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            "DATA_INTEGRITY_VIOLATION",
            userMessage,
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 5: HTTP/WEB EXCEPTIONS (Spring MVC)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Handles malformed JSON in request body.
     * 
     * When: Invalid JSON syntax, missing required fields
     * Status: 400 Bad Request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        
        log.debug("Malformed request body - Path: {}", request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "MALFORMED_REQUEST",
            "Unable to read request body. Please check JSON syntax.",
            request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handles unsupported HTTP methods.
     * 
     * When: POST to GET-only endpoint
     * Status: 405 Method Not Allowed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        
        log.debug("Method {} not supported for {} - Supported: {}", 
            ex.getMethod(), 
            request.getRequestURI(),
            ex.getSupportedHttpMethods());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            "METHOD_NOT_ALLOWED",
            String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }
    
    /**
     * Handles unsupported content types.
     * 
     * When: XML sent when JSON expected
     * Status: 415 Unsupported Media Type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        
        log.debug("Media type {} not supported - Path: {}", 
            ex.getContentType(), 
            request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            "UNSUPPORTED_MEDIA_TYPE",
            "Content type not supported. Please use application/json.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }
    
    /**
     * Handles missing required request parameters.
     * 
     * When: Required @RequestParam not provided
     * Status: 400 Bad Request
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        
        log.debug("Missing parameter '{}' - Path: {}", ex.getParameterName(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "MISSING_PARAMETER",
            String.format("Required parameter '%s' is missing", ex.getParameterName()),
            request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handles type mismatch in path variables or parameters.
     * 
     * When: String provided for Long path variable
     * Status: 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        log.debug("Type mismatch for '{}' - Path: {}", ex.getName(), request.getRequestURI());
        
        String expectedType = ex.getRequiredType() != null 
            ? ex.getRequiredType().getSimpleName() 
            : "unknown";
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "TYPE_MISMATCH",
            String.format("Parameter '%s' should be of type %s", ex.getName(), expectedType),
            request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handles requests to non-existent endpoints.
     * 
     * When: 404 for static resources or unknown paths
     * Status: 404 Not Found
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        
        log.debug("No resource found - Path: {}", request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "ENDPOINT_NOT_FOUND",
            "The requested endpoint does not exist",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 6: CATCH-ALL (Unexpected Errors)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Catch-all handler for unexpected exceptions.
     * 
     * This is your safety net. If an exception reaches here:
     * 1. It's a bug or unhandled edge case
     * 2. Log EVERYTHING for debugging
     * 3. Return NOTHING specific to client (security!)
     * 
     * Status: 500 Internal Server Error
     * Log Level: ERROR with full stack trace
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtExceptions(
            Exception ex,
            HttpServletRequest request) {
        
        // Log full stack trace - this is critical for debugging
        log.error("Unexpected error at {} - IP: {} - Exception: {}", 
            request.getRequestURI(),
            getClientIP(request),
            ex.getClass().getName(),
            ex);  // This logs the full stack trace
        
        // NEVER expose internal details to client!
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Builds error response from BusinessException.
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            BusinessException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = ErrorResponse.of(
            status.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(status).body(error);
    }
    
    /**
     * Extracts client IP, considering proxy headers.
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Extracts field name from property path (e.g., "createUser.email" → "email").
     */
    private String extractFieldName(String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return "unknown";
        }
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
    }
    
    /**
     * Masks sensitive field values in validation errors.
     * Never expose passwords in error responses!
     */
    private Object maskSensitiveValue(String fieldName, Object value) {
        if (fieldName == null || value == null) {
            return null;
        }
        
        String lowerField = fieldName.toLowerCase();
        if (lowerField.contains("password") || 
            lowerField.contains("secret") || 
            lowerField.contains("token") ||
            lowerField.contains("key")) {
            return "***MASKED***";
        }
        
        return value;
    }
    
    /**
     * Determines user-friendly message for data integrity violations.
     * Analyzes exception to provide context without exposing SQL.
     */
    private String determineDataIntegrityMessage(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        
        if (message == null) {
            return "A data conflict occurred. Please check your input.";
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Check for common constraint patterns
        if (lowerMessage.contains("duplicate") || lowerMessage.contains("unique")) {
            if (lowerMessage.contains("email")) {
                return "This email is already registered";
            }
            if (lowerMessage.contains("username")) {
                return "This username is already taken";
            }
            return "A record with this value already exists";
        }
        
        if (lowerMessage.contains("foreign key") || lowerMessage.contains("fk_")) {
            return "Referenced record does not exist or cannot be modified";
        }
        
        if (lowerMessage.contains("cannot be null") || lowerMessage.contains("not null")) {
            return "A required field is missing";
        }
        
        return "A data conflict occurred. Please check your input.";
    }
}
```

---

## 6. Handling Validation Exceptions

### Understanding the Validation Flow

```
@Valid @RequestBody CreateUserRequest request
     │
     │ Spring validates before controller method executes
     ▼
┌─────────────────────────────────────────────────────────────┐
│  Bean Validation (Hibernate Validator)                      │
│                                                             │
│  @NotBlank → Fails if null, empty, or whitespace           │
│  @Email    → Fails if not valid email format               │
│  @Size     → Fails if length outside bounds                │
│  @Pattern  → Fails if doesn't match regex                  │
└─────────────────────────────────────────────────────────────┘
     │
     │ Validation fails!
     ▼
MethodArgumentNotValidException thrown
     │
     ▼
GlobalExceptionHandler catches and transforms
     │
     ▼
ValidationErrorResponse returned to client
```

### Custom Validation Annotations

```java
package com.yourapp.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation: Strong password requirement.
 * 
 * Rules:
 * - At least 8 characters
 * - Contains digit, lowercase, uppercase, special char
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    
    String message() default "Password must be at least 8 characters with digit, lowercase, uppercase, and special character";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
```

```java
package com.yourapp.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;  // Let @NotBlank handle null case
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
```

### Using Validation in DTOs

```java
public record CreateUserRequest(
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    String username,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    String email,
    
    @NotBlank(message = "Password is required")
    @StrongPassword  // Custom annotation
    String password,
    
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be at most 50 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be at most 50 characters")
    String lastName,
    
    @Min(value = 13, message = "You must be at least 13 years old")
    @Max(value = 150, message = "Please enter a valid age")
    Integer age
) {}
```

---

## 7. Security Exception Handling

Security exceptions need special handling because they often occur BEFORE reaching controllers.

### Authentication Entry Point

```java
package com.yourapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourapp.exception.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles authentication failures (401 Unauthorized).
 * 
 * This kicks in when:
 * - No JWT token provided
 * - Invalid JWT token
 * - Expired JWT token
 * 
 * Note: This is called by the filter chain, BEFORE reaching any controller.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        
        log.warn("Authentication failed for {} - Reason: {} - IP: {}", 
            request.getRequestURI(),
            authException.getMessage(),
            getClientIP(request));
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.UNAUTHORIZED.value(),
            "AUTHENTICATION_REQUIRED",
            "Authentication is required to access this resource",
            request.getRequestURI()
        );
        
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### Access Denied Handler

```java
package com.yourapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourapp.exception.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles authorization failures (403 Forbidden).
 * 
 * This kicks in when:
 * - User is authenticated but lacks required role
 * - @PreAuthorize fails
 * - URL pattern requires specific authority
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        
        log.warn("Access denied for {} - User attempted unauthorized action - IP: {}", 
            request.getRequestURI(),
            request.getRemoteAddr());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "ACCESS_DENIED",
            "You don't have permission to access this resource",
            request.getRequestURI()
        );
        
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
```

### Integrating with Security Config

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint authEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            // Register custom handlers!
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

---

## 8. Database & JPA Exceptions

### Common JPA/Hibernate Exceptions

| Exception | Cause | Handling |
|-----------|-------|----------|
| `DataIntegrityViolationException` | Unique constraint, FK violation | 409 Conflict |
| `EntityNotFoundException` | `getReference()` on non-existent | 404 Not Found |
| `OptimisticLockException` | Concurrent modification | 409 Conflict |
| `TransactionSystemException` | Transaction failure | 500 (investigate!) |

### Handling Optimistic Locking

```java
@ExceptionHandler(OptimisticLockException.class)
public ResponseEntity<ErrorResponse> handleOptimisticLock(
        OptimisticLockException ex,
        HttpServletRequest request) {
    
    log.warn("Optimistic lock conflict at {} - Someone else modified this resource", 
        request.getRequestURI());
    
    ErrorResponse error = ErrorResponse.of(
        HttpStatus.CONFLICT.value(),
        "CONCURRENT_MODIFICATION",
        "This resource was modified by another user. Please refresh and try again.",
        request.getRequestURI()
    );
    
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
}
```

---

## 9. Problem Details (RFC 9457) - The Modern Standard

RFC 9457 (formerly RFC 7807) defines a standard format for HTTP error responses. Spring 6+ has built-in support!

### Enabling Problem Details

```java
@Configuration
public class ExceptionConfig {
    
    @Bean
    public ResponseEntityExceptionHandler responseEntityExceptionHandler() {
        return new ResponseEntityExceptionHandler() {};
    }
}
```

```yaml
# application.yml
spring:
  mvc:
    problemdetails:
      enabled: true
```

### Custom Problem Detail Implementation

```java
package com.yourapp.exception.dto;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/**
 * RFC 9457 Problem Details implementation.
 * 
 * Standard fields:
 * - type: URI identifying the problem type
 * - title: Short human-readable summary
 * - status: HTTP status code
 * - detail: Human-readable explanation
 * - instance: URI reference to specific occurrence
 * 
 * Extensions (custom fields):
 * - errorCode: Machine-readable code
 * - timestamp: When error occurred
 * - errors: Field-level errors (for validation)
 */
public record ProblemDetail(
    URI type,
    String title,
    int status,
    String detail,
    URI instance,
    String errorCode,
    Instant timestamp,
    Map<String, Object> extensions
) {
    private static final URI DEFAULT_TYPE = URI.create("about:blank");
    
    public static ProblemDetail forStatusAndDetail(int status, String detail) {
        return new ProblemDetail(
            DEFAULT_TYPE,
            getDefaultTitle(status),
            status,
            detail,
            null,
            null,
            Instant.now(),
            null
        );
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    private static String getDefaultTitle(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 500 -> "Internal Server Error";
            default -> "Error";
        };
    }
    
    public static class Builder {
        private URI type = DEFAULT_TYPE;
        private String title;
        private int status;
        private String detail;
        private URI instance;
        private String errorCode;
        private Map<String, Object> extensions;
        
        public Builder type(URI type) { this.type = type; return this; }
        public Builder type(String type) { this.type = URI.create(type); return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder status(int status) { this.status = status; return this; }
        public Builder detail(String detail) { this.detail = detail; return this; }
        public Builder instance(URI instance) { this.instance = instance; return this; }
        public Builder instance(String instance) { this.instance = URI.create(instance); return this; }
        public Builder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
        public Builder extensions(Map<String, Object> extensions) { this.extensions = extensions; return this; }
        
        public ProblemDetail build() {
            return new ProblemDetail(
                type,
                title != null ? title : getDefaultTitle(status),
                status,
                detail,
                instance,
                errorCode,
                Instant.now(),
                extensions
            );
        }
    }
}
```

---

## 10. Testing Exception Handling

### Unit Testing Exceptions in Services

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Exception Tests")
class UserServiceExceptionTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("getUserById - when user not found - throws ResourceNotFoundException")
    void getUserById_WhenNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("not found")
            .hasMessageContaining("999")
            .satisfies(ex -> {
                ResourceNotFoundException rnfe = (ResourceNotFoundException) ex;
                assertThat(rnfe.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
            });
        
        verify(userRepository).findById(999L);
    }
    
    @Test
    @DisplayName("registerUser - when email exists - throws DuplicateResourceException")
    void registerUser_WhenEmailExists_ThrowsDuplicateResourceException() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest(
            "johndoe", "john@example.com", "Password123!", "John", "Doe", 25
        );
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(request))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("email")
            .satisfies(ex -> {
                DuplicateResourceException dre = (DuplicateResourceException) ex;
                assertThat(dre.getErrorCode()).isEqualTo("DUPLICATE_RESOURCE");
            });
        
        verify(userRepository, never()).save(any());
    }
}
```

### Integration Testing the Exception Handler

```java
@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("UserController Exception Handling Tests")
class UserControllerExceptionTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private UserService userService;
    
    @Test
    @DisplayName("GET /api/users/{id} - when not found - returns 404 with error response")
    void getUser_WhenNotFound_Returns404() throws Exception {
        // Arrange
        when(userService.getUserById(999L))
            .thenThrow(new ResourceNotFoundException("User", 999L));
        
        // Act & Assert
        mockMvc.perform(get("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("User not found with id: 999"))
            .andExpect(jsonPath("$.path").value("/api/users/999"))
            .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    @DisplayName("POST /api/users - with invalid email - returns 400 with validation errors")
    void createUser_WithInvalidEmail_Returns400() throws Exception {
        // Arrange
        String invalidRequest = """
            {
                "username": "johndoe",
                "email": "invalid-email",
                "password": "Password123!",
                "firstName": "John",
                "lastName": "Doe",
                "age": 25
            }
            """;
        
        // Act & Assert
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors").isArray())
            .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }
    
    @Test
    @DisplayName("POST /api/users - with duplicate email - returns 409")
    void createUser_WithDuplicateEmail_Returns409() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest(
            "johndoe", "existing@example.com", "Password123!", "John", "Doe", 25
        );
        when(userService.registerUser(any()))
            .thenThrow(new DuplicateResourceException("User", "email", "existing@example.com"));
        
        // Act & Assert
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }
    
    @Test
    @DisplayName("POST /api/users - with malformed JSON - returns 400")
    void createUser_WithMalformedJson_Returns400() throws Exception {
        // Arrange
        String malformedJson = "{ invalid json }";
        
        // Act & Assert
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }
    
    @Test
    @DisplayName("GET /api/users/abc - type mismatch - returns 400")
    void getUser_WithInvalidIdType_Returns400() throws Exception {
        mockMvc.perform(get("/api/users/abc")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }
}
```

---

## 11. Logging Strategy

### Log Levels for Exceptions

| Exception Type | Log Level | Rationale |
|---------------|-----------|-----------|
| `ResourceNotFoundException` | DEBUG | Expected in normal operation |
| `ValidationException` | DEBUG | User input error, expected |
| `DuplicateResourceException` | WARN | Might indicate UX issue |
| `InvalidCredentialsException` | WARN | Security monitoring needed |
| `AccessDeniedException` | WARN | Security monitoring needed |
| `DataIntegrityViolationException` | ERROR | Unexpected, needs investigation |
| `Exception` (catch-all) | ERROR | Unexpected, needs investigation |

### Structured Logging

```java
@Slf4j
public class GlobalExceptionHandler {
    
    // Use structured logging for better searchability
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        // Structured logging with key-value pairs
        log.debug("Resource not found - path={} - errorCode={} - message={}", 
            request.getRequestURI(),
            ex.getErrorCode(),
            ex.getMessage());
        
        // ...
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtExceptions(
            Exception ex,
            HttpServletRequest request) {
        
        // For unexpected errors, log everything
        log.error("Unexpected error - path={} - ip={} - method={} - exception={}", 
            request.getRequestURI(),
            getClientIP(request),
            request.getMethod(),
            ex.getClass().getName(),
            ex);  // Full stack trace
        
        // ...
    }
}
```

---

## 12. Best Practices & Anti-Patterns

### ✅ DO

```java
// 1. Use specific exceptions
throw new ResourceNotFoundException("User", userId);

// 2. Include context in messages
throw new BusinessValidationException(
    String.format("Cannot delete category '%s' with %d active items", 
        categoryName, itemCount));

// 3. Use factory methods for consistent error responses
ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), path);

// 4. Log appropriately based on severity
log.debug("Resource not found: {}", ex.getMessage());  // Expected
log.error("Unexpected error", ex);  // Unexpected

// 5. Use @Transactional correctly
@Transactional
public void transferFunds(...) {
    // Exception here causes automatic rollback
    debitAccount(...);
    creditAccount(...);
}
```

### ❌ DON'T

```java
// 1. Don't expose internal details
throw new RuntimeException("SQL Error: " + sqlException.getMessage()); // ❌
throw new BusinessValidationException("Unable to process request"); // ✅

// 2. Don't catch and re-throw without value
try {
    // ...
} catch (Exception e) {
    throw e;  // ❌ Pointless, let it bubble up naturally
}

// 3. Don't use generic exceptions
throw new RuntimeException("User not found");  // ❌
throw new ResourceNotFoundException("User", userId);  // ✅

// 4. Don't log and throw
log.error("Error occurred", e);
throw e;  // ❌ Leads to duplicate logs

// 5. Don't swallow exceptions
try {
    // ...
} catch (Exception e) {
    return null;  // ❌ Hides the error!
}

// 6. Don't expose stack traces to clients
return ResponseEntity.status(500)
    .body(Map.of("error", exception.getStackTrace()));  // ❌ Security risk!
```

---

## 13. Complete Implementation Reference

### File Structure

```
src/main/java/com/yourapp/
├── exception/
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── BusinessValidationException.java
│   ├── InvalidCredentialsException.java
│   ├── AccessDeniedException.java
│   ├── InvalidStateException.java
│   ├── dto/
│   │   ├── ErrorResponse.java
│   │   └── ValidationErrorResponse.java
│   └── handler/
│       └── GlobalExceptionHandler.java
├── security/
│   ├── JwtAuthenticationEntryPoint.java
│   └── JwtAccessDeniedHandler.java
└── validation/
    ├── StrongPassword.java
    └── StrongPasswordValidator.java
```

### Quick Reference: Exception to HTTP Status

| Exception | HTTP Status | Error Code |
|-----------|-------------|------------|
| `ResourceNotFoundException` | 404 | RESOURCE_NOT_FOUND |
| `DuplicateResourceException` | 409 | DUPLICATE_RESOURCE |
| `BusinessValidationException` | 400 | BUSINESS_VALIDATION_FAILED |
| `InvalidCredentialsException` | 401 | INVALID_CREDENTIALS |
| `AccessDeniedException` | 403 | ACCESS_DENIED |
| `InvalidStateException` | 409 | INVALID_STATE |
| `MethodArgumentNotValidException` | 400 | VALIDATION_FAILED |
| `ConstraintViolationException` | 400 | VALIDATION_FAILED |
| `DataIntegrityViolationException` | 409 | DATA_INTEGRITY_VIOLATION |
| `HttpMessageNotReadableException` | 400 | MALFORMED_REQUEST |
| `HttpRequestMethodNotSupportedException` | 405 | METHOD_NOT_ALLOWED |
| `Exception` (catch-all) | 500 | INTERNAL_ERROR |

---

## Summary

You now have a complete understanding of exception handling in Spring Boot:

1. **Hierarchy** - Custom exceptions extending a base class
2. **Response DTOs** - Consistent format for frontend
3. **Global Handler** - Centralized exception processing
4. **Validation** - Bean validation with custom annotations
5. **Security** - Entry points and access denied handlers
6. **Database** - JPA exception handling
7. **Testing** - Unit and integration tests
8. **Logging** - Appropriate levels for each type

**Remember the golden rules:**
- Never expose internal details to clients
- Log everything you need for debugging
- Be consistent in your response format
- Use specific exceptions over generic ones
- Test your exception handlers!

Happy coding! 🚀
