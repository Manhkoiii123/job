package com.manh.job.repository;

import com.manh.job.domain.CompanyStatus;
import com.manh.job.domain.CompanyType;
import com.manh.job.domain.IndustryType;
import com.manh.job.modal.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company,Long> {
    Optional<Company> findByOwnerId(Long ownerId);
    boolean existsByOwnerId(Long ownerId);
    boolean existsByName(String name);
    boolean existsBySlug(String slug);
    boolean existsByRegistrationNumber(String registrationNumber);

    @Query(
            "select c from Company c where " +
                    "(:companyType IS NULL OR c.companyType = :companyType) AND " +
                    "(:industryType IS NULL OR c.industryType = :industryType) AND " +
                    "(:status IS NULL OR c.status = :status)"
    )
    List<Company> findByFilters(
            @Param("companyType") CompanyType companyType,
            @Param("industry") IndustryType industryType,
            @Param("status") CompanyStatus companyStatus
    );
}
