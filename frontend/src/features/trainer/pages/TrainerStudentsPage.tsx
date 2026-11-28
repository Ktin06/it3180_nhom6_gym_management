import { ReloadOutlined, TeamOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Col, Empty, Row, Skeleton, Space, Tabs, Tag, Typography } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useRemoteQuery } from '../../shared/use-remote-query'
import { trainerApi, trainerApiDetailMessage } from '../api/trainer-api'
import { BodyMetricsTab } from '../components/BodyMetricsTab'
import { ProgressTab } from '../components/ProgressTab'
import { WorkoutLogsTab } from '../components/WorkoutLogsTab'
import { WorkoutPlansTab } from '../components/WorkoutPlansTab'
import { addIsoDays, vnDateIso } from '../trainer-time'
import type { PtSessionScheduleItem, TrainerStudentsGateway } from '../types/trainer'
import '../../shared/feature-pages.css'
import '../trainer.css'

export interface TrainerStudentsPageProps {
  api?: TrainerStudentsGateway
  now?: () => Date
}

// Hàm mặc định phải cố định: `now` nằm trong phụ thuộc của useEffect tải lịch buổi PT. Viết `now = () => new Date()`
// ngay trong tham số thì mỗi lần render ra một hàm mới, effect chạy lại, setState, render lại: gọi API lặp vô hạn.
const systemNow = () => new Date()

/** Trang Học viên của huấn luyện viên: giáo án, nhật ký tập, chỉ số cơ thể và tiến độ (UC-F9.4–F9.6, UC-F2.5). */
export function TrainerStudentsPage({
  api = trainerApi,
  now = systemNow,
}: TrainerStudentsPageProps) {
  const loadStudents = useCallback(() => api.listStudents(), [api])
  const studentsQuery = useRemoteQuery(loadStudents, undefined)

  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(null)
  const [donePtSessions, setDonePtSessions] = useState<PtSessionScheduleItem[]>([])

  const students = useMemo(() => studentsQuery.data ?? [], [studentsQuery.data])

  const activeMemberId = useMemo(() => {
    if (selectedMemberId !== null && students.some((s) => s.memberId === selectedMemberId)) {
      return selectedMemberId
    }
    return students[0]?.memberId ?? null
  }, [selectedMemberId, students])

  const activeStudent = useMemo(
    () => students.find((s) => s.memberId === activeMemberId) ?? null,
    [students, activeMemberId],
  )

  useEffect(() => {
    let active = true
    const to = vnDateIso(now())
    const from = addIsoDays(to, -89)
    api
      .getSchedule({ from, to })
      .then((res) => {
        if (!active) return
        setDonePtSessions(res.ptSessions.filter((s) => s.status === 'DONE'))
      })
      .catch(() => {
        if (active) setDonePtSessions([])
      })
    return () => {
      active = false
    }
  }, [api, now])

  const activeStudentDoneSessions = useMemo(
    () =>
      activeMemberId === null
        ? []
        : donePtSessions.filter((s) => s.memberId === activeMemberId),
    [donePtSessions, activeMemberId],
  )

  return (
    <section className="feature-page" aria-labelledby="trainer-students-heading">
      <div className="trainer-page-header">
        <div>
          <Typography.Title level={2} id="trainer-students-heading" style={{ marginBottom: 4 }}>
            <TeamOutlined /> Học viên của tôi
          </Typography.Title>
          <Typography.Text type="secondary">
            Chỉ hiển thị học viên có buổi PT 1-1 với bạn.
          </Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={studentsQuery.reload}>
          Tải lại
        </Button>
      </div>

      {studentsQuery.loading && (
        <Card>
          <Skeleton active />
        </Card>
      )}

      {Boolean(studentsQuery.error) && (
        <Alert
          type="error"
          showIcon
          style={{ marginBottom: 16 }}
          title={trainerApiDetailMessage(studentsQuery.error)}
          action={
            <Button size="small" onClick={studentsQuery.reload}>
              Thử lại
            </Button>
          }
        />
      )}

      {!studentsQuery.loading && !studentsQuery.error && students.length === 0 && (
        <Card>
          <Empty description="Chưa có học viên" />
        </Card>
      )}

      {!studentsQuery.loading && !studentsQuery.error && students.length > 0 && (
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={7}>
            <Card size="small" title={`Danh sách học viên (${students.length})`}>
              <div className="trainer-student-list">
                {students.map((student) => {
                  const isActive = student.memberId === activeMemberId
                  return (
                    <button
                      key={student.memberId}
                      type="button"
                      className={`trainer-student-btn${isActive ? ' trainer-student-btn--active' : ''}`}
                      onClick={() => setSelectedMemberId(student.memberId)}
                    >
                      <Typography.Text strong>{student.fullName}</Typography.Text>
                      <Space wrap size={4}>
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {student.memberCode}
                        </Typography.Text>
                        {student.viaPt && <Tag color="green">PT 1-1</Tag>}
                        {student.viaClass && <Tag color="blue">Lớp nhóm</Tag>}
                      </Space>
                    </button>
                  )
                })}
              </div>
            </Card>
          </Col>

          <Col xs={24} lg={17}>
            {activeStudent && (
              <Card
                size="small"
                title={
                  <Space wrap>
                    <span>{activeStudent.fullName}</span>
                    <Typography.Text type="secondary" style={{ fontWeight: 400, fontSize: 13 }}>
                      ({activeStudent.memberCode})
                    </Typography.Text>
                  </Space>
                }
              >
                <Tabs
                  key={activeStudent.memberId}
                  destroyOnHidden
                  items={[
                    {
                      key: 'plans',
                      label: 'Giáo án',
                      children: <WorkoutPlansTab memberId={activeStudent.memberId} api={api} />,
                    },
                    {
                      key: 'logs',
                      label: 'Nhật ký tập',
                      children: (
                        <WorkoutLogsTab
                          memberId={activeStudent.memberId}
                          donePtSessions={activeStudentDoneSessions}
                          api={api}
                          now={now}
                        />
                      ),
                    },
                    {
                      key: 'metrics',
                      label: 'Chỉ số cơ thể',
                      children: (
                        <BodyMetricsTab memberId={activeStudent.memberId} api={api} now={now} />
                      ),
                    },
                    {
                      key: 'progress',
                      label: 'Tiến độ',
                      children: <ProgressTab memberId={activeStudent.memberId} api={api} />,
                    },
                  ]}
                />
              </Card>
            )}
          </Col>
        </Row>
      )}
    </section>
  )
}
