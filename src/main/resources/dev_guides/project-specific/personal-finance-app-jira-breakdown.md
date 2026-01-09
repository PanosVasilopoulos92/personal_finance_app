# Personal Finance App - Jira Breakdown

**Project:** Personal Finance & Price Tracking Application  
**Tech Stack:** Java 25, Spring Boot 4, JPA/Hibernate 7, MySQL, Lombok, Angular 20  
**Learning Goal:** Master enterprise Java development with vertical slice architecture

---

## 📦 Epic 1: Core User Management System
**Duration:** 3-4 weeks  
**Description:** Complete user authentication, authorization, and profile management functionality

### Story 1.1: User Registration & Authentication
**As a** new user  
**I want to** register an account and log in securely  
**So that** I can access my personal finance data

**Acceptance Criteria:**
- User can register with email, password, first name, last name
- Email must be unique
- Password encrypted with BCrypt
- User receives JWT token on successful login
- Token expires after 24 hours
- Default USER role assigned on registration

**Tasks:**
- Create User entity with proper JPA annotations
- Create UserRole enum (USER, ADMIN)
- Create BaseEntity with audit fields (id, createdAt, updatedAt)
- Create UserRegistrationDto and UserLoginDto with validation
- Create UserResponse DTO (excludes password)
- Create UserRepository with custom queries (findByEmail, existsByEmail)
- Implement UserService with registration logic
- Add password encryption with PasswordEncoder bean
- Create AuthService for login/JWT generation
- Create JwtUtil for token generation and validation
- Create AuthController with /register and /login endpoints
- Write unit tests for UserService (Mockito)
- Write integration tests for AuthController

---

### Story 1.2: JWT Authentication & Security Configuration
**As a** logged-in user  
**I want** my requests to be authenticated via JWT  
**So that** my data remains secure

