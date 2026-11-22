package com.techpulse.approval_workflow_engine.dto;


import java.time.LocalDateTime;

import com.techpulse.approval_workflow_engine.entity.ApprovalHistory;
import com.techpulse.approval_workflow_engine.entity.HistoryAction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HistoryResponseDTO {

    private Long id;
    private Long requestId;
    private HistoryAction action;
    private String actionBy;
    private LocalDateTime actionAt;

    public static HistoryResponseDTO from(ApprovalHistory h) {
        return new HistoryResponseDTO(
                h.getId(), h.getRequestId(), h.getAction(), h.getActionBy(), h.getActionAt());
    }
}