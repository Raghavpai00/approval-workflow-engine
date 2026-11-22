
package com.techpulse.approval_workflow_engine.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.techpulse.approval_workflow_engine.dto.AuthResponseDTO;
import com.techpulse.approval_workflow_engine.dto.LoginRequestDTO;
import com.techpulse.approval_workflow_engine.entity.Role;
import com.techpulse.approval_workflow_engine.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private LoginRequestDTO loginRequest;
    private AuthResponseDTO authResponse;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password123");

        authResponse = AuthResponseDTO.of("test-jwt-token", Role.ADMIN);
    }

    @Test
    void login_shouldReturnOkResponse() {

        when(authService.login(loginRequest)).thenReturn(authResponse);

        ResponseEntity<AuthResponseDTO> response =
                authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(authService).login(loginRequest);
    }

    @Test
    void login_shouldReturnServiceResponse() {

        when(authService.login(loginRequest)).thenReturn(authResponse);

        ResponseEntity<AuthResponseDTO> response =
                authController.login(loginRequest);

        assertEquals(authResponse, response.getBody());

        verify(authService).login(loginRequest);
    }
}
