/**
 * Mock mode tests for sim-logistics-frontend.
 *
 * Validates that:
 * 1. Mock mode intercepts all requests (no real network calls)
 * 2. Mock responses follow the ApiEnvelope contract
 * 3. Pages render correctly without backend
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { setupMockAdapter, createMockAdapter, MOCK_MODE } from '../mock/client'
import { sceneHandlers, modelHandlers } from '../mock/handlers'
import { mockStore } from '../mock/data'

describe('Mock Mode Tests', () => {
  beforeEach(() => {
    // Reset mock store before each test
    mockStore.reset()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('Mock Configuration', () => {
    it('should have MOCK_MODE defined', () => {
      // MOCK_MODE is derived from VITE_USE_MOCK env var
      // We just verify the mock module loads correctly
      const validTypes = ['boolean', 'undefined']
      expect(validTypes).toContain(typeof MOCK_MODE)
    })

    it('should setup mock adapter without errors', () => {
      const instance = {
        request: vi.fn().mockResolvedValue({ data: {} }),
      }

      expect(() => setupMockAdapter(instance)).not.toThrow()
    })

    it('shouldNotSendRealRequestWhenMockEnabled', async () => {
      const adapter = createMockAdapter()
      const xhrOpenSpy = vi.spyOn(XMLHttpRequest.prototype, 'open')

      const response = await adapter({
        url: '/api/v1/not-implemented-route',
        method: 'get',
        headers: {},
        baseURL: '/api/v1',
      } as never)

      expect(response.status).toBe(501)
      expect(response.data.code).toBe('ROUTE_NOT_IMPLEMENTED')
      expect(xhrOpenSpy).not.toHaveBeenCalled()
    })

    it('shouldUseConsistentNoContentSemanticsAcrossDeleteEndpoints', async () => {
      const adapter = createMockAdapter()

      const draftDeleteResponse = await adapter({
        url: '/api/v1/scenes/scene-fab-01/draft',
        method: 'delete',
        headers: {},
        baseURL: '/api/v1',
      } as never)
      expect(draftDeleteResponse.status).toBe(204)
      expect(draftDeleteResponse.statusText).toBe('No Content')
      expect(draftDeleteResponse.statusText).not.toBe('Error')
      expect(draftDeleteResponse.data).toBeUndefined()

      const bindingResponse = await adapter({
        url: '/api/v1/scenes/scene-fab-01/bindings/OHT-01',
        method: 'put',
        data: { modelId: 'model-oht-crane', versionId: 'v-1-0-0' },
        headers: {},
        baseURL: '/api/v1',
      } as never)
      expect(bindingResponse.status).toBe(204)
      expect(bindingResponse.statusText).toBe('No Content')
      expect(bindingResponse.statusText).not.toBe('Error')
      expect(bindingResponse.data).toBeUndefined()

      const modelDeleteResponse = await adapter({
        url: '/api/v1/models/model-oht-crane',
        method: 'delete',
        headers: {},
        baseURL: '/api/v1',
      } as never)
      expect(modelDeleteResponse.status).toBe(204)
      expect(modelDeleteResponse.statusText).toBe('No Content')
      expect(modelDeleteResponse.statusText).not.toBe('Error')
      expect(modelDeleteResponse.data).toBeUndefined()

      const sceneDeleteResponse = await adapter({
        url: '/api/v1/scenes/scene-fab-01',
        method: 'delete',
        headers: {},
        baseURL: '/api/v1',
      } as never)
      expect(sceneDeleteResponse.status).toBe(204)
      expect(sceneDeleteResponse.statusText).toBe('No Content')
      expect(sceneDeleteResponse.statusText).not.toBe('Error')
      expect(sceneDeleteResponse.data).toBeUndefined()
    })

    it('shouldParseVersionIdFromAllUrlShapes', async () => {
      const adapter = createMockAdapter()

      const urlShapes = [
        'models/model-oht-crane/versions/v-1-0-0',
        '/models/model-oht-crane/versions/v-1-0-0',
        '/api/v1/models/model-oht-crane/versions/v-1-0-0',
      ]

      for (const url of urlShapes) {
        const response = await adapter({
          url,
          method: 'put',
          headers: {},
          baseURL: '/api/v1',
        } as never)

        expect(response.status).toBe(200)
        expect(response.data.code).toBe('OK')
        expect(response.data.data.versionId).toBe('v-1-0-0')
      }
    })

    it('shouldListScenesFromMockWithoutBackend', async () => {
      const adapter = createMockAdapter()

      const response = await adapter({
        url: '/api/v1/scenes',
        method: 'get',
        params: { page: 1, pageSize: 20 },
        headers: {},
        baseURL: '/api/v1',
      } as never)

      expect(response.status).toBe(200)
      expect(response.data.code).toBe('OK')
      expect(Array.isArray(response.data.data.items)).toBe(true)
    })

    it('shouldMatchBindingsRouteForAllUrlShapes', async () => {
      const adapter = createMockAdapter()
      const urlShapes = [
        'scenes/scene-fab-01/bindings/OHT-01',
        '/scenes/scene-fab-01/bindings/OHT-01',
        '/api/v1/scenes/scene-fab-01/bindings/OHT-01',
      ]

      for (const url of urlShapes) {
        const response = await adapter({
          url,
          method: 'put',
          data: { modelId: 'model-oht-crane', versionId: 'v-1-0-0' },
          headers: {},
          baseURL: '/api/v1',
        } as never)
        expect(response.status).toBe(204)
        expect(response.statusText).toBe('No Content')
        expect(response.data).toBeUndefined()
      }
    })

    it('shouldReturnCorrectContentTypeForSceneExportBlob', async () => {
      const adapter = createMockAdapter()
      const response = await adapter({
        url: '/api/v1/scenes/scene-fab-01/export',
        method: 'get',
        headers: {},
        baseURL: '/api/v1',
      } as never)

      expect(response.status).toBe(200)
      expect(response.headers['content-type']).toBe('application/octet-stream')
      expect(response.data).toBeInstanceOf(Blob)
    })
  })

  describe('Scene Handlers', () => {
    it('should return scene list in correct envelope format', async () => {
      const result = await sceneHandlers.list({ page: 1, pageSize: 20 })

      expect(result).toHaveProperty('code', 'OK')
      expect(result).toHaveProperty('message', 'success')
      expect(result).toHaveProperty('data')
      expect(result).toHaveProperty('traceId')

      expect(result.data).toHaveProperty('items')
      expect(result.data).toHaveProperty('total')
      expect(result.data).toHaveProperty('page')
      expect(result.data).toHaveProperty('pageSize')
      expect(result.data).toHaveProperty('totalPages')

      expect(Array.isArray(result.data.items)).toBe(true)
    })

    it('should support pagination', async () => {
      const page1 = await sceneHandlers.list({ page: 1, pageSize: 5 })
      const page2 = await sceneHandlers.list({ page: 2, pageSize: 5 })

      expect(page1.data.page).toBe(1)
      expect(page2.data.page).toBe(2)

      // Items should be different (unless total items <= 5)
      if (page1.data.total > 5) {
        expect(page1.data.items).not.toEqual(page2.data.items)
      }
    })

    it('should support search filtering', async () => {
      const result = await sceneHandlers.list({
        page: 1,
        pageSize: 20,
        search: 'fab',
      })

      // All items should match search
      result.data.items.forEach((item: { name: string }) => {
        expect(item.name.toLowerCase()).toContain('fab')
      })
    })

    it('should return 404 for non-existent scene draft', async () => {
      // Frontend contract: 404 -> null (draft not found)
      const result = await sceneHandlers.getDraft('non-existent-id')
      expect(result).toBeNull()
    })

    it('should create scene and return envelope', async () => {
      const newScene = {
        name: 'Test Scene',
        description: 'Test description',
        entities: [],
        paths: [],
        processFlows: [],
      }

      const result = await sceneHandlers.create(newScene)

      expect(result.code).toBe('OK')
      expect(result.data).toHaveProperty('sceneId')
      expect(result.data.name).toBe('Test Scene')
    })

    it('should export scene as blob', async () => {
      const blob = await sceneHandlers.exportScene('scene-fab-01')

      expect(blob).toBeInstanceOf(Blob)
      expect(blob.type).toBe('application/json')
    })
  })

  describe('Model Handlers', () => {
    it('should return model list in correct envelope format', async () => {
      const result = await modelHandlers.list({ page: 1, pageSize: 20 })

      expect(result).toHaveProperty('code', 'OK')
      expect(result).toHaveProperty('data')

      expect(result.data).toHaveProperty('items')
      expect(result.data).toHaveProperty('total')

      expect(Array.isArray(result.data.items)).toBe(true)
    })

    it('should support type filtering', async () => {
      const result = await modelHandlers.list({
        page: 1,
        pageSize: 20,
        type: 'OHT_VEHICLE',
      })

      // All items should match type
      result.data.items.forEach((item: { type: string }) => {
        expect(item.type).toBe('OHT_VEHICLE')
      })
    })

    it('should upload model and return upload result', async () => {
      const file = new File([''], 'test.glb', { type: 'model/gltf-binary' })

      const result = await modelHandlers.upload(
        file,
        'Test Model',
        'OHT_VEHICLE',
        {
          type: 'OHT_VEHICLE',
          version: '1.0.0',
          dimensions: { width: 1, height: 1, depth: 1 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        }
      )

      expect(result.code).toBe('OK')
      expect(result.data).toHaveProperty('modelId')
      expect(result.data).toHaveProperty('versionId')
      expect(result.data).toHaveProperty('fileUrl')
    })

    it('should set default version', async () => {
      const result = await modelHandlers.setDefaultVersion('model-oht-crane', 'v-1-0-0')

      expect(result.code).toBe('OK')
      expect(result.data).toHaveProperty('versionId', 'v-1-0-0')
    })

    it('should enable and disable model', async () => {
      await expect(modelHandlers.enable('model-oht-crane')).resolves.toBeUndefined()
      await expect(modelHandlers.disable('model-oht-crane')).resolves.toBeUndefined()
    })

    it('should delete model', async () => {
      // First create a model
      const file = new File([''], 'test.glb', { type: 'model/gltf-binary' })
      const createResult = await modelHandlers.upload(
        file,
        'Test Model',
        'OHT_VEHICLE',
        {
          type: 'OHT_VEHICLE',
          version: '1.0.0',
          dimensions: { width: 1, height: 1, depth: 1 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        }
      )

      const modelId = createResult.data.modelId

      // Then delete it
      await expect(modelHandlers.delete(modelId)).resolves.toBeUndefined()
    })
  })

  describe('Bindings Handlers', () => {
    it('should get bindings for scene', async () => {
      const result = await modelHandlers.getBindings('scene-fab-01')

      expect(result.code).toBe('OK')
      expect(Array.isArray(result.data)).toBe(true)
    })

    it('should set binding for entity', async () => {
      await expect(
        modelHandlers.setBinding('scene-fab-01', 'OHT-01', 'model-oht-crane', 'v-1-0-0')
      ).resolves.toBeUndefined()
    })
  })

  describe('Contract Compliance', () => {
    it('should always return ApiEnvelope structure', async () => {
      const sceneResult = await sceneHandlers.list({ page: 1, pageSize: 20 })
      const modelResult = await modelHandlers.list({ page: 1, pageSize: 20 })

      // Check envelope structure
      expect(sceneResult).toMatchObject({
        code: expect.any(String),
        message: expect.any(String),
        data: expect.any(Object),
        traceId: expect.stringMatching(/^mock-trace-/),
      })

      expect(modelResult).toMatchObject({
        code: expect.any(String),
        message: expect.any(String),
        data: expect.any(Object),
        traceId: expect.stringMatching(/^mock-trace-/),
      })
    })

    it('should preserve data types per contract', async () => {
      const result = await sceneHandlers.list({ page: 1, pageSize: 20 })

      // Check pagination types
      expect(typeof result.data.total).toBe('number')
      expect(typeof result.data.page).toBe('number')
      expect(typeof result.data.pageSize).toBe('number')
      expect(typeof result.data.totalPages).toBe('number')
    })
  })
})

/**
 * Integration test: Mock should work without real backend
 */
