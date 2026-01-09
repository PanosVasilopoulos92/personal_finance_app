# 🚀 Learning Roadmap: Personal Finance App
## Mastering Enterprise Java Development with Spring Boot 4

> **Your Mentor's Note:** This roadmap follows the industry-standard implementation order: 
> **Entity → DTO → Repository → Service → Controller**. Each phase builds on the previous one,
> and you'll build complete features (vertical slices) rather than all layers at once.

---

## 📋 Table of Contents

1. [Overview](#-overview)
2. [The Implementation Philosophy](#-the-implementation-philosophy)
3. [Phase 1: Domain Model (Entities)](#phase-1-domain-model-entities---foundation)
4. [Phase 2: DTOs & Validation](#phase-2-dtos--validation---api-contracts)
5. [Phase 3: Repository Layer](#phase-3-repository-layer---data-access)
6. [Phase 4: Service Layer](#phase-4-service-layer---business-logic)
7. [Phase 5: Controller Layer](#phase-5-controller-layer---api-endpoints)
8. [Phase 6: Exception Handling](#phase-6-exception-handling---error-management)
9. [Phase 7: Security with JWT](#phase-7-security-with-jwt)
10. [Phase 8: Testing Strategy](#phase-8-comprehensive-testing)
11. [Phase 9: Production Features](#phase-9-production-ready-features)
12. [10-Week Learning Plan](#-10-week-implementation-plan)
13. [Resources & References](#-learning-resources)

---

## 🎯 Overview

**Project:** Personal Finance & Price Tracking Application  
**Your Learning Goal:** Transform a domain model into a production-ready enterprise application  
**Tech Stack:** Java 25, Spring Boot 4, JPA/Hibernate 7, MySQL, Lombok, Angular 20  

### What Makes This Different

This isn't a tutorial where you copy-paste code. This is a **structured learning journey** where:
- ✅ I show you **complete working examples**
- ✅ You implement **similar features yourself** with guided comments
- ✅ Each section has exercises that **stretch your understanding**
- ✅ You learn **why** we do things, not just how

### Your Current Progress

**✅ Already Complete:**
- Domain model with 12 entities (User, Item, Category, etc.)
- Proper relationships (@OneToMany, @ManyToOne)
- BaseEntity with audit fields
- Basic understanding of JPA annotations

**🎯 What We're Building:**
A complete REST API for personal finance tracking with:
- User authentication (JWT)
- Item management (CRUD + search)
- Shopping lists with items
- Price tracking over time
- Price alerts and comparisons
- Secure, tested, production-ready code

---

## 🏗️ The Implementation Philosophy

### Why This Order: Entity → DTO → Repository → Service → Controller

Think about building a house. You don't:
- Build all foundations, then all walls, then all roofs ❌
- Instead, you complete one room at a time ✅

**In our application:**
```
1. Entity First    → Define what data we store
2. DTO Second      → Define what crosses API boundaries  
3. Repository Third → Define how we query data
4. Service Fourth  → Define business logic
5. Controller Fifth → Define API endpoints
6. Tests Throughout → Verify it all works
```

### The Vertical Slice Approach

**Instead of** (Horizontal - building all of one layer):
```
❌ All Entities → All DTOs → All Repos → All Services → All Controllers
```

**We do** (Vertical - building one complete feature):
```
✅ User: Entity → DTOs → Repo → Service → Controller → Tests
✅ Item: Entity → DTOs → Repo → Service → Controller → Tests
✅ (Continue for each feature...)
```

**Why vertical slices win:**
- Working features faster
- Catch integration issues early
- Can demo at any point
- Better understanding of full stack
- Learn by doing, not just reading

---

## Phase 1: Domain Model (Entities) - Foundation

**Duration:** 1 week (Already mostly complete! ✅)  
**Goal:** Ensure your entities follow best practices  
**Difficulty:** ⭐⭐

### 📚 Entity Design Principles

Your entities represent your core business concepts. They should be:
1. **Focused** - One entity, one business concept
2. **Consistent** - All extend BaseEntity for audit fields
3. **Defensive** - Initialize collections to prevent NullPointerExceptions
4. **Type-safe** - Use enums instead of strings for fixed values
5. **Lazy by default** - Fetch relationships only when needed

---

### Complete Example: User Entity

```java
/**
 * Represents a user in the personal finance application.
 * 
 * Business Rules Enforced:
 * - Email must be unique (database constraint + service validation)
 * - Password is always encrypted (never stored in plain text)
 * - New users get USER role by default
 * - Users can be soft-deleted (active = false)
 * 
 * Relationships:
 * - ONE-TO-ONE with UserPreferences (cascade all, orphan removal)
 * - ONE-TO-MANY with Items (cascade all, orphan removal)
 * - ONE-TO-MANY with ShoppingLists (cascade all, orphan removal)
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email")  // Fast email lookups
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    
    // ========== BASIC FIELDS ==========
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false, length = 255)
    private String password;  // Always encrypted via PasswordEncoder
    
    @Column(nullable = false, length = 50)
    private String firstName;
    
    @Column(nullable = false, length = 50)
    private String lastName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;  // Default role
    
    @Column(nullable = false)
    private Boolean active = true;  // For soft deletion
    
    // ========== RELATIONSHIPS ==========
    
    /**
     * User preferences (theme, currency, language, etc.)
     * ONE-TO-ONE relationship
     * - cascade = ALL: When user is deleted, preferences are deleted
     * - orphanRemoval: If preferences are removed from user, delete them
     * - mappedBy: The 'user' field in UserPreferences owns the relationship
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserPreferences preferences;
    
    /**
     * Items owned by this user.
     * ONE-TO-MANY relationship
     * - cascade = ALL: Operations on user cascade to items
     * - orphanRemoval: If item is removed from list, delete it
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items = new ArrayList<>();  // Initialize to prevent NPE
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShoppingList> shoppingLists = new ArrayList<>();
    
    // ========== HELPER METHODS ==========
    
    /**
     * Gets the user's full name.
     * Useful for display purposes.
     * 
     * @return formatted full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Checks if the user is an admin.
     * 
     * @return true if user has ADMIN role
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
```

**Key Patterns Explained:**

1. **@Table with indexes** - Speed up common queries
2. **@Column constraints** - Database-level validation
3. **@Enumerated(EnumType.STRING)** - Safer than INT (survives enum reordering)
4. **Default values** - Sensible defaults prevent null issues
5. **Collection initialization** - Prevents NullPointerException
6. **cascade = ALL** - Lifecycle management (create/delete children with parent)
7. **orphanRemoval = true** - Clean up orphaned entities
8. **Helper methods** - Common operations encapsulated

---

### Practice Exercise 1: Complete the Store Entity

```java
/**
 * TODO: Complete this Store entity following the User example above
 * 
 * REQUIREMENTS:
 * 1. Extend BaseEntity for audit fields
 * 2. Use Lombok: @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor
 * 3. Add @Entity and @Table(name = "stores") with appropriate index
 * 
 * FIELDS TO IMPLEMENT:
 * - name: String, required, max 100 chars
 * - address: String, optional, max 255 chars
 * - city: String, optional, max 50 chars
 * - storeType: StoreTypeEnum, required, use @Enumerated(EnumType.STRING)
 * - latitude: Double, optional (for future location features)
 * - longitude: Double, optional
 * 
 * RELATIONSHIPS TO IMPLEMENT:
 * - user: ManyToOne with User, LAZY fetch, required (owner of the store)
 * - priceObservations: OneToMany with PriceObservation, mappedBy "store"
 * 
 * HELPER METHODS TO ADD:
 * - hasLocation(): boolean - returns true if latitude AND longitude are set
 * - getFullAddress(): String - returns formatted address:
 *   * If both address and city: "address, city"
 *   * If only address: "address"
 *   * If only city: "city"
 *   * If neither: "No address specified"
 * 
 * THINK ABOUT:
 * - What should happen to PriceObservations when a Store is deleted?
 * - Should we cascade delete or just set store to null?
 * - What index would speed up common queries?
 */
@Entity
@Table(name = "stores")  // TODO: Add appropriate index
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Store extends BaseEntity {
    
    // TODO: Add name field
    // Hints: @Column(nullable = ?, length = ?)
    
    
    // TODO: Add address field
    
    
    // TODO: Add city field
    
    
    // TODO: Add storeType field (it's an enum!)
    // Hints: @Enumerated(EnumType.STRING), @Column(nullable = ?, length = ?)
    
    
    // TODO: Add latitude field
    // Hints: Just @Column, can be null
    
    
    // TODO: Add longitude field
    
    
    // TODO: Add user relationship (ManyToOne)
    // Hints: 
    // - @ManyToOne(fetch = FetchType.LAZY)
    // - @JoinColumn(name = "user_id", nullable = ?)
    // - Think: Can a store exist without an owner?
    
    
    // TODO: Add priceObservations relationship (OneToMany)
    // Hints:
    // - @OneToMany(mappedBy = "store")
    // - Initialize with new ArrayList<>()
    // - Think: Should we cascade delete observations when store is deleted?
    //          Or keep them for historical data?
    
    
    /**
     * Checks if this store has location coordinates.
     * Location is considered complete when BOTH latitude and longitude exist.
     * 
     * @return true if both coordinates are set (not null)
     */
    public boolean hasLocation() {
        // TODO: Implement this method
        // Hint: Check if latitude != null AND longitude != null
        return false;
    }
    
    /**
     * Gets a formatted address string for display.
     * 
     * Logic:
     * - Both address and city exist → "address, city"
     * - Only address exists → "address"
     * - Only city exists → "city"  
     * - Neither exists → "No address specified"
     * 
     * @return formatted address string
     */
    public String getFullAddress() {
        // TODO: Implement this method
        // Hints:
        // 1. Check if address is not null and not empty
        // 2. Check if city is not null and not empty
        // 3. Use String concatenation or String.format()
        // 4. Handle all four cases listed above
        
        // Example structure:
        // if (hasAddress && hasCity) {
        //     return address + ", " + city;
        // } else if (hasAddress) {
        //     return address;
        // } ...
        
        return null;  // Replace with your implementation
    }
}
```

---

### Entity Best Practices Checklist

Before moving to Phase 2, verify:
- [ ] All entities extend BaseEntity
- [ ] All entities use @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor
- [ ] All @Column annotations specify nullable and length
- [ ] All enums use @Enumerated(EnumType.STRING)
- [ ] All collections initialized with new ArrayList<>()
- [ ] All relationships use FetchType.LAZY (except @ManyToOne which is LAZY by default)
- [ ] Cascade types are intentional (think about lifecycle)
- [ ] Indexes on frequently queried columns
- [ ] Helper methods for common operations
- [ ] Javadoc on all entities explaining business rules

---

## Phase 2: DTOs & Validation - API Contracts

**Duration:** 1 week  
**Goal:** Design clean API contracts that decouple API from database  
**Difficulty:** ⭐⭐⭐

### 📚 Why DTOs Matter

**Without DTOs (BAD):**
```java
@PostMapping("/users")
public User createUser(@RequestBody User user) {  // ❌ EXPOSING ENTITY
    return userService.save(user);
}
```

**Problems:**
1. **Security Risk** - Client can set ANY field (id, role, createdAt)
2. **Tight Coupling** - API structure = database structure
3. **Data Leakage** - Sends sensitive data (passwords, internal IDs)
4. **Inflexibility** - Can't change DB without breaking clients

**With DTOs (GOOD):**
```java
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(
        @Valid @RequestBody CreateUserRequest request) {  // ✅ USING DTO
    UserResponse response = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

**Benefits:**
1. **Security** - Client can only send what we allow
2. **Decoupling** - API and DB evolve independently
3. **Privacy** - Only expose what's needed
4. **Validation** - Input validated at API boundary

---

### DTO Naming Conventions

```
REQUEST DTOs (Input):
- CreateXxxRequest    → POST (creating new resources)
- UpdateXxxRequest    → PUT/PATCH (updating resources)
- XxxSearchRequest    → GET with complex search criteria

RESPONSE DTOs (Output):
- XxxResponse         → Single entity response
- XxxSummaryResponse  → Lightweight version (for lists/nested objects)
- XxxListResponse     → Wrapper for collections (if needed)
```

---

### Complete Example: User DTOs

#### CreateUserRequest - For Registration

```java
/**
 * Request DTO for user registration.
 * Contains only fields that clients can provide during signup.
 * All fields are validated before processing.
 * 
 * Security Note: Clients CANNOT set:
 * - id (auto-generated)
 * - role (defaults to USER)
 * - active (defaults to true)
 * - createdAt/updatedAt (auto-managed)
 */
public record CreateUserRequest(
        
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid format")
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        String email,
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8-100 characters")
        @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
            message = "Password must contain: digit, lowercase, uppercase, and special character"
        )
        String password,
        
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        String firstName,
        
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        String lastName
) {
    /**
     * Converts this request DTO to a User entity.
     * Password will be encrypted by the service layer (not here!).
     * 
     * Why not encrypt here?
     * - DTOs should be pure data, no business logic
     * - Service layer handles business rules like encryption
     * 
     * @return a new User entity (not persisted yet)
     */
    public User toEntity() {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);  // Service will encrypt this!
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(UserRole.USER);  // Business rule: default role
        user.setActive(true);  // Business rule: active by default
        return user;
    }
}
```

#### UpdateUserRequest - For Profile Updates

```java
/**
 * Request DTO for updating user profile.
 * 
 * Design Decision: Email and password updates are separate endpoints for security.
 * This DTO only allows updating name fields.
 * 
 * Why separate email/password endpoints?
 * - Email change requires verification (send confirmation email)
 * - Password change requires current password verification
 * - Profile updates are low-risk, these are high-risk
 */
public record UpdateUserRequest(
        
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        String firstName,
        
        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        String lastName
) {
    /**
     * Updates the given user entity with values from this DTO.
     * Only updates allowed fields.
     * 
     * Why a method instead of service code?
     * - Keeps update logic with the DTO
     * - Service layer doesn't need to know field mappings
     * - Easier to maintain (changes in one place)
     * 
     * @param user the user entity to update
     */
    public void updateEntity(User user) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        // Note: We don't update email, password, role, or active here
    }
}
```

#### UserResponse - For Returning User Data

```java
/**
 * Response DTO for user information.
 * Contains only data safe to expose to clients.
 * 
 * NEVER includes:
 * - password (obviously!)
 * - internal IDs of related entities
 * - sensitive audit data
 * 
 * Design Note: Using Java records for immutability.
 * Records are perfect for DTOs because:
 * - Immutable by default (thread-safe)
 * - Auto-generated equals/hashCode/toString
 * - Concise syntax
 * - Clear intent ("this is just data")
 */
public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String fullName,  // Computed field
        UserRole role,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Factory method to create UserResponse from User entity.
     * 
     * Why static factory method?
     * - Cleaner than constructor
     * - Self-documenting (UserResponse.from(user))
     * - Can have multiple factory methods for different scenarios
     * 
     * @param user the user entity to convert
     * @return a new UserResponse DTO
     */
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),  // Using entity helper method
            user.getRole(),
            user.getActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    /**
     * Factory method for summary responses (fewer fields).
     * Used in lists or when nesting users in other responses.
     * 
     * Why a summary version?
     * - Reduces response size
     * - Faster serialization
     * - Client doesn't always need all fields
     * 
     * Example: When returning a list of items, each item's owner
     * can be a UserSummary instead of full User details.
     * 
     * @param user the user entity to convert
     * @return a minimal UserResponse
     */
    public static UserResponse summary(User user) {
        return new UserResponse(
            user.getId(),
            null,  // Don't include email in summary
            user.getFirstName(),
            user.getLastName(),
            user.getFullName(),
            null,  // Don't include role in summary
            null,  // Don't include active status
            null,  // Don't include timestamps
            null
        );
    }
}
```

---

### Practice Exercise 2: Create Item DTOs

#### Part A: CreateItemRequest (Guided Implementation)

```java
/**
 * TODO: Complete this CreateItemRequest DTO
 * 
 * REQUIREMENTS:
 * 1. Make it a record (immutable DTO)
 * 2. Add validation annotations
 * 3. Add toEntity() conversion method
 * 
 * FIELDS TO ADD:
 * - name: String, required (@NotBlank), max 100 chars
 * - description: String, optional, max 500 chars
 * - unitType: UnitType (enum), required (@NotNull)
 * - brand: String, optional, max 100 chars
 * - barcode: String, optional, max 50 chars
 * - categoryId: Long, optional (null = no category)
 * 
 * CONVERSION METHOD:
 * - toEntity(): Item
 *   * Create new Item
 *   * Set all basic fields
 *   * DON'T set user or category (service layer does that)
 *   * Return the item
 * 
 * THINK ABOUT:
 * - Why don't we include userId in the request?
 * - Where does userId come from?
 * - Why does service set relationships, not the DTO?
 */
public record CreateItemRequest(
    // TODO: Add fields with validation annotations
    
) {
    /**
     * Converts this request to an Item entity.
     * User and Category relationships are set by service layer.
     * 
     * @return new Item entity (not persisted)
     */
    public Item toEntity() {
        // TODO: Implement conversion
        // Steps:
        // 1. Create new Item()
        // 2. Set name, description, unitType, brand, barcode
        // 3. Return the item
        // Note: DON'T set user or category here!
        
        return null;
    }
}
```

#### Part B: ItemResponse (Practice Challenge)

```java
/**
 * TODO: Create ItemResponse DTO from scratch
 * 
 * REQUIREMENTS:
 * 1. Create a record named ItemResponse
 * 2. Include all fields that should be visible to clients
 * 3. Include nested CategorySummaryResponse (if category exists)
 * 4. Include latest price information (if available)
 * 5. Add two static factory methods: from() and summary()
 * 
 * FIELDS TO INCLUDE:
 * - id: Long
 * - name: String
 * - description: String
 * - unitType: UnitType
 * - brand: String
 * - barcode: String
 * - category: CategorySummaryResponse (can be null)
 * - latestPrice: BigDecimal (can be null)
 * - latestPriceDate: LocalDateTime (can be null)
 * - latestPriceStore: String (store name, can be null)
 * - createdAt: LocalDateTime
 * - updatedAt: LocalDateTime
 * 
 * METHODS TO IMPLEMENT:
 * 
 * 1. from(Item item) - Full conversion
 *    Steps:
 *    - Extract all fields from item
 *    - If item.getCategory() != null, convert using CategorySummaryResponse.from()
 *    - To get latest price:
 *      * Check if item.getPriceObservations() is not empty
 *      * Sort by observedAt DESC (or get max)
 *      * Extract price, observedAt, and store name
 *    - Return new ItemResponse with all fields
 * 
 * 2. summary(Item item) - Minimal version for lists
 *    - Only include: id, name, unitType
 *    - Set everything else to null
 *    - Used when showing items in lists (performance)
 * 
 * HINTS:
 * - For latest price, you might use:
 *   item.getPriceObservations().stream()
 *       .max(Comparator.comparing(PriceObservation::getObservedAt))
 *       .orElse(null)
 */

// TODO: Write the complete ItemResponse record here
```

#### Part C: UpdateItemRequest (Practice Challenge)

```java
/**
 * TODO: Create UpdateItemRequest DTO from scratch
 * 
 * REQUIREMENTS:
 * 1. Record with update fields
 * 2. Add validation
 * 3. Add updateEntity(Item) method
 * 
 * FIELDS (similar to Create, but think about what's updateable):
 * - name: required
 * - description: optional
 * - unitType: optional
 * - brand: optional
 * - barcode: optional
 * - categoryId: optional (null = remove category)
 * 
 * METHOD:
 * - updateEntity(Item item): void
 *   * Update item's fields with values from DTO
 *   * DON'T handle category relationship (service does that)
 * 
 * THINK ABOUT:
 * - Why is userId not here?
 * - How do we prevent users from updating others' items?
 * - What happens if categoryId is null?
 */

// TODO: Write the complete UpdateItemRequest record here
```

---

### DTO Best Practices Summary

**1. Records for DTOs**
```java
// ✅ GOOD - Immutable, concise
public record UserResponse(Long id, String email) {}

// ❌ AVOID - Mutable, more code
public class UserResponse {
    private Long id;
    private String email;
    // getters, setters, equals, hashCode, toString...
}
```

**2. Validation Only on Requests**
```java
// ✅ Request DTOs have validation
public record CreateUserRequest(
    @NotBlank @Email String email
) {}

// ✅ Response DTOs don't (output doesn't need validation)
public record UserResponse(Long id, String email) {}
```

**3. Factory Methods for Conversion**
```java
// ✅ Static factory method
public static UserResponse from(User user) {
    return new UserResponse(...);
}

// Usage:
UserResponse response = UserResponse.from(user);
```

**4. Summary Versions for Performance**
```java
// Full version
public static ItemResponse from(Item item) {
    // All fields
}

// Summary version
public static ItemResponse summary(Item item) {
    // Only id, name
}
```

---

### Validation Annotations Quick Reference

```java
// STRING VALIDATIONS
@NotNull          // Not null
@NotBlank         // Not null, not empty, not whitespace
@Email            // Valid email format
@Size(min=, max=) // Length constraints
@Pattern(regexp=) // Regex pattern match

// NUMBER VALIDATIONS
@Min(value)       // Minimum value
@Max(value)       // Maximum value
@Positive         // Must be > 0
@PositiveOrZero   // Must be >= 0
@DecimalMin       // For BigDecimal
@DecimalMax       // For BigDecimal

// DATE VALIDATIONS
@Past             // Must be in the past
@Future           // Must be in the future
@PastOrPresent    
@FutureOrPresent

// COLLECTION VALIDATIONS
@NotEmpty         // Not null and not empty
@Size(min=, max=) // Size constraints

// CUSTOM MESSAGES
@NotBlank(message = "Your custom message")
```

---

### Phase 2 Checklist

Before moving to Phase 3:
- [ ] All request DTOs are records
- [ ] All request DTOs have validation
- [ ] All response DTOs are records
- [ ] Response DTOs don't include sensitive data
- [ ] Factory methods (from/summary) implemented
- [ ] toEntity() or updateEntity() in request DTOs
- [ ] Javadoc explains purpose and design decisions
- [ ] DTOs in separate package (dto or web.dto)
- [ ] Practice exercises completed

---

## Phase 3: Repository Layer - Data Access

**Duration:** 1-2 weeks  
**Goal:** Master Spring Data JPA and query optimization  
**Difficulty:** ⭐⭐⭐

### 📚 What You'll Learn

- Method name query derivation
- Custom JPQL queries
- JOIN FETCH to prevent N+1 problems
- Pagination and sorting
- Native SQL when needed
- Repository testing

### 🎯 Why Repositories Matter

Repositories are your **contract with the database**. They:
1. Abstract database operations (could swap databases)
2. Provide type-safe queries (compiler checks)
3. Enable easy testing (mock repositories)
4. Keep SQL/JPQL out of services (separation of concerns)
5. Follow Repository pattern (industry standard)

**Think of it this way:**
```
Service: "I need all active users"
Repository: "Here they are!" (handles HOW to get them)
```

---

### Complete Example: UserRepository

```java
/**
 * Repository for User entity data access.
 * Provides queries for user management, authentication, and search.
 * 
 * Spring Data JPA Magic:
 * - Extends JpaRepository → Automatic CRUD methods
 * - Method names → Automatic query generation
 * - @Query → Custom JPQL queries
 * - No implementation needed → Spring generates it!
 * 
 * Method Naming Convention:
 * - findBy...() → Returns Optional<T> or List<T>
 * - existsBy...() → Returns boolean
 * - countBy...() → Returns long
 * - deleteBy...() → Deletes and returns void/count
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ========== QUERY METHOD DERIVATION ==========
    // Spring generates queries from method names!
    
    /**
     * Finds user by email address.
     * Generated query: SELECT * FROM users WHERE email = ?
     * 
     * Used for:
     * - Login/authentication
     * - Email uniqueness check
     * 
     * @param email the email to search for
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Finds user by ID only if active.
     * Generated query: SELECT * FROM users WHERE id = ? AND active = ?
     * 
     * Why this exists:
     * - Authorization checks (don't allow inactive users)
     * - Soft-delete pattern (inactive = deleted)
     * 
     * @param id user ID
     * @param active active status
     * @return Optional containing user if found and active
     */
    Optional<User> findByIdAndActive(Long id, Boolean active);
    
    /**
     * Checks if user exists with given email.
     * Generated query: SELECT COUNT(*) FROM users WHERE email = ?
     * 
     * Performance Note: More efficient than findByEmail when you only
     * need to check existence (doesn't load entity).
     * 
     * @param email email to check
     * @return true if user exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Counts users by active status.
     * Generated query: SELECT COUNT(*) FROM users WHERE active = ?
     * 
     * Used for: Dashboard statistics, reports
     * 
     * @param active active status
     * @return count of users
     */
    long countByActive(Boolean active);
    
    /**
     * Finds all users by role.
     * Generated query: SELECT * FROM users WHERE role = ?
     * 
     * @param role the user role
     * @return list of users with that role
     */
    List<User> findByRole(UserRole role);
    
    // ========== CUSTOM JPQL QUERIES ==========
    // For complex queries that can't be derived from method names
    
    /**
     * Searches users by name (first or last name, case-insensitive).
     * 
     * JPQL Breakdown:
     * - SELECT u FROM User u → Select users (u is alias)
     * - WHERE LOWER(...) LIKE LOWER(...) → Case-insensitive match
     * - CONCAT('%', :searchTerm, '%') → Add wildcards for partial match
     * - OR → Match either first or last name
     * 
     * Why JPQL not method name?
     * - OR condition across fields
     * - Case-insensitive search
     * - Partial matching with LIKE
     * 
     * @param searchTerm term to search for
     * @return list of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchByName(@Param("searchTerm") String searchTerm);
    
    /**
     * Finds active users by role.
     * 
     * Could use method name: findByRoleAndActive(UserRole, Boolean)
     * But @Query is more explicit and readable for complex conditions.
     * 
     * @param role user role
     * @return list of active users with role
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = true")
    List<User> findActiveUsersByRole(@Param("role") UserRole role);
    
    /**
     * Finds users created within a date range.
     * 
     * JPQL BETWEEN:
     * - Inclusive on both ends
     * - Works with dates, numbers, strings
     * 
     * ORDER BY:
     * - DESC = newest first
     * - ASC = oldest first
     * 
     * @param startDate start of range (inclusive)
     * @param endDate end of range (inclusive)
     * @return list of users created in range, newest first
     */
    @Query("SELECT u FROM User u " +
           "WHERE u.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // ========== JOIN FETCH QUERIES (Performance Optimization) ==========
    // Prevents N+1 query problem
    
    /**
     * Finds user with preferences eagerly loaded.
     * 
     * THE N+1 PROBLEM:
     * Without JOIN FETCH:
     * 1. SELECT * FROM users WHERE id = 1           (1 query)
     * 2. SELECT * FROM user_preferences WHERE ...   (N queries for N users)
     * Total: 1 + N queries = BAD
     * 
     * With JOIN FETCH:
     * 1. SELECT * FROM users u LEFT JOIN user_preferences p ... (1 query)
     * Total: 1 query = GOOD
     * 
     * LEFT JOIN FETCH:
     * - LEFT = Include user even if no preferences
     * - FETCH = Load preferences in same query
     * 
     * Use when: You KNOW you'll access user.getPreferences()
     * 
     * @param id user ID
     * @return Optional with user and preferences loaded
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.preferences WHERE u.id = :id")
    Optional<User> findByIdWithPreferences(@Param("id") Long id);
    
    /**
     * Finds user with items loaded.
     * 
     * DISTINCT is CRITICAL when fetching collections:
     * - JPA may return duplicate User objects for each Item
     * - DISTINCT removes duplicates
     * 
     * ⚠️ WARNING: Use carefully!
     * - If user has 1000 items, loads all 1000
     * - Memory issue with large collections
     * - Consider pagination instead
     * 
     * @param id user ID
     * @return Optional with user and items loaded
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.items WHERE u.id = :id")
    Optional<User> findByIdWithItems(@Param("id") Long id);
    
    // ========== PAGINATION ==========
    // For large datasets
    
    /**
     * Finds active users with pagination.
     * 
     * Spring Data JPA automatically handles Pageable!
     * 
     * Usage:
     * ```
     * Pageable pageable = PageRequest.of(
     *     0,                        // page number (0-indexed)
     *     10,                       // page size
     *     Sort.by("lastName").ascending()  // sorting
     * );
     * Page<User> page = userRepository.findByActive(true, pageable);
     * 
     * // Page contains:
     * page.getContent()      // List of users
     * page.getTotalElements() // Total count
     * page.getTotalPages()   // Total pages
     * page.hasNext()         // More pages?
     * ```
     * 
     * @param active active status
     * @param pageable pagination parameters
     * @return page of users
     */
    Page<User> findByActive(Boolean active, Pageable pageable);
    
    // ========== MODIFYING QUERIES ==========
    // For UPDATE and DELETE
    
    /**
     * Soft deletes user (sets active = false).
     * 
     * @Modifying REQUIRED for UPDATE/DELETE queries!
     * 
     * Why soft delete?
     * - Keep historical data
     * - Auditing/compliance
     * - Can reactivate later
     * 
     * Note: Also needs @Transactional in service layer!
     * 
     * @param id user ID to soft delete
     * @return number of users updated (0 or 1)
     */
    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.id = :id")
    int softDeleteById(@Param("id") Long id);
}
```

---

### Key Repository Patterns

**1. Method Name Query Derivation**
```java
// Spring generates SQL from method names!
Optional<User> findByEmail(String email);
// → SELECT * FROM users WHERE email = ?

List<User> findByFirstNameAndLastName(String first, String last);
// → SELECT * FROM users WHERE first_name = ? AND last_name = ?

boolean existsByEmail(String email);
// → SELECT COUNT(*) > 0 FROM users WHERE email = ?

long countByActive(Boolean active);
// → SELECT COUNT(*) FROM users WHERE active = ?
```

**2. @Query for Complex Queries**
```java
// When method name gets too long or complex
@Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(:pattern)")
List<User> searchByEmailPattern(@Param("pattern") String pattern);
```

**3. JOIN FETCH to Prevent N+1**
```java
// ✅ GOOD - One query
@Query("SELECT u FROM User u LEFT JOIN FETCH u.preferences WHERE u.id = :id")
Optional<User> findByIdWithPreferences(@Param("id") Long id);

// ❌ BAD - N+1 queries
Optional<User> findById(Long id);
// Then accessing user.getPreferences() triggers another query!
```

---

### Practice Exercise 3: ItemRepository

**Part A: Basic ItemRepository (Guided)**

```java
/**
 * TODO: Complete this ItemRepository
 * 
 * REQUIREMENTS:
 * 1. Extend JpaRepository<Item, Long>
 * 2. Add @Repository annotation
 * 3. Implement methods using query derivation or @Query
 * 
 * METHODS TO IMPLEMENT:
 * 
 * 1. findByUserId(Long userId) → List<Item>
 *    - Use method name derivation
 *    - Finds all items for a user
 * 
 * 2. findByIdAndUserId(Long id, Long userId) → Optional<Item>
 *    - CRITICAL for security!
 *    - Ensures user can only access their own items
 *    - Use method name derivation
 * 
 * 3. findByCategoryId(Long categoryId) → List<Item>
 *    - Finds all items in a category
 *    - Use method name derivation
 * 
 * 4. existsByUserIdAndName(Long userId, String name) → boolean
 *    - Check for duplicate names per user
 *    - Use method name derivation
 * 
 * 5. searchByName(Long userId, String searchTerm) → List<Item>
 *    - Case-insensitive partial match on name
 *    - Only search user's items
 *    - Use @Query with LOWER and LIKE
 * 
 * 6. findByUserIdWithCategory(Long userId) → List<Item>
 *    - Load all items with categories (prevent N+1)
 *    - Use @Query with JOIN FETCH
 *    - Use DISTINCT (important for collections!)
 * 
 * 7. findByIdAndUserIdWithCategory(Long id, Long userId) → Optional<Item>
 *    - Load single item with category
 *    - Security check (userId)
 *    - Use @Query with JOIN FETCH
 * 
 * 8. findByUserId(Long userId, Pageable) → Page<Item>
 *    - Paginated items for user
 *    - Just add Pageable parameter to method 1
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    
    // TODO: Implement findByUserId
    // Hint: Just the method signature! Spring generates implementation
    
    
    // TODO: Implement findByIdAndUserId
    // Think: Why is this important for security?
    
    
    // TODO: Implement findByCategoryId
    
    
    // TODO: Implement existsByUserIdAndName
    // Think: When would you use this?
    
    
    /**
     * TODO: Implement searchByName
     * 
     * Requirements:
     * - Case-insensitive search
     * - Partial match (LIKE with wildcards)
     * - Only user's items
     * 
     * JPQL Template:
     * SELECT i FROM Item i 
     * WHERE i.user.id = :userId 
     * AND LOWER(i.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
     */
    // TODO: Write @Query and method signature
    
    
    /**
     * TODO: Implement findByUserIdWithCategory
     * 
     * Requirements:
     * - JOIN FETCH to load category
     * - Use LEFT JOIN (item may have no category)
     * - Use DISTINCT (prevents duplicates)
     * - Filter by userId
     * - Order by name
     * 
     * JPQL Template:
     * SELECT DISTINCT i FROM Item i 
     * LEFT JOIN FETCH i.category 
     * WHERE i.user.id = :userId 
     * ORDER BY i.name
     */
    // TODO: Write @Query and method signature
    
    
    /**
     * TODO: Implement findByIdAndUserIdWithCategory
     * 
     * Similar to above but for single item
     * Returns Optional<Item>
     */
    // TODO: Write @Query and method signature
    
    
    /**
     * TODO: Implement paginated version
     * 
     * Add Pageable parameter to findByUserId
     * Return type becomes Page<Item>
     */
    // TODO: Write method signature
}
```

---

### Practice Exercise 4: PriceObservationRepository (Challenge)

```java
/**
 * TODO: Create PriceObservationRepository from scratch
 * 
 * This is a challenge exercise! You'll create the entire repository.
 * 
 * REQUIREMENTS:
 * 1. Create interface extending JpaRepository<PriceObservation, Long>
 * 2. Add @Repository annotation
 * 3. Implement all methods below
 * 
 * METHODS TO IMPLEMENT:
 * 
 * BASIC QUERIES (use method name derivation):
 * - findByItemId(Long itemId) → List<PriceObservation>
 * - findByStoreId(Long storeId) → List<PriceObservation>
 * - countByItemId(Long itemId) → long
 * 
 * CUSTOM QUERIES (use @Query):
 * 
 * 1. findByItemIdOrderByObservedAtDesc(Long itemId) → List<PriceObservation>
 *    Purpose: Price history, newest first
 *    JPQL: SELECT po FROM PriceObservation po 
 *          WHERE po.item.id = :itemId 
 *          ORDER BY po.observedAt DESC
 * 
 * 2. findLatestByItemId(Long itemId) → Optional<PriceObservation>
 *    Purpose: Most recent price
 *    JPQL: SELECT po FROM PriceObservation po 
 *          WHERE po.item.id = :itemId 
 *          ORDER BY po.observedAt DESC
 *    Note: Spring automatically limits to 1 result for Optional return
 * 
 * 3. findByItemIdAndDateRange(Long itemId, LocalDateTime start, LocalDateTime end) → List<PriceObservation>
 *    Purpose: Price history in date range (for charts)
 *    JPQL: SELECT po FROM PriceObservation po 
 *          WHERE po.item.id = :itemId 
 *          AND po.observedAt BETWEEN :startDate AND :endDate
 *          ORDER BY po.observedAt ASC
 * 
 * 4. findByItemIdWithStore(Long itemId) → List<PriceObservation>
 *    Purpose: Load prices with store info (prevent N+1)
 *    JPQL: SELECT po FROM PriceObservation po 
 *          LEFT JOIN FETCH po.store 
 *          WHERE po.item.id = :itemId
 * 
 * AGGREGATE QUERIES (use @Query):
 * 
 * 5. findAveragePriceByItemId(Long itemId) → Optional<Double>
 *    Purpose: Average price across all observations
 *    JPQL: SELECT AVG(po.price) FROM PriceObservation po 
 *          WHERE po.item.id = :itemId
 * 
 * 6. findMinMaxPriceByItemId(Long itemId) → List<Object[]>
 *    Purpose: Price range (min and max)
 *    JPQL: SELECT MIN(po.price), MAX(po.price) FROM PriceObservation po 
 *          WHERE po.item.id = :itemId
 *    Note: Result is Object[] where [0] = min, [1] = max
 * 
 * MODIFYING QUERIES:
 * 
 * 7. deleteByItemId(Long itemId) → void
 *    Purpose: Delete all prices when item is deleted
 *    Annotations: @Modifying, @Query
 *    JPQL: DELETE FROM PriceObservation po WHERE po.item.id = :itemId
 * 
 * HINTS:
 * - Use @Param annotation for all parameters
 * - Remember @Modifying for DELETE/UPDATE
 * - BETWEEN is inclusive on both ends
 * - Aggregate functions (AVG, MIN, MAX) return nullable types
 */

// TODO: Write the complete PriceObservationRepository interface here
```

---

### Testing Repositories

**Complete Example: UserRepository Test**

```java
/**
 * Integration tests for UserRepository.
 * 
 * @DataJpaTest annotation:
 * - Configures in-memory H2 database
 * - Scans for @Entity classes
 * - Configures Spring Data JPA
 * - Provides TestEntityManager
 * - Rolls back after each test (clean slate)
 * 
 * Why integration tests for repositories?
 * - Verify SQL is correct
 * - Test database constraints
 * - Test JOIN FETCH actually works
 * - Catch issues early
 */
@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;  // For test data setup
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Arrange - Create test data before each test
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("encrypted_password");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(UserRole.USER);
        testUser.setActive(true);
        
        // Persist and flush
        entityManager.persist(testUser);
        entityManager.flush();  // Force write to DB
    }
    
    @Test
    void findByEmail_WhenUserExists_ReturnsUser() {
        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }
    
    @Test
    void findByEmail_WhenUserDoesNotExist_ReturnsEmpty() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        
        // Assert
        assertThat(found).isEmpty();
    }
    
    @Test
    void existsByEmail_WhenExists_ReturnsTrue() {
        // Act
        boolean exists = userRepository.existsByEmail("test@example.com");
        
        // Assert
        assertThat(exists).isTrue();
    }
    
    @Test
    void searchByName_WithMatchingFirstName_ReturnsUsers() {
        // Arrange - Create another user with similar name
        User anotherUser = new User();
        anotherUser.setEmail("johnny@example.com");
        anotherUser.setPassword("password");
        anotherUser.setFirstName("Johnny");
        anotherUser.setLastName("Smith");
        anotherUser.setRole(UserRole.USER);
        anotherUser.setActive(true);
        entityManager.persist(anotherUser);
        entityManager.flush();
        
        // Act - Case-insensitive search
        List<User> results = userRepository.searchByName("john");
        
        // Assert
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(User::getFirstName)
            .containsExactlyInAnyOrder("John", "Johnny");
    }
    
    @Test
    void findByIdWithPreferences_LoadsPreferences_WithoutLazyException() {
        // Arrange - Create user with preferences
        UserPreferences preferences = new UserPreferences();
        preferences.setUser(testUser);
        preferences.setCurrency("USD");
        preferences.setLanguage("en");
        testUser.setPreferences(preferences);
        entityManager.persist(preferences);
        entityManager.flush();
        entityManager.clear();  // Clear persistence context to test JOIN FETCH
        
        // Act
        Optional<User> found = userRepository.findByIdWithPreferences(testUser.getId());
        
        // Assert
        assertThat(found).isPresent();
        // If JOIN FETCH works, this won't throw LazyInitializationException
        assertThat(found.get().getPreferences()).isNotNull();
        assertThat(found.get().getPreferences().getCurrency()).isEqualTo("USD");
    }
    
    @Test
    void findByActive_WithPageable_ReturnsPaginatedResults() {
        // Arrange - Create 10 more users
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setEmail("user" + i + "@example.com");
            user.setPassword("password");
            user.setFirstName("User");
            user.setLastName(String.valueOf(i));
            user.setRole(UserRole.USER);
            user.setActive(true);
            entityManager.persist(user);
        }
        entityManager.flush();
        
        // Act - Request first page of 5 users, sorted by lastName
        Pageable pageable = PageRequest.of(0, 5, Sort.by("lastName").ascending());
        Page<User> page = userRepository.findByActive(true, pageable);
        
        // Assert
        assertThat(page.getContent()).hasSize(5);  // Page size
        assertThat(page.getTotalElements()).isEqualTo(11);  // Total count (10 + testUser)
        assertThat(page.getTotalPages()).isEqualTo(3);  // Ceiling(11/5)
        assertThat(page.getNumber()).isEqualTo(0);  // Current page
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }
    
    @Test
    void countByActive_ReturnsCorrectCounts() {
        // Arrange - Create inactive user
        User inactiveUser = new User();
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setPassword("password");
        inactiveUser.setFirstName("Jane");
        inactiveUser.setLastName("Inactive");
        inactiveUser.setRole(UserRole.USER);
        inactiveUser.setActive(false);
        entityManager.persist(inactiveUser);
        entityManager.flush();
        
        // Act
        long activeCount = userRepository.countByActive(true);
        long inactiveCount = userRepository.countByActive(false);
        
        // Assert
        assertThat(activeCount).isEqualTo(1);
        assertThat(inactiveCount).isEqualTo(1);
    }
}
```

---

### Practice Exercise 5: Test ItemRepository (Challenge)

```java
/**
 * TODO: Write comprehensive tests for ItemRepository
 * 
 * REQUIREMENTS:
 * 1. Use @DataJpaTest
 * 2. Autowire ItemRepository and TestEntityManager
 * 3. Create test data in @BeforeEach
 * 4. Follow AAA pattern (Arrange, Act, Assert)
 * 5. Use AssertJ assertions (assertThat)
 * 
 * TESTS TO WRITE:
 * 
 * 1. testFindByUserId_ReturnsOnlyUserItems()
 *    - Create 3 items for user1
 *    - Create 2 items for user2
 *    - Call findByUserId(user1.getId())
 *    - Verify returns 3 items
 *    - Verify all belong to user1
 * 
 * 2. testFindByIdAndUserId_WhenOwned_ReturnsItem()
 *    - Create item for user1
 *    - Call findByIdAndUserId(item.getId(), user1.getId())
 *    - Verify item is returned
 * 
 * 3. testFindByIdAndUserId_WhenNotOwned_ReturnsEmpty()
 *    - Create item for user1
 *    - Call findByIdAndUserId(item.getId(), user2.getId())
 *    - Verify empty Optional
 * 
 * 4. testExistsByUserIdAndName_WhenExists_ReturnsTrue()
 *    - Create item with name "Apple"
 *    - Call existsByUserIdAndName(userId, "Apple")
 *    - Verify returns true
 * 
 * 5. testSearchByName_CaseInsensitive_ReturnsMatches()
 *    - Create items: "Apple Juice", "Orange", "Apple Pie"
 *    - Search for "apple" (lowercase)
 *    - Verify returns both apple items
 * 
 * 6. testFindByUserIdWithCategory_LoadsCategory_NoLazyException()
 *    - Create item with category
 *    - Call entityManager.clear() to detach
 *    - Call findByUserIdWithCategory
 *    - Access item.getCategory() without exception
 * 
 * 7. testFindByUserId_WithPageable_ReturnsPaginatedResults()
 *    - Create 15 items
 *    - Request page 0, size 10
 *    - Verify page.getContent().size() == 10
 *    - Verify page.getTotalElements() == 15
 *    - Verify page.getTotalPages() == 2
 * 
 * HINTS:
 * - Create helper method for creating test users
 * - Create helper method for creating test items
 * - Use entityManager.flush() after persisting
 * - Use entityManager.clear() before testing JOIN FETCH
 */

@DataJpaTest
class ItemRepositoryTest {
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    private User testUser;
    private User otherUser;
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        // TODO: Create test data
        // 1. Create and persist testUser
        // 2. Create and persist otherUser
        // 3. Create and persist testCategory
    }
    
    // TODO: Implement all 7 tests listed above
}
```

---

### Repository Phase Checklist

Before moving to Phase 4:
- [ ] All repositories extend JpaRepository
- [ ] @Repository annotation added
- [ ] Query methods follow naming conventions
- [ ] Complex queries use @Query
- [ ] JOIN FETCH used to prevent N+1 where needed
- [ ] Pagination supported where appropriate
- [ ] @Modifying used for UPDATE/DELETE
- [ ] @Param used for all query parameters
- [ ] @DataJpaTest written for each repository
- [ ] Tests cover happy and edge cases
- [ ] Tests verify JOIN FETCH works (no LazyInitializationException)

---

This continues... Would you like me to proceed with the remaining phases (Service, Controller, Exception Handling, Security, Testing, and Production Features)? Each phase will follow the same pattern with complete examples and guided exercises.
