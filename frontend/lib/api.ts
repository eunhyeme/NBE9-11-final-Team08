// frontend/lib/api.ts
import type {
  Course,
  CourseCardResponse,
  CategoryResponse,
  CourseDetailResponse,
  ChapterInfoResponse,
  PageResponse,
  CartResponse,
  CartItemResponse,
  OrderDetailResponse,
  ConfirmPaymentResponse,
  ConfirmTossPaymentRequest,
  NicepayPreparePaymentRequest,
  NicepayPreparePaymentResponse,
  PaymentResponse,
  EnrolledCourse,
  EnrolledCourseResponse,
  UserProfile,
  SignupRequest,
  LoginRequest,
  CourseCreateRequest,
  CourseUpdateRequest,
  LectureEnterResponse,
  LectureProgressResponse,
  CourseLectureProgressResponse,
  LearningEventType,
  LearningEventResponse,
  AdminOverview,
  DailySessionPoint,
  CourseStatRow,
  LectureStatRow,
  EnrolleeRow,
  LearningEventRow,
  AnomalyResponse,
  AuditResponse,
  SellerAnalytics,
  SellerCourseDetail,
  LecturePauses,
} from './types'

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080'
const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'
const REFRESH_EXCLUDED_PATHS = new Set([
  '/api/auth/csrf',
  '/api/auth/login',
  '/api/auth/logout',
  '/api/auth/refresh',
  '/api/auth/signup',
  '/api/auth/seller/signup',
])

let refreshAuthPromise: Promise<boolean> | null = null
let csrfFetchPromise: Promise<string | undefined> | null = null

const decodeCookieValue = (value: string): string => {
  try {
    return decodeURIComponent(value)
  } catch {
    return value
  }
}

const getCookieValue = async (name: string): Promise<string | undefined> => {
  if (typeof window !== 'undefined') {
    const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
    return match ? decodeCookieValue(match[1]) : undefined
  }

  try {
    const { cookies } = await import('next/headers')
    const cookieStore = await cookies()
    const value = cookieStore.get(name)?.value
    return value ? decodeCookieValue(value) : undefined
  } catch {
    return undefined
  }
}

const getAuthHeaders = async (): Promise<Record<string, string>> => {
  if (typeof window !== 'undefined') {
    return {}
  }

  try {
    const { cookies } = await import('next/headers')
    const cookieStore = await cookies()
    const cookieHeader = cookieStore.toString()

    return cookieHeader ? { Cookie: cookieHeader } : {}
  } catch {
    return {}
  }
}

const getCredentialHeaders = async (includeCredentials: boolean): Promise<Record<string, string>> => {
  if (!includeCredentials) {
    return {}
  }

  return getAuthHeaders()
}

const getCredentialsMode = (includeCredentials: boolean): RequestCredentials => {
  return includeCredentials ? 'include' : 'same-origin'
}

const toIsoLocalDateTime = (value: string | number[]): string => {
  if (!Array.isArray(value)) {
    return value
  }

  const [year, month = 1, day = 1, hour = 0, minute = 0, second = 0, nano = 0] = value
  const millisecond = Math.floor(nano / 1_000_000)
  const pad = (num: number, length = 2) => String(num).padStart(length, '0')

  return `${pad(year, 4)}-${pad(month)}-${pad(day)}T${pad(hour)}:${pad(minute)}:${pad(second)}.${pad(millisecond, 3)}`
}

const ensureCsrfToken = async (includeCredentials: boolean): Promise<string | undefined> => {
  if (!includeCredentials || !BASE_URL) {
    return undefined
  }

  const currentToken = await getCookieValue(CSRF_COOKIE_NAME)
  if (currentToken) {
    return currentToken
  }

  if (typeof window === 'undefined') {
    return undefined
  }

  // 쿠키가 없을 때만 발급한다. 콜드스타트에 mutation 이 동시에 나가면
  // 발급이 중복되어 토큰이 매번 갱신되고 쿠키-헤더가 어긋날 수 있으므로,
  // refreshAuthPromise 와 동일하게 in-flight 요청을 하나로 합친다.
  if (!csrfFetchPromise) {
    csrfFetchPromise = (async () => {
      await fetch(`${BASE_URL}/api/auth/csrf`, {
        method: 'GET',
        credentials: 'include',
        cache: 'no-store',
      }).catch(() => undefined)

      return getCookieValue(CSRF_COOKIE_NAME)
    })().finally(() => {
      csrfFetchPromise = null
    })
  }

  return csrfFetchPromise
}

