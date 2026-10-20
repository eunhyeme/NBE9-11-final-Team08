package com.team08.backend.domain.learning.learningevent.dto;

public record CourseStatsResponse(
        Long courseId,
        long enterCount,
        long watchTimeSeconds,
        long completionCount
) {}
