import axios from 'axios'

export function invoiceFieldErrors(error: unknown): Record<string, string> {
  if (!axios.isAxiosError(error)) return {}
  const errors: unknown = error.response?.data?.errors
  if (!errors || typeof errors !== 'object') return {}
  return Object.fromEntries(
    Object.entries(errors).filter(
      (entry): entry is [string, string] => typeof entry[1] === 'string',
    ),
  )
}

/** responseType=blob áp dụng cho cả ProblemDetail; giữ nguyên mã HTTP và lỗi Axios. */
export async function decodePdfProblem(error: unknown): Promise<void> {
  if (!axios.isAxiosError(error) || !(error.response?.data instanceof Blob)) return
  const blob = error.response.data
  try {
    const text =
      typeof blob.text === 'function'
        ? await blob.text()
        : await new Promise<string>((resolve, reject) => {
            const reader = new FileReader()
            reader.onload = () => resolve(String(reader.result))
            reader.onerror = () => reject(reader.error)
            reader.readAsText(blob)
          })
    const problem: unknown = JSON.parse(text)
    if (problem && typeof problem === 'object' && !Array.isArray(problem))
      error.response.data = problem
  } catch {
    // Proxy có thể trả HTML hoặc dữ liệu hỏng; dùng thông báo lỗi mặc định.
  }
}
