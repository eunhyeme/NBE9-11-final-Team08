-- 핵심 기능(판매자 강좌 등록 → 구매 → 수강 → 대시보드) 기준 baseline 스키마.
-- 기존 V1~V19 를 하나로 합치고, 삭제된 기능(쿠폰·스터디·피드·출석·Q&A·회고·AI 피드백·Spring Batch)의 테이블은 제외했다.

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    profile_image VARCHAR(255),
    role ENUM ('ROLE_ADMIN', 'ROLE_SELLER', 'ROLE_USER') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT,
    token_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE seller (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_seller_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_category_id BIGINT,
    name VARCHAR(255) NOT NULL,
    depth INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE courses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    instructor_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    thumbnail VARCHAR(255) NOT NULL,
    price INT NOT NULL,
    status ENUM ('DELETED', 'DRAFT', 'IN_REVIEW', 'ON_SALE', 'SUSPENDED') NOT NULL,
    view_count INT NOT NULL,
    deleted_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE course_status_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    from_status ENUM ('DELETED', 'DRAFT', 'IN_REVIEW', 'ON_SALE', 'SUSPENDED'),
    to_status ENUM ('DELETED', 'DRAFT', 'IN_REVIEW', 'ON_SALE', 'SUSPENDED'),
    reason VARCHAR(1000),
    changed_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE chapters (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    order_no INT NOT NULL,
    deleted_at DATETIME(6),
    course_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chapter_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB;

CREATE TABLE lectures (
    id BIGINT NOT NULL AUTO_INCREMENT,
    m3u8path VARCHAR(255) NOT NULL,
    video_uuid VARCHAR(36),
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(255),
    duration_seconds INT NOT NULL,
    order_no INT NOT NULL,
    is_free_preview BIT NOT NULL,
    deleted_at DATETIME(6),
    chapter_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_lecture_chapter_id (chapter_id),
    KEY idx_lectures_video_uuid (video_uuid),
    CONSTRAINT fk_lecture_chapter FOREIGN KEY (chapter_id) REFERENCES chapters (id)
) ENGINE=InnoDB;

CREATE TABLE lecture_modification_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    instructor_id BIGINT NOT NULL,
    description TEXT NOT NULL,
    beforem3u8path VARCHAR(255) NOT NULL,
    afterm3u8path VARCHAR(255) NOT NULL,
    after_video_uuid VARCHAR(36),
    status ENUM ('APPROVED', 'PENDING', 'REJECTED') NOT NULL,
    rejected_reason TEXT,
    managed_by BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_lecture_mod_requests_after_video_uuid (after_video_uuid),
    CONSTRAINT fk_lecture_modification_lecture FOREIGN KEY (lecture_id) REFERENCES lectures (id)
) ENGINE=InnoDB;

CREATE TABLE s3_cleanup_dlq (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lecture_id BIGINT NOT NULL,
    target_dir_name VARCHAR(255) NOT NULL,
    status ENUM ('DEAD', 'PENDING') NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    failed_at DATETIME(6) NOT NULL,
    next_retry_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_s3_cleanup_dlq_status_retry (status, next_retry_at)
) ENGINE=InnoDB;

CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_item_cart_course (cart_id, course_id),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES carts (id)
) ENGINE=InnoDB;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(255) NOT NULL,
    total_price INT NOT NULL,
    discount_price INT NOT NULL,
    final_price INT NOT NULL,
    status ENUM ('CANCELED', 'EXPIRED', 'PAID', 'PENDING_PAYMENT', 'REFUNDED') NOT NULL,
    ordered_at DATETIME(6) NOT NULL,
    paid_at DATETIME(6),
    canceled_at DATETIME(6),
    refunded_at DATETIME(6),
    expired_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_number (order_number)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    course_title VARCHAR(255) NOT NULL,
    course_thumbnail VARCHAR(255),
    price INT NOT NULL,
    discount_price INT NOT NULL,
    final_price INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    provider ENUM ('MOCK', 'TOSS', 'NICEPAY', 'KCP') NOT NULL DEFAULT 'MOCK',
    payment_key VARCHAR(255),
    method VARCHAR(255),
    amount INT NOT NULL,
    status ENUM ('READY', 'PROCESSING', 'SUCCESS', 'DECLINED', 'UNKNOWN', 'CANCELED', 'REFUNDED') NOT NULL,
    idempotency_key VARCHAR(255),
    failure_code VARCHAR(255),
    failure_message VARCHAR(255),
    paid_at DATETIME(6),
    failed_reason VARCHAR(255),
    canceled_at DATETIME(6),
    refunded_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_order (order_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE TABLE payment_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    provider ENUM ('MOCK', 'TOSS', 'NICEPAY', 'KCP') NOT NULL,
    status ENUM ('REQUESTED', 'SUCCESS', 'DECLINED', 'PROVIDER_ERROR', 'TIMEOUT', 'UNKNOWN') NOT NULL,
    amount INT NOT NULL,
    payment_key VARCHAR(255),
    idempotency_key VARCHAR(255),
    failure_code VARCHAR(255),
    failure_message VARCHAR(255),
    requested_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_attempt_payment_id_idempotency_key (payment_id, idempotency_key),
    KEY idx_payment_attempt_payment_created_at (payment_id, created_at),
    KEY idx_payment_attempt_provider_status (provider, status),
    CONSTRAINT fk_payment_attempt_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB;

CREATE TABLE payment_success_outbox_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    status ENUM ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'DEAD') NOT NULL,
    last_error LONGTEXT,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6),
    processed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_success_outbox_payment (payment_id),
    KEY idx_payment_success_outbox_status_id (status, id),
    CONSTRAINT fk_payment_success_outbox_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_payment_success_outbox_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE TABLE enrollments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    status ENUM ('ACTIVE', 'CANCELED', 'EXPIRED') NOT NULL,
    enrolled_at DATETIME(6) NOT NULL,
    canceled_at DATETIME(6),
    expired_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_enrollment_user_status_course (user_id, status, course_id),
    KEY idx_enrollment_order_status (order_id, status),
    CONSTRAINT fk_enrollment_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE TABLE lecture_progresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    last_position_seconds INT NOT NULL,
    watched_seconds INT NOT NULL,
    progress_rate INT NOT NULL,
    completed BIT NOT NULL,
    completed_at DATETIME(6),
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lecture_progress_user_lecture (user_id, lecture_id)
) ENGINE=InnoDB;

CREATE TABLE last_watched_lectures (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    lecture_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_last_watched_user_course (user_id, course_id)
) ENGINE=InnoDB;

CREATE TABLE learning_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_id BIGINT,
    chapter_id BIGINT,
    lecture_id BIGINT,
    event_type ENUM ('LECTURE_COMPLETE', 'LECTURE_ENTER', 'LECTURE_EXIT', 'VIDEO_PAUSE') NOT NULL,
    position_seconds INT,
    event_time DATETIME(6) NOT NULL,
    unique_event_key VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uk_learning_event_key (unique_event_key)
) ENGINE=InnoDB;
