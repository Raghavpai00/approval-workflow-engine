package com.techpulse.approval_workflow_engine.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.techpulse.approval_workflow_engine.entity.ApprovalHistory;
import com.techpulse.approval_workflow_engine.entity.HistoryAction;

public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {

    List<ApprovalHistory> findByRequestIdOrderByIdAsc(Long requestId);

    // current step = this count + 1
    long countByRequestIdAndActionIn(Long requestId, Collection<HistoryAction> actions);
    
    
    boolean existsByRequestIdAndActionByAndActionIn(Long requestId, String actionBy,
            Collection<HistoryAction> actions);
}