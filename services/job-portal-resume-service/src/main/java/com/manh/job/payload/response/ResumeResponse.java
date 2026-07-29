package com.manh.job.payload.response;

import com.manh.job.domain.ResumeTemplate;
import com.manh.job.domain.ResumeVisibility;
import com.manh.job.dto.PersonalInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ResumeResponse {
    private Long id;
    private Long candidateId;
    private String title;
    private ResumeTemplate template;
    private ResumeVisibility visibility;
    private Boolean isDefault;
    private Boolean isActive;
    private PersonalInfoResponse personalInfo;
    private String summary;
    private String uploadedFileUrl;
    private String uploadedFileName;
    private Integer completionScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

//    private List<WorkExperienceResponse> workExperiences;
//    private List<EducationResponse> educations;
//    private List<ResumeSkillResponse> skills;
//    private List<ProjectResponse> projects;
//    private List<CertificationResponse> certifications;
//    private List<AwardResponse> awards;
//    private List<LanguageResponse> languages;
}
