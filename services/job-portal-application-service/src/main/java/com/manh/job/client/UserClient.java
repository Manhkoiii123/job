package com.manh.job.client;

import com.manh.job.dto.response.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserClient {

    private final RestClient restClient;

    public UserClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.user.url:http://localhost:5001}") String userServiceUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(userServiceUrl).build();
    }

    public UserResponse getUserById(Long userId) {
        return restClient.get()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .body(UserResponse.class);
    }
}
