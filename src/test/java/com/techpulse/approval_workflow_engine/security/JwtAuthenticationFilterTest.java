
package com.techpulse.approval_workflow_engine.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        filter = new JwtAuthenticationFilter(jwtUtil);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_withoutAuthorizationHeader_shouldContinueChain()
            throws Exception {

        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verify(jwtUtil, never()).isValid(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilterInternal_withNonBearerHeader_shouldContinueChain()
            throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
        verify(jwtUtil, never()).isValid(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doFilterInternal_withInvalidToken_shouldNotAuthenticate()
            throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtil.isValid("invalid-token")).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil).isValid("invalid-token");
        verify(jwtUtil, never()).parse("invalid-token");
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withValidToken_shouldAuthenticateUser()
            throws Exception {

        Claims claims = mock(Claims.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.isValid("valid-token")).thenReturn(true);
        when(jwtUtil.parse("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("admin");
        when(claims.get("role", String.class)).thenReturn("ADMIN");

        filter.doFilterInternal(request, response, chain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertEquals("admin", authentication.getName());
        assertEquals("ROLE_ADMIN",
                authentication.getAuthorities().iterator().next().getAuthority());

        verify(jwtUtil).isValid("valid-token");
        verify(jwtUtil).parse("valid-token");
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withRequesterToken_shouldSetRequesterRole()
            throws Exception {

        Claims claims = mock(Claims.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer requester-token");
        when(jwtUtil.isValid("requester-token")).thenReturn(true);
        when(jwtUtil.parse("requester-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("requester");
        when(claims.get("role", String.class)).thenReturn("REQUESTER");

        filter.doFilterInternal(request, response, chain);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        assertEquals("requester", authentication.getName());
        assertEquals("ROLE_REQUESTER",
                authentication.getAuthorities().iterator().next().getAuthority());

        verify(chain).doFilter(request, response);
    }
}
