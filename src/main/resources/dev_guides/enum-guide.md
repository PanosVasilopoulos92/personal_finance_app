# Java Enums in Spring Boot: Patterns & Best Practices

> **Stack:** Java 25, Spring Boot 4, Hibernate 7  
> **Purpose:** Definitive reference for creating and using enums in enterprise applications

---

## Why Enums Matter

Enums replace magic strings and integers with type-safe, self-documenting constants. Instead of `status = 1` or `role = "admin"`, you get compile-time safety and IDE autocomplete.

```java
// ❌ Magic strings - no compile-time safety
if (user.getRole().equals("ADMIN")) { ... }

// ✅ Type-safe enum
if (user.getRole() == UserRole.ADMIN) { ... }
```

---

## 1. Basic Enum Definition

The simplest form — just constants with no additional data.

```java
package com.yourapp.model.enums;

/**
 * User roles for authorization.
 */
public enum UserRole {
    USER,
    ADMIN,
    MODERATOR
}
```

**When to use:** Simple status codes, categories, or types where the name itself is sufficient.

---

## 2. Enums with Fields and Behavior

Most real-world enums carry additional data. Java enums are full classes — they can have fields, constructors, and methods.

```java
package com.yourapp.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Item priority levels with display information.
 */
@Getter
@RequiredArgsConstructor
public enum ItemPriority {
    LOW(1, "Low Priority", "#4CAF50"),
    MEDIUM(2, "Medium Priority", "#FF9800"),
    HIGH(3, "High Priority", "#F44336"),
    URGENT(4, "Urgent", "#9C27B0");

    private final int level;
    private final String displayName;
    private final String colorCode;

    /**
     * Check if this priority is higher than another.
     */
    public boolean isHigherThan(ItemPriority other) {
        return this.level > other.level;
    }
}
```

**Usage:**
```java
ItemPriority priority = ItemPriority.HIGH;
String label = priority.getDisplayName();  // "High Priority"
boolean urgent = priority.isHigherThan(ItemPriority.MEDIUM);  // true
```

---

## 3. Enums with Lookup Methods

A common pattern: look up an enum by a code, value, or external identifier.

```java
package com.yourapp.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Store types with external system codes.
 */
@Getter
@RequiredArgsConstructor
public enum StoreType {
    SUPERMARKET("SM", "Supermarket"),
    PHARMACY("PH", "Pharmacy"),
    ELECTRONICS("EL", "Electronics Store"),
    ONLINE("ON", "Online Retailer"),
    WHOLESALE("WH", "Wholesale Club");

    private final String code;
    private final String displayName;

    // Cache for O(1) lookup by code
    private static final Map<String, StoreType> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toMap(StoreType::getCode, Function.identity()));

    /**
     * Find StoreType by its code.
     *
     * @param code the external code
     * @return the matching StoreType
     * @throws IllegalArgumentException if code is unknown
     */
    public static StoreType fromCode(String code) {
        StoreType type = BY_CODE.get(code);
        if (type == null) {
            throw new IllegalArgumentException("Unknown store type code: " + code);
        }
        return type;
    }

    /**
     * Safely find StoreType by code, returning null if not found.
     */
    public static StoreType fromCodeOrNull(String code) {
        return BY_CODE.get(code);
    }
}
```

**Why the static Map?** Calling `values()` in a loop is O(n). The cached map gives O(1) lookups — important when parsing large datasets.

---

## 4. Enums Implementing Interfaces

Enums can implement interfaces, enabling polymorphic behavior.

```java
package com.yourapp.model.enums;

/**
 * Contract for enums that have a display representation.
 */
public interface Displayable {
    String getDisplayName();
    String getCode();
}
```

```java
package com.yourapp.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Shopping list status with workflow behavior.
 */
@Getter
@RequiredArgsConstructor
public enum ShoppingListStatus implements Displayable {
    DRAFT("D", "Draft", false),
    ACTIVE("A", "Active", false),
    COMPLETED("C", "Completed", true),
    ARCHIVED("X", "Archived", true);

    private final String code;
    private final String displayName;
    private final boolean terminal;

    /**
     * Check if transition to another status is allowed.
     */
    public boolean canTransitionTo(ShoppingListStatus target) {
        if (this.terminal) {
            return false;  // Terminal states can't transition
        }
        return switch (this) {
            case DRAFT -> target == ACTIVE || target == ARCHIVED;
            case ACTIVE -> target == COMPLETED || target == ARCHIVED;
            default -> false;
        };
    }
}
```

