package com.techpulse.approval_workflow_engine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.techpulse.approval_workflow_engine.entity.ApprovalStep;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    List<ApprovalStep> findByRequestTypeOrderByStepOrder(String requestType);

    Optional<ApprovalStep> findByRequestTypeAndStepOrder(String requestType, Integer stepOrder);

    boolean existsByRequestType(String requestType);

    boolean existsByRequestTypeAndStepOrder(String requestType, Integer stepOrder);
}
