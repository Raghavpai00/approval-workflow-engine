package com.techpulse.approval_workflow_engine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceTest {

    @Mock RequestRepository requestRepository;
    @Mock ApprovalStepRepository stepRepository;
    @Mock ApprovalHistoryRepository historyRepository;
    @InjectMocks WorkflowService service;

    // ---------- helpers ----------
    private Authentication auth(String user, Role role) {
        return new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    private ApprovalStep step(String type, int order, Role role) {
        ApprovalStep s = new ApprovalStep();
        s.setRequestType(type);
        s.setStepOrder(order);
        s.setRole(role);
        return s;
    }

    private Request request(long id, String type, RequestStatus status, String createdBy) {
        Request r = new Request();
        r.setId(id);
        r.setType(type);
        r.setStatus(status);
        r.setCreatedBy(createdBy);
        return r;
    }

    private void stubRequest(Request r) {
        when(requestRepository.findById(r.getId())).thenReturn(Optional.of(r));
    }

    private void stubStep(String type, int order, Role role) {
        when(stepRepository.findByRequestTypeAndStepOrder(type, order))
                .thenReturn(Optional.of(step(type, order, role)));
    }

    private void assertStatus(Throwable e, HttpStatus expected) {
        assertThat(e).isInstanceOf(WorkflowException.class);
        assertThat(((WorkflowException) e).getStatus()).isEqualTo(expected);
    }

    // ---------- create ----------
    @Test
    void create_success_returnsFirstStep() {
        stubStep("LEAVE", 1, Role.APPROVER);
        when(requestRepository.save(any(Request.class))).thenAnswer(i -> {
            Request r = i.getArgument(0);
            r.setId(1L);
            return r;
        });

        RequestResponseDTO res = service.create(new CreateRequestDTO("leave"), "rahul");

        assertThat(res.getType()).isEqualTo("LEAVE");
        assertThat(res.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(res.getCurrentStep()).isEqualTo(1);
        assertThat(res.getCurrentStepRole()).isEqualTo(Role.APPROVER);

        ArgumentCaptor<ApprovalHistory> cap = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(historyRepository).save(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo(HistoryAction.CREATED);
        assertThat(cap.getValue().getActionBy()).isEqualTo("rahul");
    }

    @Test
    void create_unknownType_throws400() {
        when(stepRepository.findByRequestTypeAndStepOrder(anyString(), eq(1)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateRequestDTO("PURCHASE_ORDER"), "rahul"))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
    }

    // ---------- getRequest ----------
    @Test
    void getRequest_owner_pending_showsStep() {
        stubRequest(request(1, "LEAVE", RequestStatus.PENDING, "rahul"));
        stubStep("LEAVE", 1, Role.APPROVER);
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection())).thenReturn(0L);

        RequestResponseDTO res = service.getRequest(1L, auth("rahul", Role.REQUESTER));

        assertThat(res.getCurrentStep()).isEqualTo(1);
    }

    @Test
    void getRequest_finished_hasNoStep() {
        stubRequest(request(1, "LEAVE", RequestStatus.APPROVED, "rahul"));

        RequestResponseDTO res = service.getRequest(1L, auth("rahul", Role.REQUESTER));

        assertThat(res.getCurrentStep()).isNull();
        assertThat(res.getCurrentStepRole()).isNull();
    }

    @Test
    void getRequest_notFound_throws404() {
        when(requestRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRequest(9L, auth("rahul", Role.REQUESTER)))
                .satisfies(e -> assertStatus(e, HttpStatus.NOT_FOUND));
    }

    @Test
    void getRequest_otherRequester_forbidden() {
        stubRequest(request(1, "LEAVE", RequestStatus.APPROVED, "rahul"));

        assertThatThrownBy(() -> service.getRequest(1L, auth("sneha", Role.REQUESTER)))
                .satisfies(e -> assertStatus(e, HttpStatus.FORBIDDEN));
    }

    @Test
    void getRequest_approverAndAdmin_canViewOthers() {
        stubRequest(request(1, "LEAVE", RequestStatus.APPROVED, "rahul"));

        assertThat(service.getRequest(1L, auth("anita", Role.APPROVER)).getId()).isEqualTo(1L);
        assertThat(service.getRequest(1L, auth("admin", Role.ADMIN)).getId()).isEqualTo(1L);
    }

    // ---------- getAllRequests ----------
    @Test
    void getAllRequests_requester_seesOwnOnly() {
        when(requestRepository.findByCreatedByOrderByIdAsc("rahul"))
                .thenReturn(List.of(request(1, "LEAVE", RequestStatus.APPROVED, "rahul")));

        List<RequestResponseDTO> res = service.getAllRequests(auth("rahul", Role.REQUESTER));

        assertThat(res).hasSize(1);
        verify(requestRepository).findByCreatedByOrderByIdAsc("rahul");
    }

    @Test
    void getAllRequests_approver_seesAll() {
        when(requestRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                request(1, "LEAVE", RequestStatus.APPROVED, "rahul"),
                request(2, "EXPENSE", RequestStatus.REJECTED, "sneha")));

        assertThat(service.getAllRequests(auth("anita", Role.APPROVER))).hasSize(2);
        verify(requestRepository).findAllByOrderByIdAsc();
    }

    // ---------- getHistory ----------
    @Test
    void getHistory_returnsRows() {
        stubRequest(request(1, "LEAVE", RequestStatus.APPROVED, "rahul"));
        when(historyRepository.findByRequestIdOrderByIdAsc(1L)).thenReturn(List.of(
                ApprovalHistory.builder().id(1L).requestId(1L).action(HistoryAction.CREATED)
                        .actionBy("rahul").build()));

        List<HistoryResponseDTO> res = service.getHistory(1L, auth("rahul", Role.REQUESTER));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getAction()).isEqualTo(HistoryAction.CREATED);
    }

    @Test
    void getHistory_notFound_throws404() {
        when(requestRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getHistory(5L, auth("admin", Role.ADMIN)))
                .satisfies(e -> assertStatus(e, HttpStatus.NOT_FOUND));
    }

    // ---------- approve ----------
    @Test
    void approve_movesToNextStep() {
        stubRequest(request(1, "LEAVE", RequestStatus.PENDING, "rahul"));
        stubStep("LEAVE", 1, Role.APPROVER);
        stubStep("LEAVE", 2, Role.ADMIN);
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection()))
                .thenReturn(0L, 1L);
        when(stepRepository.existsByRequestTypeAndStepOrder("LEAVE", 2)).thenReturn(true);

        RequestResponseDTO res = service.approve(1L, auth("anita", Role.APPROVER));

        assertThat(res.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(res.getCurrentStep()).isEqualTo(2);
        assertThat(res.getCurrentStepRole()).isEqualTo(Role.ADMIN);

        ArgumentCaptor<ApprovalHistory> cap = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(historyRepository).save(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo(HistoryAction.APPROVED);
    }

    @Test
    void approve_lastStep_completesWorkflow() {
        Request r = request(1, "LEAVE", RequestStatus.PENDING, "rahul");
        stubRequest(r);
        stubStep("LEAVE", 2, Role.ADMIN);
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection())).thenReturn(1L);
        when(stepRepository.existsByRequestTypeAndStepOrder("LEAVE", 3)).thenReturn(false);

        RequestResponseDTO res = service.approve(1L, auth("admin", Role.ADMIN));

        assertThat(res.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(res.getCurrentStep()).isNull();
        verify(requestRepository).save(r);
    }

    @Test
    void approve_adminAtApproverStep_isOverride() {
        stubRequest(request(1, "LEAVE", RequestStatus.PENDING, "rahul"));
        stubStep("LEAVE", 1, Role.APPROVER);
        stubStep("LEAVE", 2, Role.ADMIN);
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection()))
                .thenReturn(0L, 1L);
        when(stepRepository.existsByRequestTypeAndStepOrder("LEAVE", 2)).thenReturn(true);

        service.approve(1L, auth("admin", Role.ADMIN));

        ArgumentCaptor<ApprovalHistory> cap = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(historyRepository).save(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo(HistoryAction.ADMIN_OVERRIDE_APPROVED);
    }

    @Test
    void approve_wrongRoleForStep_forbidden() {
        stubRequest(request(1, "LEAVE", RequestStatus.PENDING, "rahul"));
        stubStep("LEAVE", 2, Role.ADMIN);
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection())).thenReturn(1L);

        assertThatThrownBy(() -> service.approve(1L, auth("anita", Role.APPROVER)))
                .satisfies(e -> assertStatus(e, HttpStatus.FORBIDDEN))
                .hasMessageContaining("requires role ADMIN");
    }

    @Test
    void approve_ownRequest_forbidden() {
        stubRequest(request(1, "LEAVE", RequestStatus.PENDING, "anita"));
        stubStep("LEAVE", 1, Role.APPROVER);
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection())).thenReturn(0L);

        assertThatThrownBy(() -> service.approve(1L, auth("anita", Role.APPROVER)))
                .hasMessageContaining("own request");
    }

    @Test
    void approve_sameApproverTwice_forbidden() {
        stubRequest(request(1, "EXPENSE", RequestStatus.PENDING, "rahul"));
        stubStep("EXPENSE", 2, Role.APPROVER);
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection())).thenReturn(1L);
        when(historyRepository.existsByRequestIdAndActionByAndActionIn(eq(1L), eq("anita"),
                anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.approve(1L, auth("anita", Role.APPROVER)))
                .hasMessageContaining("already approved");
    }

    @Test
    void approve_finishedRequest_conflict() {
        stubRequest(request(1, "LEAVE", RequestStatus.APPROVED, "rahul"));

        assertThatThrownBy(() -> service.approve(1L, auth("admin", Role.ADMIN)))
                .satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    @Test
    void approve_notFound_throws404() {
        when(requestRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(7L, auth("admin", Role.ADMIN)))
                .hasMessageContaining("not found");
    }

    @Test
    void approve_incompleteConfig_throws400() {
        stubRequest(request(1, "LEAVE", RequestStatus.PENDING, "rahul"));
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection())).thenReturn(5L);
        when(stepRepository.findByRequestTypeAndStepOrder("LEAVE", 6)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(1L, auth("admin", Role.ADMIN)))
                .hasMessageContaining("incomplete");
    }

    @Test
    void approve_noRoleAuthority_forbidden() {
        stubRequest(request(1, "LEAVE", RequestStatus.PENDING, "rahul"));
        stubStep("LEAVE", 1, Role.APPROVER);
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection())).thenReturn(0L);
        Authentication noRole = new UsernamePasswordAuthenticationToken("x", null, List.of());

        assertThatThrownBy(() -> service.approve(1L, noRole))
                .hasMessageContaining("No role");
    }

    // ---------- reject ----------
    @Test
    void reject_success() {
        stubRequest(request(1, "LEAVE", RequestStatus.PENDING, "rahul"));
        stubStep("LEAVE", 1, Role.APPROVER);
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection())).thenReturn(0L);

        RequestResponseDTO res = service.reject(1L, auth("anita", Role.APPROVER));

        assertThat(res.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(res.getCurrentStep()).isNull();
        ArgumentCaptor<ApprovalHistory> cap = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(historyRepository).save(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo(HistoryAction.REJECTED);
    }

    @Test
    void reject_adminOverride() {
        stubRequest(request(1, "LEAVE", RequestStatus.PENDING, "rahul"));
        stubStep("LEAVE", 1, Role.APPROVER);
        when(historyRepository.countByRequestIdAndActionIn(anyLong(), anyCollection())).thenReturn(0L);

        service.reject(1L, auth("admin", Role.ADMIN));

        ArgumentCaptor<ApprovalHistory> cap = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(historyRepository).save(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo(HistoryAction.ADMIN_OVERRIDE_REJECTED);
    }

    @Test
    void reject_finishedRequest_conflict() {
        stubRequest(request(1, "LEAVE", RequestStatus.REJECTED, "rahul"));

        assertThatThrownBy(() -> service.reject(1L, auth("anita", Role.APPROVER)))
                .hasMessageContaining("already REJECTED");
    }
}