import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'
import type { CookieRevision } from '../auth/session-revision'
import { createTestLockManager } from './web-locks'

// jsdom không có IndexedDB. Chromium E2E kiểm tra giao dịch thật của trình duyệt.
vi.mock('../auth/session-revision', () => {
  let revision: CookieRevision = { session: null, refresh: null }
  return {
    readCookieRevision: () => Promise.resolve({ ...revision }),
    writeCookieRevision: async (next: CookieRevision) => {
      revision = { ...next }
    },
  }
})

beforeEach(() => {
  Object.defineProperty(navigator, 'locks', { configurable: true, value: createTestLockManager() })
})

// jsdom chưa hỗ trợ style của pseudo-element; kiểm thử DOM không đo kích thước scrollbar.
const getComputedStyle = window.getComputedStyle.bind(window)
window.getComputedStyle = (element) => getComputedStyle(element)

afterEach(() => {
  cleanup()
})

// jsdom không có matchMedia; Ant Design (Grid, Layout.Sider) cần hàm này.
if (!window.matchMedia) {
  window.matchMedia = (query: string): MediaQueryList => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  })
}

// jsdom không tính bố cục; Table/Select cần API này để đăng ký theo dõi kích thước.
if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}
