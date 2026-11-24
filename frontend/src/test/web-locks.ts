/** Test double cho LockManager mà jsdom chưa cung cấp; không dùng trong ứng dụng. */
export function createTestLockManager(): Pick<LockManager, 'request'> {
  const pending = new Map<string, Promise<unknown>>()
  const request = (name: string, _options: LockOptions, callback: LockGrantedCallback<unknown>) => {
    const operation = (pending.get(name) ?? Promise.resolve()).then(() =>
      callback({ name, mode: 'exclusive' } as Lock),
    )
    pending.set(
      name,
      operation.catch(() => undefined),
    )
    return operation
  }
  return { request: request as LockManager['request'] }
}
