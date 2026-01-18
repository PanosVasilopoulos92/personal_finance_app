# Complete JUnit Testing Guide for Spring Boot Applications

> **Your Reference Guide**: A comprehensive, practical guide to writing effective unit tests in Java with JUnit 5, Mockito, and AssertJ.

---

## Table of Contents

1. [Core Testing Principles](#core-testing-principles)
2. [Essential Dependencies](#essential-dependencies)
3. [Test Anatomy - The AAA Pattern](#test-anatomy---the-aaa-pattern)
4. [Mockito Deep Dive](#mockito-deep-dive)
5. [AssertJ Assertions](#assertj-assertions)
6. [Testing Strategy](#testing-strategy)
7. [Complete Examples](#complete-examples)
8. [Common Patterns & Solutions](#common-patterns--solutions)
9. [Testing Checklist](#testing-checklist)
10. [Anti-Patterns to Avoid](#anti-patterns-to-avoid)

---

## Core Testing Principles

### What is a Unit Test?

A unit test verifies **one specific behavior** of **one component** in **isolation**.

**Key Characteristics:**
- ✅ **Fast**: Runs in milliseconds
- ✅ **Isolated**: No database, no network, no file system
- ✅ **Repeatable**: Same result every time
- ✅ **Self-validating**: Pass or fail, no manual inspection
- ✅ **Independent**: Can run in any order

### The Testing Pyramid

```
        /\
       /  \      E2E Tests (Few)
      /____\     
     /      \    Integration Tests (Some)
    /________\   
   /          \  Unit Tests (Many)
  /____________\ 
```

**Unit tests should be ~70% of your test suite.**

---

## Essential Dependencies

### Maven Dependencies (pom.xml)

```xml
<dependencies>
    <!-- JUnit 5 (Jupiter) -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ (Fluent Assertions) -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Boot Test Starter (includes all above) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**💡 Tip:** If using Spring Boot Starter Test, you get all these dependencies automatically.

---

## Test Anatomy - The AAA Pattern

Every test follows this structure:

```java
@Test
void methodName_scenario_expectedBehavior() {
    // ARRANGE: Set up test data and configure mocks
    
    // ACT: Execute the method being tested
    
    // ASSERT: Verify the results
}
```

### Test Method Naming Conventions

**Pattern:** `methodName_scenario_expectedResult`

**Examples:**
```java
findUserById_existingId_returnsUser()
findUserById_nonExistingId_throwsNotFoundException()
createOrder_validData_createsOrderSuccessfully()
createOrder_insufficientStock_throwsStockException()
calculateDiscount_premiumUser_applies20PercentDiscount()
```

**Benefits:**
- Self-documenting
- Clear intent
- Easy to locate specific scenarios

---

## Mockito Deep Dive

### 1. Core Annotations

#### @ExtendWith(MockitoExtension.class)

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    // Enables Mockito annotations
}
```

**What it does:** Initializes Mockito before each test, enables `@Mock`, `@InjectMocks`, etc.

---

#### @Mock - Creating Mock Objects

```java
@Mock
private UserRepository userRepository;

@Mock
private EmailService emailService;
```

**What it does:**
- Creates a fake implementation
- All methods return default values (null, 0, false, empty collections)
- You control the behavior via `when()` statements

**When to use:**
- External dependencies (repositories, services, clients)
- Things you don't want to test in this unit test
- Anything that would slow down the test (DB, network, file I/O)

---

#### @InjectMocks - The System Under Test

```java
@InjectMocks
private UserService userService;
```

**What it does:**
- Creates a **real** instance of UserService
- Automatically injects all `@Mock` dependencies into it
- Simulates what Spring does with dependency injection

**Important:** Only use on the class you're testing, not dependencies.

---

#### @Spy - Partial Mocking

```java
@Spy
private List<String> spyList = new ArrayList<>();
```

**What it does:**
- Creates a real object that you can selectively mock
- Real methods are called unless you stub them

**When to use:**
- Testing a class that depends on itself (calls its own methods)
- You want mostly real behavior with some overrides

**Example:**
```java
@Spy
@InjectMocks
private UserService userService;

@Test
void testMethodThatCallsAnotherMethod() {
    // Mock one method while keeping others real
    doReturn(true).when(userService).isValidEmail(any());
    
    // Other methods execute normally
    userService.registerUser(request);
}
```

---

### 2. Stubbing - Configuring Mock Behavior

#### when().thenReturn() - Simple Return Values

```java
// Return a value
when(userRepository.findById(1L)).thenReturn(Optional.of(user));

// Return different values on consecutive calls
when(userRepository.count())
    .thenReturn(10L)
    .thenReturn(20L)
    .thenReturn(30L);
```

---

#### when().thenThrow() - Throwing Exceptions

```java
// Throw an exception
when(userRepository.findById(999L))
    .thenThrow(new EntityNotFoundException("User not found"));

// Or throw exception class (it will be instantiated)
when(userRepository.save(any()))
    .thenThrow(DataIntegrityViolationException.class);
```

---

#### doReturn() - Alternative Syntax

```java
// Useful when method is void or you're using spies
doReturn(user).when(userRepository).findById(1L);

// For void methods
doNothing().when(emailService).sendEmail(any());

// Throw exception for void methods
doThrow(new RuntimeException()).when(emailService).sendEmail(any());
```

**When to use `doReturn()` vs `when()`:**
- Use `when()` for most cases (more readable)
- Use `doReturn()` for spies or void methods

---

### 3. Argument Matchers

#### Common Matchers

```java
// Exact value
when(userRepository.findById(eq(1L))).thenReturn(Optional.of(user));

// Any value of type
when(userRepository.save(any(User.class))).thenReturn(savedUser);
when(emailService.send(anyString())).thenReturn(true);

// Primitive types
anyInt()
anyLong()
anyBoolean()
anyDouble()

// Collections
anyList()
anySet()
anyMap()
anyCollection()
```

#### Custom Matchers

```java
// Using argThat() for complex matching
when(userRepository.save(argThat(user -> 
    user.getEmail().endsWith("@company.com")
))).thenReturn(savedUser);

// Combining matchers
when(orderService.createOrder(
    argThat(order -> order.getTotal() > 100),
    eq("PREMIUM")
)).thenReturn(createdOrder);
```

**⚠️ Important Rule:** If you use a matcher for one argument, use matchers for ALL arguments.

```java
// ❌ WRONG - mixing matcher with exact value
when(service.method(any(), "exact")).thenReturn(result);

// ✅ CORRECT
when(service.method(any(), eq("exact"))).thenReturn(result);
```

---

### 4. Verification - Checking Method Calls

#### Basic Verification

```java
// Verify method was called once
verify(userRepository).save(any(User.class));

// Verify specific times
verify(emailService, times(2)).sendEmail(any());
verify(emailService, never()).sendEmail(any());
verify(emailService, atLeast(1)).sendEmail(any());
verify(emailService, atMost(3)).sendEmail(any());
```

#### Verification with Argument Capture

```java
// Capture arguments for detailed inspection
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
verify(userRepository).save(userCaptor.capture());

User capturedUser = userCaptor.getValue();
assertThat(capturedUser.getEmail()).isEqualTo("test@example.com");
assertThat(capturedUser.getStatus()).isEqualTo(StatusEnum.ACTIVE);
```

#### Verification Order

```java
InOrder inOrder = inOrder(userRepository, emailService);

inOrder.verify(userRepository).save(any());
inOrder.verify(emailService).sendEmail(any());
```

---

## AssertJ Assertions

### Why AssertJ over JUnit Assertions?

```java
// JUnit style (old)
assertEquals(expected, actual);  // Which is which? 🤔
assertTrue(user.isActive());

// AssertJ style (preferred)
assertThat(actual).isEqualTo(expected);  // Clear!
assertThat(user.isActive()).isTrue();
```

**Benefits:**
- More readable
- Better error messages
- Fluent API (method chaining)
- Rich assertion library

---

### Common AssertJ Assertions

#### Basic Assertions

```java
// Null checks
assertThat(user).isNotNull();
assertThat(result).isNull();

// Equality
assertThat(user.getName()).isEqualTo("John");
assertThat(user.getAge()).isEqualTo(25);

// Comparisons
assertThat(user.getAge()).isGreaterThan(18);
assertThat(user.getAge()).isLessThanOrEqualTo(100);
assertThat(price).isBetween(10.0, 20.0);

// Boolean
assertThat(user.isActive()).isTrue();
assertThat(user.isDeleted()).isFalse();

// Strings
assertThat(email).startsWith("john");
assertThat(email).endsWith("@example.com");
assertThat(email).contains("@");
assertThat(email).containsIgnoringCase("JOHN");
assertThat(name).isNotEmpty();
assertThat(name).isNotBlank();
```

#### Collection Assertions

```java
List<User> users = userService.findAll();

// Size checks
assertThat(users).hasSize(3);
assertThat(users).isEmpty();
assertThat(users).isNotEmpty();

// Content checks
assertThat(users).contains(user1, user2);
assertThat(users).containsExactly(user1, user2, user3);  // Order matters
assertThat(users).containsExactlyInAnyOrder(user3, user1, user2);  // Order doesn't matter
assertThat(users).doesNotContain(user4);

// Element assertions
assertThat(users)
    .extracting(User::getName)
    .containsExactly("Alice", "Bob", "Charlie");

// Complex filtering
assertThat(users)
    .filteredOn(user -> user.getAge() > 18)
    .hasSize(2);

// All elements satisfy condition
assertThat(users)
    .allMatch(user -> user.getAge() > 0)
    .noneMatch(user -> user.getName().isEmpty());
```

#### Exception Assertions

```java
// Assert that code throws exception
assertThatThrownBy(() -> userService.findById(999L))
    .isInstanceOf(EntityNotFoundException.class)
    .hasMessage("User not found")
    .hasMessageContaining("not found");

// Assert exception with cause
assertThatThrownBy(() -> service.process())
    .isInstanceOf(ProcessingException.class)
    .hasCauseInstanceOf(IOException.class);

// Assert code doesn't throw
assertThatCode(() -> userService.findById(1L))
    .doesNotThrowAnyException();

// Alternative: Using assertThatExceptionOfType
assertThatExceptionOfType(EntityNotFoundException.class)
    .isThrownBy(() -> userService.findById(999L))
    .withMessage("User not found");
```

#### Object Field Assertions

```java
// Assert specific fields
assertThat(user)
    .extracting("name", "email", "age")
    .containsExactly("John", "john@example.com", 25);

// Using method references (type-safe)
assertThat(user)
    .extracting(User::getName, User::getEmail)
    .containsExactly("John", "john@example.com");

// Recursive comparison (ignoring specific fields)
assertThat(actualUser)
    .usingRecursiveComparison()
    .ignoringFields("id", "createdAt")
    .isEqualTo(expectedUser);
```

#### Optional Assertions

```java
Optional<User> optionalUser = userService.findById(1L);

assertThat(optionalUser).isPresent();
assertThat(optionalUser).isEmpty();

// Assert content
assertThat(optionalUser)
    .isPresent()
    .get()
    .extracting(User::getName)
    .isEqualTo("John");

// Alternative
assertThat(optionalUser)
    .hasValueSatisfying(user -> {
        assertThat(user.getName()).isEqualTo("John");
        assertThat(user.getEmail()).contains("@");
    });
```

---

## Testing Strategy

### What to Test

#### 1. Happy Path (Success Scenarios)

```java
@Test
void createUser_validData_createsUserSuccessfully() {
    // Test the main success flow
}
```

#### 2. Error Scenarios

```java
@Test
void createUser_duplicateEmail_throwsException() {
    // Test business rule violations
}

@Test
void createUser_invalidEmail_throwsValidationException() {
    // Test validation errors
}
```

#### 3. Edge Cases

```java
@Test
void findUsers_emptyDatabase_returnsEmptyList() {
    // Test boundary conditions
}

@Test
void processOrder_quantityZero_throwsException() {
    // Test edge values
}
```

#### 4. Business Logic

```java
@Test
void calculateDiscount_premiumMember_applies20Percent() {
    // Test domain-specific rules
}

@Test
void calculateDiscount_firstTimeBuyer_applies10Percent() {
    // Test various business scenarios
}
```

---

### Coverage Guidelines

**What needs 100% coverage:**
- Business logic methods
- Complex algorithms
- Critical paths (payment processing, security)

**What needs selective coverage:**
- Simple getters/setters (usually skip)
- Configuration classes (usually skip)
- DTOs/Entities (skip unless they have logic)

**Aim for:**
- 80-90% line coverage
- 100% coverage of critical business logic
- Focus on meaningful tests, not coverage numbers

---

## Complete Examples

### Example 1: Service Layer Testing

```java
package com.example.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private ProductService productService;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private OrderService orderService;
    
    private Order testOrder;
    private Product testProduct;
    private CreateOrderRequest request;
    
    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
            .id(1L)
            .name("Laptop")
            .price(1000.0)
            .stock(10)
            .build();
            
        testOrder = Order.builder()
            .id(1L)
            .productId(1L)
            .quantity(2)
            .totalPrice(2000.0)
            .status(OrderStatus.PENDING)
            .build();
            
        request = new CreateOrderRequest(1L, 2);
    }
    
    @Nested
    @DisplayName("createOrder() tests")
    class CreateOrderTests {
        
        @Test
        @DisplayName("Should create order successfully with valid data")
        void createOrder_validData_createsOrder() {
            // Arrange
            when(productService.findById(1L)).thenReturn(testProduct);
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
            
            // Act
            OrderResponse response = orderService.createOrder(request);
            
            // Assert
            assertThat(response).isNotNull();
            assertThat(response.totalPrice()).isEqualTo(2000.0);
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
            
            // Verify interactions
            verify(productService).findById(1L);
            verify(orderRepository).save(any(Order.class));
            verify(emailService).sendOrderConfirmation(any());
        }
        
        @Test
        @DisplayName("Should throw exception when product not found")
        void createOrder_productNotFound_throwsException() {
            // Arrange
            when(productService.findById(999L))
                .thenThrow(new EntityNotFoundException("Product not found"));
            
            CreateOrderRequest invalidRequest = new CreateOrderRequest(999L, 2);
            
            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(invalidRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");
            
            verify(orderRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Should throw exception when insufficient stock")
        void createOrder_insufficientStock_throwsException() {
            // Arrange
            Product lowStockProduct = Product.builder()
                .id(1L)
                .stock(1)
                .build();
                
            when(productService.findById(1L)).thenReturn(lowStockProduct);
            
            CreateOrderRequest highQuantityRequest = new CreateOrderRequest(1L, 5);
            
            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(highQuantityRequest))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Not enough stock");
            
            verify(orderRepository, never()).save(any());
        }
        
        @Test
        @DisplayName("Should calculate correct total price")
        void createOrder_validData_calculatesCorrectPrice() {
            // Arrange
            when(productService.findById(1L)).thenReturn(testProduct);
            
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(orderCaptor.capture())).thenReturn(testOrder);
            
            // Act
            orderService.createOrder(request);
            
            // Assert
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getTotalPrice()).isEqualTo(2000.0); // 1000 * 2
        }
    }
    
    @Nested
    @DisplayName("findOrderById() tests")
    class FindOrderByIdTests {
        
        @Test
        @DisplayName("Should return order when it exists")
        void findOrderById_existingId_returnsOrder() {
            // Arrange
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            
            // Act
            OrderResponse response = orderService.findOrderById(1L);
            
            // Assert
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
        }
        
        @Test
        @DisplayName("Should throw exception when order not found")
        void findOrderById_nonExistingId_throwsException() {
            // Arrange
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());
            
            // Act & Assert
            assertThatThrownBy(() -> orderService.findOrderById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found");
        }
    }
    
    @Nested
    @DisplayName("cancelOrder() tests")
    class CancelOrderTests {
        
        @Test
        @DisplayName("Should cancel pending order successfully")
        void cancelOrder_pendingOrder_cancelsSuccessfully() {
            // Arrange
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            
            // Act
            orderService.cancelOrder(1L);
            
            // Assert
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            
            Order cancelledOrder = orderCaptor.getValue();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
        
        @Test
        @DisplayName("Should throw exception when cancelling shipped order")
        void cancelOrder_shippedOrder_throwsException() {
            // Arrange
            Order shippedOrder = Order.builder()
                .id(1L)
                .status(OrderStatus.SHIPPED)
                .build();
                
            when(orderRepository.findById(1L)).thenReturn(Optional.of(shippedOrder));
            
            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Cannot cancel shipped order");
            
            verify(orderRepository, never()).save(any());
        }
    }
}
```

---

### Example 2: Testing with Lists and Collections

```java
@Test
@DisplayName("Should return all active users")
void findActiveUsers_multipleUsers_returnsActiveOnly() {
    // Arrange
    User activeUser1 = User.builder().id(1L).status(Status.ACTIVE).build();
    User activeUser2 = User.builder().id(2L).status(Status.ACTIVE).build();
    User inactiveUser = User.builder().id(3L).status(Status.INACTIVE).build();
    
    when(userRepository.findByStatus(Status.ACTIVE))
        .thenReturn(List.of(activeUser1, activeUser2));
    
    // Act
    List<UserResponse> responses = userService.findActiveUsers();
    
    // Assert
    assertThat(responses)
        .hasSize(2)
        .extracting(UserResponse::id)
        .containsExactly(1L, 2L);
    
    assertThat(responses)
        .allMatch(user -> user.status() == Status.ACTIVE);
}
```

---

### Example 3: Testing Methods That Call Other Methods

```java
@Test
@DisplayName("Should validate email before creating user")
void createUser_invalidEmail_throwsException() {
    // Arrange
    CreateUserRequest requestWithInvalidEmail = new CreateUserRequest(
        "john",
        "invalid-email",  // Invalid
        "password"
    );
    
    // Use a Spy to test internal method calls
    UserService spyService = spy(userService);
    doReturn(false).when(spyService).isValidEmail("invalid-email");
    
    // Act & Assert
    assertThatThrownBy(() -> spyService.createUser(requestWithInvalidEmail))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Invalid email");
    
    verify(spyService).isValidEmail("invalid-email");
    verify(userRepository, never()).save(any());
}
```

---

### Example 4: Testing Void Methods

```java
@Test
@DisplayName("Should send welcome email after user registration")
void registerUser_validUser_sendsWelcomeEmail() {
    // Arrange
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    doNothing().when(emailService).sendWelcomeEmail(any());
    
    // Act
    userService.registerUser(createUserRequest);
    
    // Assert
    ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendWelcomeEmail(emailCaptor.capture());
    
    assertThat(emailCaptor.getValue()).isEqualTo("john@example.com");
}
```

---

## Common Patterns & Solutions

### Pattern 1: Testing Pagination

```java
@Test
void findUsers_withPagination_returnsCorrectPage() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    Page<User> userPage = new PageImpl<>(
        List.of(testUser),
        pageable,
        100  // total elements
    );
    
    when(userRepository.findAll(pageable)).thenReturn(userPage);
    
    // Act
    Page<UserResponse> result = userService.findUsers(pageable);
    
    // Assert
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getTotalElements()).isEqualTo(100);
    assertThat(result.getTotalPages()).isEqualTo(10);
}
```

---

### Pattern 2: Testing Date/Time Logic

```java
@Test
void isExpired_pastDate_returnsTrue() {
    // Arrange
    LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
    Subscription subscription = Subscription.builder()
        .expiryDate(pastDate)
        .build();
    
    // Act
    boolean result = subscriptionService.isExpired(subscription);
    
    // Assert
    assertThat(result).isTrue();
}
```

**Better approach with Clock injection:**

```java
// In your service
public class SubscriptionService {
    private final Clock clock;
    
    public SubscriptionService(Clock clock) {
        this.clock = clock;
    }
    
    public boolean isExpired(Subscription subscription) {
        return LocalDateTime.now(clock).isAfter(subscription.getExpiryDate());
    }
}

// In your test
@Test
void isExpired_pastDate_returnsTrue() {
    // Arrange
    Clock fixedClock = Clock.fixed(
        Instant.parse("2024-01-15T10:00:00Z"),
        ZoneId.of("UTC")
    );
    SubscriptionService service = new SubscriptionService(fixedClock);
    
    Subscription subscription = Subscription.builder()
        .expiryDate(LocalDateTime.parse("2024-01-14T10:00:00"))
        .build();
    
    // Act
    boolean result = service.isExpired(subscription);
    
    // Assert
    assertThat(result).isTrue();
}
```

---

### Pattern 3: Testing Transactional Methods

```java
@Test
void transferMoney_sufficientBalance_transfersSuccessfully() {
    // Arrange
    Account fromAccount = Account.builder()
        .id(1L)
        .balance(1000.0)
        .build();
        
    Account toAccount = Account.builder()
        .id(2L)
        .balance(500.0)
        .build();
    
    when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
    when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
    
    // Act
    accountService.transferMoney(1L, 2L, 200.0);
    
    // Assert
    ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
    verify(accountRepository, times(2)).save(accountCaptor.capture());
    
    List<Account> savedAccounts = accountCaptor.getAllValues();
    
    // Verify from account
    assertThat(savedAccounts.get(0).getBalance()).isEqualTo(800.0);
    
    // Verify to account
    assertThat(savedAccounts.get(1).getBalance()).isEqualTo(700.0);
}
```

---

### Pattern 4: Testing Methods with Multiple Scenarios

Use `@ParameterizedTest` for testing multiple inputs:

```java
@ParameterizedTest
@CsvSource({
    "0, 0.0",      // No discount
    "100, 10.0",   // 10% discount
    "500, 50.0",   // 10% discount
    "1000, 150.0"  // 15% discount for premium
})
void calculateDiscount_variousAmounts_appliesCorrectDiscount(
    double amount, 
    double expectedDiscount
) {
    // Arrange
    Order order = Order.builder().amount(amount).build();
    
    // Act
    double discount = orderService.calculateDiscount(order);
    
    // Assert
    assertThat(discount).isEqualTo(expectedDiscount);
}
```

---

### Pattern 5: Testing Exception Messages

```java
@Test
void processPayment_invalidCard_throwsExceptionWithDetails() {
    // Arrange
    Payment payment = Payment.builder()
        .cardNumber("1234")
        .build();
    
    when(paymentGateway.process(any()))
        .thenThrow(new PaymentException("Card declined: Insufficient funds"));
    
    // Act & Assert
    assertThatThrownBy(() -> paymentService.processPayment(payment))
        .isInstanceOf(PaymentException.class)
        .hasMessage("Card declined: Insufficient funds")
        .hasNoCause();
}
```

---

## Testing Checklist

Use this before considering a method "fully tested":

### ✅ Scenarios Covered
- [ ] Happy path (success case)
- [ ] All error scenarios (exceptions)
- [ ] Edge cases (null, empty, boundaries)
- [ ] Business rule validations

### ✅ Assertions
- [ ] Return value is correct
- [ ] Side effects occurred (saves, emails, etc.)
- [ ] State changes are correct
- [ ] Exception types and messages are correct

### ✅ Verifications
- [ ] Dependencies called correctly
- [ ] Dependencies NOT called when they shouldn't be
- [ ] Call order matters? Use `InOrder`
- [ ] Arguments passed correctly? Use `ArgumentCaptor`

### ✅ Code Quality
- [ ] Test names are descriptive
- [ ] Test is isolated (no dependencies on other tests)
- [ ] No hardcoded values (use constants or test data builders)
- [ ] Test is readable (follows AAA pattern)

---

## Anti-Patterns to Avoid

### ❌ 1. Testing Implementation Details

```java
// ❌ BAD - Testing how it works
@Test
void createUser_callsRepositorySave() {
    userService.createUser(request);
    verify(userRepository).save(any());  // Who cares HOW it saves?
}

// ✅ GOOD - Testing what it does
@Test
void createUser_validRequest_returnsCreatedUser() {
    UserResponse response = userService.createUser(request);
    assertThat(response).isNotNull();
    assertThat(response.id()).isNotNull();
}
```

**Principle:** Test behavior, not implementation.

---

### ❌ 2. Over-Mocking

```java
// ❌ BAD - Mocking simple objects
@Mock
private CreateUserRequest request;

@Mock
private UserResponse response;

// ✅ GOOD - Use real objects for DTOs
CreateUserRequest request = new CreateUserRequest("john", "john@example.com");
```

**Principle:** Only mock external dependencies.

---

### ❌ 3. One Giant Test

```java
// ❌ BAD - Testing everything in one test
@Test
void testEverything() {
    // Test create
    // Test update
    // Test delete
    // Test find
    // ... 200 lines later
}

// ✅ GOOD - Separate tests
@Test
void createUser_validData_createsUser() { }

@Test
void updateUser_validData_updatesUser() { }

@Test
void deleteUser_existingId_deletesUser() { }
```

**Principle:** One test = One scenario.

---

### ❌ 4. Ignoring Test Failures

```java
// ❌ BAD - Commenting out failing tests
// @Test
// void thisTestFails() {
//     // TODO: Fix later
// }

// ✅ GOOD - Fix or use @Disabled with reason
@Disabled("Waiting for API endpoint to be deployed")
@Test
void testNewFeature() {
    // ...
}
```

**Principle:** Green tests or explicit disabled tests only.

---

### ❌ 5. Testing Getters and Setters

```java
// ❌ BAD - Waste of time
@Test
void testGetName() {
    user.setName("John");
    assertThat(user.getName()).isEqualTo("John");
}

// ✅ GOOD - Skip simple getters/setters
// Only test if they have logic
```

**Principle:** Don't test framework/library code.

---

### ❌ 6. Fragile Tests (Coupled to Data)

```java
// ❌ BAD - Hardcoded IDs
@Test
void findUser() {
    User user = userService.findById(42L);  // What if ID changes?
    assertThat(user.getName()).isEqualTo("John");
}

// ✅ GOOD - Use test data builders
@Test
void findUser() {
    User savedUser = userRepository.save(testUser);
    User foundUser = userService.findById(savedUser.getId());
    assertThat(foundUser.getName()).isEqualTo(testUser.getName());
}
```

**Principle:** Tests should be independent of external state.

---

## Quick Reference Card

### Mockito Cheat Sheet

```java
// Creating mocks
@Mock private Repository repo;
@InjectMocks private Service service;
@Spy private List<String> list;

// Stubbing
when(repo.find(1L)).thenReturn(entity);
when(repo.find(any())).thenThrow(RuntimeException.class);
doReturn(value).when(spy).method();
doThrow(exception).when(mock).voidMethod();

// Verification
verify(mock).method();
verify(mock, times(2)).method();
verify(mock, never()).method();
verify(mock, atLeast(1)).method();

// Argument capture
ArgumentCaptor<Type> captor = ArgumentCaptor.forClass(Type.class);
verify(mock).method(captor.capture());
Type arg = captor.getValue();

// Matchers
any(), anyString(), anyInt(), anyList()
eq(value), argThat(predicate)
```

---

### AssertJ Cheat Sheet

```java
// Basic
assertThat(value).isEqualTo(expected);
assertThat(value).isNotNull();
assertThat(boolean).isTrue();

// Strings
assertThat(str).contains("text");
assertThat(str).startsWith("prefix");
assertThat(str).isNotEmpty();

// Numbers
assertThat(num).isGreaterThan(5);
assertThat(num).isBetween(1, 10);

// Collections
assertThat(list).hasSize(3);
assertThat(list).contains(item);
assertThat(list).extracting(User::getName).contains("John");

// Exceptions
assertThatThrownBy(() -> code())
    .isInstanceOf(Exception.class)
    .hasMessage("error");

// Objects
assertThat(obj).extracting("field1", "field2")
    .containsExactly(value1, value2);
```

---

## Final Tips

1. **Write tests FIRST (TDD)** - It improves design
2. **Keep tests simple** - Complex tests = hard to maintain
3. **Name tests well** - Future you will thank you
4. **Don't test third-party code** - Trust the framework
5. **Refactor tests too** - They're code, treat them as such
6. **Run tests often** - Catch issues early
7. **Aim for fast tests** - Slow tests won't get run
8. **Review test coverage** - But don't obsess over 100%

---

## Next Steps

1. **Practice**: Write tests for your existing code
2. **TDD**: Try writing tests before implementation
3. **Read**: Study existing tests in your codebase
4. **Refactor**: Improve existing tests using this guide

Remember: **Good tests are an investment, not a cost.** They save debugging time, prevent regressions, and document your code's behavior.

---

*Happy Testing! 🧪*
