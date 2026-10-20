package com.team08.backend.domain.content.media.event;

public record VideoRollbackEvent(
        Long lectureId,
        String targetDirName
) {}