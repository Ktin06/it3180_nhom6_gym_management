import { CalendarOutlined, LeftOutlined, ReloadOutlined, RightOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Skeleton, Space, Typography } from 'antd'
import { useCallback, useMemo, useState } from 'react'
import { useRemoteQuery } from '../../shared/use-remote-query'
import { trainerApi, trainerApiDetailMessage } from '../api/trainer-api'
import { AvailabilitySection } from '../components/AvailabilitySection'
import { PtSessionsSection } from '../components/PtSessionsSection'
import { addIsoDays, defaultSevenDayRange, formatVnDate } from '../trainer-time'
import type { TrainerScheduleGateway, TrainerScheduleQuery } from '../types/trainer'
import '../../shared/feature-pages.css'
import '../trainer.css'

export interface TrainerSchedulePageProps {
  api?: TrainerScheduleGateway
  now?: () => Date
}

/** Trang Lịch của tôi dành riêng cho huấn luyện viên (UC-F9.1, UC-F9.3). */
export function TrainerSchedulePage({
  api = trainerApi,
  now = () => new Date(),
}: TrainerSchedulePageProps) {
  const initialRange = useMemo(() => defaultSevenDayRange(now()), [now])
  const [range, setRange] = useState(initialRange)

  const loadSchedule = useCallback(
    (query: TrainerScheduleQuery) => api.getSchedule(query),
    [api],
  )
  const scheduleQuery = useRemoteQuery(loadSchedule, initialRange)
  const [cachedData, setCachedData] = useState(scheduleQuery.data)

  const activeData = scheduleQuery.data ?? cachedData
  if (scheduleQuery.data && scheduleQuery.data !== cachedData) {
    setCachedData(scheduleQuery.data)
  }

  function shiftWeek(deltaDays: number) {
    const next = {
      from: addIsoDays(range.from, deltaDays),
      to: addIsoDays(range.to, deltaDays),
    }
    setRange(next)
    scheduleQuery.changeQuery(next)
  }

  return (
    <section className="feature-page" aria-labelledby="trainer-schedule-heading">
      <div className="trainer-page-header">
        <div>
          <Typography.Title level={2} id="trainer-schedule-heading" style={{ marginBottom: 4 }}>
            <CalendarOutlined /> Lịch của tôi
          </Typography.Title>
          <Typography.Text type="secondary">
            Quản lý khung giờ rảnh, xác nhận buổi PT và theo dõi lịch dạy trong tuần (giờ Việt Nam).
          </Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={scheduleQuery.reload}>
          Tải lại
        </Button>
      </div>

      <Card size="small" style={{ marginBottom: 16 }}>
        <div className="trainer-week-bar">
          <Button icon={<LeftOutlined />} onClick={() => shiftWeek(-7)}>
            Tuần trước
          </Button>
          <Typography.Text strong>
            {formatVnDate(range.from)} – {formatVnDate(range.to)}
          </Typography.Text>
          <Button icon={<RightOutlined />} iconPlacement="end" onClick={() => shiftWeek(7)}>
            Tuần sau
          </Button>
        </div>
      </Card>

      {scheduleQuery.loading && !activeData && (
        <Card>
          <Skeleton active />
        </Card>
      )}

      {Boolean(scheduleQuery.error) && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          title={trainerApiDetailMessage(scheduleQuery.error)}
          action={
            <Button size="small" onClick={scheduleQuery.reload}>
              Thử lại
            </Button>
          }
        />
      )}

      {!scheduleQuery.error && activeData && (
        <Space orientation="vertical" size={16} style={{ width: '100%' }}>
          <PtSessionsSection
            ptSessions={activeData.ptSessions}
            classSessions={activeData.classSessions}
            api={api}
            now={now}
            onChanged={scheduleQuery.reload}
          />
          <AvailabilitySection
            availabilities={activeData.availabilities}
            api={api}
            onChanged={scheduleQuery.reload}
          />
        </Space>
      )}
    </section>
  )
}
