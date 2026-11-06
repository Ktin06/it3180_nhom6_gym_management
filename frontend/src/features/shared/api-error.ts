import axios from 'axios'

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 401) return 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.'
    if (error.response?.status === 403) return 'Bạn không có quyền thực hiện thao tác này.'
    const detail: unknown = error.response?.data?.detail
    return typeof detail === 'string' && detail.trim()
      ? detail
      : 'Không kết nối được máy chủ. Vui lòng thử lại.'
  }
  return error instanceof Error ? error.message : 'Không thể tải dữ liệu. Vui lòng thử lại.'
}
