/**
 * Model store using Zustand for model library management state.
 *
 * Implements 5.2: Model & Component Library Module
 * - Model list (type, version, status, thumbnail)
 * - GLB file upload (via backend to MinIO)
 * - Model metadata form (type, version, dimensions, anchor)
 * - Model version management (upload new version, set default)
 * - Device type to model mapping configuration
 * - Model enable/disable
 * - Model transform parameters (scale/rotation/pivot)
 * - Entity-level model binding selector
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import type {
  ModelSummary,
  ModelDetail,
  ModelVersion,
  ModelMetadata,
  ModelUploadResult,
  EntityModelBinding,
} from '../types'
import { modelsApi } from '@api/models'

/**
 * Extract error message from unknown error type
 */
const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message
  }
  return String(error)
}

/**
 * Model store state
 */
interface ModelState {
  // Data
  models: ModelSummary[]
  currentModel: ModelDetail | null
  modelVersionsByModelId: Record<string, ModelVersion[]>
  bindings: Map<string, EntityModelBinding[]>  // sceneId -> bindings array

  // UI State
  isLoading: boolean
  isUploading: boolean
  uploadProgress: number
  error: string | null
  pagination: {
    page: number
    pageSize: number
    total: number
    totalPages: number
  }

  // Filters
  searchQuery: string
  filterType: string | null
  filterStatus: string | null

  // Actions
  fetchModels: (page?: number) => Promise<void>
  fetchAllActiveModelsForEditor: () => Promise<void>
  fetchModel: (id: string) => Promise<void>
  fetchModelVersions: (id: string) => Promise<ModelVersion[]>
  uploadModel: (
    file: File,
    name: string,
    type: string,
    metadata: ModelMetadata
  ) => Promise<ModelUploadResult>
  updateModel: (id: string, data: Partial<ModelDetail>) => Promise<void>
  uploadVersion: (
    id: string,
    file: File,
    metadata: ModelMetadata
  ) => Promise<ModelVersion>
  setDefaultVersion: (id: string, versionId: string) => Promise<void>
  enableModel: (id: string) => Promise<void>
  disableModel: (id: string) => Promise<void>
  deleteModel: (id: string) => Promise<void>

  // Binding actions
  fetchBindings: (sceneId: string) => Promise<void>
  setBinding: (
    sceneId: string,
    entityId: string,
    modelId: string,
    versionId: string
  ) => Promise<void>

  // UI actions
  setSearchQuery: (query: string) => void
  setFilterType: (type: string | null) => void
  setFilterStatus: (status: string | null) => void
  setCurrentModel: (model: ModelDetail | null) => void
  clearError: () => void
}

/**
 * Model store
 */
