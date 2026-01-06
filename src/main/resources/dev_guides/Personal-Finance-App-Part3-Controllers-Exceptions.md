# 🚀 Personal Finance App - Learning Roadmap Part 3
## Phases 5-9: Controllers, Security, Testing & Production

> **Continuation from Parts 1 & 2** - Complete Phases 1-4 before starting here!

---

## Phase 5: Controller Layer - API Endpoints

**Duration:** 1-2 weeks  
**Goal:** Build RESTful API controllers following best practices  
**Difficulty:** ⭐⭐⭐

### 📚 What You'll Learn

- RESTful API design principles
- HTTP methods and status codes
- Request/response handling
- Path variables and request parameters
- Request body validation with @Valid
- ResponseEntity for fine-grained control
- API documentation with comments

### 🎯 Controller Responsibilities

**Controllers are THIN - they delegate to services!**

```
Controller Job:
1. Receive HTTP request
2. Validate input (basic validation via @Valid)
3. Call service
4. Return HTTP response

That's it! No business logic in controllers.
```

**Think of controllers as traffic cops:**
- Direct traffic (route requests)
- Check basics (validation)
- Delegate work (to services)
- Report results (HTTP responses)

---

### HTTP Status Codes You'll Use

```java
// SUCCESS
200 OK - GET/PUT successful
201 CREATED - POST successful
204 NO CONTENT - DELETE successful

// CLIENT ERRORS
400 BAD REQUEST - Validation failed
401 UNAUTHORIZED - Not authenticated
403 FORBIDDEN - Not authorized
404 NOT FOUND - Resource doesn't exist
409 CONFLICT - Duplicate resource

// SERVER ERRORS
500 INTERNAL SERVER ERROR - Something broke
```

---

### Complete Example: UserController

```java
/**
 * REST API Controller for User management.
 * 
 * Base URL: /api/users
 * 
 * Endpoints:
 * - POST   /api/users              → Register new user
 * - GET    /api/users/{id}         → Get user by ID
 * - GET    /api/users              → Get all users (paginated)
 * - GET    /api/users/search       → Search users
 * - PUT    /api/users/{id}         → Update user
 * - DELETE /api/users/{id}         → Delete user
 * - POST   /api/users/{id}/password → Change password
 * 
 * Response Patterns:
 * - 200 OK for successful GET/PUT
 * - 201 CREATED for successful POST
 * - 204 NO CONTENT for successful DELETE
 * - 4xx for client errors
 * - 5xx for server errors (handled by @ControllerAdvice)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated  // Enables method-level validation
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    // ========== CREATE ==========
    
    /**
     * Registers a new user.
     * 
     * @Valid annotation:
     * - Triggers validation on CreateUserRequest
     * - If validation fails, throws MethodArgumentNotValidException
     * - @ControllerAdvice handles it and returns 400 BAD REQUEST
     * 
     * ResponseEntity.created():
     * - Returns 201 CREATED status
     * - Includes Location header: /api/users/{id}
     * - Body contains created user
     * 
     * @param request validated registration request
     * @return 201 CREATED with user response
     */
    @PostMapping
    public ResponseEntity<UserResponse> registerUser(
            @Valid @RequestBody CreateUserRequest request) {
        
        log.info("POST /api/users - Registering user: {}", request.email());
        
        UserResponse response = userService.registerUser(request);
        
        // Build Location header: /api/users/123
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        
        return ResponseEntity.created(location).body(response);
    }
    
    // ========== READ ==========
    
    /**
     * Gets user by ID.
     * 
     * @PathVariable:
     * - Extracts {id} from URL
     * - Type conversion automatic (String → Long)
     * - 400 BAD REQUEST if conversion fails
     * 
     * @param id user ID from URL path
     * @return 200 OK with user response
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("GET /api/users/{}", id);
        
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets all users with pagination and sorting.
     * 
     * @RequestParam:
     * - Extracts query parameters from URL
     * - defaultValue provides fallback
     * - required = false makes parameter optional
     * 
     * Example URLs:
     * - /api/users?page=0&size=10
     * - /api/users?page=1&size=20&sortBy=lastName&direction=DESC
     * 
     * @param page page number (default 0)
     * @param size page size (default 20)
     * @param sortBy field to sort by (default "id")
     * @param direction sort direction (default "ASC")
     * @return 200 OK with paginated users
     */
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        
        log.info("GET /api/users?page={}&size={}", page, size);
        
        Page<UserResponse> users = userService.getAllUsers(page, size, sortBy, direction);
        return ResponseEntity.ok(users);
    }
    
    /**
     * Searches users by name.
     * 
     * Separate endpoint for search (not query param on GET /users):
     * - More RESTful
     * - Clearer intent
     * - Easier to add complex search later
     * 
     * @param term search term
     * @return 200 OK with matching users
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam String term) {
        
        log.info("GET /api/users/search?term={}", term);
        
        List<UserResponse> users = userService.searchUsers(term);
        return ResponseEntity.ok(users);
    }
    
    // ========== UPDATE ==========
    
    /**
     * Updates user profile.
     * 
     * PUT vs PATCH:
     * - PUT: Replace entire resource
     * - PATCH: Update partial fields
     * 
     * We use PUT here but only update certain fields (hybrid approach).
     * True REST purists would use PATCH.
     * 
     * @param id user ID
     * @param request validated update request
     * @return 200 OK with updated user
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        
        log.info("PUT /api/users/{}", id);
        
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Changes user password.
     * 
     * Separate endpoint for security-sensitive operation.
     * POST not PUT because it's an action, not resource update.
     * 
     * @param id user ID
     * @param request password change request
     * @return 204 NO CONTENT (no body needed)
     */
    @PostMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        log.info("POST /api/users/{}/password", id);
        
        userService.changePassword(id, request);
        
        // 204 NO CONTENT - operation successful, no body to return
        return ResponseEntity.noContent().build();
    }
    
    // ========== DELETE ==========
    
    /**
     * Deletes user.
     * 
     * DELETE method always returns:
     * - 204 NO CONTENT on success
     * - 404 NOT FOUND if doesn't exist
     * 
     * @param id user ID
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/users/{}", id);
        
        userService.deleteUser(id);
        
        return ResponseEntity.noContent().build();
    }
}
```

