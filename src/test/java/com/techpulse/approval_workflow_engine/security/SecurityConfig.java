
package com.techpulse.approval_workflow_engine.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Test
    void passwordEncoder_shouldEncodeAndMatchPassword() {

        String encodedPassword = passwordEncoder.encode("password123");

        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches("password123", encodedPassword));
    }

    @Test
    void filterChain_shouldLoadSuccessfully() {

        assertNotNull(springSecurityFilterChain);
        assertTrue(!springSecurityFilterChain.getFilterChains().isEmpty());
    }
}
