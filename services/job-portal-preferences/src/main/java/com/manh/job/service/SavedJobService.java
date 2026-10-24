package com.manh.job.service;

import com.manh.job.dto.request.SaveJobRequest;
import com.manh.job.dto.request.SavedJobSearchRequest;
import com.manh.job.dto.request.UpdateSavedJobNotesRequest;
import com.manh.job.dto.response.SavedJobResponse;

import java.util.List;

public interface SavedJobService {

    SavedJobResponse saveJob(Long candidateId, SaveJobRequest request) throws Exception;

    SavedJobResponse getSavedJobById(Long id, Long candidateId) throws Exception;

    List<SavedJobResponse> getMySavedJobs(Long candidateId);

    List<SavedJobResponse> searchMySavedJobs(Long candidateId, SavedJobSearchRequest request);

    SavedJobResponse updateNotes(Long id, Long candidateId, UpdateSavedJobNotesRequest request) throws Exception;

    void unsaveJob(Long id, Long candidateId) throws Exception;

    boolean isJobSaved(Long candidateId, Long jobId);
}
