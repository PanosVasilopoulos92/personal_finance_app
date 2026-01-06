# 🚀 Personal Finance App - Learning Roadmap Part 5
## Phase 8: Comprehensive Testing Strategy

> **Continuation from Parts 1-4** - Complete Phases 1-7 before starting here!

---

## Phase 8: Comprehensive Testing Strategy

**Duration:** 1-2 weeks  
**Goal:** Build a robust test suite with high coverage and confidence  
**Difficulty:** ⭐⭐⭐⭐

### 📚 What You'll Learn

- Testing pyramid and strategy
- Unit testing with JUnit 5 and Mockito
- Integration testing with @SpringBootTest
- Repository testing with @DataJpaTest
- API testing with MockMvc
- Test containers for real database testing
- Test coverage with JaCoCo
- Testing best practices and patterns

### 🎯 The Testing Pyramid

```
        /       /  \        E2E Tests (Few)
      /    \       - Full application flow
     /------\      - Slowest, most expensive
    /           /   API    \    Integration Tests (Some)
  /   Tests    \   - Multiple components
 /--------------\  - Medium speed
/                \ 
/  Unit Tests    \ Unit Tests (Many)
/  (Majority)     \- Single component
-------------------|- Fast, isolated
```

**Our Testing Strategy:**
- **70% Unit Tests** - Services, utilities, helpers
- **20% Integration Tests** - Repositories, full stack
- **10% API Tests** - Controllers with MockMvc

---

### Why Testing Matters

**Without tests:**
```
You: "I changed this one line"
App: *Everything breaks*
You: "How?! It's just one line!"
*Spends 3 hours debugging*
```

**With tests:**
```
You: "I changed this one line"
Tests: "UserService.createUser() now fails!"
You: "Oh, I see the issue" *Fixes in 2 minutes*
```

---

### Step 1: Test Dependencies

Add to `pom.xml`:

```xml
<!-- JUnit 5 (included in spring-boot-starter-test) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Test containers for real database testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- JaCoCo for test coverage -->
<dependency>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
</dependency>
```

**JaCoCo Plugin Configuration:**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
                <execution>
                    <id>jacoco-check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <configuration>
                        <rules>
                            <rule>
                                <element>PACKAGE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.80</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## Unit Testing

### Complete Example: UserService Unit Tests

