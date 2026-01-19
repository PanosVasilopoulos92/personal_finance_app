# 🌱 Spring Core Concepts Deep Dive

> **Goal:** Master the foundation that powers everything in Spring — IoC, DI, Bean lifecycle, and AOP  
> **Stack:** Java 25, Spring Framework 7, Spring Boot 4  
> **Philosophy:** Understanding Spring's "magic" transforms you from a framework user to a framework master

---

## 📋 Table of Contents

1. [Inversion of Control (IoC) - The Foundation](#1-inversion-of-control-ioc---the-foundation)
2. [Dependency Injection (DI) - The Mechanism](#2-dependency-injection-di---the-mechanism)
3. [The ApplicationContext - Spring's Brain](#3-the-applicationcontext---springs-brain)
4. [Bean Definition & Registration](#4-bean-definition--registration)
5. [Bean Scopes - Lifecycle Boundaries](#5-bean-scopes---lifecycle-boundaries)
6. [Bean Lifecycle - From Birth to Death](#6-bean-lifecycle---from-birth-to-death)
7. [Conditional Beans - Smart Registration](#7-conditional-beans---smart-registration)
8. [Profiles - Environment-Specific Configuration](#8-profiles---environment-specific-configuration)
9. [Property Management & @ConfigurationProperties](#9-property-management--configurationproperties)
10. [Aspect-Oriented Programming (AOP)](#10-aspect-oriented-programming-aop)
11. [Custom Annotations with AOP](#11-custom-annotations-with-aop)
12. [Event-Driven Architecture](#12-event-driven-architecture)
13. [SpEL - Spring Expression Language](#13-spel---spring-expression-language)
14. [Resource Handling](#14-resource-handling)
15. [Common Pitfalls & Best Practices](#15-common-pitfalls--best-practices)

---

## 1. Inversion of Control (IoC) - The Foundation

### What Is IoC?

**Traditional Programming (You control everything):**
```java
// YOU create dependencies
// YOU manage lifecycle
// YOU wire everything together

public class OrderService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PaymentGateway paymentGateway;
    
    public OrderService() {
        // YOU create all dependencies
        this.userRepository = new UserRepositoryImpl(
            new DataSource("jdbc:mysql://localhost:3306/db", "user", "pass")
        );
        this.emailService = new SmtpEmailService(
            new SmtpConfig("smtp.gmail.com", 587, "user", "pass")
        );
        this.paymentGateway = new StripePaymentGateway(
            new StripeConfig("sk_test_xxx")
        );
    }
}
```

**Problems:**
- Hard to test (can't mock dependencies)
- Tight coupling (changing implementation requires code changes)
- Complex initialization (must know how to create everything)
- No centralized configuration

**Inversion of Control (Framework controls):**
```java
// SPRING creates dependencies
// SPRING manages lifecycle
// SPRING wires everything together

@Service
public class OrderService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PaymentGateway paymentGateway;
    
    // YOU just declare what you need
    public OrderService(UserRepository userRepository,
                       EmailService emailService,
                       PaymentGateway paymentGateway) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.paymentGateway = paymentGateway;
    }
}
```

### The Control Inversion

```
┌─────────────────────────────────────────────────────────────────────┐
│                    TRADITIONAL APPROACH                             │
│                                                                     │
│    Your Code                                                        │
│       │                                                             │
│       ├── Creates DatabaseConnection                                │
│       ├── Creates UserRepository(connection)                        │
│       ├── Creates EmailService(config)                              │
│       ├── Creates OrderService(repo, email)                         │
│       │                                                             │
│    YOU control the flow                                             │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    IoC APPROACH                                     │
│                                                                     │
│    Spring Container (ApplicationContext)                            │
│       │                                                             │
│       ├── Scans for @Component, @Service, @Repository               │
│       ├── Creates beans in correct order                            │
│       ├── Injects dependencies automatically                        │
│       ├── Manages lifecycle (init, destroy)                         │
│       │                                                             │
│    Your Code just declares:                                         │
│       • @Service class OrderService                                 │
│       • Constructor with dependencies                               │
│                                                                     │
│    SPRING controls the flow                                         │
└─────────────────────────────────────────────────────────────────────┘
```

### Benefits of IoC

| Benefit | Explanation |
|---------|-------------|
| **Loose Coupling** | Classes don't know how dependencies are created |
| **Testability** | Easy to inject mocks for testing |
| **Configurability** | Change implementations without code changes |
| **Lifecycle Management** | Container handles creation, initialization, destruction |
| **Centralized Configuration** | All wiring in one place |

---

## 2. Dependency Injection (DI) - The Mechanism

DI is **how** IoC is implemented. Spring "injects" dependencies into your classes.

### Three Types of Injection

#### 1. Constructor Injection (✅ RECOMMENDED)

```java
@Service
public class OrderService {
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    // Spring automatically injects dependencies
    // @Autowired is optional for single constructor (Spring 4.3+)
    public OrderService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
}
```

**Why Constructor Injection is Best:**
- ✅ Dependencies are `final` (immutable)
- ✅ Required dependencies are explicit
- ✅ Fails fast if dependency missing
- ✅ Easy to test (just pass mocks to constructor)
- ✅ No reflection needed
- ✅ Works without Spring (POJO friendly)

#### 2. Setter Injection (⚠️ OPTIONAL DEPENDENCIES)

```java
@Service
public class NotificationService {
    
    private EmailService emailService;
    private SmsService smsService;  // Optional
    
    @Autowired
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    @Autowired(required = false)  // Optional dependency
    public void setSmsService(SmsService smsService) {
        this.smsService = smsService;
    }
    
    public void notify(User user, String message) {
        emailService.send(user.getEmail(), message);
        
        if (smsService != null) {  // Check if optional service available
            smsService.send(user.getPhone(), message);
        }
    }
}
```

**When to Use Setter Injection:**
- Optional dependencies
- Circular dependencies (as a workaround, but better to refactor)
- When you need to reinject dependencies

#### 3. Field Injection (❌ AVOID)

```java
@Service
public class OrderService {
    
    @Autowired  // ❌ AVOID
    private UserRepository userRepository;
    
    @Autowired  // ❌ AVOID
    private EmailService emailService;
}
```

**Why Field Injection is Bad:**
- ❌ Can't make fields `final`
- ❌ Hides dependencies (not visible in API)
- ❌ Hard to test (needs reflection or Spring context)
- ❌ Can't create object without Spring
- ❌ Encourages too many dependencies (easy to add more)

### Injection Comparison

```java
// ✅ Constructor Injection - Clear, testable, immutable
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class OrderService {
    private final UserRepository userRepository;
    private final EmailService emailService;
}

// Testing is easy:
OrderService service = new OrderService(mockUserRepo, mockEmailService);


// ❌ Field Injection - Hidden, mutable, hard to test
@Service
public class OrderService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;
}

// Testing requires reflection or Spring context 😞
```

### Handling Multiple Implementations

```java
// Multiple implementations of same interface
public interface PaymentGateway {
    void processPayment(Order order);
}

@Service
public class StripePaymentGateway implements PaymentGateway { }

@Service  
public class PayPalPaymentGateway implements PaymentGateway { }

// Problem: Which one to inject?
@Service
public class OrderService {
    public OrderService(PaymentGateway gateway) {  // Which one? Error!
    }
}
```

**Solution 1: @Primary**

```java
@Service
@Primary  // This one is used by default
public class StripePaymentGateway implements PaymentGateway { }

@Service
public class PayPalPaymentGateway implements PaymentGateway { }
```

**Solution 2: @Qualifier**

```java
@Service
@Qualifier("stripe")
public class StripePaymentGateway implements PaymentGateway { }

@Service
@Qualifier("paypal")
public class PayPalPaymentGateway implements PaymentGateway { }

@Service
public class OrderService {
    public OrderService(@Qualifier("stripe") PaymentGateway gateway) {
        // Gets StripePaymentGateway
    }
}
```

**Solution 3: Inject All Implementations**

```java
@Service
public class PaymentService {
    
    private final Map<String, PaymentGateway> gateways;
    
    public PaymentService(List<PaymentGateway> gatewayList) {
        this.gateways = gatewayList.stream()
            .collect(Collectors.toMap(
                g -> g.getClass().getSimpleName(),
                Function.identity()
            ));
    }
    
    public void processPayment(String provider, Order order) {
        PaymentGateway gateway = gateways.get(provider + "PaymentGateway");
        gateway.processPayment(order);
    }
}
```

---

## 3. The ApplicationContext - Spring's Brain

### What Is ApplicationContext?

The `ApplicationContext` is the central interface for Spring's IoC container. It:
- Creates and manages beans
- Handles dependency injection
- Publishes events
- Provides internationalization
- Loads resources

```
┌─────────────────────────────────────────────────────────────────────┐
│                      ApplicationContext                             │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                    Bean Factory                              │   │
│   │  • Creates bean instances                                    │   │
│   │  • Manages bean definitions                                  │   │
│   │  • Handles dependency injection                              │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                    Bean Registry                             │   │
│   │  userService ──► UserService instance                        │   │
│   │  orderService ──► OrderService instance                      │   │
│   │  userRepository ──► UserRepository instance                  │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│   ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐   │
│   │ Event Publisher  │  │ Resource Loader  │  │ Environment    │   │
│   └──────────────────┘  └──────────────────┘  └────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### ApplicationContext Implementations

| Implementation | Use Case |
|----------------|----------|
| `AnnotationConfigApplicationContext` | Standalone Java config |
| `ClassPathXmlApplicationContext` | XML config (legacy) |
| `GenericWebApplicationContext` | Web applications |
| `ServletWebServerApplicationContext` | Spring Boot web apps |

### Programmatic Access

```java
@Service
@RequiredArgsConstructor
public class DynamicBeanService {
    
    private final ApplicationContext context;
    
    // Get bean by type
    public UserService getUserService() {
        return context.getBean(UserService.class);
    }
    
    // Get bean by name
    public Object getBeanByName(String name) {
        return context.getBean(name);
    }
    
    // Get bean by name and type
    public UserService getUserServiceByName() {
        return context.getBean("userService", UserService.class);
    }
    
    // Check if bean exists
    public boolean hasBean(String name) {
        return context.containsBean(name);
    }
    
    // Get all beans of a type
    public Map<String, PaymentGateway> getAllPaymentGateways() {
        return context.getBeansOfType(PaymentGateway.class);
    }
    
    // Get environment properties
    public String getProperty(String key) {
        return context.getEnvironment().getProperty(key);
    }
    
    // Publish events
    public void publishEvent(Object event) {
        context.publishEvent(event);
    }
}
```

### BeanFactory vs ApplicationContext

```java
// BeanFactory - Basic container (lazy initialization)
BeanFactory factory = new DefaultListableBeanFactory();
// • Basic DI
// • Lazy bean creation
// • No AOP, events, i18n

// ApplicationContext - Full-featured container (eager initialization)
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
// • Everything BeanFactory has, plus:
// • Eager bean creation (by default)
// • AOP support
// • Event publication
// • Resource loading
// • Internationalization
// • Environment abstraction

// In Spring Boot, you almost always use ApplicationContext
```

---

## 4. Bean Definition & Registration

### What Is a Bean?

A **bean** is an object that is:
- Created by Spring
- Managed by Spring (lifecycle, scope)
- Available for injection

### Ways to Define Beans

#### 1. Component Scanning (@Component and Stereotypes)

```java
// Generic component
@Component
public class EmailValidator { }

// Service layer
@Service
public class UserService { }

// Repository/DAO layer
@Repository
public class UserRepository { }

// Web layer
@Controller  // or @RestController
public class UserController { }

// Configuration class
@Configuration
public class AppConfig { }
```

**Stereotype Hierarchy:**

```
                    @Component
                        │
        ┌───────────────┼───────────────┐
        │               │               │
    @Service      @Repository     @Controller
                                        │
                                  @RestController
```

All stereotypes ARE @Component with additional semantics:
- `@Repository`: Exception translation for data access
- `@Service`: Business logic (no special behavior, just semantic)
- `@Controller`: Web endpoints, view resolution
- `@RestController`: @Controller + @ResponseBody

#### 2. @Bean Methods in @Configuration

```java
@Configuration
public class AppConfig {
    
    // Simple bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // Bean with dependencies (injected as parameters)
    @Bean
    public UserService userService(UserRepository repository, 
                                   PasswordEncoder encoder) {
        return new UserService(repository, encoder);
    }
    
    // Bean with custom name
    @Bean("customEmailService")
    public EmailService emailService() {
        return new SmtpEmailService();
    }
    
    // Bean with init and destroy methods
    @Bean(initMethod = "init", destroyMethod = "cleanup")
    public ConnectionPool connectionPool() {
        return new ConnectionPool();
    }
}
```

**When to Use @Bean vs @Component:**

| Use @Component | Use @Bean |
|----------------|-----------|
| Your own classes | Third-party library classes |
| Simple instantiation | Complex instantiation logic |
| Class-level annotation | Method-level definition |
| Auto-scanning | Explicit configuration |

```java
// Third-party class - can't add @Component
// Must use @Bean
@Configuration
public class RestClientConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        template.setErrorHandler(new CustomErrorHandler());
        return template;
    }
}
```

#### 3. Programmatic Registration

```java
@Configuration
public class DynamicBeanConfig {
    
    @Bean
    public BeanDefinitionRegistryPostProcessor dynamicBeanRegistrar() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                // Dynamically register beans
                GenericBeanDefinition beanDef = new GenericBeanDefinition();
                beanDef.setBeanClass(DynamicService.class);
                beanDef.setScope(BeanDefinition.SCOPE_SINGLETON);
                registry.registerBeanDefinition("dynamicService", beanDef);
            }
            
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
                // Post-process bean factory
            }
        };
    }
}
```

### Bean Naming

```java
// Default name: class name with lowercase first letter
@Service
public class UserService { }  // Bean name: "userService"

// Custom name
@Service("customUserService")
public class UserService { }  // Bean name: "customUserService"

// @Bean method name becomes bean name
@Bean
public PasswordEncoder passwordEncoder() { }  // Bean name: "passwordEncoder"

// Custom name with @Bean
@Bean("bcryptEncoder")
public PasswordEncoder passwordEncoder() { }  // Bean name: "bcryptEncoder"
```

---

## 5. Bean Scopes - Lifecycle Boundaries

### Available Scopes

| Scope | Description | Use Case |
|-------|-------------|----------|
| singleton | One instance per container (default) | Stateless services |
| prototype | New instance every injection | Stateful objects |
| request | One instance per HTTP request | Request-specific data |
| session | One instance per HTTP session | User session data |
| application | One instance per ServletContext | App-wide shared state |
| websocket | One instance per WebSocket session | WebSocket state |

### Singleton Scope (Default)

```java
@Service  // Singleton by default
public class UserService {
    // One instance shared by ALL consumers
    // Must be STATELESS (thread-safe)
}
```

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SINGLETON SCOPE                                  │
│                                                                     │
│    ApplicationContext                                               │
│         │                                                           │
│         └── userService (single instance)                           │
│                    │                                                │
│         ┌─────────┴─────────┬─────────────────┐                    │
│         │                   │                 │                     │
│    OrderService      PaymentService    AdminController              │
│    (uses same)       (uses same)       (uses same)                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**⚠️ Singleton Pitfall: Mutable State**

```java
// ❌ DANGEROUS - Shared mutable state
@Service
public class OrderService {
    private Order currentOrder;  // SHARED BETWEEN ALL THREADS!
    
    public void processOrder(Order order) {
        this.currentOrder = order;  // Race condition!
        // Thread 1 sets Order A
        // Thread 2 sets Order B
        // Thread 1 processes Order B (wrong!)
    }
}

// ✅ CORRECT - Stateless
@Service
public class OrderService {
    private final OrderRepository repository;  // Immutable reference OK
    
    public void processOrder(Order order) {
        // Order passed as parameter, not stored
        repository.save(order);
    }
}
```

### Prototype Scope

```java
@Component
@Scope("prototype")  // New instance every time
public class ShoppingCart {
    private List<Item> items = new ArrayList<>();
    
    public void addItem(Item item) {
        items.add(item);
    }
}
```

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PROTOTYPE SCOPE                                  │
│                                                                     │
│    ApplicationContext.getBean(ShoppingCart.class)                   │
│         │                                                           │
│         ├── Call 1 ──► ShoppingCart instance #1                     │
│         ├── Call 2 ──► ShoppingCart instance #2                     │
│         └── Call 3 ──► ShoppingCart instance #3                     │
│                                                                     │
│    Each call creates a NEW instance                                 │
└─────────────────────────────────────────────────────────────────────┘
```

**⚠️ Prototype in Singleton Problem:**

```java
@Service  // Singleton
public class OrderService {
    
    private final ShoppingCart cart;  // Prototype
    
    // ❌ PROBLEM: Same cart instance used forever!
    public OrderService(ShoppingCart cart) {
        this.cart = cart;  // Injected ONCE at singleton creation
    }
}
```

**Solution: ObjectProvider or @Lookup**

```java
// Solution 1: ObjectProvider
@Service
public class OrderService {
    
    private final ObjectProvider<ShoppingCart> cartProvider;
    
    public OrderService(ObjectProvider<ShoppingCart> cartProvider) {
        this.cartProvider = cartProvider;
    }
    
    public void createOrder() {
        ShoppingCart cart = cartProvider.getObject();  // New instance each time
    }
}

// Solution 2: @Lookup (requires non-final class)
@Service
public abstract class OrderService {
    
    @Lookup
    public abstract ShoppingCart getShoppingCart();  // Spring overrides this
    
    public void createOrder() {
        ShoppingCart cart = getShoppingCart();  // New instance each time
    }
}
```

### Request and Session Scopes

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {
    private String correlationId;
    private Instant requestTime = Instant.now();
    
    // One instance per HTTP request
}

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserSession {
    private User currentUser;
    private List<String> recentlyViewed = new ArrayList<>();
    
    // One instance per HTTP session
}
```

**Why proxyMode?**

Request/session scoped beans can't be directly injected into singletons. Spring creates a proxy that fetches the actual bean from the current request/session.

```java
@Service  // Singleton
public class OrderService {
    
    private final UserSession userSession;  // Session-scoped proxy
    
    public OrderService(UserSession userSession) {
        // This injects a PROXY, not the actual session bean
        this.userSession = userSession;
    }
    
    public void placeOrder(Order order) {
        // Proxy delegates to the actual session bean for current HTTP session
        User user = userSession.getCurrentUser();
    }
}
```

### Custom Scope

```java
// Define custom scope
public class TenantScope implements Scope {
    
    private final Map<String, Map<String, Object>> tenantBeans = new ConcurrentHashMap<>();
    
    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        String tenantId = TenantContext.getCurrentTenant();
        Map<String, Object> beans = tenantBeans.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>());
        return beans.computeIfAbsent(name, k -> objectFactory.getObject());
    }
    
    @Override
    public Object remove(String name) {
        String tenantId = TenantContext.getCurrentTenant();
        Map<String, Object> beans = tenantBeans.get(tenantId);
        return beans != null ? beans.remove(name) : null;
    }
    
    // ... other methods
}

// Register the scope
@Configuration
public class ScopeConfig {
    
    @Bean
    public static CustomScopeConfigurer customScopeConfigurer() {
        CustomScopeConfigurer configurer = new CustomScopeConfigurer();
        configurer.addScope("tenant", new TenantScope());
        return configurer;
    }
}

// Use the custom scope
@Component
@Scope("tenant")
public class TenantConfiguration {
    // One instance per tenant
}
```

---

## 6. Bean Lifecycle - From Birth to Death

### Complete Lifecycle Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                    BEAN LIFECYCLE                                   │
│                                                                     │
│  1. INSTANTIATION                                                   │
│     └── Constructor called                                          │
│                                                                     │
│  2. POPULATE PROPERTIES                                             │
│     └── Dependencies injected                                       │
│                                                                     │
│  3. BEAN NAME AWARE                                                 │
│     └── setBeanName() if BeanNameAware                              │
│                                                                     │
│  4. BEAN FACTORY AWARE                                              │
│     └── setBeanFactory() if BeanFactoryAware                        │
│                                                                     │
│  5. APPLICATION CONTEXT AWARE                                       │
│     └── setApplicationContext() if ApplicationContextAware          │
│                                                                     │
│  6. PRE-INITIALIZATION (BeanPostProcessor)                          │
│     └── postProcessBeforeInitialization()                           │
│                                                                     │
│  7. INITIALIZATION                                                  │
│     ├── @PostConstruct method                                       │
│     ├── InitializingBean.afterPropertiesSet()                       │
│     └── Custom init-method                                          │
│                                                                     │
│  8. POST-INITIALIZATION (BeanPostProcessor)                         │
│     └── postProcessAfterInitialization()                            │
│     └── (AOP proxies created here!)                                 │
│                                                                     │
│  ═══════════════════════════════════════════════════════════════   │
│                    BEAN IS READY FOR USE                            │
│  ═══════════════════════════════════════════════════════════════   │
│                                                                     │
│  9. DESTRUCTION (on container shutdown)                             │
│     ├── @PreDestroy method                                          │
│     ├── DisposableBean.destroy()                                    │
│     └── Custom destroy-method                                       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Lifecycle Callbacks in Code

```java
@Service
@Slf4j
public class DatabaseConnectionPool implements 
        InitializingBean, 
        DisposableBean, 
        BeanNameAware,
        ApplicationContextAware {
    
    private String beanName;
    private ApplicationContext context;
    private List<Connection> connections;
    
    // 1. Constructor
    public DatabaseConnectionPool() {
        log.info("1. Constructor called");
    }
    
    // 2. Setter injection happens here (if any)
    
    // 3. BeanNameAware
    @Override
    public void setBeanName(String name) {
        log.info("3. BeanNameAware.setBeanName(): {}", name);
        this.beanName = name;
    }
    
    // 5. ApplicationContextAware
    @Override
    public void setApplicationContext(ApplicationContext context) {
        log.info("5. ApplicationContextAware.setApplicationContext()");
        this.context = context;
    }
    
    // 7a. @PostConstruct (runs first in initialization phase)
    @PostConstruct
    public void postConstruct() {
        log.info("7a. @PostConstruct");
    }
    
    // 7b. InitializingBean
    @Override
    public void afterPropertiesSet() {
        log.info("7b. InitializingBean.afterPropertiesSet()");
        initializePool();
    }
    
    // 9a. @PreDestroy
    @PreDestroy
    public void preDestroy() {
        log.info("9a. @PreDestroy");
    }
    
    // 9b. DisposableBean
    @Override
    public void destroy() {
        log.info("9b. DisposableBean.destroy()");
        closeAllConnections();
    }
    
    private void initializePool() {
        connections = new ArrayList<>();
        // Initialize connection pool
    }
    
    private void closeAllConnections() {
        connections.forEach(this::closeConnection);
    }
}
```

### Recommended: Use @PostConstruct and @PreDestroy

```java
@Service
@Slf4j
public class CacheService {
    
    private Cache<String, Object> cache;
    
    @PostConstruct
    public void init() {
        log.info("Initializing cache...");
        cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up cache...");
        cache.invalidateAll();
    }
    
    public void put(String key, Object value) {
        cache.put(key, value);
    }
    
    public Object get(String key) {
        return cache.getIfPresent(key);
    }
}
```

### BeanPostProcessor - Customizing All Beans

```java
@Component
@Slf4j
public class LoggingBeanPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // Called BEFORE @PostConstruct
        log.debug("Before init: {}", beanName);
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // Called AFTER @PostConstruct
        // This is where AOP proxies are typically created!
        log.debug("After init: {}", beanName);
        return bean;  // Can return a proxy here
    }
}
```

### Startup and Shutdown Hooks

```java
@Component
public class AppLifecycleListener implements SmartLifecycle {
    
    private boolean running = false;
    
    @Override
    public void start() {
        log.info("Application starting...");
        running = true;
        // Initialize resources
    }
    
    @Override
    public void stop() {
        log.info("Application stopping...");
        running = false;
        // Cleanup resources
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public int getPhase() {
        return 0;  // Lower = starts first, stops last
    }
}

// Or use ApplicationRunner for startup logic
@Component
public class AppStartupRunner implements ApplicationRunner {
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("Application started with args: {}", args.getOptionNames());
        // Run after all beans are initialized
    }
}
```

---

## 7. Conditional Beans - Smart Registration

### @Conditional Annotations

Spring Boot provides many `@Conditional` annotations to control when beans are created.

```java
@Configuration
public class ConditionalConfig {
    
    // Only if property exists and equals "true"
    @Bean
    @ConditionalOnProperty(name = "feature.newui.enabled", havingValue = "true")
    public NewUIService newUIService() {
        return new NewUIService();
    }
    
    // Only if class is on classpath
    @Bean
    @ConditionalOnClass(name = "com.stripe.Stripe")
    public StripePaymentGateway stripeGateway() {
        return new StripePaymentGateway();
    }
    
    // Only if class is NOT on classpath
    @Bean
    @ConditionalOnMissingClass("com.stripe.Stripe")
    public MockPaymentGateway mockGateway() {
        return new MockPaymentGateway();
    }
    
    // Only if no other bean of this type exists
    @Bean
    @ConditionalOnMissingBean(PaymentGateway.class)
    public DefaultPaymentGateway defaultGateway() {
        return new DefaultPaymentGateway();
    }
    
    // Only if another bean exists
    @Bean
    @ConditionalOnBean(DataSource.class)
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    // Only in web application
    @Bean
    @ConditionalOnWebApplication
    public WebMetricsService webMetrics() {
        return new WebMetricsService();
    }
    
    // Only in non-web application
    @Bean
    @ConditionalOnNotWebApplication
    public CliMetricsService cliMetrics() {
        return new CliMetricsService();
    }
    
    // Based on Spring Expression Language
    @Bean
    @ConditionalOnExpression("${feature.enabled:false} and ${env.production:false}")
    public ProductionFeature productionFeature() {
        return new ProductionFeature();
    }
}
```

### Common @Conditional Annotations

| Annotation | Condition |
|------------|-----------|
| `@ConditionalOnProperty` | Property exists/matches value |
| `@ConditionalOnClass` | Class is on classpath |
| `@ConditionalOnMissingClass` | Class is NOT on classpath |
| `@ConditionalOnBean` | Bean of type exists |
| `@ConditionalOnMissingBean` | Bean of type does NOT exist |
| `@ConditionalOnWebApplication` | Running as web app |
| `@ConditionalOnNotWebApplication` | NOT running as web app |
| `@ConditionalOnExpression` | SpEL expression is true |
| `@ConditionalOnResource` | Resource exists |
| `@ConditionalOnJava` | Specific Java version |

### Custom Condition

```java
// Custom condition class
public class OnFeatureFlagCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        // Get annotation attributes
        Map<String, Object> attrs = metadata.getAnnotationAttributes(
            ConditionalOnFeatureFlag.class.getName()
        );
        String featureName = (String) attrs.get("value");
        
        // Check if feature is enabled
        String property = context.getEnvironment()
            .getProperty("features." + featureName + ".enabled", "false");
        
        return Boolean.parseBoolean(property);
    }
}

// Custom annotation
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnFeatureFlagCondition.class)
public @interface ConditionalOnFeatureFlag {
    String value();
}

// Usage
@Service
@ConditionalOnFeatureFlag("dark-mode")
public class DarkModeService {
    // Only created if features.dark-mode.enabled=true
}
```

---

## 8. Profiles - Environment-Specific Configuration

### Defining Profiles

```java
// Only active in "dev" profile
@Configuration
@Profile("dev")
public class DevConfig {
    
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
}

// Only active in "prod" profile
@Configuration
@Profile("prod")
public class ProdConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://prod-server:3306/db");
        ds.setUsername("${db.username}");
        ds.setPassword("${db.password}");
        return ds;
    }
}

// Active in multiple profiles
@Service
@Profile({"dev", "test"})
public class MockEmailService implements EmailService {
    // Used in dev and test
}

// Active when profile is NOT active
@Service
@Profile("!prod")  // Active when "prod" is NOT active
public class DebugService {
    // Active in dev, test, etc. but not prod
}
```

### Activating Profiles

```yaml
# application.yml
spring:
  profiles:
    active: dev  # Activate dev profile
    
---
# application-dev.yml (automatically loaded when dev profile is active)
spring:
  datasource:
    url: jdbc:h2:mem:testdb

---
# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://prod-server:3306/db
```

```bash
# Command line
java -jar app.jar --spring.profiles.active=prod

# Environment variable
export SPRING_PROFILES_ACTIVE=prod

# JVM argument
java -Dspring.profiles.active=prod -jar app.jar
```

### Profile Groups (Spring Boot 2.4+)

```yaml
# application.yml
spring:
  profiles:
    group:
      production:
        - prod
        - metrics
        - ssl
      development:
        - dev
        - debug
        - h2
```

```bash
# Activates prod, metrics, and ssl profiles
java -jar app.jar --spring.profiles.active=production
```

### Programmatic Profile Check

```java
@Service
@RequiredArgsConstructor
public class FeatureService {
    
    private final Environment environment;
    
    public boolean isProductionMode() {
        return environment.acceptsProfiles(Profiles.of("prod"));
    }
    
    public boolean isDevelopmentMode() {
        return environment.acceptsProfiles(Profiles.of("dev", "local"));
    }
}
```

---

## 9. Property Management & @ConfigurationProperties

### Basic Property Injection

```yaml
# application.yml
app:
  name: MyApplication
  version: 1.0.0
  max-connections: 100
  feature:
    enabled: true
```

```java
@Service
public class AppService {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version:unknown}")  // With default
    private String version;
    
    @Value("${app.max-connections}")
    private int maxConnections;
    
    @Value("${app.feature.enabled}")
    private boolean featureEnabled;
    
    @Value("${JAVA_HOME}")  // Environment variable
    private String javaHome;
    
    @Value("#{systemProperties['user.home']}")  // SpEL
    private String userHome;
}
```

### @ConfigurationProperties (Recommended)

```yaml
# application.yml
payment:
  gateway:
    url: https://api.stripe.com
    api-key: sk_test_xxx
    timeout: 30s
    retry:
      max-attempts: 3
      backoff: 1000ms
    supported-currencies:
      - USD
      - EUR
      - GBP
