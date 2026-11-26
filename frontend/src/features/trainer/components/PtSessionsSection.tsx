import { Alert, Button, Card, Col, Empty, Row, Space, Tag, Typography } from 'antd'
import axios from 'axios'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { trainerApiDetailMessage } from '../api/trainer-api'
import {
  formatVnDateFromUtc,
  formatVnDateTime,
  formatVnTime,
  ptSessionActionState,
} from '../trainer-time'
import type {
  ClassSessionScheduleItem,
  PtSessionScheduleItem,
  PtSessionStatus,
  TrainerScheduleGateway,
} from '../types/trainer'

const PT_STATUS_META: Record<PtSessionStatus, { label: string; color: string }> = {
  BOOKED: { label: 'Đã đặt', color: 'blue' },
  DONE: { label: 'Đã diễn ra', color: 'green' },
  NO_SHOW: { label: 'Vắng mặt', color: 'volcano' },
  CANCELLED: { label: 'Đã hủy', color: 'default' },
}

const CLASS_STATUS_META: Record<string, { label: string; color: string }> = {
  SCHEDULED: { label: 'Theo lịch', color: 'blue' },
  RESCHEDULED: { label: 'Đã đổi giờ', color: 'orange' },
  CANCELLED: { label: 'Đã hủy', color: 'red' },
  COMPLETED: { label: 'Đã kết thúc', color: 'default' },
}

interface PtSessionsSectionProps {
  ptSessions: PtSessionScheduleItem[]
  classSessions: ClassSessionScheduleItem[]
  api: Pick<TrainerScheduleGateway, 'confirmPtSession' | 'markPtSessionNoShow'>
  now: () => Date
  onChanged: () => void
}