---

### Controller Best Practices

**1. Keep Controllers Thin**
```java
// ✅ GOOD - Delegates to service
@PostMapping
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    UserResponse response = userService.createUser(request);
    return ResponseEntity.created(location).body(response);
}

// ❌ BAD - Business logic in controller
@PostMapping
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    if (userRepository.existsByEmail(request.email())) {  // ❌ Business logic!
        throw new DuplicateResourceException("Email exists");
    }
    User user = request.toEntity();  // ❌ Conversion logic!
    user.setPassword(encoder.encode(user.getPassword()));  // ❌ Business logic!
    // ... more logic that should be in service
}
```

**2. Always Use @Valid for Request Bodies**
```java
// ✅ GOOD - Validation triggered
@PostMapping
public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request) {
    // If validation fails, 400 BAD REQUEST automatically returned
}

// ❌ BAD - No validation
@PostMapping
public ResponseEntity<UserResponse> createUser(
        @RequestBody CreateUserRequest request) {  // Missing @Valid!
    // Invalid data gets to service layer
}
```

**3. Use Appropriate HTTP Methods and Status Codes**
```java
// ✅ GOOD - Correct methods and status codes
@PostMapping          // CREATE
public ResponseEntity<T> create(...) {
    return ResponseEntity.status(HttpStatus.CREATED).body(resource);  // 201
}

@GetMapping          // READ
public ResponseEntity<T> get(...) {
    return ResponseEntity.ok(resource);  // 200
}

@PutMapping          // UPDATE
public ResponseEntity<T> update(...) {
    return ResponseEntity.ok(updated);  // 200
}

@DeleteMapping       // DELETE
public ResponseEntity<Void> delete(...) {
    return ResponseEntity.noContent().build();  // 204
}

// ❌ BAD - Wrong methods/status codes
@GetMapping          // Wrong method for creation!
public ResponseEntity<T> create(...) {
    return ResponseEntity.ok(resource);  // Wrong status code!
}
```

