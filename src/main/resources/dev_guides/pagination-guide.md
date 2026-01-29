# Pagination in Spring Boot: A Complete Guide

This guide covers pagination from first principles through advanced patterns, specifically for your Personal Finance & Price Tracking application.

---

## 1. Why Pagination Exists

### The Problem

Without pagination, a simple "get all items" request becomes dangerous:

```java
// ❌ This will eventually kill your application
List<Item> items = itemRepository.findAll();
```

**What happens with 100,000 items:**
1. Database loads all 100,000 rows into memory
2. JDBC driver transfers all data over the network
3. Hibernate creates 100,000 entity objects
4. Jackson serializes 100,000 objects to JSON
5. Network sends potentially megabytes of data to the client
6. Browser/mobile app tries to render 100,000 items

**Result:** OutOfMemoryError, timeouts, crashed browsers, angry users.

### The Solution

Pagination breaks large result sets into manageable "pages":

```
Page 0: Items 1-20
Page 1: Items 21-40
Page 2: Items 41-60
...
```

The database only fetches what's needed, and the client only renders what's visible.

---

## 2. How Database Pagination Works

Before diving into Spring, understand what happens at the SQL level.

### LIMIT/OFFSET (MySQL)

```sql
-- Get 20 items, skip the first 40 (page 2, 0-indexed)
SELECT * FROM items 
WHERE user_id = 1 
ORDER BY name ASC
LIMIT 20 OFFSET 40;
```

- `LIMIT 20` → return at most 20 rows
- `OFFSET 40` → skip the first 40 rows

### The COUNT Query

To show "Page 3 of 15" or "Showing 41-60 of 287 items", you need the total count:

```sql
SELECT COUNT(*) FROM items WHERE user_id = 1;
```

**Important:** Spring Data JPA runs BOTH queries when you request a `Page<T>`:
1. The data query (with LIMIT/OFFSET)
2. The count query (for total elements)

This matters for performance—we'll cover optimization later.

---

## 3. Spring Data JPA Pagination Fundamentals

### Core Interfaces

```
┌─────────────────────────────────────────────────────────────┐
│                         Pageable                            │
│  (Request: "Give me page 2, size 20, sorted by name")      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                          Page<T>                            │
│  (Response: Content + metadata about total pages/elements)  │
└─────────────────────────────────────────────────────────────┘
```

### Pageable: The Request Object

`Pageable` describes WHAT you want:

```java
// Create manually
Pageable pageable = PageRequest.of(
    0,                              // page number (0-indexed!)
    20,                             // page size
    Sort.by("name").ascending()     // sorting
);

// What this contains:
pageable.getPageNumber();  // 0
pageable.getPageSize();    // 20
pageable.getOffset();      // 0 (calculated: pageNumber * pageSize)
pageable.getSort();        // Sort by name ASC
```

### Page<T>: The Response Object

`Page<T>` contains your data PLUS metadata:

```java
Page<Item> page = itemRepository.findByUserId(userId, pageable);

// The actual data
List<Item> items = page.getContent();

// Pagination metadata
page.getNumber();          // Current page (0)
page.getSize();            // Page size (20)
page.getNumberOfElements(); // Items on THIS page (might be < size on last page)
page.getTotalElements();   // Total items across ALL pages (287)
page.getTotalPages();      // Calculated: ceil(287/20) = 15

// Navigation helpers
page.isFirst();            // true if page 0
page.isLast();             // true if last page
page.hasNext();            // true if more pages exist
page.hasPrevious();        // true if not first page
```

---

## 4. Implementation: The Full Vertical Slice

Let's implement pagination for Items in your project.

### 4.1 Repository Layer

Spring Data JPA makes this trivial—just change the return type:

```java
package com.yourname.personalfinance.repository;

import com.yourname.personalfinance.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    
    /**
     * Basic paginated query using derived method.
     * Spring automatically adds LIMIT/OFFSET based on Pageable.
     */
    Page<Item> findByUserIdAndArchivedFalse(Long userId, Pageable pageable);
    
    /**
     * Paginated query with multiple conditions.
     */
    Page<Item> findByUserIdAndCategoryIdAndArchivedFalse(
        Long userId, 
        Long categoryId, 
        Pageable pageable
    );
    
    /**
     * Custom JPQL with pagination.
     * Note: Pageable works with @Query too!
     */
    @Query("""
        SELECT i FROM Item i
        WHERE i.user.id = :userId
        AND i.archived = false
        AND (
            LOWER(i.name) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(i.description) LIKE LOWER(CONCAT('%', :term, '%'))
        )
        """)
    Page<Item> searchByTerm(
        @Param("userId") Long userId, 
        @Param("term") String term, 
        Pageable pageable
    );
}
```

