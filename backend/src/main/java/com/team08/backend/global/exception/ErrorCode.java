package com.team08.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── 공통 ──────────────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 파라미터 요청입니다. 입력 값을 확인해주세요."),
    SYSTEM_BUSY(HttpStatus.SERVICE_UNAVAILABLE, "COMMON_503", "서버 처리 용량이 초과되었습니다. 잠시 후 다시 시도해주세요."),

    // ── Admin ────────────────────────────────────────────────────────────
    ADMIN_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ADMIN_001", "관리자만 접근할 수 있습니다."),

    // ── Auth ─────────────────────────────────────────────────────────────
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "AUTH_001", "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_002", "이미 존재하는 이메일입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_004", "인증이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005", "유효하지 않은 refresh token입니다."),

    // ── User ─────────────────────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,   "USER_001", "사용자를 찾을 수 없습니다."),

    // ── Course ───────────────────────────────────────────────────────────
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND,      "COURSE_001", "강좌를 찾을 수 없습니다."),
    COURSE_NOT_ON_SALE(HttpStatus.CONFLICT, "COURSE_002", "판매 중인 강의만 장바구니에 담을 수 있습니다."),
    UNAUTHORIZED_COURSE_OWNER(HttpStatus.FORBIDDEN, "COURSE_003", "해당 강좌의 소유자가 아닙니다."),
    INVALID_COURSE_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "COURSE_004", "변경할 수 없는 강좌 상태입니다."),
    COURSE_CURRICULUM_EMPTY(HttpStatus.BAD_REQUEST, "COURSE_005", "강좌에 최소 1개 이상의 챕터와 강의가 존재해야 심사를 요청할 수 있습니다."),
    REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "COURSE_006", "강좌 심사 반려 시 사유는 필수입니다."),
    COURSE_HAS_ACTIVE_ENROLLMENTS(HttpStatus.BAD_REQUEST, "COURSE_007", "수강생이 존재하므로 삭제할 수 없습니다"),
    LECTURE_MODIFICATION_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_008", "강의 변경 신청 내역을 찾을 수 없습니다."),
    INVALID_ORDER_REQUEST(HttpStatus.BAD_REQUEST, "COURSE_009", "유효하지 않은 순서 변경 요청입니다."),
    COURSE_ACCESS_DENIED(HttpStatus.FORBIDDEN,       "COURSE_010", "강좌에 접근할 권한이 없습니다."),
    COURSE_ALREADY_ON_SALE(HttpStatus.BAD_REQUEST, "COURSE_011", "이미 공개 판매 중인 강좌는 커리큘럼(챕터/강의)을 수정할 수 없습니다."),

    // ── Lecture ──────────────────────────────────────────────────────────
    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_001", "강의를 찾을 수 없습니다."),
    CHAPTER_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_002", "챕터를 찾을 수 없습니다."),
    LECTURE_NOT_FOUND_IN_CHAPTER(HttpStatus.NOT_FOUND,     "LECTURE_003", "챕터에 강의가 없습니다."),
    RECENT_LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND,     "LECTURE_004", "최근 수강한 강의가 없습니다."),

    // ── Order ────────────────────────────────────────────────────────────
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND,       "ORDER_001", "주문을 찾을 수 없습니다."),
    EMPTY_CART(HttpStatus.BAD_REQUEST,     "ORDER_002", "장바구니가 비어 있습니다."),
    LECTURE_ALREADY_ENROLLED(HttpStatus.CONFLICT,        "ORDER_003", "이미 수강 중인 강의입니다."),
    PAID_ORDER_CANNOT_BE_CANCELED(HttpStatus.CONFLICT,        "ORDER_004", "결제 완료 주문은 취소할 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT,        "ORDER_005", "취소할 수 없는 주문 상태입니다."),
    INVALID_ORDER_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "ORDER_006", "잘못된 주문 상태 전이입니다."),
    ORDER_ITEM_ALREADY_EXISTS(HttpStatus.CONFLICT, "ORDER_007", "이미 주문에 포함된 강의입니다."),

    // ── Enrollment ───────────────────────────────────────────────────────
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND,  "ENROLLMENT_001", "수강 중인 강의를 찾을 수 없습니다."),
    INVALID_ENROLLMENT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "ENROLLMENT_002", "잘못된 수강권 상태 전이입니다."),
    ENROLLMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ENROLLMENT_003", "해당 강의를 수강할 권한이 없습니다."),

    // ── Payment ──────────────────────────────────────────────────────────
    INVALID_PAYMENT_ORDER_STATUS(HttpStatus.CONFLICT,      "PAYMENT_001", "결제할 수 없는 주문 상태입니다."),
    ORDER_ALREADY_PAID(HttpStatus.CONFLICT,      "PAYMENT_002", "이미 결제 완료된 주문입니다."),
    INVALID_PAYMENT_FAILURE_STATUS(HttpStatus.CONFLICT,      "PAYMENT_003", "결제 실패 처리할 수 없는 주문 상태입니다."),
    INVALID_PAYMENT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "PAYMENT_004", "잘못된 결제 상태 전이입니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_005", "결제 요청 금액이 주문 금액과 일치하지 않습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_006", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_REFUND_UNSUPPORTED(HttpStatus.CONFLICT, "PAYMENT_007", "현재 결제수단은 자동 환불을 지원하지 않습니다."),

    // ── Cart ─────────────────────────────────────────────────────────────
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND,        "CART_001", "장바구니 항목을 찾을 수 없습니다."),
    LECTURE_ALREADY_IN_CART(HttpStatus.CONFLICT,         "CART_002", "이미 장바구니에 담긴 강의입니다."),

    // ── LearningEvent ─────────────────────────────────────────────────────
    DUPLICATE_LEARNING_EVENT(HttpStatus.CONFLICT,  "LEARNING_001", "이미 처리된 이벤트입니다."),
    LEARNING_EVENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "LEARNING_002", "학습 이벤트에 접근할 권한이 없습니다."),
    CLIENT_COMPLETE_EVENT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "LEARNING_003", "완료 이벤트는 클라이언트가 직접 기록할 수 없습니다."),

    // ── Media ────────────────────────────────────────────────────────────
    INVALID_VIDEO_FORMAT(HttpStatus.BAD_REQUEST, "MEDIA_001", "올바르지 않은 비디오 파일 형식입니다."),
    VIDEO_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_002", "로컬 디스크에 원본 비디오 파일 업로드를 실패했습니다."),
    VIDEO_ENCODING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_003", "HLS 비디오 스트리밍 인코딩 처리에 실패했습니다."),
    MEDIA_TEMP_DIR_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_004", "임시 디렉터리 생성에 실패했습니다."),
    S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_005", "S3 원격 스토리치로의 파일 업로드에 실패했습니다."),
    S3_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_006", "S3 원격 스토리지로부터의 파일 다운로드에 실패했습니다."),
    S3_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MEDIA_007", "S3 원격 스토리지의 파일 자원 삭제에 실패했습니다."),

    // ── Video Access (영상 재생 권한 제어) ──────────────────────────────────
    VIDEO_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MEDIA_008", "해당 영상의 재생 권한이 없습니다. 수강 여부를 확인해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