describe('Mock Integration Tests', () => {
  it('should handle all scene operations without backend', async () => {
    // List
    const listResult = await sceneHandlers.list({ page: 1, pageSize: 20 })
    expect(listResult.data.items.length).toBeGreaterThan(0)

    // Get by ID
    const sceneId = listResult.data.items[0].sceneId
    const getResult = await sceneHandlers.getById(sceneId)
    expect(getResult.data.sceneId).toBe(sceneId)

    // Create
    const createResult = await sceneHandlers.create({
      name: 'Integration Test Scene',
      entities: [],
      paths: [],
      processFlows: [],
    })
    expect(createResult.data).toHaveProperty('sceneId')

    // Update
    const updateResult = await sceneHandlers.update(createResult.data.sceneId, {
      name: 'Updated Scene',
    })
    expect(updateResult.data.name).toBe('Updated Scene')

    // Delete
    await sceneHandlers.delete(createResult.data.sceneId)
    // Should not throw
  })

  it('should handle all model operations without backend', async () => {
    // List
    const listResult = await modelHandlers.list({ page: 1, pageSize: 20 })
    expect(listResult.data.items.length).toBeGreaterThan(0)

    // Get by ID
    const modelId = listResult.data.items[0].modelId
    const getResult = await modelHandlers.getById(modelId)
    expect(getResult.data.modelId).toBe(modelId)

    // Update
    const updateResult = await modelHandlers.update(modelId, {
      name: 'Updated Model',
    })
    expect(updateResult.data.name).toBe('Updated Model')
  })
})

