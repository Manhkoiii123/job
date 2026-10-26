package com.manh.job.controller;

import com.manh.job.dto.ai.ResumeAnalyzeRequest;
import com.manh.job.dto.ai.ResumeAnalyzeResponse;
import com.manh.job.service.ResumeAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/resumes")
@RequiredArgsConstructor
public class ResumeAiController {

    private final ResumeAiService resumeAiService;

    @PostMapping("/analyze")
    public ResponseEntity<ResumeAnalyzeResponse> analyze(@RequestBody @Valid ResumeAnalyzeRequest request) {
        return ResponseEntity.ok(resumeAiService.analyze(request));
    }
}
