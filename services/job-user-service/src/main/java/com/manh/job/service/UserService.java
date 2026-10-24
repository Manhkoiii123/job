package com.manh.job.service;

import com.manh.job.dto.request.UpdateUserRequest;
import com.manh.job.dto.response.UserResponse;
import com.manh.job.modal.User;

import java.util.List;

public interface UserService {
    User getUserByEmail(String email) throws Exception;

    User getUserById(Long id) throws Exception;

    List<User> getUsers(Long adminId) throws Exception;

    UserResponse updateProfile(Long userId, UpdateUserRequest req) throws Exception;

    UserResponse suspendUser(Long id, Long adminId) throws Exception;

    UserResponse activatedUser(Long id, Long adminId) throws Exception;

    UserResponse deleteUser(Long id, Long adminId) throws Exception;
}
