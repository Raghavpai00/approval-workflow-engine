package com.techpulse.approval_workflow_engine.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.techpulse.approval_workflow_engine.entity.AppUser;
import com.techpulse.approval_workflow_engine.entity.Role;
import com.techpulse.approval_workflow_engine.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        save("rahul", "rahul123", Role.REQUESTER);
        save("anita", "anita123", Role.APPROVER);
        save("vikram", "vikram123", Role.APPROVER);
        save("admin", "admin123", Role.ADMIN);
    }

    private void save(String username, String rawPassword, Role role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
    }
}