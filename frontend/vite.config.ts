import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Đọc file .env ở thư mục gốc repo (dùng chung với backend và Docker Compose).
  // Trên Vercel, biến VITE_* lấy từ cấu hình môi trường của dự án.
  envDir: '..',
  // Gọi API cùng domain (ADR 0008): khi chạy dev, chuyển tiếp /api sang backend trên máy.
  // Khi deploy, frontend/vercel.json làm việc này.
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
