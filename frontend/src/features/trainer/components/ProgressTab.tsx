import { Alert, Button, Card, Empty, Input, Skeleton, Space, Typography } from 'antd'
import { useCallback, useState } from 'react'
import { useRemoteQuery } from '../../../services/use-remote-query'
import { trainerApiDetailMessage } from '../api/trainer-api'
import { formatVnDate } from '../trainer-time'
import type { ProgressQuery, TrainerStudentsGateway } from '../types/trainer'

interface ProgressTabProps {
  memberId: number
  api: Pick<TrainerStudentsGateway, 'getProgress'>
}

interface ProgressRequestKey {
  memberId: number
  from?: string
  to?: string
}

/**
 * Hiển thị tiến độ học viên từ dữ liệu thô do API trả về (UC-F9.6, BR-F9.13).
 * Biểu đồ vẽ bằng div và CSS thuần; không thêm thư viện, không tính trung bình, không gợi ý.
 */
export function ProgressTab({ memberId, api }: ProgressTabProps) {
  const [fromInput, setFromInput] = useState('')
  const [toInput, setToInput] = useState('')

  const loadProgress = useCallback(
    (req: ProgressRequestKey) => {
      const query: ProgressQuery = {}
      if (req.from) query.from = req.from
      if (req.to) query.to = req.to
      return api.getProgress(req.memberId, query)
    },
    [api],
  )
  const progressQuery = useRemoteQuery(loadProgress, { memberId } as ProgressRequestKey)

  function applyFilter() {
    progressQuery.changeQuery({
      memberId,
      from: fromInput || undefined,
      to: toInput || undefined,
    })
  }

  function resetFilter() {
    setFromInput('')
    setToInput('')
    progressQuery.changeQuery({ memberId })
  }

  const data = progressQuery.data
  const weightPoints = (data?.bodyMetrics ?? []).filter((m) => m.weight !== null)
  const maxBodyWeight = weightPoints.reduce((max, item) => Math.max(max, item.weight ?? 0), 0)

  const workoutWeightPoints = (data?.workoutLogs ?? []).filter((l) => l.weight !== null)
  const maxWorkoutWeight = workoutWeightPoints.reduce(
    (max, item) => Math.max(max, item.weight ?? 0),
    0,
  )

  const isEmpty =
    data !== undefined && data.bodyMetrics.length === 0 && data.workoutLogs.length === 0

  return (
    <Space orientation="vertical" size={16} style={{ width: '100%' }}>
      <Card size="small" title="Khoảng thời gian xem tiến độ">
        <div className="feature-filters" style={{ marginBottom: 0 }}>
          <div className="feature-filter">
            <label htmlFor={`progress-from-${memberId}`}>Từ ngày</label>
            <Input
              id={`progress-from-${memberId}`}
              type="date"
              value={fromInput}
              onChange={(e) => setFromInput(e.target.value)}
            />
          </div>
          <div className="feature-filter">
            <label htmlFor={`progress-to-${memberId}`}>Đến ngày</label>
            <Input
              id={`progress-to-${memberId}`}
              type="date"
              value={toInput}
              onChange={(e) => setToInput(e.target.value)}
            />
          </div>
          <Space wrap>
            <Button type="primary" onClick={applyFilter}>
              Xem tiến độ
            </Button>
            <Button onClick={resetFilter}>90 ngày gần nhất</Button>
          </Space>
        </div>
        {data && (
          <Typography.Text type="secondary" style={{ display: 'block', marginTop: 8, fontSize: 12 }}>
            Dữ liệu từ {formatVnDate(data.from)} đến {formatVnDate(data.to)}.
          </Typography.Text>
        )}
      </Card>

      {progressQuery.loading && (
        <Card size="small">
          <Skeleton active />
        </Card>
      )}

      {Boolean(progressQuery.error) && (
        <Alert
          type="error"
          showIcon
          title={trainerApiDetailMessage(progressQuery.error)}
          action={
            <Button size="small" onClick={progressQuery.reload}>
              Thử lại
            </Button>
          }
        />
      )}

      {!progressQuery.loading && !progressQuery.error && isEmpty && (
        <Card size="small">
          <Empty description="Chưa có dữ liệu tiến độ" />
        </Card>
      )}

      {!progressQuery.loading && !progressQuery.error && data && !isEmpty && (
        <>
          <Card size="small" title="Biểu đồ cân nặng (dữ liệu thô theo ngày đo)">
            {weightPoints.length === 0 ? (
              <Typography.Text type="secondary">
                Chưa có bản ghi cân nặng trong khoảng thời gian này.
              </Typography.Text>
            ) : (
              <div className="trainer-chart" data-testid="weight-progress-chart">
                {weightPoints.map((point) => {
                  const value = point.weight ?? 0
                  const widthPct = maxBodyWeight > 0 ? Math.round((value / maxBodyWeight) * 100) : 0
                  return (
                    <div key={point.id} className="trainer-chart-row">
                      <span className="trainer-chart-label">{formatVnDate(point.measuredAt)}</span>
                      <div className="trainer-chart-track">
                        <div className="trainer-chart-bar" style={{ width: `${widthPct}%` }} />
                      </div>
                      <span className="trainer-chart-value">{value} kg</span>
                    </div>
                  )
                })}
              </div>
            )}
          </Card>

          <Card size="small" title="Biểu đồ mức tạ nhật ký tập (dữ liệu thô)">
            {workoutWeightPoints.length === 0 ? (
              <Typography.Text type="secondary">
                Chưa có bản ghi mức tạ trong khoảng thời gian này.
              </Typography.Text>
            ) : (
              <div className="trainer-chart" data-testid="workout-progress-chart">
                {workoutWeightPoints.map((log) => {
                  const value = log.weight ?? 0
                  const widthPct =
                    maxWorkoutWeight > 0 ? Math.round((value / maxWorkoutWeight) * 100) : 0
                  return (
                    <div key={log.id} className="trainer-chart-row">
                      <span
                        className="trainer-chart-label"
                        title={`${formatVnDate(log.loggedAt)} · ${log.exercise}`}
                      >
                        {formatVnDate(log.loggedAt)} · {log.exercise}
                      </span>
                      <div className="trainer-chart-track">
                        <div
                          className="trainer-chart-bar trainer-chart-bar--workout"
                          style={{ width: `${widthPct}%` }}
                        />
                      </div>
                      <span className="trainer-chart-value">{value} kg</span>
                    </div>
                  )
                })}
              </div>
            )}
          </Card>

          {data.workoutLogs.length > 0 && (
            <Card size="small" title="Chi tiết nhật ký tập trong khoảng">
              <Space orientation="vertical" size={6} style={{ width: '100%' }}>
                {data.workoutLogs.map((log) => (
                  <div key={log.id}>
                    <Typography.Text strong>{formatVnDate(log.loggedAt)}</Typography.Text> —{' '}
                    <Typography.Text>{log.exercise}</Typography.Text>
                    <Typography.Text type="secondary">
                      {log.sets !== null ? ` · ${log.sets} hiệp` : ''}
                      {log.reps !== null ? ` × ${log.reps} lần` : ''}
                      {log.weight !== null ? ` · ${log.weight} kg` : ''}
                    </Typography.Text>
                  </div>
                ))}
              </Space>
            </Card>
          )}
        </>
      )}
    </Space>
  )
}
