package com.manh.job.service.impl;

import com.manh.job.ai.AiCompletionClient;
import com.manh.job.dto.ai.AiChatRequest;
import com.manh.job.dto.ai.AiChatResponse;
import com.manh.job.prompt.SystemPrompts;
import com.manh.job.service.AiAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAssistantServiceImpl implements AiAssistantService {

    private final AiCompletionClient aiCompletionClient;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        StringBuilder user = new StringBuilder(request.getMessage());
        if (request.getContext() != null && !request.getContext().isBlank()) {
            user.append("\n\nContext:\n").append(request.getContext());
        }
        String reply = aiCompletionClient.complete(SystemPrompts.CAREER_ASSISTANT, user.toString());
        return AiChatResponse.builder()
                .reply(reply)
                .provider(aiCompletionClient.providerName())
                .build();
    }
}
