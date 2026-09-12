package com.manh.job.service;

import com.manh.job.dto.request.LoginRequest;
import com.manh.job.dto.response.AuthResponse;
import com.manh.job.dto.request.SignupRequest;

public interface AuthService {
    AuthResponse signup(SignupRequest req) throws Exception;
    AuthResponse login(LoginRequest req) throws Exception;
}
