# 🌐 Advanced REST API Design Deep Dive

> **Goal:** Master REST API design to build professional, scalable, and developer-friendly APIs  
> **Stack:** Java 25, Spring Boot 4, Spring Framework 7  
> **Philosophy:** Great APIs are intuitive, consistent, and make the right thing easy and the wrong thing hard

---

## 📋 Table of Contents

1. [REST Principles Refresher](#1-rest-principles-refresher)
2. [Resource Naming & URI Design](#2-resource-naming--uri-design)
3. [HTTP Methods & Status Codes](#3-http-methods--status-codes)
4. [Request & Response Design](#4-request--response-design)
5. [API Versioning Strategies](#5-api-versioning-strategies)
6. [Pagination, Filtering & Sorting](#6-pagination-filtering--sorting)
7. [HATEOAS - Hypermedia APIs](#7-hateoas---hypermedia-apis)
8. [Content Negotiation](#8-content-negotiation)
9. [Caching Strategies](#9-caching-strategies)
10. [Rate Limiting](#10-rate-limiting)
11. [Async & Long-Running Operations](#11-async--long-running-operations)
12. [Bulk Operations](#12-bulk-operations)
13. [API Documentation with OpenAPI](#13-api-documentation-with-openapi)
14. [Error Handling Best Practices](#14-error-handling-best-practices)
15. [Security Headers & CORS](#15-security-headers--cors)
16. [API Design Checklist](#16-api-design-checklist)

---

## 1. REST Principles Refresher

### The Six REST Constraints

```
┌─────────────────────────────────────────────────────────────────────┐
│                     REST ARCHITECTURAL CONSTRAINTS                  │
│                                                                     │
│  1. CLIENT-SERVER                                                   │
│     └── Separation of concerns (UI vs data storage)                 │
│                                                                     │
│  2. STATELESS                                                       │
│     └── Each request contains all information needed                │
│     └── No session state on server                                  │
│                                                                     │
│  3. CACHEABLE                                                       │
│     └── Responses must define themselves as cacheable or not        │
│                                                                     │
│  4. UNIFORM INTERFACE                                               │
│     ├── Resource identification (URIs)                              │
│     ├── Resource manipulation through representations               │
│     ├── Self-descriptive messages                                   │
│     └── HATEOAS (Hypermedia as the engine of application state)     │
│                                                                     │
│  5. LAYERED SYSTEM                                                  │
│     └── Client can't tell if connected directly to server           │
│                                                                     │
│  6. CODE ON DEMAND (Optional)                                       │
│     └── Server can extend client functionality (e.g., JavaScript)   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Richardson Maturity Model

```
Level 3: Hypermedia Controls (HATEOAS)     ← TRUE REST
    ↑    Links guide client through API
    │
Level 2: HTTP Verbs                        ← Most APIs stop here
    ↑    GET, POST, PUT, DELETE properly used
    │
Level 1: Resources
    ↑    Individual URIs for resources
    │
Level 0: The Swamp of POX
         Single URI, single verb (POST everything)
```

---

## 2. Resource Naming & URI Design

### Golden Rules

| Rule | Good ✅ | Bad ❌ |
|------|---------|--------|
| Use nouns, not verbs | `/users` | `/getUsers` |
| Use plural nouns | `/orders` | `/order` |
| Use lowercase | `/shopping-carts` | `/ShoppingCarts` |
| Use hyphens for readability | `/user-profiles` | `/user_profiles` |
| Hierarchical for relationships | `/users/123/orders` | `/orders?userId=123` |
| No file extensions | `/users/123` | `/users/123.json` |
| No trailing slashes | `/users` | `/users/` |

### Resource Hierarchy Design

```
┌─────────────────────────────────────────────────────────────────────┐
│                    RESOURCE HIERARCHY                               │
│                                                                     │
│  Collection:     /users                                             │
│                     │                                               │
│  Instance:          └── /users/{userId}                             │
│                              │                                      │
│  Sub-collection:             ├── /users/{userId}/orders             │
│                              │        │                             │
│  Sub-instance:               │        └── /users/{userId}/orders/{orderId}
│                              │                                      │
│  Actions (when needed):      └── /users/{userId}/activate           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### URI Design Examples

```java
// ═══════════════════════════════════════════════════════════════════
// GOOD URI DESIGN
// ═══════════════════════════════════════════════════════════════════

// Collections
GET    /api/v1/users                    // List all users
POST   /api/v1/users                    // Create a user
GET    /api/v1/products                 // List all products

// Single resources
GET    /api/v1/users/123                // Get user 123
PUT    /api/v1/users/123                // Update user 123
PATCH  /api/v1/users/123                // Partial update user 123
DELETE /api/v1/users/123                // Delete user 123

// Nested resources (parent-child relationship)
GET    /api/v1/users/123/orders         // Get orders for user 123
POST   /api/v1/users/123/orders         // Create order for user 123
GET    /api/v1/users/123/orders/456     // Get order 456 for user 123

// Filtering via query parameters
GET    /api/v1/products?category=electronics&minPrice=100
GET    /api/v1/orders?status=pending&sort=createdAt,desc

// Search (when complex)
GET    /api/v1/users/search?q=john&role=admin

// Actions (non-CRUD operations) - use verbs sparingly
POST   /api/v1/users/123/activate       // Activate user
POST   /api/v1/orders/456/cancel        // Cancel order
POST   /api/v1/payments/789/refund      // Refund payment

// ═══════════════════════════════════════════════════════════════════
// BAD URI DESIGN
// ═══════════════════════════════════════════════════════════════════

GET    /api/v1/getUsers                 // ❌ Verb in URI
GET    /api/v1/user                     // ❌ Singular
POST   /api/v1/users/create             // ❌ Verb (POST implies create)
GET    /api/v1/users/123/getOrders      // ❌ Verb in URI
DELETE /api/v1/deleteUser/123           // ❌ Verb in URI
GET    /api/v1/Users                    // ❌ Uppercase
GET    /api/v1/user_profiles            // ❌ Underscore
```

### Implementation in Spring

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    // GET /api/v1/users
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }
    
    // GET /api/v1/users/123
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }
    
    // POST /api/v1/users
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.create(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(user.id())
            .toUri();
        return ResponseEntity.created(location).body(user);
    }
    
    // PUT /api/v1/users/123
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }
    
    // PATCH /api/v1/users/123
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> patchUser(
            @PathVariable Long id,
            @Valid @RequestBody PatchUserRequest request) {
        return ResponseEntity.ok(userService.patch(id, request));
    }
    
    // DELETE /api/v1/users/123
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    // GET /api/v1/users/123/orders
    @GetMapping("/{id}/orders")
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @PathVariable Long id,
            Pageable pageable) {
        return ResponseEntity.ok(userService.findOrdersByUserId(id, pageable));
    }
    
    // POST /api/v1/users/123/activate (action)
    @PostMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.activate(id));
    }
}
```

---

## 3. HTTP Methods & Status Codes

### HTTP Methods Semantics

| Method | Purpose | Idempotent | Safe | Request Body | Response Body |
|--------|---------|------------|------|--------------|---------------|
| GET | Read resource | ✅ Yes | ✅ Yes | ❌ No | ✅ Yes |
| POST | Create resource | ❌ No | ❌ No | ✅ Yes | ✅ Yes |
| PUT | Full update/replace | ✅ Yes | ❌ No | ✅ Yes | ✅ Yes |
| PATCH | Partial update | ❌ No* | ❌ No | ✅ Yes | ✅ Yes |
| DELETE | Remove resource | ✅ Yes | ❌ No | ❌ No | ❌ Optional |
| HEAD | Get headers only | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| OPTIONS | Get allowed methods | ✅ Yes | ✅ Yes | ❌ No | ✅ Yes |

*PATCH can be idempotent depending on implementation

### PUT vs PATCH

```java
// Original resource
{
    "id": 123,
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "555-1234",
    "address": "123 Main St"
}

// ═══════════════════════════════════════════════════════════════════
// PUT - Complete replacement (must send ALL fields)
// ═══════════════════════════════════════════════════════════════════
PUT /api/v1/users/123
{
    "name": "John Smith",
    "email": "john.smith@example.com",
    "phone": "555-1234",
    "address": "123 Main St"
}
// Missing fields would be set to null!

// ═══════════════════════════════════════════════════════════════════
// PATCH - Partial update (only send changed fields)
// ═══════════════════════════════════════════════════════════════════
PATCH /api/v1/users/123
{
    "name": "John Smith",
    "email": "john.smith@example.com"
}
// Only name and email updated; phone and address unchanged
```

### HTTP Status Codes

```java
// ═══════════════════════════════════════════════════════════════════
// 2xx SUCCESS
// ═══════════════════════════════════════════════════════════════════

200 OK                  // GET, PUT, PATCH success
201 Created             // POST success (include Location header!)
202 Accepted            // Async operation accepted, processing later
204 No Content          // DELETE success, or PUT/PATCH with no response body

// ═══════════════════════════════════════════════════════════════════
// 3xx REDIRECTION
// ═══════════════════════════════════════════════════════════════════

301 Moved Permanently   // Resource moved permanently
302 Found               // Temporary redirect
304 Not Modified        // Cached response is still valid (with ETag)

// ═══════════════════════════════════════════════════════════════════
// 4xx CLIENT ERRORS
// ═══════════════════════════════════════════════════════════════════

400 Bad Request         // Malformed request, validation error
401 Unauthorized        // Authentication required
403 Forbidden           // Authenticated but not authorized
404 Not Found           // Resource doesn't exist
405 Method Not Allowed  // HTTP method not supported for this resource
406 Not Acceptable      // Can't produce requested content type
409 Conflict            // Resource conflict (duplicate, state conflict)
410 Gone                // Resource permanently deleted
415 Unsupported Media   // Request content type not supported
422 Unprocessable       // Semantic errors in request body
429 Too Many Requests   // Rate limit exceeded

// ═══════════════════════════════════════════════════════════════════
// 5xx SERVER ERRORS
// ═══════════════════════════════════════════════════════════════════

500 Internal Error      // Unexpected server error
501 Not Implemented     // Feature not implemented
502 Bad Gateway         // Upstream server error
503 Service Unavailable // Server temporarily unavailable
504 Gateway Timeout     // Upstream server timeout
```

### Status Code Decision Tree

```
Was the request successful?
│
├── YES
│   ├── GET → 200 OK
│   ├── POST → 201 Created (with Location header)
│   ├── PUT/PATCH → 200 OK (or 204 No Content)
│   ├── DELETE → 204 No Content
│   └── Async operation → 202 Accepted
│
└── NO
    ├── Client's fault?
    │   ├── Bad syntax → 400 Bad Request
    │   ├── Not authenticated → 401 Unauthorized
    │   ├── Not authorized → 403 Forbidden
    │   ├── Resource not found → 404 Not Found
    │   ├── Duplicate resource → 409 Conflict
    │   ├── Validation failed → 422 Unprocessable Entity
    │   └── Rate limited → 429 Too Many Requests
    │
    └── Server's fault?
        ├── Bug/exception → 500 Internal Server Error
        ├── Downstream failure → 502 Bad Gateway
        └── Overloaded → 503 Service Unavailable
```

---

## 4. Request & Response Design

### Request DTOs

```java
// ═══════════════════════════════════════════════════════════════════
// CREATE REQUEST - All required fields for creation
// ═══════════════════════════════════════════════════════════════════
public record CreateUserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    String password,
    
    @NotBlank(message = "First name is required")
    @Size(max = 50)
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    String lastName,
    
    @Size(max = 20)
    String phone
) {}

// ═══════════════════════════════════════════════════════════════════
// UPDATE REQUEST (PUT) - All fields (nullable means "set to null")
// ═══════════════════════════════════════════════════════════════════
public record UpdateUserRequest(
    @NotBlank(message = "First name is required")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    String lastName,
    
    String phone,  // Can be null to clear
    
    String address  // Can be null to clear
) {}

// ═══════════════════════════════════════════════════════════════════
// PATCH REQUEST - Only fields to update (null means "don't change")
// ═══════════════════════════════════════════════════════════════════
public record PatchUserRequest(
    @Size(max = 50)
    String firstName,  // null = don't change
    
    @Size(max = 50)
    String lastName,   // null = don't change
    
    @Size(max = 20)
    String phone,      // null = don't change
    
    String address     // null = don't change
) {
    public boolean hasFirstName() { return firstName != null; }
    public boolean hasLastName() { return lastName != null; }
    public boolean hasPhone() { return phone != null; }
    public boolean hasAddress() { return address != null; }
}

// Service implementation for PATCH
@Transactional
public UserResponse patch(Long id, PatchUserRequest request) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", id));
    
    if (request.hasFirstName()) {
        user.setFirstName(request.firstName());
    }
    if (request.hasLastName()) {
        user.setLastName(request.lastName());
    }
    if (request.hasPhone()) {
        user.setPhone(request.phone());
    }
    if (request.hasAddress()) {
        user.setAddress(request.address());
    }
    
    return UserResponse.from(user);
}
```

### Response DTOs

```java
// ═══════════════════════════════════════════════════════════════════
// STANDARD RESPONSE - Consistent structure
// ═══════════════════════════════════════════════════════════════════
public record UserResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    String fullName,
    String phone,
    String role,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),
            user.getPhone(),
            user.getRole().name(),
            user.isActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}

// ═══════════════════════════════════════════════════════════════════
// SUMMARY/LIST RESPONSE - Lighter for collections
// ═══════════════════════════════════════════════════════════════════
public record UserSummary(
    Long id,
    String email,
    String fullName,
    String role
) {
    public static UserSummary from(User user) {
        return new UserSummary(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole().name()
        );
    }
}

// ═══════════════════════════════════════════════════════════════════
// DETAILED RESPONSE - With nested resources
// ═══════════════════════════════════════════════════════════════════
public record UserDetailResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    String phone,
    AddressResponse address,
    List<OrderSummary> recentOrders,
    UserPreferencesResponse preferences,
    Instant createdAt,
    Instant updatedAt
) {}
```

### Envelope Pattern (Optional)

```java
// Some APIs wrap responses in an envelope
public record ApiResponse<T>(
    boolean success,
    T data,
    ApiMetadata meta
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }
    
    public static <T> ApiResponse<T> success(T data, ApiMetadata meta) {
        return new ApiResponse<>(true, data, meta);
    }
}

public record ApiMetadata(
    Instant timestamp,
    String requestId,
    Integer page,
    Integer size,
    Long totalElements
) {}

// Usage
@GetMapping("/{id}")
public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
    return ApiResponse.success(userService.findById(id));
}

// Response:
{
    "success": true,
    "data": {
        "id": 123,
        "email": "john@example.com",
        ...
    },
    "meta": {
        "timestamp": "2025-01-20T10:30:00Z",
        "requestId": "abc-123"
    }
}
```

### Null vs Absent Fields

```java
// Configure Jackson for consistent null handling
@Configuration
public class JacksonConfig {
    
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder
            // Don't include null fields in response
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            // Or use NON_ABSENT for Optional support
            .serializationInclusion(JsonInclude.Include.NON_ABSENT);
    }
}

// Or per-class
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
    Long id,
    String email,
    String phone  // Won't appear in JSON if null
) {}
```

---

## 5. API Versioning Strategies

### Strategy Comparison

| Strategy | Example | Pros | Cons |
|----------|---------|------|------|
| URI Path | `/api/v1/users` | Clear, cacheable | URL changes |
| Query Param | `/api/users?version=1` | Optional versioning | Can be ignored |
| Header | `Api-Version: 1` | Clean URLs | Less visible |
| Media Type | `Accept: application/vnd.myapi.v1+json` | RESTful, per-resource | Complex |

### Strategy 1: URI Path Versioning (Most Common)

```java
// Version in URL path
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {
    
    @GetMapping("/{id}")
    public UserResponseV1 getUser(@PathVariable Long id) {
        // V1 response format
    }
}

