import { useEffect, useState } from 'react'

const timeZone = 'Asia/Ho_Chi_Minh'
const dateKey = new Intl.DateTimeFormat('sv-SE', { timeZone })
const day = new Intl.DateTimeFormat('vi-VN', { timeZone, dateStyle: 'full' })
const clock = new Intl.DateTimeFormat('vi-VN', {
  timeZone,
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

export const classDateKey = (value: Date | string) => dateKey.format(new Date(value))
export const classDay = (value: string) => day.format(new Date(value))
export const classClock = (value: string) => clock.format(new Date(value))

export function currentWeek(now = new Date()) {
  const from = classDateKey(now)
  const end = new Date(`${from}T00:00:00Z`)
  end.setUTCDate(end.getUTCDate() + 6)
  return { from, to: end.toISOString().slice(0, 10) }
}

export function classTimeRange(startAt: string, endAt: string) {
  const endDay = classDateKey(startAt) === classDateKey(endAt) ? '' : ` · ${classDay(endAt)}`
  return `${classClock(startAt)}–${classClock(endAt)}${endDay}`
}

export function useClassClock() {
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 15_000)
    return () => window.clearInterval(timer)
  }, [])
  return now
}