**Usage in service:**
```java
public void updateStatus(ShoppingList list, ShoppingListStatus newStatus) {
    if (!list.getStatus().canTransitionTo(newStatus)) {
        throw new InvalidStateTransitionException(
            "Cannot transition from %s to %s".formatted(list.getStatus(), newStatus)
        );
    }
    list.setStatus(newStatus);
}
```

---

## 5. JPA/Hibernate Mapping

### The Golden Rule: Always Use `EnumType.STRING`

```java
@Entity
@Table(name = "items")
public class Item extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private ItemPriority priority = ItemPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_unit", length = 30)
    private ItemUnit itemUnit;
}
```

### Why STRING Over ORDINAL?

| Aspect | `EnumType.STRING` | `EnumType.ORDINAL` |
|--------|-------------------|-------------------|
| Database value | `"HIGH"` | `2` |
| Add new enum at end | ✅ Safe | ✅ Safe |
| Add new enum in middle | ✅ Safe | ❌ **Breaks existing data** |
| Reorder enums | ✅ Safe | ❌ **Breaks existing data** |
| Rename enum | ⚠️ Migration needed | ✅ Safe |
| Database readability | ✅ Human-readable | ❌ Meaningless numbers |

**Example of ORDINAL disaster:**
```java
// Original
public enum Status { PENDING, APPROVED, REJECTED }
// PENDING=0, APPROVED=1, REJECTED=2

// After adding REVIEW
public enum Status { PENDING, REVIEW, APPROVED, REJECTED }
// PENDING=0, REVIEW=1, APPROVED=2, REJECTED=3
// Now all APPROVED (1) records become REVIEW!
```

### Column Sizing

Set `length` to accommodate your longest enum name plus buffer for future values:
```java
@Column(length = 30)  // "SUPERMARKET" = 11 chars, leave room
private StoreType storeType;
```

---

## 6. JSON Serialization (REST APIs)

### Default Behavior

By default, Jackson serializes enums as their name:
```json
{ "priority": "HIGH" }
```

### Custom JSON Value with `@JsonValue`

When your API contract differs from internal names:

```java
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CREDIT_CARD("credit_card", "Credit Card"),
    DEBIT_CARD("debit_card", "Debit Card"),
    BANK_TRANSFER("bank_transfer", "Bank Transfer"),
    CASH("cash", "Cash");

    @JsonValue  // Serialize as this value
    private final String jsonValue;
    private final String displayName;

    // Deserialize from JSON
    @JsonCreator
    public static PaymentMethod fromJson(String value) {
        return Arrays.stream(values())
                .filter(pm -> pm.jsonValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Unknown payment method: " + value
                ));
    }
}
```

**JSON output:**
```json
{ "paymentMethod": "credit_card" }
```

### Full Control with `@JsonFormat`

```java
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@Getter
@RequiredArgsConstructor
public enum ItemPriority {
    LOW(1, "Low Priority"),
    HIGH(3, "High Priority");

    private final int level;
    private final String displayName;
}
```

**JSON output:**
```json
{ "priority": { "level": 1, "displayName": "Low Priority" } }
```

---

## 7. Validation in DTOs

### Validate Enum Values in Requests

```java
public record CreateItemRequest(
    @NotBlank(message = "Name is required")
    String name,

    @NotNull(message = "Priority is required")
    ItemPriority priority,  // Jackson validates during deserialization

    String storeTypeCode  // When receiving external codes
) {}
```

### Custom Validator for Enum Codes

When receiving string codes that map to enums:

```java
package com.yourapp.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEnumCodeValidator.class)
@Documented
public @interface ValidEnumCode {
    String message() default "Invalid value";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    Class<? extends Enum<?>> enumClass();
    String method() default "fromCode";  // Lookup method name
}
```

