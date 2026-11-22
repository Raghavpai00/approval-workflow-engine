
package com.techpulse.approval_workflow_engine.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.techpulse.approval_workflow_engine.dto.CreateRequestDTO;
import com.techpulse.approval_workflow_engine.dto.HistoryResponseDTO;
import com.techpulse.approval_workflow_engine.dto.RequestResponseDTO;
import com.techpulse.approval_workflow_engine.service.WorkflowService;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private RequestController requestController;

    private Authentication auth;
    private CreateRequestDTO createRequestDTO;
    private RequestResponseDTO requestResponseDTO;
    private HistoryResponseDTO historyResponseDTO;

    @BeforeEach
    void setUp() {
        auth = mock(Authentication.class);
        createRequestDTO = mock(CreateRequestDTO.class);
        requestResponseDTO = mock(RequestResponseDTO.class);
        historyResponseDTO = mock(HistoryResponseDTO.class);
    }

    @Test
    void create_shouldReturnCreatedResponse() {

        when(auth.getName()).thenReturn("requester");

        when(workflowService.create(createRequestDTO, "requester"))
                .thenReturn(requestResponseDTO);

        ResponseEntity<RequestResponseDTO> response =
                requestController.create(createRequestDTO, auth);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(requestResponseDTO, response.getBody());

        verify(workflowService).create(createRequestDTO, "requester");
    }

    @Test
    void get_shouldReturnRequest() {

        when(workflowService.getRequest(1L, auth))
                .thenReturn(requestResponseDTO);

        ResponseEntity<RequestResponseDTO> response =
                requestController.get(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(requestResponseDTO, response.getBody());

        verify(workflowService).getRequest(1L, auth);
    }

    @Test
    void approve_shouldReturnApprovedRequest() {

        when(workflowService.approve(1L, auth))
                .thenReturn(requestResponseDTO);

        ResponseEntity<RequestResponseDTO> response =
                requestController.approve(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(requestResponseDTO, response.getBody());

        verify(workflowService).approve(1L, auth);
    }

    @Test
    void reject_shouldReturnRejectedRequest() {

        when(workflowService.reject(1L, auth))
                .thenReturn(requestResponseDTO);

        ResponseEntity<RequestResponseDTO> response =
                requestController.reject(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(requestResponseDTO, response.getBody());

        verify(workflowService).reject(1L, auth);
    }

    @Test
    void history_shouldReturnRequestHistory() {

        List<HistoryResponseDTO> historyList =
                List.of(historyResponseDTO);

        when(workflowService.getHistory(1L, auth))
                .thenReturn(historyList);

        ResponseEntity<List<HistoryResponseDTO>> response =
                requestController.history(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(historyList, response.getBody());

        verify(workflowService).getHistory(1L, auth);
    }

    @Test
    void getAll_shouldReturnAllRequests() {

        List<RequestResponseDTO> requestList =
                List.of(requestResponseDTO);

        when(workflowService.getAllRequests(auth))
                .thenReturn(requestList);

        ResponseEntity<List<RequestResponseDTO>> response =
                requestController.getAll(auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(requestList, response.getBody());

        verify(workflowService).getAllRequests(auth);
    }
}
