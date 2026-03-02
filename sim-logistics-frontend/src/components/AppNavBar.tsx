/**
 * Global application navigation bar.
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-11
 */
import React, { useEffect, useState, useCallback } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useI18n } from '../shared/i18n/useI18n'
import { useToastStore } from '@store/toastStore'
import { scenesApi } from '@api/scenes'

type NavItemKey = 'scenes' | 'models' | 'simulation' | 'config' | 'editor'
type NavLabelKey = 
  | 'nav.scenes' 
  | 'nav.models' 
  | 'nav.simulation' 
  | 'nav.config' 
  | 'nav.sceneEditor'

interface NavItem {
  key: NavItemKey
  to: string | null
  labelKey: NavLabelKey
}

const BASE_NAV_ITEMS: NavItem[] = [
  { key: 'scenes', to: '/scenes', labelKey: 'nav.scenes' },
  { key: 'models', to: '/models', labelKey: 'nav.models' },
  { key: 'simulation', to: '/simulation', labelKey: 'nav.simulation' },
  { key: 'config', to: '/config', labelKey: 'nav.config' },
]

const isActive = (pathname: string, key: NavItemKey): boolean => {
  if (key === 'scenes') {
    return (pathname === '/scenes' || pathname.startsWith('/scenes/')) && !pathname.includes('/edit')
  }
  if (key === 'editor') {
    return pathname.includes('/edit')
  }
  if (key === 'models') {
    return pathname.startsWith('/models')
  }
  if (key === 'simulation') {
    return pathname.startsWith('/simulation')
  }
  if (key === 'config') {
    return pathname.startsWith('/config')
  }
  return false
}

/**
 * AppNavBar component
 */
export const AppNavBar: React.FC = () => {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { t } = useI18n()
  const { addToast } = useToastStore()
  const [lastSceneId, setLastSceneId] = useState<string | null>(null)

  useEffect(() => {
    const id = localStorage.getItem('lastSceneId')
    setLastSceneId(id)
  }, [pathname]) // Update when pathname changes (e.g. entering editor)

  const handleEditorClick = useCallback(async (e: React.MouseEvent) => {
    e.preventDefault()
    if (!lastSceneId) return

    try {
      // Task 3: Validate scene existence before navigation
      await scenesApi.getById(lastSceneId)
      navigate(`/scenes/${lastSceneId}/edit`)
    } catch {
      localStorage.removeItem('lastSceneId')
      setLastSceneId(null)
      addToast('error', t('sceneEditor.error.sceneNotFound'))
      navigate('/scenes')
    }
  }, [lastSceneId, navigate, addToast, t])

  const navItems = [...BASE_NAV_ITEMS]
  if (lastSceneId) {
    navItems.splice(1, 0, {
      key: 'editor',
      to: `/scenes/${lastSceneId}/edit`, // Kept for key/structure, but click is intercepted
      labelKey: 'nav.sceneEditor'
    })
  }

  return (
    <header className="sticky top-0 z-50 border-b border-industrial-steel/30 bg-industrial-dark/90 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-7xl items-center gap-2 px-4 md:px-6">
        <Link
          to="/scenes"
          className="mr-4 text-sm font-bold tracking-wide text-white"
        >
          Plant Simulation
        </Link>

        <nav className="flex flex-wrap items-center gap-1" aria-label="Global Navigation">
          {navItems.map((item) => {
            if (!item.to) return null
            const active = isActive(pathname, item.key)
            const linkClassName = [
              'rounded-md px-3 py-1.5 text-sm transition-colors',
              active
                ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                : 'text-gray-300 hover:bg-industrial-slate hover:text-white border border-transparent',
            ].join(' ')

            if (item.key === 'editor') {
              return (
                <a
                  key={item.key}
                  href={item.to}
                  onClick={handleEditorClick}
                  aria-current={active ? 'page' : undefined}
                  className={linkClassName}
                >
                  {t(item.labelKey)}
                </a>
              )
            }

            return (
              <Link
                key={item.key}
                to={item.to}
                aria-current={active ? 'page' : undefined}
                className={linkClassName}
              >
                {t(item.labelKey)}
              </Link>
            )
          })}
        </nav>
      </div>
    </header>
  )
}

export default AppNavBar