```

```java
@ConfigurationProperties(prefix = "payment.gateway")
@Validated  // Enable validation
public class PaymentGatewayProperties {
    
    @NotBlank
    private String url;
    
    @NotBlank
    private String apiKey;
    
    private Duration timeout = Duration.ofSeconds(30);
    
    @Valid
    private RetryProperties retry = new RetryProperties();
    
    private List<String> supportedCurrencies = new ArrayList<>();
    
    // Getters and Setters required!
    
    public static class RetryProperties {
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(1000);
        
        // Getters and Setters
    }
}

// Enable configuration properties scanning
@SpringBootApplication
@ConfigurationPropertiesScan  // Or @EnableConfigurationProperties(PaymentGatewayProperties.class)
public class Application { }

// Usage
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentGatewayProperties properties;
    
    public void processPayment() {
        String url = properties.getUrl();
        Duration timeout = properties.getTimeout();
        int maxRetries = properties.getRetry().getMaxAttempts();
    }
}
```

### Immutable @ConfigurationProperties (Java Records)

```java
@ConfigurationProperties(prefix = "payment.gateway")
public record PaymentGatewayProperties(
    @NotBlank String url,
    @NotBlank String apiKey,
    @DefaultValue("30s") Duration timeout,
    @DefaultValue RetryProperties retry,
    List<String> supportedCurrencies
) {
    public record RetryProperties(
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("1000ms") Duration backoff
    ) {}
}
```

### Property Source Priority (Highest to Lowest)

1. Command line arguments (`--app.name=value`)
2. SPRING_APPLICATION_JSON
3. ServletConfig/ServletContext parameters
4. JNDI attributes
5. Java System properties (`-Dapp.name=value`)
6. OS environment variables
7. Profile-specific properties (`application-{profile}.yml`)
8. Application properties (`application.yml`)
9. @PropertySource annotations
10. Default properties

### Custom Property Sources

```java
@Configuration
@PropertySource("classpath:custom.properties")
@PropertySource(value = "file:/etc/myapp/config.properties", ignoreResourceNotFound = true)
public class PropertyConfig { }

