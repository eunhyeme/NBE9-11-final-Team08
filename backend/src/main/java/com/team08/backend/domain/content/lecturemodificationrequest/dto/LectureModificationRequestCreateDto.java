package com.team08.backend.domain.content.lecturemodificationrequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LectureModificationRequestCreateDto(
        @NotNull Long lectureId,
        @NotBlank String description
) {}