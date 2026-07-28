package com.manh.job.mapper;

import com.manh.job.payload.response.JobCategoryResponse;
import com.manh.job.model.JobCategory;

import java.util.List;
import java.util.stream.Collectors;

public class JobCategoryMapper {

    public static JobCategoryResponse toJobCategoryResponse(JobCategory category, boolean includeChildren) {

        List<JobCategoryResponse> subCategories = null;

        if(includeChildren){
            subCategories =  category.getSubCategories()
                    .stream()
                    .map(sub -> toJobCategoryResponse(sub,false))
                    .collect(Collectors.toList());
        }

        return JobCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .active(category.getActive())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .subCategories(subCategories)
                .createdAt(category.getCreatedAt())
                .build();
    }
}