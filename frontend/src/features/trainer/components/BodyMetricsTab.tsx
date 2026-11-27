import { Alert, Button, Card, Empty, Input, Skeleton, Space, Typography } from 'antd'
import { type FormEvent, useCallback, useState } from 'react'
import { useRemoteQuery } from '../../shared/use-remote-query'
import { trainerApiDetailMessage, trainerApiFieldErrors } from '../api/trainer-api'
import { formatVnDate, vnDateIso } from '../trainer-time'
import type { BodyMetricInput, TrainerStudentsGateway } from '../types/trainer'

interface BodyMetricsTabProps {
  memberId: number
  api: Pick<TrainerStudentsGateway, 'listBodyMetrics' | 'createBodyMetric'>
  now: () => Date
}

export function BodyMetricsTab({ memberId, api, now }: BodyMetricsTabProps) {
  const loadMetrics = useCallback((id: number) => api.listBodyMetrics(id), [api])
  const metricsQuery = useRemoteQuery(loadMetrics, memberId)

  const [measuredAt, setMeasuredAt] = useState(() => vnDateIso(now()))
  const [weight, setWeight] = useState('')
  const [height, setHeight] = useState('')
  const [bodyFat, setBodyFat] = useState('')
  const [muscleMass, setMuscleMass] = useState('')
  const [note, setNote] = useState('')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string>()
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [successText, setSuccessText] = useState<string>()

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setFormError(undefined)
    setFieldErrors({})
    setSuccessText(undefined)

    const payload: BodyMetricInput = {
      measuredAt,
      weight: weight.trim() ? Number(weight) : null,
      height: height.trim() ? Number(height) : null,
      bodyFat: bodyFat.trim() ? Number(bodyFat) : null,
      muscleMass: muscleMass.trim() ? Number(muscleMass) : null,
      note: note.trim() ? note.trim() : null,
    }

    try {
      await api.createBodyMetric(memberId, payload)
      setWeight('')
      setHeight('')
      setBodyFat('')
      setMuscleMass('')
      setNote('')
      setSuccessText('Đã lưu lần đo chỉ số cơ thể.')
      metricsQuery.reload()
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
      <Card size="small" title="Ghi chỉ số cơ thể">
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
              <label htmlFor={`metric-date-${memberId}`} style={{ display: 'block', marginBottom: 4 }}>
                Ngày đo
              </label>
              <Input
                id={`metric-date-${memberId}`}
                type="date"
                value={measuredAt}
                disabled={saving}
                status={fieldErrors.measuredAt ? 'error' : undefined}
                onChange={(e) => setMeasuredAt(e.target.value)}
              />
              {fieldErrors.measuredAt && (
                <div className="trainer-field-error">{fieldErrors.measuredAt}</div>
              )}
            </div>

            <div className="feature-form-grid">
              <div>
                <label
                  htmlFor={`metric-weight-${memberId}`}
                  style={{ display: 'block', marginBottom: 4 }}
                >
                  Cân nặng (kg)
                </label>
                <Input
                  id={`metric-weight-${memberId}`}
                  type="number"
                  step="0.1"
                  value={weight}
                  disabled={saving}
                  status={fieldErrors.weight ? 'error' : undefined}
                  onChange={(e) => setWeight(e.target.value)}
                />
                {fieldErrors.weight && (
                  <div className="trainer-field-error">{fieldErrors.weight}</div>
                )}
              </div>

              <div>
                <label
                  htmlFor={`metric-height-${memberId}`}
                  style={{ display: 'block', marginBottom: 4 }}
                >
                  Chiều cao (cm)
                </label>
                <Input
                  id={`metric-height-${memberId}`}
                  type="number"
                  step="0.1"
                  value={height}
                  disabled={saving}
                  status={fieldErrors.height ? 'error' : undefined}
                  onChange={(e) => setHeight(e.target.value)}
                />
                {fieldErrors.height && (
                  <div className="trainer-field-error">{fieldErrors.height}</div>
                )}
              </div>
            </div>

            <div className="feature-form-grid">
              <div>
                <label
                  htmlFor={`metric-fat-${memberId}`}
                  style={{ display: 'block', marginBottom: 4 }}
                >
                  Tỷ lệ mỡ (%)
                </label>
                <Input
                  id={`metric-fat-${memberId}`}
                  type="number"
                  step="0.1"
                  value={bodyFat}
                  disabled={saving}
                  status={fieldErrors.bodyFat ? 'error' : undefined}
                  onChange={(e) => setBodyFat(e.target.value)}
                />
                {fieldErrors.bodyFat && (
                  <div className="trainer-field-error">{fieldErrors.bodyFat}</div>
                )}
              </div>

              <div>
                <label
                  htmlFor={`metric-muscle-${memberId}`}
                  style={{ display: 'block', marginBottom: 4 }}
                >
                  Khối lượng cơ (kg)
                </label>
                <Input
                  id={`metric-muscle-${memberId}`}
                  type="number"
                  step="0.1"
                  value={muscleMass}
                  disabled={saving}
                  status={fieldErrors.muscleMass ? 'error' : undefined}
                  onChange={(e) => setMuscleMass(e.target.value)}
                />
                {fieldErrors.muscleMass && (
                  <div className="trainer-field-error">{fieldErrors.muscleMass}</div>
                )}
              </div>
            </div>

            <div>
              <label htmlFor={`metric-note-${memberId}`} style={{ display: 'block', marginBottom: 4 }}>
                Ghi chú (tùy chọn)
              </label>
              <Input.TextArea
                id={`metric-note-${memberId}`}
                rows={2}
                maxLength={1000}
                value={note}
                disabled={saving}
                status={fieldErrors.note ? 'error' : undefined}
                onChange={(e) => setNote(e.target.value)}
              />
              {fieldErrors.note && <div className="trainer-field-error">{fieldErrors.note}</div>}
            </div>

            <div>
              <Button type="primary" htmlType="submit" loading={saving}>
                Lưu chỉ số
              </Button>
            </div>
          </Space>
        </form>
      </Card>

      <Card size="small" title="Lịch sử chỉ số cơ thể">
        {metricsQuery.loading && <Skeleton active />}
        {Boolean(metricsQuery.error) && (
          <Alert
            type="error"
            showIcon
            title={trainerApiDetailMessage(metricsQuery.error)}
            action={
              <Button size="small" onClick={metricsQuery.reload}>
                Thử lại
              </Button>
            }
          />
        )}
        {!metricsQuery.loading && !metricsQuery.error && (metricsQuery.data?.length ?? 0) === 0 && (
          <Empty description="Chưa có lịch sử chỉ số cơ thể." />
        )}
        {!metricsQuery.loading && !metricsQuery.error && (metricsQuery.data?.length ?? 0) > 0 && (
          <Space orientation="vertical" size={8} style={{ width: '100%' }}>
            {(metricsQuery.data ?? []).map((item) => {
              const parts: string[] = []
              if (item.weight !== null) parts.push(`Cân nặng: ${item.weight} kg`)
              if (item.height !== null) parts.push(`Chiều cao: ${item.height} cm`)
              if (item.bodyFat !== null) parts.push(`Mỡ: ${item.bodyFat}%`)
              if (item.muscleMass !== null) parts.push(`Cơ: ${item.muscleMass} kg`)
              return (
                <Card key={item.id} size="small">
                  <Space orientation="vertical" size={4}>
                    <Typography.Text strong>{formatVnDate(item.measuredAt)}</Typography.Text>
                    <Typography.Text>{parts.join(' · ')}</Typography.Text>
                    {item.note && (
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        {item.note}
                      </Typography.Text>
                    )}
                  </Space>
                </Card>
              )
            })}
          </Space>
        )}
      </Card>
    </Space>
  )
}
