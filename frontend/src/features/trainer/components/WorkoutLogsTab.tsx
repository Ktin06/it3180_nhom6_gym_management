import { Alert, Button, Card, Input, Select, Space, Typography } from 'antd'
import { type FormEvent, useState } from 'react'
import { trainerApiDetailMessage, trainerApiFieldErrors } from '../api/trainer-api'
import { formatVnDate, formatVnDateTime, vnDateIso } from '../trainer-time'
import type {
  PtSessionScheduleItem,
  TrainerStudentsGateway,
  WorkoutLog,
  WorkoutLogInput,
} from '../types/trainer'

interface WorkoutLogsTabProps {
  memberId: number
  donePtSessions: PtSessionScheduleItem[]
  api: Pick<TrainerStudentsGateway, 'createWorkoutLog'>
  now: () => Date
}

export function WorkoutLogsTab({ memberId, donePtSessions, api, now }: WorkoutLogsTabProps) {
  const [ptSessionId, setPtSessionId] = useState<number | undefined>()
  const [loggedAt, setLoggedAt] = useState(() => vnDateIso(now()))
  const [exercise, setExercise] = useState('')
  const [sets, setSets] = useState('')
  const [reps, setReps] = useState('')
  const [weight, setWeight] = useState('')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string>()
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [successText, setSuccessText] = useState<string>()
  const [recentLogs, setRecentLogs] = useState<WorkoutLog[]>([])

  function handleSelectSession(value: number | undefined) {
    setPtSessionId(value)
    if (value !== undefined) {
      const matched = donePtSessions.find((s) => s.id === value)
      if (matched) {
        setLoggedAt(vnDateIso(new Date(matched.startAt)))
      }
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setFormError(undefined)
    setFieldErrors({})
    setSuccessText(undefined)

    const payload: WorkoutLogInput = {
      ptSessionId: ptSessionId ?? null,
      loggedAt,
      exercise: exercise.trim(),
      sets: sets.trim() ? Number(sets) : null,
      reps: reps.trim() ? Number(reps) : null,
      weight: weight.trim() ? Number(weight) : null,
    }

    try {
      const created = await api.createWorkoutLog(memberId, payload)
      setRecentLogs((prev) => [created, ...prev])
      setExercise('')
      setSets('')
      setReps('')
      setWeight('')
      setSuccessText('Đã ghi nhận nhật ký tập.')
    } catch (cause) {
      const errors = trainerApiFieldErrors(cause)
      if (Object.keys(errors).length > 0) {
        setFieldErrors(errors)
      } else {
        setFormError(trainerApiDetailMessage(cause))
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <Space orientation="vertical" size={16} style={{ width: '100%' }}>
      <Card size="small" title="Thêm nhật ký tập">
        <form onSubmit={(e) => void handleSubmit(e)} noValidate>
          <Space orientation="vertical" size={12} style={{ width: '100%' }}>
            {successText && (
              <Alert
                type="success"
                showIcon
                closable
                onClose={() => setSuccessText(undefined)}
                title={successText}
              />
            )}
            {formError && <Alert type="error" showIcon title={formError} />}

            {donePtSessions.length > 0 && (
              <div>
                <label
                  htmlFor={`log-pt-session-${memberId}`}
                  style={{ display: 'block', marginBottom: 4 }}
                >
                  Buổi PT đã xác nhận (tùy chọn)
                </label>
                <Select
                  id={`log-pt-session-${memberId}`}
                  allowClear
                  style={{ width: '100%' }}
                  placeholder="Không gắn buổi PT cụ thể"
                  value={ptSessionId}
                  disabled={saving}
                  status={fieldErrors.ptSessionId ? 'error' : undefined}
                  options={donePtSessions.map((session) => ({
                    value: session.id,
                    label: `Buổi #${session.id} · ${formatVnDateTime(session.startAt)}`,
                  }))}
                  onChange={(val: number | undefined) => handleSelectSession(val)}
                />
                {fieldErrors.ptSessionId && (
                  <div className="trainer-field-error">{fieldErrors.ptSessionId}</div>
                )}
              </div>
            )}

            <div className="feature-form-grid">
              <div>
                <label htmlFor={`log-date-${memberId}`} style={{ display: 'block', marginBottom: 4 }}>
                  Ngày tập
                </label>
                <Input
                  id={`log-date-${memberId}`}
                  type="date"
                  value={loggedAt}
                  disabled={saving}
                  status={fieldErrors.loggedAt ? 'error' : undefined}
                  onChange={(e) => setLoggedAt(e.target.value)}
                />
                {fieldErrors.loggedAt && (
                  <div className="trainer-field-error">{fieldErrors.loggedAt}</div>
                )}
              </div>

              <div>
                <label
                  htmlFor={`log-exercise-${memberId}`}
                  style={{ display: 'block', marginBottom: 4 }}
                >
                  Tên bài tập
                </label>
                <Input
                  id={`log-exercise-${memberId}`}
                  value={exercise}
                  maxLength={100}
                  disabled={saving}
                  placeholder="Ví dụ: Squat"
                  status={fieldErrors.exercise ? 'error' : undefined}
                  onChange={(e) => setExercise(e.target.value)}
                />
                {fieldErrors.exercise && (
                  <div className="trainer-field-error">{fieldErrors.exercise}</div>
                )}
              </div>
            </div>

            <div className="feature-form-grid">
              <div>
                <label htmlFor={`log-sets-${memberId}`} style={{ display: 'block', marginBottom: 4 }}>
                  Số hiệp (tùy chọn)
                </label>
                <Input
                  id={`log-sets-${memberId}`}
                  type="number"
                  min={1}
                  max={100}
                  value={sets}
                  disabled={saving}
                  status={fieldErrors.sets ? 'error' : undefined}
                  onChange={(e) => setSets(e.target.value)}
                />
                {fieldErrors.sets && <div className="trainer-field-error">{fieldErrors.sets}</div>}
              </div>

              <div>
                <label htmlFor={`log-reps-${memberId}`} style={{ display: 'block', marginBottom: 4 }}>
                  Số lần (tùy chọn)
                </label>
                <Input
                  id={`log-reps-${memberId}`}
                  type="number"
                  min={1}
                  max={1000}
                  value={reps}
                  disabled={saving}
                  status={fieldErrors.reps ? 'error' : undefined}
                  onChange={(e) => setReps(e.target.value)}
                />
                {fieldErrors.reps && <div className="trainer-field-error">{fieldErrors.reps}</div>}
              </div>
            </div>

            <div>
              <label htmlFor={`log-weight-${memberId}`} style={{ display: 'block', marginBottom: 4 }}>
                Mức tạ (kg, tùy chọn)
              </label>
              <Input
                id={`log-weight-${memberId}`}
                type="number"
                step="0.5"
                min={0}
                max={9999.99}
                value={weight}
                disabled={saving}
                status={fieldErrors.weight ? 'error' : undefined}
                onChange={(e) => setWeight(e.target.value)}
              />
              {fieldErrors.weight && <div className="trainer-field-error">{fieldErrors.weight}</div>}
            </div>

            <div>
              <Button type="primary" htmlType="submit" loading={saving}>
                Lưu nhật ký
              </Button>
            </div>
          </Space>
        </form>
      </Card>

      {recentLogs.length > 0 && (
        <Card size="small" title="Nhật ký vừa ghi">
          <Space orientation="vertical" size={8} style={{ width: '100%' }}>
            {recentLogs.map((item) => (
              <div key={item.id}>
                <Typography.Text strong>{item.exercise}</Typography.Text>{' '}
                <Typography.Text type="secondary">
                  · {formatVnDate(item.loggedAt)}
                  {item.sets !== null ? ` · ${item.sets} hiệp` : ''}
                  {item.reps !== null ? ` × ${item.reps} lần` : ''}
                  {item.weight !== null ? ` · ${item.weight} kg` : ''}
                </Typography.Text>
              </div>
            ))}
          </Space>
        </Card>
      )}
    </Space>
  )
}
