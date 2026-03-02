/**
 * Mock API handlers for sim-logistics-frontend.
 *
 * Implements scene and model API handlers aligned with docs/frontend-contract.md
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import type {
  ApiEnvelope,
  PagedResult,
  SceneSummary,
  SceneDetail,
  SceneCopyResult,
  SceneImportResult,
  SceneDraftPayload,
  SceneDraftSaveResult,
  ModelSummary,
  ModelDetail,
  ModelVersion,
  ModelUploadResult,
  ModelMetadata,
  EntityModelBinding,
} from '../types'
import { mockStore } from '../data'
import { randomDelay } from '../config'

/**
 * Version with isDefault flag
 */
type VersionWithDefault = ModelVersion & { isDefault?: boolean }

/**
 * Simulate async API call with delay
 */
const simulateAsync = async <T>(data: T, delay?: number): Promise<T> => {
  const ms = delay ?? randomDelay()
  await new Promise((resolve) => setTimeout(resolve, ms))
  return data
}

/**
 * Create success envelope
 */
const success = <T>(data: T): ApiEnvelope<T> => ({
  code: 'OK',
  message: 'success',
  data,
  traceId: `mock-trace-${Date.now()}`,
})

/**
 * Scene API handlers
 */
export const sceneHandlers = {
  /**
   * GET /api/v1/scenes
   * Query params: page, pageSize, search, type
   */
  list: async (params: {
    page?: number
    pageSize?: number
    search?: string
    type?: string
  }): Promise<ApiEnvelope<PagedResult<SceneSummary>>> => {
    const page = params.page ?? 1
    const pageSize = params.pageSize ?? 20
    const search = params.search?.toLowerCase()
    const type = params.type

    let filtered = [...mockStore.scenes]

    // Apply search filter
    if (search) {
      filtered = filtered.filter((s) =>
        s.name.toLowerCase().includes(search) ||
        s.description?.toLowerCase().includes(search)
      )
    }

    // Apply type filter (if we had type field on SceneSummary)
    // For now, just filter by name pattern
    if (type) {
      filtered = filtered.filter((s) =>
        s.name.toLowerCase().includes(type.toLowerCase())
      )
    }

    // Apply pagination
    const total = filtered.length
    const totalPages = Math.ceil(total / pageSize)
    const start = (page - 1) * pageSize
    const items = filtered.slice(start, start + pageSize)

    return simulateAsync(success({
      items,
      total,
      page,
      pageSize,
      totalPages,
    }))
  },

  /**
   * GET /api/v1/scenes/{id}
   */
  getById: async (id: string): Promise<ApiEnvelope<SceneDetail>> => {
    const detail = mockStore.sceneDetails[id]
    if (!detail) {
      // Return a default detail if not found
      const summary = mockStore.scenes.find((s) => s.sceneId === id)
      if (!summary) {
        throw new Error('Scene not found')
      }
      return simulateAsync(success({
        ...summary,
        entities: [],
        paths: [],
        processFlows: [],
      }))
    }
    return simulateAsync(success(detail))
  },

  /**
   * POST /api/v1/scenes
   */
  create: async (data: Partial<SceneDetail>): Promise<ApiEnvelope<SceneDetail>> => {
    const newScene: SceneDetail = {
      sceneId: `scene-${Date.now()}`,
      name: data.name || 'Untitled Scene',
      description: data.description,
      version: 1,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      entities: data.entities || [],
      paths: data.paths || [],
      processFlows: data.processFlows || [],
    }

    const summary: SceneSummary = {
      sceneId: newScene.sceneId,
      name: newScene.name,
      description: newScene.description,
      version: newScene.version,
      createdAt: newScene.createdAt,
      updatedAt: newScene.updatedAt,
      entityCount: newScene.entities.length,
    }

    mockStore.scenes.unshift(summary)
    mockStore.sceneDetails[newScene.sceneId] = newScene

    return simulateAsync(success(newScene))
  },

  /**
   * PUT /api/v1/scenes/{id}
   */
  update: async (id: string, data: Partial<SceneDetail>): Promise<ApiEnvelope<SceneDetail>> => {
    const existing = mockStore.sceneDetails[id]
    if (!existing) {
      throw new Error('Scene not found')
    }

    const updated: SceneDetail = {
      ...existing,
      ...data,
      sceneId: id, // Preserve ID
      version: existing.version + 1,
      updatedAt: new Date().toISOString(),
    }

    mockStore.sceneDetails[id] = updated

    // Update summary in list
    const summaryIndex = mockStore.scenes.findIndex((s) => s.sceneId === id)
    if (summaryIndex >= 0) {
      mockStore.scenes[summaryIndex] = {
        sceneId: updated.sceneId,
        name: updated.name,
        description: updated.description,
        version: updated.version,
        createdAt: updated.createdAt,
        updatedAt: updated.updatedAt,
        entityCount: updated.entities.length,
      }
    }

    return simulateAsync(success(updated))
  },

  /**
   * DELETE /api/v1/scenes/{id}
   * Returns void (204)
   */
  delete: async (id: string): Promise<void> => {
    const index = mockStore.scenes.findIndex((s) => s.sceneId === id)
    if (index < 0) {
      throw new Error('Scene not found')
    }

    mockStore.scenes.splice(index, 1)
    delete mockStore.sceneDetails[id]
    mockStore.drafts.delete(id)

    return simulateAsync(undefined, 200)
  },

  /**
   * POST /api/v1/scenes/{id}/copy
   */
  copy: async (id: string, name?: string): Promise<ApiEnvelope<SceneCopyResult>> => {
    const existing = mockStore.sceneDetails[id]
    if (!existing) {
      throw new Error('Scene not found')
    }

    const newSceneId = `scene-copy-${Date.now()}`
    const copyName = name || `${existing.name} (Copy)`

    const copied: SceneDetail = {
      ...JSON.parse(JSON.stringify(existing)),
      sceneId: newSceneId,
      name: copyName,
      version: 1,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }

    const summary: SceneSummary = {
      sceneId: copied.sceneId,
      name: copied.name,
      description: copied.description,
      version: copied.version,
      createdAt: copied.createdAt,
      updatedAt: copied.updatedAt,
      entityCount: copied.entities.length,
    }

    mockStore.scenes.unshift(summary)
    mockStore.sceneDetails[newSceneId] = copied

    return simulateAsync(success({
      newSceneId,
      name: copyName,
      version: 1,
    }))
  },

  /**
   * POST /api/v1/scenes/import
   */
  import: async (file: File): Promise<ApiEnvelope<SceneImportResult>> => {
    // Simulate file processing
    await new Promise((resolve) => setTimeout(resolve, 500))

    const newSceneId = `scene-import-${Date.now()}`
    const sceneName = file.name.replace('.json', '')

    const newScene: SceneDetail = {
      sceneId: newSceneId,
      name: sceneName,
      description: 'Imported from file',
      version: 1,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      entities: [],
      paths: [],
      processFlows: [],
    }

    const summary: SceneSummary = {
      sceneId: newScene.sceneId,
      name: newScene.name,
      description: newScene.description,
      version: newScene.version,
      createdAt: newScene.createdAt,
      updatedAt: newScene.updatedAt,
      entityCount: newScene.entities.length,
    }

    mockStore.scenes.unshift(summary)
    mockStore.sceneDetails[newSceneId] = newScene

    return simulateAsync(success({
      sceneId: newSceneId,
      name: sceneName,
      version: 1,
      warnings: [],
    }))
  },

  /**
   * GET /api/v1/scenes/{id}/draft
   * Returns null if no draft exists (frontend contract: 404 -> null)
   */
  getDraft: async (id: string): Promise<ApiEnvelope<SceneDraftPayload> | null> => {
    const draft = mockStore.drafts.get(id) as SceneDraftPayload | undefined

    if (!draft) {
      // Return null for missing draft (frontend contract: 404 -> null)
      return Promise.resolve(null)
    }

    return simulateAsync(success(draft))
  },

  /**
   * POST /api/v1/scenes/{id}/draft
   */
  saveDraft: async (id: string, content: SceneDetail): Promise<ApiEnvelope<SceneDraftSaveResult>> => {
    const draft: SceneDraftPayload = {
      sceneId: id,
      content,
      savedAt: new Date().toISOString(),
      version: content.version,
    }

    mockStore.drafts.set(id, draft)

    return simulateAsync(success({
      success: true,
      savedAt: draft.savedAt,
    }))
  },

  /**
   * DELETE /api/v1/scenes/{id}/draft
   */
  deleteDraft: async (id: string): Promise<void> => {
    mockStore.drafts.delete(id)
    return simulateAsync(undefined, 200)
  },

  /**
   * GET /api/v1/scenes/{id}/export
   * Returns blob (simulated)
   */
  exportScene: async (id: string): Promise<Blob> => {
    const scene = mockStore.sceneDetails[id]
    if (!scene) {
      throw new Error('Scene not found')
    }

    // Return a real Blob (not wrapped in simulateAsync)
    const json = JSON.stringify(scene, null, 2)
    return Promise.resolve(new Blob([json], { type: 'application/json' }))
  },
}

