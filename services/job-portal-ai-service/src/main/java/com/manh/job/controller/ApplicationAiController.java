package com.manh.job.controller;

import com.manh.job.dto.ai.ApplicationScreenRequest;
import com.manh.job.dto.ai.ApplicationScreenResponse;
import com.manh.job.dto.ai.CoverLetterRequest;
import com.manh.job.dto.ai.CoverLetterResponse;
import com.manh.job.service.ApplicationAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/applications")
@RequiredArgsConstructor
public class ApplicationAiController {

    private final ApplicationAiService applicationAiService;

    @PostMapping("/screen")
    public ResponseEntity<ApplicationScreenResponse> screen(@RequestBody @Valid ApplicationScreenRequest request) {
        return ResponseEntity.ok(applicationAiService.screen(request));
    }

    @PostMapping("/cover-letter")
    public ResponseEntity<CoverLetterResponse> coverLetter(@RequestBody @Valid CoverLetterRequest request) {
        return ResponseEntity.ok(applicationAiService.generateCoverLetter(request));
    }
}