const handleUnauthorized = () => {
  if (typeof window !== 'undefined') {
    const pathname = window.location.pathname
    const isPublicRoute =
      pathname === '/' ||
      pathname.startsWith('/courses') ||
      pathname === '/login' ||
      pathname === '/signup' ||
      pathname === '/seller/signup' ||
      pathname === '/cart'

    if (!isPublicRoute) {
      window.location.href = '/login'
    }
  }
}

const shouldAttemptTokenRefresh = (path: string, includeCredentials: boolean, status: number): boolean => {
  return status === 401
    && includeCredentials
    && typeof window !== 'undefined'
    && !REFRESH_EXCLUDED_PATHS.has(path)
}

const refreshAuthTokens = async (): Promise<boolean> => {
  if (!BASE_URL || typeof window === 'undefined') {
    return false
  }

  if (!refreshAuthPromise) {
    refreshAuthPromise = (async () => {
      const headers: Record<string, string> = {}
      const csrfToken = await ensureCsrfToken(true)
      if (csrfToken) {
        headers[CSRF_HEADER_NAME] = csrfToken
      }

      const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
        method: 'POST',
        headers,
        credentials: 'include',
        cache: 'no-store',
      }).catch(() => null)

      return res?.ok ?? false
    })().finally(() => {
      refreshAuthPromise = null
    })
  }

  return refreshAuthPromise
}

async function request<T>(
  path: string,
  defaultData: T,
  includeAuth = true,
  handleAuthError = true,
): Promise<T> {
  if (!BASE_URL) return defaultData
  try {
    const fetchRequest = async () => {
      // GET 은 백엔드 CsrfFilter 가 검증하지 않으므로 CSRF 토큰을 붙이지 않는다.
      const authHeaders = await getCredentialHeaders(includeAuth)
      return fetch(`${BASE_URL}${path}`, {
        headers: {
          'Content-Type': 'application/json',
          ...authHeaders,
        },
        credentials: getCredentialsMode(includeAuth),
        cache: 'no-store',
      })
    }

    let res = await fetchRequest()
    if (shouldAttemptTokenRefresh(path, includeAuth, res.status) && await refreshAuthTokens()) {
      res = await fetchRequest()
    }

    if (!res.ok) {
      console.warn(`[API 에러] ${res.status} on ${path}`)
      if (res.status === 401 && handleAuthError) {
        handleUnauthorized()
      }
      return defaultData
    }

    return await res.json() as T
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    console.warn(`[API 통신 실패] ${path}: ${message}`)
    return defaultData
  }
}

async function requestText(
  path: string,
  includeAuth = true,
  handleAuthError = true,
): Promise<string> {
  if (!BASE_URL) return ''
  try {
    const fetchRequest = async () => {
      // GET 은 백엔드 CsrfFilter 가 검증하지 않으므로 CSRF 토큰을 붙이지 않는다.
      const authHeaders = await getCredentialHeaders(includeAuth)
      return fetch(`${BASE_URL}${path}`, {
        headers: {
          ...authHeaders,
        },
        credentials: getCredentialsMode(includeAuth),
        cache: 'no-store',
      })
    }

    let res = await fetchRequest()
    if (shouldAttemptTokenRefresh(path, includeAuth, res.status) && await refreshAuthTokens()) {
      res = await fetchRequest()
    }

    if (!res.ok) {
      console.warn(`[API 에러] ${res.status} on ${path}`)
      if (res.status === 401 && handleAuthError) {
        handleUnauthorized()
      }
      return ''
    }

    return await res.text()
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    console.warn(`[API 통신 실패] ${path}: ${message}`)
    return ''
  }
}

