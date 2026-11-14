import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach } from 'vitest'
import { createTestLockManager } from './web-locks'

beforeEach(() => {
  Object.defineProperty(navigator, 'locks', { configurable: true, value: createTestLockManager() })
})

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
