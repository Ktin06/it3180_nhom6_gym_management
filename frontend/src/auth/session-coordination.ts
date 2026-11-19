import {
  expireSession,
  getAccessToken,
  getSessionEpoch,
  getSessionSubject,
  tokenSubject,
} from './session-store'

const REVISION_KEY = 'gym-auth-cookie-revision'
const CHANNEL_NAME = 'gym-auth-session'
const TOKEN_WAIT_MS = 200
const TOKEN_REUSE_MS = 5_000
const ACCOUNT_CHANGED = 'Tài khoản đã đổi ở tab khác. Vui lòng đăng nhập lại.'

type Revision = { session: string | null; refresh: string | null }
type RefreshMessage = { type: 'refreshed'; refresh: string; token: string; issuedAt: number }
type SessionMessage = { type: 'session'; session: string; subject: string | null }
type SyncMessage = RefreshMessage | SessionMessage | { type: 'request'; refresh: string }

let channel: BroadcastChannel | undefined
let tabSession: Revision['session'] | undefined
let lastRefresh: RefreshMessage | undefined
let receiveRefresh: ((result: RefreshMessage) => void) | undefined

function readRevision(): Revision {
  const stored = localStorage.getItem(REVISION_KEY)
  return stored ? JSON.parse(stored) : { session: null, refresh: null }
}

function writeRevision(revision: Revision) {
  // Chỉ ghi mã ngẫu nhiên. Token và chủ thể không vào Web Storage.
  localStorage.setItem(REVISION_KEY, JSON.stringify(revision))
}

function freshResult(refresh: string) {
  if (lastRefresh?.refresh === refresh && Date.now() - lastRefresh.issuedAt < TOKEN_REUSE_MS) {
    return lastRefresh
  }
}

function applySessionChange(message: SessionMessage) {
  if (message.session !== readRevision().session) return false
  if (tabSession === message.session) return true

  lastRefresh = undefined
  if (tabSession === undefined || (message.subject && message.subject === getSessionSubject())) {
    tabSession = message.session
  } else if (getSessionSubject() || getAccessToken()) {
    expireSession(getSessionEpoch(), message.subject ? ACCOUNT_CHANGED : undefined)
  }
  return true
}

function receiveSessionChange(message: SessionMessage) {
  if (applySessionChange(message)) return

  // BroadcastChannel và sự kiện storage có thể đến theo hai thứ tự khác nhau.
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), TOKEN_REUSE_MS)
  window.addEventListener(
    'storage',
    (event) => {
      if (event.key !== REVISION_KEY || !applySessionChange(message)) return
      clearTimeout(timeout)
      controller.abort()
    },
    { signal: controller.signal },
  )
}

function getChannel() {
  if (!channel) {
    channel = new BroadcastChannel(CHANNEL_NAME)
    channel.onmessage = ({ data }: MessageEvent<SyncMessage>) => {
      if (!data || typeof data !== 'object') return
      if (data.type === 'session') {
        receiveSessionChange(data)
        return
      }
      const current = readRevision()
      if (
        data.type === 'refreshed' &&
        tabSession === current.session &&
        data.refresh === current.refresh &&
        typeof data.token === 'string' &&
        data.token
      ) {
        lastRefresh = data
        if (freshResult(data.refresh)) receiveRefresh?.(data)
      } else if (data.type === 'request' && data.refresh === current.refresh) {
        const cached = freshResult(data.refresh)
        if (cached) channel?.postMessage(cached)
      }
    }
  }
  return channel
}

export function captureCookieRevision() {
  getChannel()
  const revision = readRevision()
  if (tabSession === undefined) tabSession = revision.session
  return revision
}

/** Kiểm tra quyền sở hữu cookie của tab sau khi lấy Web Lock. */
export function currentTabCookieRevision(epoch: number) {
  if (getSessionEpoch() !== epoch) return null

  const revision = readRevision()
  if (tabSession !== revision.session) {
    expireSession(epoch, ACCOUNT_CHANGED)
    return null
  }
  return revision
}

/** Ghi dấu đổi phiên trước khi nhả Web Lock để tab chờ nhìn thấy ngay. */
export function publishSessionChange(token: string | null) {
  const session = crypto.randomUUID()
  writeRevision({ session, refresh: null })
  tabSession = session
  lastRefresh = undefined
  const message: SessionMessage = {
    type: 'session',
    session,
    subject: token ? tokenSubject(token) : null,
  }
  getChannel().postMessage(message)
}

export function publishRefresh(revision: Revision, token: string) {
  const refresh = crypto.randomUUID()
  writeRevision({ session: revision.session, refresh })
  lastRefresh = { type: 'refreshed', refresh, token, issuedAt: Date.now() }
  getChannel().postMessage(lastRefresh)
}

export async function reuseConcurrentRefresh(before: Revision, current: Revision) {
  const refresh = current.refresh
  if (!refresh || refresh === before.refresh || current.session !== before.session) return null

  const cached = freshResult(refresh)
  if (cached) return cached.token

  // Việc cấp Web Lock và nhận broadcast nằm ở các hàng đợi khác nhau.
  // Hỏi theo mã lần refresh; nếu tab kia đã đóng, cookie mới vẫn được dùng an toàn.
  return new Promise<string | null>((resolve) => {
    const timeout = setTimeout(() => {
      receiveRefresh = undefined
      resolve(null)
    }, TOKEN_WAIT_MS)
    receiveRefresh = (result) => {
      if (result.refresh !== refresh) return
      clearTimeout(timeout)
      receiveRefresh = undefined
      resolve(result.token)
    }
    const message: SyncMessage = { type: 'request', refresh }
    getChannel().postMessage(message)
  })
}
