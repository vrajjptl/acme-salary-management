export function usd(value: string | null | undefined): string {
  if (!value) return '—'
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0
  }).format(Number(value))
}

export function money(value: string | null | undefined, currency?: string | null): string {
  if (!value) return '—'
  if (!currency) return value
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(Number(value))
  } catch {
    return `${value} ${currency}`
  }
}

export function compa(value: string | null | undefined): string {
  if (!value) return '—'
  return Number(value).toFixed(2)
}

export function bandLabel(value: string | null | undefined): string {
  if (value === 'BELOW') return 'Below band'
  if (value === 'IN_BAND') return 'In band'
  if (value === 'ABOVE') return 'Above band'
  return 'Not in force'
}

export function count(value: number): string {
  return new Intl.NumberFormat('en-US').format(value)
}

export function titleCase(value: string): string {
  return value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ')
}
