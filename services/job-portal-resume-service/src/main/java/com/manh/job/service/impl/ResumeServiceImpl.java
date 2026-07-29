package com.manh.job.service.impl;

import com.manh.job.dto.PersonalInfoResponse;
import com.manh.job.mapper.ResumeMapper;
import com.manh.job.modal.PersonalInfo;
import com.manh.job.modal.Resume;
import com.manh.job.payload.request.CreateResumeRequest;
import com.manh.job.payload.response.ResumeResponse;
import com.manh.job.repository.ResumeRepository;
import com.manh.job.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final ResumeRepository resumeRepository;
    @Override
    public ResumeResponse createResume(Long candidateId, CreateResumeRequest req) {
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            resumeRepository.findByCandidateIdAndIsDefaultTrue(candidateId)
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        resumeRepository.save(existing);
                    });
        }
        Resume resume = Resume.builder()
                .candidateId(candidateId)
                .title(req.getTitle())
                .template(req.getTemplate())
                .visibility(req.getVisibility())
                .isDefault(Boolean.TRUE.equals(req.getIsDefault()))
                .isActive(true)
                .build();

        Resume saved = resumeRepository.save(resume);
        return  buildFullResponse(saved);
    }

    @Override
    public ResumeResponse getResumeById(Long resumeId, Long candidateId) throws Exception {
        Resume resume = getResumeEntity(resumeId);
        assertOwner(resume,candidateId);
        return buildFullResponse(resume);
    }

    @Override
    public List<ResumeResponse> getMyResumes(Long candidateId) {
        return resumeRepository.findByCandidateIdAndIsActiveTrue(candidateId).stream().map(this::buildFullResponse).toList();
    }

    @Override
    public ResumeResponse updatePersonalInfo(Long resumeId, Long candidateId, PersonalInfoResponse req) throws Exception {
        Resume resume = getResumeEntity(resumeId);
        assertOwner(resume,candidateId);
        PersonalInfo info = resume.getPersonalInfo();
        if(info == null){ info = new PersonalInfo(); }

        if (req.getFirstName() != null)
            info.setFirstName(req.getFirstName());

        if (req.getLastName() != null)
            info.setLastName(req.getLastName());

        if (req.getHeadline() != null)
            info.setHeadline(req.getHeadline());

        if (req.getEmail() != null)
            info.setEmail(req.getEmail());

        if (req.getPhone() != null)
            info.setPhone(req.getPhone());

        if (req.getCity() != null)
            info.setCity(req.getCity());

        if (req.getCountry() != null)
            info.setCountry(req.getCountry());

        if (req.getLinkedinUrl() != null)
            info.setLinkedinUrl(req.getLinkedinUrl());

        if (req.getGithubUrl() != null)
            info.setGithubUrl(req.getGithubUrl());

        if (req.getPortfolioUrl() != null)
            info.setPortfolioUrl(req.getPortfolioUrl());

        if (req.getWebsiteUrl() != null)
            info.setWebsiteUrl(req.getWebsiteUrl());

        resume.setPersonalInfo(info);
        Resume saved = resumeRepository.save(resume);

        return buildFullResponse(saved);
    }

    @Override
    public ResumeResponse updateSummary(Long resumeId, Long candidateId, String summary) throws Exception {
        Resume resume = getResumeEntity(resumeId);
        assertOwner(resume,candidateId);
        resume.setSummary(summary);
        Resume saved = resumeRepository.save(resume);
        return buildFullResponse(saved);
    }

    @Override
    public ResumeResponse setDefaultResume(Long resumeId, Long candidateId) throws Exception {
        Resume resume = getResumeEntity(resumeId);
        assertOwner(resume,candidateId);
        resumeRepository.findByCandidateIdAndIsDefaultTrue(candidateId)
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    resumeRepository.save(existing);
                });
        resume.setIsDefault(true);
        Resume saved = resumeRepository.save(resume);
        return buildFullResponse(saved);
    }

    @Override
    public void deleteResume(Long resumeId, Long candidateId) throws Exception {
        Resume resume = getResumeEntity(resumeId);
        assertOwner(resume,candidateId);
        resume.setIsActive(false);
        resume.setIsDefault(false);
        resumeRepository.save(resume);
    }

    @Override
    public Resume getResumeEntity(Long resumeId) throws Exception {
            return resumeRepository.findById(resumeId).orElseThrow(
                    () -> new Exception("Resume not found")
            );

    }

    private ResumeResponse buildFullResponse(Resume resume) {
       return ResumeMapper.toResponse(resume);
    }

    private void assertOwner(Resume resume, Long candidateId) throws Exception {
        if(!resume.getCandidateId().equals(candidateId)) {
            throw new Exception("resume not found with id");
        }
    }
}