@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 {
    
    @GetMapping("/{id}")
    public UserResponseV2 getUser(@PathVariable Long id) {
        // V2 response format (breaking changes)
    }
}
```

### Strategy 2: Header Versioning

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping(value = "/{id}", headers = "Api-Version=1")
    public UserResponseV1 getUserV1(@PathVariable Long id) {
        // V1 response
    }
    
    @GetMapping(value = "/{id}", headers = "Api-Version=2")
    public UserResponseV2 getUserV2(@PathVariable Long id) {
        // V2 response
    }
    
    // Default version (latest or v1)
    @GetMapping("/{id}")
    public UserResponseV2 getUser(@PathVariable Long id) {
        return getUserV2(id);
    }
}
```

### Strategy 3: Media Type Versioning (Content Negotiation)

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping(
        value = "/{id}",
        produces = "application/vnd.mycompany.user.v1+json"
    )
    public UserResponseV1 getUserV1(@PathVariable Long id) {
        // V1 response
    }
    
    @GetMapping(
        value = "/{id}",
        produces = "application/vnd.mycompany.user.v2+json"
    )
    public UserResponseV2 getUserV2(@PathVariable Long id) {
        // V2 response
    }
}

// Client uses:
// Accept: application/vnd.mycompany.user.v2+json
```

### Strategy 4: Custom Version Resolver

```java
// Custom annotation
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {
    int value();
}

