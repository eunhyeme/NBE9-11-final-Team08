export interface Instructor {
  id: string
  name: string
  title: string
  avatarUrl?: string
}

export interface Lecture {
  id: string
  title: string
  duration: string
  progress: number
  completed: boolean
  chapterId?: string
  durationSeconds?: number
  m3u8Path?: string | null
  summary?: string
  hasVideo?: boolean
  lastPositionSeconds?: number
  watchedSeconds?: number
  isFreePreview?: boolean
}

export interface Chapter {
  id: string
  title: string
  lectures: Lecture[]
}

export interface Course {
  id: string
  title: string
  subtitle: string
  description: string
  category: string
  subCategory: string
  thumbnailUrl: string
  price: number
  discountRate?: number
  viewCount: number
  instructor: Instructor
  chapters: Chapter[]
  badges?: string[]
  tags?: string[]
  status?: 'PUBLISHED' | 'DRAFT' | 'REVIEW' | 'ON_SALE' | 'IN_REVIEW' | 'CLOSED' | 'SUSPENDED' | 'DELETED'
  statusReason?: string | null
}

export interface CourseCardResponse {
  id: number
  instructorId: number
  categoryId: number
  title: string
  thumbnail: string
  price: number
  viewCount: number
  status?: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'IN_REVIEW' | 'ON_SALE' | 'SUSPENDED' | 'DELETED'
  statusReason?: string | null
}

export interface CategoryResponse {
  id: number
  name: string
  parentCategoryId: number | null
  depth: number
}

export interface LectureInfoResponse {
  id: number
  title: string
  summary?: string
  m3u8Path: string | null
  durationSeconds: number
  orderNo: number
  isFreePreview: boolean
  hasVideo?: boolean
}

export interface ChapterInfoResponse {
  id: number
  title: string
  orderNo: number
  lectures: LectureInfoResponse[]
}

export interface CourseDetailResponse {
  id: number
  instructorId: number
  categoryId: number
  title: string
  description: string
  thumbnail: string
  price: number
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'IN_REVIEW' | 'ON_SALE' | 'SUSPENDED' | 'DELETED'
  statusReason?: string | null
  viewCount: number
  chapters: ChapterInfoResponse[]
}

export interface PageResponse<T> {
  content: T[]
  pageable: {
    pageNumber: number
    pageSize: number
  }
  totalElements: number
  totalPages: number
  last: boolean
}

export interface CartItemResponse {
  cartItemId: number
  courseId: number
  title: string
  thumbnailUrl?: string | null
  price: number
}

export interface CartResponse {
  items: CartItemResponse[]
  totalPrice: number
}

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'CANCELED'
  | 'REFUNDED'
  | 'EXPIRED'

export type PaymentStatus =
  | 'READY'
  | 'PROCESSING'
  | 'SUCCESS'
  | 'DECLINED'
  | 'UNKNOWN'
  | 'CANCELED'
  | 'REFUNDED'

export interface OrderItemResponse {
  orderItemId: number
  courseId: number
  courseTitle: string
  thumbnailUrl?: string | null
  price: number
  discountPrice: number
  finalPrice: number
}

export interface OrderPaymentResponse {
  paymentId: number
  provider: 'MOCK' | 'TOSS' | 'NICEPAY' | 'KCP' | string
  method?: string | null
  status: PaymentStatus
  paidAt?: string | null
}

export interface OrderDetailResponse {
  orderId: number
  orderNumber: string
  totalPrice: number
  discountPrice: number
  finalPrice: number
  status: OrderStatus
  orderedAt: string
  canceledAt?: string
  payment?: OrderPaymentResponse | null
  items: OrderItemResponse[]
}

export interface OrderSummaryResponse {
  orderId: number
  orderNumber: string
  orderedAt: string
  status: OrderStatus
  totalPrice: number
  finalPrice: number
}

