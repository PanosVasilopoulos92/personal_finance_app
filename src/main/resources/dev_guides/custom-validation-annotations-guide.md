# Custom Validation Annotations Guide

## Overview

Custom validation annotations extend Java's Bean Validation (JSR 380) to create reusable, declarative validation logic beyond the standard annotations like `@NotNull`, `@Size`, etc.

## Anatomy of a Custom Validation Annotation

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MyCustomValidator.class)
public @interface MyCustomValidation {
    String message() default "Validation failed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

## The Three Required Meta-Annotations

### 1. @Target - Where Can This Annotation Be Applied?

Defines which Java elements can use your custom annotation.

**Available Values:**

| ElementType | Description | Example Use Case |
|-------------|-------------|------------------|
| `FIELD` | On class fields/record components | Validating individual properties |
| `METHOD` | On methods | Validating method return values |
| `PARAMETER` | On method/constructor parameters | Validating method inputs |
| `TYPE` | On classes, interfaces, records | Cross-field validation |
| `CONSTRUCTOR` | On constructors | Constructor parameter validation |
| `ANNOTATION_TYPE` | On other annotations | Composing meta-annotations |

**Common Combinations:**

```java
// Single field validation
@Target({ElementType.FIELD})

// Field or parameter validation
@Target({ElementType.FIELD, ElementType.PARAMETER})

// Cross-field validation (entire object)
@Target({ElementType.TYPE})

// Maximum flexibility
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
```

**Decision Guide:**
- Use `FIELD` when validating a single, independent field
- Use `TYPE` when validating relationships between multiple fields
- Use multiple targets for reusable validators across different contexts

---

### 2. @Retention - How Long Should This Annotation Live?

Defines how long the annotation information is retained.

**Available Values:**

| RetentionPolicy | Description | Use Case |
|-----------------|-------------|----------|
| `SOURCE` | Discarded by compiler | Compile-time checks only (e.g., `@Override`) |
| `CLASS` | Retained in `.class` file, not at runtime | Bytecode analysis tools |
| `RUNTIME` | Available at runtime via reflection | **Required for Bean Validation** |

**For Bean Validation, ALWAYS use:**

```java
@Retention(RetentionPolicy.RUNTIME)
```

**Why?** Spring and Bean Validation frameworks use reflection at runtime to discover and process your validation annotations. Without `RUNTIME` retention, your validator won't work.

---

### 3. @Constraint - Which Validator Class Implements This?

Links your annotation to the actual validation logic.

**Syntax:**

```java
@Constraint(validatedBy = YourValidatorClass.class)
```

**Multiple Validators (for different types):**

```java
@Constraint(validatedBy = {
    StringValidator.class,
    IntegerValidator.class
})
```

**The Validator Class Must:**

```java
public class YourValidatorClass 
        implements ConstraintValidator<YourAnnotation, TypeToValidate> {
    
    @Override
    public boolean isValid(TypeToValidate value, ConstraintValidatorContext context) {
        // Your validation logic
        return true; // or false
    }
}
```

---

## Required Annotation Methods

Every custom validation annotation MUST have these three methods:

```java
public @interface YourValidation {
    // Error message when validation fails
    String message() default "Default error message";
    
    // Validation groups (for conditional validation)
    Class<?>[] groups() default {};
    
    // Additional metadata (rarely used)
    Class<? extends Payload>[] payload() default {};
}
```

### Optional: Custom Parameters

You can add your own parameters:

```java
public @interface Size {
    String message() default "Size validation failed";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    // Custom parameters
    int min() default 0;
    int max() default Integer.MAX_VALUE;
}
```

---

## Complete Examples

### Example 1: Field-Level Validation (Phone Number)

**Annotation:**

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface ValidPhoneNumber {
    String message() default "Invalid phone number format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    // Custom parameter
    String region() default "US";
}
```

**Validator:**

```java
public class PhoneNumberValidator 
        implements ConstraintValidator<ValidPhoneNumber, String> {
    
    private String region;
    
    @Override
    public void initialize(ValidPhoneNumber annotation) {
        this.region = annotation.region();
    }
    
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null) {
            return true; // Use @NotNull separately for null checks
        }
        
