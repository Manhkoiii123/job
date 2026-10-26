package com.manh.job.service;

import com.manh.job.dto.ai.SemanticSearchRequest;
import com.manh.job.dto.ai.SemanticSearchResponse;

public interface SemanticSearchService {
    SemanticSearchResponse search(SemanticSearchRequest request);
}