**What Spring generates for `findByUserIdAndArchivedFalse`:**

```sql
-- Data query
SELECT i.* FROM items i 
WHERE i.user_id = ? AND i.archived = false 
ORDER BY [sort columns] 
LIMIT ? OFFSET ?;

-- Count query (automatic)
SELECT COUNT(i.id) FROM items i 
WHERE i.user_id = ? AND i.archived = false;
```

### 4.2 Service Layer

The service transforms entities to DTOs and handles business logic:

```java
package com.yourname.personalfinance.service;

import com.yourname.personalfinance.dto.response.ItemResponse;
import com.yourname.personalfinance.entity.Item;
import com.yourname.personalfinance.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {
    
    private final ItemRepository itemRepository;
    
    /**
     * Gets paginated items for a user.
     * 
     * <p>The Page.map() method transforms each Item entity to an ItemResponse DTO
     * while preserving all pagination metadata (total elements, page info, etc.).</p>
     *
     * @param userId the owner's ID
     * @param pageable pagination parameters
     * @return page of item DTOs
     */
    @Transactional(readOnly = true)
    public Page<ItemResponse> getItems(Long userId, Pageable pageable) {
        log.debug("Fetching items for user {} with pageable {}", userId, pageable);
        
        return itemRepository.findByUserIdAndArchivedFalse(userId, pageable)
            .map(ItemResponse::from);  // Transform each entity to DTO
    }
    
    /**
     * Gets paginated items filtered by category.
     */
    @Transactional(readOnly = true)
    public Page<ItemResponse> getItemsByCategory(
            Long userId, 
            Long categoryId, 
            Pageable pageable) {
        
        return itemRepository.findByUserIdAndCategoryIdAndArchivedFalse(
                userId, categoryId, pageable)
            .map(ItemResponse::from);
    }
    
    /**
     * Searches items with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ItemResponse> searchItems(
            Long userId, 
            String searchTerm, 
            Pageable pageable) {
        
        if (searchTerm == null || searchTerm.isBlank()) {
            return getItems(userId, pageable);
        }
        
        return itemRepository.searchByTerm(userId, searchTerm.trim(), pageable)
            .map(ItemResponse::from);
    }
}
```

**Key insight:** `Page.map()` preserves all pagination metadata while transforming content. You get a `Page<ItemResponse>` with the same `totalElements`, `totalPages`, etc.

### 4.3 Controller Layer

The controller receives pagination parameters and returns the paginated response:

```java
package com.yourname.personalfinance.controller;

import com.yourname.personalfinance.dto.response.ItemResponse;
import com.yourname.personalfinance.security.UserDetailsImpl;
import com.yourname.personalfinance.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {
    
    private final ItemService itemService;
    
    /**
     * Gets paginated items for the authenticated user.
     * 
     * <p>Example requests:</p>
     * <ul>
     *   <li>GET /api/items - First page, default size (20), sorted by name</li>
     *   <li>GET /api/items?page=2&size=10 - Third page, 10 items</li>
     *   <li>GET /api/items?sort=createdAt,desc - Sorted by creation date descending</li>
     *   <li>GET /api/items?sort=priority,desc&sort=name,asc - Multi-field sort</li>
     * </ul>
     *
     * @param userDetails authenticated user
     * @param pageable pagination parameters (auto-resolved from query params)
     * @return paginated items
     */
    @GetMapping
    public ResponseEntity<Page<ItemResponse>> getItems(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) 
            Pageable pageable) {
        
        Long userId = userDetails.getUser().getId();
        log.info("GET /api/items - user: {}, page: {}, size: {}", 
            userId, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<ItemResponse> items = itemService.getItems(userId, pageable);
        return ResponseEntity.ok(items);
    }
    
    /**
     * Gets items filtered by category with pagination.
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ItemResponse>> getItemsByCategory(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        Long userId = userDetails.getUser().getId();
        Page<ItemResponse> items = itemService.getItemsByCategory(
            userId, categoryId, pageable);
        return ResponseEntity.ok(items);
    }
    
    /**
     * Searches items with pagination.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ItemResponse>> searchItems(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        
        Long userId = userDetails.getUser().getId();
        Page<ItemResponse> items = itemService.searchItems(userId, q, pageable);
        return ResponseEntity.ok(items);
    }
}
```

