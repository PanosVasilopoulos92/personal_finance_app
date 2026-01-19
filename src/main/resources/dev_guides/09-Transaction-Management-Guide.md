# 🔄 Transaction Management Deep Dive

> **Goal:** Master transaction management to build reliable, data-consistent Spring Boot applications  
> **Stack:** Java 25, Spring Boot 4, Spring Framework 7, Hibernate 7  
> **Philosophy:** Understand the "why" before the "how" — transactions are about guarantees, not just annotations

---

## 📋 Table of Contents

1. [Why Transactions Matter](#1-why-transactions-matter)
2. [ACID Properties - The Foundation](#2-acid-properties---the-foundation)
3. [How Spring Transactions Work (The Proxy Pattern)](#3-how-spring-transactions-work-the-proxy-pattern)
4. [The @Transactional Annotation Deep Dive](#4-the-transactional-annotation-deep-dive)
5. [Propagation Levels - When Transactions Meet](#5-propagation-levels---when-transactions-meet)
6. [Isolation Levels - Concurrent Access Control](#6-isolation-levels---concurrent-access-control)
7. [Rollback Rules - When Things Go Wrong](#7-rollback-rules---when-things-go-wrong)
8. [Read-Only Transactions - Performance Optimization](#8-read-only-transactions---performance-optimization)
9. [Transaction Boundaries - Where to Put @Transactional](#9-transaction-boundaries---where-to-put-transactional)
10. [Common Pitfalls & How to Avoid Them](#10-common-pitfalls--how-to-avoid-them)
11. [Testing Transactions](#11-testing-transactions)
12. [Real-World Patterns](#12-real-world-patterns)
13. [Debugging Transaction Issues](#13-debugging-transaction-issues)
14. [Quick Reference](#14-quick-reference)

---

## 1. Why Transactions Matter

### The Problem: Partial Updates

Imagine transferring money between bank accounts:

```java
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    Account from = accountRepository.findById(fromId).orElseThrow();
    Account to = accountRepository.findById(toId).orElseThrow();
    
    from.setBalance(from.getBalance().subtract(amount));  // Step 1: Debit
    accountRepository.save(from);
    
    // 💥 What if the server crashes HERE?
    // Money is gone from 'from' account but never arrived at 'to' account!
    
    to.setBalance(to.getBalance().add(amount));           // Step 2: Credit
    accountRepository.save(to);
}
```

**Without transactions:** If the system fails between Step 1 and Step 2, money vanishes. The database is left in an **inconsistent state**.

### The Solution: Atomic Operations

```java
@Transactional  // All or nothing!
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    Account from = accountRepository.findById(fromId).orElseThrow();
    Account to = accountRepository.findById(toId).orElseThrow();
    
    from.setBalance(from.getBalance().subtract(amount));
    to.setBalance(to.getBalance().add(amount));
    
    // Both saves happen together, or neither happens
}
```

**With transactions:** Either BOTH accounts are updated, or NEITHER is. The database is ALWAYS in a consistent state.

---

## 2. ACID Properties - The Foundation

Every transaction guarantees four properties. Understanding these is crucial!

### **A**tomicity - All or Nothing

```
Transaction {
    Step 1: Debit $100 from Account A  ──┐
    Step 2: Credit $100 to Account B   ──┼── Either ALL succeed or ALL fail
    Step 3: Log the transfer           ──┘
}
```

If ANY step fails, ALL steps are rolled back. The database looks like the transaction never started.

### **C**onsistency - Valid State to Valid State

```
Before Transaction:
    Account A: $500
    Account B: $300
    Total:     $800

After Transaction (success):
    Account A: $400
    Account B: $400
    Total:     $800  ✅ Still consistent!

After Transaction (failure):
    Account A: $500  (rolled back)
    Account B: $300  (unchanged)
    Total:     $800  ✅ Still consistent!
```

Transactions preserve database invariants (constraints, business rules).

### **I**solation - Transactions Don't Interfere

```
Transaction 1 (reads balance)     Transaction 2 (updates balance)
─────────────────────────────     ─────────────────────────────────
BEGIN                             
SELECT balance FROM accounts      BEGIN
  → sees $500                     UPDATE accounts SET balance = $400
                                  COMMIT
SELECT balance FROM accounts      
  → still sees $500 (isolated!)   
COMMIT
```

Concurrent transactions don't see each other's uncommitted changes (depending on isolation level).

### **D**urability - Committed = Permanent

Once a transaction commits:
- Data survives server crashes
- Data survives power failures
- Data is written to persistent storage

```java
@Transactional
public void createOrder(Order order) {
    orderRepository.save(order);
}  // After this method returns, the order is GUARANTEED to be saved
```

---

## 3. How Spring Transactions Work (The Proxy Pattern)

This is **the most important section** for understanding why some `@Transactional` calls don't work!

### The Proxy Mechanism

When you add `@Transactional`, Spring doesn't modify your class. Instead, it creates a **proxy** that wraps your bean:

```
                    ┌─────────────────────────────────────────┐
                    │           Spring Container              │
                    │                                         │
   Client           │    ┌──────────────────┐                │
   Request ────────►│───►│  Proxy (AOP)     │                │
                    │    │                  │                │
                    │    │  1. Begin TX     │                │
                    │    │  2. Call target ─┼──► Your Service│
                    │    │  3. Commit/      │     (Real Bean)│
                    │    │     Rollback     │                │
                    │    └──────────────────┘                │
                    │                                         │
                    └─────────────────────────────────────────┘
```

### What Actually Happens

```java
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        // Your business logic
    }
}

// What Spring actually creates (simplified):
public class OrderService$$SpringProxy extends OrderService {
    
    private final OrderService target;           // Your actual bean
    private final TransactionManager txManager;
    
    @Override
    public void createOrder(Order order) {
        TransactionStatus status = txManager.getTransaction(definition);
        try {
            target.createOrder(order);  // Calls YOUR method
            txManager.commit(status);
        } catch (RuntimeException e) {
            txManager.rollback(status);
            throw e;
        }
    }
}
```

### The Golden Rule

> **Transactions only work when called THROUGH THE PROXY**

This means:
- ✅ External calls to `@Transactional` methods work
- ❌ Internal calls (self-invocation) bypass the proxy!

```java
@Service
public class OrderService {
    
    public void processOrder(Order order) {
        validateOrder(order);
        createOrder(order);  // ❌ WRONG! This bypasses the proxy!
    }
    
    @Transactional
    public void createOrder(Order order) {
        // Transaction NOT started! Called internally.
        orderRepository.save(order);
    }
}
```

**Why?** When `processOrder` calls `createOrder`, it's calling `this.createOrder()` — the actual method, not the proxy method.

### Solution: Self-Injection or Refactoring

**Option 1: Inject yourself (not recommended, but works)**
```java
@Service
public class OrderService {
    
    @Autowired
    private OrderService self;  // Injects the proxy!
    
    public void processOrder(Order order) {
        validateOrder(order);
        self.createOrder(order);  // ✅ Goes through proxy
    }
    
    @Transactional
    public void createOrder(Order order) {
        orderRepository.save(order);
    }
}
```

**Option 2: Move to separate service (recommended)**
```java
@Service
@RequiredArgsConstructor
public class OrderProcessingService {
    
    private final OrderService orderService;
    
    public void processOrder(Order order) {
        validateOrder(order);
        orderService.createOrder(order);  // ✅ Goes through proxy
    }
}

@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        orderRepository.save(order);
    }
}
```

**Option 3: Move @Transactional to the calling method**
```java
@Service
public class OrderService {
    
    @Transactional  // ✅ Transaction covers everything
    public void processOrder(Order order) {
        validateOrder(order);
        createOrder(order);  // Internal call is fine now
    }
    
    private void createOrder(Order order) {  // No @Transactional needed
        orderRepository.save(order);
    }
}
```

---

## 4. The @Transactional Annotation Deep Dive

### Basic Usage

```java
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional  // Class-level: applies to ALL public methods
public class UserService {
    
    public void createUser(User user) {
        // Transactional
    }
    
    @Transactional(readOnly = true)  // Method-level overrides class-level
    public User getUser(Long id) {
        // Read-only transaction
    }
}
```

### All Attributes

```java
@Transactional(
    // Propagation behavior (how to handle existing transactions)
    propagation = Propagation.REQUIRED,  // Default
    
    // Isolation level (how to handle concurrent access)
    isolation = Isolation.DEFAULT,  // Uses database default
    
    // Timeout in seconds (-1 = no timeout)
    timeout = 30,
    
    // Read-only hint for optimization
    readOnly = false,
    
    // Which exceptions trigger rollback
    rollbackFor = {CustomException.class},
    rollbackForClassName = {"com.example.CustomException"},
    
    // Which exceptions should NOT trigger rollback
    noRollbackFor = {IgnorableException.class},
    noRollbackForClassName = {"com.example.IgnorableException"},
    
    // Which transaction manager to use (for multiple datasources)
    transactionManager = "primaryTransactionManager",
    
    // Label for monitoring/debugging
    label = {"order-processing"}
)
public void complexOperation() { }
```

### Where to Place @Transactional

| Location | Effect |
|----------|--------|
| Class level | Applies to ALL public methods |
| Method level | Overrides class-level settings |
| Interface | Works but not recommended |
| Private method | ❌ DOES NOT WORK (not proxied) |
| Protected method | ❌ DOES NOT WORK (not proxied) |
| Final method | ❌ DOES NOT WORK (cannot be overridden) |

---

## 5. Propagation Levels - When Transactions Meet

Propagation defines what happens when a `@Transactional` method calls another `@Transactional` method.

### Visual Overview

```
Method A (with TX)          Method B (with TX)           Result
─────────────────────────────────────────────────────────────────────
REQUIRED (default)    ───►  REQUIRED                     Same TX
REQUIRED              ───►  REQUIRES_NEW                 New TX (A suspended)
REQUIRED              ───►  NESTED                       Nested TX (savepoint)
REQUIRED              ───►  MANDATORY                    Same TX
REQUIRED              ───►  SUPPORTS                     Same TX
REQUIRED              ───►  NOT_SUPPORTED                No TX (A suspended)
REQUIRED              ───►  NEVER                        ❌ Exception!

No TX                 ───►  REQUIRED                     New TX
No TX                 ───►  REQUIRES_NEW                 New TX
No TX                 ───►  MANDATORY                    ❌ Exception!
No TX                 ───►  SUPPORTS                     No TX
No TX                 ───►  NOT_SUPPORTED                No TX
No TX                 ───►  NEVER                        No TX
```

### REQUIRED (Default) - Join or Create

```java
@Service
public class OrderService {
    
    @Autowired
    private PaymentService paymentService;
    
    @Transactional  // propagation = REQUIRED (default)
    public void createOrder(Order order) {
        orderRepository.save(order);
        paymentService.processPayment(order);  // Joins THIS transaction
    }
}

@Service
public class PaymentService {
    
    @Transactional  // propagation = REQUIRED (default)
    public void processPayment(Order order) {
        // Runs in SAME transaction as createOrder()
        // If this fails, order is also rolled back!
        paymentRepository.save(new Payment(order));
    }
}
```

**Use case:** Most operations. You want everything to succeed or fail together.

### REQUIRES_NEW - Always Start Fresh

```java
@Service
public class OrderService {
    
    @Autowired
    private AuditService auditService;
    
    @Transactional
    public void createOrder(Order order) {
        orderRepository.save(order);
        
        try {
            // Even if order creation fails, audit log should persist!
            auditService.logOrderAttempt(order);
        } catch (Exception e) {
            // Audit failure shouldn't affect order
        }
    }
}

@Service
public class AuditService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderAttempt(Order order) {
        // Runs in a SEPARATE transaction
        // Commits independently of the calling transaction
        auditRepository.save(new AuditLog("Order attempted: " + order.getId()));
    }
}
```

**What happens:**
```
┌─────────────────────────────────────────────────────────────┐
│ Transaction 1 (createOrder)                                 │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ save(order)                                         │   │
│   │                                                     │   │
│   │ ┌───────────────────────────────────────────────┐   │   │
│   │ │ Transaction 2 (logOrderAttempt) - INDEPENDENT │   │   │
│   │ │   save(auditLog)                              │   │   │
│   │ │   COMMIT ✓                                    │   │   │
│   │ └───────────────────────────────────────────────┘   │   │
│   │                                                     │   │
│   │ // If exception here, order rolls back but          │   │
│   │ // audit log is ALREADY COMMITTED                   │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**Use cases:**
- Audit logging (must persist even if main operation fails)
- Sending notifications
- Generating sequence numbers
- Any operation that should commit independently

### NESTED - Savepoint Within Transaction

```java
@Service
public class BatchService {
    
    @Autowired
    private ItemService itemService;
    
    @Transactional
    public void processBatch(List<Item> items) {
        for (Item item : items) {
            try {
                itemService.processItem(item);  // Each item in nested TX
            } catch (Exception e) {
                // This item failed, but continue with others
                log.warn("Failed to process item: {}", item.getId());
            }
        }
        // All successful items are committed together
    }
}

@Service
public class ItemService {
    
    @Transactional(propagation = Propagation.NESTED)
    public void processItem(Item item) {
        // Creates a SAVEPOINT
        // If this fails, only rolls back to the savepoint
        // Parent transaction continues
        itemRepository.save(item);
    }
}
```

**How NESTED differs from REQUIRES_NEW:**

| Aspect | NESTED | REQUIRES_NEW |
|--------|--------|--------------|
| New connection? | No (same connection) | Yes (new connection) |
| If parent rolls back? | Nested also rolls back | Independent (already committed) |
| If nested rolls back? | Parent can continue | Parent can continue |
| Database support | Requires savepoint support | All databases |

**Use cases:**
- Batch processing where individual failures shouldn't stop the batch
- Partial rollback scenarios

### MANDATORY - Must Have Existing Transaction

```java
@Service
public class InternalService {
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void internalOperation() {
        // MUST be called from within a transaction
        // Throws IllegalTransactionStateException if no TX exists
    }
}
```

**Use case:** Internal services that should NEVER be called directly, only as part of a larger transaction.

### SUPPORTS - Transaction Optional

```java
@Service
public class CacheService {
    
    @Transactional(propagation = Propagation.SUPPORTS)
    public User getUser(Long id) {
        // If called within TX → participates in TX
        // If called without TX → runs without TX
        return userRepository.findById(id).orElse(null);
    }
}
```

**Use case:** Read operations that can work with or without a transaction.

### NOT_SUPPORTED - Suspend Transaction

```java
@Service
public class ExternalApiService {
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void callExternalApi() {
        // Suspends any existing transaction
        // Runs outside of any transaction
        // Don't hold DB connection while waiting for external API!
        restTemplate.getForObject("https://api.example.com/data", String.class);
    }
}
```

**Use case:** Long-running operations that shouldn't hold transaction resources.

### NEVER - Transaction Not Allowed

```java
@Service  
public class NonTransactionalService {
    
    @Transactional(propagation = Propagation.NEVER)
    public void mustNotBeTransactional() {
        // Throws exception if called within a transaction
        // Useful for enforcing architectural constraints
    }
}
```

**Use case:** Explicitly forbidding transactional context (rare).

---

## 6. Isolation Levels - Concurrent Access Control

Isolation levels control what data a transaction can see when other transactions are running concurrently.

### The Problems They Solve

#### Dirty Read
```
Transaction 1                    Transaction 2
─────────────────────────────    ─────────────────────────────
BEGIN                            
UPDATE balance = 100                                           
                                 BEGIN
                                 SELECT balance → 100 (dirty!)
ROLLBACK                         
                                 // T2 saw data that never existed!
```

#### Non-Repeatable Read
```
Transaction 1                    Transaction 2
─────────────────────────────    ─────────────────────────────
BEGIN                            
SELECT balance → 100             
                                 BEGIN
                                 UPDATE balance = 200
                                 COMMIT
SELECT balance → 200             
// Same query, different result!
```

#### Phantom Read
```
Transaction 1                    Transaction 2
─────────────────────────────    ─────────────────────────────
BEGIN                            
SELECT COUNT(*) → 10             
                                 BEGIN
                                 INSERT INTO accounts...
                                 COMMIT
SELECT COUNT(*) → 11             
// New row appeared (phantom)!
```

### Isolation Levels Comparison

| Level | Dirty Read | Non-Repeatable Read | Phantom Read | Performance |
|-------|------------|---------------------|--------------|-------------|
| READ_UNCOMMITTED | ✅ Possible | ✅ Possible | ✅ Possible | Fastest |
| READ_COMMITTED | ❌ Prevented | ✅ Possible | ✅ Possible | Fast |
| REPEATABLE_READ | ❌ Prevented | ❌ Prevented | ✅ Possible | Medium |
| SERIALIZABLE | ❌ Prevented | ❌ Prevented | ❌ Prevented | Slowest |

### Usage Examples

```java
@Service
public class FinanceService {
    
    // Use database default (usually READ_COMMITTED)
    @Transactional(isolation = Isolation.DEFAULT)
    public void normalOperation() { }
    
    // For reports that need consistent point-in-time snapshot
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Report generateFinancialReport() {
        // All reads see data as of transaction start
        BigDecimal revenue = orderRepository.sumRevenue();
        BigDecimal expenses = expenseRepository.sumExpenses();
        return new Report(revenue, expenses);
    }
    
    // For critical operations that must not have any concurrency issues
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
        // Completely isolated from other transactions
        // Other transactions wait or fail with deadlock
    }
}
```

### MySQL Default: REPEATABLE_READ

MySQL uses `REPEATABLE_READ` by default, which means:
- ✅ No dirty reads
- ✅ No non-repeatable reads
- ⚠️ Phantoms possible (but MySQL's implementation largely prevents them)

```java
// In application.yml, you can verify:
spring:
  jpa:
    properties:
      hibernate:
        connection:
          isolation: 2  # TRANSACTION_READ_COMMITTED
```

### Practical Recommendations

| Scenario | Recommended Isolation |
|----------|----------------------|
| Most CRUD operations | DEFAULT (database default) |
| Financial reports | REPEATABLE_READ |
| Balance transfers | SERIALIZABLE or optimistic locking |
| High-concurrency reads | READ_COMMITTED |
| Data warehousing | READ_UNCOMMITTED (rare) |

---

## 7. Rollback Rules - When Things Go Wrong

### Default Behavior

Spring's default rollback rules:
- ✅ **RuntimeException** (unchecked) → Rollback
- ✅ **Error** → Rollback
- ❌ **Checked Exception** → NO Rollback (commits!)

```java
@Transactional
public void demonstrateRollback() {
    userRepository.save(new User("test@example.com"));
    
    // This triggers rollback (RuntimeException)
    throw new IllegalStateException("Something went wrong!");
    
    // User is NOT saved
}

@Transactional
public void demonstrateNoRollback() throws IOException {
    userRepository.save(new User("test@example.com"));
    
    // This does NOT trigger rollback (Checked exception)
    throw new IOException("File not found");
    
    // User IS saved! (Usually not what you want)
}
```

### Why This Default?

The reasoning (from Spring team):
- Checked exceptions often represent **business conditions** you can recover from
- Unchecked exceptions often represent **programming errors** or **unrecoverable conditions**

But in practice, most developers want rollback on ANY exception:

### Customizing Rollback Rules

```java
// Rollback on specific checked exceptions
@Transactional(rollbackFor = IOException.class)
public void uploadFile(File file) throws IOException {
    // Rolls back on IOException
}

// Rollback on ALL exceptions (recommended practice)
@Transactional(rollbackFor = Exception.class)
public void safeOperation() {
    // Rolls back on any exception
}

// Don't rollback on specific exceptions
@Transactional(noRollbackFor = EmailException.class)
public void createUserWithEmail(User user) {
    userRepository.save(user);
    try {
        emailService.sendWelcome(user);  // If this fails...
    } catch (EmailException e) {
        log.warn("Email failed but user created");
        // Transaction still commits - user is saved
    }
}
```

### Best Practice: Create a Custom Meta-Annotation

```java
/**
 * Standard transactional annotation for this project.
 * Rolls back on ALL exceptions, not just unchecked.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(rollbackFor = Exception.class)
public @interface AppTransactional {
    
    Propagation propagation() default Propagation.REQUIRED;
    
    Isolation isolation() default Isolation.DEFAULT;
    
    int timeout() default -1;
    
    boolean readOnly() default false;
}

// Usage
@Service
public class UserService {
    
    @AppTransactional  // Always rolls back on any exception
    public void createUser(User user) {
        // ...
    }
    
    @AppTransactional(readOnly = true)
    public User getUser(Long id) {
        // ...
    }
}
```

### Programmatic Rollback

Sometimes you need to trigger rollback without throwing an exception:

```java
@Transactional
public void processWithValidation(Order order) {
    orderRepository.save(order);
    
    ValidationResult result = validate(order);
    if (!result.isValid()) {
        // Mark for rollback without throwing exception
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        log.info("Order invalid, rolling back: {}", result.getErrors());
        return;  // Method returns normally, but TX rolls back
    }
    
    // Continue processing...
}
```

---

## 8. Read-Only Transactions - Performance Optimization

### What readOnly Does

```java
@Transactional(readOnly = true)
public List<User> getAllUsers() {
    return userRepository.findAll();
}
```

Setting `readOnly = true`:

1. **Hibernate optimization:** Disables dirty checking for loaded entities
2. **Database hints:** Some databases optimize read-only connections
3. **Connection pool:** May route to read replicas
4. **Documentation:** Signals intent to other developers

### Performance Impact

```java
@Transactional  // readOnly = false (default)
public List<User> getUsers() {
    List<User> users = userRepository.findAll();  // Load 1000 users
    // Hibernate tracks ALL 1000 entities for changes
    // At commit: checks each entity for modifications
    return users;
}

@Transactional(readOnly = true)
public List<User> getUsersOptimized() {
    List<User> users = userRepository.findAll();  // Load 1000 users
    // Hibernate does NOT track changes
    // At commit: no dirty checking needed
    return users;  // Faster!
}
```

### What Happens If You Try to Write?

```java
@Transactional(readOnly = true)
public void tryToModify() {
    User user = userRepository.findById(1L).orElseThrow();
    user.setName("New Name");  // This is silently ignored!
    // Changes are NOT persisted
}
```

**Warning:** No exception is thrown! The modification is just silently discarded.

### Best Practice: Apply at Service Level

```java
@Service
@Transactional(readOnly = true)  // Default: read-only
public class UserService {
    
    public List<User> getAllUsers() {
        // Uses read-only transaction
        return userRepository.findAll();
    }
    
    public User getUser(Long id) {
        // Uses read-only transaction
        return userRepository.findById(id).orElseThrow();
    }
    
    @Transactional  // Override: read-write
    public User createUser(CreateUserRequest request) {
        // Uses read-write transaction
        return userRepository.save(mapToEntity(request));
    }
    
    @Transactional  // Override: read-write
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

---

## 9. Transaction Boundaries - Where to Put @Transactional

### The Service Layer Rule

> **@Transactional belongs on the SERVICE layer, not Repository or Controller**

```
┌─────────────────────────────────────────────────────────────────┐
│ Controller Layer                                                │
│   - Handles HTTP requests/responses                             │
│   - NO @Transactional here!                                     │
│   - Why? Controllers should be thin, transactions are business  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ Service Layer                                                   │
│   - Business logic lives here                                   │
│   - @Transactional HERE! ✓                                      │
│   - One transaction per business operation                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ Repository Layer                                                │
│   - Data access only                                            │
│   - NO @Transactional here!                                     │
│   - Why? Spring Data already handles it per operation           │
└─────────────────────────────────────────────────────────────────┘
```

### Why Not on Controllers?

```java
// ❌ BAD - Transaction on controller
@RestController
public class UserController {
    
    @Transactional  // DON'T DO THIS
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}

// ✅ GOOD - Transaction on service
@RestController
public class UserController {
    
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);  // Transaction handled inside
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
```

**Problems with controller-level transactions:**
- Transaction stays open during serialization (JSON conversion)
- Longer transaction duration = more locking
- Mixes HTTP concerns with database concerns

### Why Not on Repositories?

```java
// ❌ BAD - Each repository call in separate transaction
@Service
public class OrderService {
    
    public void createOrder(Order order) {
        orderRepository.save(order);           // Transaction 1
        inventoryRepository.decrease(order);   // Transaction 2
        // If inventory update fails, order is already saved! 💥
    }
}

// ✅ GOOD - One transaction covers all operations
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        orderRepository.save(order);           // Same transaction
        inventoryRepository.decrease(order);   // Same transaction
        // All succeed or all fail together
    }
}
```

### Transaction Scope Guidelines

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    // ✅ GOOD: One transaction per business operation
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // All these operations in ONE transaction:
        User user = userRepository.findById(request.userId()).orElseThrow();
        Order order = new Order(user, request.items());
        orderRepository.save(order);
        inventoryService.reserveItems(order);
        paymentService.authorizePayment(order);
        return OrderResponse.from(order);
    }
    
    // ❌ BAD: Transaction too broad
    @Transactional
    public void processAllPendingOrders() {
        List<Order> orders = orderRepository.findByStatus(PENDING);
        for (Order order : orders) {
            processOrder(order);
            emailService.sendConfirmation(order);  // Don't hold TX for email!
        }
    }
    
    // ✅ BETTER: Smaller transactions
    public void processAllPendingOrders() {
        List<Long> orderIds = orderRepository.findIdsByStatus(PENDING);
        for (Long orderId : orderIds) {
            processOrder(orderId);  // Each order in separate TX
        }
    }
    
    @Transactional
    public void processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(PROCESSED);
        orderRepository.save(order);
    }
}
```

---

## 10. Common Pitfalls & How to Avoid Them

### Pitfall 1: Self-Invocation (Internal Calls)

```java
@Service
public class UserService {
    
    public void registerUser(User user) {
        saveUser(user);  // ❌ NO TRANSACTION! Internal call bypasses proxy
    }
    
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }
}
```

**Solution:** Move @Transactional to the public entry point or use separate services.

### Pitfall 2: Checked Exceptions Not Rolling Back

```java
@Transactional
public void processFile(MultipartFile file) throws IOException {
    User user = userRepository.save(new User());
    
    // This throws IOException (checked)
    Files.copy(file.getInputStream(), path);
    
    // Transaction COMMITS even though exception thrown!
}
```

**Solution:** Add `rollbackFor = Exception.class`:

```java
@Transactional(rollbackFor = Exception.class)
public void processFile(MultipartFile file) throws IOException {
    // Now rolls back on IOException
}
```

### Pitfall 3: @Transactional on Private/Protected Methods

```java
@Service
public class UserService {
    
    @Transactional  // ❌ DOES NOTHING! Private method not proxied
    private void internalSave(User user) {
        userRepository.save(user);
    }
}
```

**Solution:** Only use @Transactional on public methods.

### Pitfall 4: Transaction Within try-catch Hiding Rollback

```java
@Transactional
public void createUser(User user) {
    try {
        userRepository.save(user);
        externalService.notify(user);  // Throws exception
    } catch (Exception e) {
        log.error("Failed", e);
        // Exception caught, but transaction is MARKED FOR ROLLBACK
        // Cannot commit now!
    }
    // TransactionSystemException: Transaction silently rolled back
}
```

**Solution:** Be aware that catching exceptions doesn't prevent rollback if the exception propagated to the proxy first. Use `noRollbackFor` if you want to commit despite exceptions.

### Pitfall 5: Lazy Loading Outside Transaction

```java
@Transactional
public User getUser(Long id) {
    return userRepository.findById(id).orElseThrow();
}

// In controller:
public UserResponse getUser(Long id) {
    User user = userService.getUser(id);  // Transaction ends here
    
    // LazyInitializationException!
    user.getOrders().size();  // Orders are lazy-loaded, but TX is closed
}
```

**Solutions:**

```java
// Option 1: Eager fetch in repository
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :id")
Optional<User> findByIdWithOrders(Long id);

// Option 2: Initialize in service
@Transactional
public User getUserWithOrders(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    Hibernate.initialize(user.getOrders());  // Force load while TX open
    return user;
}

// Option 3: Use DTO projection (recommended)
@Transactional(readOnly = true)
public UserWithOrdersDto getUserWithOrders(Long id) {
    User user = userRepository.findByIdWithOrders(id).orElseThrow();
    return UserWithOrdersDto.from(user);  // Map to DTO inside transaction
}
```

### Pitfall 6: Long-Running Transactions

```java
@Transactional  // ❌ BAD: Transaction open during external HTTP call
public void processOrder(Order order) {
    orderRepository.save(order);
    
    // This takes 5 seconds...
    String response = restTemplate.postForObject(externalApi, order, String.class);
    
    // DB connection held for 5+ seconds!
}
```

**Solution:**

```java
public void processOrder(Order order) {
    // Transaction 1: Save order
    Order savedOrder = saveOrder(order);
    
    // No transaction: External call
    String response = restTemplate.postForObject(externalApi, savedOrder, String.class);
    
    // Transaction 2: Update with response
    updateOrderWithResponse(savedOrder.getId(), response);
}

@Transactional
public Order saveOrder(Order order) {
    return orderRepository.save(order);
}

@Transactional
public void updateOrderWithResponse(Long orderId, String response) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    order.setExternalResponse(response);
}
```

### Pitfall 7: Missing @EnableTransactionManagement

If transactions aren't working at all, verify configuration:

```java
@SpringBootApplication  // Includes @EnableTransactionManagement
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Spring Boot auto-configures this, but if you're using plain Spring Framework, you need:

```java
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    // ...
}
```

---

## 11. Testing Transactions

### Testing Rollback Behavior

```java
@SpringBootTest
@Transactional  // Each test runs in a transaction that rolls back
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void createUser_shouldPersistUser() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("test@example.com", "password");
        
        // Act
        User user = userService.createUser(request);
        
        // Assert
        assertThat(userRepository.findById(user.getId())).isPresent();
        
        // After test: automatically rolled back!
    }
    
    @Test
    @Rollback(false)  // Actually commit (for integration tests)
    void createUser_shouldPersistUser_withCommit() {
        // This test commits to database
    }
}
```

### Testing Exception Rollback

```java
@SpringBootTest
class UserServiceRollbackTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void createUser_whenValidationFails_shouldRollback() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("invalid-email", "password");
        long countBefore = userRepository.count();
        
        // Act & Assert
        assertThrows(ValidationException.class, () -> {
            userService.createUser(request);
        });
        
        // Verify rollback
        assertThat(userRepository.count()).isEqualTo(countBefore);
    }
}
```

### Testing Propagation

```java
@SpringBootTest
class PropagationTest {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private AuditRepository auditRepository;
    
    @Test
    void createOrder_whenFails_auditShouldStillPersist() {
        // AuditService uses REQUIRES_NEW
        long auditCountBefore = auditRepository.count();
        
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrderWithFailure();  // Fails after audit
        });
        
        // Audit should be committed (REQUIRES_NEW)
        assertThat(auditRepository.count()).isEqualTo(auditCountBefore + 1);
    }
}
```

### Verifying Transaction Boundaries

```java
@SpringBootTest
class TransactionBoundaryTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private EntityManager entityManager;
    
    @Test
    void getUser_shouldReturnDetachedEntity() {
        // Create user
        User user = userService.createUser(new CreateUserRequest("test@example.com", "pass"));
        
        // Get user (transaction ends after this)
        User retrieved = userService.getUser(user.getId());
        
        // Entity should be detached (not in persistence context)
        assertThat(entityManager.contains(retrieved)).isFalse();
    }
}
```

---

## 12. Real-World Patterns

### Pattern 1: Saga Pattern for Distributed Transactions

When operations span multiple services/databases:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaService {
    
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    
    public OrderResult createOrder(CreateOrderRequest request) {
        Order order = null;
        boolean inventoryReserved = false;
        boolean paymentAuthorized = false;
        
        try {
            // Step 1: Create order
            order = orderService.createOrder(request);
            
            // Step 2: Reserve inventory
            inventoryService.reserve(order);
            inventoryReserved = true;
            
            // Step 3: Authorize payment
            paymentService.authorize(order);
            paymentAuthorized = true;
            
            // Step 4: Confirm order
            orderService.confirm(order);
            
            return OrderResult.success(order);
            
        } catch (Exception e) {
            log.error("Order saga failed, compensating...", e);
            
            // Compensating transactions (reverse order)
            if (paymentAuthorized) {
                try {
                    paymentService.cancelAuthorization(order);
                } catch (Exception ex) {
                    log.error("Failed to cancel payment", ex);
                }
            }
            
            if (inventoryReserved) {
                try {
                    inventoryService.release(order);
                } catch (Exception ex) {
                    log.error("Failed to release inventory", ex);
                }
            }
            
            if (order != null) {
                try {
                    orderService.cancel(order);
                } catch (Exception ex) {
                    log.error("Failed to cancel order", ex);
                }
            }
            
            return OrderResult.failure(e.getMessage());
        }
    }
}
```

### Pattern 2: Optimistic Locking

For high-concurrency scenarios:

```java
@Entity
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private Integer quantity;
    
    @Version  // Optimistic locking version
    private Long version;
}

@Service
public class ProductService {
    
    @Transactional
    @Retryable(
        retryFor = OptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public void decreaseQuantity(Long productId, int amount) {
        Product product = productRepository.findById(productId).orElseThrow();
        
        if (product.getQuantity() < amount) {
            throw new InsufficientQuantityException();
        }
        
        product.setQuantity(product.getQuantity() - amount);
        // If another transaction modified this row, 
        // OptimisticLockingFailureException is thrown
    }
}
```

### Pattern 3: Transactional Outbox Pattern

For reliable event publishing:

```java
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String payload;
    private LocalDateTime createdAt;
    private boolean processed;
}

@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    
    @Transactional  // Both saved atomically
    public Order createOrder(CreateOrderRequest request) {
        Order order = orderRepository.save(new Order(request));
        
        // Store event in same transaction
        OutboxEvent event = new OutboxEvent(
            "Order",
            order.getId().toString(),
            "OrderCreated",
            toJson(order)
        );
        outboxRepository.save(event);
        
        return order;
    }
}

// Separate process reads outbox and publishes events
@Scheduled(fixedDelay = 1000)
@Transactional
public void publishEvents() {
    List<OutboxEvent> events = outboxRepository.findByProcessedFalse();
    for (OutboxEvent event : events) {
        try {
            messagePublisher.publish(event);
            event.setProcessed(true);
        } catch (Exception e) {
            log.error("Failed to publish event", e);
        }
    }
}
```

### Pattern 4: Read-Write Splitting

For scaling reads:

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("writeDataSource") DataSource writeDataSource,
            @Qualifier("readDataSource") DataSource readDataSource) {
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.WRITE, writeDataSource);
        targetDataSources.put(DataSourceType.READ, readDataSource);
        
        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(writeDataSource);
        
        return routingDataSource;
    }
}

public class RoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            ? DataSourceType.READ
            : DataSourceType.WRITE;
    }
}

// Usage - automatically routes based on readOnly flag
@Service
public class UserService {
    
    @Transactional(readOnly = true)  // Routes to read replica
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Transactional  // Routes to primary
    public User createUser(User user) {
        return userRepository.save(user);
    }
}
```

---

## 13. Debugging Transaction Issues

### Enable Transaction Logging

```yaml
# application-dev.yml
logging:
  level:
    # See transaction begin/commit/rollback
    org.springframework.transaction: DEBUG
    org.springframework.orm.jpa: DEBUG
    
    # See SQL statements
    org.hibernate.SQL: DEBUG
    
    # See transaction boundaries
    org.springframework.transaction.interceptor: TRACE
```

### Sample Log Output

```
DEBUG o.s.t.i.TransactionInterceptor - Getting transaction for [UserService.createUser]
DEBUG o.s.orm.jpa.JpaTransactionManager - Creating new transaction with name [UserService.createUser]
DEBUG o.s.orm.jpa.JpaTransactionManager - Opened new EntityManager for JPA transaction
DEBUG o.hibernate.SQL - insert into users (email, name) values (?, ?)
DEBUG o.s.t.i.TransactionInterceptor - Completing transaction for [UserService.createUser]
DEBUG o.s.orm.jpa.JpaTransactionManager - Initiating transaction commit
DEBUG o.s.orm.jpa.JpaTransactionManager - Committing JPA transaction on EntityManager
```

### Verify Transaction Is Active

```java
@Transactional
public void debugTransaction() {
    // Check if transaction is active
    boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
    log.debug("Transaction active: {}", isActive);
    
    // Check if read-only
    boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    log.debug("Transaction read-only: {}", isReadOnly);
    
    // Get transaction name
    String txName = TransactionSynchronizationManager.getCurrentTransactionName();
    log.debug("Transaction name: {}", txName);
}
```

### Common Issues Checklist

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Changes not persisted | Missing @Transactional | Add annotation |
| Changes not persisted | Self-invocation | Use separate service |
| Rollback not happening | Checked exception | Add rollbackFor |
| LazyInitializationException | Accessing outside TX | Fetch eagerly or extend TX |
| TransactionRequiredException | Missing @Transactional | Add annotation |
| Transaction silently rolled back | Exception caught after proxy | Check rollback markers |

---

## 14. Quick Reference

### @Transactional Attributes Cheat Sheet

| Attribute | Default | Purpose |
|-----------|---------|---------|
| propagation | REQUIRED | How to handle existing TX |
| isolation | DEFAULT | Concurrent access control |
| timeout | -1 (none) | Max seconds before timeout |
| readOnly | false | Optimization hint |
| rollbackFor | RuntimeException, Error | Exceptions that trigger rollback |
| noRollbackFor | (none) | Exceptions that don't trigger rollback |

### Propagation Quick Reference

| Level | Existing TX | No Existing TX |
|-------|-------------|----------------|
| REQUIRED | Join | Create new |
| REQUIRES_NEW | Suspend & create new | Create new |
| NESTED | Create savepoint | Create new |
| MANDATORY | Join | Exception! |
| SUPPORTS | Join | No TX |
| NOT_SUPPORTED | Suspend | No TX |
| NEVER | Exception! | No TX |

### Isolation Quick Reference

| Level | Dirty Read | Non-Repeatable | Phantom |
|-------|------------|----------------|---------|
| READ_UNCOMMITTED | Yes | Yes | Yes |
| READ_COMMITTED | No | Yes | Yes |
| REPEATABLE_READ | No | No | Yes |
| SERIALIZABLE | No | No | No |

### Decision Flowchart

```
Need to modify data?
├── Yes → @Transactional
│         └── Multiple services involved?
│             ├── Yes → Consider Saga pattern
│             └── No → Standard @Transactional
└── No → @Transactional(readOnly = true)

Calling external APIs?
├── Yes → Keep outside transaction (NOT_SUPPORTED or separate method)
└── No → Normal transaction

Need audit log that survives failure?
├── Yes → Use REQUIRES_NEW for audit
└── No → Standard REQUIRED

Processing items in batch?
├── Yes, all-or-nothing → Single transaction
└── Yes, partial success OK → Individual transactions or NESTED
```

---

## Summary

**Key Takeaways:**

1. **Transactions are about guarantees** - atomicity, consistency, isolation, durability
2. **Spring uses proxies** - understand why internal calls don't work
3. **@Transactional belongs on services** - not controllers, not repositories
4. **Checked exceptions don't rollback by default** - use `rollbackFor = Exception.class`
5. **Use readOnly for queries** - it's a performance optimization
6. **Keep transactions short** - don't hold connections during external calls
7. **Test your transaction boundaries** - verify rollback behavior

**The Golden Rule:**
> One `@Transactional` method = One business operation = One unit of work

---

**Next Steps:**
- Practice with the examples in this guide
- Enable transaction logging in your dev environment
- Review your existing services for transaction boundary issues
- Consider creating a custom `@AppTransactional` annotation

---

**You're now equipped to handle transactions like a pro!** 🚀
