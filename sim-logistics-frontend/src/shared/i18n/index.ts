/**
 * Minimal i18n runtime.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
import { DEFAULT_LOCALE, type Locale, messages, type MessageKey } from './messages'

const LOCALE_STORAGE_KEY = 'app.locale'
const listeners = new Set<() => void>()

const isSupportedLocale = (value: string | null | undefined): value is Locale => {
  return value === 'en' || value === 'zh'
}

const resolveInitialLocale = (): Locale => {
  if (typeof window === 'undefined') {
    return DEFAULT_LOCALE
  }

  const stored = window.localStorage.getItem(LOCALE_STORAGE_KEY)
  if (isSupportedLocale(stored)) {
    return stored
  }

  return DEFAULT_LOCALE
}

let currentLocale: Locale = resolveInitialLocale()

export const getLocale = (): Locale => currentLocale

export const setLocale = (locale: Locale): void => {
  const changed = currentLocale !== locale
  currentLocale = locale
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  }
  if (changed) {
    listeners.forEach((listener) => listener())
  }
}

export const t = (key: MessageKey): string => {
  const text = messages[currentLocale][key] ?? messages[DEFAULT_LOCALE][key]
  if (!text) {
    if (import.meta.env.DEV) {
      console.warn(`[i18n] Missing message key: ${key}`)
    }
    return key
  }
  return text
}

export const tf = (key: MessageKey, params: Record<string, string | number>): string => {
  const template = t(key)
  return Object.entries(params).reduce((acc, [name, value]) => {
    return acc.split(`{${name}}`).join(String(value))
  }, template)
}

export const subscribe = (listener: () => void): (() => void) => {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export type { Locale, MessageKey }
