package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.request.AddLanguageRequest;
import com.manh.job.dto.response.LanguageResponse;
import com.manh.job.service.LanguageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/resumes/{resumeId}/languages")
@RequiredArgsConstructor
public class LanguageController {
    private final LanguageService languageService;

    @PostMapping
    public ResponseEntity<LanguageResponse> addLanguage(
            @PathVariable Long resumeId,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid AddLanguageRequest req
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(languageService.addLanguage(resumeId, candidateId, req));
    }

    @GetMapping
    public ResponseEntity<List<LanguageResponse>> getLanguages(
            @PathVariable Long resumeId
    ) {
        return ResponseEntity.ok(languageService.getLanguages(resumeId));
    }

    @PutMapping("/{languageId}")
    public ResponseEntity<LanguageResponse> updateLanguage(
            @PathVariable Long resumeId,
            @PathVariable Long languageId,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid AddLanguageRequest req
    ) throws Exception {
        return ResponseEntity.ok(
                languageService.updateLanguage(languageId, resumeId, candidateId, req)
        );
    }

    @DeleteMapping("/{languageId}")
    public ResponseEntity<ApiResponse> deleteLanguage(
            @PathVariable Long resumeId,
            @PathVariable Long languageId,
            @RequestHeader("X-User-Id") Long candidateId
    ) throws Exception {
        languageService.deleteLanguage(languageId, resumeId, candidateId);
        return ResponseEntity.ok(new ApiResponse("Language deleted successfully", true));
    }
}
