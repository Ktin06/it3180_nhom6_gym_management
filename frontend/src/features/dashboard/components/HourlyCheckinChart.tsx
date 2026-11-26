import { Card, Empty, Skeleton, Typography } from 'antd'
import type { HourlyCheckin } from '../types/dashboard'

export interface HourlyCheckinChartProps {
  data: HourlyCheckin[]
  loading?: boolean
}

/**
 * Biểu đồ cột mật độ check-in theo khung giờ mở cửa (UC-F6.2).
 * Vẽ hoàn toàn bằng div và CSS thuần, không dùng thư viện biểu đồ.
 * Hiển thị đủ mọi khung giờ API trả về, kể cả khung có 0 lượt.
 */
export function HourlyCheckinChart({ data, loading = false }: HourlyCheckinChartProps) {
  const maxCount = data.reduce((max, item) => (item.count > max ? item.count : max), 0)

  return (
    <Card
      title="Mật độ check-in theo khung giờ trong ngày"
      extra={
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          Múi giờ Việt Nam (UTC+7)
        </Typography.Text>
      }
      className="dashboard-chart-card"
    >
      {loading ? (
        <Skeleton active paragraph={{ rows: 4 }} />
      ) : data.length === 0 ? (
        <Empty description="Chưa có dữ liệu khung giờ check-in." />
      ) : (
        <div
          className="dashboard-chart-scroll"
          data-testid="hourly-checkin-chart"
          role="region"
          aria-label="Biểu đồ số lượt check-in theo khung giờ"
        >
          <div className="dashboard-chart-bars">
            {data.map((item) => {
              const heightPercent = maxCount > 0 ? Math.round((item.count / maxCount) * 100) : 0
              const isPeak = maxCount > 0 && item.count === maxCount
              const isZero = item.count === 0
              return (
                <div
                  key={item.hour}
                  className="dashboard-chart-col"
                  data-testid={`hour-bar-${item.hour}`}
                  title={`${item.hour}: ${item.count} lượt check-in`}
                >
                  <span
                    className={`dashboard-chart-count${isPeak ? ' is-peak' : ''}${isZero ? ' is-zero' : ''}`}
                  >
                    {item.count}
                  </span>
                  <div className="dashboard-chart-track">
                    <div
                      className={`dashboard-chart-fill${isPeak ? ' is-peak' : ''}${isZero ? ' is-zero' : ''}`}
                      style={{ height: isZero ? '4px' : `${Math.max(heightPercent, 8)}%` }}
                    />
                  </div>
                  <span className={`dashboard-chart-label${isPeak ? ' is-peak' : ''}`}>
                    {item.hour}
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </Card>
  )
}
