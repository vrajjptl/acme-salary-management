import { money } from './format'

const LEVELS = ['L1', 'L2', 'L3', 'L4', 'L5', 'L6', 'M1', 'M2', 'M3']

export interface PayPreviewInput {
  changeType: string
  currentBase: string | null
  nextBase: string
  bandMin: string | null
  bandMax: string | null
  currency: string | null
  effectiveDate: string
  latestEffectiveDate: string | null
  currentLevel: string
  nextLevel: string
  reason: string
  exceptionReason: string
  today: string
}

export interface PayPreview {
  tone: 'good' | 'warn' | 'neutral'
  items: string[]
}

function cents(value: string | null | undefined): number | null {
  if (!value) return null
  const text = value.trim()
  if (!/^\d{1,9}(\.\d{1,2})?$/.test(text)) return null
  const [whole, fraction = ''] = text.split('.')
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
}

function addMonths(iso: string, months: number): string {
  const [year, month, day] = iso.split('-').map(Number)
  const cursor = new Date(Date.UTC(year, month - 1 + months, 1))
  const lastDay = new Date(Date.UTC(cursor.getUTCFullYear(), cursor.getUTCMonth() + 1, 0)).getUTCDate()
  const chosen = Math.min(day, lastDay)
  const monthText = String(cursor.getUTCMonth() + 1).padStart(2, '0')
  const dayText = String(chosen).padStart(2, '0')
  return `${cursor.getUTCFullYear()}-${monthText}-${dayText}`
}

export function previewPay(input: PayPreviewInput): PayPreview {
  const next = cents(input.nextBase)
  const current = cents(input.currentBase)
  if (next == null || next <= 0) {
    return { tone: 'neutral', items: ['Enter the new annual base as a number with up to two decimal places.'] }
  }

  const notes: string[] = []
  const blockers: string[] = []
  const coveredByException: string[] = []
  const currency = input.currency
  const levelChanged = input.changeType === 'PROMOTION' || (input.changeType === 'CORRECTION' && input.nextLevel !== input.currentLevel)
  const min = levelChanged ? null : cents(input.bandMin)
  const max = levelChanged ? null : cents(input.bandMax)

  function needsException(message: string) {
    if (input.exceptionReason.trim().length >= 12) notes.push(message.replace(/\.$/, '') + ', and the exception reason covers it.')
    else coveredByException.push(message)
  }

  if (current != null && current > 0) {
    const delta = ((next - current) / current) * 100
    const sign = delta > 0 ? '+' : ''
    notes.push(`${sign}${delta.toFixed(1)}% from ${money(input.currentBase, currency)} to ${money(input.nextBase, currency)}.`)
    if (next < current) needsException('A decrease needs an exception reason of at least 12 characters.')
    if (input.changeType !== 'PROMOTION' && next * 4 > current * 5) {
      needsException('An increase above 25% needs an exception reason of at least 12 characters.')
    }
  }

  if (input.changeType === 'PROMOTION') {
    const steps = LEVELS.indexOf(input.nextLevel) - LEVELS.indexOf(input.currentLevel)
    if (steps < 1) blockers.push('A promotion has to move to a higher level, at most two steps up.')
    else if (steps > 2) blockers.push('A promotion cannot skip more than two levels.')
    else notes.push(`Moves up ${steps === 1 ? 'one level' : 'two levels'}. The 25% cap does not apply. The new level's band is checked when you save.`)
  } else if (min != null && max != null) {
    if (next < min || next > max) {
      const edge = next < min ? `below the minimum of ${money(input.bandMin, currency)}` : `above the maximum of ${money(input.bandMax, currency)}`
      needsException(`This base is ${edge}. An exception reason of at least 12 characters is required.`)
    } else {
      notes.push(`Inside the current band, ${money(input.bandMin, currency)} to ${money(input.bandMax, currency)}.`)
    }
  }

  if (input.latestEffectiveDate && input.effectiveDate < input.latestEffectiveDate) {
    blockers.push('The effective date is before the latest pay row.')
  } else if (input.latestEffectiveDate && input.effectiveDate === input.latestEffectiveDate && input.changeType !== 'CORRECTION') {
    blockers.push('A change on the same day as the latest row has to be a correction.')
  }
  if (input.today && input.effectiveDate > addMonths(input.today, 3)) {
    blockers.push('The effective date is more than three months ahead.')
  }
  if (input.reason.trim().length < 8) blockers.push('Add a reason of at least 8 characters.')
  if (input.exceptionReason.trim().length > 0 && input.exceptionReason.trim().length < 12) {
    blockers.push('An exception reason, when present, needs at least 12 characters.')
  }

  const items = [...notes, ...coveredByException, ...blockers]
  if (coveredByException.length || blockers.length) return { tone: 'warn', items }
  return { tone: 'good', items: [...items, 'This looks ready to record. The server still makes the final check.'] }
}
