import axios from 'axios'
import { http } from '../../../services/http'
import { errorMessage } from '../../shared/api-error'
import type {
  BodyMetric,
  ProgressResponse,
  TrainerScheduleResponse,
  TrainerStudent,
  TrainerStudentsGateway,
  WorkoutLog,
  WorkoutPlan,
} from '../types/trainer'

/**
 * Ưu tiên hiển thị nguyên văn `detail` từ ProblemDetail của backend cho 400, 403, 404, 409, 422.
 */
export function trainerApiDetailMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { detail?: unknown } | undefined
    if (typeof data?.detail === 'string' && data.detail.trim()) {
      return data.detail
    }
  }
  return errorMessage(error)
}

/**
 * Đọc bản đồ lỗi từng trường `errors` từ phản hồi 400 của backend (VD: `errors.title`, `errors.exercise`).
 */
export function trainerApiFieldErrors(error: unknown): Record<string, string> {
  if (!axios.isAxiosError(error) || error.response?.status !== 400) {
    return {}
  }
  const raw = (error.response.data as { errors?: unknown } | undefined)?.errors
  if (!raw || typeof raw !== 'object') {
    return {}
  }
  const result: Record<string, string> = {}
  for (const [key, value] of Object.entries(raw as Record<string, unknown>)) {
    if (typeof value === 'string' && value.trim()) {
      result[key] = value
    }
  }
  return result
}

export const trainerApi: TrainerStudentsGateway = {
  getSchedule: async (query) =>
    (await http.get<TrainerScheduleResponse>('/pt/schedule', { params: query })).data,

  listStudents: async () => (await http.get<TrainerStudent[]>('/pt/students')).data,

  listWorkoutPlans: async (memberId) =>
    (await http.get<WorkoutPlan[]>(`/pt/members/${memberId}/workout-plans`)).data,

  createWorkoutPlan: async (memberId, input) =>
    (await http.post<WorkoutPlan>(`/pt/members/${memberId}/workout-plans`, input)).data,

  createWorkoutLog: async (memberId, input) =>
    (await http.post<WorkoutLog>(`/pt/members/${memberId}/workout-logs`, input)).data,

  listBodyMetrics: async (memberId) =>
    (await http.get<BodyMetric[]>(`/members/${memberId}/body-metrics`)).data,

  createBodyMetric: async (memberId, input) =>
    (await http.post<BodyMetric>(`/members/${memberId}/body-metrics`, input)).data,

  getProgress: async (memberId, query) =>
    (await http.get<ProgressResponse>(`/members/${memberId}/progress`, { params: query })).data,
}
