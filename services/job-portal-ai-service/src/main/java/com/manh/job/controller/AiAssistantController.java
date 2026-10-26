package com.manh.job.controller;

import com.manh.job.dto.ai.AiChatRequest;
import com.manh.job.dto.ai.AiChatResponse;
import com.manh.job.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody @Valid AiChatRequest request) {
        return ResponseEntity.ok(aiAssistantService.chat(request));
    }
}
