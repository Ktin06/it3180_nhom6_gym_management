import { AxiosError, AxiosHeaders, type AxiosAdapter, type AxiosResponse } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const requestRefresh = vi.hoisted(() => vi.fn())
vi.mock('./refresh-client', () => ({ requestRefresh }))

import {
  beginSession,
  clearSession,
  getAccessToken,
  getSessionEpoch,
  setAccessToken,
  subscribeToSessionRecovery,
} from '../auth/session-store'
import { http, refreshAccessToken } from './http'

function response(config: Parameters<AxiosAdapter>[0], data: unknown, status = 200): AxiosResponse {
  return {
    data,
    status,
    statusText: status === 200 ? 'OK' : 'Unauthorized',
    headers: new AxiosHeaders(),
    config,
  }
}

function unauthorized(config: Parameters<AxiosAdapter>[0]) {
  return new AxiosError('Unauthorized', AxiosError.ERR_BAD_REQUEST, config, undefined, {
    ...response(config, { detail: 'Chưa xác thực' }, 401),
  })
}

function refreshFailure(status: number) {
  return Object.assign(new Error(`Refresh failed (${status})`), {
    isAxiosError: true,
    response: { status },
  })
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

describe('http auth interceptors', () => {
  const originalAdapter = http.defaults.adapter

  beforeEach(() => {
    clearSession()
    const epoch = beginSession()
    setAccessToken('old-jwt', epoch)
    requestRefresh.mockReset()
  })

  afterEach(() => {
    http.defaults.adapter = originalAdapter
  })

  it('gộp 401 đồng thời thành một refresh rồi thử lại từng request một lần', async () => {
    let attempts = 0
    const pendingRefresh = deferred<{ accessToken: string }>()
    requestRefresh.mockReturnValue(pendingRefresh.promise)
    http.defaults.adapter = (async (config) => {
      attempts += 1
      if (config.headers.get('Authorization') !== 'Bearer fresh-jwt') throw unauthorized(config)
      return response(config, { ok: true })
    }) as AxiosAdapter

    const first = http.get('/private')
    const second = http.get('/private')
    await vi.waitFor(() => expect(requestRefresh).toHaveBeenCalledTimes(1))
    pendingRefresh.resolve({ accessToken: 'fresh-jwt' })

    await expect(Promise.all([first, second])).resolves.toHaveLength(2)
    expect(attempts).toBe(4)
    expect(getAccessToken()).toBe('fresh-jwt')
  })

  it('không lặp nếu request vẫn trả 401 sau lần retry duy nhất', async () => {
    let attempts = 0
    requestRefresh.mockResolvedValue({ accessToken: 'fresh-jwt' })
    http.defaults.adapter = (async (config) => {
      attempts += 1
      throw unauthorized(config)
    }) as AxiosAdapter

    await expect(http.get('/private')).rejects.toMatchObject({ response: { status: 401 } })
    expect(requestRefresh).toHaveBeenCalledTimes(1)
    expect(attempts).toBe(2)
    expect(getAccessToken()).toBeNull()
  })

  it('401 đến trễ của request cũ không refresh hay xóa phiên đăng nhập mới', async () => {
    const started = deferred<void>()
    const release = deferred<void>()
    requestRefresh.mockResolvedValue({ accessToken: 'should-not-be-used' })
    http.defaults.adapter = (async (config) => {
      started.resolve()
      await release.promise
      throw unauthorized(config)
    }) as AxiosAdapter

    const staleRequest = http.get('/private')
    await started.promise
    clearSession(getSessionEpoch())
    const newEpoch = beginSession()
    setAccessToken('new-login-jwt', newEpoch)
    release.resolve()

    await expect(staleRequest).rejects.toMatchObject({ response: { status: 401 } })
    expect(requestRefresh).not.toHaveBeenCalled()
    expect(getAccessToken()).toBe('new-login-jwt')
  })

  it('không gửi request có epoch cũ bằng bearer token của phiên mới', async () => {
    let attempts = 0
    http.defaults.adapter = (async (config) => {
      attempts += 1
      return response(config, { private: true })
    }) as AxiosAdapter
    const oldEpoch = getSessionEpoch()
    const staleRequest = http.get('/private', { authEpoch: oldEpoch })
    const newEpoch = beginSession()
    setAccessToken('new-login-jwt', newEpoch)

    await expect(staleRequest).rejects.toThrow('Phiên đăng nhập đã thay đổi.')
    expect(attempts).toBe(0)
  })

  it('bỏ response thành công đến sau khi người dùng đã logout hoặc đổi tài khoản', async () => {
    const started = deferred<void>()
    const release = deferred<void>()
    http.defaults.adapter = (async (config) => {
      started.resolve()
      await release.promise
      return response(config, { private: 'dữ liệu của phiên cũ' })
    }) as AxiosAdapter

    const staleRequest = http.get('/private')
    await started.promise
    clearSession(getSessionEpoch())
    const newEpoch = beginSession()
    setAccessToken('new-login-jwt', newEpoch)
    release.resolve()

    await expect(staleRequest).rejects.toThrow('Phiên đăng nhập đã thay đổi.')
    expect(getAccessToken()).toBe('new-login-jwt')
  })

  it('refresh 401 xác nhận phiên hết hạn và xóa access token', async () => {
    requestRefresh.mockRejectedValue(refreshFailure(401))
    http.defaults.adapter = (async (config) => {
      throw unauthorized(config)
    }) as AxiosAdapter

    await expect(http.get('/private')).rejects.toMatchObject({ response: { status: 401 } })
    expect(requestRefresh).toHaveBeenCalledTimes(1)
    expect(getAccessToken()).toBeNull()
  })

  it('refresh lỗi mạng/5xx chuyển sang recoverable và có thể refresh lại', async () => {
    requestRefresh.mockRejectedValue(refreshFailure(503))
    http.defaults.adapter = (async (config) => {
      throw unauthorized(config)
    }) as AxiosAdapter
    const startingEpoch = getSessionEpoch()
    const onRecovery = vi.fn()
    const unsubscribe = subscribeToSessionRecovery(onRecovery)

    try {
      await expect(http.get('/private')).rejects.toMatchObject({ response: { status: 503 } })

      expect(getAccessToken()).toBeNull()
      expect(getSessionEpoch()).toBeGreaterThan(startingEpoch)
      expect(onRecovery).toHaveBeenCalledOnce()
      requestRefresh.mockResolvedValue({ accessToken: 'recovered-jwt' })
      await expect(refreshAccessToken()).resolves.toBe('recovered-jwt')
      expect(getAccessToken()).toBe('recovered-jwt')
    } finally {
      unsubscribe()
    }
  })

  it('không refresh thêm lần nữa nếu response lỗi của auth endpoint', async () => {
    requestRefresh.mockResolvedValue({ accessToken: 'unused' })
    http.defaults.adapter = (async (config) => {
      throw unauthorized(config)
    }) as AxiosAdapter

    await expect(
      http.post('/auth/login', {}, { skipBearer: true, skipAuthRefresh: true }),
    ).rejects.toMatchObject({ response: { status: 401 } })
    expect(requestRefresh).not.toHaveBeenCalled()
  })
})
