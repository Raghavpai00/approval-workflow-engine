
package com.techpulse.approval_workflow_engine.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.techpulse.approval_workflow_engine.entity.Role;

import io.jsonwebtoken.Claims;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET =
            "my-test-secret-key-for-jwt-token-generation-123456";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3600000);
    }

    @Test
    void generateToken_shouldReturnToken() {
        String token = jwtUtil.generateToken("admin", Role.ADMIN);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void parse_shouldReturnCorrectClaims() {
        String token = jwtUtil.generateToken("admin", Role.ADMIN);

        Claims claims = jwtUtil.parse(token);

        assertEquals("admin", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void isValid_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("requester", Role.REQUESTER);

        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    void isValid_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.isValid("invalid.jwt.token"));
    }

    @Test
    void isValid_shouldReturnFalseForEmptyToken() {
        assertFalse(jwtUtil.isValid(""));
    }

    @Test
    void isValid_shouldReturnFalseForExpiredToken() {
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, -1000);

        String token = expiredJwtUtil.generateToken("admin", Role.ADMIN);

        assertFalse(expiredJwtUtil.isValid(token));
    }

    @Test
    void parse_shouldReturnRequesterRole() {
        String token = jwtUtil.generateToken("user1", Role.REQUESTER);

        Claims claims = jwtUtil.parse(token);

        assertEquals("user1", claims.getSubject());
        assertEquals("REQUESTER", claims.get("role", String.class));
    }
}
