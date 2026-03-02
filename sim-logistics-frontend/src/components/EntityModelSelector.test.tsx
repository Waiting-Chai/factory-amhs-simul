/**
 * EntityModelSelector tests.
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-11
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { EntityModelSelector } from './EntityModelSelector'
import { setLocale } from '../shared/i18n'

const mockFetchBindings = vi.fn()
const mockFetchModelVersions = vi.fn()
const mockSetBinding = vi.fn()
const mockFetchScenes = vi.fn()

const modelStoreState = {
  models: [
    {
      modelId: 'model-1',
      name: 'OHT Model',
      type: 'OHT_VEHICLE',
      defaultVersion: '1.0.0',
      status: 'ACTIVE',
      versions: [],
      createdAt: '2026-02-11T00:00:00',
      updatedAt: '2026-02-11T00:00:00',
    },
  ],
  bindings: new Map<string, Array<{ entityId: string, modelId: string, versionId: string }>>(),
  modelVersionsByModelId: {},
  fetchBindings: mockFetchBindings,
  fetchModelVersions: mockFetchModelVersions,
  setBinding: mockSetBinding,
}

const sceneStoreState = {
  scenes: [
    {
      sceneId: 'scene-1',
      name: 'Scene One',
      description: 'scene-1',
      version: 1,
      entityCount: 1,
      createdAt: '2026-02-11T00:00:00',
      updatedAt: '2026-02-11T00:00:00',
    },
    {
      sceneId: 'scene-2',
      name: 'Scene Two',
      description: 'scene-2',
      version: 1,
      entityCount: 1,
      createdAt: '2026-02-11T00:00:00',
      updatedAt: '2026-02-11T00:00:00',
    },
  ],
  fetchScenes: mockFetchScenes,
}

vi.mock('@store/modelStore', () => ({
  useModelStore: vi.fn(() => modelStoreState),
}))

vi.mock('@store/sceneStore', () => ({
  useSceneStore: vi.fn(() => sceneStoreState),
}))

vi.mock('@api/scenes', () => ({
  scenesApi: {
    getById: vi.fn((sceneId: string) => Promise.resolve({
      sceneId,
      name: sceneId === 'scene-2' ? 'Scene Two' : 'Scene One',
      version: 1,
      createdAt: '2026-02-11T00:00:00',
      updatedAt: '2026-02-11T00:00:00',
      entities: [
        {
          id: sceneId === 'scene-2' ? 'entity-2' : 'entity-1',
          name: sceneId === 'scene-2' ? 'Entity Two' : 'Entity One',
          type: 'MACHINE',
          position: { x: 0, y: 0, z: 0 },
          properties: {},
        },
      ],
      paths: [],
    })),
  },
}))

describe('EntityModelSelector', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setLocale('en')
    modelStoreState.bindings = new Map()
    mockFetchScenes.mockResolvedValue(undefined)
    mockFetchModelVersions.mockResolvedValue([
      {
        versionId: 'real-version-1',
        version: '1.0.0',
        isDefault: true,
        status: 'ACTIVE',
        fileSize: 100,
        fileUrl: '/file.glb',
        metadata: {
          type: 'OHT_VEHICLE',
          version: '1.0.0',
          dimensions: { width: 1, height: 1, depth: 1 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        },
        createdAt: '2026-02-11T00:00:00',
      },
    ])
    mockSetBinding.mockResolvedValue(undefined)
  })

  it('shouldRenderI18nTextInEnglishByDefault', () => {
    render(<EntityModelSelector />)

    expect(screen.getByText('Scene')).toBeInTheDocument()
    expect(screen.getByText('Select a scene to configure entity bindings')).toBeInTheDocument()
  })

  it('shouldRenderI18nTextInChineseWhenLocaleIsZh', () => {
    setLocale('zh')
    render(<EntityModelSelector />)

    expect(screen.getByText('场景')).toBeInTheDocument()
    expect(screen.getByText('请选择场景以配置实体绑定')).toBeInTheDocument()
  })

  it('shouldFetchBindingsOnlyOncePerSceneSelection', async () => {
    mockFetchBindings.mockImplementation(async (sceneId: string) => {
      modelStoreState.bindings = new Map([[sceneId, []]])
    })

    const { rerender } = render(<EntityModelSelector />)

    fireEvent.change(screen.getByTestId('binding-scene-select'), { target: { value: 'scene-1' } })

    await waitFor(() => {
      expect(mockFetchBindings).toHaveBeenCalledTimes(1)
      expect(mockFetchBindings).toHaveBeenCalledWith('scene-1')
    })

    rerender(<EntityModelSelector />)

    await waitFor(() => {
      expect(mockFetchBindings).toHaveBeenCalledTimes(1)
    })
  })

  it('shouldRefreshBindingsWhenSceneChanges', async () => {
    mockFetchBindings.mockImplementation(async (sceneId: string) => {
      if (sceneId === 'scene-1') {
        modelStoreState.bindings = new Map([[
          'scene-1',
          [{ entityId: 'entity-1', modelId: 'model-1', versionId: 'version-scene-1' }],
        ]])
        return
      }
      modelStoreState.bindings = new Map([[
        'scene-2',
        [{ entityId: 'entity-2', modelId: 'model-1', versionId: 'version-scene-2' }],
      ]])
    })

    const { rerender } = render(<EntityModelSelector />)

    fireEvent.change(screen.getByTestId('binding-scene-select'), { target: { value: 'scene-1' } })

    await waitFor(() => {
      expect(mockFetchBindings).toHaveBeenCalledWith('scene-1')
    })

    fireEvent.change(screen.getByTestId('binding-scene-select'), { target: { value: 'scene-2' } })

    await waitFor(() => {
      expect(mockFetchBindings).toHaveBeenCalledWith('scene-2')
      expect(mockFetchBindings).toHaveBeenCalledTimes(2)
    })

    rerender(<EntityModelSelector />)
    fireEvent.change(screen.getByTestId('binding-entity-select'), { target: { value: 'entity-2' } })

    await waitFor(() => {
      expect(screen.getByText(/Version ID: version-scene-2/)).toBeInTheDocument()
    })
  })

  it('shouldUseLatestBindingsFromStoreAfterFetch', async () => {
    mockFetchBindings.mockImplementation(async () => {
      modelStoreState.bindings = new Map([[
        'scene-1',
        [{ entityId: 'entity-1', modelId: 'model-1', versionId: 'latest-version-1' }],
      ]])
    })

    const { rerender } = render(<EntityModelSelector />)

    fireEvent.change(screen.getByTestId('binding-scene-select'), { target: { value: 'scene-1' } })

    await waitFor(() => {
      expect(mockFetchBindings).toHaveBeenCalledWith('scene-1')
    })

    rerender(<EntityModelSelector />)
    fireEvent.change(screen.getByTestId('binding-entity-select'), { target: { value: 'entity-1' } })

    await waitFor(() => {
      expect(screen.getByText(/Version ID: latest-version-1/)).toBeInTheDocument()
    })
  })

  it('shouldUseRealVersionIdWhenSavingBinding', async () => {
    mockFetchBindings.mockImplementation(async (sceneId: string) => {
      modelStoreState.bindings = new Map([[sceneId, []]])
    })

    render(<EntityModelSelector />)

    fireEvent.change(screen.getByTestId('binding-scene-select'), { target: { value: 'scene-1' } })

    await waitFor(() => {
      expect(screen.getByTestId('binding-entity-select')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByTestId('binding-entity-select'), { target: { value: 'entity-1' } })
    fireEvent.change(screen.getByTestId('binding-model-select'), { target: { value: 'model-1' } })

    await waitFor(() => {
      expect(mockFetchModelVersions).toHaveBeenCalledWith('model-1')
    })

    fireEvent.click(screen.getByTestId('binding-save-button'))

    await waitFor(() => {
      expect(mockSetBinding).toHaveBeenCalledWith('scene-1', 'entity-1', 'model-1', 'real-version-1')
    })
  })
})
