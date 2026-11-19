import { http } from '../../../services/http'
import { publicRequest } from '../../../services/request-options'
import type { PublicPackagesGateway } from '../types/package'

/**
 * Bảng giá công khai: không gửi access token và không tự làm mới phiên.
 * Backend cũng bỏ qua token ở GET /api/v1/public/**; tùy chọn này tránh gửi token và tránh tự làm mới phiên vô ích.
 */
export const publicPackagesApi: PublicPackagesGateway = {
  list: async () => (await http.get('/public/packages', publicRequest)).data,
}
