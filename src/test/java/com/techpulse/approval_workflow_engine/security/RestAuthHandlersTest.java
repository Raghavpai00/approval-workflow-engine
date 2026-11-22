
package com.techpulse.approval_workflow_engine.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class RestAuthHandlersTest {

    private RestAuthHandlers handlers;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter stringWriter;

    @BeforeEach
    void setUp() throws Exception {
        handlers = new RestAuthHandlers();

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        stringWriter = new StringWriter();

        when(request.getRequestURI()).thenReturn("/requests/1");

        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
    }

    @Test
    void commence_shouldReturnUnauthorizedResponse() throws Exception {

        handlers.commence(
                request,
                response,
                new BadCredentialsException("Invalid token"));

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        String body = stringWriter.toString();

        assertTrue(body.contains("\"status\":401"));
        assertTrue(body.contains("\"error\":\"Unauthorized\""));
        assertTrue(body.contains("Authentication token is missing or invalid"));
        assertTrue(body.contains("\"path\":\"/requests/1\""));
    }

    @Test
    void handle_shouldReturnForbiddenResponse() throws Exception {

        handlers.handle(
                request,
                response,
                new AccessDeniedException("Access denied"));

        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        String body = stringWriter.toString();

        assertTrue(body.contains("\"status\":403"));
        assertTrue(body.contains("\"error\":\"Forbidden\""));
        assertTrue(body.contains("You do not have permission to perform this action"));
        assertTrue(body.contains("\"path\":\"/requests/1\""));
    }

    @Test
    void commence_shouldEscapeQuotesInRequestPath() throws Exception {

        when(request.getRequestURI()).thenReturn("/requests/\"test\"");

        handlers.commence(
                request,
                response,
                new BadCredentialsException("Invalid token"));

        String body = stringWriter.toString();

        assertTrue(body.contains("/requests/\\\"test\\\""));
    }

    @Test
    void commence_shouldEscapeBackslashesInRequestPath() throws Exception {

        when(request.getRequestURI()).thenReturn("/requests\\test");

        handlers.commence(
                request,
                response,
                new BadCredentialsException("Invalid token"));

        String body = stringWriter.toString();

        assertTrue(body.contains("/requests\\\\test"));
    }
}