// YAML requires custom factory
@Configuration
@PropertySource(value = "classpath:custom.yml", factory = YamlPropertySourceFactory.class)
public class YamlPropertyConfig { }

public class YamlPropertySourceFactory implements PropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());
        Properties properties = factory.getObject();
        return new PropertiesPropertySource(
            resource.getResource().getFilename(), 
            properties
        );
    }
}
```

---

## 10. Aspect-Oriented Programming (AOP)

### What Is AOP?

AOP lets you add behavior to existing code **without modifying it**. Perfect for cross-cutting concerns like:
- Logging
- Security
- Transactions
- Caching
- Performance monitoring

### AOP Terminology

```
┌─────────────────────────────────────────────────────────────────────┐
│                        AOP CONCEPTS                                 │
│                                                                     │
│  ASPECT         The module containing cross-cutting logic           │
│                 (@Aspect class)                                     │
│                                                                     │
│  JOIN POINT     A point during execution (method call, exception)   │
│                 In Spring AOP: only method execution                │
│                                                                     │
│  ADVICE         Action taken at a join point                        │
│                 @Before, @After, @Around, etc.                      │
│                                                                     │
│  POINTCUT       Expression that matches join points                 │
│                 "Which methods should the advice apply to?"         │
│                                                                     │
│  TARGET         The object being advised                            │
│                 (Your service class)                                │
│                                                                     │
│  PROXY          The object created by AOP to wrap target            │
│                 (Intercepts calls and applies advice)               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### How Spring AOP Works

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   Client Code                                                       │
│       │                                                             │
│       │ userService.createUser(user)                                │
│       ▼                                                             │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                    AOP PROXY                                 │   │
│   │                                                              │   │
│   │   1. @Before advice executes                                │   │
│   │   2. Call actual method ──────────────────┐                 │   │
│   │   3. @AfterReturning / @AfterThrowing     │                 │   │
│   │   4. @After advice executes               │                 │   │
│   │                                           ▼                 │   │
│   │                                    ┌──────────────────┐     │   │
│   │                                    │   UserService    │     │   │
│   │                                    │   (actual bean)  │     │   │
│   │                                    │   createUser()   │     │   │
│   │                                    └──────────────────┘     │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Setting Up AOP

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### Advice Types

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {
    
    // ═══════════════════════════════════════════════════════════════
    // @Before - Runs BEFORE the method
    // ═══════════════════════════════════════════════════════════════
    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        log.info(">>> Calling: {}.{}()", 
            joinPoint.getSignature().getDeclaringTypeName(),
            joinPoint.getSignature().getName());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // @AfterReturning - Runs after method returns successfully
    // ═══════════════════════════════════════════════════════════════
    @AfterReturning(
        pointcut = "execution(* com.example.service.*.*(..))",
        returning = "result"
    )
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("<<< Returned: {} = {}", 
            joinPoint.getSignature().getName(), result);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // @AfterThrowing - Runs when method throws exception
    // ═══════════════════════════════════════════════════════════════
    @AfterThrowing(
        pointcut = "execution(* com.example.service.*.*(..))",
        throwing = "exception"
    )
    public void logAfterThrowing(JoinPoint joinPoint, Exception exception) {
        log.error("!!! Exception in {}: {}", 
            joinPoint.getSignature().getName(), exception.getMessage());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // @After - Runs after method (regardless of outcome) - "finally"
    // ═══════════════════════════════════════════════════════════════
    @After("execution(* com.example.service.*.*(..))")
    public void logAfter(JoinPoint joinPoint) {
        log.debug("--- Completed: {}", joinPoint.getSignature().getName());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // @Around - Wraps the method (most powerful)
    // ═══════════════════════════════════════════════════════════════
    @Around("execution(* com.example.service.*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();  // Call actual method
            
            long duration = System.currentTimeMillis() - start;
            log.info("⏱ {}.{}() took {} ms",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                duration);
            
            return result;
        } catch (Exception e) {
            log.error("Exception in {}: {}", 
                joinPoint.getSignature().getName(), e.getMessage());
            throw e;
        }
    }
}
```

### Pointcut Expressions

```java
@Aspect
@Component
public class PointcutExamples {
    
    // ═══════════════════════════════════════════════════════════════
    // EXECUTION - Match method execution
    // ═══════════════════════════════════════════════════════════════
    
    // Any method in service package
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void serviceLayer() {}
    
    // Any public method
    @Pointcut("execution(public * *(..))")
    public void publicMethod() {}
    
    // Methods returning void
    @Pointcut("execution(void *(..))")
    public void voidMethods() {}
    
    // Methods starting with "get"
    @Pointcut("execution(* get*(..))")
    public void getters() {}
    
    // Methods with specific parameters
    @Pointcut("execution(* *..UserService.find*(Long))")
    public void findUserById() {}
    
    // Methods with any parameters
    @Pointcut("execution(* *..UserService.find*(..))")
    public void anyFindMethod() {}
    
    // ═══════════════════════════════════════════════════════════════
    // WITHIN - Match types
    // ═══════════════════════════════════════════════════════════════
    
    // All methods in specific class
    @Pointcut("within(com.example.service.UserService)")
    public void withinUserService() {}
    
    // All methods in package
    @Pointcut("within(com.example.service..*)")
    public void withinServicePackage() {}
    
    // ═══════════════════════════════════════════════════════════════
    // @annotation - Match by annotation
    // ═══════════════════════════════════════════════════════════════
    
    // Methods annotated with @Transactional
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalMethods() {}
    
    // Methods with custom annotation
    @Pointcut("@annotation(com.example.annotation.Auditable)")
    public void auditableMethods() {}
    
    // ═══════════════════════════════════════════════════════════════
    // @within - Match types with annotation
    // ═══════════════════════════════════════════════════════════════
    
    // All methods in @Service classes
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void serviceClasses() {}
    
    // ═══════════════════════════════════════════════════════════════
    // COMBINING POINTCUTS
    // ═══════════════════════════════════════════════════════════════
    
    @Pointcut("serviceLayer() && publicMethod()")
    public void publicServiceMethods() {}
    
    @Pointcut("serviceLayer() || repositoryLayer()")
    public void dataAccessMethods() {}
    
    @Pointcut("serviceLayer() && !getters()")
    public void nonGetterServiceMethods() {}
    
    // ═══════════════════════════════════════════════════════════════
    // USING COMBINED POINTCUTS
    // ═══════════════════════════════════════════════════════════════
    
    @Before("publicServiceMethods()")
    public void beforePublicServiceMethod(JoinPoint jp) {
        // ...
    }
}
```

### Pointcut Expression Syntax

```
execution(modifiers? return-type declaring-type? method-name(params) throws?)

execution(public * com.example.service.UserService.find*(..))
          │      │ │                        │        │
          │      │ │                        │        └── Any arguments
          │      │ │                        └── Method starts with "find"
          │      │ └── In UserService class
          │      └── Any return type
          └── Public methods only

Wildcards:
  *     - matches any single element
  ..    - matches zero or more elements
  +     - matches subclasses

Examples:
  * *..*Service.*(..)     - Any method in any class ending with Service
  * set*(..)              - Any setter method
  * *(.., String)         - Methods with last param as String
  * com.example..*.*(..)  - All methods in com.example and sub-packages
```

---

## 11. Custom Annotations with AOP

### Example 1: Method Timing

```java
// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {
    String value() default "";
}

