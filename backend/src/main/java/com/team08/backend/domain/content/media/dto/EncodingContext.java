package com.team08.backend.domain.content.media.dto;

import com.team08.backend.domain.content.media.entity.EncodingPurpose;

public record EncodingContext(
        Long lectureId,
        String dbSavePath,
        String targetDirName,
        EncodingPurpose purpose,
        String description,
        Long instructorId
) {}