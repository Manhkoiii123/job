package com.manh.job.service.impl;

import com.manh.job.ai.AiCompletionClient;
import com.manh.job.ai.AiJsonParser;
import com.manh.job.dto.ai.JobInsightRequest;
import com.manh.job.dto.ai.JobInsightResponse;
import com.manh.job.prompt.SystemPrompts;
import com.manh.job.service.JobAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobAiServiceImpl implements JobAiService {

    private final AiCompletionClient aiCompletionClient;
    private final AiJsonParser aiJsonParser;

    @Override
    public JobInsightResponse generateInsights(JobInsightRequest request) {
        String user = """
                Title: %s
                City: %s
                Country: %s
                Experience: %s
                Job type: %s
                Work mode: %s
                Raw notes:
                %s
                """.formatted(
                nullToEmpty(request.getTitle()),
                nullToEmpty(request.getCity()),
                nullToEmpty(request.getCountry()),
                nullToEmpty(request.getExperienceLevel()),
                nullToEmpty(request.getJobType()),
                nullToEmpty(request.getWorkMode()),
                nullToEmpty(request.getRawNotes())
        );
        String raw = aiCompletionClient.complete(
                SystemPrompts.JOB_INSIGHT + SystemPrompts.JSON_ONLY_HINT,
                user
        );
        return aiJsonParser.parse(raw, JobInsightResponse.class);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
