import { App } from 'antd'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import type {
  BodyMetric,
  ProgressResponse,
  TrainerScheduleResponse,
  TrainerStudent,
  TrainerStudentsGateway,
  WorkoutLog,
  WorkoutPlan,
} from '../types/trainer'
import { TrainerStudentsPage } from './TrainerStudentsPage'

function axiosProblem(status: number, detail: string, errors?: Record<string, string>) {
  const error = new AxiosError('Request failed')
  error.response = {
    status,
    data: errors ? { status, detail, errors } : { status, detail },
    statusText: 'Error',
    headers: {},
    config: { headers: new AxiosHeaders() },
  }
  return error
}

function makeStudent(overrides: Partial<TrainerStudent> = {}): TrainerStudent {
  return {
    memberId: 5,
    memberCode: 'HV000005',
    fullName: 'Nguyễn Văn An',
    homeBranchId: 1,
    viaPt: true,
    viaClass: false,
    ...overrides,
  }
}

function makePlan(overrides: Partial<WorkoutPlan> = {}): WorkoutPlan {
  return {
    id: 1,
    trainerId: 3,
    trainerName: 'Trần Bình',
    memberId: 5,
    title: 'Giáo án tăng cơ 4 tuần',
    content: [
      {
        session: 1,
        exercises: [
          { name: 'Squat', sets: 4, reps: 10 },
          { name: 'Plank', sets: null, reps: null },
        ],
      },
    ],
    startDate: '2026-11-23',
    endDate: '2026-12-20',
    createdAt: '2026-11-21T03:00:00Z',
    ...overrides,
  }
}

function makeLog(overrides: Partial<WorkoutLog> = {}): WorkoutLog {
  return {
    id: 7,
    memberId: 5,
    ptSessionId: 10,
    loggedAt: '2026-11-20',
    exercise: 'Squat',
    sets: 4,
    reps: 10,
    weight: 60.5,
    createdAt: '2026-11-21T03:00:00Z',
    ...overrides,
  }
}

function makeMetric(overrides: Partial<BodyMetric> = {}): BodyMetric {
  return {
    id: 3,
    memberId: 5,
    measuredAt: '2026-11-21',
    weight: 65.5,
    height: 170.0,
    bodyFat: 18.5,
    muscleMass: 30.2,
    note: 'Đo buổi sáng',
    recordedBy: 12,
    createdAt: '2026-11-21T03:00:00Z',
    ...overrides,
  }
}

function makeProgress(overrides: Partial<ProgressResponse> = {}): ProgressResponse {
  return {
    memberId: 5,
    memberCode: 'HV000005',
    fullName: 'Nguyễn Văn An',
    from: '2026-08-24',
    to: '2026-11-21',
    bodyMetrics: [],
    workoutLogs: [],
    ...overrides,
  }
}

function makeSchedule(): TrainerScheduleResponse {
  return {
    trainerId: 3,
    branchId: 1,
    from: '2026-08-24',
    to: '2026-11-21',
    ptSessions: [
      {
        id: 10,
        memberId: 5,
        memberCode: 'HV000005',
        memberName: 'Nguyễn Văn An',
        membershipId: 2,
        branchId: 1,
        startAt: '2026-11-20T07:00:00Z',
        endAt: '2026-11-20T08:00:00Z',
        status: 'DONE',
        note: null,
      },
    ],
  }
}

function makeGateway(overrides: Partial<TrainerStudentsGateway> = {}): TrainerStudentsGateway {
  return {
    listStudents: vi.fn(async () => [makeStudent()]),
    listWorkoutPlans: vi.fn(async () => [makePlan()]),
    createWorkoutPlan: vi.fn(async (_memberId, input) =>
      makePlan({ id: 2, title: input.title, content: input.content }),
    ),
    createWorkoutLog: vi.fn(async () => makeLog()),
    listBodyMetrics: vi.fn(async () => [makeMetric()]),
    createBodyMetric: vi.fn(async () => makeMetric({ id: 4, weight: 64.8 })),
    getProgress: vi.fn(async () => makeProgress()),
    getSchedule: vi.fn(async () => makeSchedule()),
    ...overrides,
  }
}

function renderStudentsPage(
  api: TrainerStudentsGateway,
  now: () => Date = () => new Date('2026-11-21T05:00:00Z'),
) {
  return render(
    <App>
      <TrainerStudentsPage api={api} now={now} />
    </App>,
  )
}

