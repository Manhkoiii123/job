package com.manh.job.repository;

import com.manh.job.dto.request.SavedJobSearchRequest;
import com.manh.job.modal.SavedJob;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SavedJobSpecification {

    public static Specification<SavedJob> forCandidateWithFilters(
            Long candidateId,
            SavedJobSearchRequest request
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("candidateId"), candidateId));

            if (request == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (request.getJobId() != null) {
                predicates.add(cb.equal(root.get("jobId"), request.getJobId()));
            }
            if (request.getCompanyId() != null) {
                predicates.add(cb.equal(root.get("companyId"), request.getCompanyId()));
            }
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String pattern = "%" + request.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("notes")), pattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