**4. Use ResponseEntity for Control**
```java
// ✅ GOOD - Full control over response
@PostMapping
public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
    UserResponse response = service.create(request);
    URI location = ServletUriComponentsBuilder
        .fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(response.id())
        .toUri();
    return ResponseEntity.created(location).body(response);  // 201 + Location header
}

// ⚠️ WORKS but less control
@PostMapping
public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
    return service.create(request);  // Always 200, no Location header
}
```

---

### Practice Exercise 9: ItemController (Guided)

```java
/**
 * TODO: Complete this ItemController
 * 
 * REQUIREMENTS:
 * 1. Add @RestController, @RequestMapping("/api/items")
 * 2. Add @RequiredArgsConstructor, @Validated, @Slf4j
 * 3. Inject ItemService
 * 4. Implement all CRUD endpoints
 * 5. Use proper HTTP methods and status codes
 * 6. Add validation where needed
 * 
 * SECURITY NOTE:
 * In a real app, userId would come from authentication (JWT token).
 * For now, we'll pass it as a header or path variable.
 * We'll fix this in Phase 7 (Security).
 * 
 * ENDPOINTS TO IMPLEMENT:
 * - POST   /api/items              → Create item
 * - GET    /api/items/{id}         → Get item by ID
 * - GET    /api/items              → Get all items for user
 * - GET    /api/items/search       → Search items
 * - PUT    /api/items/{id}         → Update item
 * - DELETE /api/items/{id}         → Delete item
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ItemController {
    
    private final ItemService itemService;
    
    /**
     * TODO: Implement createItem
     * 
     * Steps:
     * 1. Use @PostMapping
     * 2. Accept @RequestHeader("User-Id") Long userId
     * 3. Accept @Valid @RequestBody CreateItemRequest
     * 4. Log the request
     * 5. Call itemService.createItem(userId, request)
     * 6. Build Location header
     * 7. Return ResponseEntity.created(location).body(response)
     * 
     * Example:
     * POST /api/items
     * Header: User-Id: 123
     * Body: { "name": "Apple", "itemUnit": "PIECE", ... }
     * Response: 201 CREATED with Location: /api/items/456
     */
    // TODO: Implement createItem method
    
    
    /**
     * TODO: Implement getItemById
     * 
     * Steps:
     * 1. Use @GetMapping("/{id}")
     * 2. Accept @PathVariable Long id
     * 3. Accept @RequestHeader("User-Id") Long userId
     * 4. Log the request
     * 5. Call itemService.getItemById(id, userId)
     * 6. Return ResponseEntity.ok(response)
     * 
     * Example:
     * GET /api/items/456
     * Header: User-Id: 123
     * Response: 200 OK with item details
     */
    // TODO: Implement getItemById method
    
    
    /**
     * TODO: Implement getAllItems
     * 
     * Steps:
     * 1. Use @GetMapping
     * 2. Accept @RequestHeader("User-Id") Long userId
     * 3. Call itemService.getAllItemsForUser(userId)
     * 4. Return ResponseEntity.ok(items)
     * 
     * Example:
     * GET /api/items
     * Header: User-Id: 123
     * Response: 200 OK with list of items
     */
    // TODO: Implement getAllItems method
    
    
    /**
     * TODO: Implement searchItems
     * 
     * Steps:
     * 1. Use @GetMapping("/search")
     * 2. Accept @RequestParam String term
     * 3. Accept @RequestHeader("User-Id") Long userId
     * 4. Call itemService.searchItems(userId, term)
     * 5. Return ResponseEntity.ok(results)
     * 
     * Example:
     * GET /api/items/search?term=apple
     * Header: User-Id: 123
     * Response: 200 OK with matching items
     */
    // TODO: Implement searchItems method
    
    
    /**
     * TODO: Implement updateItem
     * 
     * Steps:
     * 1. Use @PutMapping("/{id}")
     * 2. Accept @PathVariable Long id
     * 3. Accept @RequestHeader("User-Id") Long userId
     * 4. Accept @Valid @RequestBody UpdateItemRequest
     * 5. Call itemService.updateItem(id, userId, request)
     * 6. Return ResponseEntity.ok(response)
     * 
     * Example:
     * PUT /api/items/456
     * Header: User-Id: 123
     * Body: { "name": "Updated Name", ... }
     * Response: 200 OK with updated item
     */
    // TODO: Implement updateItem method
    
    
    /**
     * TODO: Implement deleteItem
     * 
     * Steps:
     * 1. Use @DeleteMapping("/{id}")
     * 2. Accept @PathVariable Long id
     * 3. Accept @RequestHeader("User-Id") Long userId
     * 4. Call itemService.deleteItem(id, userId)
     * 5. Return ResponseEntity.noContent().build()
     * 
     * Example:
     * DELETE /api/items/456
     * Header: User-Id: 123
     * Response: 204 NO CONTENT
     */
    // TODO: Implement deleteItem method
}
```