```java
/**
 * Unit tests for UserService.
 * 
 * Unit Test Characteristics:
 * - Tests single component in isolation
 * - Mocks all dependencies
 * - Fast execution (no database, no Spring context)
 * - Focuses on business logic
 * 
 * @ExtendWith(MockitoExtension.class):
 * - Enables Mockito annotations
 * - Initializes mocks before each test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    private CreateUserRequest createRequest;
    
    @BeforeEach
    void setUp() {
        // Arrange - Create test data before each test
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
    
    // ========== READ OPERATIONS ==========
    
    @Test
    @DisplayName("getUserById - when user exists - should return UserResponse")
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
        
        // Verify mock interactions
        verify(userRepository, times(1)).findById(1L);
    }
    
    @Test
    @DisplayName("getUserById - when user not found - should throw ResourceNotFoundException")
    void getUserById_WhenUserNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long nonExistentId = 999L;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found with id: 999");
        
        verify(userRepository).findById(nonExistentId);
    }
    
    @Test
    @DisplayName("getUserByEmail - when user exists - should return UserResponse")
    void getUserByEmail_WhenUserExists_ReturnsUserResponse() {
        // Arrange
        when(userRepository.findByEmail("john@example.com"))
            .thenReturn(Optional.of(testUser));
        
        // Act
        UserResponse response = userService.getUserByEmail("john@example.com");
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("john@example.com");
        
        verify(userRepository).findByEmail("john@example.com");
    }
    
    @Test
    @DisplayName("searchUsers - with valid term - should return matching users")
    void searchUsers_WithValidTerm_ReturnsMatchingUsers() {
        // Arrange
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("janedoe");
        user2.setEmail("jane@example.com");
        user2.setFirstName("Jane");
        user2.setLastName("Doe");
        
        when(userRepository.searchByName("doe"))
            .thenReturn(Arrays.asList(testUser, user2));
        
        // Act
        List<UserResponse> results = userService.searchUsers("doe");
        
        // Assert
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(UserResponse::username)
            .containsExactlyInAnyOrder("johndoe", "janedoe");
        
        verify(userRepository).searchByName("doe");
    }
    
    @Test
    @DisplayName("searchUsers - with empty term - should return empty list")
    void searchUsers_WithEmptyTerm_ReturnsEmptyList() {
        // Act
        List<UserResponse> results = userService.searchUsers("   ");
        
        // Assert
        assertThat(results).isEmpty();
        
        // Verify repository was NOT called
        verify(userRepository, never()).searchByName(anyString());
    }
    
    // ========== WRITE OPERATIONS ==========
    
    @Test
    @DisplayName("registerUser - with valid data - should create user")
    void registerUser_WithValidData_CreatesUser() {
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
        
        // Verify interactions in correct order
        InOrder inOrder = inOrder(userRepository, passwordEncoder);
        inOrder.verify(userRepository).existsByEmail(createRequest.email());
        inOrder.verify(userRepository).existsByUsername(createRequest.username());
        inOrder.verify(passwordEncoder).encode(createRequest.password());
        inOrder.verify(userRepository).save(any(User.class));
    }
    
    @Test
    @DisplayName("registerUser - with existing email - should throw DuplicateResourceException")
    void registerUser_WithExistingEmail_ThrowsDuplicateResourceException() {
        // Arrange
        when(userRepository.existsByEmail(createRequest.email())).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(createRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Email already registered");
        
        // Verify save was never called
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("registerUser - with existing username - should throw DuplicateResourceException")
    void registerUser_WithExistingUsername_ThrowsDuplicateResourceException() {
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
    @DisplayName("registerUser - with age under 13 - should throw BusinessValidationException")
    void registerUser_WithAgeUnder13_ThrowsBusinessValidationException() {
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
    @DisplayName("updateUser - when user exists - should update and return UserResponse")
    void updateUser_WhenUserExists_UpdatesAndReturnsUserResponse() {
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
        
        // Verify user was fetched but save() not called (dirty checking)
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    @DisplayName("changePassword - with correct current password - should change password")
    void changePassword_WithCorrectCurrentPassword_ChangesPassword() {
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
    @DisplayName("changePassword - with incorrect current password - should throw InvalidPasswordException")
    void changePassword_WithIncorrectCurrentPassword_ThrowsInvalidPasswordException() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest(
            "WrongPassword",
            "NewPass456!"
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", testUser.getPassword())).thenReturn(false);
        
        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword(1L, request))
            .isInstanceOf(InvalidPasswordException.class)
            .hasMessageContaining("Current password is incorrect");
        
        // Verify new password was NOT encoded
        verify(passwordEncoder, never()).encode(anyString());
    }
    
    @Test
    @DisplayName("deleteUser - when user exists - should delete user")
    void deleteUser_WhenUserExists_DeletesUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // Act
        userService.deleteUser(1L);
        
        // Assert
        verify(userRepository).findById(1L);
        verify(userRepository).delete(testUser);
    }
    
    // ========== HELPER METHODS TESTS ==========
    
    @Test
    @DisplayName("existsById - when user exists - should return true")
    void existsById_WhenUserExists_ReturnsTrue() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        
        // Act
        boolean exists = userService.existsById(1L);
        
        // Assert
        assertThat(exists).isTrue();
        verify(userRepository).existsById(1L);
    }
    
    @Test
    @DisplayName("isEmailAvailable - when email available - should return true")
    void isEmailAvailable_WhenEmailAvailable_ReturnsTrue() {
        // Arrange
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        
        // Act
        boolean available = userService.isEmailAvailable("new@example.com");
        
        // Assert
        assertThat(available).isTrue();
        verify(userRepository).existsByEmail("new@example.com");
    }
}
```

