/**
 * Reactive i18n hook.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
import { useEffect, useState } from 'react'
import { getLocale, setLocale, subscribe, t, tf } from './index'

export const useI18n = () => {
  const [locale, setLocaleState] = useState(getLocale())

  useEffect(() => {
    const unsubscribe = subscribe(() => {
      setLocaleState(getLocale())
    })
    return unsubscribe
  }, [])

  return {
    locale,
    t,
    tf,
    setLocale,
  }
}

export default useI18n
