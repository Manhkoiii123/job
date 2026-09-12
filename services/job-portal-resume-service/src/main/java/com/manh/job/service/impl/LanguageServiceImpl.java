package com.manh.job.service.impl;

import com.manh.job.mapper.ResumeMapper;
import com.manh.job.modal.Language;
import com.manh.job.modal.Resume;
import com.manh.job.dto.request.AddLanguageRequest;
import com.manh.job.dto.response.LanguageResponse;
import com.manh.job.repository.LanguageRepository;
import com.manh.job.service.LanguageService;
import com.manh.job.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LanguageServiceImpl implements LanguageService {
    private final LanguageRepository languageRepository;
    private final ResumeService resumeService;

    @Override
    public LanguageResponse addLanguage(Long resumeId, Long candidateId, AddLanguageRequest req)
            throws Exception {
        Resume resume = resumeService.getResumeEntity(resumeId);
        assertOwner(resume, candidateId);

        Language language = Language.builder()
                .resume(resume)
                .languageName(req.getLanguageName())
                .proficiency(req.getProficiency())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .build();

        return ResumeMapper.toLanguageResponse(languageRepository.save(language));
    }

    @Override
    public List<LanguageResponse> getLanguages(Long resumeId) {
        return languageRepository.findByResume_IdOrderByDisplayOrderAsc(resumeId)
                .stream().map(ResumeMapper::toLanguageResponse).toList();
    }

    @Override
    public LanguageResponse updateLanguage(
            Long languageId, Long resumeId, Long candidateId, AddLanguageRequest req
    ) throws Exception {
        Language language = languageRepository.findById(languageId).orElseThrow(
                () -> new Exception("Language not found")
        );
        assertBelongsToResume(language, resumeId);
        assertOwner(language.getResume(), candidateId);

        language.setLanguageName(req.getLanguageName());
        language.setProficiency(req.getProficiency());
        if (req.getDisplayOrder() != null) {
            language.setDisplayOrder(req.getDisplayOrder());
        }

        return ResumeMapper.toLanguageResponse(languageRepository.save(language));
    }

    @Override
    public void deleteLanguage(Long languageId, Long resumeId, Long candidateId) throws Exception {
        Language language = languageRepository.findById(languageId).orElseThrow(
                () -> new Exception("Language not found")
        );
        assertBelongsToResume(language, resumeId);
        assertOwner(language.getResume(), candidateId);
        languageRepository.delete(language);
    }

    private void assertBelongsToResume(Language language, Long resumeId) throws Exception {
        if (!language.getResume().getId().equals(resumeId)) {
            throw new Exception("Language not found");
        }
    }

    private void assertOwner(Resume resume, Long candidateId) throws Exception {
        if (!resume.getCandidateId().equals(candidateId)) {
            throw new Exception("Not found");
        }
    }
}
