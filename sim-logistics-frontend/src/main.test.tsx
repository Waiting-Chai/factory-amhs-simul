/**
 * App route and placeholder i18n tests.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { App } from './main'
import { setLocale } from './shared/i18n'

vi.mock('./pages', () => ({
  SceneListPage: () => <div>Scene List Page</div>,
  SceneCreatePage: () => <div>Scene Create Page</div>,
  SceneEditorPage: () => <div>Scene Editor Page</div>,
  ModelLibraryPage: () => <div>Model Library Page</div>,
}))

vi.mock('./components', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./components')>()
  return {
    ...actual,
    AppNavBar: () => <div>Nav</div>,
    ToastContainer: () => <div>Toast</div>,
  }
})

describe('App placeholders i18n', () => {
  beforeEach(() => {
    localStorage.clear()
    setLocale('en')
  })

  it('shouldRedirectRootToScenes', async () => {
    window.history.pushState({}, '', '/')
    render(<App />)
    expect(await screen.findByText('Scene List Page')).toBeInTheDocument()
  })

  it('shouldRenderSimulationAndConfigPlaceholdersFromI18n', () => {
    window.history.pushState({}, '', '/simulation')
    const { unmount } = render(<App />)
    expect(screen.getByText('Simulation Viewer - Coming Soon')).toBeInTheDocument()
    unmount()

    window.history.pushState({}, '', '/config')
    render(<App />)
    expect(screen.getByText('Config Center - Coming Soon')).toBeInTheDocument()
  })
})
