package com.manh.job.payload.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private  String fullName;
    private String phone;
    private String profileImage;
}
