package com.manh.job.service;

import com.manh.job.payload.request.LoginRequest;
import com.manh.job.payload.response.AuthResponse;
import com.manh.job.payload.request.SignupRequest;

public interface AuthService {
    AuthResponse signup(SignupRequest req) throws Exception;
    AuthResponse login(LoginRequest req) throws Exception;
}
