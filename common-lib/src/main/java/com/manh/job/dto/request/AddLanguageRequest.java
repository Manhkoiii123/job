package com.manh.job.dto.request;

import com.manh.job.domain.LanguageProficiency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddLanguageRequest {
    @NotBlank(message = "Language name is required")
    private String languageName;

    @NotNull(message = "Proficiency level is required")
    private LanguageProficiency proficiency;

    private Integer displayOrder;
}
