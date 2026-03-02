/**
 * Scene Editor Page - 5.3 Scene Editor Module.
 *
 * @author shentw
 * @version 1.5
 * @since 2026-02-12
 */

import React, { useEffect, useCallback, useState, useRef } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useSceneStore } from '@store/sceneStore'
import { useToastStore } from '@store/toastStore'
import { useModelStore } from '@store/modelStore'
import { useGlbDiagStore } from '@store/glbDiagStore'
import { ErrorState, LoadingState } from '@components/ErrorState'
import { EditorCanvas } from '../../components/scene-editor/EditorCanvas'
import { EditorToolbar } from '../../components/scene-editor/EditorToolbar'
import { PalettePanel } from '../../components/scene-editor/PalettePanel'
import { PropertiesPanel } from '../../components/scene-editor/PropertiesPanel'
import { validateTransportTypeCompatibility } from '@/utils/transportTypeValidator'
import { validatePathTopology } from '@/utils/pathTopologyValidator'
import { validateNetworkSemantic, SemanticValidationResult } from '@/utils/networkSemanticValidator'
import type { Entity, EntityType, Path, PathSegment } from '@/types/api'
import { t, tf } from '@/shared/i18n'

const LAST_SCENE_ID_KEY = 'lastSceneId'

const setLastSceneId = (id: string): void => {
  try {
    localStorage.setItem(LAST_SCENE_ID_KEY, id)
  } catch {
    // ignore persistence failure
  }
}

const buildConnectedPaths = (
  paths: Path[],
  entities: Entity[],
  startNodeId: string,
  endNodeId: string,
  segmentType: 'LINEAR' | 'BEZIER',
  pathType: 'OHT_PATH' | 'AGV_NETWORK'
): Path[] => {
  const startNode = entities.find((item) => item.id === startNodeId)
  const endNode = entities.find((item) => item.id === endNodeId)
  if (!startNode || !endNode) return paths

  const startPointId = `node-${startNodeId}`
  const endPointId = `node-${endNodeId}`

  const nextPaths = [...paths]
  // Find existing path of same type
  const pathIndex = nextPaths.findIndex((item) => item.type === pathType)

  const path: Path = pathIndex >= 0
    ? {
      ...nextPaths[pathIndex],
      points: [...nextPaths[pathIndex].points],
      segments: [...(nextPaths[pathIndex].segments ?? [])],
      controlPoints: [...(nextPaths[pathIndex].controlPoints ?? [])],
    }
    : {
      id: `path-${Date.now()}`,
      type: pathType,
      name: pathType === 'OHT_PATH' ? 'OHT Path' : 'AGV Network',
      points: [],
      segments: [],
      controlPoints: [],
    }

  const upsertPoint = (pointId: string, entity: Entity): void => {
    const idx = path.points.findIndex((point) => point.id === pointId)
    // For AGV network, force y=0. For OHT, keep entity's y.
    const position = {
      ...entity.position,
      y: pathType === 'AGV_NETWORK' ? 0 : entity.position.y
    }
    const point = { id: pointId, position }
    if (idx >= 0) {
      path.points[idx] = point
    } else {
      path.points.push(point)
    }
  }

  upsertPoint(startPointId, startNode)
  upsertPoint(endPointId, endNode)

  const hasDuplicateSegment = (path.segments ?? []).some(
    (segment) => segment.from === startPointId && segment.to === endPointId
  )

  if (!hasDuplicateSegment) {
    const nextSegmentId = `segment-${Date.now()}`
    
    // For AGV network, force y=0 for control points. For OHT, interpolate.
    const getControlPointY = (t: number) => {
      if (pathType === 'AGV_NETWORK') return 0
      return startNode.position.y + (endNode.position.y - startNode.position.y) * t
    }

    const segment = {
      id: nextSegmentId,
      type: segmentType,
      from: startPointId,
      to: endPointId,
      ...(segmentType === 'BEZIER'
        ? {
          c1: {
            x: startNode.position.x + (endNode.position.x - startNode.position.x) / 3,
            y: getControlPointY(1/3),
            z: startNode.position.z + (endNode.position.z - startNode.position.z) / 3,
          },
          c2: {
            x: startNode.position.x + ((endNode.position.x - startNode.position.x) * 2) / 3,
            y: getControlPointY(2/3),
            z: startNode.position.z + (endNode.position.z - startNode.position.z) * 2/3,
          },
        }
        : {}),
    }

    path.segments = [...(path.segments ?? []), segment]
  }

  if (pathIndex >= 0) {
    nextPaths[pathIndex] = path
  } else {
    nextPaths.push(path)
  }

  return nextPaths
}

