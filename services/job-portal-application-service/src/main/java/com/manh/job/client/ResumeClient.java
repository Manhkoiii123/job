package com.manh.job.client;

import com.manh.job.dto.response.ResumeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ResumeClient {

    private final RestClient restClient;

    public ResumeClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.resume.url:http://localhost:5004}") String resumeServiceUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(resumeServiceUrl).build();
    }

    public ResumeResponse getResumeById(Long resumeId, Long candidateId) {
        return restClient.get()
                .uri("/api/resumes/{resumeId}", resumeId)
                .header("X-User-Id", candidateId.toString())
                .retrieve()
                .body(ResumeResponse.class);
    }
}