async function mutate<T>(
  path: string,
  method: string,
  body?: any,
  isMultipart = false,
  includeCredentials = true,
): Promise<T> {
  if (!BASE_URL) throw new Error('API BASE_URL is not defined')

  const headers = await getCredentialHeaders(includeCredentials)
  if (!isMultipart) {
    headers['Content-Type'] = 'application/json'
  }
  const csrfToken = await ensureCsrfToken(includeCredentials)
  if (csrfToken) {
    headers[CSRF_HEADER_NAME] = csrfToken
  }

  const options: RequestInit = {
    method,
    headers,
  }
  options.credentials = getCredentialsMode(includeCredentials)
  if (body) {
    options.body = isMultipart ? body : JSON.stringify(body)
  }

  let res: Response
  try {
    res = await fetch(`${BASE_URL}${path}`, options)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    console.warn(`[API 통신 실패] ${path}: ${message}`)
    throw new Error('API 서버에 연결할 수 없습니다. 백엔드 서버가 실행 중인지 확인해주세요.')
  }

  if (!res.ok) {
    if (shouldAttemptTokenRefresh(path, includeCredentials, res.status) && await refreshAuthTokens()) {
      res = await fetch(`${BASE_URL}${path}`, options)
    }

    if (!res.ok) {
      if (res.status === 401) {
        handleUnauthorized()
      }
      const errorData = await res.json().catch(() => ({}))
      throw new Error(errorData.message || errorData.detail || errorData.error || `Error ${res.status}`)
    }
  }
  if (res.status === 204 || res.status === 202) {
    return {} as T
  }
  if (res.status === 201 && res.headers.get('content-length') === '0') {
    return {} as T
  }
  const text = await res.text()
  if (!text.trim()) {
    return {} as T
  }
  return JSON.parse(text) as T
}

const mapCourseCardToCourse = (card: CourseCardResponse): Course => ({
  id: card.id?.toString() || '0',
  title: card.title || '',
  subtitle: '',
  description: '',
  category: card.categoryId?.toString() || '0',
  subCategory: '',
  thumbnailUrl: card.thumbnail || '/placeholder.svg',
  price: card.price || 0,
  viewCount: card.viewCount || 0,
  instructor: { id: card.instructorId?.toString() || '0', name: `강사 ${card.instructorId || ''}`, title: '' },
  chapters: [],
  status: card.status,
  statusReason: card.statusReason ?? null,
})

const mapCourseDetailToCourse = (detail: CourseDetailResponse): Course => ({
  id: detail.id?.toString() || '0',
  title: detail.title || '',
  subtitle: '',
  description: detail.description || '',
  category: detail.categoryId?.toString() || '0',
  subCategory: '',
  thumbnailUrl: detail.thumbnail || '/placeholder.svg',
  price: detail.price || 0,
  viewCount: detail.viewCount || 0,
  instructor: { id: detail.instructorId?.toString() || '0', name: `강사 ${detail.instructorId || ''}`, title: '' },
  chapters: detail.chapters?.map((ch) => ({
    id: ch.id.toString(),
    title: ch.title,
    lectures: ch.lectures?.map((lec) => ({
      id: lec.id.toString(),
      title: lec.title,
      duration: `${Math.floor(lec.durationSeconds / 60)}:${(lec.durationSeconds % 60).toString().padStart(2, '0')}`,
      progress: 0,
      completed: false,
      chapterId: ch.id.toString(),
      durationSeconds: lec.durationSeconds,
      m3u8Path: lec.m3u8Path ?? null,
      summary: lec.summary ?? '',
      hasVideo: lec.hasVideo ?? false,
      isFreePreview: lec.isFreePreview ?? false,
    })) || [],
  })) || [],
  status: detail.status,
  statusReason: detail.statusReason ?? null,
})

const mapEnrolledCourseResponseToCourse = (course: EnrolledCourseResponse): EnrolledCourse => ({
  id: String(course.courseId),
  courseId: String(course.courseId),
  title: course.title,
  instructor: course.instructorNickname,
  thumbnailUrl: course.thumbnailUrl || '/placeholder.svg',
  progress: course.progressRate ?? 0,
  totalLectures: course.totalLectures ?? 0,
  completedLectures: course.completedLectures ?? 0,
  status: course.progressRate >= 100 ? '완료' : '진행 중',
})

