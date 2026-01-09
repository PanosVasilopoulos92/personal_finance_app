# 🚀 Personal Finance App - Learning Roadmap Part 2
## Phases 4-9: Service Layer Through Production

> **Continuation from Part 1** - Make sure you've completed Phases 1-3 before starting here!

---

## Phase 4: Service Layer - Business Logic

**Duration:** 2 weeks  
**Goal:** Build robust services with proper transaction management and business logic  
**Difficulty:** ⭐⭐⭐⭐

### 📚 What You'll Learn

- Service layer architecture and responsibilities
- Transaction management (@Transactional)
- Business logic implementation and validation
- DTO-Entity conversion patterns
- Service composition (services calling other services)
- Error handling with custom exceptions
- Service testing with Mockito

### 🎯 Service Layer Principles

**The service layer is the BRAIN of your application:**

```
Controller  → "Someone wants to create an item"
Service     → "Let me check if user exists, validate the name, 
               check category permissions, then create it"
Repository  → "Save this to database"
```

**Key Responsibilities:**
1. **Business Logic** - Enforce business rules
2. **Transaction Management** - Ensure data consistency
3. **Orchestration** - Coordinate multiple repositories
4. **DTO Conversion** - Transform between API and domain models
5. **Validation** - Beyond what annotations can do
6. **Error Handling** - Throw meaningful exceptions

---

### Complete Example: UserService