### Understanding @PageableDefault

This annotation sets defaults when the client doesn't specify parameters:

```java
@PageableDefault(
    size = 20,                      // Default page size
    sort = "name",                  // Default sort field
    direction = Sort.Direction.ASC  // Default sort direction
)
Pageable pageable
```

The client can override ANY of these via query parameters.

### How Spring Resolves Pageable

Spring Boot auto-configures `PageableHandlerMethodArgumentResolver` which parses query params:

| Query Parameter | Meaning | Example |
|-----------------|---------|---------|
| `page` | Page number (0-indexed) | `?page=2` → third page |
| `size` | Items per page | `?size=50` → 50 items |
| `sort` | Sort field and direction | `?sort=name,asc` |

**Multi-field sorting:**
```
GET /api/items?sort=priority,desc&sort=name,asc
```
This sorts by priority descending, then by name ascending for items with the same priority.

---

## 5. JSON Response Structure

When you return `Page<ItemResponse>`, Jackson serializes it to:

```json
{
    "content": [
        {
            "id": 1,
            "name": "MacBook Pro",
            "categoryId": 5,
            "categoryName": "Electronics",
            "targetPrice": 1999.00,
            "currentPrice": 2199.00
        },
        {
            "id": 2,
            "name": "AirPods Pro",
            "categoryId": 5,
            "categoryName": "Electronics",
            "targetPrice": 199.00,
            "currentPrice": 249.00
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 20,
        "sort": {
            "sorted": true,
            "empty": false,
            "unsorted": false
        },
        "offset": 0,
        "paged": true,
        "unpaged": false
    },
    "totalElements": 87,
    "totalPages": 5,
    "last": false,
    "first": true,
    "size": 20,
    "number": 0,
    "sort": {
        "sorted": true,
        "empty": false,
        "unsorted": false
    },
    "numberOfElements": 20,
    "empty": false
}
```

### Key Fields for Your Frontend

| Field | Use In Frontend |
|-------|-----------------|
| `content` | The actual items to display |
| `totalElements` | "Showing X of **87** items" |
| `totalPages` | Page selector: "1 2 3 4 5" |
| `number` | Current page highlight |
| `first` / `last` | Disable prev/next buttons |
| `empty` | Show "No items found" message |

---

## 6. Configuration and Limits

### Application Properties

```yaml
# application.yml
spring:
  data:
    web:
      pageable:
        default-page-size: 20       # Default if not specified
        max-page-size: 100          # IMPORTANT: Prevent abuse
        one-indexed-parameters: false  # page=0 is first (default)
        page-parameter: page        # Query param name
        size-parameter: size        # Query param name
```

### Why max-page-size Matters

Without a limit, a malicious or careless client could request:

```
GET /api/items?size=1000000
```

This defeats the purpose of pagination! Always set `max-page-size`.

### Customizing Parameter Names

If your frontend expects different param names:

```yaml
spring:
  data:
    web:
      pageable:
        page-parameter: pageNumber   # ?pageNumber=0
        size-parameter: pageSize     # ?pageSize=20
```

---

## 7. Page vs Slice: Choosing the Right Return Type

### Page<T>: When You Need Totals

```java
Page<Item> findByUserId(Long userId, Pageable pageable);
```

- Runs a COUNT query
- Provides `totalElements` and `totalPages`
- Use when: Client needs to show "Page 3 of 15" or total count

### Slice<T>: When You Only Need "Has More?"

```java
Slice<Item> findByUserId(Long userId, Pageable pageable);
```

- NO COUNT query (faster!)
- Only knows if there's a next page
- Use when: Infinite scroll, "Load more" button

```java
Slice<Item> slice = itemRepository.findByUserId(userId, pageable);

slice.getContent();      // The items
slice.hasNext();         // Is there another page?
slice.hasPrevious();     // Is there a previous page?
// slice.getTotalElements() - NOT AVAILABLE!
```

**Performance comparison with 1 million items:**

| Approach | Time |
|----------|------|
| `Page<T>` (with COUNT) | ~500ms |
| `Slice<T>` (no COUNT) | ~50ms |

### When to Use Which

