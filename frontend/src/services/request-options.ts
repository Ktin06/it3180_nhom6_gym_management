/** Tùy chọn cho request không cần đăng nhập: không gửi access token, không tự làm mới phiên khi gặp 401. */
export const publicRequest = { skipBearer: true, skipAuthRefresh: true }
