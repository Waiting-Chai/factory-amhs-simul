/**
 * Model store tests.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useModelStore } from './modelStore'
import { modelsApi } from '@api/models'

vi.mock('@api/models', () => ({
  modelsApi: {
    list: vi.fn(),
    getById: vi.fn(),
    upload: vi.fn(),
    update: vi.fn(),
    uploadVersion: vi.fn(),
    setDefaultVersion: vi.fn(),
    enable: vi.fn(),
    disable: vi.fn(),
    delete: vi.fn(),
    getBindings: vi.fn(),
    setBinding: vi.fn(),
  },
}))

describe('ModelStore', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useModelStore.setState({
      models: [],
      currentModel: null,
      modelVersionsByModelId: {},
      bindings: new Map(),
      isLoading: false,
      isUploading: false,
      uploadProgress: 0,
      error: null,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0,
        totalPages: 0,
      },
      searchQuery: '',
      filterType: null,
      filterStatus: null,
    })
  })

  it('shouldPassTypeStatusSearchFiltersToListApi', async () => {
    vi.mocked(modelsApi.list).mockResolvedValue({
      items: [],
      total: 0,
      page: 1,
      pageSize: 20,
      totalPages: 0,
    })

    useModelStore.setState({
      searchQuery: 'oht',
      filterType: 'OHT_VEHICLE',
      filterStatus: 'ACTIVE',
    })

    await useModelStore.getState().fetchModels(2)

    expect(modelsApi.list).toHaveBeenCalledWith(2, 20, 'OHT_VEHICLE', 'ACTIVE', 'oht')
  })
})