---

### Controller Phase Checklist

Before moving to Phase 6:
- [ ] @RestController on all controllers
- [ ] @RequestMapping with base path
- [ ] Proper HTTP methods (@GetMapping, @PostMapping, etc.)
- [ ] @Valid on all request bodies
- [ ] @PathVariable for URL parameters
- [ ] @RequestParam for query parameters
- [ ] Appropriate HTTP status codes
- [ ] Location header for POST
- [ ] Logging for all endpoints
- [ ] Controllers delegate to services (no business logic)

---

## Phase 6: Exception Handling - Error Management

**Duration:** 3-4 days  
**Goal:** Implement global exception handling with proper error responses  
**Difficulty:** ⭐⭐⭐

### 📚 What You'll Learn

- Custom exception classes
- Global exception handling with @ControllerAdvice
- Error response DTOs
- HTTP status code mapping
- Validation error handling
- Logging exceptions

### 🎯 Why Exception Handling Matters

**Without proper exception handling:**
```json
// User gets this ugly response:
{
  "timestamp": "2024-12-31T10:15:30",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/users/999"
}
```

**With proper exception handling:**
```json
// User gets this helpful response:
{
  "status": 404,
  "message": "User not found with id: 999",
  "timestamp": "2024-12-31T10:15:30",
  "path": "/api/users/999"
}
```

---

### Complete Example: Custom Exceptions

```java
/**
 * Base exception for all business exceptions.
 * All custom exceptions extend this.
 */
public abstract class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Thrown when a requested resource doesn't exist.
 * Maps to 404 NOT FOUND.
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with id: %d", resourceName, id));
    }
}

/**
 * Thrown when trying to create duplicate resource.
 * Maps to 409 CONFLICT.
 */
public class DuplicateResourceException extends BusinessException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
}

/**
 * Thrown when business validation fails.
 * Maps to 400 BAD REQUEST.
 */
public class BusinessValidationException extends BusinessException {
    
    public BusinessValidationException(String message) {
        super(message);
    }
}

/**
 * Thrown for authentication/authorization failures.
 * Maps to 401 UNAUTHORIZED or 403 FORBIDDEN.
 */
public class SecurityException extends BusinessException {
    
    public SecurityException(String message) {
        super(message);
    }
}

/**
 * Thrown when password validation fails.
 * Maps to 400 BAD REQUEST.
 */
public class InvalidPasswordException extends BusinessException {
    
    public InvalidPasswordException(String message) {
        super(message);
    }
}
```

---

### Error Response DTOs

```java
/**
 * Standard error response format.
 * Returned for all exceptions.
 */
public record ErrorResponse(
    int status,
    String message,
    LocalDateTime timestamp,
    String path
) {
    public static ErrorResponse of(
            HttpStatus status,
            String message,
            String path) {
        
        return new ErrorResponse(
            status.value(),
            message,
            LocalDateTime.now(),
            path
        );
    }
}

/**
 * Validation error response.
 * Includes field-specific errors.
 */
public record ValidationErrorResponse(
    int status,
    String message,
    Map<String, String> fieldErrors,
    LocalDateTime timestamp,
    String path
) {
    public static ValidationErrorResponse of(
            String message,
            Map<String, String> fieldErrors,
            String path) {
        
        return new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message,
            fieldErrors,
            LocalDateTime.now(),
            path
        );
    }
}
```

---

### Global Exception Handler