```
┌─────────────────────────────────────────────────────────────┐
│  Need "Page X of Y" display?                                │
│  Need total count?                                          │
│  Traditional pagination with page numbers?                  │
│                                                             │
│  YES → Use Page<T>                                          │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  Infinite scroll UI?                                        │
│  "Load more" button?                                        │
│  Large dataset where COUNT is expensive?                    │
│                                                             │
│  YES → Use Slice<T>                                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. Common Pitfalls and Solutions

### Pitfall 1: Pagination with JOIN FETCH

This causes a Hibernate warning and incorrect results:

```java
// ❌ BAD - Will fail or produce wrong results
@Query("SELECT i FROM Item i LEFT JOIN FETCH i.category")
Page<Item> findAllWithCategories(Pageable pageable);
```

**Why:** Hibernate can't apply LIMIT/OFFSET to the joined result set correctly. It fetches everything and paginates in memory!

**Solution: Two-query approach**

```java
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    
    // Query 1: Get paginated IDs only
    @Query("SELECT i.id FROM Item i WHERE i.user.id = :userId AND i.archived = false")
    Page<Long> findIdsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // Query 2: Fetch full entities with relationships for those IDs
    @Query("""
        SELECT i FROM Item i 
        LEFT JOIN FETCH i.category 
        WHERE i.id IN :ids
        """)
    List<Item> findByIdsWithCategory(@Param("ids") List<Long> ids);
}
```

```java
// Service implementation
@Transactional(readOnly = true)
public Page<ItemResponse> getItemsWithCategories(Long userId, Pageable pageable) {
    // Step 1: Get paginated IDs (with correct LIMIT/OFFSET)
    Page<Long> idPage = itemRepository.findIdsByUserId(userId, pageable);
    
    if (idPage.isEmpty()) {
        return Page.empty(pageable);
    }
    
    // Step 2: Fetch entities with JOIN FETCH for those IDs
    List<Item> items = itemRepository.findByIdsWithCategory(idPage.getContent());
    
    // Step 3: Maintain the original sort order
    Map<Long, Item> itemMap = items.stream()
        .collect(Collectors.toMap(Item::getId, Function.identity()));
    
    List<Item> orderedItems = idPage.getContent().stream()
        .map(itemMap::get)
        .filter(Objects::nonNull)
        .toList();
    
    // Step 4: Wrap in PageImpl with original page metadata
    return new PageImpl<>(
        orderedItems.stream().map(ItemResponse::from).toList(),
        pageable,
        idPage.getTotalElements()
    );
}
```

### Pitfall 2: Expensive COUNT Queries

For complex queries, the auto-generated COUNT can be slow:

```java
// ❌ Spring generates a complex COUNT with all the joins
@Query("""
    SELECT i FROM Item i
    LEFT JOIN FETCH i.category c
    LEFT JOIN FETCH i.priceEntries pe
    WHERE i.user.id = :userId
    AND (c.name LIKE %:term% OR i.name LIKE %:term%)
    """)
Page<Item> complexSearch(Long userId, String term, Pageable pageable);
```

**Solution: Custom COUNT query**

```java
@Query(
    value = """
        SELECT i FROM Item i
        LEFT JOIN i.category c
        WHERE i.user.id = :userId
        AND (c.name LIKE %:term% OR i.name LIKE %:term%)
        """,
    countQuery = """
        SELECT COUNT(i.id) FROM Item i
        LEFT JOIN i.category c
        WHERE i.user.id = :userId
        AND (c.name LIKE %:term% OR i.name LIKE %:term%)
        """
)
Page<Item> complexSearch(Long userId, String term, Pageable pageable);
```

The `countQuery` can be simpler (no FETCH, fewer joins if possible).

### Pitfall 3: Sorting on Non-Indexed Columns

```
GET /api/items?sort=description,asc
```

If `description` isn't indexed, sorting 100,000 rows is slow.

**Solutions:**
1. Add indexes for commonly sorted columns
2. Restrict sortable fields in your controller:

```java
@GetMapping
public ResponseEntity<Page<ItemResponse>> getItems(
        @AuthenticationPrincipal UserDetailsImpl userDetails,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir) {
    
    // Whitelist allowed sort fields
    Set<String> allowedSortFields = Set.of("name", "createdAt", "priority", "targetPrice");
    
    if (!allowedSortFields.contains(sortBy)) {
        sortBy = "name";  // Fall back to default
    }
    
    Sort sort = sortDir.equalsIgnoreCase("desc") 
        ? Sort.by(sortBy).descending() 
        : Sort.by(sortBy).ascending();
    
    Pageable pageable = PageRequest.of(page, size, sort);
    
    return ResponseEntity.ok(itemService.getItems(
        userDetails.getUser().getId(), pageable));
}
```

### Pitfall 4: Deep Pagination Performance

```
GET /api/items?page=5000&size=20
```

At page 5000, the database must skip 100,000 rows before returning 20. This is inherently slow with OFFSET.

**Solutions:**

1. **Keyset/Cursor pagination** (best for large datasets):

```java
// Instead of OFFSET, use WHERE with the last seen value
@Query("""
    SELECT i FROM Item i
    WHERE i.user.id = :userId
    AND i.id > :lastSeenId
    ORDER BY i.id ASC
    """)
