package com.team08.backend.domain.content.media.event;

public record CourseThumbnailEvent(
        Long courseId,
        String oldThumbnail,
        String newS3Key
) {}