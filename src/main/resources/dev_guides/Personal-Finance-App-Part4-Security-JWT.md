# 🚀 Personal Finance App - Learning Roadmap Part 4
## Phase 7: Security with JWT - Authentication & Authorization

> **Continuation from Parts 1-3** - Complete Phases 1-6 before starting here!

---

## Phase 7: Security with JWT - Authentication & Authorization

**Duration:** 1-2 weeks  
**Goal:** Implement secure authentication and authorization with JWT tokens  
**Difficulty:** ⭐⭐⭐⭐⭐

### 📚 What You'll Learn

- Spring Security fundamentals
- JWT (JSON Web Token) authentication
- Password encryption with BCrypt
- Token generation and validation
- Security filter chain
- Method-level security
- CORS configuration
- Authentication vs Authorization

### 🎯 Security Concepts

**Authentication vs Authorization:**
```
Authentication: "Who are you?"
- Login with username/password
- Verify credentials
- Issue JWT token

Authorization: "What can you do?"
- Check JWT token
- Verify user role
- Allow/deny access to resources
```

**JWT Token Flow:**
```
1. User logs in → Send username/password
2. Server validates → Check against database
3. Server creates JWT → Contains user ID, role, expiration
4. Client stores JWT → In localStorage or cookie
5. Client sends JWT → In Authorization header for every request
6. Server validates JWT → Decode, verify signature, check expiration
7. Server allows access → If JWT valid
```

---

### Understanding JWT Structure

A JWT has 3 parts separated by dots: `header.payload.signature`

```
Example JWT:
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEyMywidXNlcm5hbWUiOiJqb2huZG9lIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3MDk0MDAwMDAsImV4cCI6MTcwOTQ4NjQwMH0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

Decoded:
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "userId": 123,
    "username": "johndoe",
    "role": "USER",
    "iat": 1709400000,  // Issued at
    "exp": 1709486400   // Expires at (24 hours later)
  },
  "signature": "..."  // Cryptographic signature
}
```

---

### Step 1: Dependencies

Add to `pom.xml`:

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT Library -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
```

---

### Step 2: Configuration Properties

Add to `application.yml`:

```yaml
# JWT Configuration
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-here-make-it-very-long-and-random}
  expiration: 86400000  # 24 hours in milliseconds
  
# Security
spring:
  security:
    user:
      name: admin  # Default admin user (for development only!)
      password: admin123
```

**IMPORTANT:** In production:
- Use environment variable for JWT_SECRET
- Generate a strong random secret (at least 256 bits)
- Never commit secrets to Git

---

### Step 3: Security DTOs

```java
/**
 * Login request DTO.
 * Client sends this to authenticate.
 */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email")
        String email,
        
        @NotBlank(message = "Password is required")
        String password
) {}

/**
 * Authentication response DTO.
 * Server returns this after successful login.
 */
public record AuthResponse(
        String token,        // JWT token
        String type,         // "Bearer"
        Long userId,         // User ID
        String username,     // Username
        String email,        // Email
        UserRolesEnum role,  // User role
        Long expiresIn       // Token expiration in milliseconds
) {
    public static AuthResponse of(String token, User user, long expiresIn) {
        return new AuthResponse(
            token,
            "Bearer",
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getUserRole(),
            expiresIn
        );
    }
}

/**
 * Register request DTO.
 * Extends CreateUserRequest (reuse validation).
 */
public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        String username,
        
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,
        
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
        @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
            message = "Password must contain: digit, lowercase, uppercase, special character"
        )
        String password,
        
        String firstName,
        String lastName,
        Integer age
) {
    /**
     * Converts to CreateUserRequest for service layer.
     */
    public CreateUserRequest toCreateUserRequest() {
        return new CreateUserRequest(
            username,
            email,
            firstName,
            lastName,
            password,
            age
        );
    }
}
```

---

### Step 4: JWT Utility Class

```java
/**
 * Utility class for JWT token operations.
 * 
 * Responsibilities:
 * - Generate JWT tokens
 * - Validate JWT tokens
 * - Extract claims from tokens
 * - Check expiration
 * 
 * Uses HS256 (HMAC with SHA-256) algorithm.
 */
