import { ReloadOutlined } from '@ant-design/icons'
import { Alert, App, Button, Card, Select, Skeleton, Typography } from 'antd'
import { useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthContext } from '../../../auth/AuthContext'
import type { Role } from '../../../auth/roles'
import { canOpen } from '../../../routes/route-access'
import { checkinsApi } from '../../checkins/api/checkins'
import { errorMessage } from '../../shared/api-error'
import { dashboardApi } from '../api/dashboard'
import { ExpiringMembershipsTable } from '../components/ExpiringMembershipsTable'
import { HourlyCheckinChart } from '../components/HourlyCheckinChart'
import { LatestCheckinsTable } from '../components/LatestCheckinsTable'
import { vndFormat } from '../dashboard-format'
import type {
  BranchOption,
  DashboardBranch,
  DashboardGateway,
  DashboardStats,
  ExpiringMembership,
  HourlyCheckin,
  LatestCheckin,
} from '../types/dashboard'
import '../../shared/feature-pages.css'
import './DashboardPage.css'

interface DashboardSnapshot {
  stats: DashboardStats
  hourly: HourlyCheckin[]
  expiring: ExpiringMembership[]
  latest: LatestCheckin[]
}

interface DashboardRequest {
  branchId: number | undefined
  nonce: number
}

function toBranchOption(branch: DashboardBranch): BranchOption {
  const base = `${branch.name} (${branch.code})`
  return {
    id: branch.id,
    label: branch.status === 'INACTIVE' ? `${base} – Ngừng hoạt động` : base,
  }
}

export interface DashboardPageProps {
  api?: DashboardGateway
  loadAvatar?: (memberId: number) => Promise<Blob>
  branchOptions?: BranchOption[]
  role?: Role
}

/**
 * Màn hình Dashboard cơ bản theo chi nhánh (F6 – S2-12.2):
 * - Lễ tân và quản lý chi nhánh: tự tải dữ liệu chi nhánh mình, không có ô chọn chi nhánh.
 * - Chủ chuỗi (SUPER_ADMIN): tải danh sách chi nhánh từ GET /api/v1/dashboard/branches để chọn;
 *   chưa chọn chi nhánh thì chưa gọi 4 API số liệu.
 * - Check-out nhanh dùng lại checkinsApi.checkout trong features/checkins/api/checkins.ts.
 */
