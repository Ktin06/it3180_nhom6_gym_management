/** Access token tồn tại trong bộ nhớ tab hiện tại, không ghi vào Web Storage. */
let accessToken: string | null = null
let sessionSubject: string | null = null
let sessionEpoch = 0
const expiredListeners = new Set<(message?: string) => void>()
const recoveryListeners = new Set<() => void>()

export function getAccessToken() {
  return accessToken
}

export function getSessionSubject() {
  return sessionSubject
}

export function getSessionEpoch() {
  return sessionEpoch
}

/** Bắt đầu một thao tác đăng nhập mới, làm mọi response của phiên trước thành stale. */
export function beginSession() {
  sessionEpoch += 1
  accessToken = null
  sessionSubject = null
  return sessionEpoch
}

export function setAccessToken(token: string, expectedEpoch: number) {
  if (sessionEpoch !== expectedEpoch) return false
  const subject = tokenSubject(token)
  if (sessionSubject !== null && subject !== sessionSubject) {
    expireSession(expectedEpoch, 'Tài khoản đã đổi ở tab khác. Vui lòng đăng nhập lại.')
    return false
  }
  sessionSubject = subject
  accessToken = token
  return true
}

/** Chỉ dùng sub để giữ nhất quán giao diện; backend vẫn xác minh chữ ký và phân quyền. */
export function tokenSubject(token: string): string | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const claims: unknown = JSON.parse(atob(payload.padEnd(Math.ceil(payload.length / 4) * 4, '=')))
    if (typeof claims !== 'object' || claims === null || !('sub' in claims)) return null
    return typeof claims.sub === 'string' && /^[1-9][0-9]*$/.test(claims.sub) ? claims.sub : null
  } catch {
    return null
  }
}

/** Xóa phiên ngay trên trình duyệt; token cũ không thể làm mới hoặc ghi đè phiên mới. */
export function clearSession(expectedEpoch?: number) {
  if (expectedEpoch !== undefined && sessionEpoch !== expectedEpoch) return false
  sessionEpoch += 1
  accessToken = null
  sessionSubject = null
  return true
}

export function expireSession(expectedEpoch: number, message?: string) {
  if (!clearSession(expectedEpoch)) return false
  expiredListeners.forEach((listener) => listener(message))
  return true
}

/** Xóa access token sau lỗi refresh tạm thời; cookie vẫn được giữ để người dùng thử lại. */
export function suspendSession(expectedEpoch: number) {
  const subject = sessionSubject
  if (!clearSession(expectedEpoch)) return false
  sessionSubject = subject
  recoveryListeners.forEach((listener) => listener())
  return true
}

export function subscribeToSessionExpiry(listener: (message?: string) => void) {
  expiredListeners.add(listener)
  return () => {
    expiredListeners.delete(listener)
  }
}

export function subscribeToSessionRecovery(listener: () => void) {
  recoveryListeners.add(listener)
  return () => {
    recoveryListeners.delete(listener)
  }
}
