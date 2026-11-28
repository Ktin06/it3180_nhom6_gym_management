import { Alert, Button, Form, Input, Modal, Select } from 'antd'
import axios from 'axios'
import { useState } from 'react'
import { errorMessage } from '../../../services/api-error'
import { packageTypeLabels } from '../package-format'
import type {
  GymPackage,
  PackageCreateInput,
  PackagesGateway,
  PackageType,
  PackageUpdateInput,
} from '../types/package'

const MAX_JAVA_INT = 2_147_483_647

const VALIDATED_FIELDS = [
  'code',
  'name',
  'type',
  'durationDays',
  'sessionCount',
  'price',
  'description',
] as const

interface FormValues {
  code: string
  name: string
  type: PackageType
  durationDays: string
  sessionCount?: string
  price: string
  description?: string
}

function parseIntegerInput(raw: string | undefined): number | null {
  const trimmed = raw?.trim() ?? ''
  if (!/^-?\d+$/.test(trimmed)) return null
  const parsed = Number(trimmed)
  return Number.isSafeInteger(parsed) && parsed <= MAX_JAVA_INT ? parsed : null
}

function integerRule(min: number, message: string) {
  return {
    validator: (_: unknown, value: string | undefined) => {
      if (!value?.trim()) return Promise.resolve()
      const parsed = parseIntegerInput(value)
      return parsed !== null && parsed >= min
        ? Promise.resolve()
        : Promise.reject(new Error(message))
    },
  }
}

/** Modal thêm mới hoặc sửa thông tin gói tập cho quản lý chi nhánh (UC-F3.2, UC-F3.3). */
export function PackageFormModal({
  pkg,
  api,
  onClose,
  onSaved,
}: {
  pkg: GymPackage | null
  api: PackagesGateway
  onClose: () => void
  onSaved: () => void
}) {
  const [form] = Form.useForm<FormValues>()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string>()
  const selectedType = Form.useWatch('type', form) ?? pkg?.type ?? 'TIME'
  const needsSessions = selectedType !== 'TIME'

  async function save(values: FormValues) {
    setSaving(true)
    setError(undefined)
    const effectiveType = pkg ? pkg.type : values.type
    const durationDays = parseIntegerInput(values.durationDays)!
    const price = parseIntegerInput(values.price)!
    const description = values.description?.trim() || null
    const sessionCount =
      effectiveType === 'TIME' ? undefined : parseIntegerInput(values.sessionCount)!

    try {
      if (pkg) {
        // Không gửi code và type khi sửa vì backend từ chối trường bất biến (BR-F3-05).
        const payload: PackageUpdateInput = {
          name: values.name.trim(),
          durationDays,
          ...(sessionCount !== undefined ? { sessionCount } : {}),
          price,
          description,
        }
        await api.update(pkg.id, payload)
      } else {
        const payload: PackageCreateInput = {
          code: values.code.trim(),
          name: values.name.trim(),
          type: effectiveType,
          durationDays,
          ...(sessionCount !== undefined ? { sessionCount } : {}),
          price,
          description,
        }
        await api.create(payload)
      }
      onSaved()
    } catch (cause) {
      setError(errorMessage(cause))
      if (axios.isAxiosError(cause) && cause.response?.status === 400) {
        const errors: unknown = cause.response.data?.errors
        if (errors && typeof errors === 'object') {
          const fields = Object.entries(errors).flatMap(([name, value]) => {
            const field = VALIDATED_FIELDS.find((candidate) => candidate === name)
            return field && typeof value === 'string' ? [{ name: field, errors: [value] }] : []
          })
          if (fields.length > 0) form.setFields(fields)
        }
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      open
      title={pkg ? `Sửa gói tập ${pkg.code}` : 'Thêm gói tập'}
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
          pkg
            ? {
                code: pkg.code,
                name: pkg.name,
                type: pkg.type,
                durationDays: String(pkg.durationDays),
                sessionCount: pkg.sessionCount !== null ? String(pkg.sessionCount) : undefined,
                price: String(pkg.price),
                description: pkg.description ?? '',
              }
            : { type: 'TIME' }
        }
      >
        {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
        <div className="feature-form-grid">
          <Form.Item
            name="code"
            label="Mã gói"
            rules={
              pkg
                ? undefined
                : [
                    { required: true, whitespace: true, message: 'Vui lòng nhập mã gói.' },
                    {
                      validator: (_, value: string | undefined) =>
                        !value || value.trim().length <= 30
                          ? Promise.resolve()
                          : Promise.reject(new Error('Mã gói tối đa 30 ký tự.')),
                    },
                  ]
            }
          >
            <Input disabled={Boolean(pkg)} placeholder="Ví dụ: TIME-1M" />
          </Form.Item>
          <Form.Item
            name="type"
            label="Loại gói"
            rules={pkg ? undefined : [{ required: true, message: 'Vui lòng chọn loại gói.' }]}
          >
            <Select<PackageType>
              disabled={Boolean(pkg)}
              options={Object.entries(packageTypeLabels).map(([value, label]) => ({
                value: value as PackageType,
                label,
              }))}
              onChange={(nextType) => {
                if (nextType === 'TIME') form.setFieldValue('sessionCount', undefined)
              }}
            />
          </Form.Item>
        </div>
        <Form.Item
          name="name"
          label="Tên gói"
          rules={[
            { required: true, whitespace: true, message: 'Vui lòng nhập tên gói.' },
            {
              validator: (_, value: string | undefined) =>
                !value || value.trim().length <= 255
                  ? Promise.resolve()
                  : Promise.reject(new Error('Tên gói tối đa 255 ký tự.')),
            },
          ]}
        >
          <Input placeholder="Ví dụ: Gói 1 tháng" />
        </Form.Item>
        <div className="feature-form-grid">
          <Form.Item
            name="durationDays"
            label="Thời hạn (ngày)"
            extra="1 tháng tính 30 ngày."
            rules={[
              { required: true, whitespace: true, message: 'Vui lòng nhập thời hạn.' },
              integerRule(1, 'Thời hạn phải là số nguyên lớn hơn 0.'),
            ]}
          >
            <Input inputMode="numeric" placeholder="Ví dụ: 30" />
          </Form.Item>
          {needsSessions && (
            <Form.Item
              name="sessionCount"
              label="Số buổi"
              rules={[
                { required: true, whitespace: true, message: 'Vui lòng nhập số buổi.' },
                integerRule(1, 'Số buổi phải là số nguyên lớn hơn 0.'),
              ]}
            >
              <Input inputMode="numeric" placeholder="Ví dụ: 10" />
            </Form.Item>
          )}
          <Form.Item
            name="price"
            label="Giá bán (VND)"
            rules={[
              { required: true, whitespace: true, message: 'Vui lòng nhập giá bán.' },
              integerRule(0, 'Giá bán phải là số nguyên từ 0 trở lên.'),
            ]}
          >
            <Input inputMode="numeric" placeholder="Ví dụ: 500000" />
          </Form.Item>
        </div>
        <Form.Item name="description" label="Mô tả (tùy chọn)">
          <Input.TextArea rows={3} placeholder="Quyền lợi hoặc ghi chú của gói tập" />
        </Form.Item>
        <div className="feature-form-actions">
          <Button disabled={saving} onClick={onClose} style={{ minHeight: 40 }}>
            Hủy
          </Button>
          <Button type="primary" htmlType="submit" loading={saving} style={{ minHeight: 40 }}>
            Lưu gói
          </Button>
        </div>
      </Form>
    </Modal>
  )
}