/**
 * Model API handlers
 */
export const modelHandlers = {
  /**
   * GET /api/v1/models
   */
  list: async (params: {
    page?: number
    pageSize?: number
    type?: string
    status?: string
    search?: string
  }): Promise<ApiEnvelope<PagedResult<ModelSummary>>> => {
    const page = params.page ?? 1
    const pageSize = params.pageSize ?? 20
    const search = params.search?.toLowerCase()
    const type = params.type
    const status = params.status

    let filtered = [...mockStore.models]

    // Apply search filter
    if (search) {
      filtered = filtered.filter((m) =>
        m.name.toLowerCase().includes(search)
      )
    }

    // Apply type filter
    if (type) {
      filtered = filtered.filter((m) => m.type === type)
    }

    // Apply status filter
    if (status) {
      filtered = filtered.filter((m) => {
        const hasDefaultVersion = mockStore.modelDetails[m.modelId]?.versions.some(
          (v: VersionWithDefault) => v.isDefault && v.status === status
        )
        return hasDefaultVersion
      })
    }

    // Apply pagination
    const total = filtered.length
    const totalPages = Math.ceil(total / pageSize)
    const start = (page - 1) * pageSize
    const items = filtered.slice(start, start + pageSize)

    return simulateAsync(success({
      items,
      total,
      page,
      pageSize,
      totalPages,
    }))
  },

  /**
   * GET /api/v1/models/{id}
   */
  getById: async (id: string): Promise<ApiEnvelope<ModelDetail>> => {
    const detail = mockStore.modelDetails[id]
    if (!detail) {
      // Return a default detail if not found
      const summary = mockStore.models.find((m) => m.modelId === id)
      if (!summary) {
        throw new Error('Model not found')
      }
      return simulateAsync(success({
        ...summary,
        versions: [],
      }))
    }
    return simulateAsync(success(detail))
  },

  /**
   * POST /api/v1/models/upload
   */
  upload: async (
    file: File,
    name: string,
    type: string,
    metadata: ModelMetadata
  ): Promise<ApiEnvelope<ModelUploadResult>> => {
    // Simulate upload delay
    await new Promise((resolve) => setTimeout(resolve, 1000))

    const modelId = `model-${Date.now()}`
    const versionId = `v-${Date.now()}`
    const version = '1.0.0'

    const newModel: ModelDetail = {
      modelId,
      name,
      type: type as ModelDetail['type'],
      description: metadata.type,
      defaultVersion: version,
      versions: [
        {
          versionId,
          version,
          isDefault: true,
          status: 'ACTIVE',
          fileSize: file.size,
          fileUrl: `/mock/models/${modelId}.glb`,
          thumbnailUrl: '',
          metadata,
          createdAt: new Date().toISOString(),
        },
      ],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }

    const summary: ModelSummary = {
      modelId: newModel.modelId,
      name: newModel.name,
      type: newModel.type,
      defaultVersion: newModel.defaultVersion,
      status: 'ACTIVE',
      versions: newModel.versions,
      thumbnailUrl: newModel.versions[0]?.thumbnailUrl,
      createdAt: newModel.createdAt,
      updatedAt: newModel.updatedAt,
    }

    mockStore.models.unshift(summary)
    mockStore.modelDetails[modelId] = newModel

    return simulateAsync(success({
      modelId,
      versionId,
      version,
      fileUrl: `/mock/models/${modelId}.glb`,
    }))
  },

  /**
   * PUT /api/v1/models/{id}
   */
  update: async (id: string, data: Partial<ModelDetail>): Promise<ApiEnvelope<ModelDetail>> => {
    const existing = mockStore.modelDetails[id]
    if (!existing) {
      throw new Error('Model not found')
    }

    const updated: ModelDetail = {
      ...existing,
      ...data,
      modelId: id,
      updatedAt: new Date().toISOString(),
    }

    mockStore.modelDetails[id] = updated

    // Update summary in list
    const summaryIndex = mockStore.models.findIndex((m) => m.modelId === id)
    if (summaryIndex >= 0) {
      mockStore.models[summaryIndex] = {
        modelId: updated.modelId,
        name: updated.name,
        type: updated.type,
        defaultVersion: updated.defaultVersion,
        status: 'ACTIVE',
        versions: updated.versions,
        thumbnailUrl: updated.versions[0]?.thumbnailUrl,
        createdAt: updated.createdAt,
        updatedAt: updated.updatedAt,
      }
    }

    return simulateAsync(success(updated))
  },

  /**
   * POST /api/v1/models/{id}/versions
   */
  uploadVersion: async (
    id: string,
    file: File,
    metadata: ModelMetadata
  ): Promise<ApiEnvelope<ModelVersion>> => {
    const existing = mockStore.modelDetails[id]
    if (!existing) {
      throw new Error('Model not found')
    }

    // Simulate upload delay
    await new Promise((resolve) => setTimeout(resolve, 800))

    const versionId = `v-${Date.now()}`
    const version = `1.${existing.versions.length + 1}.0`

    const newVersion: ModelVersion = {
      versionId,
      version,
      isDefault: false,
      status: 'ACTIVE',
      fileSize: file.size,
      fileUrl: `/mock/models/${id}-${versionId}.glb`,
      thumbnailUrl: '',
      metadata,
      createdAt: new Date().toISOString(),
    }

    existing.versions.push(newVersion)
    existing.updatedAt = new Date().toISOString()

    return simulateAsync(success(newVersion))
  },

  /**
   * PUT /api/v1/models/{id}/versions/{versionId}
   */
  setDefaultVersion: async (id: string, versionId: string): Promise<ApiEnvelope<ModelVersion>> => {
    const existing = mockStore.modelDetails[id]
    if (!existing) {
      throw new Error('Model not found')
    }

    // Unset current default
    existing.versions.forEach((v) => {
      (v as VersionWithDefault).isDefault = false
    })

    // Set new default
    const version = existing.versions.find((v) => v.versionId === versionId)
    if (!version) {
      throw new Error('Version not found')
    }
    (version as VersionWithDefault).isDefault = true

    existing.defaultVersion = version.version
    existing.updatedAt = new Date().toISOString()

    return simulateAsync(success(version))
  },

  /**
   * PATCH /api/v1/models/{id}/enable
   */
  enable: async (id: string): Promise<void> => {
    const existing = mockStore.modelDetails[id]
    if (!existing) {
      throw new Error('Model not found')
    }

    const defaultVersion = existing.versions.find((v) => (v as VersionWithDefault).isDefault)
    if (defaultVersion) {
      (defaultVersion as VersionWithDefault).status = 'ACTIVE'
    }

    return simulateAsync(undefined, 200)
  },

  /**
   * PATCH /api/v1/models/{id}/disable
   */
  disable: async (id: string): Promise<void> => {
    const existing = mockStore.modelDetails[id]
    if (!existing) {
      throw new Error('Model not found')
    }

    const defaultVersion = existing.versions.find((v) => (v as VersionWithDefault).isDefault)
    if (defaultVersion) {
      (defaultVersion as VersionWithDefault).status = 'DISABLED'
    }

    return simulateAsync(undefined, 200)
  },

  /**
   * DELETE /api/v1/models/{id}
   */
  delete: async (id: string): Promise<void> => {
    const index = mockStore.models.findIndex((m) => m.modelId === id)
    if (index < 0) {
      throw new Error('Model not found')
    }

    mockStore.models.splice(index, 1)
    delete mockStore.modelDetails[id]

    return simulateAsync(undefined, 200)
  },

  /**
   * GET /api/v1/scenes/{sceneId}/bindings
   */
  getBindings: async (sceneId: string): Promise<ApiEnvelope<EntityModelBinding[]>> => {
    const bindings = mockStore.bindings.get(sceneId) as EntityModelBinding[] | undefined
    return simulateAsync(success(bindings || []))
  },

  /**
   * PUT /api/v1/scenes/{sceneId}/bindings/{entityId}
   */
  setBinding: async (
    sceneId: string,
    entityId: string,
    modelId: string,
    versionId: string
  ): Promise<void> => {
    let bindings = mockStore.bindings.get(sceneId) as EntityModelBinding[] | undefined

    if (!bindings) {
      bindings = []
      mockStore.bindings.set(sceneId, bindings)
    }

    // Update or add binding
    const existingIndex = bindings.findIndex((b) => b.entityId === entityId)
    const binding: EntityModelBinding = { entityId, modelId, versionId }

    if (existingIndex >= 0) {
      bindings[existingIndex] = binding
    } else {
      bindings.push(binding)
    }

    return simulateAsync(undefined, 200)
  },
}

export default { sceneHandlers, modelHandlers }