export function PtSessionsSection({
  ptSessions,
  classSessions,
  api,
  now,
  onChanged,
}: PtSessionsSectionProps) {
  const [busySessionId, setBusySessionId] = useState<number | null>(null)
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(
    null,
  )

  async function handleComplete(sessionId: number, outcome: 'DONE' | 'NO_SHOW') {
    setBusySessionId(sessionId)
    setFeedback(null)
    try {
      const result =
        outcome === 'DONE'
          ? await api.confirmPtSession(sessionId)
          : await api.markPtSessionNoShow(sessionId)

      const notes: string[] = [
        outcome === 'DONE' ? 'Đã xác nhận buổi PT hoàn thành.' : 'Đã ghi nhận hội viên vắng mặt.',
      ]
      if (result.remainingSessions !== null) {
        notes.push(`Gói PT còn ${result.remainingSessions} buổi.`)
      }
      if (result.activatedMembershipId !== null) {
        notes.push('Đã kích hoạt gói PT nối tiếp.')
      }
      if (result.cancelledSessionIds.length > 0) {
        notes.push(
          `Đã hủy ${result.cancelledSessionIds.length} buổi tương lai vì gói của hội viên hết buổi.`,
        )
      }
      setFeedback({ type: 'success', text: notes.join(' ') })
      onChanged()
    } catch (cause) {
      setFeedback({ type: 'error', text: trainerApiDetailMessage(cause) })
      // 409: thao tác khác vừa đổi buổi; 422: buổi có thể đã được xác nhận, ghi vắng hoặc hủy ở tab khác.
      // Cả hai đều tải lại để thẻ hiện trạng thái thật, không để nút bấm của buổi đã đóng.
      const status = axios.isAxiosError(cause) ? cause.response?.status : undefined
      if (status === 409 || status === 422) {
        onChanged()
      }
    } finally {
      setBusySessionId(null)
    }
  }

  const currentNow = now()

  return (
    <Space orientation="vertical" size={16} style={{ width: '100%' }}>
      <Card title="Buổi PT 1-1 trong tuần">
        {feedback && (
          <Alert
            type={feedback.type}
            showIcon
            closable
            onClose={() => setFeedback(null)}
            title={feedback.text}
            style={{ marginBottom: 12 }}
          />
        )}

        {ptSessions.length === 0 ? (
          <Empty description="Không có buổi PT nào trong tuần này." />
        ) : (
          <Row gutter={[12, 12]}>
            {ptSessions.map((session) => {
              const meta = PT_STATUS_META[session.status]
              const actionState =
                session.status === 'BOOKED'
                  ? ptSessionActionState(session.startAt, currentNow)
                  : null
              const isBusy = busySessionId === session.id

              return (
                <Col key={session.id} xs={24} md={12}>
                  <Card
                    size="small"
                    data-testid={`pt-session-${session.id}`}
                    title={
                      <span>
                        {formatVnDateFromUtc(session.startAt)} · {formatVnTime(session.startAt)} –{' '}
                        {formatVnTime(session.endAt)}
                      </span>
                    }
                    extra={<Tag color={meta.color}>{meta.label}</Tag>}
                  >
                    <Space orientation="vertical" size={6} style={{ width: '100%' }}>
                      <div>
                        <Typography.Text strong>{session.memberName}</Typography.Text>{' '}
                        <Typography.Text type="secondary">({session.memberCode})</Typography.Text>
                      </div>
                      {session.note && (
                        <Typography.Text type="secondary">Ghi chú: {session.note}</Typography.Text>
                      )}

                      {actionState === 'BEFORE_START' && (
                        <div>
                          <Space wrap>
                            <Button type="primary" size="small" disabled>
                              Xác nhận buổi
                            </Button>
                            <Button danger size="small" disabled>
                              Ghi vắng mặt
                            </Button>
                          </Space>
                          <div>
                            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                              Chưa tới giờ bắt đầu buổi tập.
                            </Typography.Text>
                          </div>
                        </div>
                      )}

                      {actionState === 'ACTIONABLE' && (
                        <Space wrap>
                          <Button
                            type="primary"
                            size="small"
                            loading={isBusy}
                            disabled={busySessionId !== null && !isBusy}
                            onClick={() => void handleComplete(session.id, 'DONE')}
                          >
                            Xác nhận buổi
                          </Button>
                          <Button
                            danger
                            size="small"
                            loading={isBusy}
                            disabled={busySessionId !== null && !isBusy}
                            onClick={() => void handleComplete(session.id, 'NO_SHOW')}
                          >
                            Ghi vắng mặt
                          </Button>
                        </Space>
                      )}

                      {actionState === 'EXPIRED' && (
                        <Typography.Text type="warning">
                          Quá hạn, liên hệ quản lý chi nhánh
                        </Typography.Text>
                      )}
                    </Space>
                  </Card>
                </Col>
              )
            })}
          </Row>
        )}
      </Card>

      <Card
        title="Buổi lớp nhóm trong tuần"
        extra={
          <Link to="/classes" style={{ fontWeight: 500 }}>
            Mở lịch dạy lớp & điểm danh
          </Link>
        }
      >
        {classSessions.length === 0 ? (
          <Empty description="Không có buổi lớp nhóm nào được phân công trong tuần này." />
        ) : (
          <Row gutter={[12, 12]}>
            {classSessions.map((cls) => {
              const meta = CLASS_STATUS_META[cls.status] ?? {
                label: cls.status,
                color: 'default',
              }
              return (
                <Col key={cls.id} xs={24} md={12}>
                  <Card
                    size="small"
                    title={cls.classTypeName}
                    extra={<Tag color={meta.color}>{meta.label}</Tag>}
                  >
                    <Space orientation="vertical" size={4}>
                      <Typography.Text strong>
                        {formatVnDateFromUtc(cls.startAt)} · {formatVnTime(cls.startAt)} –{' '}
                        {formatVnTime(cls.endAt)}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        Phòng {cls.roomName} · Sức chứa {cls.capacity}
                      </Typography.Text>
                      {cls.originalStartAt !== null && (
                        <Typography.Text type="warning" style={{ fontSize: 12 }}>
                          Đã đổi giờ từ {formatVnDateTime(cls.originalStartAt)}
                        </Typography.Text>
                      )}
                    </Space>
                  </Card>
                </Col>
              )
            })}
          </Row>
        )}
      </Card>
    </Space>
  )
}