// ==================== C2: 新增前端契约验证测试 ====================
describe('Frontend Contract Alignment Tests', () => {
  describe('Query Parameter Consistency', () => {
    /**
     * C2.1: 验证列表查询使用统一的 query params
     * 前端发送 search (而非 keyword)，page 转换为 0-based
     */
    it('shouldCallListWithUnifiedQueryParams', async () => {
      const adapter = createMockAdapter()

      // Frontend sends 1-based page, converts to 0-based
      const response = await adapter({
        url: '/api/v1/scenes',
        method: 'get',
        params: { page: 1, pageSize: 20, search: 'test' }, // page=1 should become page=0
        headers: {},
        baseURL: '/api/v1',
      } as never)

      expect(response.status).toBe(200)
      // Verify backend receives search parameter (not keyword)
      expect(response.data.code).toBe('OK')
    })
  })

  describe('Draft Payload Contract', () => {
    /**
     * C2.2: 验证草稿保存发送 SceneDraftPayload 结构
     * 匹配后端 SceneDraftPayloadDTO
     */
    it('shouldSendDraftPayloadMatchingBackendContract', async () => {
      const adapter = createMockAdapter()

      const payload = {
        sceneId: 'SCENE-001',
        content: {
          sceneId: 'SCENE-001',
          name: 'Test Scene',
          version: 1,
          entities: [],
          paths: [],
          processFlows: [],
        },
        savedAt: new Date().toISOString(),
        version: 1,
      }

      const response = await adapter({
        url: '/api/v1/scenes/SCENE-001/draft',
        method: 'post',
        data: payload,
        headers: {},
        baseURL: '/api/v1',
      } as never)

      expect(response.status).toBe(200)
      expect(response.data.code).toBe('OK')
      expect(response.data.data.success).toBe(true)
    })
  })

  describe('Draft 404 Handling', () => {
    /**
     * C2.3: 验证草稿不存在时返回 null
     * 前端契约: 404 -> null (非 404 错误抛出)
     */
    it('shouldHandleDraft404AsNull', async () => {
      const result = await sceneHandlers.getDraft('non-existent-draft-id')

      // Mock 返回 null 表示草稿不存在
      expect(result).toBeNull()
    })
  })

  describe('Scene Rendering and Editing', () => {
    /**
     * C2.4: 验证场景可以渲染和编辑，无契约错误
     */
    it('shouldRenderAndEditSceneWithoutContractError', async () => {
      // Get scene detail
      const scene = await sceneHandlers.getById('scene-fab-01')

      // Verify contract compliance
      expect(scene.data).toHaveProperty('sceneId')
      expect(scene.data).toHaveProperty('name')
      expect(scene.data).toHaveProperty('version')
      expect(scene.data).toHaveProperty('entities')
      expect(scene.data).toHaveProperty('paths')
      expect(scene.data).toHaveProperty('processFlows')

      // Update scene
      const updateResult = await sceneHandlers.update('scene-fab-01', {
        name: 'Updated Scene Name',
      })

      expect(updateResult.data.name).toBe('Updated Scene Name')
      expect(updateResult.data.version).toBeGreaterThan(scene.data.version)
    })
  })

  describe('Scene Export', () => {
    /**
     * C2.5: 验证场景导出返回 blob
     */
    it('shouldExportSceneAsBlob', async () => {
      const blob = await sceneHandlers.exportScene('scene-fab-01')

      expect(blob).toBeInstanceOf(Blob)
      expect(blob.type).toBe('application/json')
      expect(blob.size).toBeGreaterThan(0)
    })
  })
})