```java
/**
 * Service layer for User management.
 * 
 * Business Rules:
 * - Email must be unique
 * - Username must be unique
 * - Passwords encrypted with BCrypt
 * - New users get USER role by default
 * - Age must be >= 13 (legal requirement)
 * 
 * Security:
 * - Passwords never returned in responses
 * - Users can only access/modify their own data (unless ADMIN)
 */
@Service
@RequiredArgsConstructor  // Constructor injection via Lombok
@Slf4j  // Logging
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // ========== READ OPERATIONS ==========
    
    /**
     * Gets user by ID.
     * 
     * @Transactional(readOnly = true) optimization:
     * - Tells Hibernate this is read-only
     * - Skips dirty checking (performance boost)
     * - Uses read replica if configured
     * 
     * @param id user ID
     * @return user response DTO
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user with id: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with id: " + id
            ));
        
        return UserResponse.from(user);
    }
    
    /**
     * Gets user by email (for authentication).
     * 
     * @param email user email
     * @return user response
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with email: " + email
            ));
        
        return UserResponse.from(user);
    }
    
    /**
     * Gets all users with pagination.
     * 
     * @param page page number (0-indexed)
     * @param size page size
     * @param sortBy field to sort by
     * @param direction ASC or DESC
     * @return page of user responses
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(
            int page, int size, String sortBy, String direction) {
        
        log.debug("Fetching users - page: {}, size: {}", page, size);
        
        Sort sort = direction.equalsIgnoreCase("DESC")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> users = userRepository.findAll(pageable);
        
        // Convert Page<User> to Page<UserResponse>
        return users.map(UserResponse::from);
    }
    
    /**
     * Searches users by name.
     * 
     * @param searchTerm search term
     * @return list of matching users
     */
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String searchTerm) {
        log.debug("Searching users with term: {}", searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return userRepository.searchByName(searchTerm.trim())
            .stream()
            .map(UserResponse::from)
            .collect(Collectors.toList());
    }
    
    // ========== WRITE OPERATIONS ==========
    
    /**
     * Registers a new user.
     * 
     * Transaction behavior:
     * - @Transactional (without readOnly) starts a transaction
     * - If method completes: automatic COMMIT
     * - If exception thrown: automatic ROLLBACK
     * 
     * Business Logic Flow:
     * 1. Validate email uniqueness
     * 2. Validate username uniqueness
     * 3. Validate age requirement
     * 4. Encrypt password
     * 5. Set default role
     * 6. Save user
     * 7. Create default preferences
     * 
     * @param request registration request
     * @return created user response
     * @throws DuplicateResourceException if email/username exists
     * @throws BusinessValidationException if age < 13
     */
    @Transactional
    public UserResponse registerUser(CreateUserRequest request) {
        log.info("Registering new user with email: {}", request.email());
        
        // Business Rule: Email must be unique
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed - email exists: {}", request.email());
            throw new DuplicateResourceException(
                "Email already registered: " + request.email()
            );
        }
        
        // Business Rule: Username must be unique
        if (userRepository.existsByUsername(request.username())) {
            log.warn("Registration failed - username exists: {}", request.username());
            throw new DuplicateResourceException(
                "Username already taken: " + request.username()
            );
        }
        
        // Business Rule: Must be 13 or older
        if (request.age() != null && request.age() < 13) {
            throw new BusinessValidationException(
                "Users must be at least 13 years old"
            );
        }
        
        // Convert DTO to entity
        User user = request.toEntity();
        
        // Business Rule: Encrypt password
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);
        
        // Business Rule: Default role
        if (user.getUserRole() == null) {
            user.setUserRole(UserRolesEnum.USER);
        }
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Create default preferences
        UserPreferences preferences = new UserPreferences();
        preferences.setUser(savedUser);
        preferences.setCurrency(CurrencyEnum.USD);
        preferences.setLanguage("en");
        savedUser.setUserPreferences(preferences);
        
        log.info("Successfully registered user with id: {}", savedUser.getId());
        
        return UserResponse.from(savedUser);
    }
    
    /**
     * Updates user profile.
     * 
     * Note: Uses dirty checking - no need to call save()!
     * When @Transactional method ends, Hibernate automatically
     * detects changes and updates the database.
     * 
     * @param id user ID
     * @param request update request
     * @return updated user response
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user with id: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with id: " + id
            ));
        
        // Update allowed fields
        request.updateEntity(user);
        
        // No need to call save() - dirty checking handles it!
        
        log.info("Successfully updated user with id: {}", id);
        return UserResponse.from(user);
    }
    
    /**
     * Changes user password.
     * 
     * Security measures:
     * - Requires current password
     * - Validates new password strength
     * - Encrypts new password
     * - Doesn't return password in response
     * 
     * @param id user ID
     * @param request password change request
     * @throws ResourceNotFoundException if user not found
     * @throws InvalidPasswordException if current password wrong
     */
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        log.info("Changing password for user with id: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with id: " + id
            ));
        
        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            log.warn("Password change failed - incorrect current password");
            throw new InvalidPasswordException("Current password is incorrect");
        }
        
        // Encrypt and set new password
        String encryptedPassword = passwordEncoder.encode(request.newPassword());
        user.setPassword(encryptedPassword);
        
        log.info("Successfully changed password for user with id: {}", id);
    }
    
    /**
     * Deletes user (soft delete).
     * 
     * Soft Delete Strategy:
     * - Don't actually delete from database
     * - Set a flag (could be 'deleted' boolean or null the user)
     * - Keeps data for auditing
     * - Can be reactivated if needed
     * 
     * Note: This example assumes you add a 'deleted' field to User entity
     * 
     * @param id user ID
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with id: " + id
            ));
        
        // Soft delete - just mark as deleted
        // You'd need to add this field to User entity
        // user.setDeleted(true);
        
        // OR hard delete if that's your requirement
        userRepository.delete(user);
        
        log.info("Successfully deleted user with id: {}", id);
    }
    
    // ========== HELPER/VALIDATION METHODS ==========
    
    /**
     * Checks if user exists by ID.
     * Used by other services for validation.
     * 
     * @param id user ID
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }
    
    /**
     * Checks if email is available.
     * 
     * @param email email to check
     * @return true if available (not taken)
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
}
```

---

### Service Best Practices Explained

**1. Always Use @Transactional**

```java
// ✅ GOOD - Read operations
@Transactional(readOnly = true)
public UserResponse getUser(Long id) {
    // readOnly = true:
    // - Performance hint to Hibernate
    // - Skip dirty checking
    // - Use read replicas if configured
}

// ✅ GOOD - Write operations
@Transactional
public UserResponse createUser(CreateUserRequest request) {
    // Automatic commit on success
    // Automatic rollback on exception
    // Consistent database state guaranteed
}

// ❌ BAD - No transaction
public UserResponse getUser(Long id) {
    // Risk of LazyInitializationException
    // No transaction boundary
}
```

**2. Validate Business Rules in Service**

```java
// ✅ GOOD - Service validates business rules
@Transactional
public UserResponse createUser(CreateUserRequest request) {
    // Check uniqueness
    if (userRepository.existsByEmail(request.email())) {
        throw new DuplicateResourceException("Email exists");
    }
    
    // Check age requirement
    if (request.age() < 13) {
        throw new BusinessValidationException("Must be 13+");
    }
    
    // ... create user
}

// ❌ BAD - Just saves without validation
@Transactional
public UserResponse createUser(CreateUserRequest request) {
    User user = request.toEntity();
    return UserResponse.from(userRepository.save(user));
    // Database constraint violation will throw ugly exception!
}
```