@Component
@Slf4j
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    /**
     * Generates JWT token for authenticated user.
     * 
     * Token includes:
     * - Subject: user email (unique identifier)
     * - Claim: userId
     * - Claim: username
     * - Claim: role
     * - Issued at: current timestamp
     * - Expiration: issued + expiration period
     * 
     * @param user authenticated user
     * @return JWT token string
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getUserRole().name());
        
        return Jwts.builder()
            .claims(claims)
            .subject(user.getEmail())  // Email as unique identifier
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }
    
    /**
     * Extracts email (subject) from token.
     * 
     * @param token JWT token
     * @return user email
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extracts user ID from token.
     * 
     * @param token JWT token
     * @return user ID
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }
    
    /**
     * Extracts username from token.
     * 
     * @param token JWT token
     * @return username
     */
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }
    
    /**
     * Extracts user role from token.
     * 
     * @param token JWT token
     * @return user role
     */
    public UserRolesEnum extractRole(String token) {
        String role = extractClaim(token, claims -> claims.get("role", String.class));
        return UserRolesEnum.valueOf(role);
    }
    
    /**
     * Extracts expiration date from token.
     * 
     * @param token JWT token
     * @return expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extracts a specific claim from token.
     * 
     * @param token JWT token
     * @param claimsResolver function to extract claim
     * @return extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extracts all claims from token.
     * 
     * @param token JWT token
     * @return all claims
     * @throws JwtException if token is invalid
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    /**
     * Validates JWT token.
     * 
     * Checks:
     * 1. Token is not expired
     * 2. Email in token matches user email
     * 
     * @param token JWT token
     * @param user user to validate against
     * @return true if valid
     */
    public boolean validateToken(String token, User user) {
        final String email = extractEmail(token);
        return (email.equals(user.getEmail()) && !isTokenExpired(token));
    }
    
    /**
     * Checks if token is expired.
     * 
     * @param token JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * Gets signing key for JWT.
     * Converts secret string to SecretKey.
     * 
     * @return signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Gets expiration time in milliseconds.
     * 
     * @return expiration time
     */
    public Long getExpirationTime() {
        return expiration;
    }
}
```

---

### Step 5: Custom UserDetails Implementation

Spring Security needs UserDetails to work with users:

```java
/**
 * Custom UserDetails implementation.
 * Adapts our User entity to Spring Security's UserDetails interface.
 * 
 * Spring Security uses UserDetails for authentication and authorization.
 */
@RequiredArgsConstructor
public class UserDetailsImpl implements UserDetails {
    
    private final User user;
    
