package com.techpulse.approval_workflow_engine.repository;

import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;


import com.techpulse.approval_workflow_engine.entity.Request;



public interface RequestRepository extends JpaRepository<Request, Long> {
	 
    
    List<Request> findAllByOrderByIdAsc();

    List<Request> findByCreatedByOrderByIdAsc(String createdBy);
}