List<Item> findNextPage(Long userId, Long lastSeenId, Pageable pageable);
```

2. **Limit maximum page depth:**

```java
@GetMapping
public ResponseEntity<?> getItems(
        @RequestParam(defaultValue = "0") int page,
        @PageableDefault(size = 20) Pageable pageable) {
    
    if (page > 500) {  // 10,000 items max
        return ResponseEntity.badRequest()
            .body("Maximum page number exceeded. Use search to narrow results.");
    }
    // ...
}
```

---

## 9. Testing Pagination

### Unit Testing the Service

```java
package com.yourname.personalfinance.service;

import com.yourname.personalfinance.dto.response.ItemResponse;
import com.yourname.personalfinance.entity.Item;
import com.yourname.personalfinance.entity.User;
import com.yourname.personalfinance.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServicePaginationTest {
    
    @Mock
    private ItemRepository itemRepository;
    
    @InjectMocks
    private ItemService itemService;
    
    private User testUser;
    private List<Item> testItems;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("testuser").build();
        
        testItems = List.of(
            Item.builder().id(1L).name("Item A").user(testUser).archived(false).build(),
            Item.builder().id(2L).name("Item B").user(testUser).archived(false).build(),
            Item.builder().id(3L).name("Item C").user(testUser).archived(false).build()
        );
    }
    
    @Test
    @DisplayName("getItems should return paginated results with correct metadata")
    void getItems_ReturnsPaginatedResults() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2, Sort.by("name").ascending());
        Page<Item> mockPage = new PageImpl<>(
            testItems.subList(0, 2),  // First 2 items
            pageable,
            3  // Total elements
        );
        
        when(itemRepository.findByUserIdAndArchivedFalse(eq(1L), any(Pageable.class)))
            .thenReturn(mockPage);
        
        // Act
        Page<ItemResponse> result = itemService.getItems(1L, pageable);
        
        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
        assertThat(result.hasNext()).isTrue();
        
        // Verify DTO transformation
        assertThat(result.getContent().get(0).name()).isEqualTo("Item A");
        assertThat(result.getContent().get(1).name()).isEqualTo("Item B");
        
        verify(itemRepository).findByUserIdAndArchivedFalse(1L, pageable);
    }
    
    @Test
    @DisplayName("getItems should return empty page when no items exist")
    void getItems_WhenNoItems_ReturnsEmptyPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Item> emptyPage = Page.empty(pageable);
        
        when(itemRepository.findByUserIdAndArchivedFalse(eq(1L), any(Pageable.class)))
            .thenReturn(emptyPage);
        
        // Act
        Page<ItemResponse> result = itemService.getItems(1L, pageable);
        
        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.isEmpty()).isTrue();
    }
    
    @Test
    @DisplayName("getItems should pass sort parameters to repository")
    void getItems_WithSorting_PassesSortToRepository() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        when(itemRepository.findByUserIdAndArchivedFalse(eq(1L), any(Pageable.class)))
            .thenReturn(Page.empty(pageable));
        
        // Act
        itemService.getItems(1L, pageable);
        
        // Assert - verify the exact pageable was passed
        verify(itemRepository).findByUserIdAndArchivedFalse(1L, pageable);
    }
}
```

### Integration Testing with @DataJpaTest

```java
package com.yourname.personalfinance.repository;

