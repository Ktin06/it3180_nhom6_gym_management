import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Input, Skeleton, Space, Tag, Typography } from 'antd'
import { type FormEvent, useCallback, useState } from 'react'
import { useRemoteQuery } from '../../../services/use-remote-query'
import { trainerApiDetailMessage, trainerApiFieldErrors } from '../api/trainer-api'
import { formatVnDate, formatVnDateTime } from '../trainer-time'
import type { TrainerStudentsGateway, WorkoutPlanInput } from '../types/trainer'

interface ExerciseDraft {
  name: string
  sets: string
  reps: string
}

interface SessionDraft {
  session: string
  exercises: ExerciseDraft[]
}

function emptyDraft(): SessionDraft[] {
  return [{ session: '1', exercises: [{ name: '', sets: '', reps: '' }] }]
}

interface WorkoutPlansTabProps {
  memberId: number
  api: Pick<TrainerStudentsGateway, 'listWorkoutPlans' | 'createWorkoutPlan'>
}

export function WorkoutPlansTab({ memberId, api }: WorkoutPlansTabProps) {
  const loadPlans = useCallback((id: number) => api.listWorkoutPlans(id), [api])
  const plansQuery = useRemoteQuery(loadPlans, memberId)

  const [title, setTitle] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [sessions, setSessions] = useState<SessionDraft[]>(emptyDraft)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string>()
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [successText, setSuccessText] = useState<string>()

  function updateSessionNumber(sessionIdx: number, value: string) {
    setSessions((prev) =>
      prev.map((item, idx) => (idx === sessionIdx ? { ...item, session: value } : item)),
    )
  }

  function updateExercise(
    sessionIdx: number,
    exIdx: number,
    key: keyof ExerciseDraft,
    value: string,
  ) {
    setSessions((prev) =>
      prev.map((item, idx) =>
        idx === sessionIdx
          ? {
              ...item,
              exercises: item.exercises.map((ex, eIdx) =>
                eIdx === exIdx ? { ...ex, [key]: value } : ex,
              ),
            }
          : item,
      ),
    )
  }

  function addExercise(sessionIdx: number) {
    setSessions((prev) =>
      prev.map((item, idx) =>
        idx === sessionIdx
          ? { ...item, exercises: [...item.exercises, { name: '', sets: '', reps: '' }] }
          : item,
      ),
    )
  }

  function removeExercise(sessionIdx: number, exIdx: number) {
    setSessions((prev) =>
      prev.map((item, idx) =>
        idx === sessionIdx && item.exercises.length > 1
          ? { ...item, exercises: item.exercises.filter((_, eIdx) => eIdx !== exIdx) }
          : item,
      ),
    )
  }

  function addSession() {
    setSessions((prev) => [
      ...prev,
      { session: String(prev.length + 1), exercises: [{ name: '', sets: '', reps: '' }] },
    ])
  }

  function removeSession(sessionIdx: number) {
    setSessions((prev) => (prev.length > 1 ? prev.filter((_, idx) => idx !== sessionIdx) : prev))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setFormError(undefined)
    setFieldErrors({})
    setSuccessText(undefined)

    const payload: WorkoutPlanInput = {
      title: title.trim(),
      startDate: startDate || null,
      endDate: endDate || null,
      content: sessions.map((s) => ({
        session: Number(s.session),
        exercises: s.exercises.map((ex) => ({
          name: ex.name.trim(),
          sets: ex.sets.trim() ? Number(ex.sets) : null,
          reps: ex.reps.trim() ? Number(ex.reps) : null,
        })),
      })),
    }

    try {
      await api.createWorkoutPlan(memberId, payload)
      setTitle('')
      setStartDate('')
      setEndDate('')
      setSessions(emptyDraft())
      setSuccessText('Đã lưu giáo án mới cho học viên.')
      plansQuery.reload()
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
      <Card size="small" title="Soạn giáo án mới">
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

            <div>
              <label htmlFor={`plan-title-${memberId}`} style={{ display: 'block', marginBottom: 4 }}>
                Tên giáo án
              </label>
              <Input
                id={`plan-title-${memberId}`}
                value={title}
                maxLength={255}
                disabled={saving}
                placeholder="Ví dụ: Giáo án tăng cơ 4 tuần"
                status={fieldErrors.title ? 'error' : undefined}
                onChange={(e) => setTitle(e.target.value)}
              />
              {fieldErrors.title && <div className="trainer-field-error">{fieldErrors.title}</div>}
            </div>

            <div className="feature-form-grid">
              <div>
                <label htmlFor={`plan-start-${memberId}`} style={{ display: 'block', marginBottom: 4 }}>
                  Từ ngày (tùy chọn)
                </label>
                <Input
                  id={`plan-start-${memberId}`}
                  type="date"
                  value={startDate}
                  disabled={saving}
                  status={fieldErrors.startDate ? 'error' : undefined}
                  onChange={(e) => setStartDate(e.target.value)}
                />
                {fieldErrors.startDate && (
                  <div className="trainer-field-error">{fieldErrors.startDate}</div>
                )}
              </div>

              <div>
                <label htmlFor={`plan-end-${memberId}`} style={{ display: 'block', marginBottom: 4 }}>
                  Đến ngày (tùy chọn)
                </label>
                <Input
                  id={`plan-end-${memberId}`}
                  type="date"
                  value={endDate}
                  disabled={saving}
                  status={fieldErrors.endDate ? 'error' : undefined}
                  onChange={(e) => setEndDate(e.target.value)}
                />
                {fieldErrors.endDate && (
                  <div className="trainer-field-error">{fieldErrors.endDate}</div>
                )}
              </div>
            </div>

            <div>
              <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 8 }}>
                <Typography.Text strong>Nội dung theo buổi</Typography.Text>
                <Button size="small" icon={<PlusOutlined />} disabled={saving} onClick={addSession}>
                  Thêm buổi
                </Button>
              </Space>

              {fieldErrors.content && (
                <div className="trainer-field-error" style={{ marginBottom: 8 }}>
                  {fieldErrors.content}
                </div>
              )}

              {sessions.map((sessionItem, sIdx) => {
                const sessionErr = fieldErrors[`content[${sIdx}].session`]
                const exercisesErr = fieldErrors[`content[${sIdx}].exercises`]
                return (
                  <div key={sIdx} className="trainer-plan-session-box">
                    <Space
                      wrap
                      style={{ width: '100%', justifyContent: 'space-between', marginBottom: 8 }}
                    >
                      <Space>
                        <label htmlFor={`plan-session-${memberId}-${sIdx}`}>Buổi số</label>
                        <Input
                          id={`plan-session-${memberId}-${sIdx}`}
                          type="number"
                          min={1}
                          max={365}
                          style={{ width: 84 }}
                          value={sessionItem.session}
                          disabled={saving}
                          status={sessionErr ? 'error' : undefined}
                          onChange={(e) => updateSessionNumber(sIdx, e.target.value)}
                        />
                      </Space>
                      {sessions.length > 1 && (
                        <Button
                          size="small"
                          danger
                          icon={<DeleteOutlined />}
                          disabled={saving}
                          onClick={() => removeSession(sIdx)}
                        >
                          Xóa buổi
                        </Button>
                      )}
                    </Space>
                    {sessionErr && <div className="trainer-field-error">{sessionErr}</div>}
                    {exercisesErr && <div className="trainer-field-error">{exercisesErr}</div>}

                    <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                      {sessionItem.exercises.map((ex, eIdx) => {
                        const nameErr = fieldErrors[`content[${sIdx}].exercises[${eIdx}].name`]
                        const setsErr = fieldErrors[`content[${sIdx}].exercises[${eIdx}].sets`]
                        const repsErr = fieldErrors[`content[${sIdx}].exercises[${eIdx}].reps`]
                        return (
                          <div key={eIdx} className="trainer-exercise-grid">
                            <div>
                              <label
                                htmlFor={`plan-ex-name-${memberId}-${sIdx}-${eIdx}`}
                                style={{ display: 'block', fontSize: 12, marginBottom: 2 }}
                              >
                                Tên bài tập
                              </label>
                              <Input
                                id={`plan-ex-name-${memberId}-${sIdx}-${eIdx}`}
                                value={ex.name}
                                maxLength={100}
                                disabled={saving}
                                placeholder="Ví dụ: Squat"
                                status={nameErr ? 'error' : undefined}
                                onChange={(e) => updateExercise(sIdx, eIdx, 'name', e.target.value)}
                              />
                              {nameErr && <div className="trainer-field-error">{nameErr}</div>}
                            </div>

                            <div>
                              <label
                                htmlFor={`plan-ex-sets-${memberId}-${sIdx}-${eIdx}`}
                                style={{ display: 'block', fontSize: 12, marginBottom: 2 }}
                              >
                                Số hiệp
                              </label>
                              <Input
                                id={`plan-ex-sets-${memberId}-${sIdx}-${eIdx}`}
                                type="number"
                                min={1}
                                max={100}
                                value={ex.sets}
                                disabled={saving}
                                placeholder="4"
                                status={setsErr ? 'error' : undefined}
                                onChange={(e) => updateExercise(sIdx, eIdx, 'sets', e.target.value)}
                              />
                              {setsErr && <div className="trainer-field-error">{setsErr}</div>}
                            </div>

                            <div>
                              <label
                                htmlFor={`plan-ex-reps-${memberId}-${sIdx}-${eIdx}`}
                                style={{ display: 'block', fontSize: 12, marginBottom: 2 }}
                              >
                                Số lần
                              </label>
                              <Input
                                id={`plan-ex-reps-${memberId}-${sIdx}-${eIdx}`}
                                type="number"
                                min={1}
                                max={1000}
                                value={ex.reps}
                                disabled={saving}
                                placeholder="10"
                                status={repsErr ? 'error' : undefined}
                                onChange={(e) => updateExercise(sIdx, eIdx, 'reps', e.target.value)}
                              />
                              {repsErr && <div className="trainer-field-error">{repsErr}</div>}
                            </div>

                            <div style={{ paddingTop: 20 }}>
                              {sessionItem.exercises.length > 1 && (
                                <Button
                                  size="small"
                                  danger
                                  aria-label="Xóa bài tập"
                                  icon={<DeleteOutlined />}
                                  disabled={saving}
                                  onClick={() => removeExercise(sIdx, eIdx)}
                                />
                              )}
                            </div>
                          </div>
                        )
                      })}

                      <div>
                        <Button
                          size="small"
                          type="dashed"
                          icon={<PlusOutlined />}
                          disabled={saving}
                          onClick={() => addExercise(sIdx)}
                        >
                          Thêm bài tập
                        </Button>
                      </div>
                    </Space>
                  </div>
                )
              })}
            </div>

            <div>
              <Button type="primary" htmlType="submit" loading={saving}>
                Lưu giáo án
              </Button>
            </div>
          </Space>
        </form>
      </Card>

      <Card size="small" title="Danh sách giáo án">
        {plansQuery.loading && <Skeleton active />}
        {Boolean(plansQuery.error) && (
          <Alert
            type="error"
            showIcon
            title={trainerApiDetailMessage(plansQuery.error)}
            action={
              <Button size="small" onClick={plansQuery.reload}>
                Thử lại
              </Button>
            }
          />
        )}
        {!plansQuery.loading && !plansQuery.error && (plansQuery.data?.length ?? 0) === 0 && (
          <Empty description="Chưa có giáo án nào cho học viên này." />
        )}
        {!plansQuery.loading && !plansQuery.error && (plansQuery.data?.length ?? 0) > 0 && (
          <Space orientation="vertical" size={12} style={{ width: '100%' }}>
            {(plansQuery.data ?? []).map((plan) => (
              <Card
                key={plan.id}
                size="small"
                title={plan.title}
                extra={
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {formatVnDateTime(plan.createdAt)}
                  </Typography.Text>
                }
              >
                <Space orientation="vertical" size={6} style={{ width: '100%' }}>
                  <Space wrap size={6}>
                    <Tag>{plan.trainerName}</Tag>
                    {(plan.startDate || plan.endDate) && (
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        Áp dụng: {plan.startDate ? formatVnDate(plan.startDate) : '—'} –{' '}
                        {plan.endDate ? formatVnDate(plan.endDate) : '—'}
                      </Typography.Text>
                    )}
                  </Space>
                  {plan.content.map((s) => (
                    <div key={`${plan.id}-${s.session}`}>
                      <Typography.Text strong>Buổi {s.session}:</Typography.Text>{' '}
                      <Typography.Text>
                        {s.exercises
                          .map((ex) => {
                            const detail: string[] = []
                            if (ex.sets !== null && ex.sets !== undefined) {
                              detail.push(`${ex.sets} hiệp`)
                            }
                            if (ex.reps !== null && ex.reps !== undefined) {
                              detail.push(`${ex.reps} lần`)
                            }
                            return detail.length > 0 ? `${ex.name} (${detail.join(' × ')})` : ex.name
                          })
                          .join(', ')}
                      </Typography.Text>
                    </div>
                  ))}
                </Space>
              </Card>
            ))}
          </Space>
        )}
      </Card>
    </Space>
  )
}
