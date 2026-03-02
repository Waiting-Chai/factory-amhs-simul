export const DEV_BACKEND_ORIGIN = (
  (import.meta.env.VITE_DEV_PROXY_TARGET as string | undefined)
  || 'http://localhost:34512'
).replace(/\/$/, '')

/**
 * Normalizes model asset URLs to ensure correct proxy routing in dev environment.
 * 
 * Logic:
 * - Returns null for empty inputs
 * - If URL matches DEV_BACKEND_ORIGIN, strips origin to use relative path (Vite proxy compatible)
 * - If URL is relative, returns as-is (Vite proxy compatible)
 * - If URL is external/production (different origin), returns as-is
 * 
 * @param rawUrl The raw fileUrl or thumbnailUrl from API
 */
export const resolveModelAssetUrl = (rawUrl: string | null | undefined): string | null => {
  if (!rawUrl) return null
  const trimmed = rawUrl.trim()
  if (!trimmed) return null

  // Check if it's a full URL pointing to dev backend
  // If so, return only the path so Vite proxy can handle it
  const urlMatch = trimmed.match(/^https?:\/\/[^/]+(\/.*)$/i)
  if (urlMatch) {
    const origin = trimmed.substring(0, trimmed.indexOf(urlMatch[1]))
    if (origin === DEV_BACKEND_ORIGIN) {
      // Return relative path for Vite proxy
      return urlMatch[1]
    }
    return trimmed
  }

  // For relative paths in dev, return as-is so Vite proxy can handle it
  if (trimmed.startsWith('/')) return trimmed

  return trimmed
}
