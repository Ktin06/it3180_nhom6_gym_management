import 'axios'

declare module 'axios' {
  interface AxiosRequestConfig {
    skipBearer?: boolean
    skipAuthRefresh?: boolean
    authEpoch?: number
    authSentToken?: string
    authRetry?: boolean
  }
}