    /**
     * Gets user authorities (roles/permissions).
     * Spring Security uses this for authorization checks.
     * 
     * We convert UserRolesEnum to GrantedAuthority.
     * 
     * @return collection of granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name())
        );
    }
    
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    
    /**
     * Spring Security uses username for authentication.
     * We use email as the username (unique identifier).
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }
    
    /**
     * Account not expired.
     * You could add an 'accountExpired' field to User entity.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    /**
     * Account not locked.
     * You could add login attempt tracking and lock accounts.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    /**
     * Credentials (password) not expired.
     * You could add password expiration logic.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    /**
     * Account enabled.
     * You could add an 'enabled' field to User entity.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    /**
     * Gets the wrapped User entity.
     * Useful for accessing custom user fields.
     * 
     * @return user entity
     */
    public User getUser() {
        return user;
    }
}
```

---

### Step 6: UserDetailsService Implementation

```java
/**
 * Loads user details for Spring Security.
 * 
 * Spring Security calls this during authentication to load user data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * Loads user by username (email in our case).
     * Called by Spring Security during authentication.
     * 
     * @param email user email
     * @return UserDetails implementation
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found with email: " + email
            ));
        
        return new UserDetailsImpl(user);
    }
}
```

---

### Step 7: JWT Authentication Filter

This filter runs on every request to validate JWT tokens:

```java
/**
 * JWT authentication filter.
 * 
 * Runs on EVERY request before reaching controllers.
 * 
 * Flow:
 * 1. Extract JWT from Authorization header
 * 2. Validate JWT
 * 3. Load user from database
 * 4. Set authentication in SecurityContext
 * 5. Continue to next filter/controller
 * 
 * If JWT invalid: Request continues without authentication
 * (SecurityConfig will block access to protected endpoints)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    
    /**
     * Filters every request to validate JWT.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain filter chain to continue
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Extract JWT from Authorization header
            String jwt = parseJwt(request);
            
            if (jwt != null) {
                // Extract email from JWT
                String email = jwtUtil.extractEmail(jwt);
                
                // Load user from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                // Validate JWT against user
                if (jwtUtil.validateToken(jwt, ((UserDetailsImpl) userDetails).getUser())) {
                    // Create authentication object
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,  // Credentials (password) not needed after authentication
                            userDetails.getAuthorities()
                        );
                    
                    // Set details (IP address, session ID, etc.)
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // Set authentication in SecurityContext
                    // This tells Spring Security: "This user is authenticated"
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("JWT valid - User {} authenticated", email);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // Continue without authentication (will be blocked by SecurityConfig)
        }
        
        // Continue filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extracts JWT from Authorization header.
     * 
     * Expected format: "Bearer <token>"
     * 
     * @param request HTTP request
     * @return JWT token or null
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);  // Remove "Bearer " prefix
        }
        
        return null;
    }
}
```

---

### Step 8: Security Configuration

This is where we configure Spring Security:

```java
/**
 * Spring Security configuration.
 * 
 * Configures:
 * - Password encoding
 * - Authentication manager
 * - Security filter chain
 * - CORS
 * - CSRF protection
 * - Public vs protected endpoints
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Enables @PreAuthorize, @Secured, etc.
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    
    /**
     * Password encoder bean.
     * Uses BCrypt hashing algorithm.
     * 
     * BCrypt:
     * - One-way hash (can't decrypt)
     * - Includes salt (prevents rainbow table attacks)
     * - Adaptive (can increase complexity over time)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Authentication manager bean.
     * Used by AuthController for login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * Configures authentication provider.
     * 
     * Links:
     * - UserDetailsService: Loads user from database
     * - PasswordEncoder: Validates password
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * Security filter chain configuration.
     * 
     * This is the heart of Spring Security configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (not needed for stateless JWT authentication)
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/error").permitAll()
                
                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Stateless session management (no server-side sessions)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Set authentication provider
            .authenticationProvider(authenticationProvider())
            
            // Add JWT filter before Spring Security's authentication filter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
    
    /**
     * CORS configuration.
     * 
     * Allows frontend (React, Angular) running on different port
     * to access our API.
     * 
     * Example:
     * - Frontend: http://localhost:4200 (Angular)
     * - Backend: http://localhost:8080 (Spring Boot)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow requests from these origins
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:4200",  // Angular dev server
            "http://localhost:3000"   // React dev server
        ));
        
        // Allow these HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // Allow these headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        ));
        
        // Expose these headers to frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        // Allow credentials (cookies)
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
```

---

### Step 9: Authentication Service

```java
/**
 * Service for authentication operations.
 * Handles login and registration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    /**
     * Authenticates user and returns JWT token.
     * 
     * Flow:
     * 1. Authenticate with Spring Security
     * 2. Load user from database
     * 3. Generate JWT token
     * 4. Return token + user info
     * 
     * @param request login request
     * @return authentication response with JWT
     * @throws BadCredentialsException if credentials invalid
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.email());
        
        try {
            // Authenticate with Spring Security
            // This validates username/password
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email(),
                    request.password()
                )
            );
            
            // Authentication successful - load user
            User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with email: " + request.email()
                ));
            
            // Generate JWT token
            String token = jwtUtil.generateToken(user);
            
            log.info("Login successful for user: {}", request.email());
            
            return AuthResponse.of(token, user, jwtUtil.getExpirationTime());
            
        } catch (BadCredentialsException e) {
            log.error("Login failed - invalid credentials for: {}", request.email());
            throw new InvalidPasswordException("Invalid email or password");
        }
    }
    
    /**
     * Registers new user and returns JWT token.
     * 
     * Flow:
     * 1. Validate email/username uniqueness
     * 2. Create user (password encrypted by UserService)
     * 3. Generate JWT token
     * 4. Return token + user info
     * 
     * @param request registration request
     * @return authentication response with JWT
     * @throws DuplicateResourceException if email/username exists
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.email());
        
        // Check email uniqueness
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                "Email already registered: " + request.email()
            );
        }
        
        // Check username uniqueness
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException(
                "Username already taken: " + request.username()
            );
        }
        
        // Create user
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setAge(request.age());
        user.setUserRole(UserRolesEnum.USER);  // Default role
        
        User savedUser = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser);
        
        log.info("Registration successful for user: {}", request.email());
        
        return AuthResponse.of(token, savedUser, jwtUtil.getExpirationTime());
    }
}
```

---

### Step 10: Authentication Controller

```java
/**
 * REST API Controller for authentication.
 * 
 * Public endpoints (no authentication required):
 * - POST /api/auth/login    → Login
 * - POST /api/auth/register → Register
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Login endpoint.
     * 
     * Request:
     * POST /api/auth/login
     * {
     *   "email": "john@example.com",
     *   "password": "SecurePass123!"
     * }
     * 
     * Response:
     * 200 OK
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "type": "Bearer",
     *   "userId": 123,
     *   "username": "johndoe",
     *   "email": "john@example.com",
     *   "role": "USER",
     *   "expiresIn": 86400000
     * }
     * 
     * @param request login request
     * @return authentication response with JWT
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        
        log.info("POST /api/auth/login - {}", request.email());
        
        AuthResponse response = authService.login(request);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Register endpoint.
     * 
     * Request:
     * POST /api/auth/register
     * {
     *   "username": "johndoe",
     *   "email": "john@example.com",
     *   "password": "SecurePass123!",
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "age": 25
     * }
     * 
     * Response:
     * 201 CREATED
     * {
     *   "token": "...",
     *   "type": "Bearer",
     *   ...
     * }
     * 
     * @param request registration request
     * @return authentication response with JWT
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        
        log.info("POST /api/auth/register - {}", request.email());
        
        AuthResponse response = authService.register(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

---

### Step 11: Getting Current User in Controllers

Now that we have authentication, we can get the current user:

```java
/**
 * Example: Updated UserController using authentication.
 * 
 * Notice:
 * - No more User-Id header!
 * - Use @AuthenticationPrincipal to get current user
 * - Spring Security automatically provides authenticated user
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * Gets current user's profile.
     * 
     * @AuthenticationPrincipal:
     * - Automatically injects authenticated user
     * - No need to extract from JWT manually
     * - Spring Security handles it via JwtAuthenticationFilter
     * 
     * @param userDetails current authenticated user
     * @return user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        User currentUser = userDetails.getUser();
        UserResponse response = UserResponse.from(currentUser);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Updates current user's profile.
     * Users can only update their own profile.
     * 
     * @param userDetails current authenticated user
     * @param request update request
     * @return updated user
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateUserRequest request) {
        
        User currentUser = userDetails.getUser();
        UserResponse response = userService.updateUser(currentUser.getId(), request);
        
        return ResponseEntity.ok(response);
    }
}
```

---

### Step 12: Updated ItemController with Security

```java
/**
 * TODO: Update ItemController to use authentication
 * 
 * Changes needed:
 * 1. Remove @RequestHeader("User-Id") parameters
 * 2. Add @AuthenticationPrincipal UserDetailsImpl parameter
 * 3. Extract userId from userDetails.getUser().getId()
 * 4. Pass userId to service methods
 * 
 * Example:
 * 
 * OLD:
 * @PostMapping
 * public ResponseEntity<ItemResponse> createItem(
 *     @RequestHeader("User-Id") Long userId,
 *     @Valid @RequestBody CreateItemRequest request) {
 *     ...
 * }
 * 
 * NEW:
 * @PostMapping
 * public ResponseEntity<ItemResponse> createItem(
 *     @AuthenticationPrincipal UserDetailsImpl userDetails,
 *     @Valid @RequestBody CreateItemRequest request) {
 *     
 *     Long userId = userDetails.getUser().getId();
 *     ...
 * }
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ItemController {
    
    private final ItemService itemService;
    
    // TODO: Update all methods to use @AuthenticationPrincipal
    // TODO: Extract userId from userDetails.getUser().getId()
    // TODO: Remove User-Id headers
}
```

---

### Step 13: Method-Level Security

You can also secure individual methods:

```java
/**
 * Example: Admin-only endpoint.
 */
