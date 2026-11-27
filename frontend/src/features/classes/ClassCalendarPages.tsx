import { Alert, Button, Popconfirm, Switch, Tag, Typography } from 'antd'
import { useRef, useState } from 'react'
import { errorMessage } from '../../services/api-error'
import { useRemoteQuery } from '../../services/use-remote-query'
import { CalendarFilters, CalendarLink, ClassCalendar, ClassQueryState } from './ClassCalendar'
import { classApi, type BookingStatus, type CalendarSession, type MemberSession } from './class-api'
import { classClock, classDay, currentWeek, useClassClock } from './class-time'

const bookingLabels: Record<BookingStatus, string> = {
  BOOKED: 'Đã đặt',
  CANCELLED: 'Đã hủy chỗ',
  ATTENDED: 'Có mặt',
  NO_SHOW: 'Vắng mặt',
}

function classOpen(session: CalendarSession, now: Date) {
  return (
    ['SCHEDULED', 'RESCHEDULED'].includes(session.status) &&
    Date.parse(session.startAt) > now.getTime()
  )
}

export function PublicClassCalendarPage() {
  const now = useClassClock()
  const branches = useRemoteQuery(classApi.branches, undefined)
  const calendar = useRemoteQuery(classApi.publicCalendar, currentWeek(now))

  return (
    <section className="class-calendar-page" aria-labelledby="public-class-heading">
      <header>
        <Typography.Title level={2} id="public-class-heading">
          Lịch lớp nhóm
        </Typography.Title>
        <Typography.Text type="secondary">Giờ Việt Nam</Typography.Text>
      </header>
      <CalendarFilters
        initial={calendar.query}
        branches={branches.data}
        onApply={calendar.changeQuery}
      />
      {Boolean(branches.error) && (
        <Alert
          type="warning"
          title="Chưa tải được bộ lọc chi nhánh."
          action={<Button onClick={branches.reload}>Thử lại</Button>}
        />
      )}
      <ClassQueryState
        {...calendar}
        empty={!calendar.data?.length}
        emptyLabel="Không có lớp trong khoảng ngày đã chọn."
      />
      <ClassCalendar
        sessions={calendar.data ?? []}
        action={(session) =>
          classOpen(session, now) && (
            <CalendarLink to="/login" state={{ from: { pathname: '/member/classes' } }}>
              Đăng nhập để đặt
            </CalendarLink>
          )
        }
      />
    </section>
  )
}

