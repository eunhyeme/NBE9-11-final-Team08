package com.team08.backend.domain.content.media.event;

public record VideoCleanUpEvent(
        Long lectureId,
        String targetDirName
) {}
