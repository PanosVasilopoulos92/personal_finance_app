# 🚀 Personal Finance App - Learning Roadmap Part 6
## Phase 9: Production-Ready Features

> **Final Phase!** - Complete Phases 1-8 before starting here!

---

## Phase 9: Production-Ready Features

**Duration:** 1-2 weeks  
**Goal:** Transform your application into a production-ready system  
**Difficulty:** ⭐⭐⭐⭐

### 📚 What You'll Learn

- Application configuration and profiles
- Database migrations with Flyway
- Structured logging with Logback
- API documentation with SpringDoc
- Docker containerization
- Health checks and monitoring
- Performance optimization
- Security hardening
- Deployment strategies
- Production troubleshooting

---

## Step 1: Application Configuration & Profiles

### Understanding Profiles

Spring profiles allow different configurations for different environments:
- **dev** - Development (localhost, H2 database, verbose logging)
- **test** - Testing (in-memory database, test data)
- **prod** - Production (real database, minimal logging, security)

### application.yml (Base Configuration)

```yaml
# Base configuration - applies to all profiles
spring:
  application:
    name: personal-finance-app
    
  # JPA Configuration
  jpa:
    show-sql: false  # Don't show SQL by default
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
        
  # Jackson Configuration (JSON)
  jackson:
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null
    time-zone: UTC
    
  # Server Configuration
server:
  port: 8080
  error:
    include-message: true
    include-binding-errors: true
    include-stacktrace: on_param  # Only show stacktrace with ?trace=true
    include-exception: false

# JWT Configuration
jwt:
  expiration: 86400000  # 24 hours in milliseconds

# Logging Configuration
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  level:
    root: INFO
    org.viators.personal_finance_app: INFO

# Management/Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

### application-dev.yml (Development Profile)

```yaml
# Development-specific configuration
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/personal_finance_dev?serverTimezone=UTC&createDatabaseIfNotExist=true
    username: dev_user
    password: dev_password
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  jpa:
    hibernate:
      ddl-auto: validate  # Let Flyway handle schema
    show-sql: true  # Show SQL in dev
    properties:
      hibernate:
        format_sql: true
        
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    
  # H2 Console (optional, for quick testing)
  h2:
    console:
      enabled: true
      path: /h2-console

# JWT Secret (use environment variable in real dev)
jwt:
  secret: dev-secret-key-at-least-256-bits-long-for-hs256-algorithm-development-only

# Logging - Verbose in dev
logging:
  level:
    root: INFO
    org.viators.personal_finance_app: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

# Server
server:
  port: 8080
  error:
    include-stacktrace: always  # Full stacktrace in dev
```

### application-test.yml (Test Profile)

```yaml
# Test-specific configuration
spring:
  datasource:
    # H2 in-memory database for tests
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: 
    driver-class-name: org.h2.Driver
    
  jpa:
    hibernate:
      ddl-auto: create-drop  # Recreate schema for each test
    show-sql: false
    
  flyway:
    enabled: false  # Don't use Flyway in tests

# JWT Secret for tests
jwt:
  secret: test-secret-key-at-least-256-bits-long-for-hs256-algorithm-testing-only
  expiration: 3600000  # 1 hour for tests

# Logging - Minimal in tests
logging:
  level:
    root: ERROR
    org.viators.personal_finance_app: INFO
```

### application-prod.yml (Production Profile)

```yaml
# Production-specific configuration
spring:
  datasource:
    # Use environment variables in production!
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
    # Connection pool configuration
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      
  jpa:
    hibernate:
      ddl-auto: validate  # NEVER use create/update in production
    show-sql: false  # Don't log SQL in production
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
    
# JWT Secret - MUST be environment variable
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000

# Logging - Production level
logging:
  level:
    root: WARN
    org.viators.personal_finance_app: INFO
  file:
    name: /var/log/personal-finance-app/application.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30

# Server
server:
  port: ${PORT:8080}
  error:
    include-message: false  # Don't expose error details
    include-binding-errors: false
    include-stacktrace: never  # Never in production
    include-exception: false
  compression:
    enabled: true
  http2:
    enabled: true

