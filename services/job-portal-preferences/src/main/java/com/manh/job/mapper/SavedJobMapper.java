package com.manh.job.mapper;

import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.dto.response.JobResponse;
import com.manh.job.dto.response.SavedJobResponse;
import com.manh.job.modal.SavedJob;

public class SavedJobMapper {

    public static SavedJobResponse toResponse(
            SavedJob savedJob,
            JobResponse job,
            CompanyResponse company
    ) {
        if (savedJob == null) {
            return null;
        }
        return SavedJobResponse.builder()
                .id(savedJob.getId())
                .candidateId(savedJob.getCandidateId())
                .jobId(savedJob.getJobId())
                .companyId(savedJob.getCompanyId())
                .notes(savedJob.getNotes())
                .savedAt(savedJob.getSavedAt())
                .job(job)
                .company(company)
                .build();
    }

    public static SavedJobResponse toResponse(SavedJob savedJob) {
        return toResponse(savedJob, null, null);
    }
}