// Version resolver
public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
    
    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        ApiVersion annotation = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        return annotation != null ? new ApiVersionCondition(annotation.value()) : null;
    }
    
    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        ApiVersion annotation = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        return annotation != null ? new ApiVersionCondition(annotation.value()) : null;
    }
}

// Usage
@RestController
@RequestMapping("/api/users")
@ApiVersion(1)
public class UserControllerV1 { }

@RestController
@RequestMapping("/api/users")
@ApiVersion(2)
public class UserControllerV2 { }
```

### Version Deprecation Pattern

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {
    
    @GetMapping("/{id}")
    @Deprecated
    public ResponseEntity<UserResponseV1> getUser(@PathVariable Long id) {
        return ResponseEntity.ok()
            .header("Deprecation", "true")
            .header("Sunset", "Sat, 31 Dec 2025 23:59:59 GMT")
            .header("Link", "</api/v2/users/{id}>; rel=\"successor-version\"")
            .body(userService.findByIdV1(id));
    }
}
```

---

## 6. Pagination, Filtering & Sorting

### Pagination Design

```java
// ═══════════════════════════════════════════════════════════════════
// OFFSET-BASED PAGINATION (Traditional)
// ═══════════════════════════════════════════════════════════════════

// Request: GET /api/v1/users?page=0&size=20&sort=createdAt,desc

@GetMapping
public ResponseEntity<Page<UserResponse>> getUsers(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
    
    Page<UserResponse> page = userService.findAll(pageable);
    return ResponseEntity.ok(page);
}

// Response includes pagination metadata:
{
    "content": [...],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 20,
        "sort": { "sorted": true, "orders": [...] }
    },
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false,
    "numberOfElements": 20
}
```

```java
// ═══════════════════════════════════════════════════════════════════
// CURSOR-BASED PAGINATION (Better for real-time data)
// ═══════════════════════════════════════════════════════════════════

public record CursorPage<T>(
    List<T> content,
    String nextCursor,
    String previousCursor,
    boolean hasNext,
    boolean hasPrevious,
    int size
) {}

@GetMapping
public ResponseEntity<CursorPage<UserResponse>> getUsers(
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size) {
    
    return ResponseEntity.ok(userService.findAllWithCursor(cursor, size));
}

// Service implementation
public CursorPage<UserResponse> findAllWithCursor(String cursor, int size) {
    Long lastId = cursor != null ? decodeCursor(cursor) : Long.MAX_VALUE;
    
    List<User> users = userRepository.findByIdLessThanOrderByIdDesc(lastId, PageRequest.of(0, size + 1));
    
    boolean hasNext = users.size() > size;
    if (hasNext) {
        users = users.subList(0, size);
    }
    
    String nextCursor = hasNext ? encodeCursor(users.get(users.size() - 1).getId()) : null;
    
    return new CursorPage<>(
        users.stream().map(UserResponse::from).toList(),
        nextCursor,
        null,  // Implement if bi-directional needed
        hasNext,
        cursor != null,
        size
    );
}

// Response:
{
    "content": [...],
    "nextCursor": "eyJpZCI6MTIzfQ==",
    "previousCursor": null,
    "hasNext": true,
    "hasPrevious": false,
    "size": 20
}
```

