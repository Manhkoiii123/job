package com.manh.job.dto.response;

import com.manh.job.domain.SocialPlatform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialLinkResponse {
    private SocialPlatform platform;
    private String url;


}
