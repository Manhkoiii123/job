package com.manh.job.payload.response;

import com.manh.job.dto.response.UserResponse;
import lombok.Data;

@Data
public class AuthResponse {
    private  String jwt;
    private  String title;
    private  String Message;
    private UserResponse user;

}
