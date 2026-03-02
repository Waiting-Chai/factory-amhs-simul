/**
 * Scene store using Zustand for scene management state.
 *
 * Implements 5.1: Project/Scene Management Module
 * - Scene list, create, edit, delete
 * - Scene copy
 * - Scene import/export JSON
 * - Draft auto-save (1 minute de-duplication)
 * - Draft recovery prompt
 *
 * Implements 5.3: Editor Efficiency Enhancement
 * - Undo/Redo with history stack (max 50)
 * - Save status visualization
 * - Batch entity operations
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-10
 */
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import type {
  SceneSummary,
  SceneDetail,
  SceneDraftPayload,
  EntityType,
  Entity,
} from '../types'
import { scenesApi } from '@api/scenes'

// Maximum undo stack depth
const MAX_HISTORY_DEPTH = 50

// Save status type for UI visualization
export type SaveStatus = 'idle' | 'saving' | 'saved' | 'dirty'

// History snapshot for undo/redo - stores minimal scene data for size efficiency
interface HistorySnapshot {
  entities: SceneDetail['entities']
  paths: SceneDetail['paths']
  processSteps?: SceneDetail['processSteps']
}

/**
 * Remove frontend runtime-only fields from payload recursively.
 */
const sanitizeRuntimeFields = <T>(value: T): T => {
  if (Array.isArray(value)) {
    return value.map(item => sanitizeRuntimeFields(item)) as T
  }

  if (value && typeof value === 'object') {
    const cleanObject: Record<string, unknown> = {}
    Object.entries(value as Record<string, unknown>).forEach(([key, childValue]) => {
      // Drop runtime fields and editor-only extensions before API save.
      if (key.startsWith('_') || key === 'extensions') {
        return
      }
      cleanObject[key] = sanitizeRuntimeFields(childValue)
    })
    return cleanObject as T
  }

  return value
}

/**
 * Sanitize scene data for API save.
 * Removes runtime-only fields that should not be persisted to backend.
 */
const sanitizeSceneForSave = (scene: SceneDetail): Partial<SceneDetail> => {
  const {
    sceneId,
    name,
    description,
    entities,
    paths,
    processSteps,
    safetyZones,
    processFlows,
  } = scene

  const sanitizedEntities = sanitizeRuntimeFields(entities)
  const sanitizedPaths = sanitizeRuntimeFields(paths)
  const sanitizedProcessSteps = sanitizeRuntimeFields(processSteps)
  const sanitizedSafetyZones = sanitizeRuntimeFields(safetyZones)
  const sanitizedProcessFlows = sanitizeRuntimeFields(processFlows)

  return {
    sceneId,
    name,
    description,
    entities: sanitizedEntities,
    paths: sanitizedPaths,
    processSteps: sanitizedProcessSteps,
    safetyZones: sanitizedSafetyZones,
    processFlows: sanitizedProcessFlows,
  }
}

/**
 * Extract error message from unknown error type
 */
const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message
  }
  return String(error)
}

const isValidScenePagedResult = (value: unknown): value is {
  items: SceneSummary[]
  total: number
  page: number
  pageSize: number
  totalPages: number
} => {
  if (!value || typeof value !== 'object') {
    return false
  }

  const payload = value as Record<string, unknown>
  return (
    Array.isArray(payload.items) &&
    typeof payload.total === 'number' &&
    typeof payload.page === 'number' &&
    typeof payload.pageSize === 'number' &&
    typeof payload.totalPages === 'number'
  )
}

/**
 * Scene store state
 */
interface SceneState {
  // Data
  scenes: SceneSummary[]
  currentScene: SceneDetail | null
  draft: SceneDraftPayload | null
  pagination: {
    page: number
    pageSize: number
    total: number
    totalPages: number
  }

