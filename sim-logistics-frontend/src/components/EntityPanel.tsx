/**
 * Entity Panel component for scene editor.
 *
 * Provides entity list, drag-and-drop, property editing, and transport type configuration.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-12
 */

import React, { useState, useCallback, useMemo } from 'react'
import { Entity, EntityType, TransportType } from '@/types/api'
import { useSceneStore } from '@/store/sceneStore'

interface EntityPanelProps {
  sceneId: string
  onEntitiesChange: (entities: Entity[]) => void
}

type EntityEditor = {
  selectedEntity: Entity | null
  isDragging: boolean
}

/**
 * Entity Panel Component
 */
export const EntityPanel: React.FC<EntityPanelProps> = ({ sceneId: _sceneId, onEntitiesChange }) => {
  const { currentScene } = useSceneStore()

  // Local state
  const [selectedEntityId, setSelectedEntityId] = useState<string | null>(null)
  const [activeTool, setActiveTool] = useState<'select' | 'add'>('select')
  const [entityEditors, setEntityEditors] = useState<Map<string, EntityEditor>>(new Map())

  // Get entities from scene
  const entities = useMemo(() => {
    return currentScene?.entities || []
  }, [currentScene])

  // Available entity types
  const entityTypes: EntityType[] = [
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
    'CONTROL_POINT',
  ]

  // Transport types for equipment
  const transportTypes: TransportType[] = ['OHT', 'AGV', 'HUMAN', 'CONVEYOR']

  // Equipment types that support transport types (memoized to avoid dependency warning)
  const equipmentTypes = useMemo<EntityType[]>(() => ['STOCKER', 'ERACK', 'MANUAL_STATION', 'MACHINE', 'BAY', 'CHUTE'], [])

  // Initialize entity editor
  const initEntityEditor = useCallback((entity: Entity): EntityEditor => {
    return {
      selectedEntity: { ...entity },
      isDragging: false,
    }
  }, [])

  // Handle tool selection
  const handleToolClick = useCallback((tool: 'select' | 'add') => {
    setActiveTool(tool)
    setSelectedEntityId(null)
  }, [])

  // Handle entity selection
  const handleEntityClick = useCallback((entityId: string) => {
    setSelectedEntityId(entityId)
    setActiveTool('select')

    // Initialize editor if not exists
    if (!entityEditors.has(entityId)) {
      const entity = entities.find((e: Entity) => e.id === entityId)
      if (entity) {
        setEntityEditors(prev => new Map(prev).set(entityId, initEntityEditor(entity)))
      }
    }
  }, [entityEditors, entities, initEntityEditor])

  // Update entity property in editor
  const updateEntity = useCallback((entityId: string, property: string, value: unknown) => {
    const editor = entityEditors.get(entityId)
    if (!editor || !editor.selectedEntity) return

    const updatedEntity = { ...editor.selectedEntity, [property]: value }
    const updatedEditor = { ...editor, selectedEntity: updatedEntity as Entity }
    setEntityEditors(prev => new Map(prev).set(entityId, updatedEditor))
  }, [entityEditors])

  // Delete entity
  const handleDeleteEntity = useCallback(() => {
    if (!selectedEntityId) return

    const updatedEntities = entities.filter((e: Entity) => e.id !== selectedEntityId)
    onEntitiesChange(updatedEntities)

    // Clear editor
    setEntityEditors(prev => {
      const newMap = new Map(prev)
      newMap.delete(selectedEntityId)
      return newMap
    })
    setSelectedEntityId(null)
  }, [selectedEntityId, entities, onEntitiesChange])

  // Handle add entity
  const handleAddEntity = useCallback((entityType: EntityType) => {
    const newEntity: Entity = {
      id: `entity-${Date.now()}`,
      type: entityType,
      name: `${entityType}_${entities.length + 1}`,
      position: { x: 400, y: 0, z: 0 },
      rotation: { x: 0, y: 0, z: 0 },
    }

    // Add supported transport types for equipment
    if (equipmentTypes.includes(entityType)) {
      (newEntity as Entity & { supportedTransportTypes: TransportType[] }).supportedTransportTypes = ['OHT', 'AGV']
    }

    const updatedEntities = [...entities, newEntity]
    onEntitiesChange(updatedEntities)

    // Initialize editor
    setEntityEditors(prev => new Map(prev).set(newEntity.id, initEntityEditor(newEntity)))
    setSelectedEntityId(newEntity.id)
    setActiveTool('select')
  }, [entities, onEntitiesChange, initEntityEditor, equipmentTypes])

  // Save changes
  const handleSave = useCallback(() => {
    // Apply all pending changes from editors
    const updatedEntities = entities.map((entity: Entity) => {
      const editor = entityEditors.get(entity.id)
      return editor?.selectedEntity || entity
    })

    onEntitiesChange(updatedEntities)

    // Clear editors after save
    setEntityEditors(new Map())
    setActiveTool('select')
    setSelectedEntityId(null)
  }, [entities, entityEditors, onEntitiesChange])

  // Handle transport type toggle
  const handleTransportTypeToggle = useCallback((entityId: string, transportType: TransportType) => {
    const editor = entityEditors.get(entityId)
    if (!editor || !editor.selectedEntity) return

    const entity = editor.selectedEntity as Entity & { supportedTransportTypes?: TransportType[] }
    const current = entity.supportedTransportTypes || []
    const isSelected = current.includes(transportType)
    const updated = isSelected
      ? current.filter((t: TransportType) => t !== transportType)
      : [...current, transportType]

    updateEntity(entityId, 'supportedTransportTypes', updated)
  }, [entityEditors, updateEntity])

  if (!currentScene) {
    return <div className="flex items-center justify-center p-8">Loading scene...</div>
  }

  const selectedEditor = selectedEntityId ? entityEditors.get(selectedEntityId) : null

  return (
    <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-4">
      {/* Toolbar */}
      <div className="flex gap-2 mb-4">
        <button
          onClick={() => handleToolClick('select')}
          className={`px-3 py-2 rounded-lg transition-colors ${
            activeTool === 'select' ? 'bg-amber-500 text-white' : 'bg-industrial-steel text-gray-300 hover:text-white'
          }`}
        >
          Select
        </button>
        <button
          onClick={() => handleToolClick('add')}
          className={`px-3 py-2 rounded-lg transition-colors ${
            activeTool === 'add' ? 'bg-amber-500 text-white' : 'bg-industrial-steel text-gray-300 hover:text-white'
          }`}
        >
          Add Entity
        </button>
      </div>

      {/* Entity type selector for add tool */}
      {activeTool === 'add' && (
        <div className="bg-industrial-dark border border-industrial-steel/30 rounded-lg p-4 mb-4">
          <h3 className="text-white font-medium mb-3">Select Entity Type</h3>
          <div className="grid grid-cols-3 gap-2">
            {entityTypes.map((type) => (
              <button
                key={type}
                onClick={() => handleAddEntity(type)}
                className="px-3 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg transition-colors text-sm"
              >
                {type.replace('_', ' ')}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Selected entity editor */}
      {selectedEntityId && selectedEditor && selectedEditor.selectedEntity && (
        <div className="bg-industrial-dark border border-industrial-steel/30 rounded-lg p-4 mb-4">
          <h3 className="text-white font-medium mb-3">Entity Properties</h3>
          <div className="space-y-3">
            {/* ID */}
            <div>
              <label className="text-gray-400 text-xs">ID</label>
              <input
                type="text"
                value={selectedEditor.selectedEntity.id}
                disabled
                className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-gray-400 text-sm"
              />
            </div>

            {/* Type */}
            <div>
              <label className="text-gray-400 text-xs">Type</label>
              <input
                type="text"
                value={selectedEditor.selectedEntity.type}
                disabled
                className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-gray-400 text-sm"
              />
            </div>

            {/* Name */}
            <div>
              <label className="text-gray-400 text-xs">Name</label>
              <input
                type="text"
                value={selectedEditor.selectedEntity?.name || ''}
                onChange={(e) => updateEntity(selectedEntityId, 'name', e.target.value)}
                className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
              />
            </div>

            {/* Position */}
            <div>
              <label className="text-gray-400 text-xs">Position (meters)</label>
              <div className="grid grid-cols-3 gap-2">
                <div>
                  <span className="text-gray-500 text-xs">X</span>
                  <input
                    type="number"
                    step="0.1"
                    value={selectedEditor.selectedEntity?.position.x ?? 0}
                    onChange={(e) => updateEntity(selectedEntityId, 'position', {
                      ...(selectedEditor.selectedEntity?.position ?? { x: 0, y: 0, z: 0 }),
                      x: parseFloat(e.target.value) || 0
                    })}
                    className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
                  />
                </div>
                <div>
                  <span className="text-gray-500 text-xs">Y</span>
                  <input
                    type="number"
                    step="0.1"
                    value={selectedEditor.selectedEntity?.position.y ?? 0}
                    onChange={(e) => updateEntity(selectedEntityId, 'position', {
                      ...(selectedEditor.selectedEntity?.position ?? { x: 0, y: 0, z: 0 }),
                      y: parseFloat(e.target.value) || 0
                    })}
                    className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
                  />
                </div>
                <div>
                  <span className="text-gray-500 text-xs">Z</span>
                  <input
                    type="number"
                    step="0.1"
                    value={selectedEditor.selectedEntity?.position.z ?? 0}
                    onChange={(e) => updateEntity(selectedEntityId, 'position', {
                      ...(selectedEditor.selectedEntity?.position ?? { x: 0, y: 0, z: 0 }),
                      z: parseFloat(e.target.value) || 0
                    })}
                    className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
                  />
                </div>
              </div>
            </div>

            {/* Rotation (radians) */}
            <div>
              <label className="text-gray-400 text-xs">Rotation (radians [-π, π])</label>
              <div className="grid grid-cols-3 gap-2">
                <div>
                  <span className="text-gray-500 text-xs">X</span>
                  <input
                    type="number"
                    step="0.1"
                    min={-Math.PI}
                    max={Math.PI}
                    value={selectedEditor.selectedEntity?.rotation?.x ?? 0}
                    onChange={(e) => updateEntity(selectedEntityId, 'rotation', {
                      ...(selectedEditor.selectedEntity?.rotation ?? { x: 0, y: 0, z: 0 }),
                      x: Math.max(-Math.PI, Math.min(Math.PI, parseFloat(e.target.value) || 0))
                    })}
                    className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
                  />
                </div>
                <div>
                  <span className="text-gray-500 text-xs">Y</span>
                  <input
                    type="number"
                    step="0.1"
                    min={-Math.PI}
                    max={Math.PI}
                    value={selectedEditor.selectedEntity?.rotation?.y ?? 0}
                    onChange={(e) => updateEntity(selectedEntityId, 'rotation', {
                      ...(selectedEditor.selectedEntity?.rotation ?? { x: 0, y: 0, z: 0 }),
                      y: Math.max(-Math.PI, Math.min(Math.PI, parseFloat(e.target.value) || 0))
                    })}
                    className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
                  />
                </div>
                <div>
                  <span className="text-gray-500 text-xs">Z</span>
                  <input
                    type="number"
                    step="0.1"
                    min={-Math.PI}
                    max={Math.PI}
                    value={selectedEditor.selectedEntity?.rotation?.z ?? 0}
                    onChange={(e) => updateEntity(selectedEntityId, 'rotation', {
                      ...(selectedEditor.selectedEntity?.rotation ?? { x: 0, y: 0, z: 0 }),
                      z: Math.max(-Math.PI, Math.min(Math.PI, parseFloat(e.target.value) || 0))
                    })}
                    className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
                  />
                </div>
              </div>
            </div>

            {/* Supported Transport Types (for equipment) */}
            {selectedEditor.selectedEntity && equipmentTypes.includes(selectedEditor.selectedEntity.type) && (
              <div>
                <label className="text-gray-400 text-xs">Supported Transport Types</label>
                <div className="flex flex-wrap gap-2 mt-1">
                  {transportTypes.map((type) => {
                    const entity = selectedEditor.selectedEntity as Entity & { supportedTransportTypes?: TransportType[] }
                    const isSelected = entity.supportedTransportTypes?.includes(type)

                    return (
                      <button
                        key={type}
                        type="button"
                        onClick={() => handleTransportTypeToggle(selectedEntityId, type)}
                        className={`px-2 py-1 rounded text-xs transition-colors ${
                          isSelected
                            ? 'bg-emerald-600 text-white'
                            : 'bg-industrial-steel text-gray-300 hover:text-white'
                        }`}
                      >
                        {type}
                      </button>
                    )
                  })}
                </div>
              </div>
            )}

            {/* Properties (custom) */}
            <div>
              <label className="text-gray-400 text-xs">Custom Properties (JSON)</label>
              <textarea
                value={JSON.stringify(selectedEditor.selectedEntity.properties || {}, null, 2)}
                onChange={(e) => {
                  try {
                    const parsed = JSON.parse(e.target.value)
                    updateEntity(selectedEntityId, 'properties', parsed)
                  } catch {
                    // Invalid JSON, ignore
                  }
                }}
                rows={3}
                className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm font-mono text-xs"
              />
            </div>
          </div>

          {/* Delete button */}
          <div className="flex justify-end mt-4">
            <button
              onClick={handleDeleteEntity}
              className="px-3 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors text-sm"
            >
              Delete Entity
            </button>
          </div>
        </div>
      )}

      {/* Entity list */}
      <div className="max-h-64 overflow-y-auto">
        <h3 className="text-gray-400 text-xs mb-2">Entities ({entities.length})</h3>
        {entities.map((entity: Entity) => (
          <div
            key={entity.id}
            className={`p-2 rounded mb-1 cursor-pointer flex justify-between items-center ${
              selectedEntityId === entity.id
                ? 'bg-amber-500/20 border border-amber-500'
                : 'bg-industrial-dark border border-industrial-steel/30 hover:bg-industrial-steel'
            }`}
            onClick={() => handleEntityClick(entity.id)}
          >
            <div>
              <span className="text-white text-sm">{entity.name || entity.id}</span>
              <span className="text-gray-500 text-xs ml-2">({entity.type})</span>
            </div>
          </div>
        ))}
        {entities.length === 0 && (
          <div className="text-gray-500 text-sm text-center py-4">
            No entities yet. Click "Add Entity" to create one.
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="flex gap-2 mt-4">
        <button
          onClick={handleSave}
          disabled={selectedEntityId === null}
          className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-lg transition-colors disabled:opacity-50 font-bold"
        >
          Save
        </button>
      </div>
    </div>
  )
}
