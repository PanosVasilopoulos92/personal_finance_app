package org.viators.personal_finance_app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.viators.personal_finance_app.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Getter
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * .claims(claims) → Adds your custom data
     * .subject(user.getEmail()) → The "subject" is the primary identifier (email here)
     * .issuedAt(new Date()) → Timestamp when token was created
     * .expiration(...) → When the token expires
     * .signWith(getSignedKey()) → Signs the token so it can't be tampered with
     * .compact() → Converts to the final JWT string format
     */
    public String generateToken(User user) {
        // claims are the "payload" of JWT - the actual data you want to carry in the token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userUuid", user.getUuid());
        claims.put("username", user.getUsername());
        claims.put("role", user.getUserRole());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignedKey())
                .compact();
    }

    /**
     * Extracts email (subject) from token.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractUserUuid(String token) {
        return extractAllClaims(token).get("userUuid", String.class);
    }

    /**
     * Validates token (not expired, valid signature).
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
        - Creates a JWT parser
        - Tells it to verify using your secret key
        - Builds the parser
        - Parses the token (this validates signature AND checks expiration)
        - Gets the payload (all the claims)
    */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignedKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
        - Converts your string secret into a proper cryptographic key
        - Uses UTF-8 encoding to ensure consistency
        - HMAC-SHA is the signing algorithm
     */
    private SecretKey getSignedKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
