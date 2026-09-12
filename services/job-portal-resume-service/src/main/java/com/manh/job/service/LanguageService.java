package com.manh.job.service;

import com.manh.job.payload.request.AddLanguageRequest;
import com.manh.job.payload.response.LanguageResponse;

import java.util.List;

public interface LanguageService {
    LanguageResponse addLanguage(Long resumeId, Long candidateId, AddLanguageRequest req) throws Exception;

    List<LanguageResponse> getLanguages(Long resumeId);

    LanguageResponse updateLanguage(
            Long languageId, Long resumeId, Long candidateId, AddLanguageRequest req
    ) throws Exception;

    void deleteLanguage(Long languageId, Long resumeId, Long candidateId) throws Exception;
}