**3. Use Specific Exceptions**

```java
// ✅ GOOD - Custom business exceptions
throw new ResourceNotFoundException("User not found");
throw new DuplicateResourceException("Email exists");
throw new BusinessValidationException("Age requirement");

// ❌ BAD - Generic exceptions
throw new RuntimeException("Something went wrong");
throw new Exception("Error");  // Never use checked exceptions!
```

**4. Log Important Operations**

```java
// ✅ GOOD - Structured logging
@Transactional
public UserResponse createUser(CreateUserRequest request) {
    log.info("Creating user with email: {}", request.email());
    
    // ... business logic
    
    log.info("Successfully created user with id: {}", user.getId());
    return response;
}

// ✅ GOOD - Log errors with context
catch (Exception e) {
    log.error("Failed to create user: {}", request.email(), e);
    throw e;
}

// ❌ BAD - No logging
@Transactional
public UserResponse createUser(CreateUserRequest request) {
    // No logging = hard to debug production issues
}
```

---

### Practice Exercise 6: ItemService (Guided Implementation)

```java
/**
 * TODO: Complete this ItemService
 * 
 * This service manages items with proper security and business rules.
 * 
 * REQUIREMENTS:
 * 1. Add @Service, @RequiredArgsConstructor, @Slf4j annotations
 * 2. Inject: ItemRepository, UserService, CategoryRepository
 * 3. Implement all CRUD operations
 * 4. Enforce security (users can only access their items)
 * 5. Validate business rules
 * 
 * BUSINESS RULES TO ENFORCE:
 * - Item name must be unique per user
 * - User must exist before creating item
 * - Category must belong to same user (if provided)
 * - Only item owner can update/delete
 * - Can't delete item with price observations (or cascade delete)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {
    
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final CategoryRepository categoryRepository;
    
    // ========== READ OPERATIONS ==========
    
    /**
     * TODO: Implement getItemById
     * 
     * Steps:
     * 1. Add @Transactional(readOnly = true)
     * 2. Log the operation
     * 3. Use itemRepository.findByIdAndUserIdWithCategory() for security + performance
     * 4. Throw ResourceNotFoundException if not found
     * 5. Convert to ItemResponse
     * 6. Return response
     * 
     * Security Note: The userId parameter ensures users can only access their items!
     * 
     * @param id item ID
     * @param userId user ID (for security)
     * @return item response
     * @throws ResourceNotFoundException if not found or doesn't belong to user
     */
    public ItemResponse getItemById(Long id, Long userId) {
        // TODO: Implement this method
        // Hint: Use findByIdAndUserIdWithCategory to get item with category loaded
        return null;
    }
    
    /**
     * TODO: Implement getAllItemsForUser
     * 
     * Steps:
     * 1. Add @Transactional(readOnly = true)
     * 2. Log the operation
     * 3. Verify user exists (userService.existsById)
     * 4. Throw ResourceNotFoundException if user doesn't exist
     * 5. Use itemRepository.findByUserIdWithCategory() for performance
     * 6. Convert each item to ItemResponse using stream().map()
     * 7. Return list
     * 
     * @param userId user ID
     * @return list of user's items
     * @throws ResourceNotFoundException if user not found
     */
    public List<ItemResponse> getAllItemsForUser(Long userId) {
        // TODO: Implement this method
        return null;
    }
    
    /**
     * TODO: Implement searchItems
     * 
     * Steps:
     * 1. Add @Transactional(readOnly = true)
     * 2. Validate searchTerm (not null/empty)
     * 3. If invalid, return empty list
     * 4. Use itemRepository.searchByName()
     * 5. Convert and return
     * 
     * @param userId user ID
     * @param searchTerm search term
     * @return matching items
     */
    public List<ItemResponse> searchItems(Long userId, String searchTerm) {
        // TODO: Implement this method
        return null;
    }
    
    // ========== WRITE OPERATIONS ==========
    
    /**
     * TODO: Implement createItem
     * 
     * This is the most complex method - follow these steps carefully:
     * 
     * 1. Add @Transactional annotation
     * 2. Log: "Creating item '{}' for user {}", request.name(), userId
     * 
     * 3. BUSINESS RULE: Verify user exists
     *    if (!userService.existsById(userId)) {
     *        throw new ResourceNotFoundException("User not found: " + userId);
     *    }
     * 
     * 4. BUSINESS RULE: Check name uniqueness per user
     *    if (itemRepository.existsByUserIdAndName(userId, request.name())) {
     *        throw new DuplicateResourceException("Item name already exists");
     *    }
     * 
     * 5. Convert request to entity: Item item = request.toEntity();
     * 
     * 6. Set user relationship:
     *    User user = new User();
     *    user.setId(userId);
     *    item.setUser(user);
     * 
     * 7. If categoryId provided in request:
     *    a. Fetch category from categoryRepository
     *    b. Verify category exists
     *    c. SECURITY: Verify category belongs to same user
     *    d. Set item.setCategory(category)
     * 
     * 8. Save: Item saved = itemRepository.save(item);
     * 
     * 9. Log success: "Successfully created item with id: {}", saved.getId()
     * 
     * 10. Convert and return: ItemResponse.from(saved)
     * 
     * @param userId user ID (from authentication)
     * @param request create item request
     * @return created item response
     */
    public ItemResponse createItem(Long userId, CreateItemRequest request) {
        // TODO: Implement this method following the steps above
        
        return null;
    }
    
    /**
     * TODO: Implement updateItem
     * 
     * Steps:
     * 1. Add @Transactional
     * 2. Log the operation
     * 3. Fetch item using findByIdAndUserId (SECURITY CHECK!)
     * 4. Throw ResourceNotFoundException if not found
     * 5. If name changed, check for duplicates
     * 6. Update basic fields using request.updateEntity(item)
     * 7. Handle category update:
     *    - If categoryId is null → remove category
     *    - If categoryId provided → fetch, validate ownership, set
     * 8. No need to call save() - dirty checking!
     * 9. Log success
     * 10. Convert and return
     * 
     * @param itemId item ID
     * @param userId user ID (security)
     * @param request update request
     * @return updated item response
     */
    public ItemResponse updateItem(Long itemId, Long userId, UpdateItemRequest request) {
        // TODO: Implement this method
        return null;
    }
    
    /**
     * TODO: Implement deleteItem
     * 
     * Steps:
     * 1. Add @Transactional
     * 2. Log the operation
     * 3. Fetch item using findByIdAndUserId (SECURITY!)
     * 4. Throw ResourceNotFoundException if not found
     * 5. Optional: Check if item has price observations
     *    - If has observations, you could:
     *      a. Throw BusinessValidationException (preserve data)
     *      b. Cascade delete (delete observations too)
     *      c. Soft delete (mark as deleted)
     * 6. Delete: itemRepository.delete(item)
     * 7. Log success
     * 
     * @param itemId item ID
     * @param userId user ID (security)
     */
    public void deleteItem(Long itemId, Long userId) {
        // TODO: Implement this method
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * TODO: Implement existsByIdAndUserId
     * 
     * Used by other services to check item existence with security.
     * 
     * @param itemId item ID
     * @param userId user ID
     * @return true if item exists and belongs to user
     */
    @Transactional(readOnly = true)
    public boolean existsByIdAndUserId(Long itemId, Long userId) {
        // TODO: Implement
        return false;
    }
}
```

