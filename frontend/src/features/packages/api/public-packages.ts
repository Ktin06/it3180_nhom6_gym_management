import { http } from '../../../services/http'
import type { PublicPackagesGateway } from '../types/package'

/**
 * Bảng giá công khai: không gửi access token và không tự làm mới phiên.
 * Lý do: token hết hạn gửi kèm sẽ bị backend trả 401, kể cả ở đường dẫn công khai.
 */
export const publicPackagesApi: PublicPackagesGateway = {
  list: async () =>
    (await http.get('/public/packages', { skipBearer: true, skipAuthRefresh: true })).data,
}
