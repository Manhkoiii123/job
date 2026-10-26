package com.manh.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class JobPortalNotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobPortalNotificationServiceApplication.class, args);
    }
}