  // UI State
  isLoading: boolean
  error: string | null
  searchQuery: string
  filterType: string | null
  showDraftRecoveryDialog: boolean
  hasUnsavedChanges: boolean
  editorUi: {
    mode: 'select' | 'place' | 'connect' | 'area'
    selectedEntityIds: string[]
    selectedSegmentIds: string[]
    transformMode: 'translate' | 'rotate' | 'scale'
    viewport: {
      x: number
      z: number
      zoom: number
    }
    snapEnabled: boolean
    showGrid: boolean
    pendingConnectNodeId: string | null
    pathSegmentType: 'LINEAR' | 'BEZIER'
    activePathType: 'OHT_PATH' | 'AGV_NETWORK'
    placingType: EntityType | null
  }

  // Draft auto-save
  lastSavedContent: string
  lastSavedAt: number | null

  // Save status for UI visualization
  saveStatus: SaveStatus

  // Undo/Redo history
  undoStack: HistorySnapshot[]
  redoStack: HistorySnapshot[]

  // Actions
  fetchScenes: (page?: number) => Promise<void>
  fetchScene: (id: string) => Promise<void>
  createScene: (data: Partial<SceneDetail>) => Promise<SceneDetail>
  updateScene: (id: string, data: Partial<SceneDetail>) => Promise<void>
  deleteScene: (id: string) => Promise<void>
  copyScene: (id: string, name?: string) => Promise<void>
  importScene: (file: File) => Promise<void>
  exportScene: (id: string) => Promise<void>

  // Patch current scene (for form editing - sync to store without network request)
  patchCurrentScene: (patch: Partial<SceneDetail>) => void

  // Draft actions
  fetchDraft: (id: string) => Promise<void>
  saveDraft: (debounceMs?: number) => Promise<void>
  discardDraft: (id: string) => Promise<void>
  recoverDraft: () => void
  dismissDraftRecovery: () => void

  // UI actions
  setSearchQuery: (query: string) => void
  setFilterType: (type: string | null) => void
  setCurrentScene: (scene: SceneDetail | null) => void
  setEditorMode: (mode: 'select' | 'place' | 'connect' | 'area') => void
  setEditorSelectedEntityIds: (ids: string[]) => void
  addToEditorSelectedEntityIds: (ids: string[]) => void
  setEditorSelectedSegmentIds: (ids: string[]) => void
  setEditorTransformMode: (mode: 'translate' | 'rotate' | 'scale') => void
  setEditorViewport: (viewport: { x: number; z: number; zoom: number }) => void
  setEditorSnapEnabled: (enabled: boolean) => void
  setEditorShowGrid: (visible: boolean) => void
  setEditorPendingConnectNodeId: (id: string | null) => void
  setEditorPathSegmentType: (type: 'LINEAR' | 'BEZIER') => void
  setEditorActivePathType: (type: 'OHT_PATH' | 'AGV_NETWORK') => void
  setEditorPlacingType: (type: EntityType | null) => void
  resetEditorUi: () => void
  markUnsavedChanges: () => void
  clearUnsavedChanges: () => void
  clearError: () => void

  // Undo/Redo actions
  undo: () => void
  redo: () => void
  pushHistory: () => void
  canUndo: () => boolean
  canRedo: () => boolean
  clearHistory: () => void

  // Batch operations
  batchUpdateEntities: (updates: Array<{ id: string; changes: Partial<Entity> }>) => void
  batchDeleteEntities: (ids: string[]) => void
}

/**
 * Hash function for content comparison (de-duplication)
 */
const hashContent = (content: unknown): string => {
  return JSON.stringify(content)
}

/**
 * Scene store
 */
