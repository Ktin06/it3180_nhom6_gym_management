import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AuthContext, type AuthUser } from './AuthContext'
import {
  beginSession,
  expireSession,
  getSessionEpoch,
  setAccessToken,
  subscribeToSessionExpiry,
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

export type AuthStatus = 'initializing' | 'authenticated' | 'anonymous'

let bootstrapFlight: { epoch: number; promise: Promise<AuthUser | null> } | null = null

function bootstrapSession(epoch: number): Promise<AuthUser | null> {
  if (bootstrapFlight?.epoch === epoch) return bootstrapFlight.promise

  const flight = (async () => {
    try {
      await refreshAccessToken(epoch)
      if (getSessionEpoch() !== epoch) return null
      const user = await getCurrentUser()
      return getSessionEpoch() === epoch ? user : null
    } catch {
      expireSession(epoch)
      return null
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

  useEffect(() => {
    const unsubscribe = subscribeToSessionExpiry(() => {
      setUser(null)
      setStatus('anonymous')
    })
    return unsubscribe
  }, [])

  useEffect(() => {
    let active = true
    const epoch = getSessionEpoch()
    void bootstrapSession(epoch).then((restoredUser) => {
      if (!active || getSessionEpoch() !== epoch) return
      setUser(restoredUser)
      setStatus(restoredUser ? 'authenticated' : 'anonymous')
    })
    return () => {
      active = false
    }
  }, [])

  const establishSession = useCallback(async (request: () => Promise<AccessTokenResponse>) => {
    const epoch = beginSession()
    setUser(null)
    setStatus('initializing')
    try {
      const response = await request()
      if (typeof response.accessToken !== 'string' || !response.accessToken) {
        throw new Error('Máy chủ không trả access token hợp lệ.')
      }
      if (!setAccessToken(response.accessToken, epoch))
        throw new Error('Phiên đăng nhập đã thay đổi.')
      const account = await getCurrentUser()
      if (getSessionEpoch() !== epoch) throw new Error('Phiên đăng nhập đã thay đổi.')
      setUser(account)
      setStatus('authenticated')
      return account
    } catch (error) {
      if (getSessionEpoch() === epoch) {
        expireSession(epoch)
        setUser(null)
        setStatus('anonymous')
      }
      throw error
    }
  }, [])

  const login = useCallback(
    async (request: LoginRequest) => {
      return establishSession(() => loginRequest(request))
    },
    [establishSession],
  )

  const register = useCallback(
    async (request: RegisterRequest) => {
      return establishSession(() => registerRequest(request))
    },
    [establishSession],
  )

  const logout = useCallback(async () => {
    expireSession(getSessionEpoch())
    setUser(null)
    setStatus('anonymous')
    await logoutRequest()
  }, [])

  const value = useMemo(
    () => ({ status, user, login, register, logout }),
    [status, user, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
