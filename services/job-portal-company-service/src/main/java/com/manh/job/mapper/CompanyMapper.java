package com.manh.job.mapper;

import com.manh.job.dto.response.CompanyResponse;
import com.manh.job.dto.response.SocialLinkResponse;
import com.manh.job.modal.Company;
import com.manh.job.modal.SocialLink;

import java.util.Collections;
import java.util.List;

public class CompanyMapper {
    private  static SocialLinkResponse toSocialLinkResponse(SocialLink socialLink){
        return SocialLinkResponse.builder()
                .platform(socialLink.getPlatform())
                .url(socialLink.getUrl())
                .build();
    }

    public  static CompanyResponse toResponse(Company company){

        List<SocialLinkResponse> socialLinks = company.getSocialLinks() == null ?
                Collections.emptyList() :
                company.getSocialLinks().stream().map(CompanyMapper::toSocialLinkResponse).toList();

        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .slug(company.getSlug())
                .tagline(company.getTagline())
                .description(company.getDescription())
                .logoUrl(company.getLogoUrl())
                .coverImageUrl(company.getCoverImageUrl())
                .website(company.getWebsite())
                .email(company.getEmail())
                .phone(company.getPhone())
                .foundedYear(company.getFoundedYear())
                .companySize(company.getCompanySize())
                .companyType(company.getCompanyType())
                .industryType(company.getIndustryType())
                .status(company.getStatus())
                .verified(company.isVerified())
                .active(company.getActive())
                .ownerId(company.getOwnerId())
                .socialLinks(socialLinks)
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .verifiedAt(company.getVerifiedAt())
                .build();
    }
}
