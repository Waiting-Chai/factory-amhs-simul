/**
 * Mock API adapter for sim-logistics-frontend.
 *
 * Implements a standard axios adapter for mock mode.
 * When VITE_USE_MOCK=true, ALL requests are handled by this adapter.
 * No real network requests are made.
 *
 * @author shentw
 * @version 2.0
 * @since 2026-02-10
 */
import type {
  AxiosAdapter,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios'
import { sceneHandlers, modelHandlers } from './handlers'
import type { SceneDetail, ModelDetail, ModelMetadata, ApiEnvelope } from './types'

/**
 * Mock mode flag
 */
export const MOCK_MODE = import.meta.env.VITE_USE_MOCK === 'true'

const isApiEnvelope = (value: unknown): value is ApiEnvelope => {
  if (!value || typeof value !== 'object') {
    return false
  }
  const payload = value as Record<string, unknown>
  return (
    typeof payload.code === 'string' &&
    typeof payload.message === 'string' &&
    'data' in payload
  )
}

/**
 * Startup logging
 */
if (MOCK_MODE) {
  console.log('%c[Mock] Mock Mode: ENABLED', 'color: #10b981; font-weight: bold; font-size: 12px;')
  console.log('%c[Mock] Using axios adapter-level interception', 'color: #10b981; font-size: 11px;')
  console.log('%c[Mock] NO real network requests will be made', 'color: #10b981; font-size: 11px;')
} else {
  console.log('%c[Mock] Mock Mode: DISABLED (using real backend)', 'color: #f59e0b; font-weight: bold; font-size: 12px;')
}

/**
 * Mock error type
 */
type MockError = Error & {
  status?: number
  code?: string
}

/**
 * Parse URL to extract resource, id, action, and query params
 * Handles:
 * - Full URLs: /api/v1/scenes?page=1
 * - Relative URLs: scenes?page=1
 * - URLs with query strings
 */
const parseRequestUrl = (config: InternalAxiosRequestConfig): {
  resource: string
  id: string | undefined
  action: string | undefined
  parts: string[]
  queryParams: Record<string, unknown>
} => {
  const url = config.url || ''
  const baseURL = config.baseURL || ''

  // Combine baseURL and url
  // axios adapter receives: url='scenes?page=1', baseURL='/api/v1'
  // We need to construct the full path
  let fullPath = url

  // If url is relative (doesn't start with /), prepend baseURL
  if (!url.startsWith('http') && !url.startsWith('/')) {
    fullPath = baseURL ? `${baseURL}/${url}` : url
  }

  // Remove protocol and host if present
  fullPath = fullPath.replace(/^https?:\/\/[^/]+/, '')

  // Remove query string for path parsing
  const queryStringStart = fullPath.indexOf('?')
  const pathWithoutQuery = queryStringStart >= 0 ? fullPath.substring(0, queryStringStart) : fullPath

  // Parse query params from config.params (axios puts parsed params here)
  const queryParams = (config.params || {}) as Record<string, unknown>

  // Parse path segments
  // Remove /api/v1/ prefix if present
  const cleanPath = pathWithoutQuery
    .replace(/^\/api\/v1\//, '')
    .replace(/^\/api\//, '')
    .replace(/^\/+/, '') // Remove leading slashes

  const parts = cleanPath.split('/').filter(Boolean)

  return {
    resource: parts[0] || '',
    id: parts[1],
    action: parts[2],
    parts,
    queryParams,
  }
}

/**
 * Create a standard AxiosResponse from mock data
 */
const createSuccessResponse = (
  config: InternalAxiosRequestConfig,
  payload: unknown,
  status: number = 200,
  statusText: string = 'OK'
): AxiosResponse<ApiEnvelope> => {
  const envelope = isApiEnvelope(payload)
    ? payload
    : {
        code: status === 200 || status === 201 ? 'OK' : 'ERROR',
        message: statusText,
        data: payload,
        traceId: `mock-trace-${Date.now()}`,
      }

  return {
    data: envelope,
    status,
    statusText,
    headers: {
      'content-type': 'application/json',
    } as unknown as Record<string, string>,
    config,
    request: {} as unknown,
  }
}

const createOkResponse = (
  config: InternalAxiosRequestConfig
): AxiosResponse<ApiEnvelope<null>> => {
  return {
    data: {
      code: 'OK',
      message: 'OK',
      data: null,
      traceId: `mock-trace-${Date.now()}`,
    },
    status: 200,
    statusText: 'OK',
    headers: {} as unknown as Record<string, string>,
    config,
    request: {} as unknown,
  }
}

const createNoContentResponse = (
  config: InternalAxiosRequestConfig
): AxiosResponse<ApiEnvelope> => {
  return {
    data: undefined as unknown as ApiEnvelope,
    status: 204,
    statusText: 'No Content',
    headers: {} as unknown as Record<string, string>,
    config,
    request: {} as unknown,
  }
}

export const setupMockAdapter = (
  instance: { defaults?: { adapter?: AxiosAdapter }; [key: string]: unknown } | null | undefined
): void => {
  if (!instance?.defaults) {
    return
  }
  instance.defaults.adapter = createMockAdapter()
}

/**
 * Create an error AxiosResponse for 404/501 etc
 */
const createErrorResponse = (
  config: InternalAxiosRequestConfig,
  status: number,
  code: string,
  message: string
): AxiosResponse<ApiEnvelope<null>> => {
  return {
    data: {
      code,
      message,
      data: null,
      traceId: `mock-trace-${Date.now()}`,
    },
    status,
    statusText: status === 404 ? 'Not Found' : status === 501 ? 'Not Implemented' : 'Error',
    headers: {} as unknown as Record<string, string>,
    config,
    request: {} as unknown,
  }
}

/**
 * Standard axios adapter for mock mode
 *
 * This adapter intercepts ALL requests when USE_MOCK=true.
 * It returns mock data without making any real network requests.
 */
export const mockAdapter: AxiosAdapter = async (
  config: InternalAxiosRequestConfig
): Promise<AxiosResponse<ApiEnvelope>> => {
  const { method } = config
  const { resource, id, action, parts, queryParams } = parseRequestUrl(config)
  const data = config.data as unknown

  // Log the request (for debugging)
  console.log(`[Mock] ${method?.toUpperCase()} ${config.url} -> resource=${resource}, id=${id}`)

  try {
    // ============================================================================
    // SCENES
    // ============================================================================
    if (resource === 'scenes') {
      if (id) {
        // /scenes/{id}/copy
        if (action === 'copy') {
          const result = await sceneHandlers.copy(id, (data as { name?: string })?.name)
          return createSuccessResponse(config, result)
        }

        // /scenes/{id}/draft (GET)
        if (action === 'draft' && method === 'get') {
          try {
            const result = await sceneHandlers.getDraft(id)
            return createSuccessResponse(config, result)
          } catch (err: unknown) {
            const error = err as MockError
            if (error.status === 404) {
              // Draft not found - this is expected, return 404
              return createErrorResponse(config, 404, 'NOT_FOUND', 'Draft not found')
            }
            throw err
          }
        }

        // /scenes/{id}/draft (POST)
        if (action === 'draft' && method === 'post') {
          const result = await sceneHandlers.saveDraft(id, data as SceneDetail)
          return createSuccessResponse(config, result)
        }

        // /scenes/{id}/draft (DELETE)
        if (action === 'draft' && method === 'delete') {
          await sceneHandlers.deleteDraft(id)
          return createNoContentResponse(config)
        }

        // /scenes/{id}/export
        if (action === 'export') {
          const blob = await sceneHandlers.exportScene(id)
          // For blob responses, we need a different structure
          return {
            data: blob as unknown as ApiEnvelope,
            status: 200,
            statusText: 'OK',
            headers: {
              'content-type': 'application/octet-stream',
            } as unknown as Record<string, string>,
            config,
            request: {} as unknown,
          }
        }

        // /scenes/{id} (GET)
        if (method === 'get') {
          const result = await sceneHandlers.getById(id)
          return createSuccessResponse(config, result)
        }

        // /scenes/{id} (PUT)
        if (method === 'put') {
          const result = await sceneHandlers.update(id, data as Partial<SceneDetail>)
          return createSuccessResponse(config, result)
        }

        // /scenes/{id} (DELETE)
        if (method === 'delete') {
          await sceneHandlers.delete(id)
          return createNoContentResponse(config)
        }
      } else {
        // /scenes (GET) - list
        if (method === 'get') {
          const result = await sceneHandlers.list(queryParams || {})
          return createSuccessResponse(config, result)
        }

        // /scenes (POST) - create
        if (method === 'post') {
          const result = await sceneHandlers.create(data as Partial<SceneDetail>)
          return createSuccessResponse(config, result, 201, 'Created')
        }
      }
    }

    // ============================================================================
    // MODELS
    // ============================================================================
    if (resource === 'models') {
      if (id) {
        // /models/{id}/versions/{versionId} (PUT)
        const versionId = action === 'versions' ? parts[3] : undefined
        if (method === 'put' && action === 'versions' && versionId) {
          const result = await modelHandlers.setDefaultVersion(id, versionId)
          return createSuccessResponse(config, result)
        }

        // /models/{id}/versions (POST)
        if (action === 'versions') {
          const formData = data as FormData
          const file = formData.get('file') as File
          const metadata = JSON.parse(formData.get('metadata') as string) as ModelMetadata
          const result = await modelHandlers.uploadVersion(id, file, metadata)
          return createSuccessResponse(config, result, 201, 'Created')
        }

        // /models/{id}/enable
        if (action === 'enable') {
          await modelHandlers.enable(id)
          return createOkResponse(config)
        }

        // /models/{id}/disable
        if (action === 'disable') {
          await modelHandlers.disable(id)
          return createOkResponse(config)
        }

        // /models/{id} (GET)
        if (method === 'get') {
          const result = await modelHandlers.getById(id)
          return createSuccessResponse(config, result)
        }

        // /models/{id} (PUT)
        if (method === 'put') {
          const result = await modelHandlers.update(id, data as Partial<ModelDetail>)
          return createSuccessResponse(config, result)
        }

        // /models/{id} (DELETE)
        if (method === 'delete') {
          await modelHandlers.delete(id)
          return createNoContentResponse(config)
        }
      } else {
        // /models/upload (POST)
        if (action === 'upload') {
          const formData = data as FormData
          const file = formData.get('file') as File
          const name = formData.get('name') as string
          const type = formData.get('type') as string
          const metadata = JSON.parse(formData.get('metadata') as string) as ModelMetadata
          const result = await modelHandlers.upload(file, name, type, metadata)
          return createSuccessResponse(config, result, 201, 'Created')
        }

        // /models (GET) - list
        if (method === 'get') {
          const result = await modelHandlers.list(queryParams || {})
          return createSuccessResponse(config, result)
        }
      }
    }

    // ============================================================================
    // UNKNOWN ROUTE - Return 501 (NOT IMPLEMENTED)
    // ============================================================================
    console.error(`[Mock] ❌ Unknown route: ${method?.toUpperCase()} ${resource}`)
    return createErrorResponse(
      config,
      501,
      'ROUTE_NOT_IMPLEMENTED',
      `Mock route not implemented: ${method?.toUpperCase()} /${resource}`
    )
  } catch (errorRaw) {
    const error = errorRaw as MockError

    // Check if this is a 404 error (for draft)
    if (error.status === 404) {
      return createErrorResponse(config, 404, 'NOT_FOUND', error.message || 'Not found')
    }

    // For other errors, still return a proper error response
    console.error(`[Mock] Error processing request:`, error)
    return createErrorResponse(
      config,
      500,
      'INTERNAL_ERROR',
      error.message || 'Internal error'
    )
  }
}

/**
 * Handle bindings requests (special case for /scenes/{id}/bindings)
 */
const isBindingsUrl = (config: InternalAxiosRequestConfig): boolean => {
  const { parts } = parseRequestUrl(config)
  return parts[0] === 'scenes' && parts[2] === 'bindings'
}

const handleBindingsRequest = async (
  config: InternalAxiosRequestConfig
): Promise<AxiosResponse<ApiEnvelope>> => {
  const { parts } = parseRequestUrl(config)
  if (parts[0] !== 'scenes' || parts[2] !== 'bindings' || !parts[1]) {
    return createErrorResponse(config, 400, 'INVALID_URL', 'Invalid bindings URL')
  }

  const sceneId = parts[1]
  const entityId = parts[3]

  if (config.method === 'get') {
    const result = await modelHandlers.getBindings(sceneId)
    return createSuccessResponse(config, result)
  }

  if (config.method === 'put' && entityId) {
    const data = config.data as { modelId?: string; versionId?: string; modelVersionId?: string }
    await modelHandlers.setBinding(
      sceneId,
      entityId,
      data?.modelId ?? '',
      data?.versionId ?? data?.modelVersionId ?? ''
    )
    return createNoContentResponse(config)
  }

  return createErrorResponse(config, 400, 'INVALID_METHOD', 'Invalid method for bindings')
}

/**
 * Wrapper adapter that checks for bindings URLs first
 */
export const createMockAdapter = (): AxiosAdapter => {
  return async (config: InternalAxiosRequestConfig): Promise<AxiosResponse<ApiEnvelope>> => {
    // Check for bindings URLs first
    if (isBindingsUrl(config)) {
      console.log(`[Mock] Handling bindings request: ${config.url}`)
      return handleBindingsRequest(config)
    }

    // All other requests go to the main mock adapter
    return mockAdapter(config)
  }
}

export default { mockAdapter, createMockAdapter, MOCK_MODE }