### Filtering

```java
// ═══════════════════════════════════════════════════════════════════
// SIMPLE FILTERING - Query parameters
// ═══════════════════════════════════════════════════════════════════

// GET /api/v1/products?category=electronics&minPrice=100&maxPrice=500&inStock=true

@GetMapping
public ResponseEntity<Page<ProductResponse>> getProducts(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Boolean inStock,
        Pageable pageable) {
    
    ProductFilter filter = new ProductFilter(category, minPrice, maxPrice, inStock);
    return ResponseEntity.ok(productService.findAll(filter, pageable));
}

public record ProductFilter(
    String category,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Boolean inStock
) {}
```

```java
// ═══════════════════════════════════════════════════════════════════
// SPECIFICATION PATTERN - Complex filtering
// ═══════════════════════════════════════════════════════════════════

public class ProductSpecifications {
    
    public static Specification<Product> withFilter(ProductFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (filter.category() != null) {
                predicates.add(cb.equal(root.get("category"), filter.category()));
            }
            
            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
            }
            
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
            }
            
            if (filter.inStock() != null && filter.inStock()) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

// Repository
public interface ProductRepository extends JpaRepository<Product, Long>, 
                                          JpaSpecificationExecutor<Product> {}

// Service
public Page<ProductResponse> findAll(ProductFilter filter, Pageable pageable) {
    return productRepository
        .findAll(ProductSpecifications.withFilter(filter), pageable)
        .map(ProductResponse::from);
}
```

### Sorting

```java
// GET /api/v1/products?sort=price,asc&sort=name,desc

@GetMapping
public ResponseEntity<Page<ProductResponse>> getProducts(
        @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
    
    // Spring automatically parses sort parameters
    return ResponseEntity.ok(productService.findAll(pageable));
}

// Custom sort validation
@GetMapping
public ResponseEntity<Page<ProductResponse>> getProducts(
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    
    // Validate sort field (prevent SQL injection)
    Set<String> allowedSortFields = Set.of("name", "price", "createdAt", "rating");
    if (!allowedSortFields.contains(sortBy)) {
        throw new BadRequestException("Invalid sort field: " + sortBy);
    }
    
    Sort sort = sortDir.equalsIgnoreCase("asc") 
        ? Sort.by(sortBy).ascending() 
        : Sort.by(sortBy).descending();
    
    Pageable pageable = PageRequest.of(page, size, sort);
    return ResponseEntity.ok(productService.findAll(pageable));
}
```

---

## 7. HATEOAS - Hypermedia APIs

### What Is HATEOAS?

HATEOAS (Hypermedia as the Engine of Application State) means responses include **links** that tell clients what actions are available.

```json
// Without HATEOAS - Client must know API structure
{
    "id": 123,
    "status": "PENDING",
    "total": 99.99
}

// With HATEOAS - Response tells client what's possible
{
    "id": 123,
    "status": "PENDING",
    "total": 99.99,
    "_links": {
        "self": { "href": "/api/v1/orders/123" },
        "cancel": { "href": "/api/v1/orders/123/cancel", "method": "POST" },
        "payment": { "href": "/api/v1/orders/123/payment", "method": "POST" },
        "customer": { "href": "/api/v1/customers/456" }
    }
}
```

### Setup Spring HATEOAS

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-hateoas</artifactId>
</dependency>
```

### Basic HATEOAS Implementation

```java
// ═══════════════════════════════════════════════════════════════════
// MODEL - Extend RepresentationModel
// ═══════════════════════════════════════════════════════════════════

public class UserModel extends RepresentationModel<UserModel> {
    
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean active;
    
    // Constructor, getters...
    
    public static UserModel from(User user) {
        UserModel model = new UserModel();
        model.id = user.getId();
        model.email = user.getEmail();
        model.firstName = user.getFirstName();
        model.lastName = user.getLastName();
        model.role = user.getRole().name();
        model.active = user.isActive();
        return model;
    }
}

// ═══════════════════════════════════════════════════════════════════
// ASSEMBLER - Creates links for model
// ═══════════════════════════════════════════════════════════════════

@Component
public class UserModelAssembler implements RepresentationModelAssembler<User, UserModel> {
    
    @Override
    public UserModel toModel(User user) {
        UserModel model = UserModel.from(user);
        
        // Self link
        model.add(linkTo(methodOn(UserController.class).getUser(user.getId())).withSelfRel());
        
        // Related resources
        model.add(linkTo(methodOn(UserController.class).getUserOrders(user.getId(), null))
            .withRel("orders"));
        
        // Conditional links based on state
        if (!user.isActive()) {
            model.add(linkTo(methodOn(UserController.class).activateUser(user.getId()))
                .withRel("activate"));
        } else {
            model.add(linkTo(methodOn(UserController.class).deactivateUser(user.getId()))
                .withRel("deactivate"));
        }
        
        // Collection link
        model.add(linkTo(methodOn(UserController.class).getAllUsers(null))
            .withRel("users"));
        
        return model;
    }
}

// ═══════════════════════════════════════════════════════════════════
// CONTROLLER
// ═══════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserModelAssembler assembler;
    
    @GetMapping("/{id}")
    public ResponseEntity<UserModel> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(assembler.toModel(user));
    }
    
    @GetMapping
    public ResponseEntity<CollectionModel<UserModel>> getAllUsers(Pageable pageable) {
        Page<User> users = userService.findAll(pageable);
        
        CollectionModel<UserModel> collection = CollectionModel.of(
            users.map(assembler::toModel).getContent()
        );
        
        collection.add(linkTo(methodOn(UserController.class).getAllUsers(pageable)).withSelfRel());
        
        return ResponseEntity.ok(collection);
    }
}
```

### Response Example

```json
{
    "id": 123,
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER",
    "active": true,
    "_links": {
        "self": {
            "href": "http://localhost:8080/api/v1/users/123"
        },
        "orders": {
            "href": "http://localhost:8080/api/v1/users/123/orders"
        },
        "deactivate": {
            "href": "http://localhost:8080/api/v1/users/123/deactivate"
        },
        "users": {
            "href": "http://localhost:8080/api/v1/users"
        }
    }
}
```

### Paginated Collection with Links

```java
@GetMapping
public ResponseEntity<PagedModel<UserModel>> getAllUsers(
        @PageableDefault(size = 20) Pageable pageable) {
    
    Page<User> page = userService.findAll(pageable);
    
    PagedModel<UserModel> pagedModel = pagedResourcesAssembler.toModel(
        page, 
        assembler
    );
    
    return ResponseEntity.ok(pagedModel);
}

