package com.manh.job.service.impl;

import com.manh.job.ai.AiCompletionClient;
import com.manh.job.ai.AiJsonParser;
import com.manh.job.dto.ai.ResumeAnalyzeRequest;
import com.manh.job.dto.ai.ResumeAnalyzeResponse;
import com.manh.job.prompt.SystemPrompts;
import com.manh.job.service.ResumeAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResumeAiServiceImpl implements ResumeAiService {

    private final AiCompletionClient aiCompletionClient;
    private final AiJsonParser aiJsonParser;

    @Override
    public ResumeAnalyzeResponse analyze(ResumeAnalyzeRequest request) {
        StringBuilder user = new StringBuilder("Resume:\n").append(request.getResumeText());
        if (request.getTargetJobDescription() != null && !request.getTargetJobDescription().isBlank()) {
            user.append("\n\nTarget job:\n").append(request.getTargetJobDescription());
        }
        String raw = aiCompletionClient.complete(
                SystemPrompts.RESUME_ANALYST + SystemPrompts.JSON_ONLY_HINT,
                user.toString()
        );
        return aiJsonParser.parse(raw, ResumeAnalyzeResponse.class);
    }
}
