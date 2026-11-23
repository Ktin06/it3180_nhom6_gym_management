import {
  expireSession,
  getAccessToken,
  getSessionEpoch,
  getSessionSubject,
  tokenSubject,
} from './session-store'

import { readCookieRevision, writeCookieRevision, type CookieRevision } from './session-revision'

const CHANNEL_NAME = 'gym-auth-session'
const TOKEN_WAIT_MS = 200
const TOKEN_REUSE_MS = 5_000
const ACCOUNT_CHANGED = 'Tài khoản đã đổi ở tab khác. Vui lòng đăng nhập lại.'

type RefreshMessage = { type: 'refreshed'; refresh: string; token: string; issuedAt: number }
type SessionMessage = { type: 'session'; session: string; subject: string | null }
type SyncMessage = RefreshMessage | SessionMessage | { type: 'request'; refresh: string }

let channel: BroadcastChannel | undefined
let tabSession: CookieRevision['session'] | undefined
let lastRefresh: RefreshMessage | undefined
let receiveRefresh: ((result: RefreshMessage) => void) | undefined

function freshResult(refresh: string) {
  if (lastRefresh?.refresh === refresh && Date.now() - lastRefresh.issuedAt < TOKEN_REUSE_MS) {
    return lastRefresh
  }
}

function applySessionChange(message: SessionMessage) {
  if (tabSession === message.session) return

  lastRefresh = undefined
  if (tabSession === undefined || (message.subject && message.subject === getSessionSubject())) {
    tabSession = message.session
  } else if (getSessionSubject() || getAccessToken()) {
    expireSession(getSessionEpoch(), message.subject ? ACCOUNT_CHANGED : undefined)
  }
}

async function syncMessage(data: SyncMessage) {
  if (!data || typeof data !== 'object') return
  const current = await readCookieRevision()
  if (data.type === 'session') {
    if (data.session === current.session) applySessionChange(data)
    return
  }
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

function getChannel() {
  if (!channel) {
    channel = new BroadcastChannel(CHANNEL_NAME)
    channel.onmessage = ({ data }: MessageEvent<SyncMessage>) => {
      void syncMessage(data).catch(() => {
        expireSession(getSessionEpoch(), 'Không thể đồng bộ phiên đăng nhập giữa các tab.')
      })
    }
  }
  return channel
}

export async function captureCookieRevision() {
  getChannel()
  const revision = await readCookieRevision()
  if (tabSession === undefined) tabSession = revision.session
  return revision
}

/** Kiểm tra quyền sở hữu cookie của tab sau khi lấy Web Lock. */
export async function currentTabCookieRevision(epoch: number) {
  const revision = await readCookieRevision()
  if (getSessionEpoch() !== epoch) return null
  if (tabSession !== revision.session) {
    expireSession(epoch, ACCOUNT_CHANGED)
    return null
  }
  return revision
}

/** Ghi dấu đổi phiên trước khi nhả Web Lock để tab chờ nhìn thấy ngay. */
export async function publishSessionChange(token: string | null) {
  const session = crypto.randomUUID()
  await writeCookieRevision({ session, refresh: null })
  tabSession = session
  lastRefresh = undefined
  const message: SessionMessage = {
    type: 'session',
    session,
    subject: token ? tokenSubject(token) : null,
  }
  getChannel().postMessage(message)
}

export async function publishRefresh(revision: CookieRevision, token: string) {
  const refresh = crypto.randomUUID()
  await writeCookieRevision({ session: revision.session, refresh })
  lastRefresh = { type: 'refreshed', refresh, token, issuedAt: Date.now() }
  getChannel().postMessage(lastRefresh)
}

export async function reuseConcurrentRefresh(before: CookieRevision, current: CookieRevision) {
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
