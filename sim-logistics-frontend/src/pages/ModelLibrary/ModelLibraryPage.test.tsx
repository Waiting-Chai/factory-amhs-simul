/**
 * ModelLibraryPage tests.
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-11
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import ModelLibraryPage from './ModelLibraryPage'
import { setLocale } from '../../shared/i18n'

const mockStore = {
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
  pagination: { page: 1, pageSize: 20, total: 1, totalPages: 1 },
  isLoading: false,
  error: null,
  searchQuery: '',
  filterType: null,
  filterStatus: null,
  isUploading: false,
  uploadProgress: 0,
  currentModel: null,
  modelVersionsByModelId: {},
  bindings: new Map(),
  fetchModels: vi.fn().mockResolvedValue(undefined),
  fetchModel: vi.fn().mockResolvedValue(undefined),
  uploadModel: vi.fn().mockResolvedValue(undefined),
  uploadVersion: vi.fn().mockResolvedValue(undefined),
  setDefaultVersion: vi.fn().mockResolvedValue(undefined),
  enableModel: vi.fn().mockResolvedValue(undefined),
  disableModel: vi.fn().mockResolvedValue(undefined),
  deleteModel: vi.fn().mockResolvedValue(undefined),
  setSearchQuery: vi.fn(),
  setFilterType: vi.fn(),
  setFilterStatus: vi.fn(),
  clearError: vi.fn(),
  fetchBindings: vi.fn().mockResolvedValue(undefined),
  fetchModelVersions: vi.fn().mockResolvedValue([
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
  ]),
  setBinding: vi.fn().mockResolvedValue(undefined),
}

const mockSceneStore = {
  scenes: [
    {
      sceneId: 'scene-1',
      name: 'Scene One',
      description: 'scene',
      version: 1,
      entityCount: 1,
      createdAt: '2026-02-11T00:00:00',
      updatedAt: '2026-02-11T00:00:00',
    },
  ],
  fetchScenes: vi.fn().mockResolvedValue(undefined),
}

vi.mock('@store/modelStore', () => ({
  useModelStore: vi.fn(() => mockStore),
}))

vi.mock('@store/sceneStore', () => ({
  useSceneStore: vi.fn(() => mockSceneStore),
}))

vi.mock('@api/scenes', () => ({
  scenesApi: {
    getById: vi.fn(() => Promise.resolve({
      sceneId: 'scene-1',
      name: 'Scene One',
      version: 1,
      createdAt: '2026-02-11T00:00:00',
      updatedAt: '2026-02-11T00:00:00',
      entities: [
        {
          id: 'entity-1',
          name: 'Entity One',
          type: 'MACHINE',
          position: { x: 0, y: 0, z: 0 },
          properties: {},
        },
      ],
      paths: [],
    })),
  },
}))

describe('ModelLibraryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    setLocale('en')
  })

  it('shouldRenderDefaultEnglishTextsFor5_2Page', () => {
    render(<ModelLibraryPage />)

    expect(screen.getByText('Model Library')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Upload Model' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Search models...')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entity Binding' })).toBeInTheDocument()
    expect(screen.getByText('Manage 3D models for simulation entities')).toBeInTheDocument()
  })

  it('shouldSwitchKeyTextsToZhWhenLocaleChanges', async () => {
    setLocale('zh')

    render(<ModelLibraryPage />)

    expect(screen.getByText('模型库')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '上传模型' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('搜索模型...')).toBeInTheDocument()

    fireEvent.click(screen.getByTestId('toggle-binding-panel'))

    await waitFor(() => {
      expect(screen.getByText('实体级模型绑定')).toBeInTheDocument()
      expect(screen.getByText('场景')).toBeInTheDocument()
    })
  })

  it('shouldOpenBindingPanelAndSaveBinding', async () => {
    render(<ModelLibraryPage />)

    fireEvent.click(screen.getByTestId('toggle-binding-panel'))

    await waitFor(() => {
      expect(screen.getByText('Entity-Level Model Binding')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByTestId('binding-scene-select'), { target: { value: 'scene-1' } })

    await waitFor(() => {
      expect(screen.getByTestId('binding-entity-select')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByTestId('binding-entity-select'), { target: { value: 'entity-1' } })
    fireEvent.change(screen.getByTestId('binding-model-select'), { target: { value: 'model-1' } })

    await waitFor(() => {
      expect(mockStore.fetchModelVersions).toHaveBeenCalledWith('model-1')
    })

    fireEvent.click(screen.getByTestId('binding-save-button'))

    await waitFor(() => {
      expect(mockStore.setBinding).toHaveBeenCalledWith('scene-1', 'entity-1', 'model-1', 'real-version-1')
    })
  })

  it('shouldBlockUploadWhenFileExceeds100MB', async () => {
    const { container } = render(<ModelLibraryPage />)

    fireEvent.click(screen.getByRole('button', { name: 'Upload Model' }))

    const fileInput = container.querySelector('input[type="file"][accept=".glb"]') as HTMLInputElement
    const oversizedFile = new File(['x'], 'too-large.glb', { type: 'model/gltf-binary' })
    Object.defineProperty(oversizedFile, 'size', { value: 100 * 1024 * 1024 + 1 })

    fireEvent.change(fileInput, { target: { files: [oversizedFile] } })
    fireEvent.change(screen.getByPlaceholderText('e.g., OHT-Crane-v1'), { target: { value: 'Large Model' } })
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }))

    await waitFor(() => {
      expect(screen.getByText('Selected file exceeds maximum allowed size (100MB).')).toBeInTheDocument()
    })
    expect(mockStore.uploadModel).not.toHaveBeenCalled()
  })

  it('shouldShowFriendlyMessageWhenBackendReturnsFileTooLarge', async () => {
    mockStore.uploadModel.mockRejectedValueOnce({
      code: 'FILE_TOO_LARGE',
      status: 413,
      message: 'Uploaded file exceeds maximum allowed size: 100MB',
    })

    const { container } = render(<ModelLibraryPage />)

    fireEvent.click(screen.getByRole('button', { name: 'Upload Model' }))

    const fileInput = container.querySelector('input[type="file"][accept=".glb"]') as HTMLInputElement
    const validFile = new File(['small'], 'ok.glb', { type: 'model/gltf-binary' })

    fireEvent.change(fileInput, { target: { files: [validFile] } })
    fireEvent.change(screen.getByPlaceholderText('e.g., OHT-Crane-v1'), { target: { value: 'Normal Model' } })
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }))

    await waitFor(() => {
      expect(screen.getByText('Upload failed: file exceeds server limit (100MB).')).toBeInTheDocument()
    })
  })

  it('shouldSendTransformAsObjectInUploadPayload', async () => {
    const { container } = render(<ModelLibraryPage />)

    fireEvent.click(screen.getByRole('button', { name: 'Upload Model' }))

    const fileInput = container.querySelector('input[type="file"][accept=".glb"]') as HTMLInputElement
    const validFile = new File(['small'], 'ok.glb', { type: 'model/gltf-binary' })

    fireEvent.change(fileInput, { target: { files: [validFile] } })
    fireEvent.change(screen.getByPlaceholderText('e.g., OHT-Crane-v1'), { target: { value: 'Transform Model' } })
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }))

    await waitFor(() => {
      expect(mockStore.uploadModel).toHaveBeenCalled()
    })

    const payload = mockStore.uploadModel.mock.calls[0][3]
    expect(payload.transform.scale).toEqual({ x: 1, y: 1, z: 1 })
    expect(payload.transform.rotation).toEqual({ x: 0, y: 0, z: 0 })
    expect(payload.transform.pivot).toEqual({ x: 0, y: 0, z: 0 })
  })

  it('shouldShowFriendlyMessageWhenBackendReturnsBadRequest', async () => {
    mockStore.uploadModel.mockRejectedValueOnce({
      code: 'BAD_REQUEST',
      status: 400,
      message: 'metadata/transform格式错误',
    })

    const { container } = render(<ModelLibraryPage />)

    fireEvent.click(screen.getByRole('button', { name: 'Upload Model' }))

    const fileInput = container.querySelector('input[type="file"][accept=".glb"]') as HTMLInputElement
    const validFile = new File(['small'], 'ok.glb', { type: 'model/gltf-binary' })

    fireEvent.change(fileInput, { target: { files: [validFile] } })
    fireEvent.change(screen.getByPlaceholderText('e.g., OHT-Crane-v1'), { target: { value: 'Bad Request Model' } })
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }))

    await waitFor(() => {
      expect(screen.getByText('Upload failed: metadata.transform format is invalid. Please use {x, y, z} object fields.')).toBeInTheDocument()
    })
  })
})