**Acceptance Criteria:**
- All API endpoints (except /auth/*) require JWT
- JWT contains userId, email, role, expiration
- Invalid/expired tokens return 401 Unauthorized
- CORS configured for Angular frontend

**Tasks:**
- Create JwtAuthenticationFilter (extract and validate JWT)
- Create SecurityConfig with filter chain
- Configure public endpoints (/auth/register, /auth/login)
- Configure protected endpoints (all others)
- Add CORS configuration for Angular localhost:4200
- Create UserDetailsService implementation
- Write tests for JwtUtil (token generation/validation)
- Write tests for JwtAuthenticationFilter

---

### Story 1.3: User Profile Management
**As a** logged-in user  
**I want to** view and update my profile  
**So that** I can keep my information current

**Acceptance Criteria:**
- User can view their own profile (not others')
- User can update first name, last name
- User cannot change email (business rule)
- Admin can view all users
- User can soft-delete account (active = false)

**Tasks:**
- Create UserUpdateDto with validation
- Add updateUser method to UserService
- Add authorization check (user can only update self)
- Create GET /api/users/me endpoint
- Create PUT /api/users/me endpoint
- Create GET /api/users (admin only) endpoint
- Create DELETE /api/users/me endpoint (soft delete)
- Add @PreAuthorize annotations for admin endpoints
- Write service tests for update/delete operations
- Write controller tests with MockMvc

---

### Story 1.4: User Preferences Management
**As a** logged-in user  
**I want to** set my currency and language preferences  
**So that** the app displays data in my preferred format

**Acceptance Criteria:**
- Each user has one UserPreferences entity
- Default currency: USD
- Default language: en
- User can update preferences
- Preferences created automatically on user registration

**Tasks:**
- Create UserPreferences entity (ONE-TO-ONE with User)
- Create UserPreferencesDto
- Update User entity with @OneToOne relationship
- Update UserService to create default preferences on registration
- Create UserPreferencesService
- Create PUT /api/users/me/preferences endpoint
- Create GET /api/users/me/preferences endpoint
- Write repository tests for preferences
- Write service tests for preferences CRUD

---

## 📦 Epic 2: Item & Category Management
**Duration:** 3-4 weeks  
**Description:** Manage items that users want to track prices for, organized by categories

### Story 2.1: Category Management
**As a** user  
**I want to** organize items into categories  
**So that** I can better track different types of products

**Acceptance Criteria:**
- User can create custom categories
- Category has name, description, optional icon
- User can only see/manage their own categories
- Categories can be archived (not deleted if items exist)

**Tasks:**
- Create Category entity with User relationship
- Create CategoryDto and CategoryResponse
- Create CategoryRepository with custom queries
- Create CategoryService with CRUD operations
- Create CategoryController with REST endpoints
- Add validation (unique category name per user)
- Write repository tests for category queries
- Write service tests for business logic
- Write controller integration tests

---

### Story 2.2: Item Creation & Management
**As a** user  
**I want to** add items I'm tracking  
**So that** I can monitor their prices over time

**Acceptance Criteria:**
- Item has name, description, category, priority, URL
- Items belong to a user
- Items can have optional target price
- Items can be marked as favorite
- User can only manage their own items

**Tasks:**
- Create Item entity with User and Category relationships
- Create ItemPriority enum (LOW, MEDIUM, HIGH, URGENT)
- Create ItemCreateDto with validation (@NotBlank, @Size, @URL)
- Create ItemUpdateDto and ItemResponse
- Create ItemRepository with query methods
- Implement ItemService with CRUD operations
- Add business rule: validate category belongs to user
- Create ItemController with REST endpoints (CRUD)
- Add pagination support for list endpoints
- Write comprehensive repository tests
- Write service tests with mocked repository
- Write controller tests with security context

---

### Story 2.3: Item Search & Filtering
**As a** user  
**I want to** search and filter my items  
**So that** I can quickly find what I'm looking for

**Acceptance Criteria:**
- Search by name (case-insensitive, partial match)
- Filter by category
- Filter by priority
- Filter by favorite status
- Results paginated (10 per page)
- Results sorted by name, priority, or creation date

**Tasks:**
- Add searchByNameAndUserId query to ItemRepository
- Add findByUserIdAndCategory query method
- Add findByUserIdAndPriority query method
- Add findByUserIdAndFavorite query method
- Create ItemSearchCriteria DTO
- Implement dynamic query building in service
- Add search endpoint to ItemController
- Add sorting and pagination parameters
- Write tests for all search/filter combinations

---

## 📦 Epic 3: Shopping List Management
**Duration:** 2-3 weeks  
**Description:** Create and manage shopping lists with items

### Story 3.1: Shopping List CRUD
**As a** user  
**I want to** create shopping lists  
**So that** I can organize items I plan to purchase

**Acceptance Criteria:**
- User can create multiple shopping lists
- List has name, optional description
- List can be archived
- User can only manage their own lists
- List can be marked as favorite

**Tasks:**
- Create ShoppingList entity with User relationship
- Create ShoppingListDto and response DTOs
- Create ShoppingListRepository
- Create ShoppingListService with CRUD
- Create ShoppingListController
- Add unique name validation per user
- Write repository, service, and controller tests

---

### Story 3.2: Add Items to Shopping List
**As a** user  
**I want to** add items to my shopping list  
**So that** I can track what I need to buy

**Acceptance Criteria:**
- Items can be added to multiple shopping lists
- Each item in list has quantity and unit
- Items can be marked as purchased
- User can only add their own items to their lists
- Item removal doesn't delete the item itself

**Tasks:**
- Create ShoppingListItem entity (join table)
- Add quantity and unit fields
- Add purchasedAt field (nullable)
- Create ShoppingListItemDto
- Update ShoppingListService to manage items
- Create endpoints: POST /api/lists/{id}/items
- Create endpoint: DELETE /api/lists/{id}/items/{itemId}
- Create endpoint: PUT /api/lists/{id}/items/{itemId}/purchase
- Add business validation (item and list belong to user)
- Write tests for all item operations

---

## 📦 Epic 4: Price Tracking System
**Duration:** 3-4 weeks  
**Description:** Track item prices over time and set up price alerts

### Story 4.1: Price History Tracking
**As a** user  
**I want to** track price changes for my items  
**So that** I can see price trends over time

**Acceptance Criteria:**
- User can manually add price entries for items
- Price entry has: price, store, date, URL
- User can view price history for an item
- Price history sorted by date (newest first)
- Price chart shows trend over time

**Tasks:**
- Create PriceHistory entity with Item relationship
- Create Store entity (name, location, website)
- Create PriceHistoryDto and response DTO
- Create PriceHistoryRepository with queries
- Create PriceHistoryService
- Create endpoints for adding/viewing price history
- Add query: findByItemIdOrderByDateDesc
- Add query: findByItemIdAndDateBetween (date range)
- Calculate price statistics (min, max, average)
- Write repository and service tests

---

### Story 4.2: Price Alerts & Comparisons
**As a** user  
**I want to** set price alerts for items  
**So that** I'm notified when prices drop below my target

**Acceptance Criteria:**
- User can set target price for an item
- System checks if current price ≤ target price
- User receives notification when alert triggered
- Alert can be dismissed or updated
- User can compare prices across different stores

**Tasks:**
- Create PriceAlert entity with Item relationship
- Create alertType enum (PRICE_DROP, TARGET_REACHED)
- Create PriceAlertDto and response DTO
- Create PriceAlertRepository
- Create PriceAlertService with alert logic
- Create notification logic (in-app for now)
- Create price comparison endpoint (group by store)
- Add endpoint: POST /api/items/{id}/alerts
- Add endpoint: GET /api/items/{id}/price-comparison
- Write tests for alert triggering logic

---

## 📦 Epic 5: Testing & Quality Assurance
**Duration:** Ongoing (2 weeks dedicated)  
**Description:** Comprehensive testing strategy for all features

### Story 5.1: Unit Test Coverage
**As a** developer  
**I want** comprehensive unit tests  
**So that** I can refactor with confidence

**Tasks:**
- Write unit tests for all service classes (Mockito)
- Achieve 80%+ code coverage
- Test all business logic edge cases
- Test exception handling paths
- Use @ExtendWith(MockitoExtension.class)
- Mock all repository dependencies
- Write parameterized tests for validation

---

### Story 5.2: Integration Test Suite
**As a** developer  
**I want** integration tests for API endpoints  
**So that** I verify the full request-response flow

**Tasks:**
- Write @SpringBootTest tests for controllers
- Use MockMvc for HTTP requests
- Test authentication flows with @WithMockUser
- Test authorization (access control)
- Verify DTOs serialize/deserialize correctly
- Test pagination and sorting
- Test error responses (400, 401, 403, 404)

---

### Story 5.3: Repository Tests
**As a** developer  
**I want** to test custom repository queries  
**So that** I ensure data access works correctly

**Tasks:**
- Write @DataJpaTest for all repositories
- Test custom query methods
- Test JOIN FETCH queries (no N+1)
- Test pagination and sorting
- Use TestEntityManager for test data
- Verify LazyInitializationException prevention

---

## 📦 Epic 6: Production-Ready Features
**Duration:** 2 weeks  
**Description:** Make the application production-ready

### Story 6.1: API Documentation with OpenAPI
**As a** frontend developer  
**I want** interactive API documentation  
**So that** I know how to consume the API

**Tasks:**
- Add springdoc-openapi dependency
- Add @Operation annotations to controllers
- Add @Schema annotations to DTOs
- Configure OpenAPI info (title, version, description)
- Add security scheme (JWT Bearer)
- Add example requests/responses
- Test Swagger UI at /swagger-ui.html

---

### Story 6.2: Logging & Monitoring
**As a** DevOps engineer  
**I want** structured logging  
**So that** I can monitor and debug the application

**Tasks:**
- Configure Logback with JSON format
- Add @Slf4j to all classes
- Log important business events (user registration, login)
- Log errors with stack traces
- Add request/response logging filter
- Configure log levels per environment
- Add actuator endpoints for health checks

---

### Story 6.3: Database Migrations
**As a** developer  
**I want** version-controlled database schema  
**So that** deployments are repeatable and safe

**Tasks:**
- Add Flyway dependency
- Create V1__initial_schema.sql migration
- Create V2__add_indexes.sql
- Configure Flyway in application.yml
- Test migrations on clean database
- Document rollback procedures

---

### Story 6.4: Exception Handling & Validation
**As a** user  
**I want** clear error messages  
**So that** I understand what went wrong

**Tasks:**
- Create @RestControllerAdvice global exception handler
- Create custom exceptions (ResourceNotFoundException, etc.)
- Create ErrorResponse DTO
- Handle validation errors (MethodArgumentNotValidException)
- Handle authentication errors (401, 403)
- Return consistent error structure
- Add error codes for frontend

---

## 📦 Epic 7: Angular Frontend (Optional)
**Duration:** 4-6 weeks  
**Description:** Build Angular frontend to consume the REST API

### Story 7.1: Authentication UI
**As a** user  
**I want** a registration and login interface  
**So that** I can access the application

**Tasks:**
- Create AuthModule with routing
- Create RegisterComponent with reactive forms
- Create LoginComponent with reactive forms
- Create AuthService for API calls
- Implement JWT storage in localStorage
- Create AuthGuard for protected routes
- Add HTTP interceptor for JWT header
- Add error handling and display

---

### Story 7.2: Item Management UI
**As a** user  
**I want** to manage my items through a web interface  
**So that** I can easily track prices

**Tasks:**
- Create ItemModule
- Create ItemListComponent with Material Table
- Create ItemFormComponent (create/edit)
- Create ItemService for API calls
- Implement pagination with MatPaginator
- Add search and filtering
- Create ItemDetailComponent
- Add Angular Material components

---

### Story 7.3: Shopping List UI
**As a** user  
**I want** to manage shopping lists  
**So that** I can organize my purchases

**Tasks:**
- Create ShoppingListModule
- Create list management components
- Create drag-and-drop for item reordering
- Implement add/remove items
- Mark items as purchased
- Add Material UI components

---

## ⏱️ Estimated Days Per Story

### Epic 1: Core User Management System
- **Story 1.1: User Registration & Authentication** - 5 days
- **Story 1.2: JWT Authentication & Security Configuration** - 4 days
- **Story 1.3: User Profile Management** - 3 days
- **Story 1.4: User Preferences Management** - 2 days

### Epic 2: Item & Category Management
- **Story 2.1: Category Management** - 3 days
- **Story 2.2: Item Creation & Management** - 4 days
- **Story 2.3: Item Search & Filtering** - 3 days

### Epic 3: Shopping List Management
- **Story 3.1: Shopping List CRUD** - 3 days
- **Story 3.2: Add Items to Shopping List** - 4 days

### Epic 4: Price Tracking System
- **Story 4.1: Price History Tracking** - 4 days
- **Story 4.2: Price Alerts & Comparisons** - 4 days

### Epic 5: Testing & Quality Assurance
- **Story 5.1: Unit Test Coverage** - 5 days
- **Story 5.2: Integration Test Suite** - 4 days
- **Story 5.3: Repository Tests** - 3 days

### Epic 6: Production-Ready Features
- **Story 6.1: API Documentation with OpenAPI** - 2 days
- **Story 6.2: Logging & Monitoring** - 2 days
- **Story 6.3: Database Migrations** - 2 days
- **Story 6.4: Exception Handling & Validation** - 3 days

### Epic 7: Angular Frontend (Optional)
- **Story 7.1: Authentication UI** - 5 days
- **Story 7.2: Item Management UI** - 6 days
- **Story 7.3: Shopping List UI** - 5 days

**Total Backend:** 60 days (~12 weeks)  
**Total Frontend:** 16 days (~3 weeks)  
**Grand Total:** 76 days (~15 weeks)

---

## 🎯 Recommended Implementation Order

### Phase 1: Foundation (Weeks 1-2)
1. Story 1.1: User Registration & Authentication - 5 days
2. Story 1.2: JWT Authentication & Security - 4 days

### Phase 2: Core Features (Weeks 3-4)
3. Story 2.1: Category Management - 3 days
4. Story 2.2: Item Creation & Management - 4 days

### Phase 3: Extended Features (Weeks 5-6)
5. Story 1.3: User Profile Management - 3 days
6. Story 2.3: Item Search & Filtering - 3 days

### Phase 4: Shopping Lists (Weeks 7-8)
7. Story 3.1: Shopping List CRUD - 3 days
8. Story 3.2: Add Items to Shopping List - 4 days

### Phase 5: Price Tracking (Weeks 9-10)
9. Story 4.1: Price History Tracking - 4 days
10. Story 4.2: Price Alerts & Comparisons - 4 days

### Phase 6: Quality & Production (Weeks 11-12)
11. Story 5.1: Unit Test Coverage - 5 days
12. Story 5.2: Integration Test Suite - 4 days
13. Story 6.1: API Documentation - 2 days
14. Story 6.4: Exception Handling - 3 days

### Phase 7: Frontend (Optional, Weeks 13-18)
15. Story 7.1: Authentication UI - 5 days
16. Story 7.2: Item Management UI - 6 days
17. Story 7.3: Shopping List UI - 5 days

---

## 📝 Notes for Learning

**Vertical Slice Approach:**
- Complete each Story fully before moving to the next
- Each Story delivers working, testable functionality
- You'll learn the full stack: Entity → DTO → Repository → Service → Controller → Tests

**Testing Strategy:**
- Write tests as you build (not after)
- Unit tests: Service layer with Mockito
- Integration tests: Controllers with MockMvc
- Repository tests: @DataJpaTest with TestEntityManager

**Best Practices:**
- Use Lombok (@Data, @RequiredArgsConstructor)
- Use records for DTOs when immutability is needed
- Always validate input with Bean Validation
- Use @Transactional(readOnly = true) for read operations
- Log important business events
- Handle exceptions gracefully

**Common Pitfalls to Avoid:**
- Don't return entities from controllers (use DTOs)
- Don't fetch lazy collections without JOIN FETCH
- Don't forget to test authorization
- Don't skip exception handling
- Don't hardcode values (use application.yml)