export const useModelStore = create<ModelState>()(
  devtools(
    (set, get) => ({
      // Initial state
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

      // Fetch model list
      fetchModels: async (page = 1) => {
        set({ isLoading: true, error: null })
        try {
          const state = get()
          const { searchQuery, filterType, filterStatus, pagination } = state
          const response = await modelsApi.list(
            page,
            pagination.pageSize,
            filterType || undefined,
            filterStatus || undefined,
            searchQuery || undefined
          )

          // modelsApi.list now returns 1-based page (converted from 0-based backend)
          set({
            models: response.items,
            pagination: {
              page: response.page,
              pageSize: response.pageSize,
              total: response.total,
              totalPages: response.totalPages,
            },
            isLoading: false,
          })
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to fetch models',
            isLoading: false,
          })
        }
      },

      // Fetch all active models for editor
      // Task B: Removed backend status filter to avoid empty results if backend uses ENABLED
      // and performing client-side filtering instead.
      fetchAllActiveModelsForEditor: async () => {
        set({ isLoading: true, error: null })
        try {
          // Fetch first page without status filter to capture all potential candidates
          const firstPage = await modelsApi.list(1, 100, undefined, undefined, undefined)
          
          let allModels = [...firstPage.items]
          const totalPages = firstPage.totalPages
          
          // If there are more pages, fetch them sequentially
          if (totalPages > 1) {
            for (let p = 2; p <= totalPages; p++) {
              // Safety limit: stop after 10 pages
              if (p > 10) break
              const nextPage = await modelsApi.list(p, 100, undefined, undefined, undefined)
              allModels = [...allModels, ...nextPage.items]
            }
          }
          
          // Filter ACTIVE models client-side to ensure compatibility
          // Compatible with backend returning ENABLED or ACTIVE
          const activeModels = allModels.filter(m => m.status === 'ACTIVE' || (m.status as 'ACTIVE' | 'DISABLED' | 'PENDING' | 'ENABLED') === 'ENABLED')

          // Update models list but KEEP original pagination state to avoid
          // disrupting the ModelLibrary view if user navigates back.
          // Note: In a real app we might want separate 'editorModels' state, 
          // but per requirements we reuse 'models'.
          set(() => ({
            models: activeModels,
            isLoading: false
            // Explicitly NOT updating pagination here to preserve Library state semantics
            // or we could set it to a "virtual" state if needed, but keeping it as is 
            // minimizes side effects on the Library page.
          }))
          
          // Task E: Debug log
          console.debug(`[ModelStore] Loaded ${activeModels.length} active models for editor`)
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to fetch active models',
            isLoading: false,
          })
        }
      },

      // Fetch single model
      fetchModel: async (id: string) => {
        set({ isLoading: true, error: null })
        try {
          const model = await modelsApi.getById(id)
          set({ currentModel: model, isLoading: false })
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to fetch model',
            isLoading: false,
          })
        }
      },

      // Fetch model versions from detail API to ensure real versionId is used.
      fetchModelVersions: async (id: string) => {
        try {
          const model = await modelsApi.getById(id)
          const versions = Array.isArray(model.versions) ? model.versions : []
          set((state) => ({
            modelVersionsByModelId: {
              ...state.modelVersionsByModelId,
              [id]: versions,
            },
          }))
          return versions
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to fetch model versions',
          })
          throw error
        }
      },

      // Upload model (GLB to MinIO)
      uploadModel: async (
        file: File,
        name: string,
        type: string,
        metadata: ModelMetadata
      ) => {
        set({ isUploading: true, uploadProgress: 0, error: null })
        try {
          const result = await modelsApi.upload(
            file,
            name,
            type,
            metadata,
            (progress) => {
              // Update upload progress from API callback
              set({ uploadProgress: progress })
            }
          )

          set({ isUploading: false, uploadProgress: 0 })
          await get().fetchModels() // Refresh list

          return result
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to upload model',
            isUploading: false,
            uploadProgress: 0,
          })
          throw error
        }
      },

      // Update model metadata
      updateModel: async (id: string, data: Partial<ModelDetail>) => {
        set({ isLoading: true, error: null })
        try {
          const model = await modelsApi.update(id, data)
          set({ currentModel: model, isLoading: false })
          await get().fetchModels() // Refresh list
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to update model',
            isLoading: false,
          })
          throw error
        }
      },

      // Upload new version
      uploadVersion: async (
        id: string,
        file: File,
        metadata: ModelMetadata
      ) => {
        set({ isUploading: true, uploadProgress: 0, error: null })
        try {
          const version = await modelsApi.uploadVersion(
            id,
            file,
            metadata,
            (progress) => set({ uploadProgress: progress })
          )

          set({ isUploading: false, uploadProgress: 0 })

          // Refresh current model
          if (get().currentModel?.modelId === id) {
            await get().fetchModel(id)
          }

          return version
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to upload version',
            isUploading: false,
            uploadProgress: 0,
          })
          throw error
        }
      },

      // Set default version
      setDefaultVersion: async (id: string, versionId: string) => {
        set({ isLoading: true, error: null })
        try {
          await modelsApi.setDefaultVersion(id, versionId)
          set({ isLoading: false })

          // Refresh current model
          if (get().currentModel?.modelId === id) {
            await get().fetchModel(id)
          }
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to set default version',
            isLoading: false,
          })
          throw error
        }
      },

      // Enable model
      enableModel: async (id: string) => {
        set({ isLoading: true, error: null })
        try {
          await modelsApi.enable(id)
          set({ isLoading: false })

          // Refresh current model
          if (get().currentModel?.modelId === id) {
            await get().fetchModel(id)
          }
          await get().fetchModels() // Refresh list
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to enable model',
            isLoading: false,
          })
          throw error
        }
      },

      // Disable model
      disableModel: async (id: string) => {
        set({ isLoading: true, error: null })
        try {
          await modelsApi.disable(id)
          set({ isLoading: false })

          // Refresh current model
          if (get().currentModel?.modelId === id) {
            await get().fetchModel(id)
          }
          await get().fetchModels() // Refresh list
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to disable model',
            isLoading: false,
          })
          throw error
        }
      },

      // Delete model
      deleteModel: async (id: string) => {
        set({ isLoading: true, error: null })
        try {
          await modelsApi.delete(id)
          set({ isLoading: false })

          if (get().currentModel?.modelId === id) {
            set({ currentModel: null })
          }
          await get().fetchModels() // Refresh list
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to delete model',
            isLoading: false,
          })
          throw error
        }
      },

      // Fetch entity model bindings for a scene
      fetchBindings: async (sceneId: string) => {
        try {
          const bindings = await modelsApi.getBindings(sceneId)
          set((state) => ({
            bindings: new Map(state.bindings).set(sceneId, bindings)
          }))
        } catch (error: unknown) {
          set({ error: getErrorMessage(error) || 'Failed to fetch bindings' })
        }
      },

      // Set entity model binding
      setBinding: async (
        sceneId: string,
        entityId: string,
        modelId: string,
        versionId: string
      ) => {
        try {
          await modelsApi.setBinding(sceneId, entityId, modelId, versionId)
          await get().fetchBindings(sceneId) // Refresh bindings
        } catch (error: unknown) {
          set({ error: getErrorMessage(error) || 'Failed to set binding' })
          throw error
        }
      },

      // UI actions
      setSearchQuery: (query: string) => {
        set({ searchQuery: query })
        get().fetchModels(1)
      },

      setFilterType: (type: string | null) => {
        set({ filterType: type })
        get().fetchModels(1)
      },

      setFilterStatus: (status: string | null) => {
        set({ filterStatus: status })
        get().fetchModels(1)
      },

      setCurrentModel: (model: ModelDetail | null) => {
        set({ currentModel: model })
      },

      clearError: () => {
        set({ error: null })
      },
    }),
    { name: 'ModelStore' }
  )
)

export default useModelStore
