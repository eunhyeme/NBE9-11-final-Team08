package com.team08.backend.domain.content.course.access;

import com.team08.backend.domain.content.course.entity.CourseStatus;

public record CourseAccessContext(
        Long userId,
        CourseStatus courseStatus,
        boolean hasActiveEnrollment,
        boolean isOwner,
        boolean hasFreePreview,
        boolean isAdmin
) {
    public boolean isReadableCourse() {
        return courseStatus == CourseStatus.ON_SALE ||
                courseStatus == CourseStatus.SUSPENDED;
    }

    public boolean isWritableCourse() {
        return courseStatus == CourseStatus.ON_SALE;
    }
}
