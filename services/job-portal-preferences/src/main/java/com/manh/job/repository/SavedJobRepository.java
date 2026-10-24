package com.manh.job.repository;

import com.manh.job.modal.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long>,
        JpaSpecificationExecutor<SavedJob> {

    List<SavedJob> findByCandidateIdOrderBySavedAtDesc(Long candidateId);

    Optional<SavedJob> findByIdAndCandidateId(Long id, Long candidateId);

    Optional<SavedJob> findByCandidateIdAndJobId(Long candidateId, Long jobId);

    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);

    void deleteByIdAndCandidateId(Long id, Long candidateId);
}
