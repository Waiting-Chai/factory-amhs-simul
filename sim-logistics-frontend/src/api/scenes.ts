/**
 * Scene API service for sim-logistics-frontend.
 *
 * Implements scene CRUD operations following frontend-contract.md
 * Section 3.1: Scene Module
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import apiClient, { unwrapEnvelope, downloadBlob } from './client'
import type {
  ApiEnvelope,
  ApiError,
  PagedResult,
  SceneSummary,
  SceneDetail,
  SceneCopyResult,
  SceneImportResult,
  SceneDraftPayload,
  SceneDraftSaveResult,
} from '../types'

const stripRuntimeFields = (value: unknown): unknown => {
  if (Array.isArray(value)) {
    return value.map((item) => stripRuntimeFields(item))
  }

  if (!value || typeof value !== 'object') {
    return value
  }

  const source = value as Record<string, unknown>
  const result: Record<string, unknown> = {}

  Object.entries(source).forEach(([key, nested]) => {
    if (key === 'extensions' || key.startsWith('_')) {
      return
    }
    result[key] = stripRuntimeFields(nested)
  })

  return result
}

const sanitizeScenePayload = (data: Partial<SceneDetail>): Record<string, unknown> => {
  return stripRuntimeFields(data) as Record<string, unknown>
}

/**
 * Scene API paths
 */
const PATHS = {
  LIST: '/scenes',
  DETAIL: (id: string) => `/scenes/${id}`,
  COPY: (id: string) => `/scenes/${id}/copy`,
  IMPORT: '/scenes/import',
  EXPORT: (id: string) => `/scenes/${id}/export`,
  DRAFT_GET: (id: string) => `/scenes/${id}/draft`,
  DRAFT_SAVE: (id: string) => `/scenes/${id}/draft`,
  DRAFT_DELETE: (id: string) => `/scenes/${id}/draft`,
} as const

/**
 * Scene API service
 */
export const scenesApi = {
  /**
   * Get scene list with pagination
   * GET /api/v1/scenes
   *
   * Contract alignment:
   * - page: 1-based in frontend, converted to 0-based for backend
   * - search: unified search parameter
   * - type: optional filter by scene type (backend may ignore if not supported)
   */
  async list(
    page: number = 1,
    pageSize: number = 20,
    search?: string,
    type?: string
  ): Promise<PagedResult<SceneSummary>> {
    const params: Record<string, string | number> = {
      // Convert 1-based page to 0-based for backend
      page: page > 0 ? page - 1 : 0,
      pageSize,
    }
    if (search) params.search = search
    if (type) params.type = type

    const response = await apiClient.get<ApiEnvelope<PagedResult<SceneSummary>>>(
      PATHS.LIST,
      { params }
    )
    return unwrapEnvelope(response)
  },

  /**
   * Get scene detail by ID
   * GET /api/v1/scenes/{id}
   */
  async getById(id: string): Promise<SceneDetail> {
    const response = await apiClient.get<ApiEnvelope<SceneDetail>>(PATHS.DETAIL(id))
    return unwrapEnvelope(response)
  },

  /**
   * Create new scene
   * POST /api/v1/scenes
   */
  async create(data: Partial<SceneDetail>): Promise<SceneDetail> {
    const response = await apiClient.post<ApiEnvelope<SceneDetail>>(
      PATHS.LIST,
      data
    )
    return unwrapEnvelope(response)
  },

  /**
   * Update scene
   * PUT /api/v1/scenes/{id}
   */
  async update(id: string, data: Partial<SceneDetail>): Promise<SceneDetail> {
    const response = await apiClient.put<ApiEnvelope<SceneDetail>>(
      PATHS.DETAIL(id),
      sanitizeScenePayload(data)
    )
    return unwrapEnvelope(response)
  },

  /**
   * Delete scene
   * DELETE /api/v1/scenes/{id} -> 204
   */
  async delete(id: string): Promise<void> {
    await apiClient.delete(PATHS.DETAIL(id))
  },

  /**
   * Copy scene
   * POST /api/v1/scenes/{id}/copy
   */
  async copy(id: string, name?: string): Promise<SceneCopyResult> {
    const response = await apiClient.post<ApiEnvelope<SceneCopyResult>>(
      PATHS.COPY(id),
      { name }
    )
    return unwrapEnvelope(response)
  },

  /**
   * Import scene from JSON file
   * POST /api/v1/scenes/import
   *
   * Note: Uses FormData for file upload, returns ApiEnvelope<SceneImportResult>
   */
  async import(file: File): Promise<SceneImportResult> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await apiClient.post<ApiEnvelope<SceneImportResult>>(
      PATHS.IMPORT,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    )
    return unwrapEnvelope(response)
  },

  /**
   * Export scene to JSON file
   * GET /api/v1/scenes/{id}/export -> blob
   *
   * Note: This endpoint returns blob directly without envelope wrapper
   */
  async exportScene(id: string, filename?: string): Promise<void> {
    // Use provided filename or generate default
    const defaultFilename = `scene_${id}_${new Date().getTime()}.json`
    await downloadBlob(PATHS.EXPORT(id), filename || defaultFilename)
  },

  /**
   * Get scene draft
   * GET /api/v1/scenes/{id}/draft
   *
   * Returns 404 if no draft exists -> null (this is expected behavior)
   * Throws error for non-404 errors (network, server error, etc.)
   */
  async getDraft(id: string): Promise<SceneDraftPayload | null> {
    try {
      const response = await apiClient.get<ApiEnvelope<SceneDraftPayload>>(
        PATHS.DRAFT_GET(id)
      )
      return unwrapEnvelope(response)
    } catch (error: unknown) {
      const err = error as ApiError

      // Check for 404 status code (no draft exists)
      // Supports multiple detection methods for backend compatibility:
      // 1. status === 404 (HTTP status from ApiError)
      // 2. code === 'NOT_FOUND' (backend error code)
      const is404 = err.status === 404 || err.code === 'NOT_FOUND'

      if (is404) {
        // 404 means no draft exists - return null (expected behavior)
        return null
      }
      // Non-404 errors should be thrown
      throw error
    }
  },

  /**
   * Save scene draft (auto-save)
   * POST /api/v1/scenes/{id}/draft
   */
  async saveDraft(id: string, content: SceneDetail): Promise<SceneDraftSaveResult> {
    const payload: SceneDraftPayload = {
      sceneId: id,
      content,
      savedAt: new Date().toISOString(),
      version: content.version,
    }

    const response = await apiClient.post<ApiEnvelope<SceneDraftSaveResult>>(
      PATHS.DRAFT_SAVE(id),
      payload
    )
    return unwrapEnvelope(response)
  },

  /**
   * Delete scene draft
   * DELETE /api/v1/scenes/{id}/draft -> 204
   */
  async deleteDraft(id: string): Promise<void> {
    await apiClient.delete(PATHS.DRAFT_DELETE(id))
  },
}

export default scenesApi