// Response:
{
    "_embedded": {
        "userModels": [
            { "id": 1, "email": "...", "_links": {...} },
            { "id": 2, "email": "...", "_links": {...} }
        ]
    },
    "_links": {
        "first": { "href": "...?page=0&size=20" },
        "prev": { "href": "...?page=0&size=20" },
        "self": { "href": "...?page=1&size=20" },
        "next": { "href": "...?page=2&size=20" },
        "last": { "href": "...?page=7&size=20" }
    },
    "page": {
        "size": 20,
        "totalElements": 150,
        "totalPages": 8,
        "number": 1
    }
}
```

---

## 8. Content Negotiation

### Request Content Type

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    // Accept JSON only
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> createUserFromJson(
            @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.create(request));
    }
    
    // Accept XML only
    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<UserResponse> createUserFromXml(
            @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.create(request));
    }
    
    // Accept multiple types
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<UserResponse> createUser(
            @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(userService.create(request));
    }
}
```

### Response Content Type

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    // Produce JSON only (default)
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse getUserAsJson(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    // Produce XML
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_XML_VALUE)
    public UserResponse getUserAsXml(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    // Produce CSV
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportUsers() {
        String csv = userService.exportToCsv();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
            .body(csv);
    }
    
    // Let client choose (based on Accept header)
    @GetMapping(
        value = "/{id}",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    public UserResponse getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

### Custom Media Types

```java
// Custom media type for your API
public class CustomMediaTypes {
    public static final String USER_V1 = "application/vnd.mycompany.user.v1+json";
    public static final String USER_V2 = "application/vnd.mycompany.user.v2+json";
}

@GetMapping(value = "/{id}", produces = CustomMediaTypes.USER_V2)
public UserResponseV2 getUserV2(@PathVariable Long id) {
    return userService.findByIdV2(id);
}
```

---

## 9. Caching Strategies

### HTTP Cache Headers

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    
    // ═══════════════════════════════════════════════════════════════
    // CACHE-CONTROL HEADER
    // ═══════════════════════════════════════════════════════════════
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        ProductResponse product = productService.findById(id);
        
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)))
            .body(product);
    }
    
    // No caching
    @GetMapping("/prices")
    public ResponseEntity<List<PriceResponse>> getPrices() {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(priceService.getCurrentPrices());
    }
    
    // Private cache (user-specific data)
    @GetMapping("/recommendations")
    public ResponseEntity<List<ProductResponse>> getRecommendations(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
            .body(recommendationService.forUser(user.getId()));
    }
}
```

### ETag Support

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long id,
            WebRequest request) {
        
        Product product = productService.findById(id);
        
        // Generate ETag from version or hash
        String etag = "\"" + product.getVersion() + "\"";
        
        // Check if client's cached version is still valid
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        
        return ResponseEntity.ok()
            .eTag(etag)
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
            .body(ProductResponse.from(product));
    }
    
    // Conditional update with ETag
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody UpdateProductRequest request,
            @RequestHeader(value = "If-Match", required = false) String ifMatch) {
        
        if (ifMatch == null) {
            throw new PreconditionRequiredException("If-Match header required");
        }
        
        Product product = productService.findById(id);
        String currentEtag = "\"" + product.getVersion() + "\"";
        
        if (!currentEtag.equals(ifMatch)) {
            throw new PreconditionFailedException("Resource has been modified");
        }
        
        ProductResponse updated = productService.update(id, request);
        return ResponseEntity.ok()
            .eTag("\"" + updated.version() + "\"")
            .body(updated);
    }
}
```

### Last-Modified Support

```java
@GetMapping("/{id}")
public ResponseEntity<ProductResponse> getProduct(
        @PathVariable Long id,
        WebRequest request) {
    
    Product product = productService.findById(id);
    
    // Convert to epoch millis
    long lastModifiedMillis = product.getUpdatedAt()
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli();
    
    // Check If-Modified-Since header
    if (request.checkNotModified(lastModifiedMillis)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }
    
    return ResponseEntity.ok()
        .lastModified(lastModifiedMillis)
        .body(ProductResponse.from(product));
}
```

### Application-Level Caching

```java
@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#id")
    public ProductResponse findById(Long id) {
        // Cached - DB query only on cache miss
        return productRepository.findById(id)
            .map(ProductResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
    
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse update(Long id, UpdateProductRequest request) {
        // Cache evicted on update
        Product product = productRepository.findById(id).orElseThrow();
        // update logic...
        return ProductResponse.from(product);
    }
    
    @CacheEvict(value = "products", allEntries = true)
    public void clearCache() {
        // Clear entire cache
    }
}
```

---

## 10. Rate Limiting

### Rate Limiting with Bucket4j

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

```java
// ═══════════════════════════════════════════════════════════════════
// RATE LIMITING CONFIGURATION
// ═══════════════════════════════════════════════════════════════════

@Configuration
public class RateLimitConfig {
    
    @Bean
    public Map<String, Bucket> rateLimitBuckets() {
        return new ConcurrentHashMap<>();
    }
    
    public Bucket createBucket(String plan) {
        return switch (plan) {
            case "FREE" -> Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofHours(1))))
                .build();
            case "BASIC" -> Bucket.builder()
                .addLimit(Bandwidth.classic(1000, Refill.intervally(1000, Duration.ofHours(1))))
                .build();
            case "PREMIUM" -> Bucket.builder()
                .addLimit(Bandwidth.classic(10000, Refill.intervally(10000, Duration.ofHours(1))))
                .build();
            default -> createBucket("FREE");
        };
    }
}

// ═══════════════════════════════════════════════════════════════════
// RATE LIMITING INTERCEPTOR
// ═══════════════════════════════════════════════════════════════════

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final Map<String, Bucket> buckets;
    private final RateLimitConfig rateLimitConfig;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null) {
            apiKey = request.getRemoteAddr();  // Fallback to IP
        }
        
        Bucket bucket = buckets.computeIfAbsent(apiKey, 
            key -> rateLimitConfig.createBucket("FREE"));
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", 
                String.valueOf(probe.getRemainingTokens()));
            return true;
        }
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.addHeader("X-Rate-Limit-Retry-After-Seconds",
            String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
        response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
        return false;
    }
}

// ═══════════════════════════════════════════════════════════════════
// REGISTER INTERCEPTOR
// ═══════════════════════════════════════════════════════════════════

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**");
    }
}
```

### Rate Limit Response Headers

```java
// Standard rate limit headers
X-Rate-Limit-Limit: 1000          // Max requests allowed
X-Rate-Limit-Remaining: 999       // Requests remaining
X-Rate-Limit-Reset: 1640000000    // Unix timestamp when limit resets
Retry-After: 3600                 // Seconds to wait (when limited)
```

### Annotation-Based Rate Limiting

