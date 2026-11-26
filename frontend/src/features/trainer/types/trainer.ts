export type PtSessionStatus = 'BOOKED' | 'DONE' | 'CANCELLED' | 'NO_SHOW'

export interface TrainerAvailability {
  id: number
  trainerId: number
  branchId: number
  dayOfWeek: number
  startTime: string
  endTime: string
  createdAt: string
  updatedAt: string
}

export interface TrainerAvailabilityInput {
  branchId?: number
  dayOfWeek: number
  startTime: string
  endTime: string
}

export interface PtSessionScheduleItem {
  id: number
  memberId: number
  memberCode: string
  memberName: string
  membershipId: number
  branchId: number
  startAt: string
  endAt: string
  status: PtSessionStatus
  note: string | null
}

export interface ClassSessionScheduleItem {
  id: number
  classScheduleId: number
  branchId: number
  classTypeId: number
  classTypeName: string
  roomId: number
  roomName: string
  occurrenceDate: string
  startAt: string
  endAt: string
  originalStartAt: string | null
  capacity: number
  status: string
}

export interface TrainerScheduleResponse {
  trainerId: number
  branchId: number
  from: string
  to: string
  availabilities: TrainerAvailability[]
  ptSessions: PtSessionScheduleItem[]
  classSessions: ClassSessionScheduleItem[]
}

export interface TrainerScheduleQuery {
  from?: string
  to?: string
}

export interface PtSessionCompletionResult {
  id: number
  trainerId: number
  memberId: number
  membershipId: number
  branchId: number
  startAt: string
  endAt: string
  status: PtSessionStatus
  confirmedAt: string | null
  commissionAmount: number | null
  remainingSessions: number | null
  membershipStatus: string
  activatedMembershipId: number | null
  movedSessionIds: number[]
  cancelledSessionIds: number[]
}

export interface TrainerStudent {
  memberId: number
  memberCode: string
  fullName: string
  homeBranchId: number
  viaPt: boolean
  viaClass: boolean
}

export interface WorkoutPlanExercise {
  name: string
  sets?: number | null
  reps?: number | null
}

export interface WorkoutPlanSession {
  session: number
  exercises: WorkoutPlanExercise[]
}

export interface WorkoutPlanInput {
  title: string
  content: WorkoutPlanSession[]
  startDate?: string | null
  endDate?: string | null
}

export interface WorkoutPlan {
  id: number
  trainerId: number
  trainerName: string
  memberId: number
  title: string
  content: WorkoutPlanSession[]
  startDate: string | null
  endDate: string | null
  createdAt: string
}

export interface WorkoutLogInput {
  ptSessionId?: number | null
  loggedAt: string
  exercise: string
  sets?: number | null
  reps?: number | null
  weight?: number | null
}

export interface WorkoutLog {
  id: number
  memberId: number
  ptSessionId: number | null
  loggedAt: string
  exercise: string
  sets: number | null
  reps: number | null
  weight: number | null
  createdAt: string
}

export interface BodyMetricInput {
  measuredAt: string
  weight?: number | null
  height?: number | null
  bodyFat?: number | null
  muscleMass?: number | null
  note?: string | null
}

export interface BodyMetric {
  id: number
  memberId: number
  measuredAt: string
  weight: number | null
  height: number | null
  bodyFat: number | null
  muscleMass: number | null
  note: string | null
  recordedBy: number | null
  createdAt: string
}

export interface ProgressQuery {
  from?: string
  to?: string
}

export interface ProgressResponse {
  memberId: number
  memberCode: string
  fullName: string
  from: string
  to: string
  bodyMetrics: BodyMetric[]
  workoutLogs: WorkoutLog[]
}

export interface TrainerScheduleGateway {
  getSchedule(query?: TrainerScheduleQuery): Promise<TrainerScheduleResponse>
  createAvailability(input: TrainerAvailabilityInput): Promise<TrainerAvailability>
  updateAvailability(id: number, input: TrainerAvailabilityInput): Promise<TrainerAvailability>
  deleteAvailability(id: number): Promise<void>
  confirmPtSession(sessionId: number): Promise<PtSessionCompletionResult>
  markPtSessionNoShow(sessionId: number): Promise<PtSessionCompletionResult>
}

export interface TrainerStudentsGateway {
  listStudents(): Promise<TrainerStudent[]>
  listWorkoutPlans(memberId: number): Promise<WorkoutPlan[]>
  createWorkoutPlan(memberId: number, input: WorkoutPlanInput): Promise<WorkoutPlan>
  createWorkoutLog(memberId: number, input: WorkoutLogInput): Promise<WorkoutLog>
  listBodyMetrics(memberId: number): Promise<BodyMetric[]>
  createBodyMetric(memberId: number, input: BodyMetricInput): Promise<BodyMetric>
  getProgress(memberId: number, query?: ProgressQuery): Promise<ProgressResponse>
  getSchedule(query?: TrainerScheduleQuery): Promise<TrainerScheduleResponse>
}
