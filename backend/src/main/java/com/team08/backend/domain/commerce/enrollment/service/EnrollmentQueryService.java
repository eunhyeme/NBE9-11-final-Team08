package com.team08.backend.domain.commerce.enrollment.service;

import com.team08.backend.domain.content.course.entity.Course;
import com.team08.backend.domain.content.course.repository.CourseRepository;
import com.team08.backend.domain.commerce.enrollment.dto.EnrolledCourseResponse;
import com.team08.backend.domain.commerce.enrollment.entity.Enrollment;
import com.team08.backend.domain.commerce.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.commerce.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.content.lecture.repository.LectureRepository;
import com.team08.backend.domain.learning.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.member.user.entity.User;
import com.team08.backend.domain.member.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentQueryService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean hasActiveEnrollment(Long userId, Long courseId) {
        return enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                userId, courseId, EnrollmentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<EnrolledCourseResponse> getMyActiveCourses(Long userId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findAllByUserIdAndStatusOrderByEnrolledAtDescIdDesc(userId, EnrollmentStatus.ACTIVE);
        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<Long> courseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .distinct()
                .toList();

        List<Course> courses = new java.util.ArrayList<>();
        courseRepository.findAllById(courseIds).forEach(courses::add);
        Map<Long, Course> courseById = courses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        List<Long> instructorIds = courseById.values().stream()
                .map(Course::getInstructorId)
                .distinct()
                .toList();
        List<User> instructors = new java.util.ArrayList<>();
        if (!instructorIds.isEmpty()) {
            userRepository.findAllById(instructorIds).forEach(instructors::add);
        }
        Map<Long, String> nicknameByUserId = instructors.stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return enrollments.stream()
                .map(enrollment -> toResponse(
                        enrollment,
                        courseById.get(enrollment.getCourseId()),
                        userId,
                        nicknameByUserId
                ))
                .filter(response -> response != null)
                .toList();
    }

    private EnrolledCourseResponse toResponse(
            Enrollment enrollment,
            Course course,
            Long userId,
            Map<Long, String> nicknameByUserId
    ) {
        if (course == null) {
            return null;
        }

        List<Long> lectureIds = lectureRepository.findIdsByCourseId(course.getId());
        int totalLectures = lectureIds.size();
        int completedLectures = totalLectures == 0
                ? 0 : (int) lectureProgressRepository.countByUserIdAndLectureIdInAndCompleted(userId, lectureIds, true);
        int progressRate = totalLectures == 0
                ? 0 : (int) Math.round((double) completedLectures * 100 / totalLectures);

        return new EnrolledCourseResponse(
                enrollment.getId(),
                course.getId(),
                course.getTitle(),
                nicknameByUserId.getOrDefault(course.getInstructorId(), "(알 수 없음)"),
                course.getThumbnail(),
                progressRate,
                completedLectures,
                totalLectures,
                enrollment.getEnrolledAt()
        );
    }
}
