package com.manh.job.ai;

/**
 * Abstraction over the LLM used by AI feature services.
 */
public interface AiCompletionClient {

    String complete(String systemPrompt, String userPrompt);

    float[] embed(String text);

    String providerName();
}