        // US phone validation: (123) 456-7890 or 123-456-7890
        if ("US".equals(region)) {
            return phoneNumber.matches("^(\\(\\d{3}\\)|\\d{3})[- ]?\\d{3}[- ]?\\d{4}$");
        }
        
        return false;
    }
}
```

**Usage:**

```java
public record CreateUserRequest(
        @ValidPhoneNumber(region = "US")
        String phoneNumber
) {}
```

---

### Example 2: Class-Level Validation (Date Range)

**Annotation:**

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidDateRangeValidator.class)
@Documented
public @interface ValidDateRange {
    String message() default "End date must be after start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**Validator:**

```java
public class ValidDateRangeValidator 
        implements ConstraintValidator<ValidDateRange, EventRequest> {
    
    @Override
    public boolean isValid(EventRequest event, ConstraintValidatorContext context) {
        if (event == null) {
            return true;
        }
        
        LocalDate start = event.startDate();
        LocalDate end = event.endDate();
        
        if (start == null || end == null) {
            return true; // Validate with @NotNull separately
        }
        
        return end.isAfter(start);
    }
}
```

**Usage:**

```java
@ValidDateRange
public record EventRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {}
```

---

### Example 3: Password Matching (Your Use Case)

**Annotation:**

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch {
    String message() default "Passwords do not match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**Validator:**

```java
public class PasswordMatchValidator 
        implements ConstraintValidator<PasswordMatch, CreateUserRequest> {
    
    @Override
    public boolean isValid(CreateUserRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        
        String password = request.password();
        String confirmPassword = request.confirmPassword();
        
        if (password == null || confirmPassword == null) {
            return true;
        }
        
        return password.equals(confirmPassword);
    }
}
```

**Usage:**

```java
@PasswordMatch
public record CreateUserRequest(
        @NotBlank
        @Size(min = 8, max = 100)
        String password,
        
        @NotBlank
        String confirmPassword
) {}
```

---

## Decision Tree: Which @Target Should I Use?

```
Are you validating a single field independently?
│
├─ YES → Use @Target({ElementType.FIELD})
│         Example: @Email, @ValidUUID, @ValidPhoneNumber
│
└─ NO → Are you validating relationships between multiple fields?
         │
         └─ YES → Use @Target({ElementType.TYPE})
                   Example: @PasswordMatch, @ValidDateRange
```

---

## Common Pitfalls and Best Practices

### ✅ DO:
- Always use `@Retention(RetentionPolicy.RUNTIME)`
- Return `true` for `null` values (use `@NotNull` separately)
- Use field-level for single-field validation
- Use class-level for cross-field validation
- Add custom parameters to make validators reusable

### ❌ DON'T:
- Use `SOURCE` or `CLASS` retention (won't work at runtime)
- Validate `null` in your custom validator (unless that's the specific purpose)
- Mix field validation logic with cross-field validation
- Forget the three required methods: `message()`, `groups()`, `payload()`

---

## Integration with Spring

When using `@Valid` in Spring controllers:

```java
@PostMapping("/users")
public ResponseEntity<User> createUser(
        @Valid @RequestBody CreateUserRequest request) {
    // If validation fails, Spring throws MethodArgumentNotValidException
    // Custom validators are automatically triggered
}
```

**Validation Order:**
1. Jackson deserializes JSON → Record constructor runs
2. Spring sees `@Valid` annotation
3. Bean Validation framework processes all validation annotations
4. Field-level validators run first
5. Class-level validators run after
6. If any fail → `MethodArgumentNotValidException` thrown

---

## Quick Reference Table

| Aspect | Field-Level | Class-Level |
|--------|-------------|-------------|
| **@Target** | `ElementType.FIELD` | `ElementType.TYPE` |
| **Use When** | Single field validation | Multiple field relationships |
| **Validator Input** | Field value only | Entire object |
| **Reusability** | High (across classes) | Low (specific to class) |
| **Example** | `@ValidUUID`, `@Email` | `@PasswordMatch`, `@ValidDateRange` |

---

## Summary

Custom validation annotations provide a clean, declarative way to extend Bean Validation:

- **@Target**: Defines WHERE the annotation can be used
- **@Retention**: Must be `RUNTIME` for Bean Validation
- **@Constraint**: Links to the validator implementation class

Choose field-level for independent field validation, class-level for cross-field validation, and always follow the Bean Validation contract with the three required methods.
