package com.techpulse.approval_workflow_engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.techpulse.approval_workflow_engine.entity.AppUser;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}