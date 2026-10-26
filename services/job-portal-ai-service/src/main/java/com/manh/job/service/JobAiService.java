package com.manh.job.service;

import com.manh.job.dto.ai.JobInsightRequest;
import com.manh.job.dto.ai.JobInsightResponse;

public interface JobAiService {
    JobInsightResponse generateInsights(JobInsightRequest request);
}
