/**
 * Model API service for sim-logistics-frontend.
 *
 * Implements model library operations following frontend-contract.md
 * Section 3.2: Model Module
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import apiClient, { unwrapEnvelope } from './client'
import type {
  ApiEnvelope,
  PagedResult,
  ModelSummary,
  ModelDetail,
  ModelVersion,
  ModelUploadResult,
  ModelMetadata,
  EntityModelBinding,
} from '../types'

/**
 * Model API paths
 */
const PATHS = {
  LIST: '/models',
  DETAIL: (id: string) => `/models/${id}`,
  UPLOAD: '/models/upload',
  VERSIONS: (id: string) => `/models/${id}/versions`,
  VERSION_DETAIL: (id: string, versionId: string) => `/models/${id}/versions/${versionId}`,
  ENABLE: (id: string) => `/models/${id}/enable`,
  DISABLE: (id: string) => `/models/${id}/disable`,
  DELETE: (id: string) => `/models/${id}`,
  BINDINGS: (sceneId: string) => `/scenes/${sceneId}/bindings`,
  BINDING_SET: (sceneId: string, entityId: string) => `/scenes/${sceneId}/bindings/${entityId}`,
} as const

/**
 * Model API service
 */
export const modelsApi = {
  /**
   * Get model list with pagination
   * GET /api/v1/models -> ApiEnvelope<PagedResult<ModelSummary>>
   *
   * Pagination semantics:
   * - page: 1-based in frontend, converted to 0-based for backend
   * - response: backend 0-based converted to 1-based for frontend
   */
  async list(
    page: number = 1,
    pageSize: number = 20,
    type?: string,
    status?: string,
    search?: string
  ): Promise<PagedResult<ModelSummary>> {
    const params: Record<string, string | number> = {
      // Convert 1-based page to 0-based for backend
      page: page > 0 ? page - 1 : 0,
      pageSize,
    }
    if (type) params.type = type
    if (status) params.status = status
    if (search) params.search = search

    const response = await apiClient.get<ApiEnvelope<PagedResult<ModelSummary>>>(
      PATHS.LIST,
      { params }
    )
    const data = unwrapEnvelope(response) as unknown as PagedResult<Record<string, unknown>>
    const rawItems = Array.isArray(data.items) ? data.items : []

    // Convert 0-based from backend to 1-based for frontend display
    return {
      items: rawItems.map(mapModelSummary),
      total: Number(data.total ?? 0),
      page: Number(data.page ?? 0) + 1,
      pageSize: Number(data.pageSize ?? pageSize),
      totalPages: Number(data.totalPages ?? 0),
    }
  },

  /**
   * Get model detail by ID
   * GET /api/v1/models/{id}
   */
  async getById(id: string): Promise<ModelDetail> {
    const response = await apiClient.get<ApiEnvelope<ModelDetail>>(PATHS.DETAIL(id))
    return mapModelDetail(unwrapEnvelope(response) as unknown as Record<string, unknown>)
  },

  /**
   * Upload new model (GLB file to MinIO)
   * POST /api/v1/models/upload
   */
  async upload(
    file: File,
    name: string,
    type: string,
    metadata: ModelMetadata,
    onProgress?: (progress: number) => void
  ): Promise<ModelUploadResult> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('name', name)
    formData.append('modelType', type)
    formData.append('version', metadata.version)
    formData.append('metadata', JSON.stringify(toBackendMetadata(metadata)))

    const response = await apiClient.post<ApiEnvelope<ModelUploadResult>>(
      PATHS.UPLOAD,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && onProgress) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress(progress)
          }
        },
      }
    )
    const raw = unwrapEnvelope(response) as unknown as Record<string, unknown>
    return {
      modelId: String(raw.modelId ?? ''),
      versionId: String(raw.versionId ?? ''),
      version: String(raw.version ?? metadata.version),
      fileUrl: String(raw.fileUrl ?? ''),
    }
  },

  /**
   * Update model metadata
   * PUT /api/v1/models/{id}
   */
  async update(id: string, data: Partial<ModelDetail>): Promise<ModelDetail> {
    const response = await apiClient.put<ApiEnvelope<ModelDetail>>(
      PATHS.DETAIL(id),
      data
    )
    return mapModelDetail(unwrapEnvelope(response) as unknown as Record<string, unknown>)
  },

  /**
   * Upload new version for existing model
   * POST /api/v1/models/{id}/versions
   */
  async uploadVersion(
    id: string,
    file: File,
    metadata: ModelMetadata,
    onProgress?: (progress: number) => void
  ): Promise<ModelVersion> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('version', metadata.version)
    formData.append('metadata', JSON.stringify(toBackendMetadata(metadata)))

    const response = await apiClient.post<ApiEnvelope<ModelVersion>>(
      PATHS.VERSIONS(id),
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total && onProgress) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress(progress)
          }
        },
      }
    )
    return mapModelVersion(unwrapEnvelope(response) as unknown as Record<string, unknown>)
  },

  /**
   * Set model version as default
   * PUT /api/v1/models/{id}/versions/{versionId}
   */
  async setDefaultVersion(id: string, versionId: string): Promise<ModelVersion> {
    const response = await apiClient.put<ApiEnvelope<ModelVersion>>(
      PATHS.VERSION_DETAIL(id, versionId),
      { isDefault: true }
    )
    return mapModelVersion(unwrapEnvelope(response) as unknown as Record<string, unknown>)
  },

  /**
   * Enable model
   * PATCH /api/v1/models/{id}/enable
   */
  async enable(id: string): Promise<void> {
    await apiClient.patch(PATHS.ENABLE(id))
  },

  /**
   * Disable model
   * PATCH /api/v1/models/{id}/disable
   */
  async disable(id: string): Promise<void> {
    await apiClient.patch(PATHS.DISABLE(id))
  },

  /**
   * Delete model
   * DELETE /api/v1/models/{id} -> 204
   */
  async delete(id: string): Promise<void> {
    await apiClient.delete(PATHS.DELETE(id))
  },

  /**
   * Get entity model bindings for a scene
   * GET /api/v1/scenes/{sceneId}/bindings
   */
  async getBindings(sceneId: string): Promise<EntityModelBinding[]> {
    const response = await apiClient.get<ApiEnvelope<EntityModelBinding[]>>(
      PATHS.BINDINGS(sceneId)
    )
    const raw = unwrapEnvelope(response)
    const items = Array.isArray(raw) ? (raw as unknown as Array<Record<string, unknown>>) : []
    return items.map((item) => ({
      entityId: String(item.entityId ?? ''),
      modelId: String(item.modelId ?? ''),
      versionId: String(item.versionId ?? item.modelVersionId ?? ''),
    }))
  },

  /**
   * Update entity model binding
   * PUT /api/v1/scenes/{sceneId}/bindings/{entityId} -> 204|200
   */
  async setBinding(
    sceneId: string,
    entityId: string,
    modelId: string,
    versionId: string
  ): Promise<void> {
    await apiClient.put(PATHS.BINDING_SET(sceneId, entityId), {
      modelVersionId: versionId,
      modelId,
      versionId,
    })
  },
}

