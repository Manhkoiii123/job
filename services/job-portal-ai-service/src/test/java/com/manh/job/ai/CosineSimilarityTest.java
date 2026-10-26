package com.manh.job.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosineSimilarityTest {

    @Test
    void identicalVectorsScoreOne() {
        float[] v = {1f, 0f, 0f};
        assertEquals(1.0, CosineSimilarity.score(v, v), 1e-6);
    }

    @Test
    void orthogonalVectorsScoreNearZero() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};
        assertEquals(0.0, CosineSimilarity.score(a, b), 1e-6);
    }

    @Test
    void similarVectorsScoreHigh() {
        float[] a = {1f, 1f, 0f};
        float[] b = {1f, 0.9f, 0.1f};
        assertTrue(CosineSimilarity.score(a, b) > 0.9);
    }
}
