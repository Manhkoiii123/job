package com.manh.job.controller;

import com.manh.job.domain.UserRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping()
    public  String HomeController () {
        return "Job portal user service" + UserRole.ROLE_ADMIN;
    }
}
