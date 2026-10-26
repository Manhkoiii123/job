package com.manh.job.controller;

import com.manh.job.dto.ai.SemanticSearchRequest;
import com.manh.job.dto.ai.SemanticSearchResponse;
import com.manh.job.service.SemanticSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/search")
@RequiredArgsConstructor
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    @PostMapping("/semantic")
    public ResponseEntity<SemanticSearchResponse> semantic(@RequestBody @Valid SemanticSearchRequest request) {
        return ResponseEntity.ok(semanticSearchService.search(request));
    }
}
