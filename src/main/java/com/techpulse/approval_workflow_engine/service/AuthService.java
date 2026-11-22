package com.techpulse.approval_workflow_engine.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.techpulse.approval_workflow_engine.dto.AuthResponseDTO;
import com.techpulse.approval_workflow_engine.dto.LoginRequestDTO;
import com.techpulse.approval_workflow_engine.entity.AppUser;
import com.techpulse.approval_workflow_engine.repository.AppUserRepository;
import com.techpulse.approval_workflow_engine.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponseDTO login(LoginRequestDTO dto) {
        AppUser user = userRepository.findByUsername(dto.getUsername())
                .filter(u -> passwordEncoder.matches(dto.getPassword(), u.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        return AuthResponseDTO.of(jwtUtil.generateToken(user.getUsername(), user.getRole()),
                user.getRole());
    }
}