describe('TrainerStudentsPage (Học viên, giáo án, nhật ký, chỉ số, tiến độ – F9, F2)', () => {
  it('TC-F9.4.1-01h: chọn học viên, xem và tạo giáo án, lỗi 400 dưới đúng ô và API 403 hiện đúng câu', async () => {
    const user = userEvent.setup()
    const createPlanMock = vi
      .fn()
      .mockRejectedValueOnce(
        axiosProblem(400, 'Dữ liệu không hợp lệ.', {
          title: 'Vui lòng nhập tên giáo án.',
        }),
      )
      .mockResolvedValueOnce(makePlan({ id: 2, title: 'Giáo án mới' }))
    const api = makeGateway({
      listStudents: vi.fn(async () => [
        makeStudent({ memberId: 5, fullName: 'Nguyễn Văn An', memberCode: 'HV000005' }),
        makeStudent({ memberId: 9, fullName: 'Lê Thị Ngoại Lệ', memberCode: 'HV000009' }),
      ]),
      listWorkoutPlans: vi.fn(async (memberId) => {
        if (memberId === 9) {
          throw axiosProblem(403, 'Bạn chỉ thao tác được với học viên mình dạy.')
        }
        return [makePlan()]
      }),
      createWorkoutPlan: createPlanMock,
    })

    renderStudentsPage(api)

    expect(await screen.findByRole('button', { name: /Nguyễn Văn An/i })).toBeInTheDocument()
    expect(await screen.findByText('Giáo án tăng cơ 4 tuần')).toBeInTheDocument()

    // Tạo giáo án: 400 hiện dưới đúng ô title
    const titleInput = screen.getByLabelText('Tên giáo án')
    await user.type(titleInput, 'A')
    const exerciseInput = screen.getByLabelText('Tên bài tập')
    await user.type(exerciseInput, 'Squat')
    await user.click(screen.getByRole('button', { name: /Lưu giáo án/ }))

    expect(await screen.findByText('Vui lòng nhập tên giáo án.')).toBeInTheDocument()

    // Gửi lại thành công
    await user.clear(titleInput)
    await user.type(titleInput, 'Giáo án mới')
    await user.click(screen.getByRole('button', { name: /Lưu giáo án/ }))

    await waitFor(() => {
      expect(createPlanMock).toHaveBeenCalledTimes(2)
    })

    // Chọn học viên bị 403 -> hiện đúng câu detail của API
    await user.click(screen.getByRole('button', { name: /Lê Thị Ngoại Lệ/i }))
    expect(
      await screen.findByText('Bạn chỉ thao tác được với học viên mình dạy.'),
    ).toBeInTheDocument()
  })

  it('TC-F9.5.1-01c: thêm nhật ký tập, hiển thị lỗi 400 dưới đúng ô và lỗi 422 đúng câu API', async () => {
    const user = userEvent.setup()
    const createLogMock = vi
      .fn()
      .mockRejectedValueOnce(
        axiosProblem(400, 'Dữ liệu không hợp lệ.', {
          exercise: 'Vui lòng nhập tên bài tập.',
        }),
      )
      .mockRejectedValueOnce(axiosProblem(422, 'Ngày tập phải trùng ngày diễn ra buổi PT.'))
      .mockResolvedValueOnce(makeLog())
    const api = makeGateway({ createWorkoutLog: createLogMock })

    renderStudentsPage(api)
    await screen.findByRole('button', { name: /Nguyễn Văn An/i })

    await user.click(screen.getByRole('tab', { name: 'Nhật ký tập' }))
    const logPanel = await screen.findByRole('tabpanel', { name: 'Nhật ký tập' })

    await user.type(within(logPanel).getByLabelText('Tên bài tập'), 'Bench press')
    await user.click(within(logPanel).getByRole('button', { name: /Lưu nhật ký/ }))
    expect(await within(logPanel).findByText('Vui lòng nhập tên bài tập.')).toBeInTheDocument()

    await user.click(within(logPanel).getByRole('button', { name: /Lưu nhật ký/ }))
    expect(
      await within(logPanel).findByText('Ngày tập phải trùng ngày diễn ra buổi PT.'),
    ).toBeInTheDocument()

    await user.click(within(logPanel).getByRole('button', { name: /Lưu nhật ký/ }))
    await waitFor(() => {
      expect(createLogMock).toHaveBeenCalledTimes(3)
    })
  })

  it('TC-F2.5.1-01c: xem lịch sử và thêm chỉ số cơ thể, lỗi 400 dưới đúng ô', async () => {
    const user = userEvent.setup()
    const createMetricMock = vi
      .fn()
      .mockRejectedValueOnce(
        axiosProblem(400, 'Dữ liệu không hợp lệ.', {
          bodyFat: 'Tỷ lệ mỡ từ 0 đến 99,99%.',
        }),
      )
      .mockResolvedValueOnce(makeMetric({ id: 4, weight: 64.8 }))
    const api = makeGateway({ createBodyMetric: createMetricMock })

    renderStudentsPage(api)
    await screen.findByRole('button', { name: /Nguyễn Văn An/i })

    await user.click(screen.getByRole('tab', { name: 'Chỉ số cơ thể' }))
    const panel = await screen.findByRole('tabpanel', { name: 'Chỉ số cơ thể' })

    expect(await within(panel).findByText(/65\.5\s*kg/i)).toBeInTheDocument()
    expect(within(panel).getByText('Đo buổi sáng')).toBeInTheDocument()

    await user.type(within(panel).getByLabelText('Tỷ lệ mỡ (%)'), '100')
    await user.click(within(panel).getByRole('button', { name: /Lưu chỉ số/ }))
    expect(await within(panel).findByText('Tỷ lệ mỡ từ 0 đến 99,99%.')).toBeInTheDocument()

    await user.clear(within(panel).getByLabelText('Tỷ lệ mỡ (%)'))
    await user.type(within(panel).getByLabelText('Cân nặng (kg)'), '64.8')
    await user.click(within(panel).getByRole('button', { name: /Lưu chỉ số/ }))

    await waitFor(() => {
      expect(createMetricMock).toHaveBeenCalledTimes(2)
    })
  })

  it('TC-F9.6.1-04b: biểu đồ tiến độ rỗng hiện chưa có dữ liệu, có dữ liệu vẽ bằng div CSS từ dữ liệu thô và 403 hiện đúng câu', async () => {
    const user = userEvent.setup()

    // Ca 1: Tiến độ rỗng -> hiện câu "Chưa có dữ liệu tiến độ"
    const emptyApi = makeGateway({
      getProgress: vi.fn(async () => makeProgress({ bodyMetrics: [], workoutLogs: [] })),
    })
    const first = renderStudentsPage(emptyApi)
    await screen.findByRole('button', { name: /Nguyễn Văn An/i })
    await user.click(screen.getByRole('tab', { name: 'Tiến độ' }))
    expect(await screen.findByText(/Chưa có dữ liệu tiến độ/i)).toBeInTheDocument()
    first.unmount()

    // Ca 2: Có dữ liệu thô -> vẽ biểu đồ div CSS, không tính trung bình, không gợi ý
    const dataApi = makeGateway({
      getProgress: vi.fn(async () =>
        makeProgress({
          bodyMetrics: [
            makeMetric({ id: 1, measuredAt: '2026-11-10', weight: 68 }),
            makeMetric({ id: 2, measuredAt: '2026-11-20', weight: 66 }),
          ],
          workoutLogs: [
            makeLog({ id: 1, loggedAt: '2026-11-12', exercise: 'Squat', weight: 60 }),
            makeLog({ id: 2, loggedAt: '2026-11-19', exercise: 'Deadlift', weight: 80 }),
          ],
        }),
      ),
    })
    const second = renderStudentsPage(dataApi)
    await screen.findByRole('button', { name: /Nguyễn Văn An/i })
    await user.click(screen.getByRole('tab', { name: 'Tiến độ' }))
    const progressPanel = await screen.findByRole('tabpanel', { name: 'Tiến độ' })
    expect(await within(progressPanel).findByTestId('weight-progress-chart')).toBeInTheDocument()
    expect(within(progressPanel).getByTestId('workout-progress-chart')).toBeInTheDocument()
    expect(within(progressPanel).getAllByText(/68\s*kg/).length).toBeGreaterThanOrEqual(1)
    expect(within(progressPanel).getAllByText(/80\s*kg/).length).toBeGreaterThanOrEqual(1)
    expect(within(progressPanel).queryByText(/trung bình/i)).not.toBeInTheDocument()
    expect(within(progressPanel).queryByText(/gợi ý/i)).not.toBeInTheDocument()
    second.unmount()

    // Ca 3: API tiến độ trả 403 -> hiện đúng câu detail của API
    const forbiddenApi = makeGateway({
      getProgress: vi.fn(async () => {
        throw axiosProblem(403, 'Bạn chỉ thao tác được với học viên mình dạy.')
      }),
    })
    renderStudentsPage(forbiddenApi)
    await screen.findByRole('button', { name: /Nguyễn Văn An/i })
    await user.click(screen.getByRole('tab', { name: 'Tiến độ' }))
    expect(
      await screen.findByText('Bạn chỉ thao tác được với học viên mình dạy.'),
    ).toBeInTheDocument()
  })
  it('TC-F9.5.1-03e: mở trang không truyền now thì chỉ tải lịch buổi PT một lần, không gọi API lặp vô hạn', async () => {
    const getScheduleMock = vi.fn(async () => makeSchedule())
    const api = makeGateway({ getSchedule: getScheduleMock })

    render(
      <App>
        <TrainerStudentsPage api={api} />
      </App>,
    )

    expect(await screen.findByRole('button', { name: /Nguyễn Văn An|HV/i })).toBeInTheDocument()
    await new Promise((resolve) => setTimeout(resolve, 300))
    expect(getScheduleMock).toHaveBeenCalledTimes(1)
  })
  it('TC-F9.4.1-01i: phụ đề nói đúng phạm vi học viên là người có buổi PT với mình, không nhắc lớp nhóm', async () => {
    renderStudentsPage(makeGateway())

    expect(await screen.findByText('Chỉ hiển thị học viên có buổi PT 1-1 với bạn.')).toBeInTheDocument()
    expect(screen.queryByText(/lớp nhóm do bạn dạy/)).not.toBeInTheDocument()
  })
})
