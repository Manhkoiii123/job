package com.manh.job.mapper;

import com.manh.job.dto.request.CreateApplicationRequest;
import com.manh.job.dto.response.ApplicationResponse;
import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.dto.response.JobResponse;
import com.manh.job.dto.response.UserResponse;
import com.manh.job.modal.Application;

public class ApplicationMapper {
    public static Application toEntity(CreateApplicationRequest req,
                                       Long candidateId,
                                       Long companyId,
                                       Long employerId) {
        if (req == null) return null;
        return Application.builder()
                .candidateId(candidateId)
                .jobId(req.getJobId())
                .companyId(companyId)
                .employerId(employerId)
                .resumeId(req.getResumeId())
                .coverLetter(req.getCoverLetter())
                .expectedSalary(req.getExpectedSalary())
                .availableFrom(req.getAvailableFrom())
                .build();
    }
    public static ApplicationResponse toResponse(Application application,
//                                            List<ApplicationNote> notes,
                                            JobResponse job,
                                            CompanyResponse company,
                                            UserResponse candidate
//                                            ApplicationScreening screening
    ) {

        return ApplicationResponse.builder()
                .id(application.getId())
                .candidate(candidate)
                .employerId(application.getEmployerId())
                .job(job)
                .company(company)
                .status(application.getStatus())

                .resumeId(application.getResumeId())
                .coverLetter(application.getCoverLetter())

                .expectedSalary(application.getExpectedSalary())

                .availableFrom(application.getAvailableFrom())

                .isStarred(application.getIsStarred())
//          .notes(toNoteResponseList(notes))
                .withdrawnAt(application.getWithdrawnAt())
                .withdrawnReason(application.getWithdrawnReason())
                .appliedAt(application.getAppliedAt())
                .updatedAt(application.getUpdatedAt())
//          .screening(toScreeningResponse(screening))
                .build();
    }
}