export interface ConfirmPaymentResponse {
  paymentId: number
  orderId: number
  orderNumber: string
  amount: number
  paymentStatus: PaymentStatus
  orderStatus: OrderStatus
  paidAt?: string | null
  enrolledCourseIds?: number[]
}

export interface ConfirmTossPaymentRequest {
  paymentKey: string
  method: string
  amount: number
  idempotencyKey?: string | null
  authResultCode?: string | null
  authResultMsg?: string | null
  authToken?: string | null
  txTid?: string | null
  mid?: string | null
  moid?: string | null
  signature?: string | null
  nextAppUrl?: string | null
  netCancelUrl?: string | null
  payMethod?: string | null
}

export interface NicepayPreparePaymentRequest {
  payMethod: 'CARD'
}

export interface NicepayPreparePaymentResponse {
  goodsName: string
  amt: number
  mid: string
  ediDate: string
  moid: string
  signData: string
  payMethod: 'CARD'
  buyerName: string
  buyerTel: string
  buyerEmail: string
  charSet: string
  reqReserved?: string | null
}

export interface PaymentResponse {
  paymentId: number
  orderId: number
  amount: number
  paymentStatus: PaymentStatus
  orderStatus: OrderStatus
  paidAt?: string | null
  failedReason?: string | null
  canceledAt?: string | null
  refundedAt?: string | null
}

// ── 강의 입장/진행 ──────────────────────────────────────────────
export interface LectureProgressSummary {
  lastPositionSeconds: number
  watchedSeconds: number
  progressRate: number
  completed: boolean
}

export interface LectureEnterResponse {
  lectureId: number
  title: string
  m3u8Path: string | null
  durationSeconds: number
  orderNo: number
  chapterId: number
  progress: LectureProgressSummary | null
}

export interface LectureProgressResponse {
  lectureId: number
  lastPositionSeconds: number
  watchedSeconds: number
  progressRate: number
  completed: boolean
}

// 강좌 커리큘럼 화면용 — 사용자의 강의별 진행도(진행 이력이 있는 강의만 내려온다)
export interface CourseLectureProgressResponse {
  lectureId: number
  lastPositionSeconds: number
  progressRate: number
  completed: boolean
}

export type LearningEventType =
  | 'LECTURE_ENTER'
  | 'VIDEO_PAUSE'
  | 'LECTURE_EXIT'
  | 'LECTURE_COMPLETE'

export interface OrderItem {
  courseId: string
  title: string
  instructor: string
  price: number
  status: OrderStatus
}

export interface Order {
  id: string
  orderNumber: string
  orderedAt: string
  status: OrderStatus
  paymentStatus: PaymentStatus
  paymentMethod: string
  items: OrderItem[]
  productTotal: number
  discount: number
  paidAmount: number
  refundAmount: number
}

export interface UserProfile {
  id: string
  name: string
  email: string
  avatarUrl?: string
  courseCount: number
  isSeller: boolean
  isAdmin: boolean
}

export interface EnrolledCourse {
  id: string
  courseId?: string
  title: string
  instructor: string
  thumbnailUrl: string
  progress: number
  totalLectures: number
  completedLectures: number
  dday?: number
  status: '진행 중' | '완료'
}

export interface EnrolledCourseResponse {
  enrollmentId: number
  courseId: number
  title: string
  instructorNickname: string
  thumbnailUrl: string
  progressRate: number
  completedLectures: number
  totalLectures: number
  enrolledAt: string
}

export interface SignupRequest {
  email: string
  password: string
  nickname: string
  profileImage?: string
}

export interface CourseCreateRequest {
  title: string
  description: string
  categoryId: number
  price: number
  thumbnail: string
}

export interface LectureUpdateRequest {
  id: number | null
  title: string
  summary?: string
  durationSeconds: number
  orderNo: number
  isFreePreview: boolean
}

