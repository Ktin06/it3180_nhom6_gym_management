import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

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