```java
/**
 * Global exception handler for all controllers.
 * 
 * @ControllerAdvice:
 * - Applies to all @RestController classes
 * - Intercepts exceptions before they reach client
 * - Returns standardized error responses
 * 
 * Each @ExceptionHandler method:
 * - Catches specific exception type
 * - Logs the error
 * - Maps to appropriate HTTP status
 * - Returns ErrorResponse
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handles ResourceNotFoundException.
     * Returns 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        log.error("Resource not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Handles DuplicateResourceException.
     * Returns 409 CONFLICT.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {
        
        log.error("Duplicate resource: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.CONFLICT,
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    /**
     * Handles BusinessValidationException and InvalidPasswordException.
     * Returns 400 BAD REQUEST.
     */
    @ExceptionHandler({
        BusinessValidationException.class,
        InvalidPasswordException.class
    })
    public ResponseEntity<ErrorResponse> handleBusinessValidation(
            BusinessException ex,
            HttpServletRequest request) {
        
        log.error("Business validation failed: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Handles bean validation errors (@Valid failures).
     * Returns 400 BAD REQUEST with field-specific errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        log.error("Validation failed: {} errors", ex.getErrorCount());
        
        // Extract field errors
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null 
                    ? error.getDefaultMessage() 
                    : "Invalid value",
                (existing, replacement) -> existing  // Keep first error if duplicates
            ));
        
        ValidationErrorResponse error = ValidationErrorResponse.of(
            "Validation failed",
            fieldErrors,
            request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * Handles unexpected exceptions.
     * Returns 500 INTERNAL SERVER ERROR.
     * 
     * IMPORTANT: Don't expose internal error details to clients!
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected error occurred", ex);  // Full stack trace in logs
        
        // Generic message for client (don't expose internals!)
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI()
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
}
```

---

### Exception Handling Best Practices

**1. Use Specific Exceptions**
```java
// ✅ GOOD - Specific exception
throw new ResourceNotFoundException("User", id);

// ❌ BAD - Generic exception
throw new RuntimeException("Not found");
```

**2. Don't Expose Internal Details**
```java
// ✅ GOOD - Safe message
"An error occurred. Please try again later."

// ❌ BAD - Exposes internals
"NullPointerException at UserService.java:42"
```

**3. Log Errors Appropriately**
```java
// ✅ GOOD - Structured logging
log.error("User creation failed for email: {}", email, exception);

// ❌ BAD - No context
System.out.println("Error!");
```

---

### Practice Exercise 10: Test Exception Handling

```java
/**
 * TODO: Write integration tests for exception handling
 * 
 * These tests verify that exceptions are properly handled and
 * the correct HTTP responses are returned.
 * 
 * Use @SpringBootTest and TestRestTemplate to test the full stack.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ExceptionHandlingIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testResourceNotFoundException_Returns404() {
        // TODO:
        // 1. Make GET request to /api/users/999999 (non-existent ID)
        // 2. Assert response status is 404
        // 3. Assert response body contains ErrorResponse
        // 4. Assert message contains "not found"
    }
    
    @Test
    void testValidationError_Returns400WithFieldErrors() {
        // TODO:
        // 1. Create invalid request (e.g., empty email)
        // 2. POST to /api/users
        // 3. Assert status is 400
        // 4. Assert response contains ValidationErrorResponse
        // 5. Assert fieldErrors contains "email" key
    }
    
    @Test
    void testDuplicateResource_Returns409() {
        // TODO:
        // 1. Create user
        // 2. Try to create same user again
        // 3. Assert second request returns 409
        // 4. Assert message contains "already exists" or "duplicate"
    }
}
```

---

### Exception Handling Checklist

Before moving to Phase 7:
- [ ] Custom exception classes created
- [ ] @RestControllerAdvice handler created
- [ ] @ExceptionHandler for each exception type
- [ ] ErrorResponse and ValidationErrorResponse DTOs
- [ ] Proper HTTP status codes mapped
- [ ] Logging in exception handlers
- [ ] No internal details exposed to clients
- [ ] Integration tests for exception scenarios

---

_Part 3 continues with Security (Phase 7), Testing Strategy (Phase 8), and Production Features (Phase 9)..._

Would you like me to continue with the remaining phases?
