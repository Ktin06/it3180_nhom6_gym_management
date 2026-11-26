import { Avatar } from 'antd'
import { useEffect, useState } from 'react'
import { membersApi } from '../api/members'

export interface MemberAvatarProps {
  /** Mã định danh (ID) của hội viên để tải ảnh qua GET /api/v1/members/{id}/avatar. */
  memberId: number
  /** Họ và tên hội viên, dùng để lấy chữ cái đầu khi chưa có ảnh và làm thuộc tính alt. */
  fullName: string
  /**
   * Gợi ý hội viên có ảnh hay không:
   * - `false`: không gọi API, hiển thị chữ cái đầu ngay;
   * - `true` hoặc `undefined`: gọi API tải Blob ảnh và rơi về chữ cái đầu khi 404 hoặc lỗi.
   */
  hasAvatar?: boolean
  /** Đường kính ảnh đại diện (px), mặc định 64. */
  size?: number
  /** Tăng giá trị sau khi tải ảnh mới lên để buộc tải lại Blob từ máy chủ. */
  reloadKey?: number
  /** Hàm tải Blob ảnh tùy chỉnh; mặc định gọi qua services/http.ts (`membersApi.getAvatar`). */
  loadAvatar?: (memberId: number) => Promise<Blob>
}

const defaultLoadAvatar = (memberId: number) => membersApi.getAvatar(memberId)

function nameInitial(fullName: string): string {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  const lastWord = parts[parts.length - 1]
  if (!lastWord) return '?'
  return Array.from(lastWord)[0].toLocaleUpperCase('vi-VN')
}

/**
 * Component ảnh đại diện hội viên dùng chung cho F2 (form hồ sơ) và F4 (check-in tại quầy).
 * Không gắn thẳng `<img src="/api/...">` (ADR 0011) vì cần gửi kèm Bearer token qua services/http.ts.
 */
export function MemberAvatar({
  memberId,
  fullName,
  hasAvatar,
  size = 64,
  reloadKey = 0,
  loadAvatar = defaultLoadAvatar,
}: MemberAvatarProps) {
  const requestKey = `${memberId}:${reloadKey}`
  const [loaded, setLoaded] = useState<{ key: string; url: string } | null>(null)

  useEffect(() => {
    if (hasAvatar === false) {
      return
    }
    let active = true
    let objectUrl: string | null = null

    void loadAvatar(memberId)
      .then((blob) => {
        if (!active || !(blob instanceof Blob) || blob.size === 0) return
        objectUrl = URL.createObjectURL(blob)
        setLoaded({ key: requestKey, url: objectUrl })
      })
      .catch(() => {
        // 404 hoặc lỗi tải ảnh: giữ trạng thái rỗng để hiển thị chữ cái đầu của tên.
      })

    return () => {
      active = false
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [hasAvatar, loadAvatar, memberId, requestKey])

  const avatarUrl = hasAvatar === false ? null : loaded?.key === requestKey ? loaded.url : null
  const initial = nameInitial(fullName)

  return (
    <Avatar
      size={size}
      src={avatarUrl ?? undefined}
      alt={`Ảnh đại diện ${fullName}`}
      style={{
        backgroundColor: avatarUrl ? undefined : '#e8ecf2',
        color: '#344054',
        fontWeight: 600,
        fontSize: Math.max(14, Math.round(size * 0.4)),
        flexShrink: 0,
      }}
    >
      {initial}
    </Avatar>
  )
}