export function MemberClassCalendarPage() {
  const now = useClassClock()
  const branches = useRemoteQuery(classApi.branches, undefined)
  const calendar = useRemoteQuery(classApi.memberCalendar, currentWeek(now))
  const [onlyMine, setOnlyMine] = useState(false)
  const [pending, setPending] = useState<number>()
  const running = useRef(false)
  const [feedback, setFeedback] = useState<{ type: 'error' | 'success'; text: string }>()
  const sessions = (calendar.data?.sessions ?? []).filter(
    (session) => !onlyMine || session.bookingId,
  )

  async function changeBooking(session: MemberSession, kind: 'book' | 'cancel') {
    const memberId = calendar.data?.memberId
    if (running.current || !memberId || (kind === 'cancel' && session.bookingId == null)) return
    running.current = true
    setPending(session.id)
    setFeedback(undefined)
    try {
      if (kind === 'cancel' && session.bookingId != null) await classApi.cancel(session.bookingId)
      if (kind === 'book') await classApi.book(session.id, memberId)
      setFeedback({
        type: 'success',
        text: kind === 'book' ? 'Đặt chỗ thành công.' : 'Đã hủy chỗ của bạn.',
      })
    } catch (error) {
      setFeedback({ type: 'error', text: errorMessage(error) })
    } finally {
      running.current = false
      setPending(undefined)
      calendar.reload()
    }
  }

  return (
    <section className="class-calendar-page" aria-labelledby="member-class-heading">
      <header>
        <Typography.Title level={2} id="member-class-heading">
          Đặt lớp nhóm
        </Typography.Title>
        <Typography.Text type="secondary">
          Giờ Việt Nam · Dành cho gói TIME đang hoạt động
        </Typography.Text>
      </header>
      <CalendarFilters
        initial={calendar.query}
        branches={branches.data}
        onApply={(filter) => {
          if (!running.current) calendar.changeQuery(filter)
        }}
      />
      {Boolean(branches.error) && (
        <Alert
          type="warning"
          title="Chưa tải được bộ lọc chi nhánh."
          action={<Button onClick={branches.reload}>Thử lại</Button>}
        />
      )}
      <label className="class-calendar-actions">
        <Switch checked={onlyMine} onChange={setOnlyMine} aria-label="Chỉ xem chỗ của tôi" />
        Chỉ xem chỗ của tôi
      </label>
      {feedback && <Alert showIcon type={feedback.type} title={feedback.text} />}
      <ClassQueryState
        {...calendar}
        empty={!sessions.length}
        emptyLabel={
          onlyMine
            ? 'Bạn chưa có chỗ đặt trong khoảng ngày này.'
            : 'Không có lớp trong khoảng ngày đã chọn.'
        }
      />
      <ClassCalendar
        sessions={sessions}
        action={(session) => (
          <>
            {session.originalStartAt && (
              <Typography.Text type="secondary">
                Giờ ban đầu {classClock(session.originalStartAt)} ·{' '}
                {classDay(session.originalStartAt)}
              </Typography.Text>
            )}
            {session.bookingStatus && (
              <Tag color={session.bookingStatus === 'BOOKED' ? 'green' : undefined}>
                {bookingLabels[session.bookingStatus]}
              </Tag>
            )}
            {classOpen(session, now) && session.bookingStatus === 'BOOKED' && (
              <Popconfirm
                title="Hủy chỗ đã đặt?"
                description={
                  session.originalStartAt
                    ? 'Buổi đã đổi giờ. Hủy chỗ không bị tính vi phạm.'
                    : 'Hủy sát giờ có thể bị tính vi phạm.'
                }
                onConfirm={() => changeBooking(session, 'cancel')}
                okText="Hủy chỗ"
                cancelText="Giữ chỗ"
                okButtonProps={{ danger: true }}
              >
                <Button danger disabled={pending != null} loading={pending === session.id}>
                  Hủy chỗ
                </Button>
              </Popconfirm>
            )}
            {classOpen(session, now) && session.bookingStatus !== 'BOOKED' && (
              <>
                <Typography.Text type="secondary">
                  {session.remaining > 0
                    ? `Còn ${session.remaining}/${session.capacity} chỗ`
                    : 'Lớp đã đủ chỗ'}
                </Typography.Text>
                <Button
                  type="primary"
                  disabled={session.remaining === 0 || pending != null}
                  loading={pending === session.id}
                  onClick={() => void changeBooking(session, 'book')}
                >
                  Đặt chỗ
                </Button>
              </>
            )}
          </>
        )}
      />
    </section>
  )
}

export function TrainerClassCalendarPage() {
  const calendar = useRemoteQuery(classApi.trainerCalendar, currentWeek())

  return (
    <section className="class-calendar-page" aria-labelledby="trainer-class-heading">
      <header>
        <Typography.Title level={2} id="trainer-class-heading">
          Lịch dạy lớp nhóm
        </Typography.Title>
        <Typography.Text type="secondary">Giờ Việt Nam</Typography.Text>
      </header>
      <CalendarFilters initial={calendar.query} onApply={calendar.changeQuery} />
      <ClassQueryState
        {...calendar}
        empty={!calendar.data?.length}
        emptyLabel="Bạn không có lớp trong khoảng ngày này."
      />
      <ClassCalendar
        sessions={calendar.data ?? []}
        action={(session) =>
          session.status !== 'CANCELLED' && (
            <CalendarLink to={`/trainer/classes/${session.id}/attendance`}>Điểm danh</CalendarLink>
          )
        }
      />
    </section>
  )
}