---

### Practice Exercise 7: PriceObservationService (Challenge)

```java
/**
 * TODO: Create PriceObservationService from scratch
 * 
 * This is a challenge! You'll create the entire service.
 * 
 * REQUIREMENTS:
 * 1. Create class with @Service, @RequiredArgsConstructor, @Slf4j
 * 2. Inject: PriceObservationRepository, ItemService, StoreRepository
 * 3. Implement all methods below
 * 
 * BUSINESS RULES:
 * - Item must exist and belong to user
 * - Store must exist and belong to user
 * - Price must be positive
 * - Observation date can't be in future
 * - Can't have duplicate observation (same item, store, date)
 * 
 * METHODS TO IMPLEMENT:
 * 
 * READ OPERATIONS:
 * 1. getPriceHistory(Long itemId, Long userId) → List<PriceObservationResponse>
 *    - Verify item belongs to user (security!)
 *    - Get all observations ordered by date DESC
 *    - Convert to responses
 * 
 * 2. getLatestPrice(Long itemId, Long userId) → PriceObservationResponse
 *    - Verify item ownership
 *    - Get latest observation
 *    - Throw if no observations exist
 * 
 * 3. getPricesByDateRange(Long itemId, Long userId, LocalDate start, LocalDate end) → List<PriceObservationResponse>
 *    - Verify item ownership
 *    - Get observations in range
 *    - Useful for charts
 * 
 * 4. getAveragePrice(Long itemId, Long userId) → BigDecimal
 *    - Calculate average across all observations
 *    - Return null if no observations
 * 
 * WRITE OPERATIONS:
 * 5. recordPrice(Long userId, CreatePriceObservationRequest request) → PriceObservationResponse
 *    - Validate item exists and belongs to user
 *    - Validate store exists and belongs to user
 *    - Validate price is positive
 *    - Validate date not in future
 *    - Check for duplicates
 *    - Save and return
 * 
 * 6. updatePrice(Long observationId, Long userId, UpdatePriceObservationRequest request) → PriceObservationResponse
 *    - Fetch observation
 *    - Verify item belongs to user (security!)
 *    - Update fields
 *    - Return updated response
 * 
 * 7. deletePrice(Long observationId, Long userId) → void
 *    - Fetch observation
 *    - Verify ownership through item
 *    - Delete
 * 
 * HINTS:
 * - Always verify ownership through the item
 * - Use itemService.existsByIdAndUserId() for validation
 * - Consider creating a private helper method: validateItemOwnership(itemId, userId)
 */

// TODO: Create the complete PriceObservationService class here
```

