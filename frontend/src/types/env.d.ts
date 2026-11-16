/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Địa chỉ gốc của API; bỏ trống thì dùng /api/v1 cùng domain (ADR 0008). */
  readonly VITE_API_URL?: string
  /** Chỉ có hiệu lực trong dev; dùng dữ liệu minh họa cho F2/F3. */
  readonly VITE_F2_F3_MOCK?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