import com.yourname.personalfinance.entity.Item;
import com.yourname.personalfinance.entity.User;
import com.yourname.personalfinance.entity.enums.UserRolesEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ItemRepositoryPaginationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ItemRepository itemRepository;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password")
            .firstName("Test")
            .lastName("User")
            .userRole(UserRolesEnum.USER)
            .active(true)
            .build();
        entityManager.persist(testUser);
        
        // Create 25 test items
        for (int i = 1; i <= 25; i++) {
            Item item = Item.builder()
                .name("Item " + String.format("%02d", i))
                .user(testUser)
                .archived(false)
                .build();
            entityManager.persist(item);
        }
        entityManager.flush();
    }
    
    @Test
    @DisplayName("Pagination should return correct page of results")
    void findByUserId_WithPagination_ReturnsCorrectPage() {
        // Act - Get second page of 10 items
        Page<Item> page = itemRepository.findByUserIdAndArchivedFalse(
            testUser.getId(),
            PageRequest.of(1, 10, Sort.by("name").ascending())
        );
        
        // Assert
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(1);
        
        // Verify correct items (11-20 due to sorting)
        assertThat(page.getContent().get(0).getName()).isEqualTo("Item 11");
        assertThat(page.getContent().get(9).getName()).isEqualTo("Item 20");
    }
    
    @Test
    @DisplayName("Last page should have fewer elements")
    void findByUserId_LastPage_HasFewerElements() {
        // Act - Get third (last) page of 10 items (only 5 remaining)
        Page<Item> page = itemRepository.findByUserIdAndArchivedFalse(
            testUser.getId(),
            PageRequest.of(2, 10, Sort.by("name").ascending())
        );
        
        // Assert
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getNumberOfElements()).isEqualTo(5);
        assertThat(page.isLast()).isTrue();
        assertThat(page.hasNext()).isFalse();
    }
    
    @Test
    @DisplayName("Sorting should be applied correctly")
    void findByUserId_WithDescendingSort_ReturnsSortedResults() {
        // Act
        Page<Item> page = itemRepository.findByUserIdAndArchivedFalse(
            testUser.getId(),
            PageRequest.of(0, 5, Sort.by("name").descending())
        );
        
        // Assert - Should get Item 25, 24, 23, 22, 21
        assertThat(page.getContent().get(0).getName()).isEqualTo("Item 25");
        assertThat(page.getContent().get(4).getName()).isEqualTo("Item 21");
    }
    
    @Test
    @DisplayName("Page beyond results should return empty")
    void findByUserId_PageBeyondResults_ReturnsEmpty() {
        // Act
        Page<Item> page = itemRepository.findByUserIdAndArchivedFalse(
            testUser.getId(),
            PageRequest.of(100, 10)  // Way beyond our 25 items
        );
        
        // Assert
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(25);  // Total still known
        assertThat(page.getNumber()).isEqualTo(100);
    }
}
```

### Controller Integration Test

```java
package com.yourname.personalfinance.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerPaginationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @WithMockUser(username = "testuser")
    void getItems_WithPaginationParams_ReturnsPagedResponse() throws Exception {
        mockMvc.perform(get("/api/items")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "name,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.pageable.pageNumber").value(0))
            .andExpect(jsonPath("$.pageable.pageSize").value(10))
            .andExpect(jsonPath("$.totalElements").exists())
            .andExpect(jsonPath("$.totalPages").exists());
    }
    
    @Test
    @WithMockUser(username = "testuser")
    void getItems_WithDefaultParams_UsesDefaults() throws Exception {
        mockMvc.perform(get("/api/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageable.pageSize").value(20))  // Default
            .andExpect(jsonPath("$.pageable.pageNumber").value(0));
    }
}
```

---

## 10. Frontend Integration (Angular)

Here's how your Angular frontend would consume paginated endpoints:

### Service

```typescript
// item.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface Item {
  id: number;
  name: string;
  categoryName: string;
  targetPrice: number;
  currentPrice: number;
}

@Injectable({ providedIn: 'root' })
export class ItemService {
  private readonly apiUrl = '/api/items';

  constructor(private http: HttpClient) {}

  getItems(
    page: number = 0,
    size: number = 20,
    sortField: string = 'name',
    sortDir: 'asc' | 'desc' = 'asc'
  ): Observable<Page<Item>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', `${sortField},${sortDir}`);

