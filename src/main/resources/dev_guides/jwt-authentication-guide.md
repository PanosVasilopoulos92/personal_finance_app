# JWT Authentication in Spring Boot - Deep Dive Guide

## Overview: The Big Picture

Think of JWT authentication like a concert venue security system:
1. **Entry Gate (Login)**: You show your ID, get a wristband (JWT token)
2. **Security Guards (Filters)**: Check your wristband at every door
3. **Access Areas (Endpoints)**: Different wristbands allow access to different areas

**The Four Pillars of Your Security Setup:**
1. **SecurityConfig** - The master blueprint (defines all security rules)
2. **JwtAuthenticationFilter** - The security guard (validates tokens on every request)
3. **UserDetailsServiceImpl** - The database lookup service (finds users)
4. **UserDetailsImpl** - The adapter (converts your User to Spring Security format)

---

## Component 1: SecurityConfig.java

This is your **security blueprint** - it defines the rules for your entire application.

### What It Does

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
```

**Think of this as**: Setting up the security office for your building.

- `@Configuration`: Tells Spring "this class contains setup instructions"
- `@EnableWebSecurity`: Activates Spring Security for your app
- `@EnableMethodSecurity`: Allows you to use `@PreAuthorize` annotations on methods (like `@PreAuthorize("hasRole('ADMIN')")`)

### The Security Filter Chain

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
```

**Think of this as**: Creating the actual security checkpoint process.

#### 1. Disable CSRF

```java
.csrf(AbstractHttpConfigurer::disable)
```

**What**: CSRF (Cross-Site Request Forgery) protection is disabled.

**Why**: With JWT tokens, you don't need CSRF protection because:
- JWTs are sent in headers, not cookies
- Attackers can't access headers from other domains
- Each request carries its own authentication proof

**When to keep CSRF enabled**: If you were using session-based authentication with cookies.

#### 2. Enable CORS (Cross-Origin Resource Sharing)

```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

**What is CORS?**

CORS is a security feature built into browsers. Let me explain why it exists:

**The Problem Without CORS:**
Imagine you're logged into your bank at `bank.com`. Without CORS, a malicious website `evil.com` could make requests to `bank.com` from your browser (because you're already logged in), potentially transferring your money without your knowledge.

**The Browser's Protection:**
Browsers implement the "Same-Origin Policy" - by default, JavaScript on `evil.com` cannot make requests to `bank.com`. An "origin" is defined by:
- Protocol (http vs https)
- Domain (example.com)
- Port (8080, 4200, etc.)

**Your Situation:**
- Frontend: `http://localhost:4200` (Angular)
- Backend: `http://localhost:8080` (Spring Boot)
- These are **different origins** (different ports)
- Without CORS configuration, your Angular app cannot talk to your Spring Boot API

**The CORS Flow (Preflight Request):**

For certain requests (like POST with custom headers), the browser sends a "preflight" request first:

```
Browser -> Backend: OPTIONS /api/transactions
Headers:
  Origin: http://localhost:4200
  Access-Control-Request-Method: POST
  Access-Control-Request-Headers: Authorization

Backend -> Browser: 200 OK
Headers:
  Access-Control-Allow-Origin: http://localhost:4200
  Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
  Access-Control-Allow-Headers: *
  Access-Control-Allow-Credentials: true

// Only if preflight succeeds:
Browser -> Backend: POST /api/transactions
Headers:
  Authorization: Bearer token...
```

---

### Deep Dive: CORS Configuration Method

```java
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    // ... configuration ...
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfiguration);
    return source;
}
```

Let's break down each class and method:

#### CorsConfiguration

**What it is**: A Plain Old Java Object (POJO) that holds CORS settings.

**Think of it as**: A settings form you're filling out.

```java
CorsConfiguration corsConfiguration = new CorsConfiguration();
```

This creates an empty configuration object. Now let's fill it in:

#### 1. setAllowedOrigins()

```java
corsConfiguration.setAllowedOrigins(List.of("http://localhost:4200"));
```

**What**: Specifies which origins (domains) can access your API.

**Breaking it down**:
- `http://localhost:4200` - Your Angular dev server
- You can add multiple: `List.of("http://localhost:4200", "https://myapp.com")`
- Use `List.of("*")` to allow ALL origins (⚠️ **DANGEROUS in production!**)

**Why it's important**: 
- Prevents random websites from accessing your API
- In production, you'd set this to your actual domain: `https://myfinanceapp.com`

**Common mistake**: Forgetting to update this when deploying to production!

#### 2. setAllowedMethods()

```java
corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
```

**What**: Specifies which HTTP methods are allowed from the specified origins.

**Breaking it down**:
- `GET` - Reading data
- `POST` - Creating new data
- `PUT` - Updating existing data
- `DELETE` - Deleting data
- `OPTIONS` - Preflight requests (browser automatically sends these)

**Why OPTIONS is critical**: 
Browsers send OPTIONS requests before "complex" requests (requests with custom headers like Authorization). If you don't allow OPTIONS, JWT authentication won't work!

**Example of what happens**:
```
// Your Angular code:
http.post('/api/transactions', data, {
  headers: { Authorization: 'Bearer token...' }
})

// Browser automatically sends first:
OPTIONS /api/transactions

// Only if OPTIONS succeeds, then:
POST /api/transactions
```

#### 3. setAllowedHeaders()

```java
corsConfiguration.setAllowedHeaders(List.of("*"));
```

**What**: Specifies which HTTP headers can be sent in requests.

**Why `*` (all headers)**:
- Your Angular app needs to send `Authorization: Bearer token...`
- You might add custom headers like `X-Request-ID`
- Using `*` is convenient in development

**Production best practice**: Be more specific for security:
```java
corsConfiguration.setAllowedHeaders(Arrays.asList(
    "Authorization",
    "Content-Type",
    "Accept",
    "X-Requested-With"
));
```

