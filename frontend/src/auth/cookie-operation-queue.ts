let tail: Promise<void> = Promise.resolve()

/** Tuần tự hóa request có thể thay đổi cookie refresh để response cũ không ghi đè login/logout mới. */
export function queueCookieOperation<T>(operation: () => Promise<T>): Promise<T> {
  const result = tail.then(operation)
  tail = result.then(
    () => undefined,
    () => undefined,
  )
  return result
}
