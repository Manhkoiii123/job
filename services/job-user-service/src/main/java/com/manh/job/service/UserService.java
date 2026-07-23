package com.manh.job.service;
import com.manh.job.dto.response.UserResponse;
import com.manh.job.model.User;
import com.manh.job.payload.request.UpdateUserRequest;

import java.util.List;
import java.util.Set;

public interface UserService {
    User getUserByEmail(String email) throws Exception;
    User getUserById(Long id) throws Exception;
    List<User> getUsers() throws Exception;
    UserResponse updateProfile(String email, UpdateUserRequest req) throws Exception;

    UserResponse suspendUser(Long id) throws Exception;
    UserResponse activatedUser(Long id) throws Exception;
    UserResponse deleteUser(Long id) throws Exception;
}
