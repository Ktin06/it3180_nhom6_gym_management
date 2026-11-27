import { CalendarOutlined, CheckCircleOutlined, ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Space, Tag, Typography } from 'antd'
import { MemberAvatar } from '../../members/components/MemberAvatar'
import { errorMessage } from '../../shared/api-error'
import { isConflictError } from '../api/checkins'
import {
  formatGender,
  formatIsoDate,
  formatVnTime,
  MEMBERSHIP_STATUS_META,
  PACKAGE_TYPE_META,
} from '../checkin-format'
import type { CheckinResult, MemberSearchResult } from '../types/checkin'

export interface MemberCheckinCardProps {
  member: MemberSearchResult
  checkingIn: boolean
  checkinResult: CheckinResult | null
  checkinError: unknown
  onCheckin: () => void
  loadAvatar?: (memberId: number) => Promise<Blob>
}

/**
 * Thẻ đối chiếu thông tin hội viên, ảnh đại diện lớn, toàn bộ gói tập ở mọi trạng thái,
 * lớp sắp diễn ra và nút xác nhận check-in (UC-F4.1, UC-F4.2, UC-F4.3).
 */
export function MemberCheckinCard({
  member,
  checkingIn,
  checkinResult,
  checkinError,
  onCheckin,
  loadAvatar,
}: MemberCheckinCardProps) {
  const isInactive = member.status === 'INACTIVE'
  const conflict = isConflictError(checkinError)

  return (
    <Card
      className="checkin-member-card"
      title={
        <div className="checkin-card-header">
          <span>Đối chiếu hồ sơ hội viên</span>
          <Space size={6} wrap>
            <Tag color={isInactive ? 'red' : 'green'}>
              {isInactive ? 'Ngừng hoạt động' : 'Đang hoạt động'}
            </Tag>
            {member.isCurrentlyCheckedIn && <Tag color="processing">Đang tập hôm nay</Tag>}
          </Space>
        </div>
      }
    >
      <div className="checkin-member-profile">
        <MemberAvatar
          memberId={member.memberId}
          fullName={member.fullName}
          hasAvatar={member.hasAvatar}
          size={96}
          loadAvatar={loadAvatar}
        />
        <div className="checkin-member-meta">
          <Typography.Title level={4} style={{ margin: 0 }}>
            {member.fullName}
          </Typography.Title>
          <div className="checkin-member-sub">
            <Tag>{member.memberCode}</Tag>
            <span>SĐT: {member.phone}</span>
            <span>Giới tính: {formatGender(member.gender)}</span>
          </div>
          {!member.hasAvatar && (
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              Hội viên chưa cập nhật ảnh đại diện.
            </Typography.Text>
          )}
        </div>
      </div>

      {isInactive && (
        <Alert
          type="error"
          showIcon
          style={{ marginTop: 16 }}
          title="Hồ sơ hội viên đã ngừng hoạt động"
          description="Hồ sơ hội viên này đã ngừng hoạt động. Vui lòng liên hệ quản lý để được hỗ trợ."
        />
      )}

      <div className="checkin-section">
        <Typography.Text strong>Danh sách gói tập ({member.memberships.length})</Typography.Text>
        {member.memberships.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Hội viên chưa có gói tập nào."
            style={{ margin: '12px 0' }}
          />
        ) : (
          <div className="checkin-packages-list">
            {member.memberships.map((pkg) => {
              const typeMeta = PACKAGE_TYPE_META[pkg.packageType]
              const statusMeta = MEMBERSHIP_STATUS_META[pkg.status]
              return (
                <div key={pkg.id} className="checkin-package-item">
                  <div className="checkin-package-top">
                    <Typography.Text strong>{pkg.packageName}</Typography.Text>
                    <Space size={4} wrap>
                      <Tag color={typeMeta.color}>{typeMeta.label}</Tag>
                      <Tag color={statusMeta.color}>{statusMeta.label}</Tag>
                      {pkg.branchId === null && <Tag color="cyan">Toàn chuỗi</Tag>}
                    </Space>
                  </div>
                  <div className="checkin-package-meta">
                    <span>
                      Hiệu lực: {formatIsoDate(pkg.startDate)} – {formatIsoDate(pkg.endDate)}
                    </span>
                    {pkg.packageType !== 'TIME' && pkg.remainingSessions !== null && (
                      <span>
                        Còn lại: <strong>{pkg.remainingSessions}</strong>
                        {pkg.totalSessions !== null ? `/${pkg.totalSessions}` : ''} buổi
                      </span>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {member.upcomingClassSession && (
        <Alert
          type="info"
          showIcon
          icon={<CalendarOutlined />}
          style={{ marginTop: 16 }}
          title="Lớp học nhóm sắp diễn ra hôm nay"
          description={`Lớp ${member.upcomingClassSession.className} tại ${member.upcomingClassSession.roomName} lúc ${formatVnTime(member.upcomingClassSession.startTime)}. Hệ thống sẽ tự động ghi nhận đã đến khi check-in.`}
        />
      )}

      <div className="checkin-action-bar">
        <Button
          type="primary"
          size="large"
          icon={<CheckCircleOutlined />}
          disabled={isInactive}
          loading={checkingIn}
          onClick={onCheckin}
          className="checkin-submit-btn"
        >
          Xác nhận Check-in
        </Button>
      </div>

      {checkinResult && (
        <Alert
          data-testid="checkin-success-alert"
          type="success"
          showIcon
          style={{ marginTop: 16 }}
          title={checkinResult.message}
          description={
            <div className="checkin-result-details">
              <div>
                Gói áp dụng:{' '}
                <strong>
                  {checkinResult.appliedMembership.packageName ??
                    `Gói #${checkinResult.appliedMembership.id}`}
                </strong>{' '}
                ({PACKAGE_TYPE_META[checkinResult.appliedMembership.packageType].label}) ·{' '}
                {checkinResult.appliedMembership.sessionDeducted
                  ? 'Đã trừ 1 buổi'
                  : 'Không trừ buổi'}
                {checkinResult.appliedMembership.remainingSessions !== null &&
                  checkinResult.appliedMembership.remainingSessions !== undefined && (
                    <> · Còn lại: {checkinResult.appliedMembership.remainingSessions} buổi</>
                  )}
              </div>
              <div>Giờ vào: {formatVnTime(checkinResult.checkedInAt)} (giờ Việt Nam)</div>
              {checkinResult.classArrivals.length > 0 && (
                <div className="checkin-class-arrivals">
                  {checkinResult.classArrivals.map((cls) => (
                    <div key={cls.bookingId}>
                      Nhắc lớp: Hội viên có lớp <strong>{cls.className}</strong> tại{' '}
                      <strong>{cls.roomName}</strong> lúc{' '}
                      <strong>{formatVnTime(cls.startTime)}</strong> (đã đánh dấu có mặt).
                    </div>
                  ))}
                </div>
              )}
            </div>
          }
        />
      )}

      {checkinError != null && (
        <Alert
          data-testid="checkin-error-alert"
          type={conflict ? 'warning' : 'error'}
          showIcon
          style={{ marginTop: 16 }}
          title={conflict ? 'Xung đột lượt check-in đồng thời' : 'Không thể check-in'}
          description={errorMessage(checkinError)}
          action={
            conflict ? (
              <Button
                size="small"
                type="primary"
                icon={<ReloadOutlined />}
                loading={checkingIn}
                onClick={onCheckin}
              >
                Thử lại
              </Button>
            ) : undefined
          }
        />
      )}
    </Card>
  )
}