# Management/Actuator - Restricted in production
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  endpoint:
    health:
      show-details: never  # Don't expose internal details
```

### Activating Profiles

**In application:**
```bash
# Command line
java -jar app.jar --spring.profiles.active=prod

# Environment variable
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar

# IDE (IntelliJ)
Run Configuration → VM Options: -Dspring.profiles.active=dev
```

**In tests:**
```java
@SpringBootTest
@ActiveProfiles("test")
class MyIntegrationTest {
    // Uses application-test.yml
}
```

---

## Step 2: Database Migrations with Flyway

### Why Flyway?

**Without Flyway:**
```
You: "Hey, did you run those ALTER TABLE commands I sent?"
Teammate: "Which ones?"
Production: *Different schema than dev* 💥
```

**With Flyway:**
```
You: "Just deploy, Flyway handles migrations"
Flyway: *Applies migrations automatically*
Production: *Always in sync* ✅
```

### Flyway Setup

**1. Add Dependency (already in pom.xml):**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

**2. Create Migration Directory:**
```
src/main/resources/
└── db/
    └── migration/
        ├── V1__create_users_table.sql
        ├── V2__create_items_table.sql
        ├── V3__create_categories_table.sql
        └── V4__create_price_observations_table.sql
```

### Migration Naming Convention

```
V{version}__{description}.sql

Examples:
V1__initial_schema.sql
V2__add_user_preferences.sql
V3__add_email_index.sql
V4__alter_item_add_barcode.sql
```

**Rules:**
- Start with `V` (capital V)
- Version number (1, 2, 3, or 1.0, 1.1, 2.0)
- Two underscores `__`
- Description with underscores
- `.sql` extension

### Complete Example Migrations

**V1__create_users_table.sql:**
```sql
-- Create users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    age INT,
    user_role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_users_email (email),
    INDEX idx_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add some constraints
ALTER TABLE users
    ADD CONSTRAINT chk_users_age CHECK (age >= 13 OR age IS NULL);