export const useSceneStore = create<SceneState>()(
  devtools(
    (set, get) => ({
      // Initial state
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
      editorUi: {
        mode: 'select',
        selectedEntityIds: [],
        selectedSegmentIds: [],
        transformMode: 'translate',
        viewport: { x: 0, z: 0, zoom: 24 },
        snapEnabled: true,
        showGrid: true,
        pendingConnectNodeId: null,
        pathSegmentType: 'LINEAR',
        activePathType: 'OHT_PATH',
        placingType: null,
      },
      lastSavedContent: '',
      lastSavedAt: null,
      saveStatus: 'idle',
      undoStack: [],
      redoStack: [],

      // Fetch scene list
      fetchScenes: async (page = 1) => {
        set({ isLoading: true, error: null })
        try {
          const state = get()
          const { searchQuery, filterType, pagination } = state
          const response = await scenesApi.list(
            page,
            pagination.pageSize,
            searchQuery || undefined,
            filterType || undefined
          )

          if (!isValidScenePagedResult(response)) {
            set({
              scenes: [],
              error: 'Invalid scene list response payload',
            })
            return
          }

          set({
            scenes: response.items,
            pagination: {
              // Convert 0-based from backend to 1-based for frontend display
              page: response.page + 1,
              pageSize: response.pageSize,
              total: response.total,
              totalPages: response.totalPages,
            },
          })
        } catch (error: unknown) {
          set({
            scenes: [],
            error: getErrorMessage(error) || 'Failed to fetch scenes',
          })
        } finally {
          set({ isLoading: false })
        }
      },

      // Fetch single scene
      fetchScene: async (id: string) => {
        set({ isLoading: true, error: null })
        try {
          const scene = await scenesApi.getById(id)

          // Preserve hasUnsavedChanges if we're loading the same scene
          // This prevents resetting user edits when fetchScene is called
          const currentState = get()
          const preserveUnsavedChanges =
            currentState.currentScene?.sceneId === scene.sceneId &&
            currentState.hasUnsavedChanges

          console.log(
            `[SceneStore] fetchScene: sceneId=${scene.sceneId}, preserveUnsavedChanges=${preserveUnsavedChanges}, currentHasUnsavedChanges=${currentState.hasUnsavedChanges}`
          )

          // Conditionally update state based on whether we have unsaved changes
          if (preserveUnsavedChanges) {
            // Keep local edits: don't overwrite currentScene, only clear loading
            console.log('[SceneStore] fetchScene skipped overwrite due to unsaved changes')
            set({ isLoading: false })
          } else {
            // Normal load: update currentScene from backend
            set({
              currentScene: scene,
              isLoading: false,
              hasUnsavedChanges: false,
            })
          }

          // Check for draft after loading scene (non-blocking)
          // 404 for draft means no draft exists, which is expected
          try {
            const draft = await scenesApi.getDraft(id)
            if (draft) {
              // Use local currentScene if available (preserves unsaved edits),
              // otherwise fall back to backend scene
              const effectiveCurrentScene = get().currentScene ?? scene
              const currentHash = hashContent(effectiveCurrentScene)
              const draftHash = hashContent(draft.content)
              if (currentHash !== draftHash) {
                set({ draft, showDraftRecoveryDialog: true })
              }
            }
          } catch (draftError: unknown) {
            // Draft check errors should not affect scene loading
            console.warn('Failed to check for draft:', draftError)
          }
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to fetch scene',
            isLoading: false,
          })
        }
      },

      // Create new scene
      createScene: async (data: Partial<SceneDetail>) => {
        set({ isLoading: true, error: null })
        try {
          const scene = await scenesApi.create(data)
          set({ isLoading: false })
          await get().fetchScenes() // Refresh list
          return scene
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to create scene',
            isLoading: false,
          })
          throw error
        }
      },

      // Update scene with sanitization
      updateScene: async (id: string, data: Partial<SceneDetail>) => {
        set({ isLoading: true, error: null, saveStatus: 'saving' })
        try {
          // Sanitize data to remove runtime fields before sending to backend
          const sanitizedData = data.entities && data.paths
            ? sanitizeSceneForSave(data as SceneDetail)
            : data
          const scene = await scenesApi.update(id, sanitizedData)
          set({
            currentScene: scene,
            isLoading: false,
            hasUnsavedChanges: false,
            saveStatus: 'saved',
          })
          // Clear history after successful save
          get().clearHistory()
          await get().fetchScenes() // Refresh list
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to update scene',
            isLoading: false,
            saveStatus: 'dirty',
          })
          throw error
        }
      },

      // Delete scene
      deleteScene: async (id: string) => {
        set({ isLoading: true, error: null })
        try {
          await scenesApi.delete(id)
          set({ isLoading: false })
          if (get().currentScene?.sceneId === id) {
            set({ currentScene: null })
          }
          await get().fetchScenes() // Refresh list
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to delete scene',
            isLoading: false,
          })
          throw error
        }
      },

      // Copy scene
      copyScene: async (id: string, name?: string) => {
        set({ isLoading: true, error: null })
        try {
          await scenesApi.copy(id, name)
          set({ isLoading: false })
          await get().fetchScenes() // Refresh list
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to copy scene',
            isLoading: false,
          })
          throw error
        }
      },

      // Import scene from JSON
      importScene: async (file: File) => {
        set({ isLoading: true, error: null })
        try {
          await scenesApi.import(file)
          set({ isLoading: false })
          await get().fetchScenes() // Refresh list
        } catch (error: unknown) {
          set({
            error: getErrorMessage(error) || 'Failed to import scene',
            isLoading: false,
          })
          throw error
        }
      },

      // Export scene to JSON
      exportScene: async (id: string) => {
        try {
          await scenesApi.exportScene(id)
        } catch (error: unknown) {
          set({ error: getErrorMessage(error) || 'Failed to export scene' })
          throw error
        }
      },

      // Fetch draft
      fetchDraft: async (id: string) => {
        try {
          const draft = await scenesApi.getDraft(id)
          set({ draft })
        } catch (error: unknown) {
          set({ error: getErrorMessage(error) || 'Failed to fetch draft' })
        }
      },

      // Save draft with de-duplication (1 minute interval)
      saveDraft: async (debounceMs = 60000) => {
        const { currentScene, lastSavedContent, lastSavedAt, hasUnsavedChanges } = get()
        if (!currentScene) {
          console.log('[Draft] skip: no-current-scene')
          return
        }

        const now = Date.now()
        const currentHash = hashContent(currentScene)

        // Log entry point with full state
        console.log(
          `[Draft] saveDraft called: sceneId=${currentScene.sceneId}, hasUnsavedChanges=${hasUnsavedChanges}, hashChanged=${currentHash !== lastSavedContent}, lastSavedAt=${lastSavedAt}, debounceMs=${debounceMs}`
        )

        // De-duplication: only save if content changed
        if (currentHash === lastSavedContent) {
          console.log('[Draft] skip: unchanged-hash')
          return
        }

        // Throttling: only save if debounceMs passed since last save
        if (lastSavedAt && now - lastSavedAt < debounceMs) {
          const timeUntilNextSave = debounceMs - (now - lastSavedAt)
          console.log(`[Draft] skip: throttled (${timeUntilNextSave}ms remaining)`)
          return
        }

        try {
          console.log(`[Draft] POST start: sceneId=${currentScene.sceneId}, url=/api/v1/scenes/${currentScene.sceneId}/draft`)
          await scenesApi.saveDraft(currentScene.sceneId, currentScene)
          set({
            lastSavedContent: currentHash,
            lastSavedAt: now,
            hasUnsavedChanges: false,
          })
          // Debug log for auto-save confirmation
          console.log(
            `%c[Draft] POST success: sceneId=${currentScene.sceneId}, savedAt=${new Date(now).toISOString()}`,
            'color: #10b981; font-weight: bold;'
          )
        } catch (error: unknown) {
          console.error('[Draft] POST failed:', error)
          throw error
        }
      },

      // Discard draft
      discardDraft: async (id: string) => {
        try {
          await scenesApi.deleteDraft(id)
          set({ draft: null })
        } catch (error: unknown) {
          set({ error: getErrorMessage(error) || 'Failed to discard draft' })
        }
      },

      // Recover draft
      recoverDraft: () => {
        const { draft } = get()
        if (draft) {
          set({ currentScene: draft.content, showDraftRecoveryDialog: false })
        }
      },

      // Dismiss draft recovery dialog
      dismissDraftRecovery: () => {
        set({ showDraftRecoveryDialog: false })
      },

      // UI actions
      setSearchQuery: (query: string) => {
        set({ searchQuery: query })
        get().fetchScenes(1) // Reset to first page when searching
      },

      setFilterType: (type: string | null) => {
        set({ filterType: type })
        get().fetchScenes(1) // Reset to first page when filtering
      },

      setCurrentScene: (scene: SceneDetail | null) => {
        set({ currentScene: scene, draft: null })
      },

      setEditorMode: (mode) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            mode,
            pendingConnectNodeId: mode === 'connect' ? state.editorUi.pendingConnectNodeId : null,
          },
        }))
      },

      setEditorSelectedEntityIds: (ids) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            selectedEntityIds: ids,
            // When selecting entities, clear segment selection unless we want multi-type selection
            selectedSegmentIds: [],
          },
        }))
      },

      // Add to selection (for shift+click and box selection)
      addToEditorSelectedEntityIds: (ids) => {
        set((state) => {
          const currentIds = new Set(state.editorUi.selectedEntityIds)
          ids.forEach(id => currentIds.add(id))
          return {
            editorUi: {
              ...state.editorUi,
              selectedEntityIds: Array.from(currentIds),
              selectedSegmentIds: [],
            },
          }
        })
      },

      setEditorSelectedSegmentIds: (ids) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            selectedSegmentIds: ids,
            // When selecting segments, clear entity selection
            selectedEntityIds: [],
          },
        }))
      },

      setEditorTransformMode: (mode) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            transformMode: mode,
          },
        }))
      },

      setEditorViewport: (viewport) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            viewport,
          },
        }))
      },

      setEditorSnapEnabled: (enabled) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            snapEnabled: enabled,
          },
        }))
      },

      setEditorShowGrid: (visible) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            showGrid: visible,
          },
        }))
      },

      setEditorPendingConnectNodeId: (id) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            pendingConnectNodeId: id,
          },
        }))
      },

      setEditorPathSegmentType: (type) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            pathSegmentType: type,
          },
        }))
      },

      setEditorActivePathType: (type) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            activePathType: type,
          },
        }))
      },

      setEditorPlacingType: (type) => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            placingType: type,
          },
        }))
      },

      resetEditorUi: () => {
        set((state) => ({
          editorUi: {
            ...state.editorUi,
            mode: 'select',
            selectedEntityIds: [],
            selectedSegmentIds: [],
            pendingConnectNodeId: null,
          },
        }))
      },

      markUnsavedChanges: () => {
        set({ hasUnsavedChanges: true, saveStatus: 'dirty' })
      },

      clearUnsavedChanges: () => {
        set({ hasUnsavedChanges: false, saveStatus: 'saved' })
      },

      clearError: () => {
        set({ error: null })
      },

      // Patch current scene (for form editing - sync to store without network request)
      patchCurrentScene: (patch: Partial<SceneDetail>) => {
        const { currentScene } = get()
        if (currentScene) {
          set({ currentScene: { ...currentScene, ...patch } })
        }
      },

      // ==================== Undo/Redo Actions ====================

      /**
       * Push current scene state to undo history.
       * Should be called BEFORE making changes.
       */
      pushHistory: () => {
        const { currentScene, undoStack } = get()
        if (!currentScene) return

        const snapshot: HistorySnapshot = {
          entities: JSON.parse(JSON.stringify(currentScene.entities)),
          paths: JSON.parse(JSON.stringify(currentScene.paths)),
          processSteps: currentScene.processSteps
            ? JSON.parse(JSON.stringify(currentScene.processSteps))
            : undefined,
        }

        // Add to undo stack, limit depth
        const newUndoStack = [...undoStack, snapshot].slice(-MAX_HISTORY_DEPTH)
        set({ undoStack: newUndoStack, redoStack: [] })
      },

      /**
       * Undo last change - restore from undo stack
       */
      undo: () => {
        const { currentScene, undoStack, redoStack } = get()
        if (!currentScene || undoStack.length === 0) return

        // Save current state to redo stack
        const currentSnapshot: HistorySnapshot = {
          entities: JSON.parse(JSON.stringify(currentScene.entities)),
          paths: JSON.parse(JSON.stringify(currentScene.paths)),
          processSteps: currentScene.processSteps
            ? JSON.parse(JSON.stringify(currentScene.processSteps))
            : undefined,
        }

        // Pop from undo stack
        const newUndoStack = [...undoStack]
        const previousSnapshot = newUndoStack.pop()

        if (previousSnapshot) {
          set({
            currentScene: {
              ...currentScene,
              entities: previousSnapshot.entities,
              paths: previousSnapshot.paths,
              processSteps: previousSnapshot.processSteps,
            },
            undoStack: newUndoStack,
            redoStack: [...redoStack, currentSnapshot],
            hasUnsavedChanges: true,
            saveStatus: 'dirty',
          })
        }
      },

      /**
       * Redo last undone change - restore from redo stack
       */
      redo: () => {
        const { currentScene, undoStack, redoStack } = get()
        if (!currentScene || redoStack.length === 0) return

        // Save current state to undo stack
        const currentSnapshot: HistorySnapshot = {
          entities: JSON.parse(JSON.stringify(currentScene.entities)),
          paths: JSON.parse(JSON.stringify(currentScene.paths)),
          processSteps: currentScene.processSteps
            ? JSON.parse(JSON.stringify(currentScene.processSteps))
            : undefined,
        }

        // Pop from redo stack
        const newRedoStack = [...redoStack]
        const nextSnapshot = newRedoStack.pop()

        if (nextSnapshot) {
          set({
            currentScene: {
              ...currentScene,
              entities: nextSnapshot.entities,
              paths: nextSnapshot.paths,
              processSteps: nextSnapshot.processSteps,
            },
            undoStack: [...undoStack, currentSnapshot],
            redoStack: newRedoStack,
            hasUnsavedChanges: true,
            saveStatus: 'dirty',
          })
        }
      },

      /**
       * Check if undo is available
       */
      canUndo: () => get().undoStack.length > 0,

      /**
       * Check if redo is available
       */
      canRedo: () => get().redoStack.length > 0,

      /**
       * Clear undo/redo history
       */
      clearHistory: () => {
        set({ undoStack: [], redoStack: [] })
      },

      // ==================== Batch Operations ====================

      /**
       * Batch update multiple entities at once (for multi-select move)
       */
      batchUpdateEntities: (updates: Array<{ id: string; changes: Partial<Entity> }>) => {
        const { currentScene } = get()
        if (!currentScene) return

        const updatedEntities = currentScene.entities.map(entity => {
          const update = updates.find(u => u.id === entity.id)
          if (update) {
            return { ...entity, ...update.changes }
          }
          return entity
        })

        set({
          currentScene: { ...currentScene, entities: updatedEntities },
          hasUnsavedChanges: true,
          saveStatus: 'dirty',
        })
      },

      /**
       * Batch delete multiple entities at once
       */
      batchDeleteEntities: (ids: string[]) => {
        const { currentScene } = get()
        if (!currentScene) return

        const deletedIds = new Set(ids)
        const updatedEntities = currentScene.entities.filter(e => !deletedIds.has(e.id))

        // Cascade delete related path segments (segments referencing deleted nodes)
        const updatedPaths = currentScene.paths.map(path => {
          const filteredSegments = path.segments?.filter(s => {
            const fromId = s.from.startsWith('node-') ? s.from : `node-${s.from}`
            const toId = s.to.startsWith('node-') ? s.to : `node-${s.to}`
            return !deletedIds.has(fromId.replace('node-', '')) && !deletedIds.has(toId.replace('node-', ''))
          }) || []
          const filteredPoints = path.points.filter(p => !deletedIds.has(p.id.replace('node-', '')))
          return {
            ...path,
            segments: filteredSegments,
            points: filteredPoints,
          }
        })

        set({
          currentScene: {
            ...currentScene,
            entities: updatedEntities,
            paths: updatedPaths,
          },
          hasUnsavedChanges: true,
          saveStatus: 'dirty',
        })
      },
    }),
    { name: 'SceneStore' }
  )
)

export default useSceneStore
