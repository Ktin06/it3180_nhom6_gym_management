import { Alert, Button, Card, Col, Empty, Row, Skeleton, Tag, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { useRemoteQuery } from '../../../services/use-remote-query'
import { publicPackagesApi } from '../api/public-packages'
import { packageTypeLabels, vndFormat } from '../package-format'
import type { PackageType, PublicPackage, PublicPackagesGateway } from '../types/package'

const typeInfo: Record<PackageType, { color: string; benefits: string[] }> = {
  TIME: {
    color: 'blue',
    benefits: ['Vào phòng tập không giới hạn trong thời hạn', 'Được đặt lớp nhóm'],
  },
  SESSION: {
    color: 'cyan',
    benefits: ['Mỗi lần vào phòng tập trừ một buổi', 'Không đặt được lớp nhóm'],
  },
  PT: {
    color: 'purple',
    benefits: ['Tập 1–1 cùng huấn luyện viên', 'Không dùng để vào phòng tập hay đặt lớp nhóm'],
  },
}

function usage(item: PublicPackage): string {
  if (item.type === 'TIME') return `Thời hạn ${item.durationDays} ngày`
  return `${item.sessionCount ?? 0} buổi, dùng trong ${item.durationDays} ngày`
}

function PackageCard({ item }: { item: PublicPackage }) {
  const info = typeInfo[item.type]
  return (
    <Card
      title={item.name}
      extra={<Tag color={info.color}>{packageTypeLabels[item.type]}</Tag>}
      style={{ height: '100%' }}
    >
      <Typography.Title level={3} style={{ marginTop: 0 }}>
        {vndFormat.format(item.price)}
      </Typography.Title>
      <Typography.Paragraph strong>{usage(item)}</Typography.Paragraph>
      <ul style={{ paddingLeft: 20 }}>
        {info.benefits.map((benefit) => (
          <li key={benefit}>{benefit}</li>
        ))}
      </ul>
      {item.description && (
        <Typography.Paragraph type="secondary">{item.description}</Typography.Paragraph>
      )}
    </Card>
  )
}

/** Bảng giá công khai cho khách chưa đăng nhập (US-F3.1.2). Giá luôn lấy từ server. */
export function PublicPricingPage({ api = publicPackagesApi }: { api?: PublicPackagesGateway }) {
  const list = useRemoteQuery(api.list, undefined)
  const items = list.data ?? []

  return (
    <section aria-labelledby="pricing-heading">
      <Typography.Title level={2} id="pricing-heading">
        Bảng giá gói tập
      </Typography.Title>
      <Typography.Paragraph>
        Giá lấy từ danh mục đang bán. <strong>Thanh toán tại quầy</strong>, không có thanh toán
        online.
      </Typography.Paragraph>

      {list.loading && (
        <Row gutter={[16, 16]} aria-busy="true">
          {[0, 1, 2].map((key) => (
            <Col key={key} xs={24} sm={12} lg={8}>
              <Card>
                <Skeleton active />
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {Boolean(list.error) && (
        <Alert
          type="error"
          showIcon
          title="Không tải được bảng giá. Vui lòng thử lại."
          action={
            <Button size="small" onClick={list.reload}>
              Thử lại
            </Button>
          }
        />
      )}

      {!list.loading && !list.error && items.length === 0 && (
        <Empty description="Chưa có gói đang bán." />
      )}

      {items.length > 0 && (
        <Row gutter={[16, 16]}>
          {items.map((item) => (
            <Col key={item.id} xs={24} sm={12} lg={8}>
              <PackageCard item={item} />
            </Col>
          ))}
        </Row>
      )}

      <Card style={{ marginTop: 24 }}>
        <Typography.Paragraph>
          Đăng ký chỉ tạo tài khoản, chưa kích hoạt gói. Sau khi đăng ký, bạn gửi yêu cầu mua gói ở
          cổng hội viên hoặc đến quầy để thanh toán.
        </Typography.Paragraph>
        <Link to="/register">
          <Button type="primary">Đăng ký tài khoản</Button>
        </Link>
      </Card>
    </section>
  )
}
