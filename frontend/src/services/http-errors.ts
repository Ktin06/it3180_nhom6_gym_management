/** Dùng chung cho bootstrap và interceptor, không phụ thuộc câu chữ lỗi của server. */
export function isUnauthorized(error: unknown): boolean {
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