const SEEDED_COURSES: Record<number, { title: string; description: string; price: number; categoryId: number; instructorId: number; thumbnail: string; status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' }> = {
  1: {
    title: '강의 1 - 프론트엔드',
    description: '강의 1에 대한 설명입니다.',
    price: 20000,
    categoryId: 5,
    instructorId: 2,
    thumbnail: 'thumb1.jpg',
    status: 'PUBLISHED',
  },
  2: {
    title: '강의 2 - DevOps',
    description: '강의 2에 대한 설명입니다.',
    price: 30000,
    categoryId: 6,
    instructorId: 2,
    thumbnail: 'thumb2.jpg',
    status: 'PUBLISHED',
  },
  3: {
    title: '강의 3 - UI/UX',
    description: '강의 3에 대한 설명입니다.',
    price: 40000,
    categoryId: 7,
    instructorId: 3,
    thumbnail: 'thumb3.jpg',
    status: 'DRAFT',
  },
  4: {
    title: '강의 4 - 마케팅',
    description: '강의 4에 대한 설명입니다.',
    price: 50000,
    categoryId: 8,
    instructorId: 3,
    thumbnail: 'thumb4.jpg',
    status: 'PUBLISHED',
  },
  5: {
    title: '1234',
    description: '1231412412',
    price: 123124,
    categoryId: 1,
    instructorId: 8,
    thumbnail: 'https://via.placeholder.com/800x450',
    status: 'DRAFT',
  }
}

const getCourseDraftFromCookies = async (id: string | number): Promise<any | null> => {
  const cookieName = `course_draft_${id}`
  if (typeof window !== 'undefined') {
    const match = document.cookie.match(new RegExp(`(^| )${cookieName}=([^;]+)`))
    if (match) {
      try {
        return JSON.parse(decodeURIComponent(match[2]))
      } catch (e) { }
    }
    try {
      const local = localStorage.getItem(cookieName)
      if (local) return JSON.parse(local)
    } catch (e) { }
  } else {
    try {
      const { cookies } = await import('next/headers')
      const cookieStore = cookies()
      const store = cookieStore instanceof Promise ? await cookieStore : cookieStore
      const val = store.get(cookieName)?.value
      if (val) {
        return JSON.parse(decodeURIComponent(val))
      }
    } catch (e) { }
  }
  return null
}

const saveCourseDraft = (id: string | number, data: any) => {
  if (typeof window !== 'undefined') {
    const cookieName = `course_draft_${id}`
    const jsonStr = JSON.stringify(data)
    document.cookie = `${cookieName}=${encodeURIComponent(jsonStr)}; path=/; max-age=${7 * 24 * 60 * 60}`
    try {
      localStorage.setItem(cookieName, jsonStr)
      const draftIds = JSON.parse(localStorage.getItem('course_draft_ids') || '[]')
      const nextDraftIds = Array.from(new Set([...draftIds, String(id)]))
      localStorage.setItem('course_draft_ids', JSON.stringify(nextDraftIds))
    } catch (e) { }
  }
}

const removeCourseDraft = (id: string | number) => {
  if (typeof window !== 'undefined') {
    const cookieName = `course_draft_${id}`
    document.cookie = `${cookieName}=; path=/; max-age=0`
    try {
      localStorage.removeItem(cookieName)
      const draftIds = JSON.parse(localStorage.getItem('course_draft_ids') || '[]')
      const nextDraftIds = draftIds.filter((dId: string) => dId !== String(id))
      localStorage.setItem('course_draft_ids', JSON.stringify(nextDraftIds))
    } catch (e) { }
  }
}

const getCurrentUserId = async (): Promise<number> => {
  try {
    const profile = await api.getProfile()
    return Number(profile?.id || 1)
  } catch (e) {
    return 1
  }
}