```java
package com.yourapp.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Method;

public class ValidEnumCodeValidator implements ConstraintValidator<ValidEnumCode, String> {

    private Class<? extends Enum<?>> enumClass;
    private String methodName;

    @Override
    public void initialize(ValidEnumCode annotation) {
        this.enumClass = annotation.enumClass();
        this.methodName = annotation.method();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;  // Let @NotNull handle null checks
        }
        try {
            Method method = enumClass.getMethod(methodName, String.class);
            method.invoke(null, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

**Usage:**
```java
public record CreateStoreRequest(
    @NotBlank String name,

    @NotBlank
    @ValidEnumCode(enumClass = StoreType.class, message = "Invalid store type")
    String storeTypeCode
) {}
```

---

## 8. Repository Queries with Enums

```java
public interface ItemRepository extends JpaRepository<Item, Long> {

    // Find by single enum value
    List<Item> findByPriority(ItemPriority priority);

    // Find by multiple enum values
    List<Item> findByPriorityIn(Collection<ItemPriority> priorities);

    // JPQL with enum
    @Query("SELECT i FROM Item i WHERE i.user.id = :userId AND i.priority = :priority")
    List<Item> findByUserIdAndPriority(
        @Param("userId") Long userId,
        @Param("priority") ItemPriority priority
    );

    // Count by enum
    long countByPriority(ItemPriority priority);

    // Enum in complex queries
    @Query("""
        SELECT i FROM Item i
        WHERE i.user.id = :userId
        AND i.priority IN :priorities
        AND i.archived = false
        ORDER BY i.priority DESC
        """)
    List<Item> findActiveByUserAndPriorities(
        @Param("userId") Long userId,
        @Param("priorities") List<ItemPriority> priorities
    );
}
```

---

## 9. Exposing Enum Values via API

Frontend applications often need the list of valid enum values.

### Enum Metadata DTO

```java
public record EnumValueResponse(
    String value,       // Enum name for API calls
    String displayName, // Human-readable label
    String code         // Optional external code
) {
    public static EnumValueResponse from(Displayable enumValue) {
        return new EnumValueResponse(
            ((Enum<?>) enumValue).name(),
            enumValue.getDisplayName(),
            enumValue.getCode()
        );
    }
}
```

### Enum Controller

```java
package com.yourapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/enums")
@RequiredArgsConstructor
public class EnumController {

    /**
     * Get all available item priorities.
     */
    @GetMapping("/item-priorities")
    public ResponseEntity<List<EnumValueResponse>> getItemPriorities() {
        List<EnumValueResponse> priorities = Arrays.stream(ItemPriority.values())
                .map(p -> new EnumValueResponse(p.name(), p.getDisplayName(), null))
                .toList();
        return ResponseEntity.ok(priorities);
    }

    /**
     * Get all available store types.
     */
    @GetMapping("/store-types")
    public ResponseEntity<List<EnumValueResponse>> getStoreTypes() {
        List<EnumValueResponse> types = Arrays.stream(StoreType.values())
                .map(EnumValueResponse::from)
                .toList();
        return ResponseEntity.ok(types);
    }

    /**
     * Get multiple enum types in one call (reduces HTTP requests).
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, List<EnumValueResponse>>> getAllEnums() {
        return ResponseEntity.ok(Map.of(
            "itemPriorities", Arrays.stream(ItemPriority.values())
                    .map(p -> new EnumValueResponse(p.name(), p.getDisplayName(), null))
                    .toList(),
            "storeTypes", Arrays.stream(StoreType.values())
                    .map(EnumValueResponse::from)
                    .toList(),
            "shoppingListStatuses", Arrays.stream(ShoppingListStatus.values())
                    .map(EnumValueResponse::from)
                    .toList()
        ));
    }
}
```

**Response:**
```json
{
  "itemPriorities": [
    { "value": "LOW", "displayName": "Low Priority", "code": null },
    { "value": "MEDIUM", "displayName": "Medium Priority", "code": null },
    { "value": "HIGH", "displayName": "High Priority", "code": null }
  ],
  "storeTypes": [
    { "value": "SUPERMARKET", "displayName": "Supermarket", "code": "SM" },
    { "value": "PHARMACY", "displayName": "Pharmacy", "code": "PH" }
  ]
}
```

---

## 10. Common Patterns Summary

### Pattern: Enum Package Structure

```
src/main/java/com/yourapp/
├── model/
│   ├── enums/
│   │   ├── ItemPriority.java
│   │   ├── ItemUnit.java
│   │   ├── ShoppingListStatus.java
│   │   ├── StoreType.java
│   │   ├── UserRole.java
│   │   └── Displayable.java  (interface)
│   ├── Item.java
│   └── Store.java
```

### Pattern: Basic Enum Template

```java
package com.yourapp.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [Description of what this enum represents].
 */
@Getter
@RequiredArgsConstructor
public enum YourEnum {
    VALUE_ONE("code1", "Display One"),
    VALUE_TWO("code2", "Display Two");