-- Insert default admin user (for initial setup)
INSERT INTO users (username, email, password, first_name, last_name, user_role)
VALUES ('admin', 'admin@example.com', '$2a$10$encrypted_password_here', 'Admin', 'User', 'ADMIN');
```

**V2__create_categories_table.sql:**
```sql
-- Create categories table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    parent_category_id BIGINT,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_category_id) REFERENCES categories(id) ON DELETE SET NULL,
    
    INDEX idx_categories_user (user_id),
    INDEX idx_categories_parent (parent_category_id),
    UNIQUE INDEX idx_categories_name_user (name, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**V3__create_items_table.sql:**
```sql
-- Create items table
CREATE TABLE items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    item_unit VARCHAR(20) NOT NULL,
    brand VARCHAR(100),
    user_id BIGINT NOT NULL,
    category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    
    INDEX idx_items_user (user_id),
    INDEX idx_items_category (category_id),
    INDEX idx_items_name (name),
    UNIQUE INDEX idx_items_name_user (name, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**V4__create_stores_table.sql:**
```sql
-- Create stores table
CREATE TABLE stores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(50),
    store_type VARCHAR(50) NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_stores_user (user_id),
    INDEX idx_stores_city (city),
    INDEX idx_stores_type (store_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**V5__create_price_observations_table.sql:**
```sql
-- Create price observations table
CREATE TABLE price_observations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    observation_date DATE NOT NULL,
    location VARCHAR(255) NOT NULL,
    notes VARCHAR(800),
    item_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    
    INDEX idx_price_observations_item (item_id),
    INDEX idx_price_observations_store (store_id),
    INDEX idx_price_observations_date (observation_date),
    
    CONSTRAINT chk_price_positive CHECK (price > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**V6__create_user_preferences_table.sql:**
```sql
-- Create user preferences table
CREATE TABLE user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    currency VARCHAR(10) DEFAULT 'USD',
    language VARCHAR(10) DEFAULT 'en',
    theme VARCHAR(20) DEFAULT 'light',
    user_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Flyway Best Practices

**1. Never Modify Existing Migrations**
```sql
-- ❌ BAD - Don't modify V1__create_users.sql after it's run

-- ✅ GOOD - Create new migration
-- V7__add_phone_to_users.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

**2. Use Descriptive Names**
```sql
-- ✅ GOOD
V8__add_user_profile_picture_url.sql
V9__create_index_on_items_name.sql
V10__add_soft_delete_to_users.sql

-- ❌ BAD
V8__update.sql
V9__changes.sql
```

**3. Test Migrations Locally First**
```bash
# Clean database
mvn flyway:clean

# Run migrations
mvn flyway:migrate

# Verify
mvn flyway:info
```

**4. Make Migrations Reversible (when possible)**
```sql
-- V11__add_status_to_items.sql
ALTER TABLE items ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';

-- If you need to rollback manually, know how:
-- ALTER TABLE items DROP COLUMN status;
```

---

## Step 3: Logging Configuration

### Logback Configuration (logback-spring.xml)

Create `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- Property for log file location -->
    <springProperty scope="context" name="LOG_FILE" source="logging.file.name" defaultValue="logs/application.log"/>
    
    <!-- Console Appender (for development) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- File Appender (for production) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE}</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_FILE}.%d{yyyy-MM-dd}.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>
    
    <!-- JSON Appender (for log aggregation services) -->
    <appender name="JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application-json.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeCallerData>true</includeCallerData>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application-json.%d{yyyy-MM-dd}.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>
    
    <!-- Development Profile -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE" />
        </root>
        <logger name="org.viators.personal_finance_app" level="DEBUG"/>
        <logger name="org.springframework.web" level="DEBUG"/>
    </springProfile>
    
    <!-- Test Profile -->
    <springProfile name="test">
        <root level="ERROR">
            <appender-ref ref="CONSOLE" />
        </root>
        <logger name="org.viators.personal_finance_app" level="INFO"/>
    </springProfile>
    
    <!-- Production Profile -->
    <springProfile name="prod">
        <root level="WARN">
            <appender-ref ref="FILE" />
            <appender-ref ref="JSON" />
        </root>
        <logger name="org.viators.personal_finance_app" level="INFO"/>
    </springProfile>
    
</configuration>
```

### Logging Best Practices

**1. Use Appropriate Log Levels**
```java
@Slf4j
@Service
public class UserService {
    
    public UserResponse createUser(CreateUserRequest request) {
        // INFO - Important business events
        log.info("Creating user with email: {}", request.email());
        
        // DEBUG - Detailed flow information
        log.debug("Validating user data: {}", request);
        
        // WARN - Something unexpected but handled
        log.warn("User registration with age {}, might be suspicious", request.age());
        
        // ERROR - Something went wrong
        try {
            // ...
        } catch (Exception e) {
            log.error("Failed to create user: {}", request.email(), e);
            throw e;
        }
        
        log.info("Successfully created user with id: {}", user.getId());
    }
}
```

**2. Use Structured Logging**
```java
// ✅ GOOD - Structured, searchable
log.info("User login successful - userId: {}, email: {}, loginTime: {}", 
    userId, email, LocalDateTime.now());

// ❌ BAD - Hard to search/parse
log.info("User " + email + " logged in at " + LocalDateTime.now());
```

**3. Don't Log Sensitive Data**
```java
// ❌ NEVER log passwords, tokens, credit cards
log.debug("User login: {}", user.getPassword());  // NO!
log.debug("JWT token: {}", token);  // NO!

