import { useRef } from 'react'
import { useModelStore } from '../store/modelStore'
import { useSceneStore } from '../store/sceneStore'
import { useGlbDiagStore } from '../store/glbDiagStore'
import type { Entity, ModelMetadata } from '../types/api'
import { resolveModelAssetUrl } from '@/utils/modelAssetUrl'

/**
 * Resolved model information for an entity, including URL and metadata.
 */
export type ResolvedEntityModel = {
  url: string
  modelId: string
  versionId: string
  metadata?: ModelMetadata
}

/**
 * Hook to retrieve the GLB model information for a given entity based on scene bindings.
 * Returns structured result with URL, modelId, versionId, and metadata.
 *
 * @param entity The entity to find the model for
 * @returns Resolved model info, or null if not found
 */
export function useEntityModel(entity: Entity): ResolvedEntityModel | null {
  const { currentScene } = useSceneStore()
  const { bindings, modelVersionsByModelId, models } = useModelStore()
  const loggedRef = useRef<Set<string>>(new Set())
  
  const logDecision = (result: ResolvedEntityModel | null, reason: string, details?: Record<string, unknown>) => {
    // Unique key per scene+entity to avoid spamming logs every frame
    const key = `${currentScene?.sceneId}:${entity.id}`
    if (!loggedRef.current.has(key)) {
      loggedRef.current.add(key)
      if (result) {
        useGlbDiagStore.getState().pushDiag('ENTITY_RESOLVED', {
          sceneId: currentScene?.sceneId,
          entityId: entity.id,
          url: result.url,
          details: { type: entity.type, reason, ...details }
        })
      } else {
        useGlbDiagStore.getState().pushDiag('ENTITY_RESOLVE_FAILED', {
          sceneId: currentScene?.sceneId,
          entityId: entity.id,
          details: { type: entity.type, reason, ...details }
        })
      }
    }
  }

  if (!currentScene) return null

  // Task B: Alias Mapping
  const getCandidateTypes = (type: string): string[] => {
    const aliases: Record<string, string[]> = {
      'OHT': ['OHT_VEHICLE'],
      'AGV': ['AGV_VEHICLE'],
      'STATION': ['MANUAL_STATION'],
      'RACK': ['ERACK'],
      // Add reverse mapping if needed or specific fallback logic
      'OHT_VEHICLE': ['OHT'],
      'AGV_VEHICLE': ['AGV'],
      'MANUAL_STATION': ['STATION'],
      'ERACK': ['RACK']
    }
    return [type, ...(aliases[type] || [])]
  }

  // 1. Try Entity Binding (Specific Override)
  const sceneBindings = bindings.get(currentScene.sceneId)
  if (sceneBindings) {
    const binding = sceneBindings.find(b => b.entityId === entity.id)
    if (binding) {
      const versions = modelVersionsByModelId[binding.modelId]
      const version = versions?.find(v => v.versionId === binding.versionId)
      if (version?.fileUrl) {
        const finalUrl = resolveModelAssetUrl(version.fileUrl)
        if (!finalUrl) {
          logDecision(null, 'BINDING_VERSION_URL_RESOLVE_FAILED', {
            modelId: binding.modelId,
            versionId: binding.versionId,
            rawUrl: version.fileUrl
          })
          return null
        }
        const result: ResolvedEntityModel = {
          url: finalUrl,
          modelId: binding.modelId,
          versionId: binding.versionId,
          metadata: version.metadata
        }
        logDecision(result, 'BINDING_MATCH', { modelId: binding.modelId, versionId: binding.versionId })
        return result
      }
      // Binding exists but version not found or no URL
      if (!versions) {
        logDecision(null, 'BINDING_VERSIONS_NOT_LOADED', { modelId: binding.modelId })
      } else if (!version) {
        logDecision(null, 'BINDING_VERSION_NOT_FOUND', { modelId: binding.modelId, versionId: binding.versionId })
      } else {
        logDecision(null, 'BINDING_VERSION_NO_FILE_URL', { modelId: binding.modelId, versionId: binding.versionId })
      }
    }
  }

  // 2. Try Default Model for Entity Type (Fallback)
  // Task C: Unified selection logic matching SceneEditorPage preloading strategy
  // Note: ModelSummary uses 'type' directly, not nested in metadata
  
  // Strategy: ACTIVE, matches type (including aliases)
  const candidateTypes = getCandidateTypes(entity.type)
  const candidates = models.filter(m =>
    candidateTypes.includes(m.type) &&
    (m.status === 'ACTIVE' || (m.status as 'ACTIVE' | 'DISABLED' | 'PENDING' | 'ENABLED') === 'ENABLED')
  )
  
  if (candidates.length > 0) {
    // Sort: Default version > UpdatedAt
    const sorted = [...candidates].sort((a, b) => {
      if (a.defaultVersion && !b.defaultVersion) return -1
      if (!a.defaultVersion && b.defaultVersion) return 1
      return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    })
    
    // Task D: Iterate candidates to find first usable one
    for (const model of sorted) {
      const versions = modelVersionsByModelId[model.modelId]
      if (versions) {
        let selectedVersion = null
        let strategy = ''

        // 1. Try default version
        if (model.defaultVersion) {
           const v = versions.find(v => v.versionId === model.defaultVersion || v.version === model.defaultVersion)
           if (v?.fileUrl) {
             selectedVersion = v
             strategy = 'DEFAULT_VERSION'
           }
        }
        
        // 2. Try any active version with fileUrl
        if (!selectedVersion) {
          const activeVer = versions.find(v => (v.status === 'ACTIVE' || (v.status as 'ACTIVE' | 'DISABLED' | 'PENDING' | 'ENABLED') === 'ENABLED') && v.fileUrl)
          if (activeVer) {
            selectedVersion = activeVer
            strategy = 'ACTIVE_VERSION'
          }
        }
        
        // 3. Try any version with fileUrl
        if (!selectedVersion) {
          const anyVer = versions.find(v => v.fileUrl)
          if (anyVer) {
            selectedVersion = anyVer
            strategy = 'ANY_VERSION'
          }
        }

        if (selectedVersion) {
          const finalUrl = resolveModelAssetUrl(selectedVersion.fileUrl)
          if (!finalUrl) {
            logDecision(null, 'DEFAULT_MODEL_URL_RESOLVE_FAILED', {
              modelId: model.modelId,
              versionId: selectedVersion.versionId,
              strategy
            })
            continue
          }
          const result: ResolvedEntityModel = {
            url: finalUrl,
            modelId: model.modelId,
            versionId: selectedVersion.versionId,
            metadata: selectedVersion.metadata
          }
          logDecision(result, `DEFAULT_MODEL_${strategy}`, {
            modelId: model.modelId,
            versionId: selectedVersion.versionId,
            matchedModelType: model.type,
            entityType: entity.type
          })
          return result
        }
      } else {
        // Versions not loaded yet for this candidate, try next? 
        // Or maybe just log and continue to see if any other model is ready.
        // Actually, if versions aren't loaded, we can't use this model.
      }
    }
    
    // If we get here, we found candidates but none had usable versions loaded
    // Check if it's because versions aren't loaded yet or because they lack URLs
    const topCandidate = sorted[0]
    if (!modelVersionsByModelId[topCandidate.modelId]) {
      logDecision(null, 'VERSIONS_NOT_LOADED', { modelId: topCandidate.modelId })
    } else {
      logDecision(null, 'VERSION_NO_FILE_URL', { modelId: topCandidate.modelId })
    }
  } else {
    logDecision(null, 'TYPE_NO_MATCH_MODEL', { 
      entityType: entity.type, 
      candidateTypes,
      availableModelTypes: Array.from(new Set(models.map(m => m.type)))
    })
  }

  return null
}
