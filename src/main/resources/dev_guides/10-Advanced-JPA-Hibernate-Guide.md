# 🗄️ Advanced JPA/Hibernate Deep Dive

> **Goal:** Master JPA and Hibernate to build high-performance, maintainable data access layers  
> **Stack:** Java 25, Spring Boot 4, Spring Data JPA, Hibernate 7, MySQL 8  
> **Philosophy:** Understand what Hibernate does behind the scenes — performance problems come from not knowing the "magic"

---

## 📋 Table of Contents

1. [The Persistence Context - Hibernate's Brain](#1-the-persistence-context---hibernates-brain)
2. [Entity Lifecycle States](#2-entity-lifecycle-states)
3. [Fetching Strategies - Lazy vs Eager](#3-fetching-strategies---lazy-vs-eager)
4. [The N+1 Problem - Performance Killer](#4-the-n1-problem---performance-killer)
5. [Entity Relationships Deep Dive](#5-entity-relationships-deep-dive)
6. [Cascade Types - When Operations Flow](#6-cascade-types---when-operations-flow)
7. [Orphan Removal - Cleaning Up Children](#7-orphan-removal---cleaning-up-children)
8. [Optimistic vs Pessimistic Locking](#8-optimistic-vs-pessimistic-locking)
9. [Second-Level Cache](#9-second-level-cache)
10. [Batch Processing - Handling Large Data](#10-batch-processing---handling-large-data)
11. [Query Optimization Techniques](#11-query-optimization-techniques)
12. [DTO Projections - The Performance Secret](#12-dto-projections---the-performance-secret)
13. [Auditing with Hibernate Envers](#13-auditing-with-hibernate-envers)
14. [Common Pitfalls & Solutions](#14-common-pitfalls--solutions)
15. [Performance Checklist](#15-performance-checklist)

---

## 1. The Persistence Context - Hibernate's Brain

### What Is the Persistence Context?

The **Persistence Context** (also called the **First-Level Cache**) is Hibernate's memory of entities. Think of it as a "shopping cart" for database operations.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Persistence Context                          │
│                    (EntityManager / Session)                        │
│                                                                     │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │
│   │ User@1      │  │ Order@42    │  │ Product@7   │               │
│   │ name="John" │  │ total=99.99 │  │ name="Book" │  ... more     │
│   │ MANAGED     │  │ MANAGED     │  │ MANAGED     │               │
│   └─────────────┘  └─────────────┘  └─────────────┘               │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐  │
│   │                    Dirty Checking                            │  │
│   │  "Which entities have changed since I loaded them?"          │  │
│   └─────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                │ flush() / commit
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           DATABASE                                  │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Behaviors

#### 1. Identity Guarantee (Same ID = Same Object)

```java
@Transactional
public void demonstrateIdentity() {
    User user1 = userRepository.findById(1L).orElseThrow();
    User user2 = userRepository.findById(1L).orElseThrow();
    
    // Same object! Only ONE query executed
    System.out.println(user1 == user2);  // true
    
    // Hibernate checks Persistence Context FIRST before hitting DB
}
```

#### 2. Automatic Dirty Checking

```java
@Transactional
public void demonstrateDirtyChecking() {
    User user = userRepository.findById(1L).orElseThrow();
    
    user.setName("New Name");  // Just modify the object
    
    // NO save() call needed!
    // Hibernate detects the change and generates UPDATE at flush time
}
// UPDATE users SET name = 'New Name' WHERE id = 1
```

#### 3. Write-Behind (Delayed SQL)

```java
@Transactional
public void demonstrateWriteBehind() {
    User user = new User("john@example.com");
    userRepository.save(user);  // Entity is MANAGED, but INSERT not yet executed
    
    user.setName("John Doe");   // Another change
    user.setAge(25);            // And another
    
    // All changes batched into ONE INSERT at flush time
}
// Only ONE INSERT executed at the end
```

### Persistence Context Scope

| Scope | Lifecycle | Use Case |
|-------|-----------|----------|
| Transaction-scoped | Lives for one `@Transactional` method | Default, most common |
| Extended | Lives across multiple transactions | Rare, for conversations |
| Detached | After transaction ends | Entities outside transaction |

```java
@Transactional
public User getUser(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    return user;  // User becomes DETACHED after method returns
}

public void modifyDetachedUser(User user) {
    user.setName("New Name");  // No automatic saving!
    // Must explicitly merge back if you want to persist changes
}
```

---

## 2. Entity Lifecycle States

Every entity exists in one of four states. Understanding this is **crucial** for avoiding bugs!

```
                    ┌─────────────────────────────────────────┐
                    │                                         │
     new User()     │            TRANSIENT                    │
         │          │   • Not associated with any PC          │
         │          │   • No database row exists              │
         │          │   • GC eligible if no references        │
         │          └─────────────────────────────────────────┘
         │                          │
         │                          │ persist() / save()
         ▼                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           MANAGED                                   │
│   • Associated with Persistence Context                             │
│   • Changes automatically tracked (dirty checking)                  │
│   • Will be synchronized with DB on flush/commit                    │
│   • Has database identity (ID assigned)                             │
└─────────────────────────────────────────────────────────────────────┘
         │                          │
         │ remove()                 │ detach() / clear() / 
         │                          │ transaction ends
         ▼                          ▼
┌───────────────────────┐  ┌───────────────────────────────────────────┐
│       REMOVED         │  │              DETACHED                      │
│ • Scheduled for       │  │ • Was managed, now disconnected            │
│   DELETE              │  │ • Has DB identity but not tracked          │
│ • Still in PC until   │  │ • Changes NOT automatically persisted      │
│   flush               │  │ • Can be reattached with merge()           │
└───────────────────────┘  └───────────────────────────────────────────┘
                                    │
                                    │ merge()
                                    ▼
                           Back to MANAGED
```

### State Transitions in Code

```java
@Service
@RequiredArgsConstructor
public class EntityLifecycleDemo {
    
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    
    @Transactional
    public void demonstrateLifecycle() {
        
        // ========== TRANSIENT ==========
        User user = new User();
        user.setEmail("john@example.com");
        // user is TRANSIENT - not managed, no ID
        
        boolean isManaged = entityManager.contains(user);
        System.out.println("After new: " + isManaged);  // false
        
        // ========== MANAGED ==========
        userRepository.save(user);  // or entityManager.persist(user)
        // user is now MANAGED - tracked, has ID
        
        isManaged = entityManager.contains(user);
        System.out.println("After save: " + isManaged);  // true
        System.out.println("ID assigned: " + user.getId());  // e.g., 1
        
        user.setName("John");  // Automatically tracked!
        
        // ========== DETACHED ==========
        entityManager.detach(user);
        // user is now DETACHED - has ID but not tracked
        
        isManaged = entityManager.contains(user);
        System.out.println("After detach: " + isManaged);  // false
        
        user.setName("Jane");  // NOT tracked! Won't be persisted
        
        // ========== BACK TO MANAGED ==========
        User managedUser = entityManager.merge(user);
        // managedUser is MANAGED (note: different reference!)
        // user is still DETACHED
        
        System.out.println(user == managedUser);  // false!
        System.out.println(entityManager.contains(managedUser));  // true
        
        // ========== REMOVED ==========
        entityManager.remove(managedUser);
        // Scheduled for deletion, still in PC until flush
    }
}
```

### Critical: merge() Returns a NEW Reference!

```java
// ❌ WRONG - common mistake
@Transactional
public void updateUser(User detachedUser) {
    entityManager.merge(detachedUser);
    detachedUser.setStatus("ACTIVE");  // This change is LOST!
}

// ✅ CORRECT - use the returned reference
@Transactional
public void updateUser(User detachedUser) {
    User managedUser = entityManager.merge(detachedUser);
    managedUser.setStatus("ACTIVE");  // This change is tracked
}
```

### Checking Entity State

```java
public class EntityStateChecker {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public String getEntityState(Object entity) {
        PersistenceUnitUtil util = entityManager.getEntityManagerFactory()
            .getPersistenceUnitUtil();
        
        if (!util.isLoaded(entity)) {
            return "PROXY (not loaded)";
        }
        
        Object id = util.getIdentifier(entity);
        
        if (id == null) {
            return "TRANSIENT";
        }
        
        if (entityManager.contains(entity)) {
            return "MANAGED";
        }
        
        return "DETACHED";
    }
}
```

---

## 3. Fetching Strategies - Lazy vs Eager

### The Basics

| Strategy | When Loaded | Default For | Annotation |
|----------|-------------|-------------|------------|
| LAZY | When accessed | @OneToMany, @ManyToMany | `fetch = FetchType.LAZY` |
| EAGER | Immediately with parent | @ManyToOne, @OneToOne | `fetch = FetchType.EAGER` |

```java
@Entity
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)  // Default
    private List<Order> orders;  // Loaded only when accessed
    
    @ManyToOne(fetch = FetchType.EAGER)  // Default for @ManyToOne
    private Department department;  // Always loaded with User
}
```

### How LAZY Loading Works (Proxy Pattern)

```java
@Transactional
public void demonstrateLazyLoading() {
    User user = userRepository.findById(1L).orElseThrow();
    // SQL: SELECT * FROM users WHERE id = 1
    
    // user.orders is NOT a real List!
    // It's a Hibernate PROXY (PersistentBag)
    System.out.println(user.getOrders().getClass());
    // org.hibernate.collection.internal.PersistentBag
    
    // When you ACCESS the collection...
    int orderCount = user.getOrders().size();  // NOW the query runs!
    // SQL: SELECT * FROM orders WHERE user_id = 1
}
```

### The LazyInitializationException

```java
// ❌ This will FAIL
public User getUser(Long id) {
    return userRepository.findById(id).orElseThrow();
}

public void processUser() {
    User user = getUser(1L);  // Transaction ended
    
    // LazyInitializationException!
    // "could not initialize proxy - no Session"
    user.getOrders().size();
}
```

### Solutions for Lazy Loading Issues

#### Solution 1: JOIN FETCH in Query

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :id")
    Optional<User> findByIdWithOrders(@Param("id") Long id);
}

// Usage
@Transactional(readOnly = true)
public User getUserWithOrders(Long id) {
    return userRepository.findByIdWithOrders(id).orElseThrow();
    // Orders are loaded IN THE SAME QUERY
}
```

#### Solution 2: @EntityGraph

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @EntityGraph(attributePaths = {"orders", "orders.items"})
    Optional<User> findWithOrdersById(Long id);
    
    // Named entity graph
    @EntityGraph(value = "User.withOrdersAndItems")
    List<User> findAllWithDetails();
}

// Define named graph on entity
@Entity
@NamedEntityGraph(
    name = "User.withOrdersAndItems",
    attributeNodes = {
        @NamedAttributeNode(value = "orders", subgraph = "orders-items")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "orders-items",
            attributeNodes = @NamedAttributeNode("items")
        )
    }
)
public class User { ... }
```

#### Solution 3: Hibernate.initialize()

```java
@Transactional(readOnly = true)
public User getUserWithOrders(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    Hibernate.initialize(user.getOrders());  // Force load
    return user;
}
```

#### Solution 4: DTO Projection (Best for Performance)

```java
public record UserWithOrderCountDto(
    Long id,
    String name,
    Long orderCount
) {}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("""
        SELECT new com.example.dto.UserWithOrderCountDto(
            u.id, u.name, COUNT(o)
        )
        FROM User u
        LEFT JOIN u.orders o
        WHERE u.id = :id
        GROUP BY u.id, u.name
        """)
    Optional<UserWithOrderCountDto> findUserWithOrderCount(@Param("id") Long id);
}
```

### Golden Rule: Default to LAZY

```java
@Entity
public class User {
    
    // ✅ GOOD - Always use LAZY for collections
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;
    
    // ⚠️ Consider LAZY even for @ManyToOne
    @ManyToOne(fetch = FetchType.LAZY)
    private Department department;
}
```

**Why?** You can always fetch eagerly when needed (JOIN FETCH), but you can't "un-fetch" eager data you don't need.

---

## 4. The N+1 Problem - Performance Killer

### What Is N+1?

The N+1 problem occurs when you execute **1 query** to load N entities, then **N additional queries** to load related data.

```java
// This innocent-looking code...
@Transactional(readOnly = true)
public void printAllUsersWithOrders() {
    List<User> users = userRepository.findAll();  // 1 query
    
    for (User user : users) {
        System.out.println(user.getName() + " has " + 
            user.getOrders().size() + " orders");  // N queries!
    }
}
```

```sql
-- Query 1: Load all users
SELECT * FROM users;

-- Query 2: Load orders for user 1
SELECT * FROM orders WHERE user_id = 1;

-- Query 3: Load orders for user 2
SELECT * FROM orders WHERE user_id = 2;

-- Query 4: Load orders for user 3
SELECT * FROM orders WHERE user_id = 3;

-- ... N more queries for N users!
```

**If you have 100 users, that's 101 queries!**

### Detecting N+1 Problems

#### Enable SQL Logging

```yaml
# application-dev.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE  # See parameter values
```

#### Use Hibernate Statistics

```java
@Configuration
public class HibernateConfig {
    
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            hibernateProperties.put("hibernate.generate_statistics", "true");
        };
    }
}

// In your code
@Autowired
private EntityManagerFactory emf;

public void checkStatistics() {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    
    log.info("Query count: {}", stats.getQueryExecutionCount());
    log.info("Entity fetch count: {}", stats.getEntityFetchCount());
    log.info("Collection fetch count: {}", stats.getCollectionFetchCount());
    
    stats.clear();  // Reset for next measurement
}
```

### Solution 1: JOIN FETCH

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ✅ Single query loads users AND their orders
    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.orders
        WHERE u.active = true
        """)
    List<User> findAllActiveWithOrders();
}
```

```sql
-- Just ONE query!
SELECT DISTINCT u.*, o.* 
FROM users u 
LEFT JOIN orders o ON u.id = o.user_id 
WHERE u.active = true;
```

### Solution 2: @BatchSize (For Large Collections)

```java
@Entity
public class User {
    
    @OneToMany(mappedBy = "user")
    @BatchSize(size = 25)  // Load orders in batches of 25 users
    private List<Order> orders;
}
```

```sql
-- Instead of N queries, uses IN clause:
SELECT * FROM orders WHERE user_id IN (1, 2, 3, ..., 25);
SELECT * FROM orders WHERE user_id IN (26, 27, 28, ..., 50);
-- etc.
```

**Global batch size configuration:**

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 25
```

### Solution 3: @Fetch(FetchMode.SUBSELECT)

```java
@Entity
public class User {
    
    @OneToMany(mappedBy = "user")
    @Fetch(FetchMode.SUBSELECT)  // Load ALL orders in one subselect
    private List<Order> orders;
}
```

```sql
-- One query for users
SELECT * FROM users WHERE active = true;

-- One query for ALL orders (subselect)
SELECT * FROM orders 
WHERE user_id IN (SELECT id FROM users WHERE active = true);
```

### Solution 4: DTO Projection

```java
// Best performance when you don't need full entities
@Query("""
    SELECT new com.example.dto.UserOrderSummary(
        u.id, u.name, COUNT(o), SUM(o.total)
    )
    FROM User u
    LEFT JOIN u.orders o
    GROUP BY u.id, u.name
    """)
List<UserOrderSummary> findUserOrderSummaries();
```

### N+1 with Multiple Collections (Cartesian Product Warning!)

```java
// ❌ DANGEROUS - Cartesian product!
@Query("""
    SELECT u FROM User u
    LEFT JOIN FETCH u.orders
    LEFT JOIN FETCH u.addresses
    """)
List<User> findAllWithOrdersAndAddresses();
// If user has 5 orders and 3 addresses = 15 rows per user!
```

**Solution: Multiple queries or @BatchSize**

```java
// ✅ BETTER - Separate fetches
@Transactional(readOnly = true)
public List<User> getUsersWithDetails() {
    List<User> users = userRepository.findAllWithOrders();
    // Second query for addresses (uses batch fetching)
    users.forEach(u -> Hibernate.initialize(u.getAddresses()));
    return users;
}
```

---

## 5. Entity Relationships Deep Dive

### Relationship Types Overview

| Type | Example | Owner | FK Location |
|------|---------|-------|-------------|
| @OneToOne | User ↔ Profile | Either side | Owner's table |
| @OneToMany / @ManyToOne | User → Orders | Many side | Many side's table |
| @ManyToMany | Students ↔ Courses | Either side | Join table |

### @OneToOne - Bidirectional

```java
@Entity
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String email;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;
    
    // Helper method for bidirectional consistency
    public void setProfile(UserProfile profile) {
        if (profile == null) {
            if (this.profile != null) {
                this.profile.setUser(null);
            }
        } else {
            profile.setUser(this);
        }
        this.profile = profile;
    }
}

@Entity
public class UserProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String bio;
    private String avatarUrl;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;  // OWNER - has the FK
}
```

**@OneToOne Lazy Loading Gotcha:**

```java
// ⚠️ @OneToOne on the NON-OWNING side cannot be truly lazy!
@OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
private UserProfile profile;
// Hibernate doesn't know if profile exists without a query

// ✅ Solution: Use @MapsId for shared primary key
@Entity
public class UserProfile {
    
    @Id  // No @GeneratedValue - uses User's ID
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId  // Shares primary key with User
    @JoinColumn(name = "id")
    private User user;
}
```

### @OneToMany / @ManyToOne - The Most Common

```java
@Entity
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
    
    // ✅ CRITICAL: Helper methods for bidirectional consistency
    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }
    
    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);
    }
}

@Entity
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)  // Always LAZY for @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // OWNER - has the FK
    
    private BigDecimal total;
}
```

**Why helper methods?**

```java
// ❌ WITHOUT helper - inconsistent state
order.setUser(user);  // Only one side updated
user.getOrders().add(order);  // Must remember to do both!

// ✅ WITH helper - always consistent
user.addOrder(order);  // Both sides updated automatically
```

### @ManyToMany

```java
@Entity
public class Student {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
    
    // Helper methods
    public void enrollIn(Course course) {
        courses.add(course);
        course.getStudents().add(this);
    }
    
    public void dropCourse(Course course) {
        courses.remove(course);
        course.getStudents().remove(this);
    }
}

@Entity
public class Course {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    
    @ManyToMany(mappedBy = "courses")
    private Set<Student> students = new HashSet<>();
}
```

**When to Use a Join Entity Instead:**

```java
// ✅ BETTER for @ManyToMany with extra attributes
@Entity
public class Enrollment {  // Join entity
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;
    
    private LocalDate enrollmentDate;
    private String grade;
    private Boolean completed;
}
```

### Use Set vs List

```java
@Entity
public class User {
    
    // ✅ Use Set for @ManyToMany (avoids duplicates, better for equals/hashCode)
    @ManyToMany
    private Set<Role> roles = new HashSet<>();
    
    // ✅ Use List when order matters or duplicates allowed
    @OneToMany(mappedBy = "user")
    @OrderBy("createdAt DESC")
    private List<Order> orders = new ArrayList<>();
}
```

### equals() and hashCode() for Entities

```java
@Entity
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NaturalId  // Business key
    @Column(nullable = false, unique = true)
    private String email;
    
    // ✅ Use business key (NaturalId) for equals/hashCode
    // NOT the database ID!
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return email != null && email.equals(user.email);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();  // Constant! Safe for Sets
    }
}
```

**Why not use ID?**
- ID is null for transient entities
- Breaks Set operations when entity transitions from transient to managed

---

## 6. Cascade Types - When Operations Flow

### Cascade Types Explained

| Type | Effect | Use Case |
|------|--------|----------|
| PERSIST | save() parent → save() children | New parent with new children |
| MERGE | merge() parent → merge() children | Update detached parent + children |
| REMOVE | delete() parent → delete() children | Delete parent deletes children |
| REFRESH | refresh() parent → refresh() children | Reload from DB |
| DETACH | detach() parent → detach() children | Disconnect from PC |
| ALL | All of the above | Composite/owned relationships |

### Visual Example

```
CascadeType.ALL on User.orders:

┌─────────────────────────────────────────────────────────────────┐
│  userRepository.save(user)                                      │
│                                                                 │
│  User (new)                                                     │
│    └── Order 1 (new) ──► ALSO saved automatically               │
│    └── Order 2 (new) ──► ALSO saved automatically               │
│                                                                 │
│  userRepository.delete(user)                                    │
│                                                                 │
│  User (managed)                                                 │
│    └── Order 1 ──► ALSO deleted automatically                   │
│    └── Order 2 ──► ALSO deleted automatically                   │
└─────────────────────────────────────────────────────────────────┘
```

### Code Examples

```java
@Entity
public class User {
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();
}

@Service
public class UserService {
    
    @Transactional
    public User createUserWithOrders() {
        User user = new User("john@example.com");
        
        Order order1 = new Order(BigDecimal.valueOf(99.99));
        Order order2 = new Order(BigDecimal.valueOf(149.99));
        
        user.addOrder(order1);
        user.addOrder(order2);
        
        // Only ONE save() call!
        // Orders are automatically persisted due to CascadeType.PERSIST
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        
        // Only ONE delete() call!
        // Orders are automatically deleted due to CascadeType.REMOVE
        userRepository.delete(user);
    }
}
```

### Cascade Best Practices

```java
// ✅ GOOD - Cascade for owned/composite relationships
@Entity
public class Order {
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;  // Items are OWNED by Order
}

// ❌ BAD - Don't cascade for referenced entities
@Entity
public class Order {
    
    @ManyToOne(cascade = CascadeType.ALL)  // DANGEROUS!
    private User user;  // User is NOT owned by Order
    // Deleting Order would delete User!
}

// ✅ GOOD - No cascade for references
@Entity
public class Order {
    
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;  // Just a reference, no cascade
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;  // Just a reference, no cascade
}
```

### Cascade Type Selection Guide

```
Is the child entity OWNED by the parent?
│
├── YES (child cannot exist without parent)
│   └── Use CascadeType.ALL + orphanRemoval = true
│       Example: Order → OrderItems
│
└── NO (child has independent lifecycle)
    └── Don't use cascade (or only PERSIST/MERGE)
        Example: Order → Product (reference)
```

---

## 7. Orphan Removal - Cleaning Up Children

### What Is Orphan Removal?

When you remove a child from a parent's collection, `orphanRemoval = true` automatically deletes it from the database.

```java
@Entity
public class Order {
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
        // With orphanRemoval=true, item is DELETED from DB!
    }
}
```

### Orphan Removal vs CASCADE.REMOVE

```java
// With only CascadeType.REMOVE:
order.getItems().remove(item);  // Item NOT deleted from DB
orderRepository.delete(order);   // Items deleted (cascade)

// With orphanRemoval = true:
order.getItems().remove(item);  // Item IS deleted from DB!
orderRepository.delete(order);   // Items also deleted
```

### Visual Comparison

```
┌─────────────────────────────────────────────────────────────────┐
│ CascadeType.REMOVE only:                                        │
│                                                                 │
│ order.getItems().remove(item1);                                │
│                                                                 │
│ Order                        Database                           │
│   └── item2                  ├── item1 (STILL EXISTS! 🚨)      │
│                              └── item2                          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ orphanRemoval = true:                                           │
│                                                                 │
│ order.getItems().remove(item1);                                │
│                                                                 │
│ Order                        Database                           │
│   └── item2                  └── item2 (item1 DELETED ✅)      │
└─────────────────────────────────────────────────────────────────┘
```

### When to Use Orphan Removal

```java
// ✅ USE orphanRemoval for truly owned children
@Entity
public class BlogPost {
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;  // Comments belong to this post only
}

// ❌ DON'T use orphanRemoval for shared references
@Entity
public class Student {
    
    @ManyToMany  // No orphanRemoval possible on @ManyToMany
    private Set<Course> courses;  // Courses are shared!
}
```

### Orphan Removal with Replacement

```java
@Transactional
public void replaceAllItems(Long orderId, List<OrderItem> newItems) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    
    // Clear existing items (all will be deleted!)
    order.getItems().clear();
    
    // Add new items
    newItems.forEach(order::addItem);
    
    // Old items deleted, new items inserted
}
```

---

## 8. Optimistic vs Pessimistic Locking

### The Problem: Lost Updates

```
Time    Transaction 1                Transaction 2
────    ─────────────────────        ─────────────────────
T1      SELECT stock FROM products   
        WHERE id = 1                 
        → stock = 10                 

T2                                   SELECT stock FROM products
                                     WHERE id = 1
                                     → stock = 10

T3      UPDATE products              
        SET stock = 9                
        WHERE id = 1                 

T4                                   UPDATE products
                                     SET stock = 9  ← WRONG!
                                     WHERE id = 1   Should be 8!
```

### Optimistic Locking (Recommended)

Assumes conflicts are rare. Uses a version column to detect concurrent modifications.

```java
@Entity
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private Integer stock;
    
    @Version  // Optimistic locking version
    private Long version;
}
```

**How it works:**

```sql
-- Read with version
SELECT id, name, stock, version FROM products WHERE id = 1;
-- Returns: id=1, stock=10, version=5

-- Update includes version check
UPDATE products 
SET stock = 9, version = 6 
WHERE id = 1 AND version = 5;
-- If version changed, 0 rows updated → OptimisticLockException
```

**Handling Conflicts:**

```java
@Service
public class ProductService {
    
    @Transactional
    @Retryable(
        retryFor = OptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        if (product.getStock() < quantity) {
            throw new InsufficientStockException();
        }
        
        product.setStock(product.getStock() - quantity);
        // If concurrent modification, OptimisticLockingFailureException thrown
        // @Retryable will retry up to 3 times
    }
}
```

### Pessimistic Locking

Assumes conflicts are likely. Locks the row in the database.

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Pessimistic READ lock (shared lock)
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithReadLock(@Param("id") Long id);
    
    // Pessimistic WRITE lock (exclusive lock)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithWriteLock(@Param("id") Long id);
}
```

**How it works:**

```sql
-- PESSIMISTIC_READ (shared lock)
SELECT * FROM products WHERE id = 1 FOR SHARE;
-- Other transactions can read but not write

-- PESSIMISTIC_WRITE (exclusive lock)
SELECT * FROM products WHERE id = 1 FOR UPDATE;
-- Other transactions must wait
```

**Usage:**

```java
@Transactional
public void decreaseStockPessimistic(Long productId, int quantity) {
    // Lock acquired here - other transactions wait
    Product product = productRepository.findByIdWithWriteLock(productId)
        .orElseThrow();
    
    if (product.getStock() < quantity) {
        throw new InsufficientStockException();
    }
    
    product.setStock(product.getStock() - quantity);
    // Lock released when transaction commits
}
```

### Comparison

| Aspect | Optimistic | Pessimistic |
|--------|------------|-------------|
| Conflict assumption | Rare | Frequent |
| Performance (low contention) | Better ✅ | Worse |
| Performance (high contention) | Worse (many retries) | Better ✅ |
| Scalability | Better ✅ | Limited by locks |
| Deadlock risk | None | Possible |
| Use case | Web apps, read-heavy | Financial, inventory |

### When to Use Which

```
┌─────────────────────────────────────────────────────────────┐
│ Decision Guide:                                             │
│                                                             │
│ Is high contention expected on this entity?                 │
│   │                                                         │
│   ├── NO (rare conflicts)                                   │
│   │   └── Use OPTIMISTIC locking (@Version)                │
│   │       • Most web applications                           │
│   │       • User profiles, settings                         │
│   │       • Content management                              │
│   │                                                         │
│   └── YES (frequent conflicts)                              │
│       └── Use PESSIMISTIC locking                          │
│           • Inventory management                            │
│           • Financial transactions                          │
│           • Ticket booking                                  │
│           • Sequential number generation                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. Second-Level Cache

### Cache Levels in Hibernate

```
┌─────────────────────────────────────────────────────────────────┐
│                     Application                                 │
│                         │                                       │
│                         ▼                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              First-Level Cache (L1)                      │   │
│  │              (Persistence Context)                       │   │
│  │  • Per EntityManager/Session                             │   │
│  │  • Transaction scoped                                    │   │
│  │  • Always ON, cannot disable                             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                         │ miss                                  │
│                         ▼                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Second-Level Cache (L2)                     │   │
│  │              (SessionFactory level)                      │   │
│  │  • Shared across all sessions                            │   │
│  │  • Application scoped                                    │   │
│  │  • Must be explicitly enabled                            │   │
│  └─────────────────────────────────────────────────────────┘   │
│                         │ miss                                  │
│                         ▼                                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    DATABASE                              │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Setting Up Second-Level Cache

#### Step 1: Add Dependencies

```xml
<!-- Ehcache 3 (JSR-107 compliant) -->
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-jcache</artifactId>
</dependency>
<dependency>
    <groupId>org.ehcache</groupId>
    <artifactId>ehcache</artifactId>
    <classifier>jakarta</classifier>
</dependency>
```

#### Step 2: Configure Hibernate

```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          use_query_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
        javax:
          cache:
            provider: org.ehcache.jsr107.EhcacheCachingProvider
            uri: classpath:ehcache.xml
```

#### Step 3: Configure Ehcache

```xml
<!-- src/main/resources/ehcache.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://www.ehcache.org/v3">
    
    <!-- Default cache template -->
    <cache-template name="default">
        <expiry>
            <ttl unit="minutes">60</ttl>
        </expiry>
        <heap unit="entries">1000</heap>
    </cache-template>
    
    <!-- Entity cache -->
    <cache alias="com.example.entity.Product" uses-template="default">
        <heap unit="entries">500</heap>
    </cache>
    
    <!-- Collection cache -->
    <cache alias="com.example.entity.Category.products" uses-template="default">
        <heap unit="entries">200</heap>
    </cache>
    
    <!-- Query cache -->
    <cache alias="default-query-results-region" uses-template="default">
        <heap unit="entries">100</heap>
    </cache>
    
</config>
```

#### Step 4: Annotate Entities

```java
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToMany(mappedBy = "product")
    private List<Review> reviews;
}

// Read-only entities (reference data)
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Immutable  // Tells Hibernate entity never changes
public class Country {
    
    @Id
    private String code;
    private String name;
}
```

### Cache Concurrency Strategies

| Strategy | Use Case | Performance |
|----------|----------|-------------|
| READ_ONLY | Reference data that never changes | Best |
| NONSTRICT_READ_WRITE | Data that rarely changes | Good |
| READ_WRITE | Data that changes, needs consistency | Medium |
| TRANSACTIONAL | Full transaction isolation (JTA) | Slowest |

### Query Cache

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    List<Product> findByCategory(String category);
    
    @Query("SELECT p FROM Product p WHERE p.active = true")
    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    List<Product> findAllActive();
}
```

### When to Use Second-Level Cache

```
✅ GOOD candidates for caching:
• Reference data (countries, currencies, categories)
• Configuration data
• Frequently read, rarely modified data
• Data that can tolerate some staleness

❌ BAD candidates for caching:
• Frequently updated data
• Data that must be real-time accurate
• Large objects (blobs, clobs)
• User-specific data with high cardinality
```

### Cache Eviction

```java
@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final EntityManager entityManager;
    
    public void evictEntity(Class<?> entityClass, Object id) {
        entityManager.getEntityManagerFactory()
            .getCache()
            .evict(entityClass, id);
    }
    
    public void evictAllOfType(Class<?> entityClass) {
        entityManager.getEntityManagerFactory()
            .getCache()
            .evict(entityClass);
    }
    
    public void evictAll() {
        entityManager.getEntityManagerFactory()
            .getCache()
            .evictAll();
    }
}
```

---

## 10. Batch Processing - Handling Large Data

### The Problem: Memory Overflow

```java
// ❌ BAD - Loads ALL entities into memory
@Transactional
public void updateAllProducts(BigDecimal priceIncrease) {
    List<Product> products = productRepository.findAll();  // 1 million products!
    // OutOfMemoryError!
    
    for (Product product : products) {
        product.setPrice(product.getPrice().add(priceIncrease));
    }
}
```

### Solution 1: Pagination

```java
@Transactional
public void updateAllProductsPaginated(BigDecimal priceIncrease) {
    int pageSize = 100;
    int page = 0;
    Page<Product> productPage;
    
    do {
        productPage = productRepository.findAll(PageRequest.of(page, pageSize));
        
        for (Product product : productPage.getContent()) {
            product.setPrice(product.getPrice().add(priceIncrease));
        }
        
        entityManager.flush();  // Write changes
        entityManager.clear();  // Clear persistence context!
        
        page++;
    } while (productPage.hasNext());
}
```

### Solution 2: Streaming with @Query

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p")
    @QueryHints(@QueryHint(name = HINT_FETCH_SIZE, value = "100"))
    Stream<Product> streamAll();
}

@Service
public class ProductService {
    
    @Transactional
    public void updateAllProductsStreaming(BigDecimal priceIncrease) {
        try (Stream<Product> stream = productRepository.streamAll()) {
            stream.forEach(product -> {
                product.setPrice(product.getPrice().add(priceIncrease));
            });
        }
    }
}
```

### Solution 3: Bulk Updates (Best Performance)

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Modifying
    @Query("UPDATE Product p SET p.price = p.price * :multiplier WHERE p.category = :category")
    int bulkUpdatePrices(@Param("multiplier") BigDecimal multiplier, 
                         @Param("category") String category);
}

@Service
public class ProductService {
    
    @Transactional
    public int applyDiscount(String category, BigDecimal discountPercent) {
        BigDecimal multiplier = BigDecimal.ONE.subtract(
            discountPercent.divide(BigDecimal.valueOf(100))
        );
        
        // Single SQL statement updates all matching rows!
        return productRepository.bulkUpdatePrices(multiplier, category);
    }
}
```

**⚠️ Bulk Update Warning:**

```java
@Transactional
public void bulkUpdateDemo() {
    Product product = productRepository.findById(1L).orElseThrow();
    System.out.println(product.getPrice());  // $100
    
    // Bulk update bypasses persistence context!
    productRepository.bulkUpdatePrices(BigDecimal.valueOf(1.1), "electronics");
    
    // Stale data! Still shows old price
    System.out.println(product.getPrice());  // Still $100! (not $110)
    
    // Must refresh or clear
    entityManager.refresh(product);  // or entityManager.clear();
    System.out.println(product.getPrice());  // Now $110
}
```

### Solution 4: JDBC Batch Inserts

```java
@Service
@RequiredArgsConstructor
public class ProductBatchService {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Transactional
    public void batchInsert(List<Product> products) {
        String sql = "INSERT INTO products (name, price, category) VALUES (?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, products, 1000, (ps, product) -> {
            ps.setString(1, product.getName());
            ps.setBigDecimal(2, product.getPrice());
            ps.setString(3, product.getCategory());
        });
    }
}
```

### Configure Hibernate Batching

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
          batch_versioned_data: true
        order_inserts: true
        order_updates: true
```

```java
@Transactional
public void saveMultipleProducts(List<Product> products) {
    for (int i = 0; i < products.size(); i++) {
        entityManager.persist(products.get(i));
        
        if (i % 50 == 0) {  // Match batch_size
            entityManager.flush();
            entityManager.clear();
        }
    }
}
```

---

## 11. Query Optimization Techniques

### Use Specific Column Selection

```java
// ❌ BAD - Fetches all columns
@Query("SELECT p FROM Product p WHERE p.category = :category")
List<Product> findByCategory(String category);

// ✅ BETTER - Fetch only what you need
@Query("SELECT p.id, p.name, p.price FROM Product p WHERE p.category = :category")
List<Object[]> findBasicInfoByCategory(String category);

// ✅ BEST - DTO projection
@Query("""
    SELECT new com.example.dto.ProductSummary(p.id, p.name, p.price)
    FROM Product p
    WHERE p.category = :category
    """)
List<ProductSummary> findSummariesByCategory(String category);
```

### Avoid Cartesian Products

```java
// ❌ BAD - Cartesian product with multiple bags
@Query("""
    SELECT u FROM User u
    LEFT JOIN FETCH u.orders
    LEFT JOIN FETCH u.addresses
    """)
List<User> findAllWithOrdersAndAddresses();

// ✅ BETTER - Multiple queries
@EntityGraph(attributePaths = "orders")
List<User> findAllWithOrders();

// Then fetch addresses separately
```

### Use EXISTS Instead of COUNT

```java
// ❌ SLOWER - Counts all matching rows
@Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.user.id = :userId")
boolean hasOrders(@Param("userId") Long userId);

// ✅ FASTER - Stops at first match
@Query("SELECT CASE WHEN EXISTS (SELECT 1 FROM Order o WHERE o.user.id = :userId) THEN true ELSE false END")
boolean hasOrdersExists(@Param("userId") Long userId);

// Or use Spring Data's existsBy
boolean existsByUserId(Long userId);
```

### Pagination Optimization

```java
// ❌ BAD - Fetches collection with pagination (Hibernate warning!)
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders")
Page<User> findAllWithOrders(Pageable pageable);
// "HHH90003004: firstResult/maxResults specified with collection fetch"

// ✅ BETTER - Two queries approach
@Query("SELECT u.id FROM User u")
Page<Long> findAllIds(Pageable pageable);

@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id IN :ids")
List<User> findByIdsWithOrders(@Param("ids") List<Long> ids);

// Usage
public Page<User> getUsersWithOrders(Pageable pageable) {
    Page<Long> idPage = userRepository.findAllIds(pageable);
    List<User> users = userRepository.findByIdsWithOrders(idPage.getContent());
    return new PageImpl<>(users, pageable, idPage.getTotalElements());
}
```

### Index Hints (Database Specific)

```java
// For MySQL
@Query(value = "SELECT * FROM products USE INDEX (idx_category_price) WHERE category = :category", 
       nativeQuery = true)
List<Product> findByCategoryWithIndex(@Param("category") String category);
```

---

## 12. DTO Projections - The Performance Secret

### Why DTOs?

```
Entity Loading:
┌─────────────────────────────────────────┐
│ SELECT * FROM users WHERE id = 1        │
│                                         │
│ → Loads ALL columns                     │
│ → Creates proxy for lazy collections    │
│ → Registered in Persistence Context     │
│ → Dirty checking overhead               │
└─────────────────────────────────────────┘

DTO Projection:
┌─────────────────────────────────────────┐
│ SELECT id, name FROM users WHERE id = 1 │
│                                         │
│ → Loads only needed columns             │
│ → No proxy creation                     │
│ → NOT in Persistence Context            │
│ → No dirty checking                     │
│ → ~30-50% faster for read operations!   │
└─────────────────────────────────────────┘
```

### Interface-Based Projection (Closed)

```java
// Define projection interface
public interface UserSummary {
    Long getId();
    String getName();
    String getEmail();
}

// Repository method
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    List<UserSummary> findByActiveTrue();
    
    Optional<UserSummary> findSummaryById(Long id);
}

// Usage
List<UserSummary> users = userRepository.findByActiveTrue();
users.forEach(u -> System.out.println(u.getName()));  // Only id, name, email loaded
```

### Interface-Based Projection with SpEL

```java
public interface UserFullName {
    
    String getFirstName();
    String getLastName();
    
    @Value("#{target.firstName + ' ' + target.lastName}")
    String getFullName();
}
```

### Class-Based Projection (Record DTO)

```java
// DTO Record
public record UserDto(
    Long id,
    String name,
    String email,
    Long orderCount
) {}

// Repository with constructor expression
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("""
        SELECT new com.example.dto.UserDto(
            u.id, u.name, u.email, COUNT(o)
        )
        FROM User u
        LEFT JOIN u.orders o
        GROUP BY u.id, u.name, u.email
        """)
    List<UserDto> findAllWithOrderCount();
}
```

### Dynamic Projections

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Generic projection - caller decides the return type!
    <T> List<T> findByActiveTrue(Class<T> type);
    
    <T> Optional<T> findById(Long id, Class<T> type);
}

// Usage
List<UserSummary> summaries = userRepository.findByActiveTrue(UserSummary.class);
List<UserDto> dtos = userRepository.findByActiveTrue(UserDto.class);
List<User> entities = userRepository.findByActiveTrue(User.class);
```

### Nested Projections

```java
public interface OrderWithUser {
    Long getId();
    BigDecimal getTotal();
    UserSummary getUser();  // Nested projection!
    
    interface UserSummary {
        Long getId();
        String getName();
    }
}

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<OrderWithUser> findByStatus(OrderStatus status);
}
```

### When to Use What

| Scenario | Recommended Approach |
|----------|----------------------|
| Simple read-only lists | Interface projection |
| Complex aggregations | Record DTO with @Query |
| API responses | Record DTO |
| Need to modify data | Entity |
| Reports with calculations | Record DTO |
| Multiple return types | Dynamic projection |

---

## 13. Auditing with Hibernate Envers

### What Is Envers?

Hibernate Envers automatically tracks entity changes and stores revision history.

```
┌─────────────────────────────────────────────────────────────────┐
│                         products                                │
│  id  │  name    │  price  │ (current state)                    │
│  1   │  Widget  │  29.99  │                                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                       products_AUD                              │
│  id  │  rev  │ revtype │  name    │  price  │ (history)        │
│  1   │  1    │  0 (ADD)│  Widget  │  19.99  │                   │
│  1   │  2    │  1 (MOD)│  Widget  │  24.99  │                   │
│  1   │  3    │  1 (MOD)│  Widget  │  29.99  │ ← current        │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         REVINFO                                 │
│  rev  │  revtstmp          │ (revision metadata)                │
│  1    │  2025-01-15 10:00  │                                    │
│  2    │  2025-01-16 14:30  │                                    │
│  3    │  2025-01-17 09:15  │                                    │
└─────────────────────────────────────────────────────────────────┘
```

### Setup

```xml
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-envers</artifactId>
</dependency>
```

```yaml
spring:
  jpa:
    properties:
      org.hibernate.envers:
        audit_table_suffix: _AUD
        revision_field_name: REV
        revision_type_field_name: REVTYPE
        store_data_at_delete: true
```

### Enable Auditing on Entity

```java
@Entity
@Audited  // Enable Envers auditing
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private BigDecimal price;
    
    @NotAudited  // Exclude from auditing
    private String internalNotes;
}
```

### Custom Revision Entity

```java
@Entity
@Table(name = "revision_info")
@RevisionEntity(CustomRevisionListener.class)
public class CustomRevisionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    private Long id;
    
    @RevisionTimestamp
    private Long timestamp;
    
    private String username;  // Who made the change
    private String ipAddress; // From where
}

public class CustomRevisionListener implements RevisionListener {
    
    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;
        
        // Get current user from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            rev.setUsername(auth.getName());
        }
        
        // Get IP from request context
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();
            rev.setIpAddress(request.getRemoteAddr());
        }
    }
}
```

### Querying Audit History

```java
@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final EntityManager entityManager;
    
    // Get all revisions of an entity
    public List<Product> getProductHistory(Long productId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        
        List<Number> revisions = reader.getRevisions(Product.class, productId);
        
        return revisions.stream()
            .map(rev -> reader.find(Product.class, productId, rev))
            .toList();
    }
    
    // Get entity at specific revision
    public Product getProductAtRevision(Long productId, Number revision) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.find(Product.class, productId, revision);
    }
    
    // Get entity at specific date
    public Product getProductAtDate(Long productId, LocalDateTime date) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        Number revision = reader.getRevisionNumberForDate(
            Date.from(date.atZone(ZoneId.systemDefault()).toInstant())
        );
        return reader.find(Product.class, productId, revision);
    }
    
    // Query with criteria
    public List<Product> findProductsModifiedBy(String username) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        
        return reader.createQuery()
            .forRevisionsOfEntity(Product.class, true, true)
            .add(AuditEntity.revisionProperty("username").eq(username))
            .getResultList();
    }
}
```

---

## 14. Common Pitfalls & Solutions

### Pitfall 1: toString() with Lazy Collections

```java
// ❌ BAD - triggers lazy loading
@Entity
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;
    
    @Override
    public String toString() {
        return "User{id=" + id + ", orders=" + orders + "}";  // LazyInitException outside TX!
    }
}

// ✅ GOOD - exclude lazy collections
@Entity
@ToString(exclude = "orders")  // Lombok
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;
}

// Or manual
@Override
public String toString() {
    return "User{id=" + id + ", name=" + name + "}";  // No lazy collections
}
```

### Pitfall 2: Modifying Detached Entities

```java
// ❌ BAD - Changes lost!
public void updateUser(Long id, String newName) {
    User user = userRepository.findById(id).orElseThrow();
    // Transaction ends here, user is DETACHED
    
    user.setName(newName);  // Not tracked!
    // Changes are lost!
}

// ✅ GOOD - Keep in transaction
@Transactional
public void updateUser(Long id, String newName) {
    User user = userRepository.findById(id).orElseThrow();
    user.setName(newName);  // Tracked, auto-saved at commit
}
```

### Pitfall 3: Open Session in View Anti-Pattern

```yaml
# ❌ BAD - Keeps session open during view rendering
spring:
  jpa:
    open-in-view: true  # Default!

# ✅ GOOD - Explicit fetching in service layer
spring:
  jpa:
    open-in-view: false
```

### Pitfall 4: Unnecessary save() Calls

```java
// ❌ UNNECESSARY - Entity is already managed
@Transactional
public void updateUser(Long id, String newName) {
    User user = userRepository.findById(id).orElseThrow();
    user.setName(newName);
    userRepository.save(user);  // Redundant! Dirty checking handles it
}

// ✅ GOOD - Let dirty checking do its job
@Transactional
public void updateUser(Long id, String newName) {
    User user = userRepository.findById(id).orElseThrow();
    user.setName(newName);
    // Auto-saved at transaction commit
}
```

### Pitfall 5: saveAll() Without Batching

```java
// ❌ SLOW - No batching configured
@Transactional
public void createProducts(List<Product> products) {
    productRepository.saveAll(products);  // N individual INSERTs!
}

// ✅ FAST - With batching config
// application.yml:
// hibernate.jdbc.batch_size: 50
// hibernate.order_inserts: true
@Transactional
public void createProducts(List<Product> products) {
    productRepository.saveAll(products);  // Batched INSERTs
}
```

### Pitfall 6: Missing Index on FK Columns

```java
@Entity
@Table(indexes = {
    @Index(name = "idx_order_user_id", columnList = "user_id")  // Add this!
})
public class Order {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
```

### Pitfall 7: Returning Entity from Controller

```java
// ❌ BAD - Exposes entity, lazy loading issues
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userService.getUser(id);  // Serialization may trigger lazy load
}

// ✅ GOOD - Return DTO
@GetMapping("/users/{id}")
public UserResponse getUser(@PathVariable Long id) {
    return userService.getUserResponse(id);  // DTO with only needed fields
}
```

---

## 15. Performance Checklist

### Before Going to Production

```
□ Lazy Loading
  □ All @OneToMany and @ManyToMany are LAZY
  □ Consider LAZY for @ManyToOne and @OneToOne
  □ No LazyInitializationException in logs

□ N+1 Prevention
  □ SQL logging enabled in dev
  □ JOIN FETCH for known associations
  □ @BatchSize configured (global or per-entity)
  □ No more than 2-3 queries per request

□ Indexing
  □ All FK columns have indexes
  □ Frequently queried columns indexed
  □ Composite indexes for common query patterns

□ Caching
  □ Second-level cache for reference data
  □ Query cache for stable queries
  □ Cache eviction strategy defined

□ Connection Pool
  □ HikariCP configured properly
  □ Pool size matches expected load
  □ Connection timeout set

□ Query Optimization
  □ No SELECT * (use projections)
  □ Pagination for large result sets
  □ EXISTS instead of COUNT where applicable
  □ Bulk updates for batch operations

□ Entity Design
  □ @Version for optimistic locking
  □ Proper equals/hashCode (use business key)
  □ No unnecessary bidirectional relationships

□ Configuration
  □ open-in-view: false
  □ show-sql: false in production
  □ Batch size configured
  □ Statistics enabled for monitoring
```

### Query Performance Quick Wins

| Problem | Solution | Impact |
|---------|----------|--------|
| N+1 queries | JOIN FETCH / @BatchSize | 10-100x faster |
| Lazy loading outside TX | DTO projections | Prevents errors |
| Full entity load | Select only needed columns | 30-50% faster |
| COUNT for existence | Use EXISTS | 2-10x faster |
| Large result sets | Pagination | Prevents OOM |
| Frequent reads | Second-level cache | 5-20x faster |

---

## Summary

**Key Takeaways:**

1. **Understand the Persistence Context** — it's the key to understanding Hibernate behavior
2. **Default to LAZY loading** — fetch eagerly only when needed
3. **Watch for N+1** — enable SQL logging, use JOIN FETCH or @BatchSize
4. **Use DTOs for reads** — better performance, cleaner API
5. **Configure batching** — essential for bulk operations
6. **Add indexes** — especially on FK columns
7. **Use @Version** — optimistic locking prevents lost updates
8. **Consider caching** — but only for appropriate data

**The Golden Rules:**
- Never return entities from controllers
- Always use transactions for write operations
- Profile your queries in development
- Test with production-like data volumes

---

**You now have the knowledge to build high-performance data access layers!** 🚀