#### 4. setAllowCredentials()

```java
corsConfiguration.setAllowCredentials(true);
```

**What**: Allows sending credentials (cookies, authorization headers) with cross-origin requests.

**Why you need it**: 
- To send the `Authorization` header with JWT token
- To send cookies if you were using session-based auth

**Critical security note**: 
When `setAllowCredentials(true)`, you **cannot** use `*` for `setAllowedOrigins()`. You must specify exact origins.

**Why this restriction exists**:
```java
// ❌ This will cause an error:
corsConfiguration.setAllowedOrigins(List.of("*"));
corsConfiguration.setAllowCredentials(true);

// ✅ This works:
corsConfiguration.setAllowedOrigins(List.of("http://localhost:4200"));
corsConfiguration.setAllowCredentials(true);
```

**Reason**: If any website could access your API with credentials, it would defeat the purpose of CORS security.

---

#### UrlBasedCorsConfigurationSource

**What it is**: A class that maps URL patterns to CORS configurations.

**Why it exists**: You might want different CORS rules for different endpoints.

```java
UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
source.registerCorsConfiguration("/**", corsConfiguration);
```

**Breaking it down**:
- `UrlBasedCorsConfigurationSource` - The container that holds mapping rules
- `registerCorsConfiguration(pattern, config)` - Registers a configuration for a URL pattern
- `"/**"` - Ant-style pattern meaning "all paths" (/ = root, ** = any subdirectories)

**Example with multiple patterns**:
```java
UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

// Public API - more permissive
CorsConfiguration publicConfig = new CorsConfiguration();
publicConfig.setAllowedOrigins(List.of("*"));
publicConfig.setAllowedMethods(List.of("GET"));
source.registerCorsConfiguration("/api/public/**", publicConfig);

// Private API - restrictive
CorsConfiguration privateConfig = new CorsConfiguration();
privateConfig.setAllowedOrigins(List.of("http://localhost:4200"));
privateConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
privateConfig.setAllowCredentials(true);
source.registerCorsConfiguration("/api/private/**", privateConfig);

return source;
```

**CorsConfigurationSource interface**: 
- `UrlBasedCorsConfigurationSource` implements `CorsConfigurationSource`
- Spring Security expects a `CorsConfigurationSource`
- This allows for flexibility in how CORS is configured

#### 3. Stateless Session Management - The Heart of JWT

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

This is arguably the **most critical concept** in JWT authentication. Let's understand it deeply.

---

### What is Session Management?

**Traditional Session-Based Authentication (How It Used to Work):**

```
1. User logs in with username/password
   ↓
2. Server validates credentials
   ↓
3. Server creates a session object in memory
   Session: {
     sessionId: "abc123",
     userId: 42,
     username: "john",
     loginTime: "2025-01-18T10:00:00",
     roles: ["USER"]
   }
   ↓
4. Server sends session ID to browser in a cookie
   Set-Cookie: JSESSIONID=abc123
   ↓
5. Browser stores cookie and sends it with every request
   Cookie: JSESSIONID=abc123
   ↓
6. Server looks up session in memory using session ID
   ↓
7. Server retrieves user info from session
```

**The Session Object in Memory:**
```java
// Conceptually, server maintains a map:
Map<String, HttpSession> sessions = {
    "abc123": { userId: 42, username: "john", ... },
    "def456": { userId: 17, username: "jane", ... },
    // ... thousands of sessions in server memory
}
```

