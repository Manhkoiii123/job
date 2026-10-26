package com.manh.job.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.job.dto.ai.ResumeAnalyzeResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiJsonParserTest {

    private final AiJsonParser parser = new AiJsonParser(new ObjectMapper());

    @Test
    void parsesPlainJson() {
        String raw = """
                {"overallScore":80,"summary":"ok","strengths":["a"],"skillGaps":[],"suggestions":[],"extractedSkills":["Java"]}
                """;
        ResumeAnalyzeResponse response = parser.parse(raw, ResumeAnalyzeResponse.class);
        assertEquals(80, response.getOverallScore());
        assertEquals("Java", response.getExtractedSkills().get(0));
    }

    @Test
    void stripsMarkdownFences() {
        String raw = """
                ```json
                {"overallScore":70,"summary":"fine","strengths":[],"skillGaps":[],"suggestions":["x"],"extractedSkills":[]}
                ```
                """;
        ResumeAnalyzeResponse response = parser.parse(raw, ResumeAnalyzeResponse.class);
        assertEquals(70, response.getOverallScore());
        assertFalse(response.getSuggestions().isEmpty());
    }
}
