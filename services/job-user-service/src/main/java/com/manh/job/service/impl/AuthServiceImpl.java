package com.manh.job.service.impl;

import com.manh.job.domain.UserRole;
import com.manh.job.domain.UserStatus;
import com.manh.job.mapper.UserMapper;
import com.manh.job.modal.User;
import com.manh.job.payload.request.LoginRequest;
import com.manh.job.payload.request.SignupRequest;
import com.manh.job.payload.response.AuthResponse;
import com.manh.job.repository.UserRepository;
import com.manh.job.security.CustomUserDetailsService;
import com.manh.job.security.JwtProvider;
import com.manh.job.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;

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
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .phone(req.getPhone())
                .lastLogin(LocalDateTime.now())
                .status(UserStatus.ACTIVE)
                .build();
        User saveUser = userRepository.save(user);

        Authentication authentication
                = new UsernamePasswordAuthenticationToken(
                saveUser.getEmail(), saveUser.getPassword()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtProvider.generateToken(authentication, saveUser.getId());

        AuthResponse res = new AuthResponse();
        res.setTitle("Welcome " + saveUser.getFullName());
        res.setMessage("Registered Successfully");
        res.setJwt(jwt);
        res.setUser(UserMapper.toDTO(saveUser));

        return res;
    }

    @Override
    public AuthResponse login(LoginRequest req) throws Exception {

        Authentication authentication = authenticate(req.getEmail(), req.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(req.getEmail());
        String token = jwtProvider.generateToken(authentication, user.getId());

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.setTitle("Login successful");
        response.setMessage("Welcome back " + user.getFullName());
        response.setJwt(token);
        response.setUser(UserMapper.toDTO(user));
        return response;
    }
    private Authentication authenticate(String email, String password) throws Exception {
        UserDetails userDetails = customUserDetailsService
                .loadUserByUsername(email);
        if (userDetails == null) {
            throw new Exception("User not found with email: " + email);
        }
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new Exception("Invalid credentials");
        }
        return new UsernamePasswordAuthenticationToken(
                email, null, userDetails.getAuthorities());
    }
}
