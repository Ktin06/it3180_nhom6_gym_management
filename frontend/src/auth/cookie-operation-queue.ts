let tail: Promise<void> = Promise.resolve()

/** Cùng khóa cho mọi tab vì refresh, login, register và logout đều đổi cookie dùng chung. */
export function queueCookieOperation<T>(operation: () => Promise<T>): Promise<T> {
  const result = tail.then(() => {
    if (!navigator.locks) {
      throw new Error('Trình duyệt chưa hỗ trợ phối hợp phiên. Vui lòng cập nhật trình duyệt.')
    }
    return navigator.locks.request('gym-auth-cookie', { mode: 'exclusive' }, operation)
  })
  tail = result.then(
    () => undefined,
    () => undefined,
  )
  return result
}
