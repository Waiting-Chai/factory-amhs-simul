/**
 * SceneCreatePage i18n tests.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import SceneCreatePage from './SceneCreatePage'
import { setLocale } from '../../shared/i18n'

const mockCreateScene = vi.fn()
const mockAddToast = vi.fn()

vi.mock('@store/sceneStore', () => ({
  useSceneStore: () => ({
    createScene: mockCreateScene,
    isLoading: false,
  }),
}))

vi.mock('@store/toastStore', () => ({
  useToastStore: () => ({
    addToast: mockAddToast,
  }),
}))

describe('SceneCreatePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setLocale('en')
  })

  it('shouldRenderEnglishTextsByDefault', () => {
    render(
      <MemoryRouter>
        <SceneCreatePage />
      </MemoryRouter>
    )

    expect(screen.getByText('New Scene')).toBeInTheDocument()
    expect(screen.getByText('Create a new simulation scenario')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create Scene' })).toBeInTheDocument()
  })

  it('shouldRenderChineseTextsAfterLocaleSwitch', () => {
    setLocale('zh')

    render(
      <MemoryRouter>
        <SceneCreatePage />
      </MemoryRouter>
    )

    expect(screen.getByText('新建场景')).toBeInTheDocument()
    expect(screen.getByText('创建新的仿真场景')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '创建场景' })).toBeInTheDocument()
  })

  it('shouldUseI18nValidationMessage', async () => {
    render(
      <MemoryRouter>
        <SceneCreatePage />
      </MemoryRouter>
    )

    fireEvent.change(screen.getByPlaceholderText('Enter scene name (min. 3 characters)'), {
      target: { value: 'a' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Create Scene' }))
    expect(screen.getByText('Scene name must be at least 3 characters')).toBeInTheDocument()
  })
})