export default modelsApi

const normalizeStatus = (status: unknown): 'ACTIVE' | 'DISABLED' | 'PENDING' => {
  const value = String(status ?? '').toUpperCase()
  if (value === 'ACTIVE' || value === 'ENABLED') return 'ACTIVE'
  if (value === 'DISABLED') return 'DISABLED'
  return 'PENDING'
}

const mapModelSummary = (item: Record<string, unknown>): ModelSummary => {
  const defaultVersion = String(item.defaultVersion ?? item.defaultVersionId ?? item.version ?? 'N/A')
  return {
    modelId: String(item.modelId ?? ''),
    name: String(item.name ?? ''),
    type: String(item.type ?? item.modelType ?? 'MACHINE') as ModelSummary['type'],
    defaultVersion,
    status: normalizeStatus(item.status),
    // List endpoint is summary-only, real version IDs must come from detail endpoint.
    versions: [],
    thumbnailUrl: item.thumbnailUrl ? String(item.thumbnailUrl) : undefined,
    createdAt: String(item.createdAt ?? ''),
    updatedAt: String(item.updatedAt ?? ''),
  }
}

const mapModelDetail = (item: Record<string, unknown>): ModelDetail => {
  const rawVersions = Array.isArray(item.versions) ? item.versions as Array<Record<string, unknown>> : []
  return {
    modelId: String(item.modelId ?? ''),
    name: String(item.name ?? ''),
    type: String(item.type ?? item.modelType ?? 'MACHINE') as ModelDetail['type'],
    description: item.description ? String(item.description) : undefined,
    defaultVersion: String(item.defaultVersion ?? item.defaultVersionId ?? ''),
    versions: rawVersions.map(mapModelVersion),
    createdAt: String(item.createdAt ?? ''),
    updatedAt: String(item.updatedAt ?? ''),
  }
}

