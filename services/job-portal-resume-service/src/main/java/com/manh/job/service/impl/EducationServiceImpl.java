package com.manh.job.service.impl;

import com.manh.job.mapper.ResumeMapper;
import com.manh.job.modal.Education;
import com.manh.job.modal.Resume;
import com.manh.job.dto.request.AddEducationRequest;
import com.manh.job.dto.response.EducationResponse;
import com.manh.job.repository.EducationRepository;
import com.manh.job.service.EducationService;
import com.manh.job.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EducationServiceImpl implements EducationService {

    private final EducationRepository educationRepository;
    private final ResumeService resumeService;

    @Override
    public EducationResponse addEducation(Long resumeId, Long candidateId, AddEducationRequest req) throws Exception {

        Resume resume = resumeService.getResumeEntity(resumeId);
        assertOwner(resume,candidateId);

        Education education = Education.builder()
                .resume(resume)
                .institutionName(req.getInstitutionName())
                .degree(req.getDegree())
                .fieldOfStudy(req.getFieldOfStudy())
                .grade(req.getGrade())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isCurrentlyStudying(Boolean.TRUE.equals(req.getIsCurrentlyStudying()))
                .description(req.getDescription())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .build();
        Education saved = educationRepository.save(education);
        return ResumeMapper.toEducationResponse(saved);
    }

    @Override
    public List<EducationResponse> getEducations(Long resumeId) {
        return educationRepository.findByResume_IdOrderByDisplayOrderAsc(resumeId)
                .stream().map(ResumeMapper::toEducationResponse).toList();
    }

    @Override
    public EducationResponse updateEducation(Long educationId, Long resumeId, Long candidateId, AddEducationRequest req)
            throws Exception {
        Education education = educationRepository.findById(educationId).orElseThrow(
                () -> new Exception("Education not found")
        );
        assertOwner(education.getResume(), candidateId);

        education.setInstitutionName(req.getInstitutionName());
        education.setDegree(req.getDegree());
        education.setFieldOfStudy(req.getFieldOfStudy());
        education.setGrade(req.getGrade());
        education.setStartDate(req.getStartDate());
        education.setEndDate(req.getEndDate());
        education.setIsCurrentlyStudying(Boolean.TRUE.equals(req.getIsCurrentlyStudying()));
        education.setDescription(req.getDescription());
        if (req.getDisplayOrder() != null) {
            education.setDisplayOrder(req.getDisplayOrder());
        }

        return ResumeMapper.toEducationResponse(educationRepository.save(education));
    }

    @Override
    public void deleteEducation(Long educationId, Long resumeId, Long candidateId) throws Exception {
        Education education = educationRepository.findById(educationId).orElseThrow(
                () -> new Exception("Education not found")
        );
        assertOwner(education.getResume(), candidateId);
        educationRepository.delete(education);
    }
    private void assertOwner(Resume resume, Long candidateId) throws Exception {
        if(!resume.getCandidateId().equals(candidateId)) {
            throw new Exception("Not found");
        }
    }
}
