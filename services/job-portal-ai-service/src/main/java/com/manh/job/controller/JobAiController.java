package com.manh.job.controller;

import com.manh.job.dto.ai.JobInsightRequest;
import com.manh.job.dto.ai.JobInsightResponse;
import com.manh.job.service.JobAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/jobs")
@RequiredArgsConstructor
public class JobAiController {

    private final JobAiService jobAiService;

    @PostMapping("/insights")
    public ResponseEntity<JobInsightResponse> insights(@RequestBody @Valid JobInsightRequest request) {
        return ResponseEntity.ok(jobAiService.generateInsights(request));
    }
}