const DraftRecoveryDialog: React.FC<{
  draftSavedAt: string
  onRecover: () => void
  onDismiss: () => void
}> = ({ draftSavedAt, onRecover, onDismiss }) => {
  return (
    <div className="fixed inset-0 bg-black/70 backdrop-blur flex items-center justify-center z-50">
      <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-6 w-full max-w-md">
        <h2 className="text-xl font-display font-bold text-white mb-4">
          {t('sceneEditor.draft.title')}
        </h2>
        <p className="text-gray-400 text-sm mb-4">
          {tf('sceneEditor.draft.message', { time: new Date(draftSavedAt).toLocaleString() })}
        </p>
        <div className="flex justify-end gap-3">
          <button
            onClick={onDismiss}
            className="px-4 py-2 text-gray-400 hover:text-white transition-colors"
          >
            {t('sceneEditor.draft.dismiss')}
          </button>
          <button
            onClick={onRecover}
            className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors font-bold"
          >
            {t('sceneEditor.draft.recover')}
          </button>
        </div>
      </div>
    </div>
  )
}

const GlbDiagPanel = () => {
  // Call hook unconditionally to follow React Hooks rules
  const { events, isOpen, toggleOpen, clear } = useGlbDiagStore()

  if (!import.meta.env.DEV) return null
  
  // Task D: Summary counters
  const summary = {
    entities: new Set(events.filter(e => e.type === 'ENTITY_MESH_MOUNTED').map(e => e.entityId)).size,
    resolved: events.filter(e => e.type === 'ENTITY_RESOLVED').length,
    noUrl: events.filter(e => e.type === 'ENTITY_MESH_NO_URL').length,
    gltfSuccess: events.filter(e => e.type === 'GLTF_LOAD_SUCCESS').length,
    gltfFailed: events.filter(e => e.type === 'GLTF_LOAD_FAILED').length
  }
  
  if (!isOpen) {
    return (
      <button 
        onClick={toggleOpen}
        className="fixed bottom-4 right-4 bg-gray-800 text-xs text-white px-2 py-1 rounded shadow opacity-50 hover:opacity-100 z-50"
      >
        GLB Diag
      </button>
    )
  }

  return (
    <div className="fixed bottom-4 right-4 w-96 h-96 bg-gray-900/95 border border-gray-700 rounded shadow-xl flex flex-col z-50 text-xs font-mono">
      <div className="flex items-center justify-between p-2 border-b border-gray-700 bg-gray-800">
        <span className="font-bold text-gray-300">GLB Diagnostics</span>
        <div className="flex gap-2">
          <button 
            onClick={async () => {
              try {
                await navigator.clipboard.writeText(JSON.stringify(events, null, 2))
                console.info('[GLB-DIAG] copied diagnostics to clipboard')
              } catch (error) {
                console.warn('[GLB-DIAG] failed to copy diagnostics', error)
              }
            }}
            className="text-blue-400 hover:text-blue-300"
          >
            Copy
          </button>
          <button onClick={clear} className="text-gray-400 hover:text-white">Clear</button>
          <button onClick={toggleOpen} className="text-gray-400 hover:text-white">Close</button>
        </div>
      </div>
      <div className="p-2 border-b border-gray-700 bg-gray-800/50 grid grid-cols-3 gap-1 text-[10px]">
        <div className="text-gray-400">Entities: <span className="text-white">{summary.entities}</span></div>
        <div className="text-emerald-400">Resolved: <span className="text-white">{summary.resolved}</span></div>
        <div className="text-amber-400">No URL: <span className="text-white">{summary.noUrl}</span></div>
        <div className="text-emerald-400">GLTF OK: <span className="text-white">{summary.gltfSuccess}</span></div>
        <div className="text-red-400">GLTF Fail: <span className="text-white">{summary.gltfFailed}</span></div>
      </div>
      <div className="flex-1 overflow-auto p-2 space-y-1">
        {events.slice(0, 50).map((e) => (
          <div key={e.id} className="border-b border-gray-800 pb-1 mb-1">
            <div className="flex justify-between text-gray-500 text-[10px]">
              <span>{e.at.split('T')[1].slice(0, 12)}</span>
              <span className={e.type.includes('FAIL') || e.type.includes('NO_URL') ? 'text-red-400' : 'text-emerald-400'}>{e.type}</span>
            </div>
            {e.entityId && <div className="text-gray-300">Entity: {e.entityId}</div>}
            {e.modelId && <div className="text-gray-400">Model: {e.modelId}</div>}
            {e.url && <div className="text-gray-500 break-all">{e.url}</div>}
            {e.details && <pre className="text-[10px] text-gray-600 mt-1 overflow-hidden">{JSON.stringify(e.details)}</pre>}
          </div>
        ))}
      </div>
    </div>
  )
}

const SceneEditorPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { addToast } = useToastStore()

  const {
    currentScene,
    draft,
    isLoading,
    error,
    showDraftRecoveryDialog,
    hasUnsavedChanges,
    updateScene,
    saveDraft,
    recoverDraft,
    dismissDraftRecovery,
    markUnsavedChanges,
    patchCurrentScene,
    clearError,
    resetEditorUi,
    editorUi,
    setEditorMode,
    setEditorTransformMode,
    setEditorSnapEnabled,
    setEditorShowGrid,
    setEditorPathSegmentType,
    setEditorPlacingType,
    setEditorSelectedEntityIds,
    addToEditorSelectedEntityIds,
    setEditorSelectedSegmentIds,
    setEditorPendingConnectNodeId,
    setEditorActivePathType,
    // Undo/Redo
    undo,
    redo,
    pushHistory,
    canUndo,
    canRedo,
    saveStatus,
    // Batch operations
    batchDeleteEntities,
  } = useSceneStore()
  
  const { fetchBindings, fetchModelVersions, fetchAllActiveModelsForEditor, bindings, models } = useModelStore()

  const [isSaving, setIsSaving] = useState(false)
  const [lastAutoSave, setLastAutoSave] = useState<Date | null>(null)

  const lastFetchedIdRef = useRef<string | null>(null)
  const preloadedModelIdsRef = useRef<Set<string>>(new Set())

  // Task A: Preload models and versions
  useEffect(() => {
    if (id && lastFetchedIdRef.current !== id) {
      lastFetchedIdRef.current = id
      setLastSceneId(id)
      resetEditorUi()
      preloadedModelIdsRef.current.clear()
      
      void useSceneStore.getState().fetchScene(id)
      
      // Task B: Fetch ALL active models for editor (not just page 1)
      void fetchAllActiveModelsForEditor().then(() => {
        // Task B: Log active models stats
        const allModels = useModelStore.getState().models
        const counts: Record<string, number> = {}
        const typesOfInterest = ['OHT_VEHICLE', 'AGV_VEHICLE', 'MACHINE', 'STOCKER', 'CONVEYOR']
         typesOfInterest.forEach(t => {
           counts[t] = allModels.filter(m => m.type === t).length
         })
         
         // Task A: console.info
         // Task B: glbDiagStore
         useGlbDiagStore.getState().pushDiag('MODELS_LOADED', {
            details: { total: allModels.length, byType: counts }
         })
       })
     }
   }, [id, resetEditorUi, fetchAllActiveModelsForEditor])

  useEffect(() => {
    if (models.length > 0) {
      // Task A: SCENE_ENTITIES_SNAPSHOT
      if (currentScene) {
        const entityCounts: Record<string, number> = {}
        currentScene.entities.forEach(e => {
          entityCounts[e.type] = (entityCounts[e.type] || 0) + 1
        })
        
        const modelCounts: Record<string, number> = {}
        models.forEach(m => {
          modelCounts[m.type] = (modelCounts[m.type] || 0) + 1
        })
        
        useGlbDiagStore.getState().pushDiag('SCENE_ENTITIES_SNAPSHOT', {
          sceneId: currentScene.sceneId,
          details: {
            totalEntities: currentScene.entities.length,
            entityTypeCounts: entityCounts,
            modelTypeCounts: modelCounts
          }
        })
      }

      // Collect types from static list and current scene (if available)
      const typesToPreload = new Set<string>([
        'OHT_VEHICLE', 'AGV_VEHICLE', 'MACHINE', 'STOCKER', 'CONVEYOR', 'SAFETY_ZONE'
      ])
      
      if (currentScene) {
        currentScene.entities.forEach(e => typesToPreload.add(e.type))
      }

      const modelIdsToFetch = new Set<string>()

      // Task C: Preload multiple candidates (N=3) per type
      typesToPreload.forEach(type => {
        // Strategy: ACTIVE, matches type
        const candidates = models.filter(m => m.type === type && (m.status === 'ACTIVE' || (m.status as 'ACTIVE' | 'DISABLED' | 'PENDING' | 'ENABLED') === 'ENABLED'))
        if (candidates.length === 0) return

        // Sort: Default version > UpdatedAt
        const sorted = [...candidates].sort((a, b) => {
          if (a.defaultVersion && !b.defaultVersion) return -1
          if (!a.defaultVersion && b.defaultVersion) return 1
          return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
        })
        
        // Take top 3 candidates to ensure at least one valid model is found
        const topCandidates = sorted.slice(0, 3)
        
        topCandidates.forEach(model => {
          modelIdsToFetch.add(model.modelId)
        })
      })

      // Task B: Preload summary
      useGlbDiagStore.getState().pushDiag('PRELOAD_MODEL_VERSIONS_START', {
        details: { modelIds: Array.from(modelIdsToFetch) }
      })

      // Execute fetches with deduplication
      modelIdsToFetch.forEach(modelId => {
        if (!preloadedModelIdsRef.current.has(modelId)) {
          preloadedModelIdsRef.current.add(modelId)
          fetchModelVersions(modelId)
            .then(versions => {
              // Task B: Fetch success summary
              useGlbDiagStore.getState().pushDiag('PRELOAD_MODEL_VERSIONS_DONE', {
                modelId,
                details: {
                  total: versions.length,
                  withFileUrl: versions.filter(v => !!v.fileUrl).length
                }
              })
            })
            .catch(err => {
              // Task B: Fetch failed warning
              useGlbDiagStore.getState().pushDiag('PRELOAD_MODEL_VERSIONS_FAILED', {
                modelId,
                details: { error: err.message || String(err) }
              })
            })
        }
      })
    }
  }, [models, currentScene, fetchModelVersions])

  // Keyboard shortcuts: Undo/Redo/Delete/Esc
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0
      const cmdKey = isMac ? event.metaKey : event.ctrlKey

      // Undo: Ctrl/Cmd + Z
      if (cmdKey && !event.shiftKey && event.key === 'z') {
        event.preventDefault()
        if (canUndo()) {
          undo()
          addToast('info', t('sceneEditor.toast.undone'))
        }
        return
      }

      // Redo: Ctrl/Cmd + Shift + Z (or Ctrl/Cmd + Y)
      if (cmdKey && (event.shiftKey && event.key === 'z') || (cmdKey && event.key === 'y')) {
        event.preventDefault()
        if (canRedo()) {
          redo()
          addToast('info', t('sceneEditor.toast.redone'))
        }
        return
      }

      // Delete selected entities/segments
      if (event.key === 'Delete' || event.key === 'Backspace') {
        // Only handle if not in an input field
        if (document.activeElement?.tagName === 'INPUT' || document.activeElement?.tagName === 'TEXTAREA') {
          return
        }
        event.preventDefault()
        if (editorUi.selectedEntityIds.length > 0) {
          pushHistory()
          batchDeleteEntities(editorUi.selectedEntityIds)
          setEditorSelectedEntityIds([])
          addToast('info', t('sceneEditor.toast.entitiesDeleted'))
        } else if (editorUi.selectedSegmentIds.length > 0) {
          // Delete selected segment
          const segId = editorUi.selectedSegmentIds[0]
          if (currentScene) {
            pushHistory()
            const updatedPaths = currentScene.paths.map(p => {
              if (p.segments?.some(s => s.id === segId)) {
                return { ...p, segments: p.segments?.filter(s => s.id !== segId) || [] }
              }
              return p
            })
            patchCurrentScene({ paths: updatedPaths })
            setEditorSelectedSegmentIds([])
            markUnsavedChanges()
            addToast('info', t('sceneEditor.toast.segmentDeleted'))
          }
        }
        return
      }

      // ESC to cancel connection or clear selection
      if (event.key === 'Escape') {
        if (editorUi.pendingConnectNodeId) {
          setEditorPendingConnectNodeId(null)
          addToast('info', t('sceneEditor.toast.connectCanceled'))
        } else if (editorUi.selectedEntityIds.length > 0) {
          setEditorSelectedEntityIds([])
        } else if (editorUi.selectedSegmentIds.length > 0) {
          setEditorSelectedSegmentIds([])
        }
        return
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
    }
  }, [
    editorUi.pendingConnectNodeId,
    editorUi.selectedEntityIds,
    editorUi.selectedSegmentIds,
    setEditorPendingConnectNodeId,
    setEditorSelectedEntityIds,
    setEditorSelectedSegmentIds,
    addToast,
    canUndo,
    canRedo,
    undo,
    redo,
    pushHistory,
    batchDeleteEntities,
    currentScene,
    patchCurrentScene,
    markUnsavedChanges,
  ])

  // Fetch bindings and models
  useEffect(() => {
    if (currentScene?.sceneId) {
      fetchBindings(currentScene.sceneId)
    }
  }, [currentScene?.sceneId, fetchBindings])

  useEffect(() => {
    if (currentScene?.sceneId) {
      const sceneBindings = bindings.get(currentScene.sceneId)
      if (sceneBindings) {
        const modelIds = new Set(sceneBindings.map(b => b.modelId))
        modelIds.forEach(modelId => {
           fetchModelVersions(modelId).catch(() => {})
        })
      }
    }
  }, [bindings, currentScene?.sceneId, fetchModelVersions])

  useEffect(() => {
    if (!hasUnsavedChanges || !currentScene) {
      return
    }

    const timerId = setTimeout(() => {
      saveDraft(0)
        .then(() => {
          setLastAutoSave(new Date())
        })
        .catch(() => {
          // ignore auto-save transient failure
        })
    }, 60000)

    return () => {
      clearTimeout(timerId)
    }
  }, [hasUnsavedChanges, currentScene, saveDraft])

  const handleSelectEntity = useCallback((entityId: string | null) => {
    setEditorSelectedEntityIds(entityId ? [entityId] : [])
  }, [setEditorSelectedEntityIds])

  const handleSelectEntities = useCallback((entityIds: string[]) => {
    setEditorSelectedEntityIds(entityIds)
  }, [setEditorSelectedEntityIds])

  const handleAddToSelection = useCallback((entityIds: string[]) => {
    addToEditorSelectedEntityIds(entityIds)
  }, [addToEditorSelectedEntityIds])

  const handleSelectSegment = useCallback((segmentId: string | null) => {
    setEditorSelectedSegmentIds(segmentId ? [segmentId] : [])
  }, [setEditorSelectedSegmentIds])

  const handleDeleteSegment = useCallback((pathId: string, segmentId: string) => {
    if (!currentScene) return
    pushHistory()
    const updatedPaths = currentScene.paths.map(p => {
      if (p.id === pathId) {
        return {
          ...p,
          segments: p.segments?.filter(s => s.id !== segmentId) || []
        }
      }
      return p
    })
    patchCurrentScene({ paths: updatedPaths })
    setEditorSelectedSegmentIds([])
    markUnsavedChanges()
  }, [currentScene, markUnsavedChanges, patchCurrentScene, setEditorSelectedSegmentIds, pushHistory])

  const handleUpdateSegment = useCallback((pathId: string, segment: PathSegment) => {
    if (!currentScene) return
    const updatedPaths = currentScene.paths.map(p => {
      if (p.id === pathId) {
        return {
          ...p,
          segments: p.segments?.map(s => s.id === segment.id ? segment : s) || []
        }
      }
      return p
    })
    patchCurrentScene({ paths: updatedPaths })
    markUnsavedChanges()
  }, [currentScene, markUnsavedChanges, patchCurrentScene])

  const handleDeleteEntity = useCallback((id: string) => {
    if (!currentScene) return
    pushHistory()

    // Check if this is a CONTROL_POINT to show specific message
    const entity = currentScene.entities.find(e => e.id === id)
    const isControlPoint = entity?.type === 'CONTROL_POINT'

    // 1. Remove the entity
    const nextEntities = currentScene.entities.filter(e => e.id !== id)

    // 2. Remove associated segments (Task C: cascade delete)
    const nodeId = `node-${id}`
    let removedSegmentCount = 0
    const nextPaths = currentScene.paths.map(path => {
      const originalSegmentCount = path.segments?.length || 0
      const segments = path.segments?.filter(s => s.from !== nodeId && s.to !== nodeId) || []
      removedSegmentCount += originalSegmentCount - segments.length
      const points = path.points.filter(p => p.id !== nodeId)
      return { ...path, segments, points }
    })

    patchCurrentScene({ entities: nextEntities, paths: nextPaths })
    setEditorSelectedEntityIds([])
    markUnsavedChanges()

    // Show toast notification for control point deletion
    if (isControlPoint && removedSegmentCount > 0) {
      addToast('info', t('sceneEditor.toast.nodeDeleted'))
    }
  }, [currentScene, markUnsavedChanges, patchCurrentScene, setEditorSelectedEntityIds, addToast, pushHistory])

  const handlePlaceEntity = useCallback((type: EntityType, position: { x: number; y: number; z: number }) => {
    if (!currentScene) return
    pushHistory()

    let finalPosition = { ...position }

    // Task C: Vehicle Node Snapping
    if (type === 'OHT_VEHICLE' || type === 'AGV_VEHICLE') {
      let bestExplicit: { entity: Entity; dist: number } | null = null
      let bestLegacy: { entity: Entity; dist: number } | null = null
      let foundMismatch = false
      let foundYInvalid = false
      const SNAP_THRESHOLD = 2.0 // meters
      const EPSILON = 1e-6

      for (const entity of currentScene.entities) {
        if (entity.type === 'CONTROL_POINT') {
          const dx = entity.position.x - position.x
          const dz = entity.position.z - position.z // 2D distance
          const dist = Math.sqrt(dx * dx + dz * dz)
          
          if (dist <= SNAP_THRESHOLD) {
            // Task: AGV Y=0 Check
            if (type === 'AGV_VEHICLE' && Math.abs(entity.position.y) > EPSILON) {
              foundYInvalid = true
              continue
            }

            // Task 2: Network Type Filtering
            const nodeNetworkType = entity.properties?.networkType as string | undefined
            const expectedNetwork = type === 'OHT_VEHICLE' ? 'OHT_PATH' : 'AGV_NETWORK'
            
            if (nodeNetworkType === expectedNetwork) {
              if (!bestExplicit || dist < bestExplicit.dist) {
                bestExplicit = { entity, dist }
              }
            } else if (!nodeNetworkType) {
              if (!bestLegacy || dist < bestLegacy.dist) {
                bestLegacy = { entity, dist }
              }
            } else {
              foundMismatch = true
            }
          }
        }
      }

      const nearestNode = bestExplicit?.entity || bestLegacy?.entity

      if (nearestNode) {
        finalPosition = { ...nearestNode.position }
      } else {
        if (foundYInvalid) {
          addToast('error', t('sceneEditor.toast.agvControlPointYInvalid'))
        } else if (foundMismatch) {
          const expected = type === 'OHT_VEHICLE' ? 'OHT' : 'AGV'
          addToast('error', tf('sceneEditor.toast.snapFailedByNetworkType', { expected }))
        } else {
          addToast('error', t('sceneEditor.toast.snapFailed'))
        }
        return
      }
    }

    // Task D: Y-Axis Semantics
    if (type === 'AGV_VEHICLE') {
      finalPosition.y = 0
    }

    const nextId = `${type}-${Date.now().toString(36)}`
    const nextEntity: Entity = {
      id: nextId,
      type,
      name: type,
      position: finalPosition,
      rotation: { x: 0, y: 0, z: 0 },
      ...(type === 'SAFETY_ZONE'
        ? {
          properties: {
            shape: 'RECTANGLE',
            width: 6,
            height: 4,
            priority: 'HUMAN_FIRST',
          },
        }
        : {}),
    }

    patchCurrentScene({ entities: [...currentScene.entities, nextEntity] })
    setEditorSelectedEntityIds([nextId])
    markUnsavedChanges()
  }, [currentScene, markUnsavedChanges, patchCurrentScene, setEditorSelectedEntityIds, addToast, pushHistory])

  const handleConnectNode = useCallback((nodeId: string, segmentType: 'LINEAR' | 'BEZIER') => {
    if (!currentScene) return

    // Task B: Path State Machine
    const targetNode = currentScene.entities.find(e => e.id === nodeId)
    if (targetNode?.type !== 'CONTROL_POINT') {
      addToast('error', t('sceneEditor.toast.invalidConnection'))
      return
    }

    const firstNodeId = editorUi.pendingConnectNodeId
    if (!firstNodeId) {
      setEditorPendingConnectNodeId(nodeId)
      addToast('info', t('sceneEditor.toast.connectStart'))
      return
    }

    if (firstNodeId === nodeId) {
      setEditorPendingConnectNodeId(null)
      return
    }

    const firstNode = currentScene.entities.find(e => e.id === firstNodeId)
    if (firstNode?.type !== 'CONTROL_POINT') {
      setEditorPendingConnectNodeId(null)
      return
    }

    const nextPaths = buildConnectedPaths(
      currentScene.paths,
      currentScene.entities,
      firstNodeId,
      nodeId,
      segmentType,
      editorUi.activePathType
    )
    pushHistory()
    patchCurrentScene({ paths: nextPaths })
    setEditorPendingConnectNodeId(null)
    markUnsavedChanges()
    addToast('success', t('sceneEditor.toast.connectDone'))
  }, [addToast, currentScene, editorUi.pendingConnectNodeId, editorUi.activePathType, markUnsavedChanges, patchCurrentScene, setEditorPendingConnectNodeId, pushHistory])

  const handleUpdateEntityTransform = useCallback((updated: Entity) => {
    if (!currentScene) return
    pushHistory()

    // Task C: Enforce AGV y=0 rule
    let finalPosition = updated.position
    if (updated.type === 'AGV_VEHICLE' || (updated.type === 'CONTROL_POINT' &&
        currentScene.paths.some(p => p.type === 'AGV_NETWORK' && p.points.some(pt => pt.id === `node-${updated.id}`)))) {
      finalPosition = { ...updated.position, y: 0 }
    }

    const updatedEntity = { ...updated, position: finalPosition }
    const nextEntities = currentScene.entities.map((entity) => (
      entity.id === updated.id ? updatedEntity : entity
    ))

    // Task C: Update path points when CONTROL_POINT is moved
    let nextPaths = currentScene.paths
    if (updated.type === 'CONTROL_POINT') {
      const nodeId = `node-${updated.id}`
      nextPaths = currentScene.paths.map(path => {
        const pointIndex = path.points.findIndex(p => p.id === nodeId)
        if (pointIndex >= 0) {
          const newPoints = [...path.points]
          // For AGV_NETWORK, enforce y=0
          const newY = path.type === 'AGV_NETWORK' ? 0 : finalPosition.y
          newPoints[pointIndex] = {
            ...newPoints[pointIndex],
            position: { x: finalPosition.x, y: newY, z: finalPosition.z }
          }
          return { ...path, points: newPoints }
        }
        return path
      })
    }

    patchCurrentScene({ entities: nextEntities, paths: nextPaths })
    markUnsavedChanges()
  }, [currentScene, markUnsavedChanges, patchCurrentScene, pushHistory])

  const focusValidationTarget = useCallback((target: SemanticValidationResult['target']) => {
    if (!target || !currentScene) return

    if (target.type === 'entity') {
      setEditorSelectedEntityIds([target.id])
      setEditorSelectedSegmentIds([])
    } else if (target.type === 'segment') {
      setEditorSelectedSegmentIds([target.id])
      setEditorSelectedEntityIds([])
    } else if (target.type === 'point' && target.pathId) {
      // Try to find the entity associated with this point
      const nodeId = target.id.replace('node-', '')
      const entity = currentScene.entities.find(e => e.id === nodeId)
      if (entity) {
        setEditorSelectedEntityIds([entity.id])
        setEditorSelectedSegmentIds([])
      }
    }
    
    addToast('info', t('sceneEditor.validation.focusHint'))
  }, [currentScene, setEditorSelectedEntityIds, setEditorSelectedSegmentIds, addToast])

  const handleSave = useCallback(async () => {
    if (!id || !currentScene) return

    setIsSaving(true)
    try {
      // Task D: Unit validation - check rotation values are within [-π, π]
      const PI = Math.PI
      for (const entity of currentScene.entities) {
        if (entity.rotation) {
          const { x, y, z } = entity.rotation
          if (x < -PI || x > PI || y < -PI || y > PI || z < -PI || z > PI) {
            addToast('error', tf('sceneEditor.validation.unitOutOfRange', { name: entity.name || entity.id }))
            setEditorSelectedEntityIds([entity.id])
            setIsSaving(false)
            return
          }
        }
      }

      // 1. Transport Type Compatibility
      const steps = currentScene?.processSteps || []
      const entities = currentScene?.entities || []
      const validationResult = validateTransportTypeCompatibility(steps, entities)

      if (!validationResult.isCompatible) {
        addToast('error', validationResult.errorMessage || t('sceneEditor.toast.transportValidationFailed'))
        return
      }

      // 2. Topology Validation
      const topologyResult = validatePathTopology(currentScene)
      if (!topologyResult.isValid) {
        addToast('error', topologyResult.error || t('sceneEditor.validation.topologyFailed'))
        return
      }

      // 3. Network Semantic Validation
      const semanticResult = validateNetworkSemantic(currentScene)
      if (!semanticResult.isValid) {
        addToast('error', semanticResult.error || t('sceneEditor.validation.networkSemanticFailed'))
        if (semanticResult.target) {
          focusValidationTarget(semanticResult.target)
        }
        return
      }

      await updateScene(id, {
        ...currentScene,
      })
      addToast('success', t('sceneEditor.toast.saveSuccess'))
      setLastAutoSave(new Date())
    } catch {
      addToast('error', t('sceneEditor.toast.saveFailed'))
    } finally {
      setIsSaving(false)
    }
  }, [id, currentScene, updateScene, addToast, focusValidationTarget, setEditorSelectedEntityIds])

  const handleRecoverDraft = useCallback(() => {
    recoverDraft()
    addToast('info', t('sceneEditor.toast.draftRecovered'))
  }, [recoverDraft, addToast])

  const handleCancel = useCallback(() => {
    if (hasUnsavedChanges && !confirm(t('sceneEditor.confirm.leave'))) {
      return
    }
    navigate('/scenes')
  }, [hasUnsavedChanges, navigate])

  if (isLoading) {
    return <LoadingState message={t('sceneEditor.loading')} />
  }

  if (error && !currentScene) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-industrial-dark via-industrial-slate to-industrial-blue">
        <div className="max-w-7xl mx-auto px-6 py-8">
          <ErrorState
            title={t('sceneEditor.loadFailedTitle')}
            message={error}
            onRetry={() => {
              clearError()
              if (id) {
                void useSceneStore.getState().fetchScene(id)
              }
            }}
          />
          <div className="mt-4">
            <Link
              to="/scenes"
              className="text-amber-500 hover:text-amber-400 transition-colors"
            >
              ← {t('sceneEditor.backToScenes')}
            </Link>
          </div>
        </div>
      </div>
    )
  }

  const draftRecoveryDialogElement = showDraftRecoveryDialog && draft ? (
    <DraftRecoveryDialog
      draftSavedAt={draft.savedAt}
      onRecover={handleRecoverDraft}
      onDismiss={dismissDraftRecovery}
    />
  ) : null

  return (
    <div className="h-screen flex flex-col overflow-hidden bg-gradient-to-br from-industrial-dark via-industrial-slate to-industrial-blue">
      {draftRecoveryDialogElement}
      <GlbDiagPanel />

      <header className="border-b border-industrial-steel/30 bg-industrial-dark/50 backdrop-blur sticky top-0 z-40">
        <div className="max-w-[1700px] mx-auto px-6 py-2">
          <div className="flex items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <Link
                to="/scenes"
                className="text-gray-400 hover:text-white transition-colors"
              >
                ←
              </Link>
              <div>
                <h1 className="text-lg font-display font-bold text-white">{t('sceneEditor.title')}</h1>
                <p className="text-gray-500 text-xs">{currentScene?.sceneId}</p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              {/* Save Status Indicator */}
              <div className="flex items-center gap-2">
                {saveStatus === 'saving' && (
                  <span className="flex items-center gap-1 text-blue-400 text-xs">
                    <svg className="animate-spin h-3 w-3" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    {t('sceneEditor.status.saving')}
                  </span>
                )}
                {saveStatus === 'saved' && (
                  <span className="flex items-center gap-1 text-emerald-400 text-xs">
                    <svg className="h-3 w-3" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                    {t('sceneEditor.status.saved')}
                  </span>
                )}
                {saveStatus === 'dirty' && (
                  <span className="flex items-center gap-1 text-amber-500 text-xs">
                    <svg className="h-3 w-3" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                    </svg>
                    {t('sceneEditor.status.unsaved')}
                  </span>
                )}
              </div>

              {/* Undo/Redo Buttons */}
              <div className="flex items-center gap-1">
                <button
                  onClick={() => {
                    if (canUndo()) {
                      undo()
                      addToast('info', t('sceneEditor.toast.undone'))
                    }
                  }}
                  disabled={!canUndo()}
                  className="p-1.5 text-gray-400 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  title={t('sceneEditor.toolbar.undo')}
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6" />
                  </svg>
                </button>
                <button
                  onClick={() => {
                    if (canRedo()) {
                      redo()
                      addToast('info', t('sceneEditor.toast.redone'))
                    }
                  }}
                  disabled={!canRedo()}
                  className="p-1.5 text-gray-400 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  title={t('sceneEditor.toolbar.redo')}
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 10h-10a8 8 0 00-8 8v2M21 10l-6 6m6-6l-6-6" />
                  </svg>
                </button>
              </div>

              {lastAutoSave ? (
                <span className="text-gray-500 text-xs">
                  {tf('sceneEditor.autoSavedAt', { time: lastAutoSave.toLocaleTimeString() })}
                </span>
              ) : null}

              <button
                onClick={handleCancel}
                className="px-3 py-1 text-sm text-gray-400 hover:text-white transition-colors"
              >
                {t('sceneEditor.cancel')}
              </button>

              <button
                onClick={handleSave}
                disabled={isSaving || !hasUnsavedChanges}
                className="px-3 py-1 text-sm bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors font-bold disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSaving ? t('sceneEditor.saving') : t('sceneEditor.save')}
              </button>
            </div>
          </div>
        </div>
      </header>

      {error ? (
        <div className="max-w-[1700px] mx-auto px-6 mt-6">
          <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 flex items-center justify-between">
            <span className="text-red-400 text-sm">{error}</span>
            <button
              onClick={clearError}
              className="text-gray-400 hover:text-white transition-colors"
            >
              ✕
            </button>
          </div>
        </div>
      ) : null}

      <main className="flex-1 flex overflow-hidden relative">
        <div className="w-64 flex flex-col border-r border-industrial-steel/30 bg-industrial-slate">
           <PalettePanel 
            selectedType={editorUi.placingType as EntityType} 
            onSelectType={(type) => {
              setEditorMode('place')
              setEditorPlacingType(type)
            }} 
          />
        </div>

        <div className="flex-1 relative bg-[#1a1a2e]">
          <EditorToolbar
            mode={editorUi.mode}
            onModeChange={setEditorMode}
            transformMode={editorUi.transformMode}
            onTransformModeChange={setEditorTransformMode}
            snapEnabled={editorUi.snapEnabled}
            onSnapEnabledChange={setEditorSnapEnabled}
            showGrid={editorUi.showGrid}
            onShowGridChange={setEditorShowGrid}
            pathSegmentType={editorUi.pathSegmentType}
            onPathSegmentTypeChange={setEditorPathSegmentType}
            activePathType={editorUi.activePathType}
            onActivePathTypeChange={setEditorActivePathType}
          />
          <EditorCanvas
            entities={currentScene?.entities || []}
            paths={currentScene?.paths || []}
            mode={editorUi.mode}
            placingType={editorUi.placingType}
            snapEnabled={editorUi.snapEnabled}
            showGrid={editorUi.showGrid}
            selectedEntityIds={editorUi.selectedEntityIds}
            selectedSegmentIds={editorUi.selectedSegmentIds}
            pendingConnectNodeId={editorUi.pendingConnectNodeId}
            pathSegmentType={editorUi.pathSegmentType}
            transformMode={editorUi.transformMode}
            onSelectEntity={handleSelectEntity}
            onSelectEntities={handleSelectEntities}
            onAddToSelection={handleAddToSelection}
            onSelectSegment={handleSelectSegment}
            onPlaceEntity={handlePlaceEntity}
            onConnectNode={handleConnectNode}
            onUpdateEntityTransform={handleUpdateEntityTransform}
            onUpdateSegment={handleUpdateSegment}
          />
        </div>

        <div className="w-80 flex flex-col border-l border-industrial-steel/30 bg-industrial-slate overflow-y-auto">
          <PropertiesPanel
            selectedEntity={currentScene?.entities.find(e => editorUi.selectedEntityIds.includes(e.id)) || null}
            selectedSegment={(() => {
              const segId = editorUi.selectedSegmentIds[0]
              if (!segId || !currentScene) return null
              for (const p of currentScene.paths) {
                const s = p.segments?.find(seg => seg.id === segId)
                if (s) return { pathId: p.id, segment: s }
              }
              return null
            })()}
            onUpdateEntity={(entity) => {
              if (!currentScene) return
              const entities = currentScene.entities.map(e => e.id === entity.id ? entity : e)
              patchCurrentScene({ entities })
              markUnsavedChanges()
            }}
            onDeleteEntity={handleDeleteEntity}
            onDeleteSegment={(pathId, segmentId) => handleDeleteSegment(pathId, segmentId)}
          />
        </div>
      </main>
    </div>
  )
}

export default SceneEditorPage
