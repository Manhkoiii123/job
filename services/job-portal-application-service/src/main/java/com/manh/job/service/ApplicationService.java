package com.manh.job.service;

import com.manh.job.domain.ApplicationStatus;
import com.manh.job.dto.request.CompanyApplicationFilterRequest;
import com.manh.job.dto.request.CreateApplicationRequest;
import com.manh.job.dto.request.WithdrawApplicationRequest;
import com.manh.job.dto.response.ApplicationResponse;
import com.manh.job.modal.Application;

import java.util.List;

public interface ApplicationService {
    ApplicationResponse createApplication(
            Long candidateId,
            CreateApplicationRequest req
    ) throws Exception;

    ApplicationResponse getApplicationById(Long id) throws Exception;

    List<ApplicationResponse> getMyApplications(Long candidateId);

    List<ApplicationResponse> getApplicationsForJob(Long jobId);

    List<ApplicationResponse> getApplicationsForCompany(Long userId, CompanyApplicationFilterRequest request);

    ApplicationResponse updateStatus(Long applicationId,Long employerId, ApplicationStatus status) throws Exception;
    ApplicationResponse withDraw(Long applicationId, Long candidateId, WithdrawApplicationRequest req) throws Exception;
    ApplicationResponse toggleStar(Long applicationId, Long employerId) throws Exception;

    void deleteApplication(Long applicationId) throws Exception;

    Application getApplicationEntity(Long id) throws Exception;

}
