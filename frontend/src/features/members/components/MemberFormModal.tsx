import { Alert, Button, Form, Input, Modal, Select } from 'antd'
import { useState } from 'react'
import axios from 'axios'
import { errorMessage } from '../../shared/api-error'
import type { Gender, MemberDetail, MembersGateway } from '../types/member'
import { normalizePhone } from '../phone'

interface Values {
  fullName: string
  phone: string
  gender?: Gender
  dob?: string
  status: 'ACTIVE' | 'INACTIVE'
}

export function MemberFormModal({
  member,
  api,
  onClose,
  onSaved,
}: {
  member: MemberDetail | null
  api: MembersGateway
  onClose: () => void
  onSaved: () => void
}) {
  const [form] = Form.useForm<Values>()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string>()
  const status = Form.useWatch('status', form) ?? member?.status

  async function save(values: Values) {
    setSaving(true)
    setError(undefined)
    const input = {
      fullName: values.fullName.trim(),
      phone: values.phone.trim(),
      gender: values.gender ?? null,
      dob: values.dob || null,
    }
    try {
      if (member) await api.update(member.id, { ...input, status: values.status })
      else await api.create(input)
      onSaved()
    } catch (cause) {
      setError(errorMessage(cause))
      if (axios.isAxiosError(cause) && cause.response?.status === 400) {
        const errors: unknown = cause.response.data?.errors
        if (errors && typeof errors === 'object') {
          const fieldNames = ['fullName', 'phone', 'gender', 'dob', 'status'] as const
          const fields = Object.entries(errors).flatMap(([name, value]) => {
            const field = fieldNames.find((candidate) => candidate === name)
            return field && typeof value === 'string' ? [{ name: field, errors: [value] }] : []
          })
          form.setFields(fields)
        }
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      open
      title={member ? `Sửa hồ sơ ${member.memberCode}` : 'Thêm hội viên'}
      onCancel={saving ? undefined : onClose}
      closable={!saving}
      mask={{ closable: !saving }}
      width={640}
      footer={null}
    >
      <Form
        form={form}
        layout="vertical"
        disabled={saving}
        onFinish={save}
        initialValues={
          member
            ? { ...member, gender: member.gender ?? undefined, dob: member.dob ?? '' }
            : { status: 'ACTIVE' }
        }
      >
        {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
        <p>Họ tên và số điện thoại là thông tin bắt buộc. Mã hội viên được cấp tự động.</p>
        <Form.Item
          name="fullName"
          label="Họ và tên"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập họ và tên.' },
            { max: 255, message: 'Họ tên không quá 255 ký tự.' },
          ]}
        >
          <Input autoComplete="name" maxLength={255} />
        </Form.Item>
        <Form.Item
          name="phone"
          label="Số điện thoại"
          extra="Chấp nhận 0xxxxxxxxx, +84xxxxxxxxx hoặc 84xxxxxxxxx; có thể có dấu cách, chấm hoặc gạch ngang."
          rules={[
            { required: true, message: 'Vui lòng nhập số điện thoại.' },
            {
              validator: (_, value: string | undefined) => {
                const phone = normalizePhone(value ?? '')
                return !value || /^0\d{9}$/.test(phone)
                  ? Promise.resolve()
                  : Promise.reject(new Error('Số điện thoại không hợp lệ.'))
              },
            },
          ]}
        >
          <Input type="tel" autoComplete="tel" />
        </Form.Item>
        <div className="feature-form-grid">
          <Form.Item name="gender" label="Giới tính (tùy chọn)">
            <Select
              allowClear
              placeholder="Chưa cung cấp"
              options={[
                { value: 'MALE', label: 'Nam' },
                { value: 'FEMALE', label: 'Nữ' },
                { value: 'OTHER', label: 'Khác' },
              ]}
            />
          </Form.Item>
          <Form.Item name="dob" label="Ngày sinh (tùy chọn)">
            <Input type="date" autoComplete="bday" />
          </Form.Item>
        </div>
        {member && (
          <Form.Item name="status" label="Trạng thái" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ACTIVE', label: 'Đang hoạt động' },
                { value: 'INACTIVE', label: 'Ngừng hoạt động' },
              ]}
            />
          </Form.Item>
        )}
        {member && status === 'INACTIVE' && (
          <Alert
            type="warning"
            showIcon
            title="Ngừng hoạt động hội viên sẽ đồng thời vô hiệu hóa tài khoản liên kết."
            style={{ marginBottom: 16 }}
          />
        )}
        <div className="feature-form-actions">
          <Button disabled={saving} onClick={onClose}>
            Hủy
          </Button>
          <Button type="primary" htmlType="submit" loading={saving}>
            Lưu hồ sơ
          </Button>
        </div>
      </Form>
    </Modal>
  )
}
