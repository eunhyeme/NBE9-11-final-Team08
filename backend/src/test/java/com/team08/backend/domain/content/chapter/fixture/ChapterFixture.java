package com.team08.backend.domain.content.chapter.fixture;

import com.team08.backend.domain.content.chapter.entity.Chapter;
import com.team08.backend.domain.content.course.entity.Course;
import org.springframework.test.util.ReflectionTestUtils;

public final class ChapterFixture {

    private ChapterFixture() {
    }

    public static Chapter chapter(String title, int orderNo, Course course) {
        return Chapter.create(title, orderNo, course);
    }

    public static Chapter chapter(Long id, String title, int orderNo, Course course) {
        Chapter chapter = chapter(title, orderNo, course);
        ReflectionTestUtils.setField(chapter, "id", id);
        return chapter;
    }
}