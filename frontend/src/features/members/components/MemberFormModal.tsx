import { UploadOutlined, UserAddOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input, Modal, Select } from 'antd'
import { type ChangeEvent, useCallback, useContext, useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { AuthContext } from '../../../auth/AuthContext'
import { canManageMembers } from '../../../auth/roles'
import { errorMessage } from '../../shared/api-error'
import type { Gender, MemberDetail, MembersGateway } from '../types/member'
import { normalizePhone } from '../phone'
import { MemberAvatar } from './MemberAvatar'

const MAX_AVATAR_BYTES = 5 * 1024 * 1024
const ALLOWED_AVATAR_TYPES = new Set(['image/jpeg', 'image/jpg', 'image/png'])
const ALLOWED_AVATAR_EXTENSIONS = /\.(jpe?g|png)$/i

function validateAvatarFile(file: File): string | null {
  const mime = file.type.toLowerCase()
  const validType = mime
    ? ALLOWED_AVATAR_TYPES.has(mime)
    : ALLOWED_AVATAR_EXTENSIONS.test(file.name)
  if (!validType) {
    return 'Chỉ chấp nhận ảnh định dạng JPEG hoặc PNG.'
  }
  if (file.size === 0) {
    return 'File ảnh rỗng. Vui lòng chọn ảnh JPEG hoặc PNG hợp lệ.'
  }
  if (file.size > MAX_AVATAR_BYTES) {
    return 'Dung lượng ảnh không được vượt quá 5 MB.'
  }
  return null
}

function avatarErrorMessage(cause: unknown): string {
  if (axios.isAxiosError(cause) && cause.response?.status === 400) {
    const data = cause.response.data as
      { detail?: unknown; errors?: Record<string, unknown> } | undefined
    const fileError = data?.errors?.file
    if (typeof fileError === 'string' && fileError.trim()) {
      return fileError
    }
    if (typeof data?.detail === 'string' && data.detail.trim()) {
      return data.detail
    }
  }
  return errorMessage(cause)
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validateAccountEmail(rawEmail: string): string | null {
  const email = rawEmail.trim()
  if (!email) {
    return 'Vui lòng nhập email.'
  }
  if (email.length > 255) {
    return 'Email không được vượt quá 255 ký tự.'
  }
  if (!EMAIL_PATTERN.test(email)) {
    return 'Email không đúng định dạng.'
  }
  return null
}

function accountErrorMessage(cause: unknown): string {
  if (axios.isAxiosError(cause) && cause.response?.status === 400) {
    const data = cause.response.data as
      | { detail?: unknown; errors?: Record<string, unknown> }
      | undefined
    const emailError = data?.errors?.email
    if (typeof emailError === 'string' && emailError.trim()) {
      return emailError
    }
  }
  return errorMessage(cause)
}

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
  onAccountCreated,
  initialAccountFormOpen = false,
}: {
  member: MemberDetail | null
  api: MembersGateway
  onClose: () => void
  onSaved: () => void
  onAccountCreated?: () => void
  initialAccountFormOpen?: boolean
}) {
  const canManageMember = canManageMembers(useContext(AuthContext)?.user?.role)

  const [form] = Form.useForm<Values>()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string>()
  const [accountInfo, setAccountInfo] = useState<{ userId: number | null; email: string | null }>(
    () => ({
      userId: member?.userId ?? null,
      email: member?.email ?? null,
    }),
  )
  const [accountFormOpen, setAccountFormOpen] = useState(
    () =>
      initialAccountFormOpen &&
      member !== null &&
      member.status === 'ACTIVE' &&
      member.userId === null &&
      canManageMember,
  )
  const [accountEmail, setAccountEmail] = useState('')
  const [creatingAccount, setCreatingAccount] = useState(false)
  const [accountError, setAccountError] = useState<string>()
  const [accountSuccess, setAccountSuccess] = useState<string>()
  const [uploadingAvatar, setUploadingAvatar] = useState(false)
  const [avatarError, setAvatarError] = useState<string>()
  const [avatarSuccess, setAvatarSuccess] = useState(false)
  const [avatarReloadKey, setAvatarReloadKey] = useState(0)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const mountedRef = useRef(true)
  const status = Form.useWatch('status', form) ?? member?.status
  const busy = saving || creatingAccount || uploadingAvatar
  const canCreateAccount =
    member !== null &&
    canManageMember &&
    member.status === 'ACTIVE' &&
    status === 'ACTIVE' &&
    accountInfo.userId === null

  useEffect(() => {
    mountedRef.current = true
    return () => {
      mountedRef.current = false
    }
  }, [])

  const loadAvatar = useCallback((id: number) => api.getAvatar(id), [api])

  async function handleAvatarChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file || !member || !canManageMember || busy) return

    const validationError = validateAvatarFile(file)
    if (validationError) {
      setAvatarSuccess(false)
      setAvatarError(validationError)
      return
    }

    setUploadingAvatar(true)
    setAvatarError(undefined)
    setAvatarSuccess(false)
    try {
      await api.uploadAvatar(member.id, file)
      if (!mountedRef.current) return
      setAvatarReloadKey((prev) => prev + 1)
      setAvatarSuccess(true)
    } catch (cause) {
      if (!mountedRef.current) return
      setAvatarError(avatarErrorMessage(cause))
    } finally {
      if (mountedRef.current) setUploadingAvatar(false)
    }
  }

  async function handleCreateAccount() {
    if (!canCreateAccount || busy) return
    const validationError = validateAccountEmail(accountEmail)
    if (validationError) {
      setAccountSuccess(undefined)
      setAccountError(validationError)
      return
    }

    const trimmedEmail = accountEmail.trim()
    setCreatingAccount(true)
    setAccountError(undefined)
    setAccountSuccess(undefined)
    try {
      const created = await api.createAccount(member.id, { email: trimmedEmail })
      if (!mountedRef.current) return
      setAccountInfo({ userId: created.userId, email: created.email })
      setAccountSuccess(`Đã gửi email đặt mật khẩu tới ${created.email}.`)
      setAccountFormOpen(false)
      setAccountEmail('')
      try {
        const refreshed = await api.detail(member.id)
        if (mountedRef.current) {
          setAccountInfo({ userId: refreshed.userId, email: refreshed.email })
        }
      } catch {
        // Giữ lại email và userId đã nhận từ phản hồi tạo tài khoản.
      }
      onAccountCreated?.()
    } catch (cause) {
      if (!mountedRef.current) return
      setAccountError(accountErrorMessage(cause))
    } finally {
      if (mountedRef.current) setCreatingAccount(false)
    }
  }

  async function save(values: Values) {
    if (busy) return
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
      onCancel={busy ? undefined : onClose}
      closable={!busy}
      mask={{ closable: !busy }}
      width={640}
      footer={null}
    >
      <Form
        form={form}
        layout="vertical"
        disabled={saving || uploadingAvatar}
        onFinish={save}
        initialValues={
          member
            ? { ...member, gender: member.gender ?? undefined, dob: member.dob ?? '' }
            : { status: 'ACTIVE' }
        }
      >
        {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
        {member ? (
          <div
            style={{
              marginBottom: 16,
              padding: 12,
              border: '1px solid #e8ecf2',
              borderRadius: 8,
              background: '#fafbfc',
            }}
          >
            <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 16 }}>
              <MemberAvatar
                memberId={member.id}
                fullName={member.fullName}
                size={72}
                hasAvatar={avatarReloadKey > 0 || member.hasAvatar}
                reloadKey={avatarReloadKey}
                loadAvatar={loadAvatar}
              />
              <div style={{ flex: 1, minWidth: 180 }}>
                <div style={{ fontWeight: 500, marginBottom: 6 }}>Ảnh đại diện</div>
                {canManageMember ? (
                  <>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/jpeg,image/png"
                      aria-label="Chọn file ảnh đại diện"
                      style={{ display: 'none' }}
                      disabled={busy}
                      onChange={(event) => void handleAvatarChange(event)}
                    />
                    <Button
                      icon={<UploadOutlined />}
                      loading={uploadingAvatar}
                      disabled={saving || creatingAccount}
                      style={{ minHeight: 40 }}
                      onClick={() => fileInputRef.current?.click()}
                    >
                      Đổi ảnh đại diện
                    </Button>
                    <div style={{ marginTop: 6, fontSize: 12, color: '#667085' }}>
                      Định dạng JPEG hoặc PNG, tối đa 5 MB.
                    </div>
                  </>
                ) : (
                  <div style={{ fontSize: 12, color: '#667085' }}>
                    Liên hệ quầy lễ tân khi cần cập nhật ảnh đại diện.
                  </div>
                )}
              </div>
            </div>
            {avatarError && (
              <Alert type="error" showIcon title={avatarError} style={{ marginTop: 12 }} />
            )}
            {avatarSuccess && (
              <Alert
                type="success"
                showIcon
                title="Đã cập nhật ảnh đại diện."
                style={{ marginTop: 12 }}
              />
            )}
          </div>
        ) : (
          <p style={{ color: '#667085', fontSize: 13 }}>
            Ảnh đại diện có thể tải lên sau khi lưu hồ sơ hội viên.
          </p>
        )}
        {member && (
          <div
            style={{
              marginBottom: 16,
              padding: 12,
              border: '1px solid #e8ecf2',
              borderRadius: 8,
              background: '#fafbfc',
            }}
          >
            <div style={{ fontWeight: 500, marginBottom: 6 }}>Tài khoản đăng nhập</div>
            {accountInfo.userId !== null ? (
              <div style={{ fontSize: 14, wordBreak: 'break-word' }}>
                Email đăng nhập: <strong>{accountInfo.email}</strong>
              </div>
            ) : (
              <>
                <div style={{ fontSize: 13, color: '#667085', marginBottom: 8 }}>
                  {member.status !== 'ACTIVE' || status !== 'ACTIVE'
                    ? 'Chưa có tài khoản. Chỉ có thể tạo tài khoản khi hồ sơ đang hoạt động.'
                    : 'Hồ sơ chưa có tài khoản đăng nhập cổng hội viên.'}
                </div>
                {canCreateAccount && !accountFormOpen && (
                  <Button
                    icon={<UserAddOutlined />}
                    disabled={busy}
                    style={{ minHeight: 40 }}
                    onClick={() => {
                      setAccountError(undefined)
                      setAccountFormOpen(true)
                    }}
                  >
                    Tạo tài khoản
                  </Button>
                )}
                {canCreateAccount && accountFormOpen && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    <label
                      htmlFor="member-account-email"
                      style={{ fontSize: 13, fontWeight: 500 }}
                    >
                      Email đăng nhập
                    </label>
                    <Input
                      id="member-account-email"
                      type="email"
                      autoComplete="email"
                      maxLength={255}
                      placeholder="hoivien@example.com"
                      value={accountEmail}
                      disabled={busy}
                      onChange={(event) => {
                        setAccountEmail(event.target.value)
                        if (accountError) setAccountError(undefined)
                      }}
                      onKeyDown={(event) => {
                        if (event.key !== 'Enter' || event.nativeEvent.isComposing) return
                        event.preventDefault()
                        if (!event.repeat) void handleCreateAccount()
                      }}
                    />
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                      <Button
                        type="primary"
                        htmlType="button"
                        loading={creatingAccount}
                        disabled={saving || uploadingAvatar}
                        style={{ minHeight: 40 }}
                        onClick={() => void handleCreateAccount()}
                      >
                        Xác nhận tạo tài khoản
                      </Button>
                      <Button
                        htmlType="button"
                        disabled={busy}
                        style={{ minHeight: 40 }}
                        onClick={() => {
                          setAccountFormOpen(false)
                          setAccountEmail('')
                          setAccountError(undefined)
                        }}
                      >
                        Đóng
                      </Button>
                    </div>
                  </div>
                )}
              </>
            )}
            {accountError && (
              <Alert type="error" showIcon title={accountError} style={{ marginTop: 12 }} />
            )}
            {accountSuccess && (
              <Alert type="success" showIcon title={accountSuccess} style={{ marginTop: 12 }} />
            )}
          </div>
        )}
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
          <Button disabled={busy} onClick={onClose}>
            Hủy
          </Button>
          <Button
            type="primary"
            htmlType="submit"
            loading={saving}
            disabled={creatingAccount || uploadingAvatar}
          >
            Lưu hồ sơ
          </Button>
        </div>
      </Form>
    </Modal>
  )
}
