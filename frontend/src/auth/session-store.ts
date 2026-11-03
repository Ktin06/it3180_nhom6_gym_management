/** Access token tồn tại trong bộ nhớ tab hiện tại, không ghi vào Web Storage. */
let accessToken: string | null = null
let sessionEpoch = 0
const expiredListeners = new Set<() => void>()

export function getAccessToken() {
  return accessToken
}

export function getSessionEpoch() {
  return sessionEpoch
}

/** Bắt đầu một thao tác đăng nhập mới, làm mọi response của phiên trước thành stale. */
export function beginSession() {
  sessionEpoch += 1
  accessToken = null
  return sessionEpoch
}

export function setAccessToken(token: string, expectedEpoch: number) {
  if (sessionEpoch !== expectedEpoch) return false
  accessToken = token
  return true
}

/** Xóa phiên ngay trên trình duyệt; token cũ không thể làm mới hoặc ghi đè phiên mới. */
export function clearSession(expectedEpoch?: number) {
  if (expectedEpoch !== undefined && sessionEpoch !== expectedEpoch) return false
  sessionEpoch += 1
  accessToken = null
  return true
}

export function expireSession(expectedEpoch: number) {
  if (!clearSession(expectedEpoch)) return false
  expiredListeners.forEach((listener) => listener())
  return true
}

export function subscribeToSessionExpiry(listener: () => void) {
  expiredListeners.add(listener)
  return () => {
    expiredListeners.delete(listener)
  }
}
