
package com.techpulse.approval_workflow_engine.globle_exception_handling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.techpulse.approval_workflow_engine.dto.ErrorResponseDTO;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);

        when(request.getRequestURI()).thenReturn("/requests/1");
    }

    @Test
    void handleWorkflow_shouldReturnExceptionStatus() {
        WorkflowException exception =
                WorkflowException.notFound("Request not found");

        ResponseEntity<ErrorResponseDTO> response =
                handler.handleWorkflow(exception, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleValidation_shouldReturnBadRequest() {
        BindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");

        bindingResult.addError(
                new FieldError("request", "title", "Title is required"));

        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponseDTO> response =
                handler.handleValidation(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleUnreadable_shouldReturnBadRequest() {
        HttpMessageNotReadableException exception =
                mock(HttpMessageNotReadableException.class);

        ResponseEntity<ErrorResponseDTO> response =
                handler.handleUnreadable(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleAccessDenied_shouldReturnForbidden() {
        AccessDeniedException exception =
                mock(AccessDeniedException.class);

        ResponseEntity<ErrorResponseDTO> response =
                handler.handleAccessDenied(exception, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleBadCredentials_shouldReturnUnauthorized() {
        BadCredentialsException exception =
                mock(BadCredentialsException.class);

        ResponseEntity<ErrorResponseDTO> response =
                handler.handleBadCredentials(exception, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleUnsupportedMedia_shouldReturnUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException exception =
                mock(HttpMediaTypeNotSupportedException.class);

        ResponseEntity<ErrorResponseDTO> response =
                handler.handleUnsupportedMedia(exception, request);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                response.getStatusCode());

        assertNotNull(response.getBody());
    }

    @Test
    void handleUnexpected_shouldReturnInternalServerError() {
        Exception exception = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponseDTO> response =
                handler.handleUnexpected(exception, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());

        assertNotNull(response.getBody());
    }
}
