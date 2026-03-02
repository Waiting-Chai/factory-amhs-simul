/**
 * Scene store tests.
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-10
 */
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { useSceneStore } from './sceneStore'
import { scenesApi } from '@api/scenes'

// Mock API
vi.mock('@api/scenes', () => ({
  scenesApi: {
    list: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    copy: vi.fn(),
    import: vi.fn(),
    exportScene: vi.fn(),
    getDraft: vi.fn(),
    saveDraft: vi.fn(),
    deleteDraft: vi.fn(),
  },
}))

describe('SceneStore', () => {
  beforeEach(() => {
    useSceneStore.setState({
      scenes: [],
      currentScene: null,
      draft: null,
      pagination: {
        page: 1,
        pageSize: 20,
        total: 0,
        totalPages: 0,
      },
      isLoading: false,
      error: null,
      searchQuery: '',
      filterType: null,
      showDraftRecoveryDialog: false,
      hasUnsavedChanges: false,
      lastSavedContent: '',
      lastSavedAt: null,
    })
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should have initial state', () => {
    const state = useSceneStore.getState()
    expect(state.scenes).toEqual([])
    expect(state.currentScene).toBeNull()
    expect(state.isLoading).toBe(false)
    expect(state.error).toBeNull()
  })

  it('should mark unsaved changes', () => {
    useSceneStore.getState().markUnsavedChanges()
    expect(useSceneStore.getState().hasUnsavedChanges).toBe(true)
  })

  it('should clear unsaved changes', () => {
    useSceneStore.getState().markUnsavedChanges()
    useSceneStore.getState().clearUnsavedChanges()
    expect(useSceneStore.getState().hasUnsavedChanges).toBe(false)
  })

  it('shouldMapSceneListResponseCorrectlyInStore', async () => {
    // Mock API response with 0-based page (as returned by backend)
    vi.mocked(scenesApi.list).mockResolvedValue({
      items: [
        {
          sceneId: 'scene-1',
          name: 'Scene 1',
          description: 'Test scene',
          version: 1,
          createdAt: '2026-02-10T10:00:00.000Z',
          updatedAt: '2026-02-10T10:00:00.000Z',
          entityCount: 5,
        },
      ],
      total: 1,
      page: 0, // 0-based from backend
      pageSize: 20,
      totalPages: 1,
    })

    await useSceneStore.getState().fetchScenes()

    const state = useSceneStore.getState()
    expect(state.scenes).toHaveLength(1)
    expect(state.scenes[0]?.sceneId).toBe('scene-1')
    expect(state.pagination.total).toBe(1)
    expect(state.pagination.page).toBe(1)
    expect(state.error).toBeNull()
    expect(state.isLoading).toBe(false)
  })

  it('shouldFallbackToEmptyArrayWhenListPayloadMalformed', async () => {
    vi.mocked(scenesApi.list).mockResolvedValue({} as never)

    await useSceneStore.getState().fetchScenes()

    const state = useSceneStore.getState()
    expect(state.scenes).toEqual([])
    expect(state.error).toBe('Invalid scene list response payload')
    expect(state.isLoading).toBe(false)
  })

  it('shouldPassSearchAndFilterParamsToApi', async () => {
    // Mock API response
    vi.mocked(scenesApi.list).mockResolvedValue({
      items: [],
      total: 0,
      page: 0,
      pageSize: 20,
      totalPages: 0,
    })

    // Set search and filter
    useSceneStore.setState({
      searchQuery: 'test search',
      filterType: 'MANUFACTURING',
    })

    await useSceneStore.getState().fetchScenes()

    // Verify API was called with search and filter params
    expect(scenesApi.list).toHaveBeenCalledWith(
      1, // page (1-based from frontend default)
      20, // pageSize
      'test search', // search
      'MANUFACTURING' // type (filterType)
    )
  })

  describe('Draft 404 handling', () => {
    it('should not log warning when getDraft returns null (no draft exists)', async () => {
      const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined)

      // Mock scene and draft responses
      vi.mocked(scenesApi.getById).mockResolvedValue({
        sceneId: 'scene-123',
        name: 'Test Scene',
        version: 1,
        createdAt: '2026-02-11T10:00:00.000Z',
        updatedAt: '2026-02-11T10:00:00.000Z',
        entities: [],
        paths: [],
      })

      // getDraft returns null when no draft exists (404)
      vi.mocked(scenesApi.getDraft).mockResolvedValue(null)

      await useSceneStore.getState().fetchScene('scene-123')

      // No warning should be logged for 404 (no draft)
      expect(warnSpy).not.toHaveBeenCalled()
      expect(useSceneStore.getState().draft).toBeNull()

      warnSpy.mockRestore()
    })

    it('should show recovery dialog when draft exists and content differs', async () => {
      // Mock scene response
      vi.mocked(scenesApi.getById).mockResolvedValue({
        sceneId: 'scene-123',
        name: 'Test Scene',
        version: 1,
        createdAt: '2026-02-11T10:00:00.000Z',
        updatedAt: '2026-02-11T10:00:00.000Z',
        entities: [],
        paths: [],
      })

      // Mock draft with different content
      vi.mocked(scenesApi.getDraft).mockResolvedValue({
        sceneId: 'scene-123',
        content: {
          sceneId: 'scene-123',
          name: 'Modified Scene', // Different name
          version: 1,
          createdAt: '2026-02-11T10:00:00.000Z',
          updatedAt: '2026-02-11T10:00:00.000Z',
          entities: [],
          paths: [],
        },
        savedAt: '2026-02-11T10:05:00.000Z',
        version: 1,
      })

      await useSceneStore.getState().fetchScene('scene-123')

      // Should show recovery dialog
      expect(useSceneStore.getState().showDraftRecoveryDialog).toBe(true)
      expect(useSceneStore.getState().draft).not.toBeNull()
    })

    it('should log warning when getDraft throws non-404 error', async () => {
      const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined)

      // Mock scene response
      vi.mocked(scenesApi.getById).mockResolvedValue({
        sceneId: 'scene-123',
        name: 'Test Scene',
        version: 1,
        createdAt: '2026-02-11T10:00:00.000Z',
        updatedAt: '2026-02-11T10:00:00.000Z',
        entities: [],
        paths: [],
      })

      // getDraft throws 500 error
      const mockError = { code: 'INTERNAL_SERVER_ERROR', message: 'Server error' }
      vi.mocked(scenesApi.getDraft).mockRejectedValue(mockError)

      await useSceneStore.getState().fetchScene('scene-123')

      // Warning should be logged for non-404 errors
      expect(warnSpy).toHaveBeenCalledWith('Failed to check for draft:', mockError)

      warnSpy.mockRestore()
    })
  })

  describe('patchCurrentScene - form editing sync', () => {
    const mockScene = {
      sceneId: 'scene-123',
      name: 'Original Name',
      description: 'Original Description',
      version: 1,
      createdAt: '2026-02-11T10:00:00.000Z',
      updatedAt: '2026-02-11T10:00:00.000Z',
      entities: [],
      paths: [],
    }

    beforeEach(() => {
      useSceneStore.setState({
        currentScene: mockScene,
      })
    })

    it('should update currentScene.name when patchCurrentScene is called with name', () => {
      useSceneStore.getState().patchCurrentScene({ name: 'Updated Name' })

      expect(useSceneStore.getState().currentScene?.name).toBe('Updated Name')
      // Other fields should remain unchanged
      expect(useSceneStore.getState().currentScene?.description).toBe('Original Description')
    })

    it('should update currentScene.description when patchCurrentScene is called with description', () => {
      useSceneStore.getState().patchCurrentScene({ description: 'Updated Description' })

      expect(useSceneStore.getState().currentScene?.description).toBe('Updated Description')
      // Other fields should remain unchanged
      expect(useSceneStore.getState().currentScene?.name).toBe('Original Name')
    })

    it('should support updating multiple fields at once', () => {
      useSceneStore.getState().patchCurrentScene({
        name: 'New Name',
        description: 'New Description',
      })

      expect(useSceneStore.getState().currentScene?.name).toBe('New Name')
      expect(useSceneStore.getState().currentScene?.description).toBe('New Description')
    })

    it('should do nothing when currentScene is null', () => {
      useSceneStore.setState({ currentScene: null })

      // Should not throw
      expect(() => {
        useSceneStore.getState().patchCurrentScene({ name: 'Test' })
      }).not.toThrow()

      expect(useSceneStore.getState().currentScene).toBeNull()
    })
  })

  describe('saveDraft - with currentScene updates', () => {
    const mockScene = {
      sceneId: 'scene-123',
      name: 'Test Scene',
      description: 'Test Description',
      version: 1,
      createdAt: '2026-02-11T10:00:00.000Z',
      updatedAt: '2026-02-11T10:00:00.000Z',
      entities: [],
      paths: [],
    }

    beforeEach(() => {
      vi.clearAllMocks()
      useSceneStore.setState({
        currentScene: mockScene,
        lastSavedContent: '',
        lastSavedAt: null,
        hasUnsavedChanges: true,
      })
    })

    it('should call saveDraft API when currentScene is updated', async () => {
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
      vi.mocked(scenesApi.saveDraft).mockResolvedValue({
        success: true,
        savedAt: '2026-02-11T10:05:00.000Z',
      })

      await useSceneStore.getState().saveDraft(0) // No debounce for test

      expect(scenesApi.saveDraft).toHaveBeenCalledWith('scene-123', mockScene)
      expect(useSceneStore.getState().lastSavedContent).toBeTruthy()
      // Check that log was called with POST success message
      expect(logSpy).toHaveBeenCalledWith(
        expect.stringContaining('[Draft] POST success'),
        expect.any(String) // CSS style string (second argument)
      )

      logSpy.mockRestore()
    })

    it('should call saveDraft again when currentScene changes', async () => {
      vi.mocked(scenesApi.saveDraft).mockResolvedValue({
        success: true,
        savedAt: '2026-02-11T10:05:00.000Z',
      })

      // First save
      await useSceneStore.getState().saveDraft(0)
      expect(scenesApi.saveDraft).toHaveBeenCalledTimes(1)

      // Update currentScene (simulating form edit)
      useSceneStore.getState().patchCurrentScene({ name: 'Updated Name' })

      // Second save should trigger (content changed)
      await useSceneStore.getState().saveDraft(0)
      expect(scenesApi.saveDraft).toHaveBeenCalledTimes(2)
      expect(scenesApi.saveDraft).toHaveBeenCalledWith('scene-123', {
        ...mockScene,
        name: 'Updated Name',
      })
    })

    it('should skip saveDraft when currentScene unchanged (de-duplication)', async () => {
      vi.mocked(scenesApi.saveDraft).mockResolvedValue({
        success: true,
        savedAt: '2026-02-11T10:05:00.000Z',
      })

      // First save
      await useSceneStore.getState().saveDraft(0)
      expect(scenesApi.saveDraft).toHaveBeenCalledTimes(1)

      // Second save without changes - should be skipped
      await useSceneStore.getState().saveDraft(0)
      expect(scenesApi.saveDraft).toHaveBeenCalledTimes(1) // Still 1, not called again
    })

    it('should respect throttle interval when saving', async () => {
      vi.mocked(scenesApi.saveDraft).mockResolvedValue({
        success: true,
        savedAt: '2026-02-11T10:05:00.000Z',
      })

      // First save
      await useSceneStore.getState().saveDraft(0)

      // Second save with different content, within throttle window - should be skipped
      useSceneStore.getState().patchCurrentScene({ name: 'Changed' })
      await useSceneStore.getState().saveDraft(60000) // 60s throttle, but we're testing throttle behavior

      expect(scenesApi.saveDraft).toHaveBeenCalledTimes(1) // Still 1, second was throttled

      // Third save after throttle interval - should trigger
      await useSceneStore.getState().saveDraft(0) // No throttle restriction

      expect(scenesApi.saveDraft).toHaveBeenCalledTimes(2) // Now 2, third save triggered
    })
  })

  describe('Auto-save: core logic tests', () => {
    const mockScene = {
      sceneId: 'scene-auto-save-test',
      name: 'Auto Save Test Scene',
      description: 'Testing auto-save',
      version: 1,
      createdAt: '2026-02-11T10:00:00.000Z',
      updatedAt: '2026-02-11T10:00:00.000Z',
      entities: [],
      paths: [],
    }

    beforeEach(() => {
      vi.clearAllMocks()
      useSceneStore.setState({
        currentScene: mockScene,
        lastSavedContent: '',
        lastSavedAt: null,
        hasUnsavedChanges: false,
      })
      vi.mocked(scenesApi.saveDraft).mockResolvedValue({
        success: true,
        savedAt: '2026-02-11T10:05:00.000Z',
      })
    })

    it('shouldTriggerSaveWhenSceneChanges', async () => {
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

      // Simulate user editing the scene
      useSceneStore.getState().patchCurrentScene({ name: 'Updated Name' })
      useSceneStore.getState().markUnsavedChanges()

      // Call saveDraft (simulating page useEffect timer firing)
      await useSceneStore.getState().saveDraft(0)

      // Should have called saveDraft API
      expect(scenesApi.saveDraft).toHaveBeenCalledWith('scene-auto-save-test', {
        ...mockScene,
        name: 'Updated Name',
      })

      // Verify logs - check all three log calls
      expect(logSpy).toHaveBeenCalledWith(
        expect.stringContaining('[Draft] saveDraft called')
      )
      expect(logSpy).toHaveBeenCalledWith(
        expect.stringContaining('[Draft] POST start')
      )
      expect(logSpy).toHaveBeenCalledWith(
        expect.stringContaining('[Draft] POST success'),
        expect.any(String) // CSS style string
      )

      logSpy.mockRestore()
    })

    it('shouldSkipSaveWhenNoScene', async () => {
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

      // Set currentScene to null
      useSceneStore.setState({ currentScene: null })

      // Call saveDraft
      await useSceneStore.getState().saveDraft(0)

      // Should not call API (null scene check)
      expect(scenesApi.saveDraft).not.toHaveBeenCalled()
      expect(logSpy).toHaveBeenCalledWith(
        '[Draft] skip: no-current-scene'
      )

      logSpy.mockRestore()
    })

    it('shouldSkipSaveWhenHashUnchanged', async () => {
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

      // Set up initial saved content
      useSceneStore.setState({
        lastSavedContent: JSON.stringify(mockScene),
        lastSavedAt: Date.now(),
      })

      // Edit scene
      useSceneStore.getState().patchCurrentScene({ name: mockScene.name })
      useSceneStore.getState().markUnsavedChanges()

      // Call saveDraft - should be skipped due to same hash
      await useSceneStore.getState().saveDraft(0)

      // Should not call API (same hash)
      expect(scenesApi.saveDraft).not.toHaveBeenCalled()
      expect(logSpy).toHaveBeenCalledWith(
        '[Draft] skip: unchanged-hash'
      )

      logSpy.mockRestore()
    })

    it('shouldSkipSaveWhenThrottled', async () => {
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

      const now = Date.now()
      useSceneStore.setState({
        lastSavedAt: now - 30000, // Saved 30 seconds ago
      })

      // Edit scene
      useSceneStore.getState().patchCurrentScene({ name: 'Updated' })
      useSceneStore.getState().markUnsavedChanges()

      // Call saveDraft with 60s throttle - should be skipped
      await useSceneStore.getState().saveDraft(60000)

      // Should not call API (throttled)
      expect(scenesApi.saveDraft).not.toHaveBeenCalled()
      expect(logSpy).toHaveBeenCalledWith(
        expect.stringContaining('[Draft] skip: throttled')
      )

      logSpy.mockRestore()
    })
  })

  describe('fetchScene - should not reset unsaved changes', () => {
    const mockScene = {
      sceneId: 'scene-fetch-test',
      name: 'Fetch Test Scene',
      description: 'Testing fetch behavior',
      version: 1,
      createdAt: '2026-02-11T10:00:00.000Z',
      updatedAt: '2026-02-11T10:00:00.000Z',
      entities: [],
      paths: [],
    }

    beforeEach(() => {
      vi.clearAllMocks()
      vi.mocked(scenesApi.getById).mockResolvedValue(mockScene)
      vi.mocked(scenesApi.getDraft).mockRejectedValue({ code: 'NOT_FOUND', message: 'Draft not found' })
    })

    it('should keep hasUnsavedChanges true when fetching same scene', async () => {
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

      // Set initial state with edits
      useSceneStore.setState({
        currentScene: { ...mockScene, name: 'Modified Name' },
        hasUnsavedChanges: true,
      })

      // Fetch the same scene
      await useSceneStore.getState().fetchScene('scene-fetch-test')

      // hasUnsavedChanges should still be true
      expect(useSceneStore.getState().hasUnsavedChanges).toBe(true)

      logSpy.mockRestore()
    })

    it('should reset hasUnsavedChanges when fetching different scene', async () => {
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

      // Set initial state with edits for a different scene
      useSceneStore.setState({
        currentScene: { sceneId: 'different-scene', name: 'Different', version: 1, createdAt: '', updatedAt: '', entities: [], paths: [] },
        hasUnsavedChanges: true,
      })

      // Fetch a different scene
      await useSceneStore.getState().fetchScene('scene-fetch-test')

      // hasUnsavedChanges should be reset to false
      expect(useSceneStore.getState().hasUnsavedChanges).toBe(false)

      logSpy.mockRestore()
    })

    it('should compare local currentScene with draft when preserveUnsavedChanges is true', async () => {
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

      // Local scene with edits (name = "Local Edited Name")
      const localScene = {
        ...mockScene,
        name: 'Local Edited Name',
      }
      useSceneStore.setState({
        currentScene: localScene,
        hasUnsavedChanges: true,
      })

      // Backend returns original scene (name = "Fetch Test Scene")
      vi.mocked(scenesApi.getById).mockResolvedValue(mockScene)

      // Draft matches LOCAL content (name = "Local Edited Name")
      vi.mocked(scenesApi.getDraft).mockResolvedValue({
        sceneId: 'scene-fetch-test',
        content: {
          ...mockScene,
          name: 'Local Edited Name', // Same as local edit
        },
        savedAt: '2026-02-11T10:05:00.000Z',
        version: 1,
      })

      // Fetch the same scene (preserveUnsavedChanges should be true)
      await useSceneStore.getState().fetchScene('scene-fetch-test')

      // Should NOT show recovery dialog because local content matches draft
      // (comparing local currentScene, not backend scene)
      expect(useSceneStore.getState().showDraftRecoveryDialog).toBe(false)

      logSpy.mockRestore()
    })

    it('should show recovery dialog when local currentScene differs from draft', async () => {
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => undefined)

      // Local scene with edits (name = "Local Edited Name")
      const localScene = {
        ...mockScene,
        name: 'Local Edited Name',
      }
      useSceneStore.setState({
        currentScene: localScene,
        hasUnsavedChanges: true,
      })

      // Backend returns original scene (name = "Fetch Test Scene")
      vi.mocked(scenesApi.getById).mockResolvedValue(mockScene)

      // Draft has different content (name = "Draft Saved Name")
      vi.mocked(scenesApi.getDraft).mockResolvedValue({
        sceneId: 'scene-fetch-test',
        content: {
          ...mockScene,
          name: 'Draft Saved Name', // Different from local edit
        },
        savedAt: '2026-02-11T10:05:00.000Z',
        version: 1,
      })

      // Fetch the same scene (preserveUnsavedChanges should be true)
      await useSceneStore.getState().fetchScene('scene-fetch-test')

      // Should show recovery dialog because local content differs from draft
      expect(useSceneStore.getState().showDraftRecoveryDialog).toBe(true)

      logSpy.mockRestore()
    })
  })
})
