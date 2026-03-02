/**
 * SceneListPage rendering tests.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import SceneListPage from './SceneListPage'
import { useSceneStore } from '@store/sceneStore'

vi.mock('@store/sceneStore', () => ({
  useSceneStore: vi.fn(),
}))

describe('SceneListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shouldRenderSceneListWithoutCrashWhenScenesUndefined', () => {
    vi.mocked(useSceneStore).mockReturnValue({
      scenes: undefined,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0,
        totalPages: 1,
      },
      isLoading: false,
      error: null,
      searchQuery: '',
      filterType: null,
      fetchScenes: vi.fn(),
      deleteScene: vi.fn(),
      copyScene: vi.fn(),
      exportScene: vi.fn(),
      importScene: vi.fn(),
      setSearchQuery: vi.fn(),
      setFilterType: vi.fn(),
      clearError: vi.fn(),
    } as never)

    expect(() => {
      render(
        <MemoryRouter>
          <SceneListPage />
        </MemoryRouter>
      )
    }).not.toThrow()

    expect(screen.getByText('No scenes found')).toBeInTheDocument()
  })
})
