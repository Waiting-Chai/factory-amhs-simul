/**
 * AppNavBar tests.
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-11
 */
import { describe, expect, it, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { act } from 'react'
import { MemoryRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppNavBar } from './AppNavBar'
import { getLocale, setLocale } from '../shared/i18n'

const LAST_SCENE_ID_KEY = 'lastSceneId'

const renderWithRouter = (initialPath: string) => {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <AppNavBar />
      <Routes>
        <Route path="/" element={<Navigate to="/scenes" replace />} />
        <Route path="/scenes" element={<div>Scenes Page</div>} />
        <Route path="/scenes/new" element={<div>Scene Create Page</div>} />
        <Route path="/scenes/:id/edit" element={<div>Scene Edit Page</div>} />
        <Route path="/models" element={<div>Model Library Page</div>} />
        <Route path="/simulation" element={<div>Simulation Page</div>} />
        <Route path="/config" element={<div>Config Page</div>} />
      </Routes>
    </MemoryRouter>
  )
}

describe('AppNavBar', () => {
  beforeEach(() => {
    localStorage.clear()
    setLocale('en')
    vi.clearAllMocks()
  })

  it('shouldRenderEnglishNavigationEntriesByDefault', () => {
    renderWithRouter('/scenes')

    expect(getLocale()).toBe('en')
    expect(screen.getByRole('link', { name: 'Scenes' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Scene Editor' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Models' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Simulation' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Config' })).toBeInTheDocument()
  })

  it('shouldHighlightSceneEditorWhenRouteIsSceneEdit', () => {
    renderWithRouter('/scenes/scene-1/edit')

    // Scene Editor should be active on /scenes/:id/edit
    expect(screen.getByRole('link', { name: 'Scene Editor' })).toHaveAttribute('aria-current', 'page')
    // Scenes should NOT be active on /scenes/:id/edit
    expect(screen.getByRole('link', { name: 'Scenes' })).not.toHaveAttribute('aria-current', 'page')
  })

  it('shouldNotHighlightSceneEditorWhenRouteIsScenesNew', () => {
    renderWithRouter('/scenes/new')

    // Scene Editor should NOT be active on /scenes/new
    expect(screen.getByRole('link', { name: 'Scene Editor' })).not.toHaveAttribute('aria-current', 'page')
    // Scenes should be active on /scenes/new
    expect(screen.getByRole('link', { name: 'Scenes' })).toHaveAttribute('aria-current', 'page')
  })

  it('shouldNotHighlightSceneEditorWhenRouteIsScenes', () => {
    renderWithRouter('/scenes')

    // Scene Editor should NOT be active on /scenes
    expect(screen.getByRole('link', { name: 'Scene Editor' })).not.toHaveAttribute('aria-current', 'page')
    // Scenes should be active on /scenes
    expect(screen.getByRole('link', { name: 'Scenes' })).toHaveAttribute('aria-current', 'page')
  })

  it('shouldNavigateToLastSceneEditWhenClickingSceneEditorWithLastSceneId', async () => {
    const user = userEvent.setup()
    localStorage.setItem(LAST_SCENE_ID_KEY, 'test-scene-123')
    renderWithRouter('/models')

    await act(async () => {
      await user.click(screen.getByRole('link', { name: 'Scene Editor' }))
    })
    expect(await screen.findByText('Scene Edit Page')).toBeInTheDocument()
  })

  it('shouldNavigateToScenesWhenClickingSceneEditorWithoutLastSceneId', async () => {
    const user = userEvent.setup()
    renderWithRouter('/models')

    await act(async () => {
      await user.click(screen.getByRole('link', { name: 'Scene Editor' }))
    })
    expect(await screen.findByText('Scenes Page')).toBeInTheDocument()
  })

  it('shouldRedirectRootToScenes', async () => {
    renderWithRouter('/')

    expect(await screen.findByText('Scenes Page')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Scenes' })).toHaveAttribute('aria-current', 'page')
  })

  it('shouldSwitchNavLabelsWhenLocaleChangesToZh', async () => {
    renderWithRouter('/scenes')
    expect(screen.getByRole('link', { name: 'Scenes' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Scene Editor' })).toBeInTheDocument()

    await act(async () => {
      setLocale('zh')
    })

    expect(screen.getByRole('link', { name: '场景列表' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '场景编辑器' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '模型库' })).toBeInTheDocument()
  })

  it('shouldNavigateToTargetRoutesWhenClickingNavItems', async () => {
    const user = userEvent.setup()
    renderWithRouter('/scenes')

    await act(async () => {
      await user.click(screen.getByRole('link', { name: 'Models' }))
    })
    expect(await screen.findByText('Model Library Page')).toBeInTheDocument()

    await act(async () => {
      await user.click(screen.getByRole('link', { name: 'Config' }))
    })
    expect(await screen.findByText('Config Page')).toBeInTheDocument()
  })
})
