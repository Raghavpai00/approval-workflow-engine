package com.techpulse.approval_workflow_engine.controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.techpulse.approval_workflow_engine.dto.CreateRequestDTO;
import com.techpulse.approval_workflow_engine.dto.HistoryResponseDTO;
import com.techpulse.approval_workflow_engine.dto.RequestResponseDTO;
import com.techpulse.approval_workflow_engine.service.WorkflowService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {

    private final WorkflowService workflowService;

    @PostMapping
    @PreAuthorize("hasRole('REQUESTER')")
    public ResponseEntity<RequestResponseDTO> create(@Valid @RequestBody CreateRequestDTO dto,
                                                     Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowService.create(dto, auth.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('REQUESTER','APPROVER','ADMIN')")
    public ResponseEntity<RequestResponseDTO> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(workflowService.getRequest(id, auth));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public ResponseEntity<RequestResponseDTO> approve(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(workflowService.approve(id, auth));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    public ResponseEntity<RequestResponseDTO> reject(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(workflowService.reject(id, auth));
    }

    @GetMapping("/history/{id}")
    @PreAuthorize("hasAnyRole('REQUESTER','ADMIN')")
    public ResponseEntity<List<HistoryResponseDTO>> history(@PathVariable Long id,
                                                            Authentication auth) {
        return ResponseEntity.ok(workflowService.getHistory(id, auth));
    }
    
    @GetMapping
    @PreAuthorize("hasAnyRole('REQUESTER','APPROVER','ADMIN')")
    public ResponseEntity<List<RequestResponseDTO>> getAll(Authentication auth) {
        return ResponseEntity.ok(workflowService.getAllRequests(auth));
    }
    
}