---

### Testing Services with Mockito

**Complete Example: UserService Test**

```java
/**
 * Unit tests for UserService.
 * 
 * Testing Strategy:
 * - Mock dependencies (repositories, other services)
 * - Test business logic in isolation
 * - Verify interactions with mocks
 * - Test both happy paths and error cases
 * 
 * @ExtendWith(MockitoExtension.class):
 * - Enables Mockito annotations
 * - Auto-closes resources
 * - Better than @RunWith for JUnit 5
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock  // Creates a mock instance
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks  // Creates instance and injects mocks
    private UserService userService;
    
    private User testUser;
    private CreateUserRequest createRequest;
    
    @BeforeEach
    void setUp() {
        // Arrange - Create test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("johndoe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("encrypted_password");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setAge(25);
        testUser.setUserRole(UserRolesEnum.USER);
        
        createRequest = new CreateUserRequest(
            "johndoe",
            "john@example.com",
            "John",
            "Doe",
            "SecurePass123!",
            25
        );
    }
    
    // ========== READ OPERATION TESTS ==========
    
    @Test
    void getUserById_WhenUserExists_ReturnsUserResponse() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // Act
        UserResponse response = userService.getUserById(1L);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.username()).isEqualTo("johndoe");
        
        // Verify mock interaction
        verify(userRepository).findById(1L);
    }
    
    @Test
    void getUserById_WhenUserNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found with id: 999");
        
        verify(userRepository).findById(999L);
    }
    
    // ========== WRITE OPERATION TESTS ==========
    
    @Test
    void registerUser_WhenValid_CreatesUser() {
        // Arrange
        when(userRepository.existsByEmail(createRequest.email())).thenReturn(false);
        when(userRepository.existsByUsername(createRequest.username())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.password())).thenReturn("encrypted");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // Act
        UserResponse response = userService.registerUser(createRequest);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("john@example.com");
        
        // Verify all interactions
        verify(userRepository).existsByEmail(createRequest.email());
        verify(userRepository).existsByUsername(createRequest.username());
        verify(passwordEncoder).encode(createRequest.password());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void registerUser_WhenEmailExists_ThrowsDuplicateResourceException() {
        // Arrange
        when(userRepository.existsByEmail(createRequest.email())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(createRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Email already registered");
        
        // Verify no save was attempted
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void registerUser_WhenUsernameExists_ThrowsDuplicateResourceException() {
        // Arrange
        when(userRepository.existsByEmail(createRequest.email())).thenReturn(false);
        when(userRepository.existsByUsername(createRequest.username())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(createRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Username already taken");
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void registerUser_WhenAgeUnder13_ThrowsBusinessValidationException() {
        // Arrange
        CreateUserRequest youngUser = new CreateUserRequest(
            "young",
            "young@example.com",
            "Young",
            "User",
            "Password123!",
            12  // Under 13
        );
        
        when(userRepository.existsByEmail(youngUser.email())).thenReturn(false);
        when(userRepository.existsByUsername(youngUser.username())).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(youngUser))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("at least 13 years old");
    }
    
    @Test
    void updateUser_WhenUserExists_UpdatesUser() {
        // Arrange
        UpdateUserRequest updateRequest = new UpdateUserRequest(
            "UpdatedFirst",
            "UpdatedLast",
            30
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // Act
        UserResponse response = userService.updateUser(1L, updateRequest);
        
        // Assert
        assertThat(response.firstName()).isEqualTo("UpdatedFirst");
        assertThat(response.lastName()).isEqualTo("UpdatedLast");
        assertThat(response.age()).isEqualTo(30);
        
        verify(userRepository).findById(1L);
        // Note: No verify for save() - dirty checking handles it
    }
    
    @Test
    void changePassword_WhenCurrentPasswordCorrect_ChangesPassword() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest(
            "OldPass123!",
            "NewPass456!"
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("OldPass123!", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPass456!")).thenReturn("new_encrypted");
        
        // Act
        userService.changePassword(1L, request);
        
        // Assert
        assertThat(testUser.getPassword()).isEqualTo("new_encrypted");
        verify(passwordEncoder).matches("OldPass123!", testUser.getPassword());
        verify(passwordEncoder).encode("NewPass456!");
    }
    
    @Test
    void changePassword_WhenCurrentPasswordIncorrect_ThrowsInvalidPasswordException() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest(
            "WrongPass",
            "NewPass456!"
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPass", testUser.getPassword())).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword(1L, request))
            .isInstanceOf(InvalidPasswordException.class)
            .hasMessageContaining("Current password is incorrect");
        
        verify(passwordEncoder, never()).encode(anyString());
    }
}
```

