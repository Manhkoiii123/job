package com.manh.job.client;

import com.manh.job.dto.response.JobResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class JobClient {

    private final RestClient restClient;

    public JobClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.job.url:http://localhost:5003}") String jobServiceUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(jobServiceUrl).build();
    }

    public JobResponse getJobById(Long jobId) {
        return restClient.get()
                .uri("/api/jobs/{jobId}", jobId)
                .retrieve()
                .body(JobResponse.class);
    }
}
