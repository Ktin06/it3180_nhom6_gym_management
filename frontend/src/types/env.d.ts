/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Địa chỉ gốc của API; bỏ trống thì dùng /api/v1 cùng domain (ADR 0008). */
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
