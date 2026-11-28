import { useEffect, useState } from 'react'

/** Chỉ nhận kết quả của yêu cầu hiện tại; yêu cầu cũ không ghi đè tìm kiếm mới. */
export function useRemoteQuery<Q, T>(load: (query: Q) => Promise<T>, initialQuery: Q) {
  const [request, setRequest] = useState({ query: initialQuery })
  const [result, setResult] = useState<{
    request: typeof request
    data?: T
    error?: unknown
  }>()

  useEffect(() => {
    let active = true
    load(request.query).then(
      (data) => active && setResult({ request, data }),
      (error: unknown) => active && setResult({ request, error }),
    )
    return () => {
      active = false
    }
  }, [load, request])

  const current = result?.request === request ? result : undefined
  return {
    query: request.query,
    data: current?.data,
    error: current?.error,
    loading: !current,
    changeQuery: (query: Q) => setRequest({ query }),
    reload: () => setRequest({ query: request.query }),
  }
}