```java
// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int requests() default 100;
    int durationSeconds() default 3600;
}

// Aspect
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = getKey(joinPoint);
        
        Bucket bucket = buckets.computeIfAbsent(key, k -> 
            Bucket.builder()
                .addLimit(Bandwidth.classic(
                    rateLimit.requests(), 
                    Refill.intervally(rateLimit.requests(), Duration.ofSeconds(rateLimit.durationSeconds()))
                ))
                .build()
        );
        
        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        }
        
        throw new RateLimitExceededException("Rate limit exceeded");
    }
}

// Usage
@RestController
public class ApiController {
    
    @GetMapping("/expensive-operation")
    @RateLimit(requests = 10, durationSeconds = 60)  // 10 requests per minute
    public ResponseEntity<?> expensiveOperation() {
        // ...
    }
}
```

---

## 11. Async & Long-Running Operations

### Async Endpoints with CompletableFuture

```java
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportService reportService;
    
    // ═══════════════════════════════════════════════════════════════
    // ASYNC ENDPOINT - Returns immediately with CompletableFuture
    // ═══════════════════════════════════════════════════════════════
    
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<ReportResponse>> getReport(@PathVariable Long id) {
        return reportService.generateReportAsync(id)
            .thenApply(ResponseEntity::ok)
            .exceptionally(ex -> ResponseEntity.status(500).build());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // DEFERRED RESULT - Releases servlet thread
    // ═══════════════════════════════════════════════════════════════
    
    @GetMapping("/{id}/deferred")
    public DeferredResult<ResponseEntity<ReportResponse>> getReportDeferred(@PathVariable Long id) {
        DeferredResult<ResponseEntity<ReportResponse>> result = new DeferredResult<>(30000L);
        
        result.onTimeout(() -> 
            result.setErrorResult(ResponseEntity.status(408).body(null)));
        
        reportService.generateReportAsync(id)
            .thenAccept(report -> result.setResult(ResponseEntity.ok(report)))
            .exceptionally(ex -> {
                result.setErrorResult(ResponseEntity.status(500).build());
                return null;
            });
        
        return result;
    }
}

@Service
public class ReportService {
    
    @Async
    public CompletableFuture<ReportResponse> generateReportAsync(Long id) {
        // Long-running operation
        ReportResponse report = generateReport(id);
        return CompletableFuture.completedFuture(report);
    }
}
```

### Long-Running Operations Pattern

```java
// ═══════════════════════════════════════════════════════════════════
// PATTERN: Submit job, return immediately, poll for status
// ═══════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportController {
    
    private final ExportService exportService;
    
    // Step 1: Submit job - Returns 202 Accepted
    @PostMapping
    public ResponseEntity<ExportJobResponse> startExport(
            @RequestBody ExportRequest request) {
        
        ExportJob job = exportService.submitExportJob(request);
        
        URI statusUri = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{jobId}")
            .buildAndExpand(job.getId())
            .toUri();
        
        return ResponseEntity
            .accepted()
            .location(statusUri)
            .body(new ExportJobResponse(
                job.getId(),
                job.getStatus(),
                "Job submitted. Check status at: " + statusUri
            ));
    }
    
    // Step 2: Check status
    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        ExportJob job = exportService.getJob(jobId);
        
        return switch (job.getStatus()) {
            case PENDING, PROCESSING -> ResponseEntity
                .ok()
                .header("Retry-After", "5")  // Suggest polling interval
                .body(new ExportJobResponse(job.getId(), job.getStatus(), job.getProgress()));
                
            case COMPLETED -> ResponseEntity
                .ok()
                .body(new ExportJobResponse(
                    job.getId(), 
                    job.getStatus(), 
                    "Download at: /api/v1/exports/" + jobId + "/download"
                ));
                
            case FAILED -> ResponseEntity
                .status(500)
                .body(new ExportJobResponse(job.getId(), job.getStatus(), job.getErrorMessage()));
        };
    }
    
    // Step 3: Download result
    @GetMapping("/{jobId}/download")
    public ResponseEntity<Resource> downloadExport(@PathVariable String jobId) {
        ExportJob job = exportService.getJob(jobId);
        
        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new BadRequestException("Export not ready");
        }
        
        Resource file = exportService.getExportFile(jobId);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + job.getFilename() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(file);
    }
}

// Response DTOs
public record ExportJobResponse(
    String jobId,
    JobStatus status,
    String message
) {}

public record ExportJobResponse(
    String jobId,
    JobStatus status,
    Integer progressPercent
) {}

public enum JobStatus {
    PENDING, PROCESSING, COMPLETED, FAILED
}
```

### Server-Sent Events (SSE)

```java
@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents() {
        return Flux.interval(Duration.ofSeconds(1))
            .map(seq -> ServerSentEvent.<String>builder()
                .id(String.valueOf(seq))
                .event("heartbeat")
                .data("Event " + seq)
                .build());
    }
    
    // Or with SseEmitter for WebMvc
    @GetMapping("/stream-mvc")
    public SseEmitter streamEventsMvc() {
        SseEmitter emitter = new SseEmitter(30_000L);  // 30 second timeout
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    emitter.send(SseEmitter.event()
                        .id(String.valueOf(i))
                        .name("update")
                        .data("Event " + i));
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
}
```

---

## 12. Bulk Operations

### Batch Create

```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    
    // ═══════════════════════════════════════════════════════════════
    // BATCH CREATE - All or nothing
    // ═══════════════════════════════════════════════════════════════
    
    @PostMapping("/batch")
    public ResponseEntity<List<ProductResponse>> createBatch(
            @Valid @RequestBody List<CreateProductRequest> requests) {
        
        if (requests.size() > 100) {
            throw new BadRequestException("Maximum 100 items per batch");
        }
        
        List<ProductResponse> created = productService.createBatch(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // BATCH CREATE - Partial success allowed
    // ═══════════════════════════════════════════════════════════════
    
    @PostMapping("/batch-partial")
    public ResponseEntity<BatchResult<ProductResponse>> createBatchPartial(
            @RequestBody List<CreateProductRequest> requests) {
        
        BatchResult<ProductResponse> result = productService.createBatchWithPartialSuccess(requests);
        
        HttpStatus status = result.failures().isEmpty() 
            ? HttpStatus.CREATED 
            : HttpStatus.MULTI_STATUS;  // 207
        
        return ResponseEntity.status(status).body(result);
    }
}

public record BatchResult<T>(
    List<T> successes,
    List<BatchFailure> failures,
    int totalProcessed,
    int successCount,
    int failureCount
) {
    public static <T> BatchResult<T> of(List<T> successes, List<BatchFailure> failures) {
        return new BatchResult<>(
            successes,
            failures,
            successes.size() + failures.size(),
            successes.size(),
            failures.size()
        );
    }
}

public record BatchFailure(
    int index,
    String error,
    Object input
) {}
```

### Batch Update