// ✅ GOOD - Log safe information
log.debug("User login attempt: email={}", user.getEmail());
log.debug("JWT token generated for userId: {}", userId);
```

---

## Step 4: API Documentation with SpringDoc

### SpringDoc Setup

**1. Add Dependency:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**2. Configuration Class:**
```java
/**
 * OpenAPI/Swagger configuration.
 * 
 * Provides interactive API documentation at:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI personalFinanceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Personal Finance API")
                .description("API for managing personal finances, price tracking, and shopping lists")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Your Name")
                    .email("your.email@example.com")
                    .url("https://github.com/yourusername/personal-finance-app"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .externalDocs(new ExternalDocumentation()
                .description("Project Documentation")
                .url("https://github.com/yourusername/personal-finance-app/wiki"))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }
    
    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .bearerFormat("JWT")
            .scheme("bearer");
    }
}
```

**3. Document Controllers with Annotations:**
```java
/**
 * User management API.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
public class UserController {
    
    private final UserService userService;
    
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves user details by their unique identifier",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "User found",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "User not found",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)
                )
            )
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID", required = true, example = "123")
            @PathVariable Long id) {
        
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Create new user",
        description = "Registers a new user in the system",
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "User created successfully"
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid input data"
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Email or username already exists"
            )
        }
    )
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "User registration data",
                required = true,
                content = @Content(schema = @Schema(implementation = CreateUserRequest.class))
            )
            @Valid @RequestBody CreateUserRequest request) {
        
        UserResponse response = userService.registerUser(request);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        
        return ResponseEntity.created(location).body(response);
    }
}
```

**4. Access Swagger UI:**
```
Open browser: http://localhost:8080/swagger-ui.html
```

---

## Step 5: Docker Containerization

### Dockerfile

Create `Dockerfile` in project root:

```dockerfile
# Multi-stage build for smaller image

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s     CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### docker-compose.yml

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  # MySQL Database
  mysql:
    image: mysql:8.0
    container_name: personal-finance-db
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: personal_finance
      MYSQL_USER: appuser
      MYSQL_PASSWORD: apppassword
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - app-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Spring Boot Application
  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: personal-finance-app
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATABASE_URL: jdbc:mysql://mysql:3306/personal_finance?serverTimezone=UTC
      DATABASE_USERNAME: appuser
      DATABASE_PASSWORD: apppassword
      JWT_SECRET: ${JWT_SECRET:-your-production-jwt-secret-change-this}
    ports:
      - "8080:8080"
    networks:
      - app-network
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

volumes:
  mysql-data:

networks:
  app-network:
    driver: bridge
```

### Docker Commands

```bash
# Build and run with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all containers
docker-compose down

# Stop and remove volumes (clean start)
docker-compose down -v

# Build only (without starting)
docker-compose build

# Rebuild and restart
docker-compose up -d --build
```

---

## Step 6: Health Checks & Monitoring

### Spring Boot Actuator

**Already included in dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Actuator Endpoints

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
```

### Available Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### Custom Health Indicator

```java
/**
 * Custom health indicator for database connectivity.
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final UserRepository userRepository;
    
    public DatabaseHealthIndicator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public Health health() {
        try {
            // Try to count users
            long count = userRepository.count();
            return Health.up()
                .withDetail("database", "Available")
                .withDetail("userCount", count)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "Unavailable")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

---

## Step 7: Performance Optimization

### Database Optimization

**1. Index Important Columns:**
```sql
-- Already in migrations, but verify:
CREATE INDEX idx_items_user_id ON items(user_id);
CREATE INDEX idx_items_name ON items(name);
CREATE INDEX idx_price_observations_date ON price_observations(observation_date);
```

**2. Use Pagination:**
```java
// ✅ GOOD - Paginated
@GetMapping
public Page<ItemResponse> getItems(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    
    Pageable pageable = PageRequest.of(page, size);
    return itemService.getItems(pageable);
}

// ❌ BAD - Loading all records
@GetMapping
public List<ItemResponse> getItems() {
    return itemService.getAllItems();  // Could be thousands!
}
```

**3. Use JOIN FETCH:**
```java
// ✅ GOOD - One query
@Query("SELECT i FROM Item i LEFT JOIN FETCH i.category WHERE i.user.id = :userId")
List<Item> findByUserIdWithCategory(@Param("userId") Long userId);

// ❌ BAD - N+1 queries
List<Item> items = itemRepository.findByUserId(userId);
items.forEach(item -> item.getCategory().getName());  // N additional queries!
```

**4. Enable Second-Level Cache (Optional):**
```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          use_second_level_cache: true
          region:
            factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
```

### Application Optimization

**1. Connection Pooling:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
```

**2. Enable HTTP Compression:**
```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
```

**3. Enable Caching for Static Resources:**
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
    }
}
```

---

## Step 8: Security Hardening

### Production Security Checklist

**1. Environment Variables for Secrets:**
```yaml
# ✅ GOOD
jwt:
  secret: ${JWT_SECRET}

# ❌ BAD
jwt:
  secret: hardcoded-secret-in-code
```

**2. Disable Unnecessary Endpoints:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics  # Only expose what's needed
```

**3. Use HTTPS in Production:**
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

**4. Set Security Headers:**
```java
@Configuration
public class SecurityHeadersConfig {
    
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SecurityHeadersFilter());
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
    
    private static class SecurityHeadersFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "DENY");
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            
            chain.doFilter(request, response);
        }
    }
}
```

**5. Rate Limiting (Optional):**
```java
// Using Bucket4j library
@Component
public class RateLimitFilter implements Filter {
    
    private final Bucket bucket;
    
    public RateLimitFilter() {
        // 100 requests per minute
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        this.bucket = Bucket.builder()
            .addLimit(limit)
            .build();
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429);  // Too Many Requests
            httpResponse.getWriter().write("Rate limit exceeded");
        }
    }
}
```

---

## Step 9: Deployment Strategies

### Option 1: Traditional Server Deployment

**1. Build JAR:**
```bash
mvn clean package -DskipTests
```

**2. Run on Server:**
```bash
# Transfer JAR to server
scp target/personal-finance-app.jar user@server:/opt/app/

# Run with systemd service
sudo systemctl start personal-finance-app
```

**systemd service file (`/etc/systemd/system/personal-finance-app.service`):**
```ini
[Unit]
Description=Personal Finance Application
After=syslog.target network.target

[Service]
User=appuser
ExecStart=/usr/bin/java -jar /opt/app/personal-finance-app.jar
SuccessExitStatus=143
StandardOutput=journal
StandardError=journal
Restart=always
RestartSec=10

Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="DATABASE_URL=jdbc:mysql://localhost:3306/personal_finance"
Environment="JWT_SECRET=your-secret-from-env-or-secrets-manager"

[Install]
WantedBy=multi-user.target
```

### Option 2: Docker Deployment

```bash
# Build image
docker build -t personal-finance-app:1.0.0 .

# Run with docker-compose
docker-compose up -d

# Or run single container
docker run -d   --name personal-finance-app   -p 8080:8080   -e SPRING_PROFILES_ACTIVE=prod   -e DATABASE_URL=jdbc:mysql://db-host:3306/personal_finance   -e JWT_SECRET=your-secret   personal-finance-app:1.0.0
```

### Option 3: Cloud Deployment (Heroku Example)

**1. Create Procfile:**
```
web: java -Dserver.port=$PORT -jar target/personal-finance-app.jar
```

**2. Deploy:**
```bash
# Login to Heroku
heroku login

# Create app
heroku create personal-finance-app

# Add MySQL addon
heroku addons:create jawsdb:kitefin

# Set environment variables
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set JWT_SECRET=your-secret

# Deploy
git push heroku main

# Open app
heroku open
```

---

## Step 10: Production Troubleshooting

### Common Issues and Solutions

**1. Application Won't Start:**
```bash
# Check logs
tail -f /var/log/personal-finance-app/application.log

# Common causes:
# - Database connection failed (check credentials)
# - Port already in use (change port or kill process)
# - Missing environment variables
# - Flyway migration failed
```

**2. Database Connection Issues:**
```bash
# Test connection manually
mysql -h localhost -u appuser -p personal_finance

# Check Flyway status
mvn flyway:info

# Check connection pool
curl http://localhost:8080/actuator/health
```

**3. High Memory Usage:**
```bash
# Set JVM memory limits
java -Xms512m -Xmx1024m -jar app.jar

# Monitor with actuator
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

**4. Slow Performance:**
```bash
# Enable SQL logging temporarily
logging.level.org.hibernate.SQL=DEBUG

# Check slow queries
# Look for N+1 problems
# Add missing indexes
```

### Monitoring Tools

**1. Application Logs:**
```bash
# Tail logs
tail -f logs/application.log

# Search for errors
grep -i "error" logs/application.log

# Search for specific user
grep "userId: 123" logs/application-json.log
```

**2. Database Monitoring:**
```sql
-- Check slow queries
SHOW PROCESSLIST;

-- Check table sizes
SELECT 
    table_name,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'personal_finance'
ORDER BY (data_length + index_length) DESC;
```

**3. Health Checks:**
```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","components":{"db":{"status":"UP"}}}
```

---

## Production Checklist

Before deploying to production:

### Configuration
- [ ] Environment variables for all secrets
- [ ] Production profile configured
- [ ] Database credentials secured
- [ ] JWT secret is strong and secret
- [ ] HTTPS enabled
- [ ] CORS configured for frontend domain

### Database
- [ ] All Flyway migrations tested
- [ ] Database backups configured
- [ ] Indexes on frequently queried columns
- [ ] Connection pool configured
- [ ] Never use `ddl-auto: create` or `update`

### Security
- [ ] All endpoints require authentication (except login/register)
- [ ] Authorization rules enforced
- [ ] Rate limiting enabled
- [ ] Security headers configured
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention

### Logging
- [ ] Log levels appropriate for production
- [ ] No sensitive data logged
- [ ] Log rotation configured
- [ ] Centralized logging (optional but recommended)

### Monitoring
- [ ] Health checks enabled
- [ ] Metrics collection enabled
- [ ] Alerts configured for critical errors
- [ ] Uptime monitoring

### Performance
- [ ] Pagination for large datasets
- [ ] JOIN FETCH to prevent N+1
- [ ] Connection pooling configured
- [ ] HTTP compression enabled
- [ ] Static resources cached

### Testing
- [ ] All tests passing
- [ ] Integration tests with real database
- [ ] Load testing performed
- [ ] Security testing performed

### Documentation
- [ ] API documentation (Swagger) available
- [ ] README updated
- [ ] Deployment guide written
- [ ] Troubleshooting guide created

### Deployment
- [ ] Automated deployment pipeline
- [ ] Rollback strategy defined
- [ ] Database migration strategy
- [ ] Zero-downtime deployment (if needed)

---

## 🎉 Congratulations!

You've completed all 9 phases! Your Personal Finance App is now:

✅ **Production-Ready:**
- Configured for multiple environments
- Database migrations managed
- Comprehensive logging
- Full API documentation

✅ **Secure:**
- JWT authentication
- Role-based authorization
- Encrypted passwords
- Security headers

✅ **Well-Tested:**
- Unit tests
- Integration tests
- API tests
- 80%+ coverage

✅ **Performant:**
- Optimized queries
- Pagination
- Connection pooling
- Caching

✅ **Deployable:**
- Docker containerization
- Multiple deployment options
- Health checks
- Monitoring

✅ **Maintainable:**
- Clean architecture
- Well-documented
- Best practices followed
- Easy to troubleshoot

---

## What's Next?

### Continue Learning:
1. **Advanced Topics:**
   - Microservices architecture
   - Event-driven architecture
   - GraphQL API
   - WebSockets for real-time features

2. **Frontend Integration:**
   - Build Angular 20 frontend
   - Integrate with your API
   - State management (NgRx)
   - Responsive design

3. **Advanced Features:**
   - Email notifications
   - PDF report generation
   - Data export/import
   - Analytics dashboard
   - Mobile app (Flutter/React Native)

4. **DevOps:**
   - CI/CD with GitHub Actions
   - Kubernetes deployment
   - Blue-green deployments
   - Infrastructure as Code (Terraform)

### Build Your Portfolio:
- Deploy to production
- Write blog posts about what you learned
- Create video tutorials
- Contribute to open source
- Share on LinkedIn/GitHub

---

**You did it!** 🎊

You've built a production-ready enterprise application from scratch. This is a huge accomplishment and demonstrates real-world skills that employers value.

Keep coding, keep learning, and most importantly - keep building! 🚀