export function DashboardPage({
  api = dashboardApi,
  loadAvatar,
  branchOptions,
  role: roleProp,
}: DashboardPageProps) {
  const { message } = App.useApp()
  const auth = useContext(AuthContext)
  const navigate = useNavigate()

  const role: Role = roleProp ?? auth?.user?.role ?? 'RECEPTIONIST'
  const isSuperAdmin = role === 'SUPER_ADMIN'
  const canCheckout = role === 'RECEPTIONIST' || role === 'BRANCH_MANAGER'
  const canRenew = canOpen(role, '/ban-goi')

  const [branchesNonce, setBranchesNonce] = useState(0)
  const [branchLoadState, setBranchLoadState] = useState<{
    nonce: number
    options?: BranchOption[]
    error?: unknown
  }>()

  const [request, setRequest] = useState<DashboardRequest>({
    branchId: undefined,
    nonce: 0,
  })
  const [result, setResult] = useState<{
    request: DashboardRequest
    data?: DashboardSnapshot
    error?: unknown
  }>()

  const [checkoutBusyId, setCheckoutBusyId] = useState<number | null>(null)
  const [checkoutNotice, setCheckoutNotice] = useState<{
    type: 'success' | 'error'
    text: string
  } | null>(null)

  const shouldLoadBranches = isSuperAdmin && branchOptions === undefined

  useEffect(() => {
    if (!shouldLoadBranches) {
      return
    }
    let active = true
    api.branches().then(
      (rows) => {
        if (!active) return
        setBranchLoadState({
          nonce: branchesNonce,
          options: rows.map(toBranchOption),
        })
      },
      (error: unknown) => {
        if (!active) return
        setBranchLoadState({ nonce: branchesNonce, error })
      },
    )
    return () => {
      active = false
    }
  }, [api, branchesNonce, shouldLoadBranches])

  const currentBranches =
    shouldLoadBranches && branchLoadState?.nonce === branchesNonce ? branchLoadState : undefined
  const resolvedBranchOptions = branchOptions ?? currentBranches?.options ?? []
  const loadingBranches = shouldLoadBranches && !currentBranches
  const branchesError = shouldLoadBranches ? currentBranches?.error : undefined

  const waitingForBranch = isSuperAdmin && request.branchId === undefined

  useEffect(() => {
    if (waitingForBranch) {
      return
    }
    let active = true
    const queryBranchId = isSuperAdmin ? request.branchId : undefined

    Promise.all([
      api.stats(queryBranchId),
      api.hourlyCheckins(queryBranchId),
      api.expiringMemberships({ branchId: queryBranchId, limit: 10 }),
      api.latestCheckins({ branchId: queryBranchId, limit: 10 }),
    ]).then(
      ([statsData, hourlyData, expiringData, latestData]) => {
        if (!active) return
        setResult({
          request,
          data: {
            stats: statsData,
            hourly: hourlyData,
            expiring: expiringData,
            latest: latestData,
          },
        })
      },
      (error: unknown) => {
        if (!active) return
        setResult({ request, error })
      },
    )

    return () => {
      active = false
    }
  }, [api, isSuperAdmin, request, waitingForBranch])

  const current = result?.request === request ? result : undefined
  const loading = !waitingForBranch && !current
  const loadError = waitingForBranch ? undefined : current?.error
  const snapshot = waitingForBranch ? undefined : (current?.data ?? result?.data)
  const stats = snapshot?.stats ?? null
  const hourly = snapshot?.hourly ?? []
  const expiring = snapshot?.expiring ?? []
  const latest = snapshot?.latest ?? []

  function reloadDashboard() {
    setRequest((prev) => ({ branchId: prev.branchId, nonce: prev.nonce + 1 }))
  }

  async function handleQuickCheckout(item: LatestCheckin) {
    if (!canCheckout) return
    setCheckoutBusyId(item.checkinId)
    setCheckoutNotice(null)
    try {
      const checkoutResult = await checkinsApi.checkout(item.checkinId)
      const successText = `${checkoutResult.message} (${item.fullName} · ${checkoutResult.durationMinutes} phút)`
      setCheckoutNotice({ type: 'success', text: successText })
      void message?.success?.(checkoutResult.message)
      reloadDashboard()
    } catch (cause) {
      setCheckoutNotice({ type: 'error', text: errorMessage(cause) })
    } finally {
      setCheckoutBusyId(null)
    }
  }

  function handleRenew(item: ExpiringMembership) {
    navigate('/ban-goi', {
      state: {
        prefillMember: {
          id: item.memberId,
          memberCode: item.memberCode,
          fullName: item.fullName,
          phone: item.phone,
          status: 'ACTIVE' as const,
        },
      },
    })
  }

  return (
    <section className="feature-page" aria-labelledby="dashboard-heading">
      <div className="feature-heading">
        <div>
          <h2 id="dashboard-heading">Dashboard vận hành trong ngày</h2>
          <p>Theo dõi hội viên đang tập, mật độ check-in theo giờ, doanh thu và gói sắp hết.</p>
        </div>

        <div className="dashboard-controls">
          {isSuperAdmin && (
            <div className="dashboard-branch-picker">
              <label htmlFor="dashboard-branch-select">Chọn chi nhánh</label>
              <Select<number>
                id="dashboard-branch-select"
                aria-label="Chọn chi nhánh"
                placeholder={loadingBranches ? 'Đang tải chi nhánh...' : 'Chọn chi nhánh'}
                loading={loadingBranches}
                disabled={loadingBranches && resolvedBranchOptions.length === 0}
                allowClear
                value={request.branchId}
                options={resolvedBranchOptions.map((option) => ({
                  value: option.id,
                  label: option.label,
                }))}
                onChange={(value) => {
                  setCheckoutNotice(null)
                  setRequest((prev) => ({ branchId: value, nonce: prev.nonce + 1 }))
                }}
              />
            </div>
          )}

          <Button
            icon={<ReloadOutlined />}
            loading={loading}
            disabled={waitingForBranch}
            onClick={reloadDashboard}
            style={{ minHeight: 36 }}
          >
            Tải lại
          </Button>
        </div>
      </div>

      {Boolean(branchesError) && (
        <Alert
          type="error"
          showIcon
          title={errorMessage(branchesError)}
          action={
            <Button size="small" onClick={() => setBranchesNonce((n) => n + 1)}>
              Thử lại
            </Button>
          }
          style={{ marginBottom: 16 }}
        />
      )}

      {waitingForBranch ? (
        <Alert
          type="info"
          showIcon
          title="Vui lòng chọn chi nhánh để xem Dashboard."
          description="Chủ chuỗi cần chọn một chi nhánh cụ thể ở góc trên để tải số liệu trong ngày."
        />
      ) : (
        <>
          {Boolean(loadError) && (
            <Alert
              type="error"
              showIcon
              title={errorMessage(loadError)}
              action={
                <Button size="small" onClick={reloadDashboard}>
                  Thử lại
                </Button>
              }
              style={{ marginBottom: 16 }}
            />
          )}

          {checkoutNotice && (
            <Alert
              type={checkoutNotice.type}
              showIcon
              closable
              data-testid="checkout-notice"
              title={checkoutNotice.text}
              onClose={() => setCheckoutNotice(null)}
              style={{ marginBottom: 16 }}
            />
          )}

          <div className="dashboard-kpi-grid">
            <Card className="dashboard-kpi-card" data-testid="kpi-currently-in-gym">
              <div className="dashboard-kpi-title">
                <span>Hội viên đang tập</span>
                <span className="dashboard-live-dot" aria-hidden="true" />
              </div>
              {loading && !stats ? (
                <Skeleton.Input active size="small" />
              ) : (
                <>
                  <div className="dashboard-kpi-value">{stats?.currentlyInGym ?? 0}</div>
                  <div className="dashboard-kpi-sub">Đang có mặt chưa check-out</div>
                </>
              )}
            </Card>

            <Card className="dashboard-kpi-card" data-testid="kpi-today-checkins">
              <div className="dashboard-kpi-title">
                <span>Lượt check-in hôm nay</span>
              </div>
              {loading && !stats ? (
                <Skeleton.Input active size="small" />
              ) : (
                <>
                  <div className="dashboard-kpi-value">{stats?.todayCheckinCount ?? 0}</div>
                  <div className="dashboard-kpi-sub">Tổng lượt vào phòng tập trong ngày</div>
                </>
              )}
            </Card>

            <Card className="dashboard-kpi-card" data-testid="kpi-today-revenue">
              <div className="dashboard-kpi-title">
                <span>Doanh thu hôm nay</span>
              </div>
              {loading && !stats ? (
                <Skeleton.Input active size="small" />
              ) : (
                <>
                  <div className="dashboard-kpi-value">
                    {vndFormat.format(stats?.todayRevenue ?? 0)}
                  </div>
                  <div className="dashboard-kpi-sub">
                    <span>Tiền mặt: {vndFormat.format(stats?.cashRevenue ?? 0)}</span>
                    <span>Chuyển khoản: {vndFormat.format(stats?.transferRevenue ?? 0)}</span>
                  </div>
                </>
              )}
            </Card>

            <Card className="dashboard-kpi-card" data-testid="kpi-expiring-packages">
              <div className="dashboard-kpi-title">
                <span>Gói sắp hết</span>
              </div>
              {loading && !stats ? (
                <Skeleton.Input active size="small" />
              ) : (
                <>
                  <div className="dashboard-kpi-value is-warning">
                    {stats?.expiringPackagesCount ?? 0}
                  </div>
                  <div className="dashboard-kpi-sub">
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      Sắp hết hạn 7 ngày hoặc sắp hết buổi
                    </Typography.Text>
                  </div>
                </>
              )}
            </Card>
          </div>

          <HourlyCheckinChart data={hourly} loading={loading && hourly.length === 0} />

          <div className="dashboard-tables-grid">
            <LatestCheckinsTable
              items={latest}
              loading={loading && latest.length === 0}
              canCheckout={canCheckout}
              checkoutBusyId={checkoutBusyId}
              onQuickCheckout={(item) => void handleQuickCheckout(item)}
              loadAvatar={loadAvatar}
            />

            <ExpiringMembershipsTable
              items={expiring}
              loading={loading && expiring.length === 0}
              canRenew={canRenew}
              onRenew={handleRenew}
              loadAvatar={loadAvatar}
            />
          </div>
        </>
      )}
    </section>
  )
}