// Aspect
@Aspect
@Component
@Slf4j
public class TimingAspect {
    
    @Around("@annotation(timed)")
    public Object timeMethod(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        String methodName = timed.value().isEmpty() 
            ? joinPoint.getSignature().getName() 
            : timed.value();
        
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = (System.nanoTime() - start) / 1_000_000;
            log.info("⏱ {} executed in {} ms", methodName, duration);
        }
    }
}

// Usage
@Service
public class OrderService {
    
    @Timed("order-creation")
    public Order createOrder(CreateOrderRequest request) {
        // Method execution time will be logged
    }
}
```

### Example 2: Retry Mechanism

```java
// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {
    int maxAttempts() default 3;
    long backoffMs() default 1000;
    Class<? extends Throwable>[] retryOn() default {Exception.class};
}

// Aspect
@Aspect
@Component
@Slf4j
public class RetryAspect {
    
    @Around("@annotation(retry)")
    public Object retryMethod(ProceedingJoinPoint joinPoint, Retry retry) throws Throwable {
        int attempts = 0;
        Throwable lastException = null;
        
        while (attempts < retry.maxAttempts()) {
            attempts++;
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                lastException = e;
                
                if (!shouldRetry(e, retry.retryOn())) {
                    throw e;
                }
                
                log.warn("Attempt {}/{} failed for {}: {}. Retrying in {} ms...",
                    attempts, retry.maxAttempts(),
                    joinPoint.getSignature().getName(),
                    e.getMessage(),
                    retry.backoffMs());
                
                if (attempts < retry.maxAttempts()) {
                    Thread.sleep(retry.backoffMs());
                }
            }
        }
        
