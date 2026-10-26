package com.manh.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class JobPortalAiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobPortalAiServiceApplication.class, args);
    }
}