    return this.http.get<Page<Item>>(this.apiUrl, { params });
  }
  
  searchItems(
    query: string,
    page: number = 0,
    size: number = 20
  ): Observable<Page<Item>> {
    const params = new HttpParams()
      .set('q', query)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<Item>>(`${this.apiUrl}/search`, { params });
  }
}
```

### Component with Angular Material Paginator

```typescript
// item-list.component.ts
import { Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort, Sort } from '@angular/material/sort';
import { ItemService, Item, Page } from './item.service';

@Component({
  selector: 'app-item-list',
  template: `
    <mat-table [dataSource]="items" matSort (matSortChange)="onSortChange($event)">
      <ng-container matColumnDef="name">
        <mat-header-cell *matHeaderCellDef mat-sort-header>Name</mat-header-cell>
        <mat-cell *matCellDef="let item">{{ item.name }}</mat-cell>
      </ng-container>
      
      <ng-container matColumnDef="categoryName">
        <mat-header-cell *matHeaderCellDef mat-sort-header>Category</mat-header-cell>
        <mat-cell *matCellDef="let item">{{ item.categoryName }}</mat-cell>
      </ng-container>
      
      <ng-container matColumnDef="currentPrice">
        <mat-header-cell *matHeaderCellDef mat-sort-header>Price</mat-header-cell>
        <mat-cell *matCellDef="let item">{{ item.currentPrice | currency }}</mat-cell>
      </ng-container>
      
      <mat-header-row *matHeaderRowDef="displayedColumns"></mat-header-row>
      <mat-row *matRowDef="let row; columns: displayedColumns"></mat-row>
    </mat-table>
    
    <mat-paginator
      [length]="totalElements"
      [pageSize]="pageSize"
      [pageIndex]="currentPage"
      [pageSizeOptions]="[10, 20, 50]"
      (page)="onPageChange($event)"
      showFirstLastButtons>
    </mat-paginator>
  `
})
export class ItemListComponent implements OnInit {
  items: Item[] = [];
  displayedColumns = ['name', 'categoryName', 'currentPrice'];
  
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  sortField = 'name';
  sortDir: 'asc' | 'desc' = 'asc';

  constructor(private itemService: ItemService) {}

  ngOnInit(): void {
    this.loadItems();
  }

  loadItems(): void {
    this.itemService.getItems(
      this.currentPage,
      this.pageSize,
      this.sortField,
      this.sortDir
    ).subscribe(page => {
      this.items = page.content;
      this.totalElements = page.totalElements;
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadItems();
  }

  onSortChange(sort: Sort): void {
    this.sortField = sort.active;
    this.sortDir = sort.direction as 'asc' | 'desc' || 'asc';
    this.currentPage = 0;  // Reset to first page when sorting changes
    this.loadItems();
  }
}
```

---

## 11. Quick Reference

### Creating Pageable Objects

```java
// Basic
PageRequest.of(0, 20);

// With single sort
PageRequest.of(0, 20, Sort.by("name").ascending());

// With multiple sorts
PageRequest.of(0, 20, Sort.by(
    Sort.Order.desc("priority"),
    Sort.Order.asc("name")
));

// Unpaged (return all - use carefully!)
Pageable.unpaged();
```

### Return Types Comparison

| Return Type | COUNT Query | Use Case |
|-------------|-------------|----------|
| `Page<T>` | Yes | Traditional pagination with page numbers |
| `Slice<T>` | No | Infinite scroll, "Load more" |
| `List<T>` | No | When you need all results (careful!) |

### Controller Annotations

```java
// With defaults
@PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
Pageable pageable

// Restrict max size
@PageableDefault(size = 20)  // Combined with spring.data.web.pageable.max-page-size
```

### Query Parameter Cheat Sheet

```
?page=0                     # First page
?size=50                    # 50 items per page
?sort=name,asc              # Sort by name ascending
?sort=name,desc             # Sort by name descending
?sort=priority,desc&sort=name,asc  # Multi-field sort
```

---

## 12. Summary: When to Use What

| Scenario | Approach |
|----------|----------|
| Standard list view with page numbers | `Page<T>` with `@PageableDefault` |
| Mobile infinite scroll | `Slice<T>` |
| Need relationships loaded | Two-query approach (IDs first) |
| Complex filters + pagination | Specification + Pageable |
| Very large datasets (millions) | Cursor-based pagination |
| COUNT is too slow | Custom `countQuery` or `Slice<T>` |

---

*Guide created for Personal Finance & Price Tracking project - Spring Boot 4 / Spring Data JPA*
