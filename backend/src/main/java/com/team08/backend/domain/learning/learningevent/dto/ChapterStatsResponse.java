package com.team08.backend.domain.learning.learningevent.dto;

public record ChapterStatsResponse(
        Long chapterId,
        long enterCount,
        long completionCount,
        long avgWatchTimeSeconds
) {}
