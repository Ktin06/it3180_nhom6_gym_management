import { Alert, Card, Empty, Input, Tabs } from 'antd'
import { isAxiosError } from 'axios'
import { useCallback, useRef, useState } from 'react'
import { errorMessage } from '../../../services/api-error'
import { useRemoteQuery } from '../../../services/use-remote-query'
import { checkinsApi } from '../api/checkins'
import { ActiveCheckinsPanel } from '../components/ActiveCheckinsPanel'
import { CheckinHistoryTab } from '../components/CheckinHistoryTab'
import { MemberCheckinCard } from '../components/MemberCheckinCard'
import type {
  CheckinGateway,
  CheckinHistoryQuery,
  CheckinResult,
  CheckoutResult,
  MemberSearchResult,
} from '../types/checkin'
import '../../../components/feature-pages.css'
import './CheckinPage.css'

const initialActiveQuery: CheckinHistoryQuery = {
  status: 'CHECKED_IN',
  page: 0,
  size: 20,
}

export interface CheckinPageProps {
  api?: CheckinGateway
  loadAvatar?: (memberId: number) => Promise<Blob>
}

/**
 * Màn hình Check-in tại quầy (F4 – S2-11.3):
 * - Tab "Check-in tại quầy": tra cứu theo mã HV / SĐT, đối chiếu ảnh lớn, xem mọi gói + lớp sắp diễn ra,
 *   xác nhận check-in (kèm thử lại khi 409 VD-20), danh sách "Đang tập hôm nay" với nút Check-out.
 * - Tab "Lịch sử check-in": lọc ngày, trạng thái, phương thức, từ khóa, phân trang.
 */
