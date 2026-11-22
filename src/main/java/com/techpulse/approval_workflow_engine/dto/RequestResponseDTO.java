package com.techpulse.approval_workflow_engine.dto;


import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.techpulse.approval_workflow_engine.entity.ApprovalStep;
import com.techpulse.approval_workflow_engine.entity.Request;
import com.techpulse.approval_workflow_engine.entity.RequestStatus;
import com.techpulse.approval_workflow_engine.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestResponseDTO {

    private Long id;
    private String type;
    private RequestStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private Integer currentStep;     // null once the workflow is finished
    private Role currentStepRole;    // null once the workflow is finished

    /** currentStep is null when the request is APPROVED or REJECTED. */
    public static RequestResponseDTO from(Request request, ApprovalStep currentStep) {
        return new RequestResponseDTO(
                request.getId(),
                request.getType(),
                request.getStatus(),
                request.getCreatedBy(),
                request.getCreatedAt(),
                currentStep == null ? null : currentStep.getStepOrder(),
                currentStep == null ? null : currentStep.getRole()
        );
    }
}