```java
@PatchMapping("/batch")
public ResponseEntity<BatchResult<ProductResponse>> updateBatch(
        @RequestBody List<BatchUpdateItem<PatchProductRequest>> items) {
    
    return ResponseEntity.ok(productService.updateBatch(items));
}

public record BatchUpdateItem<T>(
    Long id,
    T data
) {}

// Service
@Transactional
public BatchResult<ProductResponse> updateBatch(List<BatchUpdateItem<PatchProductRequest>> items) {
    List<ProductResponse> successes = new ArrayList<>();
    List<BatchFailure> failures = new ArrayList<>();
    
    for (int i = 0; i < items.size(); i++) {
        BatchUpdateItem<PatchProductRequest> item = items.get(i);
        try {
            ProductResponse updated = patch(item.id(), item.data());
            successes.add(updated);
        } catch (Exception e) {
            failures.add(new BatchFailure(i, e.getMessage(), item));
        }
    }
    
    return BatchResult.of(successes, failures);
}
```

### Batch Delete

```java
@DeleteMapping("/batch")
public ResponseEntity<BatchDeleteResult> deleteBatch(
        @RequestBody List<Long> ids) {
    
    if (ids.size() > 100) {
        throw new BadRequestException("Maximum 100 items per batch");
    }
    
    BatchDeleteResult result = productService.deleteBatch(ids);
    return ResponseEntity.ok(result);
}

public record BatchDeleteResult(
    int requestedCount,
    int deletedCount,
    List<Long> notFoundIds,
    List<DeletionFailure> failures
) {}

public record DeletionFailure(
    Long id,
    String reason
) {}
```

---

## 13. API Documentation with OpenAPI

### Comprehensive OpenAPI Setup