---

### Unit Testing Best Practices

**1. AAA Pattern (Arrange, Act, Assert)**
```java
@Test
void testMethod() {
    // Arrange - Set up test data and mocks
    when(repository.findById(1L)).thenReturn(Optional.of(entity));
    
    // Act - Execute the method being tested
    Result result = service.doSomething(1L);
    
    // Assert - Verify the outcome
    assertThat(result).isNotNull();
    verify(repository).findById(1L);
}
```

**2. Descriptive Test Names**
```java
// ✅ GOOD - Clear what's being tested
@Test
@DisplayName("createUser - with duplicate email - should throw DuplicateResourceException")
void createUser_WithDuplicateEmail_ThrowsDuplicateResourceException() {}

// ❌ BAD - Unclear what's tested
@Test
void testCreateUser() {}
```

**3. One Assertion Per Test (when possible)**
```java
// ✅ GOOD - Tests one specific scenario
@Test
void getUserById_WhenUserExists_ReturnsUser() {
    // Test happy path
}

@Test
void getUserById_WhenUserNotFound_ThrowsException() {
    // Test error case separately
}

// ⚠️ ACCEPTABLE - Related assertions for same scenario
@Test
void getUserById_WhenUserExists_ReturnsCompleteUserData() {
    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.email()).isEqualTo("john@example.com");
    assertThat(response.username()).isEqualTo("johndoe");
}
```

**4. Verify Mock Interactions**
```java
// ✅ GOOD - Verify important interactions
verify(repository).save(any(User.class));
verify(passwordEncoder).encode("password");

// ✅ GOOD - Verify something was NOT called
verify(repository, never()).delete(any(User.class));

// ✅ GOOD - Verify call count
verify(repository, times(2)).findById(anyLong());

// ✅ GOOD - Verify order of calls
InOrder inOrder = inOrder(repo1, repo2);
inOrder.verify(repo1).findById(1L);
inOrder.verify(repo2).save(any());
```

---

## Integration Testing - Repositories

### Complete Example: UserRepository Integration Tests