---

### Practice Exercise 8: Test ItemService (Challenge)

```java
/**
 * TODO: Write comprehensive unit tests for ItemService
 * 
 * REQUIREMENTS:
 * 1. Use @ExtendWith(MockitoExtension.class)
 * 2. Mock: ItemRepository, UserService, CategoryRepository
 * 3. Use @InjectMocks for ItemService
 * 4. Create test data in @BeforeEach
 * 5. Write tests for all service methods
 * 6. Test happy paths AND error cases
 * 
 * TESTS TO WRITE:
 * 
 * READ OPERATIONS:
 * 1. testGetItemById_WhenExists_ReturnsItem()
 * 2. testGetItemById_WhenNotFound_ThrowsException()
 * 3. testGetItemById_WhenNotOwned_ThrowsException()
 * 4. testGetAllItemsForUser_WhenUserExists_ReturnsItems()
 * 5. testGetAllItemsForUser_WhenUserNotFound_ThrowsException()
 * 
 * WRITE OPERATIONS:
 * 6. testCreateItem_WhenValid_CreatesItem()
 * 7. testCreateItem_WhenUserNotFound_ThrowsException()
 * 8. testCreateItem_WhenNameExists_ThrowsException()
 * 9. testCreateItem_WithValidCategory_SetsCategory()
 * 10. testCreateItem_WithCategoryNotOwnedByUser_ThrowsException()
 * 11. testUpdateItem_WhenValid_UpdatesItem()
 * 12. testUpdateItem_WhenNotOwned_ThrowsException()
 * 13. testDeleteItem_WhenOwned_DeletesItem()
 * 14. testDeleteItem_WhenNotOwned_ThrowsException()
 * 
 * MOCKITO PATTERNS TO USE:
 * - when(...).thenReturn(...) for stubbing
 * - verify(...) to check interactions
 * - verify(..., never()) to ensure something wasn't called
 * - any(Class.class) for argument matchers
 * - assertThatThrownBy(...) for exception testing
 */

// TODO: Write the complete ItemServiceTest class
```

---

### Service Phase Checklist

Before moving to Phase 5:
- [ ] All services annotated with @Service
- [ ] Constructor injection via @RequiredArgsConstructor
- [ ] @Transactional on all methods (readOnly for reads)
- [ ] Business rules validated in services
- [ ] Custom exceptions thrown for errors
- [ ] Logging added for important operations
- [ ] DTOs converted to/from entities
- [ ] Services tested with Mockito
- [ ] Both happy and error paths tested
- [ ] Mock interactions verified

---

_Continue to Part 3 for Controller Layer, Exception Handling, Security, Testing Strategy, and Production Features..._
