package com.techpulse.approval_workflow_engine.service;

import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.techpulse.approval_workflow_engine.dto.CreateRequestDTO;
import com.techpulse.approval_workflow_engine.dto.HistoryResponseDTO;
import com.techpulse.approval_workflow_engine.dto.RequestResponseDTO;
import com.techpulse.approval_workflow_engine.entity.ApprovalHistory;
import com.techpulse.approval_workflow_engine.entity.ApprovalStep;
import com.techpulse.approval_workflow_engine.entity.HistoryAction;
import com.techpulse.approval_workflow_engine.entity.Request;
import com.techpulse.approval_workflow_engine.entity.RequestStatus;
import com.techpulse.approval_workflow_engine.entity.Role;
import com.techpulse.approval_workflow_engine.globle_exception_handling.WorkflowException;
import com.techpulse.approval_workflow_engine.repository.ApprovalHistoryRepository;
import com.techpulse.approval_workflow_engine.repository.ApprovalStepRepository;
import com.techpulse.approval_workflow_engine.repository.RequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private static final Set<HistoryAction> APPROVAL_ACTIONS =
            Set.of(HistoryAction.APPROVED, HistoryAction.ADMIN_OVERRIDE_APPROVED);

    private final RequestRepository requestRepository;
    private final ApprovalStepRepository stepRepository;
    private final ApprovalHistoryRepository historyRepository;

    // ---------- create ----------
    @Transactional
    public RequestResponseDTO create(CreateRequestDTO dto, String username) {
        String type = dto.getType().trim().toUpperCase();

        ApprovalStep firstStep = stepRepository.findByRequestTypeAndStepOrder(type, 1)
                .orElseThrow(() -> WorkflowException.badRequest(
                        "No workflow configured for type " + type));

        Request request = new Request();
        request.setType(type);
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedBy(username);
        request = requestRepository.save(request);

        record(request.getId(), HistoryAction.CREATED, username);
        return RequestResponseDTO.from(request, firstStep);
    }

    // ---------- read ----------
    @Transactional(readOnly = true)
    public RequestResponseDTO getRequest(Long id, Authentication auth) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        assertCanView(request, auth);
        return RequestResponseDTO.from(request, currentStep(request));
    }

    @Transactional(readOnly = true)
    public List<RequestResponseDTO> getAllRequests(Authentication auth) {
        Role role = roleOf(auth);
        List<Request> requests = (role == Role.REQUESTER)
                ? requestRepository.findByCreatedByOrderByIdAsc(auth.getName())
                : requestRepository.findAllByOrderByIdAsc();

        return requests.stream()
                .map(r -> RequestResponseDTO.from(r, currentStep(r)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryResponseDTO> getHistory(Long id, Authentication auth) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        assertCanView(request, auth);
        return historyRepository.findByRequestIdOrderByIdAsc(id).stream()
                .map(HistoryResponseDTO::from)
                .toList();
    }

    // ---------- approve ----------
    @Transactional
    public RequestResponseDTO approve(Long id, Authentication auth) {
        Request request = loadPending(id);
        ApprovalStep step = currentStep(request);
        boolean override = authorize(request, step, auth);

        // One person cannot approve two different steps of the same request
        if (!roleOf(auth).canOverride()
                && historyRepository.existsByRequestIdAndActionByAndActionIn(
                        id, auth.getName(), APPROVAL_ACTIONS)) {
            throw WorkflowException.forbidden(
                    "You have already approved an earlier step of this request");
        }

        record(id, override ? HistoryAction.ADMIN_OVERRIDE_APPROVED : HistoryAction.APPROVED,
                auth.getName());

        boolean hasNext = stepRepository.existsByRequestTypeAndStepOrder(
                request.getType(), step.getStepOrder() + 1);
        if (!hasNext) {
            request.setStatus(RequestStatus.APPROVED);
            requestRepository.save(request);
        }
        return RequestResponseDTO.from(request, currentStep(request));
    }

    // ---------- reject ----------
    @Transactional
    public RequestResponseDTO reject(Long id, Authentication auth) {
        Request request = loadPending(id);
        ApprovalStep step = currentStep(request);
        boolean override = authorize(request, step, auth);

        record(id, override ? HistoryAction.ADMIN_OVERRIDE_REJECTED : HistoryAction.REJECTED,
                auth.getName());

        request.setStatus(RequestStatus.REJECTED);
        requestRepository.save(request);
        return RequestResponseDTO.from(request, null);
    }

    // ---------- helpers ----------
    private Request loadPending(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw WorkflowException.conflict("Request is already " + request.getStatus());
        }
        return request;
    }

    /** Current step = (approvals so far) + 1, looked up in the DB config. Null once finished. */
    private ApprovalStep currentStep(Request request) {
        if (request.getStatus() != RequestStatus.PENDING) {
            return null;
        }
        long approvals = historyRepository.countByRequestIdAndActionIn(
                request.getId(), APPROVAL_ACTIONS);
        return stepRepository.findByRequestTypeAndStepOrder(request.getType(), (int) approvals + 1)
                .orElseThrow(() -> WorkflowException.badRequest(
                        "Workflow configuration for " + request.getType() + " is incomplete"));
    }

    /** Returns true when the action is an admin override (acting outside the step's role). */
    private boolean authorize(Request request, ApprovalStep step, Authentication auth) {
        if (request.getCreatedBy().equals(auth.getName())) {
            throw WorkflowException.forbidden("You cannot act on your own request");
        }
        Role userRole = roleOf(auth);
        if (userRole == step.getRole()) {
            return false;
        }
        if (userRole.canOverride()) {
            return true;
        }
        throw WorkflowException.forbidden("Current step requires role " + step.getRole());
    }

    /** Requesters see only their own requests. Approvers and admins see everything. */
    private void assertCanView(Request request, Authentication auth) {
        Role role = roleOf(auth);
        boolean owner = request.getCreatedBy().equals(auth.getName());
        if (!owner && role != Role.APPROVER && !role.canOverride()) {
            throw WorkflowException.forbidden("You can only view your own requests");
        }
    }

    private Role roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> Role.valueOf(a.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> WorkflowException.forbidden("No role assigned"));
    }

    private void record(Long requestId, HistoryAction action, String actionBy) {
        historyRepository.save(ApprovalHistory.builder()
                .requestId(requestId)
                .action(action)
                .actionBy(actionBy)
                .build());
    }

    private WorkflowException notFound(Long id) {
        return WorkflowException.notFound("Request " + id + " not found");
    }
}