```java
/**
 * Integration tests for UserRepository.
 * 
 * Integration Test Characteristics:
 * - Tests component with its real dependencies
 * - Uses real database (H2 in-memory or TestContainers)
 * - Slower than unit tests
 * - Verifies actual database operations
 * 
 * @DataJpaTest:
 * - Configures in-memory database
 * - Scans @Entity classes
 * - Configures Spring Data JPA
 * - Provides TestEntityManager
 * - Rolls back after each test (clean slate)
 */
@DataJpaTest
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("johndoe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("encrypted_password");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setAge(25);
        testUser.setUserRole(UserRolesEnum.USER);
        
        entityManager.persist(testUser);
        entityManager.flush();
    }
    
    @Test
    @DisplayName("findByEmail - when user exists - should return user")
    void findByEmail_WhenUserExists_ReturnsUser() {
        // Act
        Optional<User> found = userRepository.findByEmail("john@example.com");
        
        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
        assertThat(found.get().getUsername()).isEqualTo("johndoe");
    }
    
    @Test
    @DisplayName("findByEmail - when user doesn't exist - should return empty")
    void findByEmail_WhenUserDoesNotExist_ReturnsEmpty() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        
        // Assert
        assertThat(found).isEmpty();
    }
    
    @Test
    @DisplayName("existsByEmail - when email exists - should return true")
    void existsByEmail_WhenEmailExists_ReturnsTrue() {
        // Act
        boolean exists = userRepository.existsByEmail("john@example.com");
        
        // Assert
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("existsByUsername - when username exists - should return true")
    void existsByUsername_WhenUsernameExists_ReturnsTrue() {
        // Act
        boolean exists = userRepository.existsByUsername("johndoe");
        
        // Assert
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("searchByName - with matching first name - should return users")
    void searchByName_WithMatchingFirstName_ReturnsUsers() {
        // Arrange - Create another user with similar name
        User user2 = new User();
        user2.setUsername("johnny");
        user2.setEmail("johnny@example.com");
        user2.setPassword("password");
        user2.setFirstName("Johnny");
        user2.setLastName("Smith");
        user2.setAge(30);
        user2.setUserRole(UserRolesEnum.USER);
        entityManager.persist(user2);
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
    @DisplayName("searchByName - with matching last name - should return users")
    void searchByName_WithMatchingLastName_ReturnsUsers() {
        // Act
        List<User> results = userRepository.searchByName("doe");
        
        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLastName()).isEqualTo("Doe");
    }
    
    @Test
    @DisplayName("findByIdAndActive - when user active - should return user")
    void findByIdAndActive_WhenUserActive_ReturnsUser() {
        // Act
        Optional<User> found = userRepository.findByIdAndActive(testUser.getId(), true);
        
        // Assert
        assertThat(found).isPresent();
    }
    
    @Test
    @DisplayName("countByActive - should return correct count")
    void countByActive_ReturnsCorrectCount() {
        // Arrange - Create inactive user
        User inactiveUser = new User();
        inactiveUser.setUsername("inactive");
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setPassword("password");
        inactiveUser.setFirstName("Inactive");
        inactiveUser.setLastName("User");
        inactiveUser.setAge(20);
        inactiveUser.setUserRole(UserRolesEnum.USER);
        // Note: Add active field to User entity or use soft delete pattern
        entityManager.persist(inactiveUser);
        entityManager.flush();
        
        // Act
        long activeCount = userRepository.countByActive(true);
        
        // Assert - Depends on your soft delete implementation
        assertThat(activeCount).isGreaterThanOrEqualTo(1);
    }
    
    @Test
    @DisplayName("findByActive - with pagination - should return correct page")
    void findByActive_WithPagination_ReturnsCorrectPage() {
        // Arrange - Create multiple users
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setPassword("password");
            user.setFirstName("User");
            user.setLastName(String.valueOf(i));
            user.setAge(20 + i);
            user.setUserRole(UserRolesEnum.USER);
            entityManager.persist(user);
        }
        entityManager.flush();
        
        // Act - Request first page with 5 items
        Pageable pageable = PageRequest.of(0, 5, Sort.by("lastName").ascending());
        Page<User> page = userRepository.findByActive(true, pageable);
        
        // Assert
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(10);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }
    
    @Test
    @DisplayName("save - should persist user with generated ID")
    void save_ShouldPersistUserWithGeneratedId() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("password");
        newUser.setFirstName("New");
        newUser.setLastName("User");
        newUser.setAge(28);
        newUser.setUserRole(UserRolesEnum.USER);
        
        // Act
        User saved = userRepository.save(newUser);
        entityManager.flush();
        
        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();  // From BaseEntity
        assertThat(saved.getUpdatedAt()).isNotNull();  // From BaseEntity
    }
    
    @Test
    @DisplayName("delete - should remove user from database")
    void delete_ShouldRemoveUserFromDatabase() {
        // Arrange
        Long userId = testUser.getId();
        
        // Act
        userRepository.delete(testUser);
        entityManager.flush();
        
        // Assert
        Optional<User> found = userRepository.findById(userId);
        assertThat(found).isEmpty();
    }
}
```

---

## Integration Testing - Full Stack with TestContainers

### TestContainers Setup

```java
/**
 * Base class for integration tests using TestContainers.
 * 
 * TestContainers:
 * - Spins up real MySQL database in Docker container
 * - Tests against production database (not H2)
 * - Catches MySQL-specific issues
 * - Automatically cleans up after tests
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);  // Reuse container across tests for speed
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

### Complete Example: ItemService Integration Test

```java
/**
 * Integration test for ItemService.
 * Tests the full stack: Controller → Service → Repository → Database
 */
