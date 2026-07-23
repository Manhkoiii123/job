package com.manh.job.service.impl;

import com.manh.job.domain.UserRole;
import com.manh.job.domain.UserStatus;
import com.manh.job.mapper.UserMapper;
import com.manh.job.model.User;
import com.manh.job.payload.request.LoginRequest;
import com.manh.job.payload.request.SignupRequest;
import com.manh.job.payload.response.AuthResponse;
import com.manh.job.repository.UserRepository;
import com.manh.job.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;

    @Override
    public AuthResponse signup(SignupRequest req) throws Exception {
        if(userRepository.existsByEmail(req.getEmail())){
            throw new Exception("Email already registered: "+ req.getEmail());
        }
        if(req.getRole() == UserRole.ROLE_ADMIN){
            throw new Exception("Can't sign up as admin");
        }
        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(req.getPassword())
                .role(req.getRole())
                .phone(req.getPhone())
                .lastLogin(LocalDateTime.now())
                .status(UserStatus.ACTIVE)
                .build();
        User saveUser = userRepository.save(user);

        AuthResponse res = new AuthResponse();
        res.setTitle("Welcome" + saveUser.getFullName());
        res.setMessage("Registered Successfully");
        res.setJwt("JWT");
        res.setUser(UserMapper.toDTO(saveUser));

        return res;
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        return null;
    }
}
