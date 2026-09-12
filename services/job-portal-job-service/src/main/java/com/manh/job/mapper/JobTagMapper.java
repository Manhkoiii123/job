package com.manh.job.mapper;

import com.manh.job.modal.JobTag;
import com.manh.job.dto.response.JobTagResponse;

public class JobTagMapper {
    public static JobTagResponse toTagResponse(JobTag req) {
        return JobTagResponse.builder()
                .id(req.getId())
                .name(req.getName())
                .slug(req.getSlug())
                .build();
    }
}