        throw lastException;
    }
    
    private boolean shouldRetry(Throwable e, Class<? extends Throwable>[] retryOn) {
        for (Class<? extends Throwable> retryable : retryOn) {
            if (retryable.isInstance(e)) {
                return true;
            }
        }
        return false;
    }
}

// Usage
@Service
public class ExternalApiService {
    
    @Retry(maxAttempts = 5, backoffMs = 2000, retryOn = {IOException.class, TimeoutException.class})
    public ApiResponse callExternalApi(ApiRequest request) {
        // Will retry up to 5 times on IO/Timeout exceptions
    }
}
```

### Example 3: Audit Logging

```java
// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();
    String resourceType() default "";
}

// Aspect
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {
    
    private final AuditLogRepository auditLogRepository;
    
    @AfterReturning(
        pointcut = "@annotation(auditable)",
        returning = "result"
    )
    public void auditMethod(JoinPoint joinPoint, Auditable auditable, Object result) {
        AuditLog log = AuditLog.builder()
            .action(auditable.action())
            .resourceType(auditable.resourceType())
            .resourceId(extractResourceId(result))
            .userId(getCurrentUserId())
            .timestamp(Instant.now())
            .methodName(joinPoint.getSignature().toShortString())
            .arguments(Arrays.toString(joinPoint.getArgs()))
            .build();
        
        auditLogRepository.save(log);
    }
    
    @AfterThrowing(
        pointcut = "@annotation(auditable)",
        throwing = "exception"
    )
    public void auditFailure(JoinPoint joinPoint, Auditable auditable, Exception exception) {
        AuditLog log = AuditLog.builder()
            .action(auditable.action() + "_FAILED")
            .resourceType(auditable.resourceType())
            .userId(getCurrentUserId())
            .timestamp(Instant.now())
            .errorMessage(exception.getMessage())
            .build();
        
        auditLogRepository.save(log);
    }
}

