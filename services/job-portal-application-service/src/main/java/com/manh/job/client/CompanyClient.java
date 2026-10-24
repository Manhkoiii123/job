package com.manh.job.client;

import com.manh.job.dto.response.CompanyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "job-portal-company-service")
public interface CompanyClient {

    @GetMapping("/api/companies/{companyId}")
    CompanyResponse getCompanyById(@PathVariable("companyId") Long companyId);

    @GetMapping("/api/companies/my")
    CompanyResponse getCompanyByOwnerId(@RequestHeader("X-User-Id") Long ownerId);
}
