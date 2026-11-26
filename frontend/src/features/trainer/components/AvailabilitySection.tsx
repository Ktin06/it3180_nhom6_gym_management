import { PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Col, Empty, Form, Input, Modal, Row, Select, Space, Typography } from 'antd'
import { useState } from 'react'
import { trainerApiDetailMessage, trainerApiFieldErrors } from '../api/trainer-api'
import { DAY_OF_WEEK_LABELS, DAY_OF_WEEK_OPTIONS, formatClockTime } from '../trainer-time'
import type { TrainerAvailability, TrainerScheduleGateway } from '../types/trainer'

interface AvailabilityFormValues {
  dayOfWeek: number
  startTime: string
  endTime: string
}

interface AvailabilitySectionProps {
  availabilities: TrainerAvailability[]
  api: Pick<
    TrainerScheduleGateway,
    'createAvailability' | 'updateAvailability' | 'deleteAvailability'
  >
  onChanged: () => void
}

export function AvailabilitySection({ availabilities, api, onChanged }: AvailabilitySectionProps) {
  const [form] = Form.useForm<AvailabilityFormValues>()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<TrainerAvailability | null>(null)
  const [saving, setSaving] = useState(false)
  const [modalError, setModalError] = useState<string>()
  const [deletingSlot, setDeletingSlot] = useState<TrainerAvailability | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [sectionError, setSectionError] = useState<string>()
  const [sectionSuccess, setSectionSuccess] = useState<string>()

  function openCreate() {
    setEditing(null)
    setModalError(undefined)
    setSectionError(undefined)
    form.resetFields()
    form.setFieldsValue({ dayOfWeek: 1, startTime: '08:00', endTime: '12:00' })
    setModalOpen(true)
  }

  function openEdit(slot: TrainerAvailability) {
    setEditing(slot)
    setModalError(undefined)
    setSectionError(undefined)
    form.resetFields()
    form.setFieldsValue({
      dayOfWeek: slot.dayOfWeek,
      startTime: formatClockTime(slot.startTime),
      endTime: formatClockTime(slot.endTime),
    })
    setModalOpen(true)
  }

  async function handleSave(values: AvailabilityFormValues) {
    setSaving(true)
    setModalError(undefined)
    setSectionSuccess(undefined)
    const payload = {
      dayOfWeek: values.dayOfWeek,
      startTime: values.startTime,
      endTime: values.endTime,
    }
    try {
      if (editing) {
        await api.updateAvailability(editing.id, payload)
        setSectionSuccess('Đã cập nhật khung giờ rảnh.')
      } else {
        await api.createAvailability(payload)
        setSectionSuccess('Đã thêm khung giờ rảnh mới.')
      }
      setModalOpen(false)
      onChanged()
    } catch (cause) {
      const fieldErrors = trainerApiFieldErrors(cause)
      const knownFields = ['dayOfWeek', 'startTime', 'endTime'] as const
      const fieldsToSet = knownFields.flatMap((name) =>
        fieldErrors[name] ? [{ name, errors: [fieldErrors[name]] }] : [],
      )
      if (fieldsToSet.length > 0) {
        form.setFields(fieldsToSet)
      } else {
        setModalError(trainerApiDetailMessage(cause))
      }
    } finally {
      setSaving(false)
    }
  }

  async function handleConfirmDelete() {
    if (!deletingSlot) return
    setDeleting(true)
    setSectionError(undefined)
    setSectionSuccess(undefined)
    try {
      await api.deleteAvailability(deletingSlot.id)
      setDeletingSlot(null)
      setSectionSuccess('Đã xóa khung giờ rảnh.')
      onChanged()
    } catch (cause) {
      setDeletingSlot(null)
      setSectionError(trainerApiDetailMessage(cause))
    } finally {
      setDeleting(false)
    }
  }

  return (
    <Card
      title="Khung giờ rảnh hằng tuần"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
          Thêm khung giờ rảnh
        </Button>
      }
    >
      <Typography.Paragraph type="secondary" style={{ marginTop: 0 }}>
        Hội viên chỉ đặt được buổi PT trong các khung giờ bạn khai báo (lặp theo thứ hằng tuần).
      </Typography.Paragraph>

      {sectionSuccess && (
        <Alert
          type="success"
          showIcon
          closable
          onClose={() => setSectionSuccess(undefined)}
          title={sectionSuccess}
          style={{ marginBottom: 12 }}
        />
      )}

      {sectionError && (
        <Alert
          type="error"
          showIcon
          closable
          onClose={() => setSectionError(undefined)}
          title={sectionError}
          style={{ marginBottom: 12 }}
        />
      )}

      {availabilities.length === 0 ? (
        <Empty description="Chưa có khung giờ rảnh nào được khai báo." />
      ) : (
        <Row gutter={[12, 12]}>
          {availabilities.map((slot) => (
            <Col key={slot.id} xs={24} sm={12} lg={8}>
              <Card
                size="small"
                data-testid={`availability-item-${slot.id}`}
                title={DAY_OF_WEEK_LABELS[slot.dayOfWeek] ?? `Thứ ${slot.dayOfWeek}`}
                extra={
                  <Space size={4}>
                    <Button size="small" onClick={() => openEdit(slot)}>
                      Sửa
                    </Button>
                    <Button
                      size="small"
                      danger
                      onClick={() => {
                        setSectionError(undefined)
                        setDeletingSlot(slot)
                      }}
                    >
                      Xóa
                    </Button>
                  </Space>
                }
              >
                <Typography.Text strong>
                  {formatClockTime(slot.startTime)} – {formatClockTime(slot.endTime)}
                </Typography.Text>
                <div>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    Chi nhánh #{slot.branchId}
                  </Typography.Text>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {modalOpen && (
        <Modal
          open
          title={editing ? 'Sửa khung giờ rảnh' : 'Thêm khung giờ rảnh'}
          onCancel={saving ? undefined : () => setModalOpen(false)}
          closable={!saving}
          mask={{ closable: !saving }}
          footer={null}
          destroyOnHidden
        >
          <Form
            form={form}
            layout="vertical"
            disabled={saving}
            onFinish={(values) => void handleSave(values)}
          >
            {modalError && (
              <Alert type="error" showIcon title={modalError} style={{ marginBottom: 12 }} />
            )}

            <Form.Item
              name="dayOfWeek"
              label="Thứ trong tuần"
              rules={[{ required: true, message: 'Vui lòng chọn thứ trong tuần.' }]}
            >
              <Select options={DAY_OF_WEEK_OPTIONS} />
            </Form.Item>

            <div className="feature-form-grid">
              <Form.Item
                name="startTime"
                label="Giờ bắt đầu"
                rules={[{ required: true, message: 'Vui lòng nhập giờ bắt đầu.' }]}
              >
                <Input type="time" />
              </Form.Item>

              <Form.Item
                name="endTime"
                label="Giờ kết thúc"
                rules={[{ required: true, message: 'Vui lòng nhập giờ kết thúc.' }]}
              >
                <Input type="time" />
              </Form.Item>
            </div>

            <div className="feature-form-actions">
              <Button disabled={saving} onClick={() => setModalOpen(false)}>
                Hủy
              </Button>
              <Button type="primary" htmlType="submit" loading={saving}>
                Lưu
              </Button>
            </div>
          </Form>
        </Modal>
      )}

      {deletingSlot && (
        <Modal
          open
          title="Xác nhận xóa khung giờ rảnh"
          onCancel={deleting ? undefined : () => setDeletingSlot(null)}
          closable={!deleting}
          mask={{ closable: !deleting }}
          footer={
            <Space>
              <Button disabled={deleting} onClick={() => setDeletingSlot(null)}>
                Hủy
              </Button>
              <Button
                danger
                type="primary"
                loading={deleting}
                onClick={() => void handleConfirmDelete()}
              >
                Xác nhận xóa
              </Button>
            </Space>
          }
        >
          <Typography.Paragraph style={{ marginBottom: 0 }}>
            Bạn có chắc muốn xóa khung giờ rảnh{' '}
            <strong>
              {DAY_OF_WEEK_LABELS[deletingSlot.dayOfWeek] ?? `Thứ ${deletingSlot.dayOfWeek}`}{' '}
              ({formatClockTime(deletingSlot.startTime)} – {formatClockTime(deletingSlot.endTime)})
            </strong>
            ? Khung giờ đã có hội viên đặt buổi chưa diễn ra sẽ không thể xóa.
          </Typography.Paragraph>
        </Modal>
      )}
    </Card>
  )
}