// Usage
@Service
public class UserService {
    
    @Auditable(action = "CREATE_USER", resourceType = "USER")
    public User createUser(CreateUserRequest request) {
        // Automatically audited
    }
    
    @Auditable(action = "DELETE_USER", resourceType = "USER")
    public void deleteUser(Long userId) {
        // Automatically audited
    }
}
```

### Example 4: Method-Level Caching with TTL

```java
// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheResult {
    String cacheName();
    String key() default "";
    int ttlSeconds() default 300;
}

// Aspect
@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {
    
    private final CacheManager cacheManager;
    
    @Around("@annotation(cacheResult)")
    public Object cacheMethod(ProceedingJoinPoint joinPoint, CacheResult cacheResult) throws Throwable {
        String cacheKey = buildCacheKey(joinPoint, cacheResult.key());
        Cache cache = cacheManager.getCache(cacheResult.cacheName());
        
        // Check cache
        Cache.ValueWrapper cached = cache.get(cacheKey);
        if (cached != null) {
            return cached.get();
        }
        
        // Execute method and cache result
        Object result = joinPoint.proceed();
        cache.put(cacheKey, result);
        
        return result;
    }
    
    private String buildCacheKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        if (keyExpression.isEmpty()) {
            return joinPoint.getSignature().toShortString() + 
                   Arrays.toString(joinPoint.getArgs());
        }
        // Parse SpEL expression for custom key
        return parseKeyExpression(joinPoint, keyExpression);
    }
}
```

---

## 12. Event-Driven Architecture

### Publishing and Listening to Events

```java
// Custom event
public class UserCreatedEvent extends ApplicationEvent {
    
