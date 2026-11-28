import { Alert, Button, Card, Tag, Typography } from 'antd'
import { useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { errorMessage } from '../../services/api-error'
import { useRemoteQuery } from '../../services/use-remote-query'
import { ClassQueryState } from './ClassCalendar'
import { classApi, type AttendanceRow, type AttendanceStatus } from './class-api'
import { classClock, classDateKey, classDay, classTimeRange, useClassClock } from './class-time'

const results: [AttendanceStatus, string][] = [
  ['ATTENDED', 'Có mặt'],
  ['NO_SHOW', 'Vắng'],
]

function AttendanceRoster({ sessionId }: { sessionId: number }) {
  const roster = useRemoteQuery(classApi.roster, sessionId)
  const now = useClassClock()
  const [pending, setPending] = useState<number>()
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string }>()
  const running = useRef(false)
  const session = roster.data?.session
  const canMark =
    session &&
    session.status !== 'CANCELLED' &&
    now.getTime() >= Date.parse(session.startAt) &&
    classDateKey(now) === classDateKey(session.startAt)

  async function mark(row: AttendanceRow, status: AttendanceStatus) {
    if (running.current) return
    running.current = true
    setPending(row.bookingId)
    setFeedback(undefined)
    try {
      await classApi.mark(sessionId, row, status)
      setFeedback({ type: 'success', text: `Đã lưu điểm danh của ${row.fullName}.` })
    } catch (error) {
      setFeedback({ type: 'error', text: errorMessage(error) })
    } finally {
      running.current = false
      setPending(undefined)
      roster.reload()
    }
  }

  return (
    <section className="class-calendar-page" aria-labelledby="attendance-heading">
      <header>
        <Link to="/trainer/classes">Về lịch dạy</Link>
        <Typography.Title level={2} id="attendance-heading">
          Điểm danh lớp nhóm
        </Typography.Title>
        {session && (
          <Typography.Text type="secondary">
            {classDay(session.startAt)} · {classTimeRange(session.startAt, session.endAt)}
          </Typography.Text>
        )}
      </header>
      {session && !canMark && (
        <Alert
          type="info"
          title="Có thể điểm danh từ giờ bắt đầu đến hết ngày diễn ra theo giờ Việt Nam."
        />
      )}
      {feedback && <Alert showIcon type={feedback.type} title={feedback.text} />}
      <ClassQueryState
        {...roster}
        empty={!roster.data?.bookings.length}
        emptyLabel="Buổi này chưa có hội viên đặt chỗ."
      />
      {roster.data?.bookings.map((row) => (
        <Card key={row.bookingId} size="small">
          <Typography.Text strong>{row.fullName}</Typography.Text>
          <Typography.Text type="secondary"> · {row.memberCode}</Typography.Text>
          <div>
            {row.arrivedAt && <Tag color="blue">Check-in {classClock(row.arrivedAt)}</Tag>}
            <Tag>{results.find(([status]) => status === row.status)?.[1] ?? 'Chưa điểm danh'}</Tag>
          </div>
          <div
            className="class-calendar-actions"
            role="group"
            aria-label={`Điểm danh ${row.fullName}`}
          >
            {results.map(([status, label]) => (
              <Button
                key={status}
                type={row.status === status ? 'primary' : 'default'}
                disabled={!canMark || pending != null || row.status === status}
                loading={pending === row.bookingId && row.status !== status}
                onClick={() => void mark(row, status)}
              >
                {label}
              </Button>
            ))}
          </div>
        </Card>
      ))}
    </section>
  )
}

export function TrainerAttendancePage() {
  const id = Number(useParams().sessionId)
  if (!Number.isSafeInteger(id) || id < 1)
    return <Alert type="error" title="Mã buổi học không hợp lệ." />
  return <AttendanceRoster key={id} sessionId={id} />
}