const mapModelVersion = (item: Record<string, unknown>): ModelVersion => {
  // Extract metadata from backend response
  const rawMetadata = (item.metadata ?? {}) as Record<string, unknown>

  // Resolve transform: prefer nested, then top-level, then from metadata
  const transform = (item.transform ?? item.transformConfig ?? rawMetadata.transform ?? rawMetadata.transformConfig) as Record<string, unknown> | undefined
  const scale = toVector3(transform?.scale, { x: 1, y: 1, z: 1 })
  const rotation = toVector3(transform?.rotation, { x: 0, y: 0, z: 0 })
  const pivot = toVector3(transform?.pivot, { x: 0, y: 0, z: 0 })

  // Resolve dimensions: metadata.size > metadata.dimensions > fallback
  const rawSize = rawMetadata.size ?? rawMetadata.dimensions
  const dimensions = rawSize
    ? {
        width: Number((rawSize as Record<string, unknown>)?.width ?? 1),
        height: Number((rawSize as Record<string, unknown>)?.height ?? 1),
        depth: Number((rawSize as Record<string, unknown>)?.depth ?? 1),
      }
    : { width: 1, height: 1, depth: 1 }

  // Resolve anchorPoint: metadata.anchor > metadata.anchorPoint > fallback
  const rawAnchor = rawMetadata.anchor ?? rawMetadata.anchorPoint
  const anchorPoint = rawAnchor
    ? {
        x: Number((rawAnchor as Record<string, unknown>)?.x ?? 0),
        y: Number((rawAnchor as Record<string, unknown>)?.y ?? 0),
        z: Number((rawAnchor as Record<string, unknown>)?.z ?? 0),
      }
    : { x: 0, y: 0, z: 0 }

  const fileUrl = String(item.fileUrl ?? '')

  // Task A: Diagnostic logging
  if (import.meta.env.DEV && !fileUrl) {
    const msg = '[GLB-DIAG] mapModelVersion missing fileUrl'
    console.warn(msg, {
      modelId: item.modelId,
      versionId: item.versionId,
      version: item.version,
      rawKeys: Object.keys(item)
    })
  }

  // Log when using non-default metadata values
  if (
    import.meta.env.DEV &&
    (
      dimensions.width !== 1 ||
      dimensions.height !== 1 ||
      dimensions.depth !== 1 ||
      anchorPoint.x !== 0 ||
      anchorPoint.y !== 0 ||
      anchorPoint.z !== 0
    )
  ) {
    console.log('[GLB-DIAG] mapModelVersion metadata resolved', {
      modelId: item.modelId,
      versionId: item.versionId,
      dimensions,
      anchorPoint,
      scale
    })
  }

  return {
    versionId: String(item.versionId ?? ''),
    version: String(item.version ?? ''),
    isDefault: Boolean(item.isDefault),
    status: normalizeStatus(item.status),
    fileSize: Number(item.fileSize ?? 0),
    fileUrl,
    thumbnailUrl: item.thumbnailUrl ? String(item.thumbnailUrl) : undefined,
    metadata: {
      type: (rawMetadata.type ?? 'MACHINE') as ModelMetadata['type'],
      version: String(item.version ?? ''),
      dimensions,
      anchorPoint,
      transform: {
        scale,
        rotation,
        pivot,
      },
    },
    createdAt: String(item.createdAt ?? ''),
  }
}

const toBackendMetadata = (metadata: ModelMetadata): Record<string, unknown> => ({
  type: metadata.type,
  version: metadata.version,
  size: {
    width: metadata.dimensions.width,
    height: metadata.dimensions.height,
    depth: metadata.dimensions.depth,
  },
  anchor: metadata.anchorPoint,
  transform: toBackendTransform(metadata.transform),
})

const toBackendTransform = (transform: ModelMetadata['transform']) => {
  if (!transform) return undefined
  return {
    scale: toVector3(transform.scale, { x: 1, y: 1, z: 1 }),
    rotation: toVector3(transform.rotation, { x: 0, y: 0, z: 0 }),
    pivot: toVector3(transform.pivot, { x: 0, y: 0, z: 0 }),
  }
}

const toVector3 = (
  value: unknown,
  fallback: { x: number; y: number; z: number }
): { x: number; y: number; z: number } => {
  if (Array.isArray(value)) {
    return {
      x: Number(value[0] ?? fallback.x),
      y: Number(value[1] ?? fallback.y),
      z: Number(value[2] ?? fallback.z),
    }
  }
  const vector = (value ?? {}) as Record<string, unknown>
  return {
    x: Number(vector.x ?? fallback.x),
    y: Number(vector.y ?? fallback.y),
    z: Number(vector.z ?? fallback.z),
  }
}
