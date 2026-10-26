package com.manh.job.service.impl;

import com.manh.job.ai.AiCompletionClient;
import com.manh.job.ai.AiJsonParser;
import com.manh.job.ai.CosineSimilarity;
import com.manh.job.dto.ai.SemanticSearchDocument;
import com.manh.job.dto.ai.SemanticSearchHit;
import com.manh.job.dto.ai.SemanticSearchRequest;
import com.manh.job.dto.ai.SemanticSearchResponse;
import com.manh.job.prompt.SystemPrompts;
import com.manh.job.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private final AiCompletionClient aiCompletionClient;
    private final AiJsonParser aiJsonParser;

    @Override
    public SemanticSearchResponse search(SemanticSearchRequest request) {
        int topK = request.getTopK() == null || request.getTopK() < 1 ? 5 : request.getTopK();

        String intentRaw = aiCompletionClient.complete(
                SystemPrompts.SEMANTIC_INTENT + SystemPrompts.JSON_ONLY_HINT,
                "Query: " + request.getQuery()
        );
        SemanticSearchResponse intent = aiJsonParser.parse(intentRaw, SemanticSearchResponse.class);

        float[] queryVector = aiCompletionClient.embed(request.getQuery());
        List<SemanticSearchHit> hits = request.getDocuments().stream()
                .map(doc -> toHit(doc, queryVector))
                .sorted(Comparator.comparing(SemanticSearchHit::getScore).reversed())
                .limit(topK)
                .toList();

        intent.setQuery(request.getQuery());
        intent.setHits(hits);
        return intent;
    }

    private SemanticSearchHit toHit(SemanticSearchDocument doc, float[] queryVector) {
        String blob = (doc.getTitle() == null ? "" : doc.getTitle() + "\n") + doc.getText();
        float[] docVector = aiCompletionClient.embed(blob);
        return SemanticSearchHit.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .score(CosineSimilarity.score(queryVector, docVector))
                .build();
    }
}
