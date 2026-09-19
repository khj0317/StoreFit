export function daysUntil(dateStr: string): number {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(dateStr)
  target.setHours(0, 0, 0, 0)
  return Math.round((target.getTime() - today.getTime()) / 86_400_000)
}

export function formatDday(days: number): string {
  if (days > 0) return `D-${days}`
  if (days === 0) return 'D-DAY'
  return `기간 초과 ${Math.abs(days)}일`
}