export function CheckinPage({ api = checkinsApi, loadAvatar }: CheckinPageProps) {
  const loadActive = useCallback((query: CheckinHistoryQuery) => api.history(query), [api])
  const activeList = useRemoteQuery(loadActive, initialActiveQuery)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState<unknown>()
  const [candidate, setCandidate] = useState<MemberSearchResult | null>(null)
  const searchSeqRef = useRef(0)

  const [checkingIn, setCheckingIn] = useState(false)
  const [checkinResult, setCheckinResult] = useState<CheckinResult | null>(null)
  const [checkinError, setCheckinError] = useState<unknown>()

  const [checkoutBusyId, setCheckoutBusyId] = useState<number | null>(null)
  const [checkoutSuccess, setCheckoutSuccess] = useState<CheckoutResult | null>(null)
  const [checkoutError, setCheckoutError] = useState<unknown>()

  const [historyRefreshKey, setHistoryRefreshKey] = useState(0)

  async function handleSearch(rawKeyword: string) {
    const seq = ++searchSeqRef.current
    setSearching(true)
    setSearchError(undefined)
    setCheckinResult(null)
    setCheckinError(undefined)
    try {
      const found = await api.searchMember(rawKeyword)
      if (searchSeqRef.current === seq) {
        setCandidate(found)
      }
    } catch (cause) {
      if (searchSeqRef.current === seq) {
        setCandidate(null)
        setSearchError(cause)
      }
    } finally {
      if (searchSeqRef.current === seq) {
        setSearching(false)
      }
    }
  }

  async function handleCheckin() {
    if (!candidate || candidate.status === 'INACTIVE') return
    setCheckingIn(true)
    setCheckinError(undefined)
    setCheckinResult(null)
    try {
      const res = await api.checkin(candidate.memberId)
      setCheckinResult(res)
      setCandidate((prev) =>
        prev && prev.memberId === candidate.memberId
          ? { ...prev, isCurrentlyCheckedIn: true }
          : prev,
      )
      activeList.reload()
      setHistoryRefreshKey((k) => k + 1)
    } catch (cause) {
      setCheckinError(cause)
    } finally {
      setCheckingIn(false)
    }
  }

  function refreshActiveListAfterCheckout() {
    if (
      activeList.query.page > 0 &&
      activeList.data &&
      activeList.data.content.length <= 1
    ) {
      activeList.changeQuery({
        ...activeList.query,
        page: activeList.query.page - 1,
      })
      return
    }
    activeList.reload()
  }

  async function handleCheckout(checkinId: number) {
    setCheckoutBusyId(checkinId)
    setCheckoutError(undefined)
    setCheckoutSuccess(null)
    try {
      const res = await api.checkout(checkinId)
      setCheckoutSuccess(res)
      refreshActiveListAfterCheckout()
      setHistoryRefreshKey((k) => k + 1)
    } catch (cause) {
      setCheckoutError(cause)
      // 404: lượt đã được check-out ở tab hay quầy khác (UC-F4.4 E1). Tải lại để bỏ dòng cũ khỏi danh sách.
      if (isAxiosError(cause) && cause.response?.status === 404) {
        refreshActiveListAfterCheckout()
        setHistoryRefreshKey((k) => k + 1)
      }
    } finally {
      setCheckoutBusyId(null)
    }
  }

  return (
    <section className="feature-page" aria-labelledby="checkin-heading">
      <div className="feature-heading">
        <div>
          <h2 id="checkin-heading">Check-in tại quầy</h2>
          <p>
            Tra cứu hội viên theo mã HV hoặc số điện thoại, đối chiếu ảnh đại diện, ghi nhận
            check-in/check-out và xem lịch sử trong ngày.
          </p>
        </div>
      </div>

      <div className="feature-content">
        <Tabs
          defaultActiveKey="desk"
          items={[
            {
              key: 'desk',
              label: 'Check-in tại quầy',
              children: (
                <div className="checkin-desk-grid">
                  <div>
                    <Card className="checkin-search-card">
                      <label htmlFor="checkin-member-search" className="checkin-search-label">
                        Tìm hội viên (Mã HV… hoặc số điện thoại)
                      </label>
                      <Input.Search
                        id="checkin-member-search"
                        placeholder="Ví dụ: HV000012 hoặc 0912345678"
                        allowClear
                        size="large"
                        value={searchKeyword}
                        loading={searching}
                        onChange={(e) => setSearchKeyword(e.target.value)}
                        onSearch={handleSearch}
                        enterButton="Tra cứu"
                      />
                    </Card>

                    {searchError != null && (
                      <Alert
                        type="error"
                        showIcon
                        style={{ marginBottom: 16 }}
                        title="Không tra cứu được hội viên"
                        description={errorMessage(searchError)}
                      />
                    )}

                    {candidate ? (
                      <MemberCheckinCard
                        member={candidate}
                        checkingIn={checkingIn}
                        checkinResult={checkinResult}
                        checkinError={checkinError}
                        onCheckin={handleCheckin}
                        loadAvatar={loadAvatar}
                      />
                    ) : (
                      !searchError && (
                        <Card>
                          <Empty
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                            description="Nhập mã hội viên (HV…) hoặc số điện thoại 10 chữ số để đối chiếu và check-in."
                          />
                        </Card>
                      )
                    )}
                  </div>

                  <ActiveCheckinsPanel
                    data={activeList.data}
                    loading={activeList.loading}
                    error={activeList.error}
                    checkoutBusyId={checkoutBusyId}
                    checkoutSuccess={checkoutSuccess}
                    checkoutError={checkoutError}
                    onSearchKeyword={(kw) =>
                      activeList.changeQuery({
                        ...activeList.query,
                        keyword: kw.trim() || undefined,
                        page: 0,
                      })
                    }
                    onPageChange={(page, size) =>
                      activeList.changeQuery({
                        ...activeList.query,
                        page,
                        size,
                      })
                    }
                    onReload={activeList.reload}
                    onCheckout={handleCheckout}
                    loadAvatar={loadAvatar}
                  />
                </div>
              ),
            },
            {
              key: 'history',
              label: 'Lịch sử check-in',
              children: (
                <CheckinHistoryTab
                  api={api}
                  refreshKey={historyRefreshKey}
                  checkoutBusyId={checkoutBusyId}
                  onCheckout={handleCheckout}
                  loadAvatar={loadAvatar}
                />
              ),
            },
          ]}
        />
      </div>
    </section>
  )
}
