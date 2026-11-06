import type { PageQuery, PageResponse, Status } from '../../shared/types'

export type Gender = 'MALE' | 'FEMALE' | 'OTHER'

/** DTO từ PR #97, commit 37bc98a; không gửi ảnh/email/chi nhánh trong form F2. */
export interface MemberSummary {
  id: number
  memberCode: string
  fullName: string
  phone: string
  email: string | null
  status: Status
  homeBranchId: number
}

export interface MemberDetail extends MemberSummary {
  userId: number | null
  gender: Gender | null
  dob: string | null
  createdAt: string
  updatedAt: string
}

export interface MemberInput {
  fullName: string
  phone: string
  gender: Gender | null
  dob: string | null
}

export interface MemberUpdate extends MemberInput {
  status: Status
}

export interface MemberQuery extends PageQuery {
  keyword?: string
  status?: Status
}

export interface MembersGateway {
  list(query: MemberQuery): Promise<PageResponse<MemberSummary>>
  detail(id: number): Promise<MemberDetail>
  create(input: MemberInput): Promise<MemberDetail>
  update(id: number, input: MemberUpdate): Promise<MemberDetail>
}
