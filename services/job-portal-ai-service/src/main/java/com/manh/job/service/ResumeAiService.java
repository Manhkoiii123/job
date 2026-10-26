package com.manh.job.service;

import com.manh.job.dto.ai.ResumeAnalyzeRequest;
import com.manh.job.dto.ai.ResumeAnalyzeResponse;

public interface ResumeAiService {
    ResumeAnalyzeResponse analyze(ResumeAnalyzeRequest request);
}
