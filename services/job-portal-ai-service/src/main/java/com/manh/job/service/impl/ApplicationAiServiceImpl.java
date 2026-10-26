package com.manh.job.service.impl;

import com.manh.job.ai.AiCompletionClient;
import com.manh.job.ai.AiJsonParser;
import com.manh.job.dto.ai.ApplicationScreenRequest;
import com.manh.job.dto.ai.ApplicationScreenResponse;
import com.manh.job.dto.ai.CoverLetterRequest;
import com.manh.job.dto.ai.CoverLetterResponse;
import com.manh.job.prompt.SystemPrompts;
import com.manh.job.service.ApplicationAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationAiServiceImpl implements ApplicationAiService {

    private final AiCompletionClient aiCompletionClient;
    private final AiJsonParser aiJsonParser;

    @Override
    public ApplicationScreenResponse screen(ApplicationScreenRequest request) {
        String user = """
                Job title: %s
                Job requirements:
                %s
                Resume:
                %s
                Cover letter:
                %s
                """.formatted(
                request.getJobTitle(),
                request.getJobRequirements(),
                request.getResumeText(),
                request.getCoverLetter() == null ? "" : request.getCoverLetter()
        );
        String raw = aiCompletionClient.complete(
                SystemPrompts.APPLICATION_SCREENER + SystemPrompts.JSON_ONLY_HINT,
                user
        );
        return aiJsonParser.parse(raw, ApplicationScreenResponse.class);
    }

    @Override
    public CoverLetterResponse generateCoverLetter(CoverLetterRequest request) {
        String user = """
                Candidate: %s
                Job title: %s
                Company: %s
                Tone: %s
                Resume highlights:
                %s
                """.formatted(
                request.getCandidateName(),
                request.getJobTitle(),
                request.getCompanyName(),
                request.getTone() == null ? "professional" : request.getTone(),
                request.getResumeHighlights()
        );
        String raw = aiCompletionClient.complete(
                SystemPrompts.COVER_LETTER_WRITER + SystemPrompts.JSON_ONLY_HINT,
                user
        );
        return aiJsonParser.parse(raw, CoverLetterResponse.class);
    }
}
