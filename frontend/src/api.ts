import type { ApiPayload } from './types'

export class ApiError extends Error {
  status: number
  payload: ApiPayload

  constructor(status: number, payload: ApiPayload) {
    super(payload.message || 'Request failed')
    this.status = status
    this.payload = payload
  }
}

let onUnauthorized = () => {}

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

function csrfToken(): string | null {
  const entry = document.cookie.split('; ').find((part) => part.startsWith('XSRF-TOKEN='))
  if (!entry) return null
  return decodeURIComponent(entry.slice('XSRF-TOKEN='.length))
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const token = csrfToken()
  if (token) headers.set('X-XSRF-TOKEN', token)
  const response = await fetch(path, { ...options, headers, credentials: 'include' })
  if (response.status === 204) return undefined as T
  const text = await response.text()
  const payload = text ? (JSON.parse(text) as ApiPayload & T) : ({} as ApiPayload & T)
  if (response.status === 401 && !path.startsWith('/api/auth/login')) {
    onUnauthorized()
  }
  if (!response.ok) throw new ApiError(response.status, payload)
  return payload
}

export function warmup() {
  return fetch('/api/health', { credentials: 'include' })
}
