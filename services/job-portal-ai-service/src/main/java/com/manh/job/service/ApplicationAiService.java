package com.manh.job.service;

import com.manh.job.dto.ai.ApplicationScreenRequest;
import com.manh.job.dto.ai.ApplicationScreenResponse;
import com.manh.job.dto.ai.CoverLetterRequest;
import com.manh.job.dto.ai.CoverLetterResponse;

public interface ApplicationAiService {
    ApplicationScreenResponse screen(ApplicationScreenRequest request);

    CoverLetterResponse generateCoverLetter(CoverLetterRequest request);
}
