
package com.techpulse.approval_workflow_engine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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
class WorkflowServiceAdditionalTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private ApprovalStepRepository stepRepository;

    @Mock
    private ApprovalHistoryRepository historyRepository;

    @InjectMocks
    private WorkflowService service;

    private Authentication auth(String username, Role role) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }

    private Request request(Long id, String type,
                            RequestStatus status, String createdBy) {
        Request request = new Request();
        request.setId(id);
        request.setType(type);
        request.setStatus(status);
        request.setCreatedBy(createdBy);
        return request;
    }

    private ApprovalStep step(String type, int order, Role role) {
        ApprovalStep step = new ApprovalStep();
        step.setRequestType(type);
        step.setStepOrder(order);
        step.setRole(role);
        return step;
    }

    // 1. Admin can view all requests
    @Test
    void getAllRequests_adminCanViewAll() {

        when(requestRepository.findAllByOrderByIdAsc())
                .thenReturn(List.of(
                        request(1L, "LEAVE",
                                RequestStatus.APPROVED, "rahul"),
                        request(2L, "EXPENSE",
                                RequestStatus.REJECTED, "sneha")
                ));

        var result = service.getAllRequests(auth("admin", Role.ADMIN));

        assertThat(result).hasSize(2);

        verify(requestRepository).findAllByOrderByIdAsc();
    }

    // 2. Requester cannot view another user's history
    @Test
    void getHistory_otherRequesterForbidden() {

        when(requestRepository.findById(1L))
                .thenReturn(Optional.of(
                        request(1L, "LEAVE",
                                RequestStatus.APPROVED, "rahul")
                ));

        assertThatThrownBy(() ->
                service.getHistory(1L, auth("sneha", Role.REQUESTER))
        )
                .isInstanceOf(WorkflowException.class)
                .satisfies(exception ->
                        assertThat(((WorkflowException) exception).getStatus())
                                .isEqualTo(HttpStatus.FORBIDDEN)
                );
    }

    // 3. Approver can view another user's history
    @Test
    void getHistory_approverCanView() {

        when(requestRepository.findById(1L))
                .thenReturn(Optional.of(
                        request(1L, "LEAVE",
                                RequestStatus.APPROVED, "rahul")
                ));

        when(historyRepository.findByRequestIdOrderByIdAsc(1L))
                .thenReturn(List.of(
                        ApprovalHistory.builder()
                                .id(1L)
                                .requestId(1L)
                                .action(HistoryAction.CREATED)
                                .actionBy("rahul")
                                .build()
                ));

        var result = service.getHistory(
                1L, auth("anita", Role.APPROVER));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction())
                .isEqualTo(HistoryAction.CREATED);
    }

    // 4. Requester cannot approve an approver's step
    @Test
    void approve_requesterWrongRoleForbidden() {

        when(requestRepository.findById(1L))
                .thenReturn(Optional.of(
                        request(1L, "LEAVE",
                                RequestStatus.PENDING, "rahul")
                ));

        when(historyRepository.countByRequestIdAndActionIn(
                anyLong(), anyCollection()))
                .thenReturn(0L);

        when(stepRepository.findByRequestTypeAndStepOrder("LEAVE", 1))
                .thenReturn(Optional.of(
                        step("LEAVE", 1, Role.APPROVER)
                ));

        assertThatThrownBy(() ->
                service.approve(1L, auth("sneha", Role.REQUESTER))
        )
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("requires role");
    }

    // 5. Admin can override the duplicate-approval restriction
    @Test
    void approve_adminCanOverrideDuplicateApprovalRestriction() {

        Request request = request(
                1L, "LEAVE", RequestStatus.PENDING, "rahul");

        when(requestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        when(historyRepository.countByRequestIdAndActionIn(
                anyLong(), anyCollection()))
                .thenReturn(0L);

        when(stepRepository.findByRequestTypeAndStepOrder("LEAVE", 1))
                .thenReturn(Optional.of(
                        step("LEAVE", 1, Role.APPROVER)
                ));

     

        when(stepRepository.existsByRequestTypeAndStepOrder(
                "LEAVE", 2))
                .thenReturn(false);

        var result = service.approve(
                1L, auth("admin", Role.ADMIN));

        assertThat(result.getStatus())
                .isEqualTo(RequestStatus.APPROVED);

        ArgumentCaptor<ApprovalHistory> captor =
                ArgumentCaptor.forClass(ApprovalHistory.class);

        verify(historyRepository).save(captor.capture());

        assertThat(captor.getValue().getAction())
                .isEqualTo(HistoryAction.ADMIN_OVERRIDE_APPROVED);
    }

    // 6. User cannot reject their own request
    @Test
    void reject_ownRequestForbidden() {

        when(requestRepository.findById(1L))
                .thenReturn(Optional.of(
                        request(1L, "LEAVE",
                                RequestStatus.PENDING, "rahul")
                ));

        when(historyRepository.countByRequestIdAndActionIn(
                anyLong(), anyCollection()))
                .thenReturn(0L);

        when(stepRepository.findByRequestTypeAndStepOrder("LEAVE", 1))
                .thenReturn(Optional.of(
                        step("LEAVE", 1, Role.APPROVER)
                ));

        assertThatThrownBy(() ->
                service.reject(1L, auth("rahul", Role.REQUESTER))
        )
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("own request");
    }

    // 7. Wrong role cannot reject a request
    @Test
    void reject_wrongRoleForbidden() {

        when(requestRepository.findById(1L))
                .thenReturn(Optional.of(
                        request(1L, "LEAVE",
                                RequestStatus.PENDING, "rahul")
                ));

        when(historyRepository.countByRequestIdAndActionIn(
                anyLong(), anyCollection()))
                .thenReturn(0L);

        when(stepRepository.findByRequestTypeAndStepOrder("LEAVE", 1))
                .thenReturn(Optional.of(
                        step("LEAVE", 1, Role.APPROVER)
                ));

        assertThatThrownBy(() ->
                service.reject(1L, auth("sneha", Role.REQUESTER))
        )
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("requires role");
    }

    // 8. Reject request that does not exist
    @Test
    void reject_requestNotFound() {

        when(requestRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.reject(99L, auth("admin", Role.ADMIN))
        )
                .isInstanceOf(WorkflowException.class)
                .satisfies(exception ->
                        assertThat(((WorkflowException) exception).getStatus())
                                .isEqualTo(HttpStatus.NOT_FOUND)
                );
    }

    // 9. Reject when user has no role authority
    @Test
    void reject_noRoleAuthorityForbidden() {

        when(requestRepository.findById(1L))
                .thenReturn(Optional.of(
                        request(1L, "LEAVE",
                                RequestStatus.PENDING, "rahul")
                ));

        when(historyRepository.countByRequestIdAndActionIn(
                anyLong(), anyCollection()))
                .thenReturn(0L);

        when(stepRepository.findByRequestTypeAndStepOrder("LEAVE", 1))
                .thenReturn(Optional.of(
                        step("LEAVE", 1, Role.APPROVER)
                ));

        Authentication noRole =
                new UsernamePasswordAuthenticationToken(
                        "sneha", null, List.of());

        assertThatThrownBy(() ->
                service.reject(1L, noRole)
        )
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("No role assigned");
    }

    // 10. User without a role cannot view requests
    @Test
    void getRequest_noRoleAuthorityForbidden() {

        when(requestRepository.findById(1L))
                .thenReturn(Optional.of(
                        request(1L, "LEAVE",
                                RequestStatus.APPROVED, "rahul")
                ));

        Authentication noRole =
                new UsernamePasswordAuthenticationToken(
                        "sneha", null, List.of());

        assertThatThrownBy(() ->
                service.getRequest(1L, noRole)
        )
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("No role assigned");
    }
}