export interface ChapterUpdateRequest {
  id: number | null
  title: string
  orderNo: number
  lectures: LectureUpdateRequest[]
}

export interface CourseUpdateRequest extends CourseCreateRequest {
  chapters: ChapterUpdateRequest[]
}

export interface LoginRequest {
  email: string
  password: string
}

export type BackendDateTime = string | number[]

export interface LearningEventResponse {
  id: number
  userId: number
  eventType: string
  courseId: number
  chapterId: number | null
  lectureId: number | null
  createdAt: string
}

// ── 관리자 대시보드 ────────────────────────────────────────────────────
export interface AdminOverview {
  totalUsers: number
  sellerCount: number
  regularUserCount: number
  onSaleCourseCount: number
  activeEnrollmentCount: number
  totalLearningEvents: number
  totalCompletions: number
  todaySessions: number
  todayActiveLearners: number
}

export interface DailySessionPoint {
  date: string
  sessions: number
  distinctLearners: number
}

export interface SellerAnalytics {
  totalRevenue: number
  totalOrders: number
  totalStudents: number
  totalCourses: number
  onSaleCourses: number
  revenueDelta: number
  ordersDelta: number
  monthly: { month: string; revenue: number; orders: number }[]
  categories: { name: string; value: number }[]
  topCourses: { courseId: number; title: string; price: number; studentCount: number; revenue: number }[]
  courseBreakdown: {
    courseId: number
    title: string
    status: string
    price: number
    studentCount: number
    orders: number
    revenue: number
  }[]
}

export interface LectureEngagement {
  lectureId: number
  chapterTitle: string
  title: string
  durationSeconds: number
  enterCount: number
  completeCount: number
  viewerCount: number
  avgWatchSeconds: number
}

export interface SellerCourseDetail {
  courseId: number
  title: string
  status: string
  price: number
  totalRevenue: number
  totalOrders: number
  activeStudents: number
  completions: number
  revenueDelta: number
  ordersDelta: number
  monthly: { month: string; revenue: number; orders: number }[]
  lectures: LectureEngagement[]
}

export interface LecturePauses {
  lectureId: number
  title: string
  durationSeconds: number
  binSeconds: number
  totalPauses: number
  viewerCount: number
  bins: { index: number; startSeconds: number; endSeconds: number; count: number; heat: number }[]
  hotspots: { startSeconds: number; endSeconds: number; count: number; heat: number }[]
}

export interface CourseStatRow {
  courseId: number
  title: string
  instructorId: number
  status: string
  enrollees: number
  enterCount: number
  completionCount: number
  fullyCompleted: number
  incompletionRate: number
}

export interface LectureStatRow {
  lectureId: number
  chapterTitle: string
  title: string
  enterCount: number
  completeCount: number
  avgWatchSeconds: number
}

export interface EnrolleeRow {
  userId: number
  nickname: string
  completedLectures: number
  totalLectures: number
  progressRate: number
  lastEventTime: string | null
}

export interface LearningEventRow {
  id: number
  userId: number
  courseId: number | null
  chapterId: number | null
  lectureId: number | null
  eventType: string
  positionSeconds: number | null
  eventTime: string
}

export interface DuplicateBurst {
  userId: number
  lectureId: number
  eventType: string
  bucketMinute: string
  count: number
}

export interface AnomalyResponse {
  incompletionThreshold: number
  burstThreshold: number
  windowMinutes: number
  highIncompletionCourses: CourseStatRow[]
  duplicateBursts: DuplicateBurst[]
}

export interface AuditResponse {
  retention: {
    learningEventCount: number
    oldestEventTime: string | null
    newestEventTime: string | null
    courseStatusHistoryCount: number
    lectureProgressCount: number
  }
  accessHistory: {
    source: string
    description: string
    actorId: number | null
    occurredAt: string | null
  }[]
  integrityErrors: {
    type: string
    description: string
    count: number
    sampleIds: number[]
  }[]
}