@DisplayName("ItemService Integration Tests")
class ItemServiceIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private ItemService itemService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private ItemRepository itemRepository;
    
    private User testUser;
    private Category testCategory;
    
    @BeforeEach
    void setUp() {
        // Clean database
        itemRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAge(25);
        testUser.setUserRole(UserRolesEnum.USER);
        testUser = userRepository.save(testUser);
        
        // Create test category
        testCategory = new Category();
        testCategory.setName("Groceries");
        testCategory.setUser(testUser);
        testCategory = categoryRepository.save(testCategory);
    }
    
    @Test
    @DisplayName("createItem - with valid data - should create and return item")
    void createItem_WithValidData_CreatesAndReturnsItem() {
        // Arrange
        CreateItemRequest request = new CreateItemRequest(
            "Apple",
            "Fresh apple",
            ItemUnitEnum.PIECE,
            "Brand A",
            null,
            testCategory.getId()
        );
        
        // Act
        ItemResponse response = itemService.createItem(testUser.getId(), request);
        
        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Apple");
        assertThat(response.category()).isNotNull();
        assertThat(response.category().name()).isEqualTo("Groceries");
        
        // Verify in database
        Optional<Item> savedItem = itemRepository.findById(response.id());
        assertThat(savedItem).isPresent();
        assertThat(savedItem.get().getName()).isEqualTo("Apple");
    }
    
    @Test
    @DisplayName("createItem - with duplicate name for same user - should throw DuplicateResourceException")
    void createItem_WithDuplicateName_ThrowsDuplicateResourceException() {
        // Arrange - Create first item
        CreateItemRequest request1 = new CreateItemRequest(
            "Apple",
            "Fresh apple",
            ItemUnitEnum.PIECE,
            null,
            null,
            null
        );
        itemService.createItem(testUser.getId(), request1);
        
        // Try to create duplicate
        CreateItemRequest request2 = new CreateItemRequest(
            "Apple",  // Same name
            "Another apple",
            ItemUnitEnum.PIECE,
            null,
            null,
            null
        );
        
        // Act & Assert
        assertThatThrownBy(() -> itemService.createItem(testUser.getId(), request2))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("already exists");
    }
    
    @Test
    @DisplayName("getItemById - when item exists and user is owner - should return item")
    void getItemById_WhenItemExistsAndUserIsOwner_ReturnsItem() {
        // Arrange - Create item
        CreateItemRequest createRequest = new CreateItemRequest(
            "Banana",
            "Yellow banana",
            ItemUnitEnum.PIECE,
            null,
            null,
            null
        );
        ItemResponse created = itemService.createItem(testUser.getId(), createRequest);
        
        // Act
        ItemResponse retrieved = itemService.getItemById(created.id(), testUser.getId());
        
        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.name()).isEqualTo("Banana");
    }
    
    @Test
    @DisplayName("getItemById - when user is not owner - should throw ResourceNotFoundException")
    void getItemById_WhenUserNotOwner_ThrowsResourceNotFoundException() {
        // Arrange - Create another user
        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");
        otherUser.setAge(30);
        otherUser.setUserRole(UserRolesEnum.USER);
        otherUser = userRepository.save(otherUser);
        
        // Create item for testUser
        CreateItemRequest request = new CreateItemRequest(
            "Orange",
            "Citrus fruit",
            ItemUnitEnum.PIECE,
            null,
            null,
            null
        );
        ItemResponse created = itemService.createItem(testUser.getId(), request);
        
        // Act & Assert - Try to access with otherUser
        assertThatThrownBy(() -> itemService.getItemById(created.id(), otherUser.getId()))
            .isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    @DisplayName("updateItem - with valid data - should update and return item")
    void updateItem_WithValidData_UpdatesAndReturnsItem() {
        // Arrange - Create item
        CreateItemRequest createRequest = new CreateItemRequest(
            "Milk",
            "Dairy product",
            ItemUnitEnum.LITER,
            "Brand A",
            null,
            null
        );
        ItemResponse created = itemService.createItem(testUser.getId(), createRequest);
        
        // Update request
        UpdateItemRequest updateRequest = new UpdateItemRequest(
            "Organic Milk",  // Updated name
            "Organic dairy product",  // Updated description
            ItemUnitEnum.LITER,
            "Brand B",  // Updated brand
            null,
            null
        );
        
        // Act
        ItemResponse updated = itemService.updateItem(created.id(), testUser.getId(), updateRequest);
        
        // Assert
        assertThat(updated.name()).isEqualTo("Organic Milk");
        assertThat(updated.description()).isEqualTo("Organic dairy product");
        assertThat(updated.brand()).isEqualTo("Brand B");
        
        // Verify in database
        Optional<Item> savedItem = itemRepository.findById(created.id());
        assertThat(savedItem).isPresent();
        assertThat(savedItem.get().getName()).isEqualTo("Organic Milk");
    }
    
    @Test
    @DisplayName("deleteItem - when user is owner - should delete item")
    void deleteItem_WhenUserIsOwner_DeletesItem() {
        // Arrange - Create item
        CreateItemRequest request = new CreateItemRequest(
            "Bread",
            "Whole wheat",
            ItemUnitEnum.PIECE,
            null,
            null,
            null
        );
        ItemResponse created = itemService.createItem(testUser.getId(), request);
        
        // Act
        itemService.deleteItem(created.id(), testUser.getId());
        
        // Assert - Item should be deleted
        Optional<Item> deleted = itemRepository.findById(created.id());
        assertThat(deleted).isEmpty();
    }
    
    @Test
    @DisplayName("getAllItemsForUser - should return all user's items")
    void getAllItemsForUser_ReturnsAllUserItems() {
        // Arrange - Create multiple items
        for (int i = 0; i < 5; i++) {
            CreateItemRequest request = new CreateItemRequest(
                "Item " + i,
                "Description " + i,
                ItemUnitEnum.PIECE,
                null,
                null,
                null
            );
            itemService.createItem(testUser.getId(), request);
        }
        
        // Act
        List<ItemResponse> items = itemService.getAllItemsForUser(testUser.getId());
        
        // Assert
        assertThat(items).hasSize(5);
        assertThat(items)
            .extracting(ItemResponse::name)
            .containsExactlyInAnyOrder("Item 0", "Item 1", "Item 2", "Item 3", "Item 4");
    }
    
    @Test
    @DisplayName("searchItems - with matching term - should return matching items")
    void searchItems_WithMatchingTerm_ReturnsMatchingItems() {
        // Arrange - Create items
        CreateItemRequest apple1 = new CreateItemRequest(
            "Apple Juice",
            "Fresh juice",
            ItemUnitEnum.LITER,
            null,
            null,
            null
        );
        CreateItemRequest apple2 = new CreateItemRequest(
            "Apple Pie",
            "Baked dessert",
            ItemUnitEnum.PIECE,
            null,
            null,
            null
        );
        CreateItemRequest orange = new CreateItemRequest(
            "Orange",
            "Citrus fruit",
            ItemUnitEnum.PIECE,
            null,
            null,
            null
        );
        
        itemService.createItem(testUser.getId(), apple1);
        itemService.createItem(testUser.getId(), apple2);
        itemService.createItem(testUser.getId(), orange);
        
        // Act - Search for "apple" (case-insensitive)
        List<ItemResponse> results = itemService.searchItems(testUser.getId(), "apple");
        
        // Assert
        assertThat(results).hasSize(2);
        assertThat(results)
            .extracting(ItemResponse::name)
            .containsExactlyInAnyOrder("Apple Juice", "Apple Pie");
    }
}
```

---

## API Testing with MockMvc

### Complete Example: UserController API Tests

```java
/**
 * API tests for UserController using MockMvc.
 * 
 * MockMvc:
 * - Tests HTTP layer without starting server
 * - Faster than full @SpringBootTest with REST calls
 * - Tests request/response, status codes, headers
 * - Can mock service layer for isolated controller tests
 */