    private final String code;
    private final String displayName;
}
```

### Pattern: Enum with Lookup

```java
@Getter
@RequiredArgsConstructor
public enum YourEnum {
    VALUE_ONE("code1"),
    VALUE_TWO("code2");

    private final String code;

    private static final Map<String, YourEnum> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toMap(YourEnum::getCode, Function.identity()));

    public static YourEnum fromCode(String code) {
        YourEnum result = BY_CODE.get(code);
        if (result == null) {
            throw new IllegalArgumentException("Unknown code: " + code);
        }
        return result;
    }
}
```

---

## 11. Anti-Patterns to Avoid

### ❌ Using ORDINAL in JPA

```java
// NEVER DO THIS
@Enumerated(EnumType.ORDINAL)
private ItemPriority priority;
```

### ❌ Mutable Enum Fields

```java
// NEVER DO THIS - enums should be immutable
public enum BadEnum {
    VALUE;
    private String data;
    public void setData(String data) { this.data = data; }  // ❌
}
```

### ❌ Enum with Too Many Responsibilities

```java
// TOO MUCH - enum doing database queries
public enum ProductType {
    FOOD {
        @Override
        public List<Product> findProducts(ProductRepository repo) {
            return repo.findByType(this);  // ❌ Don't inject dependencies into enums
        }
    }
}
```

### ❌ Large Enums That Should Be Database Tables

If your enum has:
- More than ~20 values
- Values that change frequently
- Values users need to configure

**→ Use a database table instead.**

---

## 12. Testing Enums

```java
package com.yourapp.model.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class StoreTypeTest {

    @Test
    void fromCode_withValidCode_returnsCorrectEnum() {
        assertThat(StoreType.fromCode("SM")).isEqualTo(StoreType.SUPERMARKET);
        assertThat(StoreType.fromCode("PH")).isEqualTo(StoreType.PHARMACY);
    }

    @Test
    void fromCode_withInvalidCode_throwsException() {
        assertThatThrownBy(() -> StoreType.fromCode("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown store type code");
    }

    @ParameterizedTest
    @EnumSource(StoreType.class)
    void allEnumValues_haveNonNullFields(StoreType type) {
        assertThat(type.getCode()).isNotBlank();
        assertThat(type.getDisplayName()).isNotBlank();
    }

    @ParameterizedTest
    @CsvSource({
        "SM, SUPERMARKET",
        "PH, PHARMACY",
        "EL, ELECTRONICS"
    })
    void fromCode_coversAllMappings(String code, StoreType expected) {
        assertThat(StoreType.fromCode(code)).isEqualTo(expected);
    }
}
```

```java
class ShoppingListStatusTest {

    @ParameterizedTest
    @CsvSource({
        "DRAFT, ACTIVE, true",
        "DRAFT, ARCHIVED, true",
        "ACTIVE, COMPLETED, true",
        "COMPLETED, ACTIVE, false",
        "ARCHIVED, DRAFT, false"
    })
    void canTransitionTo_validatesCorrectly(
            ShoppingListStatus from,
            ShoppingListStatus to,
            boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }
}
```

---

## Quick Reference Card

| Scenario | Pattern |
|----------|---------|
| Simple status/type | Basic enum (just constants) |
| Need display labels | Enum with `displayName` field |
| External system codes | Enum with `code` + `fromCode()` lookup |
| State machine | Enum implementing behavior interface |
| JPA persistence | `@Enumerated(EnumType.STRING)` always |
| REST serialization | `@JsonValue` for custom JSON |
| Frontend dropdowns | Enum endpoint returning metadata |
| Frequent changes | Database table, not enum |
