package com.team08.backend.domain.enrollment.dto;

import java.time.LocalDateTime;

public record EnrolledCourseResponse(
        Long enrollmentId,
        Long courseId,
        String title,
        String instructorNickname,
        String thumbnailUrl,
        int progressRate,
        int completedLectures,
        int totalLectures,
        LocalDateTime enrolledAt
) {
}
