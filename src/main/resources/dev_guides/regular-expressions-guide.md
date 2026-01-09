# Regular Expressions in Java: A Practical Guide

## Table of Contents
1. [What Are Regular Expressions?](#what-are-regular-expressions)
2. [Core Pattern Components](#core-pattern-components)
3. [Reading Patterns Step-by-Step](#reading-patterns-step-by-step)
4. [Common Patterns Explained](#common-patterns-explained)
5. [Java Implementation](#java-implementation)
6. [Validation Best Practices](#validation-best-practices)
7. [Real-World Examples](#real-world-examples)
8. [Common Pitfalls](#common-pitfalls)

---

## What Are Regular Expressions?

Regular expressions (regex) are patterns that describe text. Think of them as a specialized language for saying "I want text that looks like THIS."

**Why use them?**
- Validate user input (emails, phone numbers, passwords)
- Extract data from text
- Search and replace operations
- Parse structured data

---

## Core Pattern Components

### 1. Literal Characters
The simplest patterns - they match themselves exactly.

```
Pattern: cat
Matches: "cat", "catch", "category"
Doesn't match: "Car", "CAT" (unless case-insensitive)
```

### 2. Character Classes
Match ONE character from a set.

| Pattern | Meaning | Example |
|---------|---------|---------|
| `[abc]` | Match a, b, OR c | `[aeiou]` matches any vowel |
| `[a-z]` | Match any lowercase letter | `[a-zA-Z]` matches any letter |
| `[0-9]` | Match any digit | `[0-9]` same as `\d` |
| `[^abc]` | Match anything EXCEPT a, b, or c | `[^0-9]` matches non-digits |

### 3. Predefined Character Classes (Shortcuts)

| Pattern | Meaning | Equivalent To |
|---------|---------|---------------|
| `\d` | Any digit | `[0-9]` |
| `\D` | Any non-digit | `[^0-9]` |
| `\w` | Word character (letter, digit, underscore) | `[a-zA-Z0-9_]` |
| `\W` | Non-word character | `[^a-zA-Z0-9_]` |
| `\s` | Whitespace (space, tab, newline) | `[ \t\n\r]` |
| `\S` | Non-whitespace | `[^ \t\n\r]` |
| `.` | ANY character (except newline) | - |

### 4. Quantifiers
Specify HOW MANY times something should appear.

| Pattern | Meaning | Example |
|---------|---------|---------|
| `*` | 0 or more times | `a*` matches "", "a", "aaa" |
| `+` | 1 or more times | `a+` matches "a", "aaa" (not "") |
| `?` | 0 or 1 time (optional) | `colou?r` matches "color" or "colour" |
| `{n}` | Exactly n times | `\d{3}` matches exactly 3 digits |
| `{n,}` | n or more times | `\d{2,}` matches 2+ digits |
| `{n,m}` | Between n and m times | `\d{2,4}` matches 2, 3, or 4 digits |

### 5. Anchors
Specify WHERE in the string to match.

| Pattern | Meaning |
|---------|---------|
| `^` | Start of string |
| `$` | End of string |
| `\b` | Word boundary (between \w and \W) |
| `\B` | Non-word boundary |

### 6. Groups and Alternation

| Pattern | Meaning | Example |
|---------|---------|---------|
| `(abc)` | Group - treat as single unit | `(ha)+` matches "ha", "haha", "hahaha" |
| `a|b` | OR - match a OR b | `cat|dog` matches "cat" or "dog" |
| `(?:abc)` | Non-capturing group | Groups without saving |

---

## Reading Patterns Step-by-Step

Let's decode patterns piece by piece, like reading a sentence.

### Example 1: Email Pattern
```regex
^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$
```

**Let's break it down:**

1. `^` - Start at the beginning of the string
2. `[a-zA-Z0-9._-]+` - One or more of: letters, digits, dot, underscore, or hyphen
3. `@` - Literal @ symbol
4. `[a-zA-Z0-9.-]+` - One or more of: letters, digits, dot, or hyphen
5. `\.` - Literal dot (escaped because . means "any character")
6. `[a-zA-Z]{2,}` - Two or more letters (for .com, .org, .info, etc.)
7. `$` - Must reach the end of the string

**In plain English:** "Start with letters/numbers/special chars, then @, then domain name, then a dot, then at least 2 letters for the extension, and that's it."

### Example 2: Phone Number
```regex
^\d{3}-\d{3}-\d{4}$
```

**Breaking it down:**

1. `^` - Start
2. `\d{3}` - Exactly 3 digits
3. `-` - Literal hyphen
4. `\d{3}` - Exactly 3 digits
5. `-` - Literal hyphen
6. `\d{4}` - Exactly 4 digits
7. `$` - End

**Matches:** "555-123-4567"
**Doesn't match:** "5551234567" (no hyphens), "555-12-34567" (wrong grouping)

### Example 3: Password Requirements
```regex
^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$
```

**Breaking it down:**

1. `^` - Start
2. `(?=.*[a-z])` - Lookahead: must contain at least one lowercase letter
3. `(?=.*[A-Z])` - Lookahead: must contain at least one uppercase letter
4. `(?=.*\d)` - Lookahead: must contain at least one digit
5. `(?=.*[@$!%*?&])` - Lookahead: must contain at least one special character
6. `[A-Za-z\d@$!%*?&]{8,}` - Actually match 8 or more of these characters
7. `$` - End

**How lookaheads work:** They "peek ahead" without consuming characters. Think of them as requirements checks.

---

## Common Patterns Explained

### Username
```regex
^[a-zA-Z0-9_]{3,20}$
```
- Start to end
- Only letters, numbers, underscores
- Between 3 and 20 characters

### URL
```regex
^https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(/.*)?$
```
- `https?` - http or https (the s is optional)
- `://` - Literal characters
- `[a-zA-Z0-9.-]+` - Domain name
- `\.` - Literal dot
- `[a-zA-Z]{2,}` - Extension (.com, .org, etc.)
- `(/.*)?` - Optional path (everything after domain)

### Date (YYYY-MM-DD)
```regex
^\d{4}-\d{2}-\d{2}$
```
- 4 digits, hyphen, 2 digits, hyphen, 2 digits

### Hexadecimal Color
```regex
^#[0-9A-Fa-f]{6}$
```
- Starts with #
- Followed by exactly 6 hex digits (0-9, A-F, case insensitive)

---

## Java Implementation

### 1. Basic Pattern Matching

```java
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RegexBasics {
    
    public static void main(String[] args) {
        String text = "My email is john@example.com";
        String regex = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
        
        // Compile pattern (reusable for performance)
        Pattern pattern = Pattern.compile(regex);
        
        // Create matcher
        Matcher matcher = pattern.matcher(text);
        
        // Find match
        if (matcher.find()) {
            System.out.println("Found: " + matcher.group());
            // Output: Found: john@example.com
        }
    }
}
```

### 2. Validation Method

```java
public class InputValidator {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\d{3}-\\d{3}-\\d{4}$");
    
    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }
}
```

**Why compile patterns as constants?**
- Pattern compilation is expensive
- Reusing compiled patterns improves performance
- Thread-safe (Pattern objects are immutable)

### 3. Using String Methods (Simpler for Basic Cases)

```java
public class StringRegex {
    
    public static void main(String[] args) {
        String email = "test@example.com";
        
        // matches() - checks if ENTIRE string matches
        boolean valid = email.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        
        // replaceAll() - replace all matches
        String censored = "My number is 555-123-4567"
            .replaceAll("\\d{3}-\\d{3}-\\d{4}", "XXX-XXX-XXXX");
        
        // split() - split by pattern
        String[] words = "Hello,World;Java:Spring"
            .split("[,;:]");
        // Result: ["Hello", "World", "Java", "Spring"]
    }
}
```

---

## Validation Best Practices

### 1. Complete Validator Class with Spring Boot

```java
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;
import java.util.Optional;

@Component
public class UserInputValidator {
    
    // Pre-compiled patterns (better performance)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_]{3,20}$"
    );
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );
    
    /**
     * Validates email format
     * @param email The email to validate
     * @return ValidationResult with success/error details
     */
    public ValidationResult validateEmail(String email) {
        if (email == null || email.isBlank()) {
            return ValidationResult.error("Email cannot be empty");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.error("Invalid email format");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates username format
     */
    public ValidationResult validateUsername(String username) {
        if (username == null || username.isBlank()) {
            return ValidationResult.error("Username cannot be empty");
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidationResult.error(
                "Username must be 3-20 characters, letters, numbers, and underscores only"
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validates password strength
     */
    public ValidationResult validatePassword(String password) {
        if (password == null || password.isBlank()) {
            return ValidationResult.error("Password cannot be empty");
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return ValidationResult.error(
                "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"
            );
        }
        
        return ValidationResult.success();
    }
}

// Simple result record
record ValidationResult(boolean valid, Optional<String> errorMessage) {
    
    public static ValidationResult success() {
        return new ValidationResult(true, Optional.empty());
    }
    
    public static ValidationResult error(String message) {
        return new ValidationResult(false, Optional.of(message));
    }
}
```

### 2. Using Bean Validation (Jakarta/Hibernate Validator)

```java
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegistrationDTO {
    
    @NotBlank(message = "Username is required")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]{3,20}$",
        message = "Username must be 3-20 characters, alphanumeric and underscores only"
    )
    private String username;
    
    @NotBlank(message = "Email is required")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        message = "Invalid email format"
    )
    private String email;
    
    @NotBlank(message = "Phone is required")
    @Pattern(
        regexp = "^\\d{3}-\\d{3}-\\d{4}$",
        message = "Phone must be in format: XXX-XXX-XXXX"
    )
    private String phone;
}
```

### 3. Controller Usage

```java
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    
    private final UserInputValidator validator;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO dto) {
        // If we reach here, Bean Validation already passed
        
        // Additional custom validation if needed
        var emailValidation = validator.validateEmail(dto.getEmail());
        if (!emailValidation.valid()) {
            return ResponseEntity.badRequest()
                .body(emailValidation.errorMessage().orElse("Validation failed"));
        }
        
        // Process registration...
        return ResponseEntity.ok("Registration successful");
    }
}
```

---

## Real-World Examples

### Example 1: Sanitizing User Input

```java
public class InputSanitizer {
    
    // Remove all non-alphanumeric characters except spaces
    public static String sanitizeText(String input) {
        if (input == null) return "";
        return input.replaceAll("[^a-zA-Z0-9 ]", "");
    }
    
    // Extract all URLs from text
    public static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        Pattern pattern = Pattern.compile(
            "https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/[^\\s]*)?"
        );
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        
        return urls;
    }
    
    // Mask sensitive data
    public static String maskCreditCard(String text) {
        // Match credit card numbers (simple pattern)
        return text.replaceAll(
            "\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b",
            "XXXX-XXXX-XXXX-XXXX"
        );
    }
}
```

### Example 2: Parsing Structured Data

```java
public class LogParser {
    
    // Parse log entries: [2024-01-09 10:30:45] INFO: User logged in
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "\\[(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\] (\\w+): (.+)"
    );
    
    public record LogEntry(String timestamp, String level, String message) {}
    
    public static Optional<LogEntry> parseLogLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        
        if (matcher.matches()) {
            return Optional.of(new LogEntry(
                matcher.group(1), // timestamp
                matcher.group(2), // level
                matcher.group(3)  // message
            ));
        }
        
        return Optional.empty();
    }
}
```

### Example 3: Custom Validation Annotation

```java
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.*;

// Custom annotation
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
@Documented
public @interface StrongPassword {
    String message() default "Password does not meet strength requirements";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator implementation
public class StrongPasswordValidator 
        implements ConstraintValidator<StrongPassword, String> {
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}

// Usage
public class UserDTO {
    @StrongPassword
    private String password;
}
```

---

## Common Pitfalls

### 1. Escaping Special Characters

**Problem:** In Java strings, backslash is an escape character

```java
// WRONG - Single backslash
Pattern pattern = Pattern.compile("\d+");  // Compiler error!

// RIGHT - Double backslash
Pattern pattern = Pattern.compile("\\d+");

// For literal backslash in pattern
Pattern pattern = Pattern.compile("\\\\");  // Matches single \
```

### 2. Greedy vs Lazy Quantifiers

**Problem:** `*` and `+` are greedy - they match as much as possible

```java
String html = "<div>Hello</div><div>World</div>";

// Greedy: matches from first < to last >
String wrong = html.replaceAll("<.*>", "");  
// Result: "" (removes everything!)

// Lazy: matches shortest possible
String right = html.replaceAll("<.*?>", "");  
// Result: "HelloWorld" (removes only tags)
```

**Rule:** Add `?` after quantifiers for lazy matching: `*?`, `+?`, `??`, `{n,m}?`

### 3. Not Anchoring When Needed

```java
String pattern = "\\d{3}";  // Matches 3 digits

// Problem: This matches!
boolean result = "abc123456def".matches(pattern);  // false (correct)

// But find() will match
Matcher m = Pattern.compile(pattern).matcher("abc123456def");
m.find();  // true - finds "123"

// Solution: Use anchors for exact matching
Pattern exact = Pattern.compile("^\\d{3}$");
```

### 4. Case Sensitivity

```java
// Case sensitive (default)
boolean matches = "Hello".matches("hello");  // false

// Case insensitive flag
Pattern pattern = Pattern.compile("hello", Pattern.CASE_INSENSITIVE);
boolean matches2 = pattern.matcher("Hello").matches();  // true

// In-pattern flag
Pattern pattern2 = Pattern.compile("(?i)hello");  // Also case insensitive
```

### 5. Forgetting to Escape Metacharacters

```java
// Want to match "example.com" literally
String wrong = "example.com";  // . matches ANY character
// This matches: "exampleXcom"

String right = "example\\.com";  // Escaped dot matches literal dot
```

**Common metacharacters to escape:** `. * + ? ^ $ { } [ ] ( ) | \`

---

## Practice Exercises

### Exercise 1: Create a pattern for Canadian postal codes
Format: A1A 1A1 (letter-digit-letter space digit-letter-digit)

<details>
<summary>Solution</summary>

```java
Pattern postalCode = Pattern.compile("^[A-Z]\\d[A-Z] \\d[A-Z]\\d$");
```
</details>

### Exercise 2: Validate IPv4 addresses
Format: 192.168.1.1 (four numbers 0-255 separated by dots)

<details>
<summary>Solution</summary>

```java
Pattern ipv4 = Pattern.compile(
    "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
);
```
</details>

### Exercise 3: Extract hashtags from social media text
Example: "Love #Java and #SpringBoot!" → ["Java", "SpringBoot"]

<details>
<summary>Solution</summary>

```java
public static List<String> extractHashtags(String text) {
    List<String> hashtags = new ArrayList<>();
    Pattern pattern = Pattern.compile("#([a-zA-Z0-9_]+)");
    Matcher matcher = pattern.matcher(text);
    
    while (matcher.find()) {
        hashtags.add(matcher.group(1));  // Group 1 is without #
    }
    
    return hashtags;
}
```
</details>

---

## Quick Reference Card

```
BASICS
.       Any character except newline
\d      Digit [0-9]
\w      Word character [a-zA-Z0-9_]
\s      Whitespace

QUANTIFIERS
*       0 or more
+       1 or more
?       0 or 1 (optional)
{n}     Exactly n times
{n,m}   Between n and m times

ANCHORS
^       Start of string
$       End of string
\b      Word boundary

CHARACTER CLASSES
[abc]   a, b, or c
[^abc]  Not a, b, or c
[a-z]   Range a to z

GROUPS
(abc)   Capturing group
(?:abc) Non-capturing group
a|b     a or b

ESCAPING IN JAVA
\\      Literal backslash
\\.     Literal dot
\\d     Digit pattern
```

---

## Summary

**Key Takeaways:**

1. **Read patterns left to right** - Break them into chunks
2. **Anchors matter** - Use `^` and `$` for exact matching
3. **Escape in Java** - Double backslashes: `\\d`, `\\.`
4. **Compile patterns once** - Store as constants for performance
5. **Validate inputs** - Use regex for format validation
6. **Test your patterns** - Tools like regex101.com are invaluable

**Remember:** Regular expressions are powerful but can be complex. Start simple, test thoroughly, and add complexity only when needed. Readability matters - sometimes a simple `.contains()` or `.startsWith()` is better than a complex regex.

Happy pattern matching! 🎯