@WebMvcTest(UserController.class)
@DisplayName("UserController API Tests")
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;  // For JSON serialization
    
    private UserResponse userResponse;
    
    @BeforeEach
    void setUp() {
        userResponse = new UserResponse(
            1L,
            "john@example.com",
            "johndoe",
            "John",
            "Doe",
            "John Doe",
            UserRolesEnum.USER,
            25,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    
    @Test
    @DisplayName("GET /api/users/{id} - when user exists - should return 200 OK")
    void getUserById_WhenUserExists_Returns200() throws Exception {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(userResponse);
        
        // Act & Assert
        mockMvc.perform(get("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("john@example.com"))
            .andExpect(jsonPath("$.username").value("johndoe"))
            .andExpect(jsonPath("$.role").value("USER"));
        
        verify(userService).getUserById(1L);
    }
    
    @Test
    @DisplayName("GET /api/users/{id} - when user not found - should return 404")
    void getUserById_WhenUserNotFound_Returns404() throws Exception {
        // Arrange
        when(userService.getUserById(999L))
            .thenThrow(new ResourceNotFoundException("User not found with id: 999"));
        
        // Act & Assert
        mockMvc.perform(get("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("User not found with id: 999"));
        
        verify(userService).getUserById(999L);
    }
    
    @Test
    @DisplayName("POST /api/users - with valid data - should return 201 CREATED")
    void createUser_WithValidData_Returns201() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest(
            "johndoe",
            "john@example.com",
            "John",
            "Doe",
            "SecurePass123!",
            25
        );
        
        when(userService.registerUser(any(CreateUserRequest.class)))
            .thenReturn(userResponse);
        
        // Act & Assert
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("john@example.com"));
        
        verify(userService).registerUser(any(CreateUserRequest.class));
    }
    
    @Test
    @DisplayName("POST /api/users - with invalid data - should return 400 BAD REQUEST")
    void createUser_WithInvalidData_Returns400() throws Exception {
        // Arrange - Invalid email
        CreateUserRequest request = new CreateUserRequest(
            "johndoe",
            "invalid-email",  // Invalid
            "John",
            "Doe",
            "SecurePass123!",
            25
        );
        
        // Act & Assert
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.email").exists());
        
        // Service should NOT be called due to validation failure
        verify(userService, never()).registerUser(any());
    }
    
    @Test
    @DisplayName("PUT /api/users/{id} - with valid data - should return 200 OK")
    void updateUser_WithValidData_Returns200() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest(
            "UpdatedFirst",
            "UpdatedLast",
            30
        );
        
        UserResponse updatedResponse = new UserResponse(
            1L,
            "john@example.com",
            "johndoe",
            "UpdatedFirst",
            "UpdatedLast",
            "UpdatedFirst UpdatedLast",
            UserRolesEnum.USER,
            30,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        when(userService.updateUser(eq(1L), any(UpdateUserRequest.class)))
            .thenReturn(updatedResponse);
        
        // Act & Assert
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("UpdatedFirst"))
            .andExpect(jsonPath("$.lastName").value("UpdatedLast"))
            .andExpect(jsonPath("$.age").value(30));
        
        verify(userService).updateUser(eq(1L), any(UpdateUserRequest.class));
    }
    
    @Test
    @DisplayName("DELETE /api/users/{id} - should return 204 NO CONTENT")
    void deleteUser_Returns204() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(1L);
        
        // Act & Assert
        mockMvc.perform(delete("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
        
        verify(userService).deleteUser(1L);
    }
    
    @Test
    @DisplayName("GET /api/users/search - with term - should return matching users")
    void searchUsers_WithTerm_ReturnsMatchingUsers() throws Exception {
        // Arrange
        List<UserResponse> users = Arrays.asList(userResponse);
        when(userService.searchUsers("john")).thenReturn(users);
        
        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("term", "john")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].username").value("johndoe"));
        
        verify(userService).searchUsers("john");
    }
}
```

---

### Practice Exercise 12: Write Comprehensive Tests

```java
/**
 * TODO: Write comprehensive tests for ItemService
 * 
 * UNIT TESTS (ItemServiceTest):
 * 1. Test getItemById - happy path
 * 2. Test getItemById - item not found
 * 3. Test getItemById - user not owner (security)
 * 4. Test createItem - valid data
 * 5. Test createItem - duplicate name
 * 6. Test createItem - user not found
 * 7. Test createItem - category not found
 * 8. Test createItem - category belongs to different user
 * 9. Test updateItem - valid data
 * 10. Test updateItem - not owner
 * 11. Test deleteItem - success
 * 12. Test deleteItem - not owner
 * 13. Test searchItems - matching results
 * 14. Test searchItems - empty term
 * 
 * INTEGRATION TESTS (ItemServiceIntegrationTest):
 * 1. Test full create flow with database
 * 2. Test uniqueness constraint enforcement
 * 3. Test cascade operations with category
 * 4. Test pagination
 * 5. Test search with real database
 * 
 * API TESTS (ItemControllerTest):
 * 1. Test GET /api/items/{id} - 200 OK
 * 2. Test GET /api/items/{id} - 404 NOT FOUND
 * 3. Test POST /api/items - 201 CREATED
 * 4. Test POST /api/items - 400 BAD REQUEST (validation)
 * 5. Test PUT /api/items/{id} - 200 OK
 * 6. Test DELETE /api/items/{id} - 204 NO CONTENT
 * 7. Test GET /api/items/search - 200 OK with results
 */
```

---

## Test Coverage with JaCoCo

After running tests, generate coverage report:

```bash
# Run tests and generate coverage report
mvn clean test

# View coverage report
# Open: target/site/jacoco/index.html in browser
```

**Coverage Goals:**
- Overall: 80%+
- Services: 90%+
- Repositories: 80%+
- Controllers: 85%+
- DTOs: Not necessary (no logic)
- Entities: Not necessary (just data)

---

## Testing Checklist

Before considering testing complete:
- [ ] Unit tests for all service methods
- [ ] Integration tests for all repositories
- [ ] API tests for all controllers
- [ ] Test both happy paths and error cases
- [ ] Test security (authorization checks)
- [ ] Test validation errors
- [ ] Test edge cases (null, empty, boundary values)
- [ ] All tests have descriptive names
- [ ] All tests follow AAA pattern
- [ ] Mock interactions verified
- [ ] Test coverage ≥ 80%
- [ ] No flaky tests (pass consistently)
- [ ] Tests run quickly (< 2 minutes total)

---

## Testing Best Practices Summary

**1. Write Tests First (TDD) or Immediately After**
```
Write code → Write test → Refactor → Repeat
```

**2. Keep Tests Independent**
```java
// ✅ GOOD - Each test sets up its own data
@BeforeEach
void setUp() {
    testData = createTestData();
}

// ❌ BAD - Tests depend on order
static User user;  // Shared state!
```

**3. Use Descriptive Names**
```java
// ✅ GOOD
@Test
void createUser_WithDuplicateEmail_ThrowsDuplicateResourceException() {}

// ❌ BAD
@Test
void test1() {}
```

**4. Test One Thing Per Test**
```java
// ✅ GOOD - Focused test
@Test
void getUserById_WhenUserExists_ReturnsUser() {
    // Test only happy path
}

// ❌ BAD - Testing multiple scenarios
@Test
void testGetUser() {
    // Test happy path
    // Test not found
    // Test security
    // Too much!
}
```

**5. Don't Test Framework Code**
```java
// ❌ DON'T TEST - Spring Data handles this
@Test
void testSave() {
    repository.save(entity);
    assertThat(repository.findById(1L)).isPresent();
}

// ✅ DO TEST - Your business logic
@Test
void createUser_WithDuplicateEmail_ThrowsException() {
    // Test YOUR validation logic
}
```

---

**Congratulations!** 🎉

You now have a comprehensive testing strategy covering:
- Unit tests (fast, isolated)
- Integration tests (real dependencies)
- API tests (HTTP layer)
- Test containers (production database)
- Coverage reporting

Your application is now well-tested and ready for production!

Next: **Phase 9 - Production-Ready Features** where we'll add configuration, logging, documentation, and deployment!