@Service
public class AdminService {
    
    /**
     * Only users with ADMIN role can call this.
     * 
     * @PreAuthorize:
     * - Checks before method execution
     * - Uses SpEL (Spring Expression Language)
     * - Throws AccessDeniedException if false
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAnyUser(Long userId) {
        // Only admins can delete any user
    }
    
    /**
     * Users can only delete their own account.
     * 
     * @param userId user ID to delete
     * @param currentUserId current user's ID
     */
    @PreAuthorize("#userId == #currentUserId or hasRole('ADMIN')")
    public void deleteUser(Long userId, Long currentUserId) {
        // Users can delete their own account
        // OR admins can delete any account
    }
}
```

---

### Testing Authentication

```java
/**
 * Integration tests for authentication.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    void testRegister_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
            "johndoe",
            "john@example.com",
            "SecurePass123!",
            "John",
            "Doe",
            25
        );
        
        // Act
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/auth/register",
            request,
            AuthResponse.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().email()).isEqualTo("john@example.com");
    }
    
    @Test
    void testLogin_Success() {
        // Arrange - Create user
        User user = new User();
        user.setUsername("johndoe");
        user.setEmail("john@example.com");
        user.setPassword(passwordEncoder.encode("SecurePass123!"));
        user.setUserRole(UserRolesEnum.USER);
        userRepository.save(user);
        
        LoginRequest loginRequest = new LoginRequest(
            "john@example.com",
            "SecurePass123!"
        );
        
        // Act
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "/api/auth/login",
            loginRequest,
            AuthResponse.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
    }
    
    @Test
    void testLogin_InvalidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest(
            "nonexistent@example.com",
            "WrongPassword"
        );
        
        // Act
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/api/auth/login",
            request,
            ErrorResponse.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    
    @Test
    void testAccessProtectedEndpoint_WithoutToken_Returns401() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/users/me",
            String.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    
    @Test
    void testAccessProtectedEndpoint_WithToken_Success() {
        // Arrange - Register and get token
        RegisterRequest registerRequest = new RegisterRequest(
            "johndoe",
            "john@example.com",
            "SecurePass123!",
            "John",
            "Doe",
            25
        );
        
        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
            "/api/auth/register",
            registerRequest,
            AuthResponse.class
        );
        
        String token = authResponse.getBody().token();
        
        // Act - Access protected endpoint with token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<UserResponse> response = restTemplate.exchange(
            "/api/users/me",
            HttpMethod.GET,
            entity,
            UserResponse.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().email()).isEqualTo("john@example.com");
    }
}
```

---

### Practice Exercise 11: Secure Your Endpoints

```java
/**
 * TODO: Update all your controllers to use authentication
 * 
 * TASKS:
 * 
 * 1. Update ItemController:
 *    - Remove User-Id headers
 *    - Add @AuthenticationPrincipal UserDetailsImpl
 *    - Extract userId from userDetails.getUser().getId()
 * 
 * 2. Update PriceObservationController:
 *    - Same changes as ItemController
 * 
 * 3. Create CategoryController:
 *    - Secure all endpoints
 *    - Use authentication for userId
 * 
 * 4. Test with Postman:
 *    - Register a user
 *    - Login and get JWT token
 *    - Use token in Authorization header for all requests
 *    - Format: "Bearer <token>"
 * 
 * 5. Write integration tests:
 *    - Test accessing endpoints without token (401)
 *    - Test accessing endpoints with valid token (200)
 *    - Test accessing other users' resources (403 or 404)
 */