    private final User user;
    
    public UserCreatedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
    
    public User getUser() {
        return user;
    }
}

// Or use a record (simpler, Spring 4.2+)
public record UserCreatedEvent(User user) {}

// Publisher
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public User createUser(CreateUserRequest request) {
        User user = userRepository.save(mapToEntity(request));
        
        // Publish event
        eventPublisher.publishEvent(new UserCreatedEvent(user));
        
        return user;
    }
}

// Listener
@Component
@Slf4j
public class UserEventListener {
    
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("User created: {}", event.user().getEmail());
        // Send welcome email, create default settings, etc.
    }
}
```

### Asynchronous Event Handling

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

@Component
@Slf4j
public class UserEventListener {
    
    @Async  // Runs in separate thread
    @EventListener
    public void handleUserCreatedAsync(UserCreatedEvent event) {
        log.info("Async processing user: {}", event.user().getEmail());
        // Long-running task won't block the main thread
    }
}
```

### Transactional Event Listeners

```java
@Component
@Slf4j
public class UserEventListener {
    
    // Runs AFTER transaction commits successfully
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreatedAfterCommit(UserCreatedEvent event) {
        log.info("User committed to DB: {}", event.user().getId());
        // Safe to send email - user definitely exists
    }
    
    // Runs BEFORE transaction commits
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleBeforeCommit(UserCreatedEvent event) {
        // Validate or modify before commit
    }
    
    // Runs AFTER transaction rollback
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleAfterRollback(UserCreatedEvent event) {
        log.warn("User creation rolled back");
    }
    
    // Runs after completion (commit or rollback)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void handleAfterCompletion(UserCreatedEvent event) {
        // Cleanup regardless of outcome
    }
}
```

### Conditional Event Handling

```java
@Component
public class UserEventListener {
    
    // Only handle if condition is true
    @EventListener(condition = "#event.user.role == 'ADMIN'")
    public void handleAdminCreated(UserCreatedEvent event) {
        // Only for admin users
    }
    
    // Handle multiple event types
    @EventListener({UserCreatedEvent.class, UserUpdatedEvent.class})
    public void handleUserChange(Object event) {
        // Handles both event types
    }
}
```

### Generic Event Pattern

```java
// Generic event wrapper
public record EntityEvent<T>(
    EventType type,
    T entity,
    Instant timestamp
) {
    public enum EventType {
        CREATED, UPDATED, DELETED
    }
    
    public static <T> EntityEvent<T> created(T entity) {
        return new EntityEvent<>(EventType.CREATED, entity, Instant.now());
    }
    
    public static <T> EntityEvent<T> updated(T entity) {
        return new EntityEvent<>(EventType.UPDATED, entity, Instant.now());
    }
    
    public static <T> EntityEvent<T> deleted(T entity) {
        return new EntityEvent<>(EventType.DELETED, entity, Instant.now());
    }
}

// Publish
eventPublisher.publishEvent(EntityEvent.created(user));

// Listen with generic type resolution
@EventListener
public void handleUserEvent(EntityEvent<User> event) {
    switch (event.type()) {
        case CREATED -> handleUserCreated(event.entity());
        case UPDATED -> handleUserUpdated(event.entity());
        case DELETED -> handleUserDeleted(event.entity());
    }
}
```

---

## 13. SpEL - Spring Expression Language

### Basic Expressions

```java
@Component
public class SpELExamples {
    
    // Literal values
    @Value("#{100}")
    private int number;
    
    @Value("#{'Hello World'}")
    private String text;
    
    @Value("#{true}")
    private boolean flag;
    
    // Property access
    @Value("#{systemProperties['user.home']}")
    private String userHome;
    
    @Value("#{systemEnvironment['JAVA_HOME']}")
    private String javaHome;
    
    // Arithmetic
    @Value("#{10 * 2 + 5}")
    private int calculated;  // 25
    
    // String concatenation
    @Value("#{'Hello ' + 'World'}")
    private String greeting;
    
    // Ternary operator
    @Value("#{systemProperties['os.name'].contains('Windows') ? 'win' : 'unix'}")
    private String osType;
    
    // Elvis operator (default value)
    @Value("#{systemProperties['missing.property'] ?: 'default'}")
    private String withDefault;
    
    // Safe navigation (null-safe)
    @Value("#{systemProperties['missing']?.length()}")
    private Integer nullSafeLength;  // null instead of NPE
}
```

### Bean References in SpEL

```java
@Component("myConfig")
public class AppConfig {
    private String appName = "MyApp";
    private int maxUsers = 100;
    
    public String getAppName() { return appName; }
    public int getMaxUsers() { return maxUsers; }
    public boolean isProduction() { return true; }
}

@Service
public class MyService {
    
    // Reference another bean
    @Value("#{myConfig.appName}")
    private String appName;
    
    // Call bean method
    @Value("#{myConfig.maxUsers * 2}")
    private int doubleMaxUsers;
    
    // Conditional based on bean
    @Value("#{myConfig.production ? 'PROD' : 'DEV'}")
    private String environment;
}
```

### SpEL in Annotations

```java
// @Scheduled with SpEL
@Scheduled(fixedDelayString = "#{@schedulerConfig.interval}")
public void scheduledTask() { }

// @Cacheable with SpEL key
@Cacheable(value = "users", key = "#userId")
public User getUser(Long userId) { }

// @PreAuthorize with SpEL
@PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
public User getUser(Long userId) { }

// @EventListener with SpEL condition
@EventListener(condition = "#event.user.active")
public void handleActiveUser(UserEvent event) { }

// @ConditionalOnExpression
@Bean
@ConditionalOnExpression("${feature.enabled:false} and '${spring.profiles.active}' == 'prod'")
public FeatureService featureService() { }
```

### Programmatic SpEL

```java
@Service
public class SpELService {
    
    private final ExpressionParser parser = new SpelExpressionParser();
    
    public Object evaluate(String expression, Object context) {
        Expression exp = parser.parseExpression(expression);
        
        // Simple evaluation
        if (context == null) {
            return exp.getValue();
        }
        
        // With context object
        StandardEvaluationContext evalContext = new StandardEvaluationContext(context);
        return exp.getValue(evalContext);
    }
    
    public void examples() {
        // Simple expressions
        Integer result = parser.parseExpression("10 * 5").getValue(Integer.class);
        
        // With root object
        User user = new User("John", 30);
        StandardEvaluationContext context = new StandardEvaluationContext(user);
        String name = parser.parseExpression("name").getValue(context, String.class);
        Boolean isAdult = parser.parseExpression("age >= 18").getValue(context, Boolean.class);
        
        // Collection operations
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        context.setVariable("numbers", numbers);
        List<?> filtered = parser.parseExpression("#numbers.?[#this > 2]")
            .getValue(context, List.class);  // [3, 4, 5]
        
        // Collection projection
        List<?> doubled = parser.parseExpression("#numbers.![#this * 2]")
            .getValue(context, List.class);  // [2, 4, 6, 8, 10]
    }
}
```

### SpEL Collection Operations

```java
// Filtering with selection (.?[])
@Value("#{users.?[age > 18]}")
private List<User> adults;

// First match (.^[])
@Value("#{users.^[role == 'ADMIN']}")
private User firstAdmin;

// Last match (.$[])
@Value("#{users.$[active == true]}")
private User lastActiveUser;

// Projection (.![])
@Value("#{users.![name]}")
private List<String> userNames;

// Combined
@Value("#{users.?[active].![email]}")
private List<String> activeUserEmails;
```

---

## 14. Resource Handling

### Loading Resources

