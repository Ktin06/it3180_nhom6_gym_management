import { Alert, Button, Form, Input, InputNumber, Modal, Select } from 'antd'
import { useEffect, useState } from 'react'
import { errorMessage } from '../../shared/api-error'
import type { CreatePackageInput, GymPackage, PackageType, PackagesGateway, UpdatePackageInput } from '../types/package'
import { packageTypeLabels } from '../package-format'

type Values = Omit<CreatePackageInput, 'sessionCount' | 'description'> & { sessionCount?: number; description?: string }

export function PackageFormModal({ item, api, onClose, onSaved }: {
  item: GymPackage | null
  api: PackagesGateway
  onClose: () => void
  onSaved: () => void
}) {
  const [form] = Form.useForm<Values>()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string>()
  const type = Form.useWatch('type', form) ?? item?.type ?? 'TIME'
  useEffect(() => {
    if (item) form.setFieldsValue({
      code: item.code, name: item.name, type: item.type, durationDays: item.durationDays,
      sessionCount: item.sessionCount ?? undefined, price: item.price,
      description: item.description ?? undefined,
    })
    else form.setFieldsValue({ type: 'TIME', durationDays: 30, price: 0 })
  }, [form, item])

  async function save(values: Values) {
    setSaving(true)
    setError(undefined)
    const sessionCount = type === 'TIME' ? null : values.sessionCount!
    try {
      if (item) {
        const input: UpdatePackageInput = {
          name: values.name.trim(), durationDays: values.durationDays,
          ...(item.type === 'TIME' ? {} : { sessionCount }), price: values.price,
          description: values.description?.trim() || null,
        }
        await api.update(item.id, input)
      } else {
        const input: CreatePackageInput = {
          code: values.code.trim(), name: values.name.trim(), type: values.type,
          durationDays: values.durationDays, sessionCount, price: values.price,
          description: values.description?.trim() || null,
        }
        await api.create(input)
      }
      onSaved()
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSaving(false)
    }
  }

  return <Modal open title={item ? `Sửa gói ${item.code}` : 'Thêm gói tập'} onCancel={saving ? undefined : onClose} footer={null} destroyOnHidden>
    <Form form={form} layout="vertical" onFinish={save} disabled={saving}>
      {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
      {!item && <Form.Item name="code" label="Mã gói" rules={[{ required: true, whitespace: true, message: 'Nhập mã gói.' }, { max: 30, message: 'Mã gói tối đa 30 ký tự.' }]}><Input maxLength={30} /></Form.Item>}
      <Form.Item name="name" label="Tên gói" rules={[{ required: true, whitespace: true, message: 'Nhập tên gói.' }, { max: 255, message: 'Tên gói tối đa 255 ký tự.' }]}><Input maxLength={255} /></Form.Item>
      {!item && <Form.Item name="type" label="Loại gói" rules={[{ required: true }]}><Select options={Object.entries(packageTypeLabels).map(([value, label]) => ({ value: value as PackageType, label }))} /></Form.Item>}
      <Form.Item name="durationDays" label="Thời hạn (ngày)" rules={[{ required: true, message: 'Nhập thời hạn.' }, { type: 'number', min: 1, message: 'Thời hạn phải lớn hơn 0.' }]}><InputNumber min={1} precision={0} style={{ width: '100%' }} /></Form.Item>
      {type !== 'TIME' && <Form.Item name="sessionCount" label="Số buổi" rules={[{ required: true, message: 'Nhập số buổi.' }, { type: 'number', min: 1, message: 'Số buổi phải lớn hơn 0.' }]}><InputNumber min={1} precision={0} style={{ width: '100%' }} /></Form.Item>}
      <Form.Item name="price" label="Giá bán (VND)" rules={[{ required: true, message: 'Nhập giá bán.' }, { type: 'number', min: 0, message: 'Giá không được âm.' }]}><InputNumber min={0} precision={0} style={{ width: '100%' }} /></Form.Item>
      <Form.Item name="description" label="Mô tả"><Input.TextArea rows={3} /></Form.Item>
      <div className="feature-form-actions"><Button onClick={onClose} disabled={saving}>Hủy</Button><Button type="primary" htmlType="submit" loading={saving}>Lưu gói</Button></div>
    </Form>
  </Modal>
}