```

---

### Security Checklist

Before moving to Phase 8:
- [ ] JWT dependencies added
- [ ] JWT secret configured (environment variable in production)
- [ ] JwtUtil class created
- [ ] UserDetailsImpl created
- [ ] UserDetailsServiceImpl created
- [ ] JwtAuthenticationFilter created
- [ ] SecurityConfig configured
- [ ] AuthService created
- [ ] AuthController created
- [ ] All controllers updated to use @AuthenticationPrincipal
- [ ] Password encoder bean configured
- [ ] CORS configured for frontend
- [ ] Integration tests for auth endpoints
- [ ] Integration tests for protected endpoints

---

### Common Security Pitfalls to Avoid

**1. Weak JWT Secret**
```java
// ❌ BAD - Too short, predictable
jwt.secret=mysecret

// ✅ GOOD - Long, random, stored in environment variable
jwt.secret=${JWT_SECRET}
```

**2. No Token Expiration**
```java
// ❌ BAD - Token never expires
.expiration(new Date(Long.MAX_VALUE))

// ✅ GOOD - Reasonable expiration (24 hours)
.expiration(new Date(System.currentTimeMillis() + 86400000))
```

**3. Trusting Client-Provided User ID**
```java
// ❌ BAD - Client can fake userId
@PostMapping("/items")
public ItemResponse create(@RequestParam Long userId, ...) {
    // Client could send userId=999 to access others' data!
}

// ✅ GOOD - Get userId from authenticated user
@PostMapping("/items")
public ItemResponse create(@AuthenticationPrincipal UserDetailsImpl userDetails, ...) {
    Long userId = userDetails.getUser().getId();
    // Can't be faked - comes from validated JWT
}
```

**4. Not Validating Token on Every Request**
```java
// JwtAuthenticationFilter MUST run on every request
// SecurityConfig ensures this:
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

---

### Frontend Integration Example (Angular)

Here's how your Angular app will use the JWT:

```typescript
// auth.service.ts
@Injectable()
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  
  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, {
      email,
      password
    }).pipe(
      tap(response => {
        // Store token in localStorage
        localStorage.setItem('token', response.token);
        localStorage.setItem('userId', response.userId.toString());
      })
    );
  }
  
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
  }
  
  getToken(): string | null {
    return localStorage.getItem('token');
  }
}

// auth.interceptor.ts
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}
  
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getToken();
    
    if (token) {
      // Add Authorization header to every request
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    
    return next.handle(req);
  }
}
```

---

**Congratulations!** 🎉

You've now implemented a complete, production-ready authentication system with JWT! Your API is secure and ready for the frontend integration.

Next up: **Phase 8 - Comprehensive Testing Strategy** where we'll write integration tests, E2E tests, and achieve high test coverage!
