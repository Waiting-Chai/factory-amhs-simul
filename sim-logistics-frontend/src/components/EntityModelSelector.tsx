/**
 * Entity-level model binding selector component.
 *
 * Allows binding a specific model version to an entity in a scene.
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-11
 */
import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react'
import type { EntityModelBinding, ModelVersion, SceneSummary } from '../types'
import { useModelStore } from '@store/modelStore'
import { useSceneStore } from '@store/sceneStore'
import { scenesApi } from '@api/scenes'
import type { SceneDetail } from '../types'
import { t, tf } from '../shared/i18n'

interface EntityModelSelectorProps {
  sceneId?: string
  entityId?: string
  onBindingChange?: (entityId: string, modelId: string, versionId: string) => void
}

/**
 * Entity model selector component
 */
export const EntityModelSelector: React.FC<EntityModelSelectorProps> = ({
  sceneId: initialSceneId,
  entityId: initialEntityId,
  onBindingChange,
}) => {
  const {
    models,
    bindings: bindingsByScene,
    modelVersionsByModelId,
    fetchBindings,
    fetchModelVersions,
    setBinding,
  } = useModelStore()
  const { scenes, fetchScenes } = useSceneStore()

  const [sceneId, setSceneId] = useState<string | null>(initialSceneId || null)
  const [entityId, setEntityId] = useState<string | null>(initialEntityId || null)
  const [selectedModelId, setSelectedModelId] = useState<string | null>(null)
  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(null)
  const [sceneDetail, setSceneDetail] = useState<SceneDetail | null>(null)
  const [sceneBindings, setSceneBindings] = useState<Record<string, EntityModelBinding>>({})
  const [versionOptions, setVersionOptions] = useState<ModelVersion[]>([])
  const [isLoadingScene, setIsLoadingScene] = useState(false)
  const [isLoadingVersions, setIsLoadingVersions] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const lastFetchedSceneIdRef = useRef<string | null>(null)

  useEffect(() => {
    fetchScenes(1).catch((err: unknown) => {
      console.error(t('validation.fetchScenesFailed'), err)
    })
  }, [fetchScenes])

  useEffect(() => {
    if (!sceneId) {
      setSceneDetail(null)
      return
    }

    setIsLoadingScene(true)
    setError(null)

    scenesApi.getById(sceneId)
      .then((scene) => {
        setSceneDetail(scene)
      })
      .catch((err: unknown) => {
        const message = err instanceof Error ? err.message : t('validation.loadSceneFailed')
        setError(message)
      })
      .finally(() => {
        setIsLoadingScene(false)
      })
  }, [sceneId])

  // Effect A: request bindings for selected scene only.
  useEffect(() => {
    if (!sceneId) {
      lastFetchedSceneIdRef.current = null
      setSceneBindings({})
      return
    }

    if (lastFetchedSceneIdRef.current === sceneId) {
      return
    }

    lastFetchedSceneIdRef.current = sceneId
    fetchBindings(sceneId).catch((err) => {
      console.error(t('validation.fetchBindingsFailed'), err)
    })
  }, [sceneId, fetchBindings])

  // Effect B: consume latest bindings from store and map to local state.
  useEffect(() => {
    if (!sceneId) {
      setSceneBindings({})
      return
    }

    const bindings = bindingsByScene.get(sceneId) || []
    const bindingMap = bindings.reduce<Record<string, EntityModelBinding>>((acc, binding) => {
      acc[binding.entityId] = binding
      return acc
    }, {})
    setSceneBindings(bindingMap)
  }, [sceneId, bindingsByScene])

  const handleSceneChange = useCallback((e: React.ChangeEvent<HTMLSelectElement>) => {
    const newSceneId = e.target.value || null
    if (newSceneId !== sceneId) {
      lastFetchedSceneIdRef.current = null
    }
    setSceneId(newSceneId)
    setEntityId(null)
    setSelectedModelId(null)
    setSelectedVersionId(null)
    setVersionOptions([])
  }, [sceneId])

  const handleEntityChange = useCallback((e: React.ChangeEvent<HTMLSelectElement>) => {
    const newEntityId = e.target.value || null
    setEntityId(newEntityId)

    if (newEntityId && sceneBindings[newEntityId]) {
      setSelectedModelId(sceneBindings[newEntityId].modelId)
      setSelectedVersionId(sceneBindings[newEntityId].versionId)
      return
    }

    setSelectedModelId(null)
    setSelectedVersionId(null)
    setVersionOptions([])
  }, [sceneBindings])

  const handleModelChange = useCallback(async (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newModelId = e.target.value || null
    setSelectedModelId(newModelId)
    setSelectedVersionId(null)
    setVersionOptions([])

    if (!newModelId) {
      return
    }

    try {
      setIsLoadingVersions(true)
      const cached = modelVersionsByModelId[newModelId]
      const versions = cached ?? await fetchModelVersions(newModelId)
      const safeVersions = Array.isArray(versions) ? versions : []
      setVersionOptions(safeVersions)

      const defaultVersion = safeVersions.find((v) => v.isDefault) ?? safeVersions[0]
      if (defaultVersion?.versionId) {
        setSelectedVersionId(defaultVersion.versionId)
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : t('validation.loadVersionsFailed')
      setError(message)
    } finally {
      setIsLoadingVersions(false)
    }
  }, [fetchModelVersions, modelVersionsByModelId])

  const handleSaveBinding = useCallback(async () => {
    if (!sceneId || !entityId || !selectedModelId || !selectedVersionId) {
      return
    }

    setIsSaving(true)
    setError(null)

    try {
      await setBinding(sceneId, entityId, selectedModelId, selectedVersionId)
      // Let Effect A fetch latest bindings once after a successful save.
      lastFetchedSceneIdRef.current = null
      await fetchBindings(sceneId)

      setSceneBindings((prev) => ({
        ...prev,
        [entityId]: { entityId, modelId: selectedModelId, versionId: selectedVersionId },
      }))

      if (onBindingChange) {
        onBindingChange(entityId, selectedModelId, selectedVersionId)
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : t('validation.saveBindingFailed')
      setError(message)
    } finally {
      setIsSaving(false)
    }
  }, [sceneId, entityId, selectedModelId, selectedVersionId, onBindingChange, setBinding, fetchBindings])

  const bindableEntities = useMemo(() => {
    const entities = Array.isArray(sceneDetail?.entities) ? sceneDetail.entities : []
    return entities.filter((entity) => [
      'OHT_VEHICLE',
      'AGV_VEHICLE',
      'STOCKER',
      'ERACK',
      'MANUAL_STATION',
      'CONVEYOR',
      'OPERATOR',
      'MACHINE',
      'BAY',
      'CHUTE',
    ].includes(entity.type))
  }, [sceneDetail])

  const safeScenes = Array.isArray(scenes) ? scenes : []

  return (
    <div className="space-y-4">
      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-3 flex items-center justify-between">
          <span className="text-red-400 text-sm">{error}</span>
          <button
            onClick={() => setError(null)}
            className="text-gray-400 hover:text-white transition-colors"
          >
            ✕
          </button>
        </div>
      )}

      <div>
        <label className="block text-gray-400 text-sm mb-2">{t('binding.scene')}</label>
        <select
          data-testid="binding-scene-select"
          value={sceneId || ''}
          onChange={handleSceneChange}
          disabled={isLoadingScene}
          className="w-full px-4 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
        >
          <option value="">{t('binding.selectScene')}</option>
          {safeScenes.map((scene: SceneSummary) => (
            <option key={scene.sceneId} value={scene.sceneId}>
              {scene.name}
            </option>
          ))}
        </select>
      </div>

      {sceneDetail && (
        <div>
          <label className="block text-gray-400 text-sm mb-2">{t('binding.entity')}</label>
          <select
            data-testid="binding-entity-select"
            value={entityId || ''}
            onChange={handleEntityChange}
            disabled={isLoadingScene}
            className="w-full px-4 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
          >
            <option value="">{t('binding.selectEntity')}</option>
            {bindableEntities.map((entity) => (
              <option key={entity.id} value={entity.id}>
                {entity.name || `${entity.type} (${entity.id.slice(0, 8)})`}
              </option>
            ))}
          </select>
        </div>
      )}

      {entityId && (
        <div>
          <label className="block text-gray-400 text-sm mb-2">{t('binding.model')}</label>
          <select
            data-testid="binding-model-select"
            value={selectedModelId || ''}
            onChange={handleModelChange}
            className="w-full px-4 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors"
          >
            <option value="">{t('binding.selectModel')}</option>
            {models
              .filter((m) => m.status === 'ACTIVE')
              .map((model) => (
                <option key={model.modelId} value={model.modelId}>
                  {model.name} ({model.type})
                </option>
              ))}
          </select>
        </div>
      )}

      {selectedModelId && (
        <div>
          <label className="block text-gray-400 text-sm mb-2">{t('binding.version')}</label>
          <select
            data-testid="binding-version-select"
            value={selectedVersionId || ''}
            onChange={(e) => setSelectedVersionId(e.target.value || null)}
            disabled={isLoadingVersions}
            className="w-full px-4 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors disabled:opacity-50"
          >
            <option value="">{t('binding.selectVersion')}</option>
            {versionOptions.map((version) => (
              <option key={version.versionId} value={version.versionId}>
                v{version.version} {version.isDefault ? t('binding.defaultSuffix') : ''} - {version.status}
              </option>
            ))}
          </select>
        </div>
      )}

      {entityId && sceneBindings[entityId] && (
        <div className="p-3 bg-industrial-dark border border-industrial-steel/30 rounded-lg">
          <div className="text-gray-400 text-xs mb-1">{t('binding.currentBinding')}</div>
          <div className="text-white text-sm">
            {tf('binding.currentModel', { name: models.find((m) => m.modelId === sceneBindings[entityId]?.modelId)?.name || t('common.unknown') })}
          </div>
          <div className="text-gray-500 text-xs mt-1">
            {tf('binding.currentVersionId', { versionId: sceneBindings[entityId].versionId })}
          </div>
        </div>
      )}

      {entityId && (
        <div className="flex justify-end">
          <button
            data-testid="binding-save-button"
            onClick={handleSaveBinding}
            disabled={!selectedModelId || !selectedVersionId || isSaving}
            className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors text-sm font-bold disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSaving ? t('binding.saving') : t('binding.saveBinding')}
          </button>
        </div>
      )}

      {!sceneId && (
        <div className="text-center text-gray-400 text-sm py-4">
          {t('binding.chooseScenePrompt')}
        </div>
      )}
    </div>
  )
}

export default EntityModelSelector