export const api = {
  // Auth
  signup: (data: SignupRequest) => mutate<void>('/api/auth/signup', 'POST', data, false, true),
  sellerSignup: (data: SignupRequest) => mutate<void>('/api/auth/seller/signup', 'POST', data, false, true),
  login: (data: LoginRequest) => mutate<void>('/api/auth/login', 'POST', data, false, true),
  logout: async () => {
    return mutate<void>('/api/auth/logout', 'POST', undefined, false, true)
  },

  // Courses
  getCourses: async (page = 0, size = 10, sort = 'VIEW_DESC'): Promise<PageResponse<Course>> => {
    const res = await request<PageResponse<CourseCardResponse>>(
      `/api/courses?page=${page}&size=${size}&sort=${sort}`,
      { content: [], pageable: { pageNumber: page, pageSize: size }, totalElements: 0, totalPages: 0, last: true },
      false,
      false,
    )
    return {
      ...res,
      content: res.content ? res.content.map(c => mapCourseCardToCourse(c)) : []
    }
  },

  getAdminCourses: async (status?: string, page = 0, size = 20): Promise<PageResponse<Course>> => {
    const statusQuery = status && status !== 'ALL' ? `&status=${status}` : ''
    const res = await request<PageResponse<CourseCardResponse>>(
      `/api/admin/courses?page=${page}&size=${size}${statusQuery}`,
      { content: [], pageable: { pageNumber: page, pageSize: size }, totalElements: 0, totalPages: 0, last: true },
      true,
      true,
    )
    return {
      ...res,
      content: res.content ? res.content.map(c => mapCourseCardToCourse(c)) : []
    }
  },

  // Categories
  getCategories: () => request<CategoryResponse[]>('/api/categories', [], false, false),

  getCourse: async (id: string | number): Promise<Course | undefined> => {
    const detail = await request<CourseDetailResponse | undefined>(
      `/api/courses/${id}`,
      undefined,
      false,
      false,
    )
    if (detail && detail.id) return mapCourseDetailToCourse(detail)
    return undefined
  },

  getInstructorCourses: async (status?: string, page = 0, size = 100): Promise<PageResponse<Course>> => {
    const statusQuery = status && status !== 'ALL' ? `&status=${status}` : ''
    const res = await request<PageResponse<CourseCardResponse>>(
      `/api/courses/instructor?page=${page}&size=${size}${statusQuery}`,
      { content: [], pageable: { pageNumber: page, pageSize: size }, totalElements: 0, totalPages: 0, last: true },
      true,
      true,
    )
    return {
      ...res,
      content: res.content ? res.content.map(c => mapCourseCardToCourse(c)) : []
    }
  },

  getAdminCourse: async (id: string | number): Promise<Course | undefined> => {
    const detail = await request<CourseDetailResponse | undefined>(
      `/api/admin/courses/${id}`,
      undefined,
      true,
      true,
    )
    if (detail && detail.id) return mapCourseDetailToCourse(detail)
    return undefined
  },

  getInstructorCourse: async (id: string | number): Promise<Course | undefined> => {
    const detail = await request<CourseDetailResponse | undefined>(
      `/api/courses/instructor/${id}`,
      undefined,
      true,
      true,
    )
    if (detail && detail.id) return mapCourseDetailToCourse(detail)
    return undefined
  },

  createCourse: async (formData: FormData) => {
    return mutate<number>('/api/courses', 'POST', formData, true)
  },

  updateCourse: async (courseId: string | number, formData: FormData) => {
    return mutate<void>(`/api/courses/${courseId}`, 'PUT', formData, true)
  },

  reorderChapters: async (courseId: string | number, reorders: { chapterId: number; orderNo: number }[]) => {
    return mutate<void>(`/api/curriculums/courses/${courseId}/chapters/reorder`, 'PUT', { reorders }, false, true)
  },

  reorderLectures: async (chapterId: string | number, reorders: { lectureId: number; orderNo: number }[]) => {
    return mutate<void>(`/api/curriculums/chapters/${chapterId}/lectures/reorder`, 'PUT', { reorders }, false, true)
  },

  requestCourseReview: async (courseId: string | number) => {
    const res = await mutate<void>(`/api/courses/${courseId}/reviews`, 'POST')
    const draft = await getCourseDraftFromCookies(courseId)
    if (draft) {
      saveCourseDraft(courseId, { ...draft, status: 'IN_REVIEW' })
    }
    return res
  },

  cancelCourseReview: async (courseId: string | number) => {
    const res = await mutate<void>(`/api/courses/${courseId}/reviews`, 'DELETE')
    const draft = await getCourseDraftFromCookies(courseId)
    if (draft) {
      saveCourseDraft(courseId, { ...draft, status: 'DRAFT' })
    }
    return res
  },

  closeCourse: (courseId: string | number) =>
    mutate<void>(`/api/courses/${courseId}/closing`, 'POST'),

  deleteCourse: async (courseId: string | number) => {
    const res = await mutate<void>(`/api/courses/${courseId}`, 'DELETE')
    removeCourseDraft(courseId)
    return res
  },

  uploadLectureVideo: (lectureId: string | number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return mutate<void>(`/api/courses/lectures/${lectureId}/videos`, 'POST', formData, true)
  },

  // Lecture enter / progress / last-watched
  enterLecture: (
    courseId: string | number,
    chapterId: string | number,
    lectureId: string | number,
  ) =>
    request<LectureEnterResponse | null>(
      `/api/courses/${courseId}/chapters/${chapterId}/lectures/${lectureId}/enter`,
      null,
    ),

  // New API to fetch lecture details directly (including m3u8Path)
  getLecture: (
    courseId: string | number,
    chapterId: string | number,
    lectureId: string | number,
  ) =>
    request<any>(
      `/api/courses/${courseId}/chapters/${chapterId}/lectures/${lectureId}`,
      null,
    ),

  getVideoStreamUrl: (
    courseId: string | number,
    chapterId: string | number,
    lectureId: string | number,
  ) =>
    requestText(
      `/api/courses/${courseId}/chapters/${chapterId}/lectures/${lectureId}/stream`,
      true,
    ),

  getLastWatched: (courseId: string | number) =>
    request<LectureEnterResponse | null>(`/api/courses/${courseId}/lectures/last-watched`, null),

  // 강좌 내 사용자별 강의 진행도 목록 (진행 이력이 있는 강의만 내려옴)
  getCourseLectureProgress: (courseId: string | number) =>
    request<CourseLectureProgressResponse[]>(
      `/api/courses/${courseId}/lectures/progress`,
      [],
      true,
      false,
    ),

  // 강좌 상세 + 사용자별 강의 진행도를 머지해서 반환 (커리큘럼 화면용)
  getCourseWithProgress: async (
    courseId: string | number,
  ): Promise<Course | undefined> => {
    const [course, progressList] = await Promise.all([
      api.getCourse(courseId),
      api.getCourseLectureProgress(courseId),
    ])
    if (!course) return undefined

    const byLectureId = new Map(
      (Array.isArray(progressList) ? progressList : []).map((p) => [
        p.lectureId.toString(),
        p,
      ]),
    )

    return {
      ...course,
      chapters: course.chapters.map((ch) => ({
        ...ch,
        lectures: ch.lectures.map((lec) => {
          const p = byLectureId.get(lec.id)
          if (!p) return lec
          return {
            ...lec,
            progress: p.progressRate,
            completed: p.completed,
            lastPositionSeconds: p.lastPositionSeconds,
          }
        }),
      })),
    }
  },

  enterFirstLecture: (courseId: string | number, chapterId: string | number) =>
    request<LectureEnterResponse | null>(
      `/api/courses/${courseId}/chapters/${chapterId}/lectures/first`,
      null,
    ),

  updateLectureProgress: (
    lectureId: string | number,
    positionSeconds: number,
    watchedDeltaSeconds: number,
  ) =>
    mutate<LectureProgressResponse>(`/api/lectures/${lectureId}/progress`, 'PATCH', {
      positionSeconds,
      watchedDeltaSeconds,
    }),

  // Cart & Order
  getCart: () => request<CartResponse>('/api/cart', { items: [], totalPrice: 0 }, true, false),
  addToCart: (courseId: number) => mutate<CartResponse>('/api/cart/items', 'POST', { courseId }),
  removeFromCart: (cartItemId: number) => mutate<CartResponse>(`/api/cart/items/${cartItemId}`, 'DELETE'),
  clearCart: () => mutate<CartResponse>('/api/cart', 'DELETE'),

  isCourseEnrollmentActive: async (courseId: string | number): Promise<boolean> => {
    const res = await request<boolean | { exists?: boolean }>(
      `/api/enrollments/courses/${courseId}/active`,
      false,
      true,
      false,
    )
    return typeof res === 'boolean' ? res : !!res?.exists
  },

  createOrderFromCart: () => mutate<OrderDetailResponse>('/api/orders/cart', 'POST'),
  createDirectOrder: (courseId: number) => mutate<OrderDetailResponse>('/api/orders/direct', 'POST', { courseId }),
  cancelOrder: (orderId: number | string) => mutate<OrderDetailResponse>(`/api/orders/${orderId}/cancel`, 'PATCH'),
  confirmMockPayment: (
    orderId: number,
    paymentKey: string,
    method: string,
    amount: number,
    idempotencyKey?: string | null,
  ) =>
    mutate<ConfirmPaymentResponse>(`/api/payments/${orderId}/confirm`, 'POST', {
      paymentKey,
      method,
      amount,
      idempotencyKey,
    }),
  confirmTossPayment: (orderId: number, request: ConfirmTossPaymentRequest) =>
    mutate<ConfirmPaymentResponse>(`/api/payments/${orderId}/toss/confirm`, 'POST', request),
  confirmProviderPayment: (orderId: number, providerType: string, request: ConfirmTossPaymentRequest) =>
    mutate<ConfirmPaymentResponse>(`/api/payments/${orderId}/providers/${providerType}/confirm`, 'POST', request),
  prepareNicepayPayment: (orderId: number, request: NicepayPreparePaymentRequest) =>
    mutate<NicepayPreparePaymentResponse>(`/api/payments/${orderId}/nicepay/prepare`, 'POST', request),
  refundPayment: (orderId: number | string) =>
    mutate<PaymentResponse>(`/api/payments/${orderId}/refund`, 'POST'),
  confirmPayment: (
    orderId: number,
    paymentKey: string,
    method: string,
    amount: number,
    idempotencyKey?: string | null,
  ) =>
    api.confirmMockPayment(orderId, paymentKey, method, amount, idempotencyKey),

  // Learning events
  recordLearningEvent: (data: {
    eventType: LearningEventType
    lectureId: number
    courseId?: number
    chapterId?: number
    positionSeconds?: number
    eventKey?: string
  }) =>
    mutate<LearningEventResponse>('/api/learning-events', 'POST', {
      ...data,
      // 이벤트 발생 시각은 서버가 수신 시각으로 직접 찍는다(클라이언트는 시각을 보내지 않음).
      // 멱등 처리를 위한 클라이언트 고유 키 (중복 이벤트 방지)
      eventKey: data.eventKey ?? (typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `${data.lectureId}-${data.eventType}-${Date.now()}`),
    }),

  getMyLearningActivities: async (page = 0, size = 20): Promise<PageResponse<LearningEventResponse>> => {
    const empty: PageResponse<LearningEventResponse> = {
      content: [], pageable: { pageNumber: page, pageSize: size }, totalElements: 0, totalPages: 0, last: true,
    }
    const userId = await getCurrentUserId()
    return request<PageResponse<LearningEventResponse>>(
      `/api/learning-events/users/${userId}/activities?page=${page}&size=${size}`,
      empty,
    )
  },

  // User Profile
  getProfile: async (): Promise<UserProfile | null> => {
    try {
      const claims = await request<any>('/api/auth/me', null)
      if (!claims) return null
      return {
        id: String(claims.id || claims.sub || ''),
        name: claims.nickname || claims.name || '',
        email: claims.email || '',
        courseCount: 0,
        isSeller: claims.role === 'ROLE_SELLER' || claims.role === 'ROLE_ADMIN',
        isAdmin: claims.role === 'ROLE_ADMIN',
      }
    } catch (e) {
      console.error('Failed to decode token:', e)
      return null
    }
  },
  getPurchasedCourses: async (): Promise<EnrolledCourse[]> => {
    const courses = await request<EnrolledCourseResponse[]>('/api/enrollments/me/courses', [])
    return Array.isArray(courses) ? courses.map(mapEnrolledCourseResponseToCourse) : []
  },
  // Orders
  getOrders: async () => {
    const res = await request<any>('/api/orders', { content: [] })
    return Array.isArray(res) ? res : (res?.content || [])
  },

  getOrder: async (id: string) => request<OrderDetailResponse | undefined>(`/api/orders/${id}`, undefined),

  // Admin Course APIs
  approveCourseByAdmin: async (courseId: number | string) => {
    const res = await mutate<void>(`/api/admin/courses/${courseId}/approve`, 'POST')
    removeCourseDraft(courseId)
    return res
  },
  rejectCourseByAdmin: async (courseId: number | string, reason: string) => {
    const res = await mutate<void>(`/api/admin/courses/${courseId}/reject`, 'POST', { reason })
    const draft = await getCourseDraftFromCookies(courseId)
    if (draft) {
      saveCourseDraft(courseId, { ...draft, status: 'DRAFT' })
    }
    return res
  },
  suspendCourseByAdmin: async (courseId: number | string, reason: string) => {
    const res = await mutate<void>(`/api/admin/courses/${courseId}/suspension`, 'POST', { reason })
    const draft = await getCourseDraftFromCookies(courseId)
    if (draft) {
      saveCourseDraft(courseId, { ...draft, status: 'SUSPENDED' })
    }
    return res
  },
  deleteCourseByAdmin: async (courseId: number | string) => {
    const res = await mutate<void>(`/api/admin/courses/${courseId}`, 'DELETE')
    removeCourseDraft(courseId)
    return res
  },

  // ── Admin Dashboard APIs ────────────────────────────────────────────
  getAdminOverview: () =>
    request<AdminOverview | null>('/api/admin/dashboard/overview', null),
  getAdminDailySessions: (from?: string, to?: string) => {
    const qs = new URLSearchParams()
    if (from) qs.set('from', from)
    if (to) qs.set('to', to)
    const suffix = qs.toString() ? `?${qs.toString()}` : ''
    return request<DailySessionPoint[]>(`/api/admin/dashboard/sessions/daily${suffix}`, [])
  },
  getAdminCourseStats: (page = 0, size = 20, status?: string) => {
    const qs = new URLSearchParams({ page: String(page), size: String(size) })
    if (status) qs.set('status', status)
    return request<PageResponse<CourseStatRow> | null>(
      `/api/admin/dashboard/courses?${qs.toString()}`,
      null,
    )
  },
  getAdminLectureStats: (courseId: number) =>
    request<LectureStatRow[]>(`/api/admin/dashboard/courses/${courseId}/lectures`, []),
  getAdminEnrollees: (courseId: number, page = 0, size = 20) =>
    request<PageResponse<EnrolleeRow> | null>(
      `/api/admin/dashboard/courses/${courseId}/enrollees?page=${page}&size=${size}`,
      null,
    ),
  getAdminUserTimeline: (userId: number, courseId?: number, page = 0, size = 30) => {
    const qs = new URLSearchParams({ page: String(page), size: String(size) })
    if (courseId != null) qs.set('courseId', String(courseId))
    return request<PageResponse<LearningEventRow> | null>(
      `/api/admin/dashboard/users/${userId}/timeline?${qs.toString()}`,
      null,
    )
  },
  getAdminAnomalies: (incompletionThreshold?: number, burstThreshold?: number, windowMinutes?: number) => {
    const qs = new URLSearchParams()
    if (incompletionThreshold != null) qs.set('incompletionThreshold', String(incompletionThreshold))
    if (burstThreshold != null) qs.set('burstThreshold', String(burstThreshold))
    if (windowMinutes != null) qs.set('windowMinutes', String(windowMinutes))
    const suffix = qs.toString() ? `?${qs.toString()}` : ''
    return request<AnomalyResponse | null>(`/api/admin/dashboard/anomalies${suffix}`, null)
  },
  getAdminAudit: () =>
    request<AuditResponse | null>('/api/admin/dashboard/audit/retention', null),

  // ── Seller Dashboard APIs ───────────────────────────────────────────
  getSellerAnalytics: (range: '3m' | '6m' | '1y' = '6m') =>
    request<SellerAnalytics | null>(`/api/seller/dashboard/analytics?range=${range}`, null),
  getSellerCourseDetail: (courseId: number, range: '3m' | '6m' | '1y' = '6m') =>
    request<SellerCourseDetail | null>(
      `/api/seller/dashboard/courses/${courseId}?range=${range}`,
      null,
    ),
  getSellerLecturePauses: (lectureId: number, bins = 40) =>
    request<LecturePauses | null>(
      `/api/seller/dashboard/lectures/${lectureId}/pauses?bins=${bins}`,
      null,
    ),
}