```java
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("E-Commerce API")
                .version("1.0.0")
                .description("""
                    # Introduction
                    This API provides access to our e-commerce platform.
                    
                    ## Authentication
                    All endpoints except `/auth/**` require a JWT token.
                    Include it in the `Authorization` header as `Bearer <token>`.
                    
                    ## Rate Limiting
                    - Free tier: 100 requests/hour
                    - Basic tier: 1,000 requests/hour
                    - Premium tier: 10,000 requests/hour
                    
                    ## Errors
                    All errors follow RFC 7807 Problem Details format.
                    """)
                .termsOfService("https://example.com/terms")
                .contact(new Contact()
                    .name("API Support")
                    .email("api-support@example.com")
                    .url("https://example.com/support"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .externalDocs(new ExternalDocumentation()
                .description("Full Documentation")
                .url("https://docs.example.com"))
            .servers(List.of(
                new Server().url("https://api.example.com").description("Production"),
                new Server().url("https://staging-api.example.com").description("Staging"),
                new Server().url("http://localhost:8080").description("Local")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token from /auth/login"))
                .addSchemas("Problem", new Schema<>()
                    .$ref("#/components/schemas/ProblemDetail")));
    }
}
```

### Documenting Controllers

```java
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product management endpoints")
@RequiredArgsConstructor
public class ProductController {
    
    @Operation(
        summary = "Get all products",
        description = "Retrieves a paginated list of products with optional filtering"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Products retrieved successfully",
            content = @Content(schema = @Schema(implementation = ProductPageResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid filter parameters",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "Minimum price", example = "10.00")
            @RequestParam(required = false) BigDecimal minPrice,
            
            @Parameter(description = "Maximum price", example = "100.00")
            @RequestParam(required = false) BigDecimal maxPrice,
            
            @ParameterObject Pageable pageable) {
        // ...
    }
    
    @Operation(
        summary = "Create a product",
        description = "Creates a new product. Requires ADMIN role."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Product created successfully",
            headers = @Header(name = "Location", description = "URL of created product")
        ),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized (requires ADMIN)")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Product to create",
                required = true,
                content = @Content(schema = @Schema(implementation = CreateProductRequest.class))
            )
            @Valid @RequestBody CreateProductRequest request) {
        // ...
    }
}
```

### Documenting DTOs

```java
@Schema(description = "Request to create a new product")
public record CreateProductRequest(
    
    @Schema(
        description = "Product name",
        example = "Wireless Headphones",
        minLength = 1,
        maxLength = 100,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    @Size(max = 100)
    String name,
    
    @Schema(
        description = "Product description",
        example = "High-quality wireless headphones with noise cancellation",
        maxLength = 2000
    )
    @Size(max = 2000)
    String description,
    
    @Schema(
        description = "Product price in USD",
        example = "99.99",
        minimum = "0.01"
    )
    @NotNull
    @Positive
    BigDecimal price,
    
    @Schema(
        description = "Product category",
        example = "ELECTRONICS",
        allowableValues = {"ELECTRONICS", "CLOTHING", "BOOKS", "HOME"}
    )
    @NotNull
    ProductCategory category,
    
    @Schema(
        description = "Stock quantity",
        example = "100",
        minimum = "0",
        defaultValue = "0"
    )
    @PositiveOrZero
    Integer stockQuantity
) {}

@Schema(description = "Product response")
public record ProductResponse(
    
    @Schema(description = "Unique product ID", example = "123")
    Long id,
    
    @Schema(description = "Product name", example = "Wireless Headphones")
    String name,
    
    @Schema(description = "Product description")
    String description,
    
    @Schema(description = "Current price in USD", example = "99.99")
    BigDecimal price,
    
    @Schema(description = "Product category")
    ProductCategory category,
    
    @Schema(description = "Available stock quantity", example = "50")
    Integer stockQuantity,
    
    @Schema(description = "Whether product is in stock")
    boolean inStock,
    
    @Schema(description = "Creation timestamp")
    Instant createdAt,
    
    @Schema(description = "Last update timestamp")
    Instant updatedAt
) {}
```

---

## 14. Error Handling Best Practices

### RFC 7807 Problem Details

```java
// ═══════════════════════════════════════════════════════════════════
// PROBLEM DETAIL RESPONSE (RFC 7807)
// ═══════════════════════════════════════════════════════════════════

public record ProblemDetail(
    @Schema(description = "URI identifying the problem type")
    String type,
    
    @Schema(description = "Short summary of the problem")
    String title,
    
    @Schema(description = "HTTP status code")
    int status,
    
    @Schema(description = "Human-readable explanation")
    String detail,
    
    @Schema(description = "URI of the affected resource")
    String instance,
    
    @Schema(description = "Timestamp of the error")
    Instant timestamp,
    
    @Schema(description = "Unique error trace ID for support")
    String traceId,
    
    @Schema(description = "Validation errors (if applicable)")
    List<ValidationError> errors
) {
    public record ValidationError(
        String field,
        String message,
        Object rejectedValue
    ) {}
}

// ═══════════════════════════════════════════════════════════════════
// GLOBAL EXCEPTION HANDLER
// ═══════════════════════════════════════════════════════════════════

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        ProblemDetail problem = new ProblemDetail(
            "https://api.example.com/errors/not-found",
            "Resource Not Found",
            404,
            ex.getMessage(),
            request.getRequestURI(),
            Instant.now(),
            MDC.get("traceId"),
            null
        );
        
        return ResponseEntity.status(404)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        List<ProblemDetail.ValidationError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> new ProblemDetail.ValidationError(
                fe.getField(),
                fe.getDefaultMessage(),
                fe.getRejectedValue()
            ))
            .toList();
        
        ProblemDetail problem = new ProblemDetail(
            "https://api.example.com/errors/validation",
            "Validation Failed",
            422,
            "One or more fields have invalid values",
            request.getRequestURI(),
            Instant.now(),
            MDC.get("traceId"),
            errors
        );
        
        return ResponseEntity.status(422)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected error", ex);
        
        ProblemDetail problem = new ProblemDetail(
            "https://api.example.com/errors/internal",
            "Internal Server Error",
            500,
            "An unexpected error occurred. Please contact support with trace ID.",
            request.getRequestURI(),
            Instant.now(),
            MDC.get("traceId"),
            null
        );
        
        return ResponseEntity.status(500)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }
}
```

### Error Response Examples

```json
// 404 Not Found
{
    "type": "https://api.example.com/errors/not-found",
    "title": "Resource Not Found",
    "status": 404,
    "detail": "Product with ID 999 not found",
    "instance": "/api/v1/products/999",
    "timestamp": "2025-01-20T10:30:00Z",
    "traceId": "abc-123-def"
}

// 422 Validation Error
{
    "type": "https://api.example.com/errors/validation",
    "title": "Validation Failed",
    "status": 422,
    "detail": "One or more fields have invalid values",
    "instance": "/api/v1/products",
    "timestamp": "2025-01-20T10:30:00Z",
    "traceId": "abc-123-def",
    "errors": [
        {
            "field": "price",
            "message": "must be greater than 0",
            "rejectedValue": -10
        },
        {
            "field": "name",
            "message": "must not be blank",
            "rejectedValue": ""
        }
    ]
}

// 429 Rate Limited
{
    "type": "https://api.example.com/errors/rate-limited",
    "title": "Too Many Requests",
    "status": 429,
    "detail": "Rate limit exceeded. Retry after 3600 seconds.",
    "instance": "/api/v1/products",
    "timestamp": "2025-01-20T10:30:00Z",
    "traceId": "abc-123-def"
}
```

---

## 15. Security Headers & CORS

### Security Headers Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Security headers
            .headers(headers -> headers
                // Prevent clickjacking
                .frameOptions(frame -> frame.deny())
                
                // Prevent MIME type sniffing
                .contentTypeOptions(Customizer.withDefaults())
                
                // XSS protection
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                
                // Content Security Policy
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'"))
                
                // HSTS (HTTPS enforcement)
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true))
                
                // Referrer policy
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                
                // Permissions policy
                .permissionsPolicy(permissions -> permissions
                    .policy("geolocation=(), microphone=(), camera=()"))
            );
        
        return http.build();
    }
}
```

### CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Allowed origins
        config.setAllowedOrigins(List.of(
            "https://app.example.com",
            "https://admin.example.com"
        ));
        
        // Or use patterns
        config.setAllowedOriginPatterns(List.of(
            "https://*.example.com",
            "http://localhost:*"  // Development
        ));
        
        // Allowed methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // Allowed headers
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-API-Key"
        ));
        
        // Exposed headers (accessible to JavaScript)
        config.setExposedHeaders(List.of(
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset",
            "Location"
        ));
        
        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);
        
        // Preflight cache duration
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

// Or per-controller
@RestController
@RequestMapping("/api/v1/public")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PublicController { }
```

---

## 16. API Design Checklist

### Pre-Launch Checklist

```
═══════════════════════════════════════════════════════════════════
URI DESIGN
═══════════════════════════════════════════════════════════════════
□ Use nouns, not verbs (except for actions)
□ Use plural nouns for collections
□ Use lowercase with hyphens
□ Use proper hierarchy for relationships
□ Version your API (v1, v2)
□ Keep URIs consistent across resources

═══════════════════════════════════════════════════════════════════
HTTP METHODS & STATUS CODES
═══════════════════════════════════════════════════════════════════
□ Use correct HTTP methods for operations
□ Return appropriate status codes
□ POST returns 201 with Location header
□ DELETE returns 204 No Content
□ Distinguish 400 vs 422 for validation
□ Include Retry-After for 429 responses

═══════════════════════════════════════════════════════════════════
REQUEST & RESPONSE
═══════════════════════════════════════════════════════════════════
□ Use DTOs (never expose entities)
□ Validate all input
□ Use consistent response format
□ Handle null values consistently
□ Include timestamps in responses
□ Use ISO 8601 for dates (with timezone)

═══════════════════════════════════════════════════════════════════
PAGINATION & FILTERING
═══════════════════════════════════════════════════════════════════
□ Paginate all collection endpoints
□ Use consistent pagination parameters
□ Include total count in responses
□ Support sorting with validation
□ Support filtering on common fields
□ Limit maximum page size

═══════════════════════════════════════════════════════════════════
ERROR HANDLING
═══════════════════════════════════════════════════════════════════
□ Use RFC 7807 Problem Details
□ Include trace ID for debugging
□ Never expose stack traces
□ Provide helpful error messages
□ Include field-level validation errors
□ Document all error codes

═══════════════════════════════════════════════════════════════════
SECURITY
═══════════════════════════════════════════════════════════════════
□ Require authentication where needed
□ Implement authorization checks
□ Configure CORS properly
□ Add security headers
□ Implement rate limiting
□ Validate and sanitize input
□ Use HTTPS only

═══════════════════════════════════════════════════════════════════
DOCUMENTATION
═══════════════════════════════════════════════════════════════════
□ Document all endpoints (OpenAPI)
□ Include request/response examples
□ Document authentication requirements
□ Document rate limits
□ Document error codes
□ Keep docs in sync with code

═══════════════════════════════════════════════════════════════════
PERFORMANCE
═══════════════════════════════════════════════════════════════════
□ Implement HTTP caching (ETag, Cache-Control)
□ Use compression (gzip)
□ Optimize database queries
□ Consider async for long operations
□ Monitor response times
□ Set appropriate timeouts
```

---

## Summary

**Key Takeaways:**

1. **URIs are for resources** — Use nouns, not verbs; be consistent
2. **HTTP methods have meaning** — Use them correctly (GET safe, PUT idempotent)
3. **Status codes communicate** — Use the right code for the situation
4. **Pagination is mandatory** — Never return unbounded collections
5. **Version from day one** — Breaking changes will happen
6. **Error handling is UX** — Make errors helpful and consistent
7. **Security is non-negotiable** — Headers, CORS, rate limiting
8. **Documentation is the product** — If it's not documented, it doesn't exist

**The Golden Rule:**
> Design your API for your consumers, not your implementation.

---

**You now have the knowledge to design professional-grade REST APIs!** 🚀