```java
@Service
public class ResourceService {
    
    @Value("classpath:data/config.json")
    private Resource configFile;
    
    @Value("classpath:templates/*.html")
    private Resource[] templates;  // Multiple resources with wildcard
    
    @Value("file:/etc/myapp/settings.yml")
    private Resource externalConfig;
    
    @Value("https://example.com/api/config")
    private Resource remoteConfig;
    
    public String readConfig() throws IOException {
        return new String(configFile.getInputStream().readAllBytes());
    }
    
    public boolean configExists() {
        return configFile.exists();
    }
}
```

### ResourceLoader

```java
@Service
@RequiredArgsConstructor
public class ResourceService {
    
    private final ResourceLoader resourceLoader;
    
    public Resource loadResource(String location) {
        // Supports: classpath:, file:, http:, etc.
        return resourceLoader.getResource(location);
    }
    
    public Resource[] loadResources(String pattern) throws IOException {
        ResourcePatternResolver resolver = (ResourcePatternResolver) resourceLoader;
        return resolver.getResources(pattern);
    }
    
    public void examples() throws IOException {
        // Classpath resource
        Resource classpathRes = resourceLoader.getResource("classpath:data.json");
        
        // File system resource
        Resource fileRes = resourceLoader.getResource("file:/path/to/file.txt");
        
        // URL resource
        Resource urlRes = resourceLoader.getResource("https://example.com/data.json");
        
        // Pattern matching
        Resource[] allYaml = loadResources("classpath*:config/*.yml");
    }
}
```

### Resource Prefixes

| Prefix | Description | Example |
|--------|-------------|---------|
| `classpath:` | From classpath root | `classpath:config.yml` |
| `classpath*:` | All matching from classpath | `classpath*:META-INF/*.xml` |
| `file:` | File system | `file:/etc/app/config.yml` |
| `http:` / `https:` | URL resource | `https://example.com/config` |
| (none) | Depends on context | `config.yml` |

---

## 15. Common Pitfalls & Best Practices

### Pitfall 1: Field Injection

```java
// ❌ BAD
@Service
public class UserService {
    @Autowired
    private UserRepository repository;
}

// ✅ GOOD
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
}
```

### Pitfall 2: Circular Dependencies

```java
// ❌ BAD - Circular dependency
@Service
public class ServiceA {
    @Autowired
    private ServiceB serviceB;
}

@Service
public class ServiceB {
    @Autowired
    private ServiceA serviceA;
}

// ✅ GOOD - Refactor to break the cycle
@Service
public class ServiceA {
    private final CommonService common;
}

@Service
public class ServiceB {
    private final CommonService common;
}

@Service
public class CommonService {
    // Shared logic extracted here
}
```

### Pitfall 3: Proxy Self-Invocation

```java
// ❌ BAD - @Transactional won't work
@Service
public class UserService {
    
    public void process() {
        saveUser(new User());  // Internal call - no proxy!
    }
    
    @Transactional
    public void saveUser(User user) {
        // Transaction NOT started!
    }
}

// ✅ GOOD - External call or self-injection
@Service
public class UserService {
    
    @Transactional
    public void process() {
        saveUser(new User());  // Now inside transaction
    }
    
    private void saveUser(User user) {
        // Part of the transaction
    }
}
```

### Pitfall 4: Mutable Singleton State

```java
// ❌ BAD - Shared mutable state
@Service
public class CounterService {
    private int counter = 0;  // Shared across all threads!
    
    public int increment() {
        return counter++;  // Race condition!
    }
}

// ✅ GOOD - Use atomic or stateless
@Service
public class CounterService {
    private final AtomicInteger counter = new AtomicInteger(0);
    
    public int increment() {
        return counter.incrementAndGet();  // Thread-safe
    }
}
```

### Pitfall 5: Missing @Configuration on Config Classes

```java
// ❌ BAD - @Bean methods may not work correctly
@Component  // Should be @Configuration!
public class AppConfig {
    
    @Bean
    public ServiceA serviceA() {
        return new ServiceA(serviceB());  // May create multiple instances!
    }
    
    @Bean
    public ServiceB serviceB() {
        return new ServiceB();
    }
}

// ✅ GOOD - @Configuration enables CGLIB proxying
@Configuration
public class AppConfig {
    
    @Bean
    public ServiceA serviceA() {
        return new ServiceA(serviceB());  // Calls cached bean
    }
    
    @Bean
    public ServiceB serviceB() {
        return new ServiceB();
    }
}
```

### Best Practices Summary

| Category | Best Practice |
|----------|---------------|
| **DI** | Use constructor injection with `final` fields |
| **Beans** | Keep singletons stateless |
| **Config** | Use `@ConfigurationProperties` over `@Value` |
| **Profiles** | Use profile groups for related configurations |
| **AOP** | Keep aspects focused and single-purpose |
| **Events** | Use `@TransactionalEventListener` for data-dependent events |
| **Testing** | Design for testability (interfaces, constructor injection) |
| **Naming** | Use meaningful bean names |
| **Scope** | Default to singleton; use prototype only when needed |
| **Lifecycle** | Use `@PostConstruct`/`@PreDestroy` over interfaces |

---

## Quick Reference Card

### Stereotype Annotations

| Annotation | Purpose |
|------------|---------|
| `@Component` | Generic Spring-managed component |
| `@Service` | Business logic layer |
| `@Repository` | Data access layer (+ exception translation) |
| `@Controller` | Web MVC controller |
| `@RestController` | REST API controller |
| `@Configuration` | Java configuration class |

### Injection Annotations

| Annotation | Purpose |
|------------|---------|
| `@Autowired` | Inject dependency (optional for constructors) |
| `@Qualifier` | Specify which bean to inject |
| `@Primary` | Mark as default bean |
| `@Value` | Inject property or SpEL expression |

### Lifecycle Annotations

| Annotation | Purpose |
|------------|---------|
| `@PostConstruct` | Run after initialization |
| `@PreDestroy` | Run before destruction |
| `@Scope` | Define bean scope |
| `@Lazy` | Delay bean creation |

### Configuration Annotations

| Annotation | Purpose |
|------------|---------|
| `@Bean` | Define bean in @Configuration |
| `@Profile` | Activate for specific profiles |
| `@Conditional*` | Conditional bean registration |
| `@ConfigurationProperties` | Type-safe configuration |

### AOP Annotations

| Annotation | Purpose |
|------------|---------|
| `@Aspect` | Mark class as aspect |
| `@Before` | Run before method |
| `@After` | Run after method (finally) |
| `@AfterReturning` | Run after successful return |
| `@AfterThrowing` | Run after exception |
| `@Around` | Wrap method execution |
| `@Pointcut` | Define reusable pointcut |

---

## Summary

**Key Takeaways:**

1. **IoC inverts control** — Spring manages object creation and lifecycle
2. **Constructor injection is king** — immutable, testable, explicit
3. **Understand the proxy pattern** — it powers @Transactional, AOP, and more
4. **Singletons must be stateless** — shared state causes concurrency bugs
5. **@ConfigurationProperties > @Value** — type-safe, validated, documented
6. **AOP is powerful** — but keep aspects focused and simple
7. **Events decouple components** — use @TransactionalEventListener for safety
8. **Profiles organize environments** — use profile groups for complex setups

**The Spring Mental Model:**
```
Your Code (POJOs) + Configuration = Spring-Managed Application
                         │
                         ├── @Component, @Service, @Repository
                         ├── @Configuration + @Bean
                         ├── @ConfigurationProperties
                         └── @Conditional, @Profile
```

---

**You now understand the foundation that powers everything in Spring!** 🚀
