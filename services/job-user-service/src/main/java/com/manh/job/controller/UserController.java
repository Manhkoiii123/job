package com.manh.job.controller;

import com.manh.job.dto.request.UpdateUserRequest;
import com.manh.job.dto.response.UserResponse;
import com.manh.job.mapper.UserMapper;
import com.manh.job.modal.User;
import com.manh.job.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getUserProfile(
            @RequestHeader("X-User-Id") Long userId
    ) throws Exception {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody UpdateUserRequest req
    ) throws Exception {
        return ResponseEntity.ok(userService.updateProfile(userId, req));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long userId
    ) throws Exception {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    @GetMapping("")
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestHeader("X-User-Id") Long adminId
    ) throws Exception {
        List<User> users = userService.getUsers(adminId);
        return ResponseEntity.ok(UserMapper.toDTOList(users));
    }

    @PatchMapping("/{userid}/suspend")
    public ResponseEntity<UserResponse> suspendUser(
            @PathVariable Long userid,
            @RequestHeader("X-User-Id") Long adminId
    ) throws Exception {
        return ResponseEntity.ok(userService.suspendUser(userid, adminId));
    }

    @PatchMapping("/{userid}/activate")
    public ResponseEntity<UserResponse> activatedUser(
            @PathVariable Long userid,
            @RequestHeader("X-User-Id") Long adminId
    ) throws Exception {
        return ResponseEntity.ok(userService.activatedUser(userid, adminId));
    }

    @PatchMapping("/{userid}/delete")
    public ResponseEntity<UserResponse> deleteUser(
            @PathVariable Long userid,
            @RequestHeader("X-User-Id") Long adminId
    ) throws Exception {
        return ResponseEntity.ok(userService.deleteUser(userid, adminId));
    }
}
