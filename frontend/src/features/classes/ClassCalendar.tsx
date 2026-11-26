import { Alert, Button, Card, Empty, Input, Select, Skeleton, Tag, theme, Typography } from 'antd'
import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, type LinkProps } from 'react-router-dom'
import { errorMessage } from '../shared/api-error'
import type { BranchOption, CalendarFilter, CalendarSession } from './class-api'
import { classDateKey, classDay, classTimeRange } from './class-time'
import './class-calendar.css'

const sessionLabels: Record<CalendarSession['status'], string> = {
  SCHEDULED: 'Theo lịch',
  RESCHEDULED: 'Đã đổi giờ',
  CANCELLED: 'Đã hủy',
  COMPLETED: 'Đã hoàn thành',
}

export function CalendarLink(props: LinkProps) {
  const { token } = theme.useToken()
  return (
    <Link
      {...props}
      className="class-calendar-link"
      style={{ color: token.colorPrimaryText, borderRadius: token.borderRadius }}
    />
  )
}

export function CalendarFilters({
  initial,
  branches,
  onApply,
}: {
  initial: CalendarFilter
  branches?: BranchOption[]
  onApply: (filter: CalendarFilter) => void
}) {
  const [filter, setFilter] = useState(initial)
  const [dateError, setDateError] = useState('')
  function submit(event: FormEvent) {
    event.preventDefault()
    const days = (Date.parse(filter.to) - Date.parse(filter.from)) / 86_400_000
    if (!Number.isFinite(days) || days < 0 || days > 30) {
      setDateError('Chọn khoảng từ 1 đến 31 ngày. Ngày kết thúc phải từ ngày bắt đầu trở đi.')
      return
    }
    setDateError('')
    onApply(filter)
  }

  return (
    <form className="class-calendar-filters" onSubmit={submit}>
      {branches && (
        <div className="class-calendar-field">
          <label htmlFor="class-branch">Chi nhánh</label>
          <Select
            id="class-branch"
            allowClear
            showSearch={{ optionFilterProp: 'label' }}
            placeholder="Tất cả chi nhánh"
            value={filter.branchId}
            onChange={(branchId) => setFilter({ ...filter, branchId })}
            options={branches.map(({ id, name }) => ({ value: id, label: name }))}
            size="large"
          />
        </div>
      )}
      <label className="class-calendar-field">
        Từ ngày
        <Input
          type="date"
          size="large"
          required
          value={filter.from}
          onChange={(e) => setFilter({ ...filter, from: e.target.value })}
        />
      </label>
      <label className="class-calendar-field">
        Đến ngày
        <Input
          type="date"
          size="large"
          required
          value={filter.to}
          onChange={(e) => setFilter({ ...filter, to: e.target.value })}
        />
      </label>
      <Button type="primary" htmlType="submit" size="large">
        Xem lịch
      </Button>
      {dateError && (
        <Alert className="class-calendar-filter-error" title={dateError} type="error" />
      )}
    </form>
  )
}

export function ClassQueryState({
  loading,
  error,
  empty,
  emptyLabel,
  reload,
}: {
  loading: boolean
  error: unknown
  empty: boolean
  emptyLabel: string
  reload: () => void
}) {
  if (loading) return <Skeleton active aria-busy="true" paragraph={{ rows: 3 }} />
  if (error)
    return (
      <Alert
        type="error"
        showIcon
        title={errorMessage(error)}
        action={<Button onClick={reload}>Thử lại</Button>}
      />
    )
  return empty ? <Empty description={emptyLabel} /> : null
}

export function ClassCalendar<T extends CalendarSession>({
  sessions,
  action,
}: {
  sessions: T[]
  action: (session: T) => ReactNode
}) {
  const { token } = theme.useToken()
  const days = new Map<string, T[]>()
  for (const session of sessions) {
    const key = classDateKey(session.startAt)
    const slots = days.get(key)
    if (slots) slots.push(session)
    else days.set(key, [session])
  }

  return Array.from(days, ([date, slots]) => (
    <section className="class-calendar-day" key={date} aria-label={classDay(slots[0].startAt)}>
      <Typography.Title level={4}>{classDay(slots[0].startAt)}</Typography.Title>
      <div className="class-calendar-grid">
        {slots.map((session) => (
          <Card key={session.id} size="small" className="class-calendar-card">
            <div className="class-calendar-card-heading">
              <Typography.Title level={5}>{session.className}</Typography.Title>
              {session.status !== 'SCHEDULED' && (
                <Tag color={session.status === 'RESCHEDULED' ? 'gold' : undefined}>
                  {sessionLabels[session.status]}
                </Tag>
              )}
            </div>
            <Typography.Text strong>
              {classTimeRange(session.startAt, session.endAt)}
            </Typography.Text>
            <div>
              {session.branchName} · {session.roomName}
            </div>
            <Typography.Text style={{ color: token.colorTextSecondary }}>
              {session.trainerName}
            </Typography.Text>
            <div className="class-calendar-actions">{action(session)}</div>
          </Card>
        ))}
      </div>
    </section>
  ))
}