**Problems with Sessions:**
1. **Memory overhead**: Server must store every user's session
2. **Scalability issues**: 
   - Load balancer sends User A to Server 1 (session stored there)
   - Next request goes to Server 2 (session doesn't exist there!)
   - Solution: "Sticky sessions" or shared session storage (complex!)
3. **Stateful**: Server must maintain state
4. **Not ideal for microservices**: Each service would need session access

---

### JWT Approach: STATELESS

```java
session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
```

**What this means**: Tell Spring Security "don't create or use HTTP sessions."

**How JWT works instead:**

```
1. User logs in with username/password
   ↓
2. Server validates credentials
   ↓
3. Server creates a JWT token containing user info
   Token: {
     header: { alg: "HS256", typ: "JWT" },
     payload: { 
       email: "john@example.com",
       userId: 42,
       role: "USER",
       exp: 1705849200  // expiration timestamp
     },
     signature: "encrypted_hash"
   }
   ↓
4. Server sends token to client (NO SESSION CREATED)
   { "token": "eyJhbGciOiJIUz..." }
   ↓
5. Client stores token (localStorage/sessionStorage)
   ↓
6. Client sends token with every request
   Authorization: Bearer eyJhbGciOiJIUz...
   ↓
7. Server validates token signature and extracts user info
   (NO DATABASE/MEMORY LOOKUP FOR SESSION)
```

**Key difference**: 
- **Session**: Server stores user info, client holds only session ID
- **JWT**: Client holds all user info (in token), server stores nothing

---

### SessionCreationPolicy Options

```java
public enum SessionCreationPolicy {
    ALWAYS,      // Always create a session
    IF_REQUIRED, // Create session if needed (default)
    NEVER,       // Don't create session, but use existing if present
    STATELESS    // Never create or use sessions
}
```

**For JWT, you MUST use STATELESS because:**
1. JWT tokens contain all needed authentication info
2. Creating sessions defeats the purpose of JWTs
3. Sessions would waste memory and complicate scaling

---

### What Happens with STATELESS?

When you set `SessionCreationPolicy.STATELESS`:

**1. Spring Security won't create HttpSession:**
```java
// This will return null:
HttpSession session = request.getSession(false);
```

**2. SecurityContextHolder uses a different strategy:**
```java
// Instead of storing in session, Spring uses:
SecurityContextHolder.setStrategyName(
    SecurityContextHolder.MODE_THREADLOCAL
);
```

**MODE_THREADLOCAL means**: 
- Authentication is stored in the current thread's local storage
- It's available only for the duration of the request
- It's automatically cleared after the request completes
- Each request starts fresh (truly stateless)

**3. The authentication flow per request:**
```java
Request arrives
   ↓
JwtAuthenticationFilter extracts token
   ↓
Token is validated and user info extracted
   ↓
Authentication stored in ThreadLocal
   ↓
Request processed by controllers
   ↓
Response sent
   ↓
ThreadLocal automatically cleared
   ↓
Next request starts fresh (no memory of previous request)
```

---

### Visualizing Stateless vs Stateful

**STATEFUL (Session-based):**
```
Request 1: Login
Server RAM: { 
  sessions: { 
    "abc123": { user: "john", ... } 
  } 
}

Request 2: Get transactions
Server RAM: { 
  sessions: { 
    "abc123": { user: "john", ... },  // Still here!
    "def456": { user: "jane", ... }   // New user
  } 
}

// Memory grows with each user
```

**STATELESS (JWT-based):**
```
Request 1: Login
Server RAM: { } // Empty!
Response: { token: "eyJ..." }

Request 2: Get transactions
Server RAM: { } // Still empty!
// Token validated, user info extracted from token
// Request processed
// Nothing stored

// Memory usage constant regardless of users
```

---

### Benefits of Stateless

**1. Horizontal Scalability:**
```
Load Balancer
    ↓
┌───────┬───────┬───────┐
│Server1│Server2│Server3│  ← Any server can handle any request
└───────┴───────┴───────┘
```

No need for:
- Sticky sessions
- Session replication
- Shared session storage (Redis, etc.)

**2. Microservices-Friendly:**
```
API Gateway (validates JWT)
    ↓
┌────────────┬─────────────┬──────────────┐
│User Service│Order Service│Payment Service│
└────────────┴─────────────┴──────────────┘
      ↓              ↓              ↓
Each service can independently validate JWT
No shared session storage needed
```

**3. Memory Efficiency:**
- 10,000 users logged in
- Session-based: ~50MB+ of session data in memory
- JWT-based: 0 bytes (no sessions stored)

**4. Simpler Server Code:**
- No session lifecycle management
- No session timeout handling
- No session synchronization across servers

---

### The Trade-off: Logout/Revocation

**Session-based logout:**
```java
// Simple - just delete session
session.invalidate();
```

**JWT-based logout:**
```java
// Problem: Token is valid until expiration
// Can't "delete" a JWT that client holds

// Solutions:
// 1. Short expiration (15 minutes) + refresh tokens
// 2. Token blacklist in Redis
// 3. Token versioning in database
```

**This is why JWT expiration times are crucial!**

---

### Summary: Session Management

| Aspect | Session-Based | JWT (Stateless) |
|--------|--------------|-----------------|
| **Server Storage** | Yes, in memory/database | No |
| **Scalability** | Complex (sticky sessions) | Simple (any server) |
| **Memory Usage** | Grows with users | Constant |
| **Logout** | Easy (delete session) | Complex (blacklist needed) |
| **Mobile Apps** | Problematic (cookies) | Perfect (tokens in headers) |
| **Microservices** | Difficult | Excellent |

**For your app**: STATELESS is the right choice because you're using JWTs and want a modern, scalable architecture.

#### 4. Authorization Rules

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .anyRequest().authenticated()
)
```

**What**: Defines which endpoints are public and which require authentication.

**Breaking it down**:
- `/api/auth/**`: Login, register, password reset - public (anyone can access)
- `/actuator/health`: Health check - public (for monitoring tools)
- `/swagger-ui/**`: API documentation - public (for developers)
- `anyRequest().authenticated()`: Everything else requires a valid JWT

**Think of it as**: Some areas of the concert (lobby, ticket booth) are public, but the actual show requires a ticket.

#### 5. Add JWT Filter

```java
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

**What**: Inserts your custom JWT filter into the security chain.

**Why "before"**: Your filter needs to run BEFORE Spring's default authentication filter, so you can:
1. Extract and validate the JWT token
2. Set up authentication in Spring Security context
3. Let the request continue through the chain

**Think of it as**: Adding your custom wristband checker before the venue's default ID checker.

### Password Encoder Bean

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**What**: Creates a password hasher using BCrypt algorithm.

**Why**: 
- Never store passwords in plain text
- BCrypt is slow by design (makes brute-force attacks impractical)
- Includes automatic salting (same password → different hash each time)

---

## Component 2: JwtAuthenticationFilter.java

This is your **security checkpoint** - it runs on EVERY request to check if the user has a valid JWT.

### The Flow

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
```

**What**: `OncePerRequestFilter` ensures this filter runs exactly once per request (even if the request is forwarded internally).

**Why extend OncePerRequestFilter?**
- Spring has many filters in its security chain
- Without this, your filter might run multiple times for a single request
- Example: Request forwarded from `/api/users` to `/api/users/profile` internally
- `OncePerRequestFilter` guarantees single execution

---

### Dependencies Injected

```java
private final UserDetailsServiceImpl userDetailsService;
private final JwtUtil jwtUtil;
```

**UserDetailsServiceImpl**: Loads user details from database (we'll cover this next)
**JwtUtil**: Your custom utility class for JWT operations (validating, extracting claims, etc.)

**Why inject UserDetailsService?**
- To load the full user from database
- To get user's roles/authorities
- To verify the user still exists and is active

---

### Step-by-Step Execution

#### Step 1: Extract Authorization Header

```java
final String authHeader = request.getHeader("Authorization");

if (authHeader == null || !authHeader.startsWith("Bearer ")) {
    filterChain.doFilter(request, response);
    return;
}
```

**What happens**:
- Looks for `Authorization` header in the HTTP request
- Expected format: `Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
- If missing or wrong format → skip authentication and continue

**Why continue instead of rejecting?**
- Some endpoints are public (`/api/auth/login`, `/api/auth/register`)
- The authorization rules in SecurityConfig will handle rejection later
- This filter's job is only to SET authentication if valid token exists

**Example headers:**
```
✅ Valid:   Authorization: Bearer eyJhbGciOiJIUz...
❌ Invalid: Authorization: eyJhbGciOiJIUz...     (missing "Bearer ")
❌ Invalid: Bearer eyJhbGciOiJIUz...              (wrong header name)
❌ Invalid: No header at all
```

#### Step 2: Extract JWT Token

```java
final String jwt = authHeader.substring(7);
```

**What**: Removes "Bearer " (7 characters) to get just the token.

**String manipulation breakdown:**
```java
authHeader = "Bearer eyJhbGciOiJIUz..."
           = "0123456789..."  (index positions)
              
substring(7) = "eyJhbGciOiJIUz..."  (starts from index 7)
```

**Example**:
- Input: `"Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."`
- Output: `"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."`

#### Step 3: Extract Email from Token

```java
final String email = jwtUtil.extractEmail(jwt);
```

**What**: Decodes the JWT and pulls out the email (or username) claim.

**How JWTs are structured:**
```
JWT = Header.Payload.Signature

Example JWT (before encoding):
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "email": "user@example.com",
    "userId": 42,
    "role": "USER",
    "exp": 1705849200,
    "iat": 1705845600
  },
  "signature": "hashed_value_here"
}

After encoding: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJ1c2VySWQiOjQyLCJyb2xlIjoiVVNFUiIsImV4cCI6MTcwNTg0OTIwMCwiaWF0IjoxNzA1ODQ1NjAwfQ.hashed_signature
```

**What extractEmail() does internally:**
```java
// Conceptually:
public String extractEmail(String token) {
    Claims claims = parseToken(token);  // Decode the payload
    return claims.get("email", String.class);
}
```

#### Step 4: Check Authentication Status

```java
if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
```

**Two conditions**:

**1. `email != null`**
- Ensures we successfully extracted an email from the token
- If token is malformed, extractEmail() might return null

**2. `SecurityContextHolder.getContext().getAuthentication() == null`**
- Checks if user is NOT already authenticated
- Prevents redundant work if another filter already authenticated them
- Performance optimization

**SecurityContextHolder explained:**
```java
// Thread-local storage for security context
SecurityContextHolder
    ↓
  SecurityContext (per thread)
    ↓
  Authentication (null if not authenticated)
```

#### Step 5: Load User from Database

```java
UserDetails userDetails = userDetailsService.loadUserByUsername(email);
```

**This is a critical change from your original code!**

**What happens**:
1. Calls `UserDetailsServiceImpl.loadUserByUsername(email)`
2. That method queries the database for the user
3. Returns `UserDetailsImpl` wrapping your `User` entity
4. Contains user's password, roles, and account status

**Why load from database?**
- To verify the user still exists
- To check if account is active (`isEnabled()`)
- To get current roles (in case they changed since token was issued)
- To ensure the user wasn't deleted or deactivated

**Example:**
```java
// Token says: email = "john@example.com"
// We load from DB to verify:
// - User still exists? ✓
// - Account still active? ✓
// - Current role? "ADMIN" (might have changed since token was issued)
```

#### Step 6: Validate Token

```java
if (jwtUtil.isTokenValid(jwt) && !jwtUtil.isTokenExpired(jwt)) {
```

**Two separate checks:**

**1. `isTokenValid(jwt)`**
- Verifies the signature is correct
- Ensures token wasn't tampered with

**How signature validation works:**
```java
// When creating token:
String signature = hash(header + payload + SECRET_KEY);
token = header.payload.signature

// When validating:
String expectedSignature = hash(header + payload + SECRET_KEY);
if (token.signature == expectedSignature) {
    return true;  // Valid
} else {
    return false; // Tampered!
}
```

**2. `!jwtUtil.isTokenExpired(jwt)`**
- Checks if current time < expiration time

```java
// Conceptually:
public boolean isTokenExpired(String token) {
    Date expiration = extractExpiration(token);
    return expiration.before(new Date());  // true if expired
}

// So we check: !isTokenExpired() → token is NOT expired
```

**Why two separate checks?**
- A token can be valid but expired (correct signature, but too old)
- A token can be not-expired but invalid (tampered with)
- Both must pass

#### Step 7: Create Authentication Token

```java
UsernamePasswordAuthenticationToken authToken =
    new UsernamePasswordAuthenticationToken(
        userDetails,                    // Principal
        null,                           // Credentials
        userDetails.getAuthorities()    // Authorities
    );
```

**What is UsernamePasswordAuthenticationToken?**
- Spring Security's standard authentication object
- Represents an authenticated user
- Despite the name, it's not limited to username/password (works for JWT too!)

**Breaking down the constructor parameters:**

**1. Principal (userDetails)**
- The authenticated user object
- Can be accessed in controllers via `@AuthenticationPrincipal`
- Type: `UserDetailsImpl` (which wraps your `User` entity)

```java
// Later in a controller:
@GetMapping("/profile")
public UserProfile getProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
    User user = userDetails.getUser();
    return new UserProfile(user.getUsername(), user.getEmail());
}
```

**2. Credentials (null)**
- Usually the password
- For JWT, we don't need it (token is the credential)
- Passing `null` is standard practice for token-based auth

**3. Authorities (userDetails.getAuthorities())**
- User's roles and permissions
- Returns: `[GrantedAuthority("ROLE_USER")]` or `[GrantedAuthority("ROLE_ADMIN")]`
- Used for `@PreAuthorize` annotations

**Example of what's created:**
```java
authToken = {
    principal: UserDetailsImpl {
        user: User { id: 42, email: "john@example.com", role: "USER" }
    },
    credentials: null,
    authorities: [SimpleGrantedAuthority("ROLE_USER")],
    authenticated: true
}
```

#### Step 8: Add Request Details

```java
authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
```

**What**: Attaches additional request information to the authentication object.

**What details are added:**
```java
WebAuthenticationDetails {
    remoteAddress: "192.168.1.100",  // Client's IP
    sessionId: null                   // (null because stateless)
}
```

**Why add this?**
- Security auditing (know which IP made the request)
- Logging and monitoring
- Detecting suspicious activity (same account from different IPs)

**Example usage:**
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
WebAuthenticationDetails details = (WebAuthenticationDetails) auth.getDetails();
logger.info("Request from IP: {}", details.getRemoteAddress());
```

#### Step 9: Set Authentication in Context

```java
SecurityContextHolder.getContext().setAuthentication(authToken);
```

**This is the critical step!**

**What happens:**
1. Gets the SecurityContext for current thread
2. Sets the authentication object we just created
3. Now Spring Security knows this request is authenticated

**Visual representation:**
```
Before:
SecurityContextHolder
    → ThreadLocal
        → SecurityContext
            → authentication = null ❌

After:
SecurityContextHolder
    → ThreadLocal
        → SecurityContext
            → authentication = authToken ✅
```

**Why this matters:**
- `@PreAuthorize` annotations now work
- `authorizeHttpRequests()` rules see the user as authenticated
- Controllers can access the user

**Example of what can now happen:**
```java
// In your controller:
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public void deleteUser(@PathVariable Long id) {
    // This method only executes if authentication has ROLE_ADMIN
    // Spring Security checks: SecurityContextHolder → authToken → authorities
}
```

#### Step 10: Continue the Chain

```java
filterChain.doFilter(request, response);
```

**What**: Passes the request to the next filter in the chain.

**Why**: The security chain has many filters. After we've done our JWT work, other filters (CORS, CSRF, authorization, etc.) need to run.

**Filter chain visualization:**
```
Request
   ↓
JwtAuthenticationFilter (← We are here)
   ↓
CorsFilter
   ↓
CsrfFilter
   ↓
AuthorizationFilter
   ↓
ExceptionTranslationFilter
   ↓
FilterSecurityInterceptor
   ↓
Your Controller
```

---

### Error Handling

```java
} catch (Exception e) {
    log.warn("Could not set user authentication: {}", e.getMessage());
}

filterChain.doFilter(request, response);
```

**Why catch exceptions?**
- JWT might be malformed → parsing fails
- User might have been deleted → database lookup fails
- Token signature might be invalid

**Why just log and continue?**
- We don't want to crash the request
- Let it continue to the authorization filter
- That filter will see no authentication and return 401 Unauthorized

**What gets logged:**
```
WARN - Could not set user authentication: JWT signature does not match
WARN - Could not set user authentication: User not found with email: deleted@example.com
WARN - Could not set user authentication: JWT expired at 2025-01-17T10:00:00Z
```

**The elegant failure:**
```
Bad token → Exception → Log warning → Don't set authentication → Continue
                                              ↓
                                    Authorization filter sees no auth
                                              ↓
                                    Returns 401 Unauthorized
```

---

---

## Component 4: UserDetailsServiceImpl.java

This is your **database bridge** - it loads user information from the database and converts it to Spring Security's format.

### What is UserDetailsService?

**UserDetailsService** is a Spring Security interface with ONE method:

```java
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

**Purpose**: Spring Security doesn't know about your database or your `User` entity. This interface is the contract for "give me a username, I'll give you user details."

**Why it exists**: 
- Separation of concerns (Spring Security doesn't couple to your database)
- Flexibility (you can load users from database, LDAP, file, API, anywhere)
- Standard interface all authentication mechanisms can use

---

### Your Implementation

```java
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));

        return new UserDetailsImpl(user);
    }
}
```

---

### Breaking It Down

#### 1. The Annotation

```java
@Service
```

**What**: Marks this as a Spring service bean.

**Why**: 
- Spring will create an instance and manage it
- Makes it available for dependency injection (used in `JwtAuthenticationFilter`)
- Scanned during component scanning

#### 2. Dependency Injection

```java
@RequiredArgsConstructor
private final UserRepository userRepository;
```

**What**: Lombok generates a constructor that injects `UserRepository`.

**Equivalent code without Lombok:**
```java
private final UserRepository userRepository;

public UserDetailsServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
}
```

**UserRepository**: Your JPA repository interface for database access.

#### 3. The Method Implementation

```java
@Override
public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
```

**Method signature notes:**
- Parameter is called "username" but you're using email (perfectly fine!)
- Returns `UserDetails` interface (not your `User` entity)
- Can throw `UsernameNotFoundException` if user not found

**Where this gets called from:**
1. **During login** (when you validate credentials)
2. **During JWT validation** (in your filter - `userDetailsService.loadUserByUsername(email)`)
3. **Any custom authentication logic** you add

#### 4. Database Query

```java
User user = userRepository.findByEmail(email)
```

**What happens:**
- Calls your repository method: `Optional<User> findByEmail(String email)`
- JPA/Hibernate generates SQL: `SELECT * FROM users WHERE email = ?`
- Returns `Optional<User>` (might be empty if not found)

**Example UserRepository:**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

#### 5. Handle Not Found Case

```java
.orElseThrow(() -> new UsernameNotFoundException(
    "User not found with email: " + email
))
```

**What**: If `Optional` is empty, throw exception.

**Why `UsernameNotFoundException`?**
- Spring Security's standard exception for "user not found"
- Authentication mechanisms know how to handle it
- Results in proper 401 Unauthorized response

**What happens when thrown:**
```
loadUserByUsername("nonexistent@example.com")
    ↓
Database query returns empty Optional
    ↓
Throw UsernameNotFoundException
    ↓
Caught by Spring Security
    ↓
Returns 401 Unauthorized to client
```

**Error message example:**
```
User not found with email: hacker@evil.com
```

**Security note**: Some argue you shouldn't reveal whether an email exists in your system. Alternative:
```java
.orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
```

#### 6. Wrap and Return

```java
return new UserDetailsImpl(user);
```

**What**: Wraps your `User` entity in `UserDetailsImpl`.

**Why**: 
- Spring Security expects `UserDetails` interface
- Your `User` entity doesn't implement `UserDetails`
- `UserDetailsImpl` is the adapter

**What gets returned:**
```java
UserDetailsImpl {
    user: User {
        id: 42,
        username: "john_doe",
        email: "john@example.com",
        password: "$2a$10$hashed_password...",
        userRole: "USER",
        active: true
    }
}
```

---

### The Complete Flow: Login Example

Let's trace what happens when a user logs in:

```java
// 1. User submits login request
POST /api/auth/login
{
    "email": "john@example.com",
    "password": "Password123"
}

// 2. AuthController receives request
@PostMapping("/login")
public AuthResponse login(@RequestBody LoginRequest request) {
    // ... authentication logic ...
}

// 3. Inside authentication logic, Spring Security calls:
UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
// ↓
// UserDetailsServiceImpl.loadUserByUsername("john@example.com")
// ↓
// userRepository.findByEmail("john@example.com")
// ↓
// SQL: SELECT * FROM users WHERE email = 'john@example.com'
// ↓
// Returns: User { id: 42, email: "john@example.com", ... }
// ↓
// return new UserDetailsImpl(user);

// 4. Now we have UserDetails, validate password
if (passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
    // Password correct! Generate JWT
    String token = jwtUtil.generateToken(userDetails.getUsername());
    return new AuthResponse(token);
} else {
    // Password wrong
    throw new BadCredentialsException("Invalid credentials");
}
```

---

### When Is This Called?

**1. During Login (explicit call)**
```java
// In your AuthService or similar:
UserDetails userDetails = userDetailsService.loadUserByUsername(email);
boolean passwordMatches = passwordEncoder.matches(rawPassword, userDetails.getPassword());
```

**2. During JWT Validation (in filter)**
```java
// In JwtAuthenticationFilter:
String email = jwtUtil.extractEmail(jwt);
UserDetails userDetails = userDetailsService.loadUserByUsername(email);
// Use this to create authentication token
```

**3. During Spring Security's Authentication (if using AuthenticationManager)**
```java
// Spring Security internally calls this when you use:
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(email, password)
);
// Internally: loadUserByUsername(email) → compare passwords
```

---

### Why Not Load User Directly in Filter?

You might wonder: "Why not just inject `UserRepository` into the filter?"

```java
// ❌ Why not just do this in filter?
User user = userRepository.findByEmail(email).orElseThrow(...);
```

**Answer: Separation of Concerns and Flexibility**

**1. Single Responsibility:**
- Filter's job: Extract token, validate, set authentication
- Service's job: Load user details
- Repository's job: Database access

**2. Reusability:**
- Multiple places need to load users (login, JWT validation, password reset, etc.)
- One service method for all use cases

**3. Flexibility:**
- Easy to switch authentication sources (database → LDAP → OAuth → etc.)
- Just swap `UserDetailsService` implementation

**4. Spring Security Integration:**
- Many Spring Security features expect `UserDetailsService`
- Following conventions makes integration easier

**5. Testing:**
- Easy to mock `UserDetailsService` in tests
- Don't need to mock repository + create UserDetailsImpl

**Example of flexibility:**
```java
// Database implementation (current)
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    // ... load from database ...
}

// Could easily switch to LDAP:
@Service
public class LdapUserDetailsService implements UserDetailsService {
    private final LdapTemplate ldapTemplate;
    // ... load from LDAP ...
}

// Or external API:
@Service
public class ApiUserDetailsService implements UserDetailsService {
    private final RestTemplate restTemplate;
    // ... load from API ...
}

// Filter doesn't need to change!
```

---

### Common Mistakes to Avoid

**1. Returning null instead of throwing exception:**
```java
// ❌ Bad:
public UserDetails loadUserByUsername(String email) {
    return userRepository.findByEmail(email)
        .map(UserDetailsImpl::new)
        .orElse(null);  // Don't return null!
}

// ✅ Good:
public UserDetails loadUserByUsername(String email) {
    return userRepository.findByEmail(email)
        .map(UserDetailsImpl::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
}
```

**2. Not handling the Optional properly:**
```java
// ❌ Dangerous:
User user = userRepository.findByEmail(email).get();  // Throws NoSuchElementException if empty!

// ✅ Safe:
User user = userRepository.findByEmail(email)
    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
```

**3. Exposing too much information in exception:**
```java
// ⚠️ Security concern:
throw new UsernameNotFoundException("No user with email: " + email);
// Reveals which emails exist in your system

// 🔒 Better:
throw new UsernameNotFoundException("Invalid credentials");
```

---

## Component 3: UserDetailsImpl.java

This is your **user wrapper** - it adapts your `User` entity to Spring Security's `UserDetails` interface.

### Why This Exists

Spring Security doesn't know about your custom `User` class. It expects a `UserDetails` object. This class bridges the gap.

### The Implementation

```java
@RequiredArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private final User user;
```

**What**: Wraps your `User` entity. Lombok's `@RequiredArgsConstructor` creates a constructor that takes a `User`.

### Method 1: Get Authorities

```java
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole()));
}
```

**What**: Converts your user's role into Spring Security's format.

**Example**:
- Your DB has: `userRole = "ADMIN"`
- This returns: `ROLE_ADMIN`
- Now you can use: `@PreAuthorize("hasRole('ADMIN')")`

**Why "ROLE_" prefix**: Spring Security's convention for role-based checks.

### Method 2: Get Password

```java
@Override
public String getPassword() {
    return user.getPassword();
}
```

**What**: Returns the hashed password from your User entity.

**When it's used**: During login (not during JWT validation).

### Method 3: Get Username

```java
@Override
public String getUsername() {
    return user.getUsername();
}
```

**What**: Returns the username for Spring Security to use.

**Note**: Could also return email if you use email as username.

### Methods 4-7: Account Status

```java
@Override
public boolean isAccountNonExpired() { return true; }

@Override
public boolean isAccountNonLocked() { return true; }

@Override
public boolean isCredentialsNonExpired() { return true; }

@Override
public boolean isEnabled() { return user.isActive(); }
```

**What**: These tell Spring Security the account's status.

**Breaking it down**:
- `isAccountNonExpired`: Account hasn't passed its expiration date (you might add this feature later)
- `isAccountNonLocked`: Account isn't locked (e.g., after too many failed login attempts)
- `isCredentialsNonExpired`: Password hasn't expired (some systems force password changes)
- `isEnabled`: Account is active (maps to your `user.isActive()` field)

**Current implementation**: Only `isEnabled()` actually checks anything; the others return `true` (all accounts are valid unless inactive).

---

## The Complete Flow: Login to API Call

### 1. User Logs In

```
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}
```

**What happens**:
1. Controller receives request
2. Service validates email/password
3. If valid, creates JWT token with user's email
4. Returns token to frontend

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com"
}
```

### 2. Frontend Stores Token

```typescript
// Angular code
localStorage.setItem('jwt_token', response.token);
```

### 3. User Makes Authenticated Request

```
GET /api/transactions
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**What happens (in order)**:

#### a) Request hits JwtAuthenticationFilter
1. Extracts token from header
2. Validates token signature
3. Checks expiration
4. Loads user from database
5. Creates authentication object
6. Sets it in SecurityContext

#### b) Request hits SecurityFilterChain
1. Checks authorization rules
2. Sees `/api/transactions` requires authentication
3. Finds authentication in SecurityContext (set by filter)
4. Allows request through

#### c) Request hits Controller
```java
@GetMapping("/api/transactions")
public List<Transaction> getTransactions(@AuthenticationPrincipal User user) {
    // 'user' is automatically populated from SecurityContext
    return transactionService.getTransactionsForUser(user);
}
```

### 4. Response Returns to Frontend

```json
[
  { "id": 1, "amount": 50.00, "category": "Food" },
  { "id": 2, "amount": 100.00, "category": "Transport" }
]
```

---

## Security Best Practices

### 1. Token Expiration
Always set expiration times on JWTs (typically 15 minutes to 1 hour).

### 2. Refresh Tokens
For better UX, implement refresh tokens (long-lived tokens used to get new access tokens).

### 3. HTTPS Only
In production, only send JWTs over HTTPS to prevent interception.

### 4. Token Invalidation
Consider maintaining a blacklist for revoked tokens (for logout, compromised accounts).

### 5. Secret Key Management
Store JWT secret key in environment variables, not in code:
```java
@Value("${jwt.secret}")
private String secretKey;
```

---

## Key Takeaways

1. **SecurityConfig**: Defines WHAT is protected and HOW
2. **JwtAuthenticationFilter**: Validates tokens on EVERY request
3. **UserDetailsImpl**: Bridges YOUR user model with SPRING SECURITY
4. **Stateless**: Server doesn't remember users; token contains all info
5. **Filter Chain**: Request passes through multiple filters before reaching controller

## Questions to Test Your Understanding

1. Why do we disable CSRF for JWT authentication?
2. What would happen if we removed `@EnableMethodSecurity`?
3. Why does the filter extend `OncePerRequestFilter`?
4. What's the difference between authentication and authorization?
5. Why do we add "ROLE_" prefix to authorities?
6. What's the difference between `SessionCreationPolicy.NEVER` and `SessionCreationPolicy.STATELESS`?
7. Why can't we use `setAllowedOrigins("*")` with `setAllowCredentials(true)`?
8. What happens if we don't include OPTIONS in `setAllowedMethods()`?

Think through these, and you'll have a solid grasp of Spring Security with JWT!

---

## Complete Architecture Diagram

Here's how all the components work together:

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT (Angular)                         │
│                                                                   │
│  1. Login with email/password                                    │
│     POST /api/auth/login                                         │
│     { email: "user@example.com", password: "pass123" }           │
└────────────────────────────┬──────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                      SPRING BOOT BACKEND                         │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              SecurityConfig (Blueprint)                    │ │
│  │  • CORS: Allow localhost:4200                              │ │
│  │  • CSRF: Disabled                                          │ │
│  │  • Session: STATELESS                                      │ │
│  │  • Public: /api/auth/**                                    │ │
│  │  • Protected: Everything else                              │ │
│  └────────────────────────────────────────────────────────────┘ │
│                             │                                     │
│                             ↓                                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │         JwtAuthenticationFilter (Security Guard)           │ │
│  │  1. Extract "Bearer token" from Authorization header       │ │
│  │  2. Validate token signature                               │ │
│  │  3. Check expiration                                       │ │
│  │  4. Load user via UserDetailsService                       │ │
│  │  5. Set authentication in SecurityContext                  │ │
│  └────────────────┬───────────────────────────────────────────┘ │
│                   │                                              │
│                   ↓                                              │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │      UserDetailsServiceImpl (Database Bridge)              │ │
│  │  • Queries database for user by email                      │ │
│  │  • Throws exception if not found                           │ │
│  │  • Wraps User in UserDetailsImpl                           │ │
│  └────────────────┬───────────────────────────────────────────┘ │
│                   │                                              │
│                   ↓                                              │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │            UserDetailsImpl (Adapter)                       │ │
│  │  • Wraps your User entity                                  │ │
│  │  • Implements UserDetails interface                        │ │
│  │  • Converts role to "ROLE_USER" format                     │ │
│  │  • Provides password, account status, etc.                 │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘

                             │
                             ↓
                             
┌─────────────────────────────────────────────────────────────────┐
│                  REQUEST PROCESSING FLOW                         │
│                                                                   │
│  Public Endpoint (/api/auth/login):                              │
│    Request → CORS → JWT Filter (skips - no token) → Controller  │
│                                                                   │
│  Protected Endpoint (/api/transactions):                         │
│    Request → CORS → JWT Filter (validates token)                 │
│             → Sets SecurityContext → Authorization Check         │
│             → Controller (can access @AuthenticationPrincipal)   │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

---

## Key Concepts Summary

### 1. CORS (Cross-Origin Resource Sharing)
- **What**: Browser security mechanism
- **Why**: Prevents malicious sites from accessing your API
- **Config**: Allow specific origins, methods, headers
- **Critical**: Must allow OPTIONS for preflight requests

### 2. Session Management
- **Stateful (Traditional)**: Server stores session in memory
- **Stateless (JWT)**: Server stores nothing, token contains all info
- **Policy**: STATELESS tells Spring to never create sessions
- **Benefit**: Easy horizontal scaling, microservices-friendly

### 3. Filter Chain
- **Order matters**: JWT filter runs before authorization
- **OncePerRequestFilter**: Guarantees single execution
- **ThreadLocal**: Authentication stored per thread, auto-cleared
- **Error handling**: Log and continue, let authorization reject

### 4. UserDetailsService
- **Contract**: Give me username, I'll return user details
- **Separation**: Decouples authentication from data access
- **Reusability**: One service for login, JWT, password reset, etc.
- **Flexibility**: Easy to swap implementations (DB, LDAP, API)

### 5. Authentication vs Authorization
- **Authentication**: Who are you? (handled by JWT filter)
- **Authorization**: What can you do? (handled by SecurityFilterChain)
- **Flow**: Authenticate first → then check permissions

---

## Practical Tips

### Development Best Practices

**1. Environment-Specific Configuration**
```java
// application-dev.yml
cors:
  allowed-origins: http://localhost:4200

// application-prod.yml  
cors:
  allowed-origins: https://myapp.com
```

**2. JWT Token Lifecycle**
```java
// Short-lived access token
accessTokenExpiration: 15 minutes

// Long-lived refresh token
refreshTokenExpiration: 7 days

// Store refresh token separately, use to get new access tokens
```

**3. Logging for Debugging**
```java
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(...) {
        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());
        log.debug("Authorization header present: {}", authHeader != null);
        
        if (email != null) {
            log.debug("Extracted email from token: {}", email);
        }
        
        // ... rest of code
    }
}
```

**4. Exception Handling**
```java
// Create custom exception handler for authentication errors
@RestControllerAdvice
public class AuthExceptionHandler {
    
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UsernameNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("Invalid credentials"));
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("Invalid credentials"));
    }
}
```

---

## Common Debugging Scenarios

### Problem: CORS error in browser
**Symptom**: Console shows "CORS policy: No 'Access-Control-Allow-Origin' header"
**Solutions**:
1. Check `setAllowedOrigins()` includes your frontend URL
2. Verify CORS configured BEFORE other security rules
3. Ensure OPTIONS method is allowed
4. Check `setAllowCredentials(true)` is set

### Problem: 401 Unauthorized on protected endpoints
**Symptom**: Valid token but still getting 401
**Debug steps**:
1. Add logging to filter to see if token is being extracted
2. Check if `SecurityContextHolder.getContext().getAuthentication()` is set
3. Verify token hasn't expired
4. Ensure JWT secret key matches between creation and validation
5. Check user still exists in database

### Problem: Token works but no user in controller
**Symptom**: `@AuthenticationPrincipal` is null
**Solutions**:
1. Verify authentication principal is set correctly in filter
2. Check you're using `UserDetailsImpl` not just `User`
3. Ensure filter runs before the controller
4. Add `@PreAuthorize` might help trigger authentication

### Problem: OPTIONS request fails
**Symptom**: POST/PUT/DELETE fail, but GET works
**Solutions**:
1. Add OPTIONS to `setAllowedMethods()`
2. Verify preflight request returns 200 OK
3. Check all required headers are in `setAllowedHeaders()`

---

## Next Steps: Enhancements

**1. Refresh Token Implementation**
```java
// When access token expires, use refresh token to get new one
POST /api/auth/refresh
Headers: { "Refresh-Token": "long_lived_token" }
Response: { "accessToken": "new_short_lived_token" }
```

**2. Token Blacklist (Logout)**
```java
// Store revoked tokens in Redis
@Service
public class TokenBlacklistService {
    private final RedisTemplate<String, String> redisTemplate;
    
    public void blacklistToken(String token) {
        String tokenId = jwtUtil.extractTokenId(token);
        long expirationTime = jwtUtil.getExpirationTime(token);
        redisTemplate.opsForValue().set(
            "blacklist:" + tokenId, 
            "revoked",
            expirationTime, 
            TimeUnit.SECONDS
        );
    }
    
    public boolean isBlacklisted(String token) {
        String tokenId = jwtUtil.extractTokenId(token);
        return redisTemplate.hasKey("blacklist:" + tokenId);
    }
}
```

**3. Rate Limiting**
```java
// Prevent brute force attacks
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    // Limit login attempts per IP
}
```

**4. Audit Logging**
```java
// Track who did what and when
@Aspect
@Component
public class AuditAspect {
    @AfterReturning("@annotation(Auditable)")
    public void audit(JoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Log user action
    }
}
```

---

## Conclusion

You now understand:
✅ How CORS protects your API while allowing your frontend
✅ Why stateless sessions are better for modern apps
✅ How JWT filters validate every request
✅ How Spring Security's components work together
✅ The role of each class in your security setup

**The beauty of this architecture:**
- Scalable (no server-side sessions)
- Secure (signature validation, expiration)
- Flexible (works with any frontend)
- Standard (uses Spring Security conventions)

Keep building, and don't hesitate to dive deeper into any of these concepts!
