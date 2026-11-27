import { useEffect, useRef, useState } from 'react'
import { errorMessage } from '../shared/api-error'

/** Dùng chung cho nhân viên và điểm vào PDF của hội viên; chỉ cấp URL sau khi API kiểm tra quyền. */
export function useInvoicePdf(loadPdf: (id: number) => Promise<Blob>) {
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState<string>()
  const active = useRef(false)
  const busy = useRef(false)
  const urls = useRef(new Map<string, number>())

  useEffect(() => {
    active.current = true
    const pendingUrls = urls.current
    return () => {
      active.current = false
      for (const [url, timer] of pendingUrls) {
        window.clearTimeout(timer)
        URL.revokeObjectURL(url)
      }
      pendingUrls.clear()
    }
  }, [])

  async function exportPdf(id: number, filename: string) {
    if (busy.current || !active.current) return
    busy.current = true
    setExporting(true)
    setError(undefined)
    try {
      const blob = await loadPdf(id)
      if (!active.current) return
      const url = URL.createObjectURL(blob)
      const timer = window.setTimeout(() => {
        URL.revokeObjectURL(url)
        urls.current.delete(url)
      }, 60_000)
      urls.current.set(url, timer)
      // Giữ bản sửa thứ hai của prototype: không tải thêm khi tab đã mở.
      const opened = window.open(url, '_blank')
      if (opened) opened.opener = null
      else {
        const link = document.createElement('a')
        link.href = url
        link.download = filename
        document.body.appendChild(link)
        link.click()
        link.remove()
      }
    } catch (cause) {
      if (active.current) setError(errorMessage(cause))
    } finally {
      busy.current = false
      if (active.current) setExporting(false)
    }
  }

  return { exporting, error, exportPdf, clearError: () => setError(undefined) }
}
