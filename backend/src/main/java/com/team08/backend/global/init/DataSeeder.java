package com.team08.backend.global.init;

import com.team08.backend.domain.cart.entity.Cart;
import com.team08.backend.domain.cart.repository.CartRepository;
import com.team08.backend.domain.category.entity.Category;
import com.team08.backend.domain.category.repository.CategoryRepository;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.entity.CourseStatus;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.coursestatushistory.entity.CourseStatusHistory;
import com.team08.backend.domain.coursestatushistory.repository.CourseStatusHistoryRepository;
import com.team08.backend.domain.enrollment.entity.Enrollment;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.learningevent.entity.LearningEvent;
import com.team08.backend.domain.learningevent.entity.LearningEventType;
import com.team08.backend.domain.learningevent.repository.LearningEventRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecturemodificationrequest.entity.LectureModificationRequest;
import com.team08.backend.domain.lecturemodificationrequest.entity.RequestStatus;
import com.team08.backend.domain.lecturemodificationrequest.repository.LectureModificationRequestRepository;
import com.team08.backend.domain.lectureprogress.entity.LectureProgress;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.domain.order.entity.Order;
import com.team08.backend.domain.order.repository.OrderRepository;
import com.team08.backend.domain.payment.entity.Payment;
import com.team08.backend.domain.payment.repository.PaymentRepository;
import com.team08.backend.domain.seller.entity.Seller;
import com.team08.backend.domain.seller.repository.SellerRepository;
import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import com.team08.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 더미 데이터 생성 로직을 한곳에 모은 공유 시더.
 * <p>
 * SimpleDataInitializer / BulkDataInitializer 는 {@link SeedConfig} 의 "수량"만 다르게 넘기고,
 * 실제 엔티티 생성 코드는 모두 이 클래스에서만 관리한다. (엔티티 변경 시 수정 지점이 한 곳)
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder {

    /** 생성 규모 설정. 값만 바꾸면 simple/bulk 가 동일 로직으로 동작한다. */
    public record SeedConfig(
            int sellerCount,
            int coursesPerSeller,
            int chaptersPerCourse,
            int lecturesPerChapter,
            int userCount,
            int batchSize
    ) {
        public int courseCount() {
            return sellerCount * coursesPerSeller;
        }
    }

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final CourseStatusHistoryRepository courseStatusHistoryRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LearningEventRepository learningEventRepository;
    private final LectureModificationRequestRepository lectureModificationRequestRepository;
    private final PasswordEncoder passwordEncoder;

    /** 강의(Course)별 대표 Lecture + 전체 Lecture 목록을 묶어 카탈로그 결과를 전달한다. */
    private record CatalogData(List<Course> courses,
                              Map<Long, Lecture> sampleLectureByCourse,
                              Map<Long, List<Lecture>> lecturesByCourse) {}

    /** @return 실제로 데이터를 새로 생성했으면 true, 이미 데이터가 있어 건너뛰면 false */
    @Transactional
    public boolean seed(SeedConfig cfg) {
        if (userRepository.count() > 0) {
            log.info("[DataInit] 이미 데이터가 존재해 건너뜀");
            return false;
        }

        String password = passwordEncoder.encode("Test1234!");

        userRepository.save(User.createAdmin("admin@test.com", password, "관리자", null));

        List<Category> categories = saveCategories();
        List<User> sellers        = saveSellers(cfg, password);
        List<User> users          = saveRegularUsers(cfg, password);
        CatalogData catalog       = saveCourses(cfg, sellers, categories);

        seedCommerce(cfg, users, catalog);
        seedLearning(users, catalog);
        seedModificationRequests(catalog);
        return true;
    }

    private List<Category> saveCategories() {
        List<Category> roots = categoryRepository.saveAll(List.of(
                new Category(null, null, "개발",   0),
                new Category(null, null, "디자인", 0),
                new Category(null, null, "비즈니스", 0)
        ));
        List<Category> children = new ArrayList<>(List.of(
                new Category(null, roots.get(0).getId(), "백엔드",     1),
                new Category(null, roots.get(0).getId(), "프론트엔드", 1),
                new Category(null, roots.get(0).getId(), "DevOps",     1),
                new Category(null, roots.get(1).getId(), "UI/UX",      1),
                new Category(null, roots.get(2).getId(), "마케팅",     1)
        ));
        return categoryRepository.saveAll(children);
    }

    private List<User> saveSellers(SeedConfig cfg, String password) {
        List<User> sellers = new ArrayList<>();
        for (int i = 1; i <= cfg.sellerCount(); i++) {
            User seller = userRepository.save(User.createSeller(
                    "seller" + i + "@test.com", password, "강사" + i, null));
            sellerRepository.save(new Seller(null, seller.getId(), LocalDateTime.now(), LocalDateTime.now()));
            sellers.add(seller);
        }
        log.info("[DataInit] 강사 {}명 생성", cfg.sellerCount());
        return sellers;
    }

    private List<User> saveRegularUsers(SeedConfig cfg, String password) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= cfg.userCount(); i++) {
            users.add(User.createUser("user" + i + "@test.com", password, "수강생" + i, null));
        }
        List<User> saved = userRepository.saveAll(users);
        log.info("[DataInit] 수강생 {}명 생성", cfg.userCount());
        return saved;
    }

    private CatalogData saveCourses(SeedConfig cfg, List<User> sellers, List<Category> categories) {
        int categoryCount = categories.size();
        int batchSize = cfg.batchSize();

        List<Course> courseBatch   = new ArrayList<>();
        List<Chapter> chapterBatch = new ArrayList<>();
        List<Lecture> lectureBatch = new ArrayList<>();
        Map<Long, Lecture> sampleLectureByCourse = new HashMap<>();
        Map<Long, List<Lecture>> lecturesByCourse = new HashMap<>();

        for (int s = 0; s < sellers.size(); s++) {
            User seller = sellers.get(s);
            for (int c = 0; c < cfg.coursesPerSeller(); c++) {
                int idx = s * cfg.coursesPerSeller() + c + 1;
                courseBatch.add(Course.createDraft(
                        seller.getId(),
                        categories.get(idx % categoryCount).getId(),
                        "강의 " + idx + " - " + categories.get(idx % categoryCount).getName(),
                        "강의 " + idx + "에 대한 설명입니다.",
//                        "thumb" + idx + ".jpg",
                        "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?q=80&w=800&auto=format&fit=crop",
                        (idx % 5 + 1) * 10000
                ));
            }
        }

        List<Course> savedCourses = courseRepository.saveAll(courseBatch);
        log.info("[DataInit] 강의 {}개 생성", savedCourses.size());

        for (Course course : savedCourses) {
            for (int i = 1; i <= cfg.chaptersPerCourse(); i++) {
                chapterBatch.add(Chapter.create(
                        course.getTitle() + " - " + i + "장",
                        i,
                        course
                ));

                if (chapterBatch.size() >= batchSize) {
                    addLectures(cfg, chapterRepository.saveAll(chapterBatch), lectureBatch);
                    chapterBatch.clear();
                    if (lectureBatch.size() >= batchSize) {
                        persistLectures(lectureBatch, sampleLectureByCourse, lecturesByCourse);
                    }
                }
            }
        }

        if (!chapterBatch.isEmpty()) {
            addLectures(cfg, chapterRepository.saveAll(chapterBatch), lectureBatch);
        }
        if (!lectureBatch.isEmpty()) {
            persistLectures(lectureBatch, sampleLectureByCourse, lecturesByCourse);
        }

        log.info("[DataInit] 챕터 {}개, 강의영상 {}개 생성 완료",
                (long) savedCourses.size() * cfg.chaptersPerCourse(),
                (long) savedCourses.size() * cfg.chaptersPerCourse() * cfg.lecturesPerChapter());

        return new CatalogData(savedCourses, sampleLectureByCourse, lecturesByCourse);
    }

    private void addLectures(SeedConfig cfg, List<Chapter> chapters, List<Lecture> buffer) {
        for (Chapter chapter : chapters) {
            for (int j = 1; j <= cfg.lecturesPerChapter(); j++) {
                String mockUuid = UUID.randomUUID().toString();
                buffer.add(Lecture.createWithStream(
                        "/hls/" + chapter.getId() + "/" + mockUuid + "/index.m3u8",
                        mockUuid,
                        chapter.getTitle() + " - " + j + "강",
                        "강의 요약",
                        600 * j,
                        j,
                        j == 1,
                        chapter
                ));
            }
        }
    }

    /** 강의 영상 저장 + 강의(Course)별 대표/전체 Lecture 캡처 (in-memory 참조라 추가 조회 없음) */
    private void persistLectures(List<Lecture> buffer, Map<Long, Lecture> sampleLectureByCourse,
                                 Map<Long, List<Lecture>> lecturesByCourse) {
        for (Lecture lecture : lectureRepository.saveAll(buffer)) {
            Long courseId = lecture.getChapter().getCourse().getId();
            sampleLectureByCourse.putIfAbsent(courseId, lecture);
            lecturesByCourse.computeIfAbsent(courseId, k -> new ArrayList<>()).add(lecture);
        }
        buffer.clear();
    }

    /** 장바구니 → 주문/결제/수강 (수강생 1명당 1건) */
    private void seedCommerce(SeedConfig cfg, List<User> users, CatalogData catalog) {
        List<Course> courses = catalog.courses();
        LocalDateTime now = LocalDateTime.now();
        int count = Math.min(users.size(), courses.size());

        // 장바구니 (CartItem 은 cascade 로 함께 저장)
        List<Cart> carts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Cart cart = Cart.create(users.get(i).getId());
            cart.addItem(courses.get(i).getId());
            carts.add(cart);
        }
        cartRepository.saveAll(carts);

        // 주문 (결제 완료 상태) — OrderItem 은 addItem 으로 추가되어 cascade 저장된다.
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Course course = courses.get(i);
            Order order = Order.createPendingPayment(users.get(i).getId(), "ORD-SEED-" + (i + 1), now);
            order.addItem(course.getId(), course.getTitle(), course.getThumbnail(), course.getPrice(), now);
            order.markPaid(now);
            orders.add(order);
        }
        List<Order> savedOrders = orderRepository.saveAll(orders);

        // 결제 / 수강등록
        List<Payment> payments = new ArrayList<>();
        List<Enrollment> enrollments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Course course = courses.get(i);
            Order order = savedOrders.get(i);

            Payment payment = Payment.createReady(order, now);
            payment.markProcessing(LocalDateTime.now());
            payment.succeed("PAY-SEED-" + (i + 1), "CARD", now);
            payments.add(payment);

            enrollments.add(Enrollment.createActive(users.get(i).getId(), course.getId(), order, now));
        }
        paymentRepository.saveAll(payments);
        enrollmentRepository.saveAll(enrollments);

        log.info("[DataInit] 커머스 데이터 생성 (주문 {}건)", savedOrders.size());
    }

    /** 학습자 1인이 시청하는 최대 강의 수(진행도 분포용). */
    private static final int LECTURES_PER_LEARNER = 5;
    /** 학습 활동을 흩뿌릴 날짜 범위(잔디/일별진도용). */
    private static final int SPREAD_DAYS = 14;
    /**
     * 학습 연쇄 데이터: 모든 수강생이 강좌의 여러 강의를 여러 날에 걸쳐 시청한 것으로
     * lecture_progresses · learning_events 를 서로 일관되게 생성한다.
     * 일부는 완강(COMPLETE), 일부는 중도 이탈로 진행도 분포를 만든다.
     */
    private void seedLearning(List<User> users, CatalogData catalog) {
        List<Course> courses = catalog.courses();
        Map<Long, List<Lecture>> lecturesByCourse = catalog.lecturesByCourse();
        if (courses.isEmpty()) return;

        LocalDate today = LocalDate.now();

        List<LectureProgress> progresses = new ArrayList<>();
        List<LearningEvent> events = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            Long userId = users.get(i).getId();
            Course course = courses.get(i % courses.size()); // 강좌당 여러 수강생 분포
            Long courseId = course.getId();
            List<Lecture> lectures = lecturesByCourse.getOrDefault(courseId, List.of());
            if (lectures.isEmpty()) continue;

            int watchCount = 1 + (i % Math.min(lectures.size(), LECTURES_PER_LEARNER));
            int completedCount = switch (i % 3) {
                case 0 -> watchCount;                    // 완강형
                case 1 -> Math.max(1, watchCount - 1);   // 거의 완강
                default -> Math.max(0, watchCount - 2);  // 초반 이탈형
            };
            int userOffset = i % SPREAD_DAYS; // 수강생마다 활동 시작일을 흩뿌림

            for (int j = 0; j < watchCount; j++) {
                Lecture lecture = lectures.get(j);
                Long chapterId = lecture.getChapter().getId();
                int duration = lecture.getDurationSeconds();
                boolean completed = j < completedCount;
                LocalDate date = today.minusDays(userOffset + (watchCount - j));
                LocalDateTime base = date.atTime(20, 0).plusMinutes(j);

                int watched = completed ? duration : Math.max(60, duration * 6 / 10);
                int rate = completed ? 100 : Math.min(89, watched * 100 / Math.max(1, duration));
                progresses.add(new LectureProgress(
                        null, lecture.getId(), userId, watched, watched, rate,
                        completed, completed ? base.plusSeconds(duration) : null, base, base));

                String k = "evt-seed-" + userId + "-" + lecture.getId() + "-";
                events.add(LearningEvent.create(userId, courseId, chapterId, lecture.getId(),
                        LearningEventType.LECTURE_ENTER, 0, base, k + "enter"));
                events.add(LearningEvent.create(userId, courseId, chapterId, lecture.getId(),
                        LearningEventType.VIDEO_PAUSE, watched, base.plusSeconds(2), k + "pause"));
                if (completed) {
                    events.add(LearningEvent.create(userId, courseId, chapterId, lecture.getId(),
                            LearningEventType.LECTURE_COMPLETE, duration, base.plusSeconds(duration), k + "complete"));
                }
                events.add(LearningEvent.create(userId, courseId, chapterId, lecture.getId(),
                        LearningEventType.LECTURE_EXIT, watched, base.plusSeconds(watched), k + "exit"));

            }
        }

        lectureProgressRepository.saveAll(progresses);
        learningEventRepository.saveAll(events);

        log.info("[DataInit] 학습 연쇄 데이터 — 진행 {}건, 이벤트 {}건", progresses.size(), events.size());
    }

    /** 강의 영상 수정 요청 (강좌당 1건) */
    private void seedModificationRequests(CatalogData catalog) {
        List<LectureModificationRequest> modRequests = new ArrayList<>();
        for (Course course : catalog.courses()) {
            Lecture lecture = catalog.sampleLectureByCourse().get(course.getId());
            String mockUuid = UUID.randomUUID().toString();
            modRequests.add(new LectureModificationRequest(
                    null, lecture, course.getInstructorId(), "영상 교체 요청 " + course.getId(),
                    lecture.getM3u8Path(), "/hls/new/" + mockUuid + "/index.m3u8", mockUuid, RequestStatus.PENDING, null, null));
        }
        lectureModificationRequestRepository.saveAll(modRequests);

        log.info("[DataInit] 영상 수정 요청 데이터 생성 ({}건)", modRequests.size());
    }

    // =========================================================================
    // 데모 전용 시나리오 보강 (DemoDataInitializer 에서만 호출)
    // -------------------------------------------------------------------------
    // seed() 가 만든 "단일 상태" 데이터를, 기능/정책 요구사항이 시연에서 드러나도록
    // 다양한 상태·흐름으로 펼친다. simple/bulk 는 호출하지 않으므로 영향 없음.
    // 강좌-수강 인덱스 매핑(seedCommerce): user[i] ↔ course[i]
    // =========================================================================
    @Transactional
    public void seedDemoScenarios() {
        Long adminId = userRepository.findByEmail("admin@test.com").orElseThrow().getId();

        List<Course> courses = courseRepository.findAll();
        courses.sort(Comparator.comparing(Course::getId));
        List<User> learners = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ROLE_USER)
                .sorted(Comparator.comparing(User::getId))
                .toList();

        if (courses.size() < 10 || learners.size() < 11) {
            log.warn("[DataInit] 데모 시나리오 보강 스킵 (데이터 부족: 강좌 {}, 수강생 {})",
                    courses.size(), learners.size());
            return;
        }

        demoCourseLifecycle(courses, adminId);
        demoRefund(courses, learners);
        demoLearningFlow(courses, learners);
        demoLectureModificationRequests(courses, adminId);

        log.info("[DataInit] 데모 시나리오 보강 완료");
    }

    // =========================================================================
    // demo 모드 전용: "의미있는 이름" 페르소나로 엣지/예외 케이스를 또렷이 보여주는 쇼케이스.
    // 파라미터 기반 seed() 와 별개로, 적은 수의 계정을 손으로 구성한다. (비밀번호 모두 Test1234!)
    // =========================================================================
    @Transactional
    public boolean seedPersonaShowcase() {
        if (userRepository.count() > 0) {
            log.info("[DataInit] 이미 데이터가 존재해 페르소나 쇼케이스를 건너뜀");
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        String pw = passwordEncoder.encode("Test1234!");

        User admin = userRepository.save(User.createAdmin("admin@test.com", pw, "관리자", null));

        List<Category> cats = saveCategories(); // 백엔드/프론트엔드/DevOps/UI/UX/마케팅
        Long catBackend = cats.get(0).getId();
        Long catFront = cats.get(1).getId();
        Long catDevops = cats.get(2).getId();
        Long catUiux = cats.get(3).getId();
        Long catMkt = cats.get(4).getId();

        // ── 판매자 + 강좌(생명주기 상태) ──────────────────────────────────────
        List<CourseStatusHistory> hist = new ArrayList<>();

        User sellerPopular = saveShowcaseSeller("seller1@test.com", "인기강사", pw);
        Course coursePopular = buildShowcaseCourse(sellerPopular, catBackend, "스프링 부트 실전", 30000);
        hist.add(coursePopular.requestReview(sellerPopular.getId()));
        hist.add(coursePopular.approve(admin.getId())); // ON_SALE

        User sellerPending = saveShowcaseSeller("seller2@test.com", "승인대기판매자", pw);
        Course coursePending = buildShowcaseCourse(sellerPending, catFront, "React 입문", 20000);
        hist.add(coursePending.requestReview(sellerPending.getId())); // IN_REVIEW(승인 대기)

        User sellerRejected = saveShowcaseSeller("seller3@test.com", "반려당한판매자", pw);
        Course courseRejected = buildShowcaseCourse(sellerRejected, catDevops, "도커 기초", 15000);
        hist.add(courseRejected.requestReview(sellerRejected.getId()));
        hist.add(courseRejected.reject(admin.getId(), "커리큘럼 구성이 미흡하여 반려합니다.")); // SUSPENDED(반려)

        User sellerClosed = saveShowcaseSeller("seller4@test.com", "판매중지판매자", pw);
        Course courseClosed = buildShowcaseCourse(sellerClosed, catUiux, "피그마 마스터", 25000);
        hist.add(courseClosed.requestReview(sellerClosed.getId()));
        hist.add(courseClosed.approve(admin.getId()));
        hist.add(courseClosed.close(sellerClosed.getId())); // SUSPENDED(판매중지)

        User sellerDraft = saveShowcaseSeller("seller5@test.com", "초안작성자", pw);
        Course courseDraft = buildShowcaseCourse(sellerDraft, catMkt, "퍼포먼스 마케팅", 18000); // DRAFT 유지

        Course courseJpa = buildShowcaseCourse(sellerPopular, catBackend, "JPA 성능 튜닝", 35000);
        hist.add(courseJpa.requestReview(sellerPopular.getId()));
        hist.add(courseJpa.approve(admin.getId()));

        Course courseTypeScript = buildShowcaseCourse(sellerPending, catFront, "TypeScript 실무", 28000);
        hist.add(courseTypeScript.requestReview(sellerPending.getId()));
        hist.add(courseTypeScript.approve(admin.getId()));

        Course courseKubernetes = buildShowcaseCourse(sellerRejected, catDevops, "쿠버네티스 운영", 42000);
        hist.add(courseKubernetes.requestReview(sellerRejected.getId()));
        hist.add(courseKubernetes.approve(admin.getId()));

        Course courseDesignSystem = buildShowcaseCourse(sellerClosed, catUiux, "디자인 시스템 구축", 32000);
        hist.add(courseDesignSystem.requestReview(sellerClosed.getId()));
        hist.add(courseDesignSystem.approve(admin.getId()));

        Course courseGrowth = buildShowcaseCourse(sellerDraft, catMkt, "그로스 마케팅 분석", 26000);
        hist.add(courseGrowth.requestReview(sellerDraft.getId()));
        hist.add(courseGrowth.approve(admin.getId()));

        List<Course> showcaseCourses = List.of(
                coursePopular, coursePending, courseRejected, courseClosed, courseDraft,
                courseJpa, courseTypeScript, courseKubernetes, courseDesignSystem, courseGrowth);

        courseStatusHistoryRepository.saveAll(hist);

        List<Lecture> popularLectures = lecturesOf(coursePopular);
        Lecture freeLecture = popularLectures.get(0); // 1장 1강 = 무료 미리보기
        Lecture firstPaidLecture = popularLectures.get(1);

        List<LectureProgress> progresses = new ArrayList<>();
        List<LearningEvent> events = new ArrayList<>();

        // ── 수강생 페르소나 ──────────────────────────────────────────────────
        // 완강수강생: 인기강좌 수강 + 전 강의 100% + LECTURE_COMPLETE
        User learnerComplete = saveLearner("user1@test.com", "완강수강생", pw);
        enroll(learnerComplete, coursePopular, now, "COMPLETE");
        for (Lecture lec : popularLectures) {
            progresses.add(completedProgress(learnerComplete.getId(), lec, now));
            events.add(LearningEvent.create(learnerComplete.getId(), coursePopular.getId(),
                    lec.getChapter().getId(), lec.getId(), LearningEventType.LECTURE_COMPLETE,
                    lec.getDurationSeconds(), now, "evt-complete-" + lec.getId()));
        }

        // 진행중수강생: 수강 + 일부 진행
        User learnerInProgress = saveLearner("learner-inprogress@test.com", "진행중수강생", pw);
        enroll(learnerInProgress, coursePopular, now, "INPROG");
        LectureProgress partial = LectureProgress.start(learnerInProgress.getId(), firstPaidLecture.getId(), 0, now);
        partial.applyProgress(firstPaidLecture.getDurationSeconds() / 2,
                firstPaidLecture.getDurationSeconds() / 2, firstPaidLecture.getDurationSeconds(), now);
        progresses.add(partial);
        events.add(LearningEvent.create(learnerInProgress.getId(), coursePopular.getId(),
                firstPaidLecture.getChapter().getId(), firstPaidLecture.getId(),
                LearningEventType.LECTURE_ENTER, 0, now, "evt-inprog-enter"));

        // 환불수강생: 수강 후 환불 → CANCELED
        User learnerRefund = saveLearner("learner-refunded@test.com", "환불수강생", pw);
        Enrollment refundEnrollment = enroll(learnerRefund, coursePopular, now, "REFUND");
        refundEnrollment.cancel(now);

        // 맛보기시청자: 미구매·미수강. 무료 미리보기 강의 시청 이벤트만 존재
        User learnerPreview = saveLearner("learner-preview@test.com", "맛보기시청자", pw);
        events.add(LearningEvent.create(learnerPreview.getId(), coursePopular.getId(),
                freeLecture.getChapter().getId(), freeLecture.getId(),
                LearningEventType.VIDEO_PAUSE, freeLecture.getDurationSeconds(), now, "evt-preview-pause"));

        // 판매중지강좌수강생: 판매중지된 강좌도 기존 수강생은 계속 수강 가능
        User learnerClosedCourse = saveLearner("learner-closed@test.com", "판매중지강좌수강생", pw);
        enroll(learnerClosedCourse, courseClosed, now, "CLOSED");

        lectureProgressRepository.saveAll(progresses);
        learningEventRepository.saveAll(events);

        seedSixMonthDemoUsage(showcaseCourses, courseClosed, List.of(
                learnerComplete, learnerInProgress, learnerRefund, learnerPreview, learnerClosedCourse), now);

        log.info("[DataInit] 페르소나 쇼케이스 생성 완료 (판매자 5 · 강좌 10 · 수강생 5 · 관리자 1)");
        return true;
    }

    // ── 페르소나 쇼케이스 헬퍼 ────────────────────────────────────────────────
    private User saveShowcaseSeller(String email, String nickname, String pw) {
        User user = userRepository.save(User.createSeller(email, pw, nickname, null));
        sellerRepository.save(new Seller(null, user.getId(), LocalDateTime.now(), LocalDateTime.now()));
        return user;
    }

    private User saveLearner(String email, String nickname, String pw) {
        return userRepository.save(User.createUser(email, pw, nickname, null));
    }

    /** 챕터 2개 × 강의 2개(1강은 무료 미리보기)를 가진 강좌를 만들어 저장한다. */
    private Course buildShowcaseCourse(User seller, Long categoryId, String title, int price) {
        Course course = Course.createDraft(seller.getId(), categoryId, title,
                title + " 강좌입니다.",
//                "thumb-demo.jpg",
                "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?q=80&w=800&auto=format&fit=crop",
                price);
        for (int ch = 1; ch <= 2; ch++) {
            Chapter chapter = Chapter.create(ch + "장", ch, course);
            course.addChapter(chapter);
            for (int j = 1; j <= 2; j++) {
                String uuid = UUID.randomUUID().toString();
                chapter.addLecture(Lecture.createWithStream(
                        "/hls/demo/" + uuid + "/index.m3u8", uuid,
                        ch + "장 " + j + "강", "강의 요약", 600 * j, j, j == 1, chapter));
            }
        }
        return courseRepository.save(course); // chapters/lectures cascade
    }

    private List<Lecture> lecturesOf(Course course) {
        List<Lecture> result = new ArrayList<>();
        course.getChapters().forEach(c -> result.addAll(c.getLectures()));
        return result;
    }

    /** 주문(PAID)·결제(SUCCESS)·수강(ACTIVE)을 한 번에 생성한다. */
    private Enrollment enroll(User learner, Course course, LocalDateTime now, String tag) {
        Order order = Order.createPendingPayment(
                learner.getId(), "ORD-DEMO-" + tag + "-" + learner.getId(), now);
            order.addItem(course.getId(), course.getTitle(), course.getThumbnail(), course.getPrice(), now);
        order.markPaid(now);
        orderRepository.save(order);

        Payment payment = Payment.createReady(order, now);
        payment.markProcessing(now);
        payment.succeed("PAY-DEMO-" + tag + "-" + learner.getId(), "CARD", now);
        paymentRepository.save(payment);

        return enrollmentRepository.save(Enrollment.createActive(learner.getId(), course.getId(), order, now));
    }

    private LectureProgress completedProgress(Long userId, Lecture lec, LocalDateTime now) {
        LectureProgress p = LectureProgress.start(userId, lec.getId(), 0, now);
        p.applyProgress(lec.getDurationSeconds(), lec.getDurationSeconds(), lec.getDurationSeconds(), now);
        return p;
    }

    /**
     * 6개월 사용 흔적: 수강생마다 3~5개 강좌를 듣고, 강의별 시청 이벤트/진행도를 남긴다.
     * 쇼케이스의 단일 엣지 케이스 위에 실제 서비스처럼 누적된 데이터를 얹는 데모 전용 보강이다.
     */
    private void seedSixMonthDemoUsage(List<Course> courses, Course closedCourse, List<User> learners, LocalDateTime now) {
        List<Course> learnableCourses = courses.stream()
                .filter(course -> course.getStatus() == CourseStatus.ON_SALE || course.getId().equals(closedCourse.getId()))
                .toList();

        Set<String> existingEnrollments = new HashSet<>();
        Set<String> activeEnrollments = new HashSet<>();
        enrollmentRepository.findAll().forEach(e -> {
            String key = e.getUserId() + ":" + e.getCourseId();
            existingEnrollments.add(key);
            if (e.getStatus() == EnrollmentStatus.ACTIVE) {
                activeEnrollments.add(key);
            }
        });

        Set<String> existingProgresses = new HashSet<>();
        lectureProgressRepository.findAll().forEach(p -> existingProgresses.add(p.getUserId() + ":" + p.getLectureId()));

        List<LectureProgress> progresses = new ArrayList<>();
        List<LearningEvent> events = new ArrayList<>();

        LocalDate startDate = now.toLocalDate().minusMonths(6).plusDays(3);
        int coursePool = learnableCourses.size();
        for (int userIndex = 0; userIndex < learners.size(); userIndex++) {
            User learner = learners.get(userIndex);
            int targetCourseCount = 3 + (userIndex % 3); // 3~5개
            int enrolledCourseCount = 0;

            for (int offset = 0; offset < coursePool && enrolledCourseCount < targetCourseCount; offset++) {
                Course course = learnableCourses.get((userIndex + offset * 2) % coursePool);
                String enrollmentKey = learner.getId() + ":" + course.getId();
                if (existingEnrollments.contains(enrollmentKey)) {
                    continue;
                }

                enroll(learner, course, now.minusMonths(6).plusDays(userIndex * 3L + offset), "6M-" + userIndex + "-" + offset);
                existingEnrollments.add(enrollmentKey);
                activeEnrollments.add(enrollmentKey);
                enrolledCourseCount++;
            }

            List<Course> learnerCourses = learnableCourses.stream()
                    .filter(course -> activeEnrollments.contains(learner.getId() + ":" + course.getId()))
                    .limit(targetCourseCount)
                    .toList();

            for (int courseIndex = 0; courseIndex < learnerCourses.size(); courseIndex++) {
                Course course = learnerCourses.get(courseIndex);
                List<Lecture> lectures = orderedLectures(course);
                if (lectures.isEmpty()) continue;

                int completedLectures = 1 + ((userIndex + courseIndex) % lectures.size());

                for (int lectureIndex = 0; lectureIndex < lectures.size(); lectureIndex++) {
                    Lecture lecture = lectures.get(lectureIndex);
                    boolean complete = lectureIndex < completedLectures;
                    int watchRate = complete ? 100 : 35 + ((userIndex + lectureIndex) % 4) * 10;
                    int watchedSeconds = lecture.getDurationSeconds() * watchRate / 100;
                    LocalDateTime watchedAt = startDate
                            .plusDays((long) courseIndex * 23 + (long) lectureIndex * 11 + userIndex)
                            .atTime(19, 0)
                            .plusMinutes(lectureIndex * 12L);

                    String progressKey = learner.getId() + ":" + lecture.getId();
                    if (!existingProgresses.contains(progressKey)) {
                        progresses.add(new LectureProgress(
                                null, lecture.getId(), learner.getId(), watchedSeconds, watchedSeconds,
                                watchRate, complete, complete ? watchedAt.plusMinutes(9) : null, watchedAt, watchedAt));
                        existingProgresses.add(progressKey);
                    }

                    appendSixMonthWatchEvents(events, learner.getId(), lecture, watchedAt, complete, watchedSeconds);
                }
            }
        }

        lectureProgressRepository.saveAll(progresses);
        learningEventRepository.saveAll(events);

        log.info("[DataInit] 6개월 데모 사용 흔적 생성 — 진행 {}건, 이벤트 {}건", progresses.size(), events.size());
    }

    private int appendSixMonthWatchEvents(List<LearningEvent> out, Long userId, Lecture lec,
                                          LocalDateTime base, boolean complete, int watchedSeconds) {
        Long courseId = lec.getChapter().getCourse().getId();
        Long chapterId = lec.getChapter().getId();
        Long lecId = lec.getId();
        int duration = lec.getDurationSeconds();
        String key = "sixm-" + userId + "-" + lecId + "-" + base.toLocalDate() + "-";

        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_ENTER, 0, base, key + "enter"));
        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.VIDEO_PAUSE, watchedSeconds, base.plusMinutes(8), key + "pause"));
        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_EXIT, watchedSeconds, base.plusMinutes(9), key + "exit"));
        if (complete) {
            out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_COMPLETE, duration, base.plusMinutes(9).plusSeconds(1), key + "complete"));
            return 6;
        }
        return 5;
    }

    /** 강좌 생명주기: ON_SALE 5 / IN_REVIEW 1 / SUSPENDED(판매중지) 2 / SUSPENDED(반려) 1 / DRAFT 1 */
    private void demoCourseLifecycle(List<Course> courses, Long adminId) {
        List<CourseStatusHistory> histories = new ArrayList<>();

        // 0~4: 심사요청 → 승인 → ON_SALE
        for (int i = 0; i <= 4; i++) {
            Course c = courses.get(i);
            histories.add(c.requestReview(c.getInstructorId()));
            histories.add(c.approve(adminId));
        }
        // 5: 심사 중(IN_REVIEW)
        Course inReview = courses.get(5);
        histories.add(inReview.requestReview(inReview.getInstructorId()));
        // 6, 8: ON_SALE 후 판매자 판매중지(close) → SUSPENDED
        for (int i : List.of(6, 8)) {
            Course c = courses.get(i);
            histories.add(c.requestReview(c.getInstructorId()));
            histories.add(c.approve(adminId));
            histories.add(c.close(c.getInstructorId()));
        }
        // 7: 관리자 반려(reject) → SUSPENDED
        Course rejected = courses.get(7);
        histories.add(rejected.requestReview(rejected.getInstructorId()));
        histories.add(rejected.reject(adminId, "커리큘럼 구성이 미흡하여 반려합니다."));
        // 9: DRAFT 유지

        courseStatusHistoryRepository.saveAll(histories);
    }

    /** 환불 시나리오: user[4] 수강 취소(CANCELED). */
    private void demoRefund(List<Course> courses, List<User> learners) {
        Long courseId = courses.get(4).getId();
        Long userId = learners.get(4).getId();
        enrollmentRepository.findAll().stream()
                .filter(e -> courseId.equals(e.getCourseId()) && userId.equals(e.getUserId()))
                .findFirst()
                .ifPresent(e -> e.cancel(LocalDateTime.now()));
    }

    /**
     * 학습 흐름: 이벤트(ENTER~COMPLETE) + 완강/진행중 진행도.
     * <p>
     * lecture_progresses 는 (user_id, lecture_id) 유니크 제약이 있고
     * seedLearning() 이 각 강좌 1강에 user[i] 진행도를 이미 만들어 두었다. 충돌을 피하려고
     * 1강은 기존 행을 완강으로 끌어올리고(in-place), 2강 이후만 새 행으로 추가한다.
     */
    private void demoLearningFlow(List<Course> courses, List<User> learners) {
        LocalDateTime now = LocalDateTime.now();

        Course course0 = courses.get(0);
        Long user0 = learners.get(0).getId();
        List<Lecture> lectures0 = orderedLectures(course0);

        List<LearningEvent> events = new ArrayList<>();
        List<LectureProgress> newProgresses = new ArrayList<>();

        // 1강: seed 가 만든 진행도를 완강으로 끌어올림
        if (!lectures0.isEmpty()) {
            Lecture lec0 = lectures0.get(0);
            int dur0 = lec0.getDurationSeconds();
            appendWatchEvents(events, user0, lec0, now.minusDays(4), true);
            lectureProgressRepository.findByUserIdAndLectureId(user0, lec0.getId())
                    .ifPresent(p -> p.applyProgress(dur0, dur0, dur0, now)); // 누적 시청 → 100% 완강
        }
        // 2·3강: 신규 완강
        for (int i = 1; i <= 2 && i < lectures0.size(); i++) {
            Lecture lec = lectures0.get(i);
            LocalDateTime base = now.minusDays(3 - i); // 날짜 분산(히트맵용)
            appendWatchEvents(events, user0, lec, base, true);
            newProgresses.add(completedProgress(user0, lec.getId(), lec.getDurationSeconds(), base));
        }
        // 4강: 신규 진행중(40%)
        if (lectures0.size() > 3) {
            Lecture lec = lectures0.get(3);
            appendWatchEvents(events, user0, lec, now, false);
            newProgresses.add(partialProgress(user0, lec.getId(), lec.getDurationSeconds(), 40, now));
        }

        // user[2] / course2 : seed 진행도(진행중) 위에 시청 이벤트만 보강
        Course course2 = courses.get(2);
        Long user2 = learners.get(2).getId();
        List<Lecture> lectures2 = orderedLectures(course2);
        if (!lectures2.isEmpty()) {
            appendWatchEvents(events, user2, lectures2.get(0), now, false);
        }

        learningEventRepository.saveAll(events);
        lectureProgressRepository.saveAll(newProgresses);
    }

    /** 영상 수정 요청: PENDING / APPROVED / REJECTED 각 1건. */
    private void demoLectureModificationRequests(List<Course> courses, Long adminId) {
        Course course0 = courses.get(0);
        Long instructor = course0.getInstructorId();
        List<Lecture> lectures = orderedLectures(course0);
        if (lectures.size() < 3) {
            return;
        }

        lectureModificationRequestRepository.save(
                pendingModRequest(lectures.get(0), instructor, "오디오 음질 개선본 재업로드"));

        LectureModificationRequest approved = lectureModificationRequestRepository.save(
                pendingModRequest(lectures.get(1), instructor, "자막 추가본 재업로드"));
        approved.approve(adminId);

        LectureModificationRequest rejected = lectureModificationRequestRepository.save(
                pendingModRequest(lectures.get(2), instructor, "화질 변경본 재업로드"));
        rejected.reject("기존 영상보다 화질이 낮아 반려합니다.", adminId);
    }

    private LectureModificationRequest pendingModRequest(Lecture lecture, Long instructorId, String description) {
        String uuid = UUID.randomUUID().toString();
        String afterPath = "/hls/" + lecture.getChapter().getId() + "/" + uuid + "/index.m3u8";
        return LectureModificationRequest.createPending(lecture, instructorId, description, afterPath, uuid);
    }

    // --- 데모 보강 헬퍼 ---------------------------------------------------------

    /** 강좌의 강의영상을 (챕터 순서 → 강의 순서)로 평탄화. 트랜잭션 내 lazy 로딩 전제. */
    private List<Lecture> orderedLectures(Course course) {
        List<Lecture> result = new ArrayList<>();
        course.getChapters().stream()
                .sorted(Comparator.comparingInt(Chapter::getOrderNo))
                .forEach(ch -> ch.getLectures().stream()
                        .sorted(Comparator.comparingInt(Lecture::getOrderNo))
                        .forEach(result::add));
        return result;
    }

    /** 강의 시청 이벤트 시퀀스: ENTER → START → END → EXIT (완강 시 COMPLETE). */
    private void appendWatchEvents(List<LearningEvent> out, Long userId, Lecture lec,
                                   LocalDateTime base, boolean complete) {
        Long courseId = lec.getChapter().getCourse().getId();
        Long chapterId = lec.getChapter().getId();
        Long lecId = lec.getId();
        int dur = lec.getDurationSeconds();
        int endPos = complete ? dur : dur * 4 / 10;
        String k = userId + "-" + lecId + "-";

        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_ENTER, 0, base, k + "enter"));
        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.VIDEO_PAUSE, endPos, base.plusMinutes(8), k + "pause"));
        out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_EXIT, endPos, base.plusMinutes(9), k + "exit"));
        if (complete) {
            out.add(LearningEvent.create(userId, courseId, chapterId, lecId, LearningEventType.LECTURE_COMPLETE, dur, base.plusMinutes(9).plusSeconds(1), k + "complete"));
        }
    }

    private LectureProgress completedProgress(Long userId, Long lectureId, int dur, LocalDateTime when) {
        return new LectureProgress(null, lectureId, userId, dur, dur, 100, true, when.plusMinutes(9), when, when);
    }

    private LectureProgress partialProgress(Long userId, Long lectureId, int dur, int rate, LocalDateTime when) {
        int pos = dur * rate / 100;
        return new LectureProgress(null, lectureId, userId, pos, pos, rate, false, null, when, when);
    }
}
