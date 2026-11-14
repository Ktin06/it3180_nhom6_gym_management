import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AuthCompletionError, AuthContext, type AuthStatus, type AuthUser } from './AuthContext'
import {
  beginSession,
  expireSession,
  getAccessToken,
  getSessionEpoch,
  setAccessToken,
  subscribeToSessionExpiry,
  subscribeToSessionRecovery,
} from './session-store'
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
} from '../features/auth/api/auth-api'
import { refreshAccessToken } from '../services/http'
import type {
  AccessTokenResponse,
  LoginRequest,
  RegisterRequest,
} from '../features/auth/types/auth'

type BootstrapResult = AuthUser | null | 'recoverable-error'
const RECOVERABLE_SESSION_MESSAGE =
  'Chưa thể xác minh phiên đăng nhập. Hãy kiểm tra kết nối rồi thử lại.'

let bootstrapFlight: { epoch: number; promise: Promise<BootstrapResult> } | null = null

function isUnauthorized(error: unknown) {
  return (
    typeof error === 'object' &&
    error !== null &&
    'response' in error &&
    typeof error.response === 'object' &&
    error.response !== null &&
    'status' in error.response &&
    error.response.status === 401
  )
}

function bootstrapSession(epoch: number): Promise<BootstrapResult> {
  if (bootstrapFlight?.epoch === epoch) return bootstrapFlight.promise

  const flight = (async (): Promise<BootstrapResult> => {
    try {
      await refreshAccessToken(epoch)
      if (getSessionEpoch() !== epoch) return null
      const user = await getCurrentUser()
      return getSessionEpoch() === epoch ? user : null
    } catch (error) {
      if (getSessionEpoch() !== epoch) return null
      if (isUnauthorized(error)) {
        expireSession(epoch)
        return null
      }
      return 'recoverable-error'
    }
  })()
  const promise = flight.finally(() => {
    if (bootstrapFlight?.promise === promise) bootstrapFlight = null
  })
  bootstrapFlight = { epoch, promise }
  return promise
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [status, setStatus] = useState<AuthStatus>('initializing')
  const [sessionError, setSessionError] = useState<string | null>(null)

  useEffect(() => {
    const unsubscribeExpiry = subscribeToSessionExpiry(() => {
      setUser(null)
      setStatus('anonymous')
      setSessionError(null)
    })
    const unsubscribeRecovery = subscribeToSessionRecovery(() => {
      setUser(null)
      setStatus('recoverable-error')
      setSessionError(RECOVERABLE_SESSION_MESSAGE)
    })
    return () => {
      unsubscribeExpiry()
      unsubscribeRecovery()
    }
  }, [])

  useEffect(() => {
    let active = true
    const epoch = getSessionEpoch()
    void bootstrapSession(epoch).then((result) => {
      if (!active || getSessionEpoch() !== epoch) return
      if (result === 'recoverable-error') {
        setUser(null)
        setStatus('recoverable-error')
        setSessionError(RECOVERABLE_SESSION_MESSAGE)
        return
      }
      setUser(result)
      setStatus(result ? 'authenticated' : 'anonymous')
      setSessionError(null)
    })
    return () => {
      active = false
    }
  }, [])

  const retrySession = useCallback(async () => {
    const epoch = getSessionEpoch()
    setUser(null)
    setStatus('initializing')
    setSessionError(null)
    try {
      if (!getAccessToken()) await refreshAccessToken(epoch)
      if (getSessionEpoch() !== epoch) throw new Error('Phiên đăng nhập đã thay đổi.')
      const account = await getCurrentUser()
      if (getSessionEpoch() !== epoch) throw new Error('Phiên đăng nhập đã thay đổi.')
      setUser(account)
      setStatus('authenticated')
      setSessionError(null)
      return account
    } catch (error) {
      if (getSessionEpoch() === epoch) {
        if (isUnauthorized(error)) {
          expireSession(epoch)
        } else {
          setUser(null)
          setStatus('recoverable-error')
          setSessionError(RECOVERABLE_SESSION_MESSAGE)
        }
      }
      throw error
    }
  }, [])

  const establishSession = useCallback(
    async (
      request: () => Promise<AccessTokenResponse>,
      action: 'login' | 'register',
    ) => {
      const epoch = beginSession()
      setUser(null)
      setStatus('initializing')
      setSessionError(null)

      let response: AccessTokenResponse
      try {
        response = await request()
      } catch (error) {
        if (getSessionEpoch() === epoch) expireSession(epoch)
        throw error
      }

      if (typeof response.accessToken !== 'string' || !response.accessToken) {
        const error = new Error('Máy chủ không trả access token hợp lệ.')
        if (getSessionEpoch() === epoch) {
          setStatus('recoverable-error')
          setSessionError('Yêu cầu đã được tiếp nhận nhưng chưa tải được thông tin tài khoản.')
        }
        throw new AuthCompletionError(action, error)
      }
      if (!setAccessToken(response.accessToken, epoch)) {
        throw new Error('Phiên đăng nhập đã thay đổi.')
      }

      try {
        const account = await getCurrentUser()
        if (getSessionEpoch() !== epoch) throw new Error('Phiên đăng nhập đã thay đổi.')
        setUser(account)
        setStatus('authenticated')
        setSessionError(null)
        return account
      } catch (error) {
        if (getSessionEpoch() === epoch) {
          setUser(null)
          setStatus('recoverable-error')
          setSessionError('Chưa tải được thông tin tài khoản. Hãy thử lại hoặc đăng nhập sau.')
        }
        throw new AuthCompletionError(action, error)
      }
    },
    [],
  )

  const login = useCallback(
    async (request: LoginRequest) => {
      return establishSession(() => loginRequest(request), 'login')
    },
    [establishSession],
  )

  const register = useCallback(
    async (request: RegisterRequest) => {
      return establishSession(() => registerRequest(request), 'register')
    },
    [establishSession],
  )

  const logout = useCallback(async () => {
    const epoch = getSessionEpoch()
    await logoutRequest()
    if (getSessionEpoch() === epoch) expireSession(epoch)
  }, [])

  const value = useMemo(
    () => ({ status, user, sessionError, login, register, logout, retrySession }),
    [status, user, sessionError, login, register, logout, retrySession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
