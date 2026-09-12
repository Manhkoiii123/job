package com.manh.job.client;

import com.manh.job.dto.response.CompanyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CompanyClient {
    private final RestClient restClient;

    public CompanyClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.company.url:http://localhost:5002}") String companyServiceUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(companyServiceUrl).build();
    }

    public CompanyResponse getCompanyByOwnerId(Long ownerId) {
        return restClient.get()
                .uri("/api/companies/my")
                .header("X-User-Id", ownerId.toString())
                .retrieve()
                .body(CompanyResponse.class);
    }

    public CompanyResponse getCompanyById(Long companyId) {
        return restClient.get()
                .uri("/api/companies/{companyId}", companyId)
                .retrieve()
                .body(CompanyResponse.class);
    }
}
