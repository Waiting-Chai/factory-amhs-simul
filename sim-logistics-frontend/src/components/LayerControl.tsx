/**
 * Layer Control component for scene editor.
 *
 * Provides layer visibility, opacity control, and z-order management.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-12
 */

import React, { useState, useCallback } from 'react'
import { t } from '@/shared/i18n'

export type SceneLayer =
  | 'entities'
  | 'paths'
  | 'safetyZones'
  | 'controlPoints'
  | 'processSteps'

export interface LayerConfig {
  id: SceneLayer
  name: string
  visible: boolean
  opacity: number
  locked: boolean
  zIndex: number
}

interface LayerControlProps {
  layers: LayerConfig[]
  onLayersChange: (layers: LayerConfig[]) => void
}

const DEFAULT_LAYERS: LayerConfig[] = [
  { id: 'safetyZones', name: 'Safety Zones', visible: true, opacity: 0.3, locked: false, zIndex: 0 },
  { id: 'paths', name: 'OHT Tracks', visible: true, opacity: 1.0, locked: false, zIndex: 1 },
  { id: 'controlPoints', name: 'Control Points', visible: true, opacity: 1.0, locked: false, zIndex: 2 },
  { id: 'entities', name: 'Entities', visible: true, opacity: 1.0, locked: false, zIndex: 3 },
  { id: 'processSteps', name: 'Process Steps', visible: true, opacity: 0.5, locked: false, zIndex: 4 },
]

/**
 * Layer Control Component
 */
export const LayerControl: React.FC<LayerControlProps> = ({ layers, onLayersChange }) => {
  // Local state for drag operation
  const [draggedLayerId, setDraggedLayerId] = useState<SceneLayer | null>(null)

  // Toggle visibility
  const handleToggleVisibility = useCallback((layerId: SceneLayer) => {
    const updatedLayers = layers.map((layer) =>
      layer.id === layerId ? { ...layer, visible: !layer.visible } : layer
    )
    onLayersChange(updatedLayers)
  }, [layers, onLayersChange])

  // Toggle lock
  const handleToggleLock = useCallback((layerId: SceneLayer) => {
    const updatedLayers = layers.map((layer) =>
      layer.id === layerId ? { ...layer, locked: !layer.locked } : layer
    )
    onLayersChange(updatedLayers)
  }, [layers, onLayersChange])

  // Update opacity
  const handleOpacityChange = useCallback((layerId: SceneLayer, opacity: number) => {
    const updatedLayers = layers.map((layer) =>
      layer.id === layerId ? { ...layer, opacity: opacity / 100 } : layer
    )
    onLayersChange(updatedLayers)
  }, [layers, onLayersChange])

  // Move layer up
  const handleMoveUp = useCallback((layerId: SceneLayer) => {
    const layerIndex = layers.findIndex((l) => l.id === layerId)
    if (layerIndex >= layers.length - 1) return

    const updatedLayers = [...layers]
    const temp = updatedLayers[layerIndex].zIndex
    updatedLayers[layerIndex].zIndex = updatedLayers[layerIndex + 1].zIndex
    updatedLayers[layerIndex + 1].zIndex = temp

    // Sort by zIndex
    updatedLayers.sort((a, b) => a.zIndex - b.zIndex)
    onLayersChange(updatedLayers)
  }, [layers, onLayersChange])

  // Move layer down
  const handleMoveDown = useCallback((layerId: SceneLayer) => {
    const layerIndex = layers.findIndex((l) => l.id === layerId)
    if (layerIndex <= 0) return

    const updatedLayers = [...layers]
    const temp = updatedLayers[layerIndex].zIndex
    updatedLayers[layerIndex].zIndex = updatedLayers[layerIndex - 1].zIndex
    updatedLayers[layerIndex - 1].zIndex = temp

    // Sort by zIndex
    updatedLayers.sort((a, b) => a.zIndex - b.zIndex)
    onLayersChange(updatedLayers)
  }, [layers, onLayersChange])

  // Handle drag start
  const handleDragStart = useCallback((layerId: SceneLayer) => {
    setDraggedLayerId(layerId)
  }, [])

  // Handle drag over
  const handleDragOver = useCallback((e: React.DragEvent, targetLayerId: SceneLayer) => {
    e.preventDefault()
    if (draggedLayerId === null || draggedLayerId === targetLayerId) return

    const draggedIndex = layers.findIndex((l) => l.id === draggedLayerId)
    const targetIndex = layers.findIndex((l) => l.id === targetLayerId)

    if (draggedIndex === -1 || targetIndex === -1) return

    const updatedLayers = [...layers]
    const [draggedLayer] = updatedLayers.splice(draggedIndex, 1)
    updatedLayers.splice(targetIndex, 0, draggedLayer)

    // Update z-indices
    updatedLayers.forEach((layer, index) => {
      layer.zIndex = index
    })

    onLayersChange(updatedLayers)
  }, [draggedLayerId, layers, onLayersChange])

  // Handle drag end
  const handleDragEnd = useCallback(() => {
    setDraggedLayerId(null)
  }, [])

  // Reset to defaults
  const handleReset = useCallback(() => {
    onLayersChange(DEFAULT_LAYERS.map(l => ({ ...l })))
  }, [onLayersChange])

  // Show/hide all
  const handleShowAll = useCallback(() => {
    const updatedLayers = layers.map((layer) => ({ ...layer, visible: true }))
    onLayersChange(updatedLayers)
  }, [layers, onLayersChange])

  const handleHideAll = useCallback(() => {
    const updatedLayers = layers.map((layer) => ({ ...layer, visible: false }))
    onLayersChange(updatedLayers)
  }, [layers, onLayersChange])

  return (
    <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-4">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-white font-medium">{t('layer.title')}</h3>
        <div className="flex gap-2">
          <button
            onClick={handleShowAll}
            className="px-2 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded text-xs transition-colors"
          >
            {t('layer.showAll')}
          </button>
          <button
            onClick={handleHideAll}
            className="px-2 py-1 bg-red-600 hover:bg-red-700 text-white rounded text-xs transition-colors"
          >
            {t('layer.hideAll')}
          </button>
          <button
            onClick={handleReset}
            className="px-2 py-1 bg-industrial-steel hover:bg-industrial-steel/80 text-white rounded text-xs transition-colors"
          >
            {t('layer.reset')}
          </button>
        </div>
      </div>

      {/* Layer list */}
      <div className="space-y-1">
        {layers
          .slice()
          .sort((a, b) => b.zIndex - a.zIndex)
          .map((layer) => (
            <div
              key={layer.id}
              draggable={!layer.locked}
              onDragStart={() => handleDragStart(layer.id)}
              onDragOver={(e) => handleDragOver(e, layer.id)}
              onDragEnd={handleDragEnd}
              className={`flex items-center gap-2 p-2 rounded transition-colors ${
                draggedLayerId === layer.id
                  ? 'opacity-50 bg-amber-500/20'
                  : 'bg-industrial-dark border border-industrial-steel/30 hover:bg-industrial-steel'
              } ${layer.locked ? 'cursor-not-allowed' : 'cursor-move'}`}
            >
              {/* Drag handle */}
              <div className={`text-gray-500 ${layer.locked ? 'opacity-30' : ''}`}>
                ⋮⋮
              </div>

              {/* Layer name */}
              <span className={`flex-1 text-sm ${
                layer.visible ? 'text-white' : 'text-gray-500'
              }`}>
                {layer.name}
              </span>

              {/* Visibility toggle */}
              <button
                onClick={() => handleToggleVisibility(layer.id)}
                className={`p-1 rounded transition-colors ${
                  layer.visible
                    ? 'bg-emerald-600 text-white'
                    : 'bg-industrial-steel text-gray-500'
                }`}
                title={layer.visible ? t('layer.visible') : t('layer.hidden')}
              >
                {layer.visible ? '👁️' : '👁️‍🗨️'}
              </button>

              {/* Lock toggle */}
              <button
                onClick={() => handleToggleLock(layer.id)}
                className={`p-1 rounded transition-colors ${
                  layer.locked
                    ? 'bg-amber-600 text-white'
                    : 'bg-industrial-steel text-gray-500'
                }`}
                title={layer.locked ? t('layer.locked') : t('layer.unlocked')}
              >
                {layer.locked ? '🔒' : '🔓'}
              </button>

              {/* Opacity slider */}
              <div className="flex items-center gap-1 w-20">
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={layer.opacity * 100}
                  onChange={(e) => handleOpacityChange(layer.id, parseInt(e.target.value))}
                  className="w-full h-1 bg-industrial-steel rounded-lg appearance-none cursor-pointer"
                  disabled={layer.locked}
                />
                <span className="text-gray-400 text-xs w-8 text-right">
                  {Math.round(layer.opacity * 100)}%
                </span>
              </div>

              {/* Move up/down buttons */}
              <div className="flex gap-1">
                <button
                  onClick={() => handleMoveUp(layer.id)}
                  disabled={layer.locked}
                  className="p-1 bg-industrial-steel hover:bg-industrial-steel/80 text-gray-400 rounded disabled:opacity-30 text-xs"
                  title={t('layer.moveUp')}
                >
                  ▲
                </button>
                <button
                  onClick={() => handleMoveDown(layer.id)}
                  disabled={layer.locked}
                  className="p-1 bg-industrial-steel hover:bg-industrial-steel/80 text-gray-400 rounded disabled:opacity-30 text-xs"
                  title={t('layer.moveDown')}
                >
                  ▼
                </button>
              </div>
            </div>
          ))}
      </div>

      {/* Legend */}
      <div className="mt-4 pt-4 border-t border-industrial-steel/30">
        <div className="text-gray-500 text-xs space-y-1">
          <div>👁️ {t('layer.legend.visible')} | 👁️‍🗨️ {t('layer.legend.hidden')}</div>
          <div>🔒 {t('layer.legend.locked')} | 🔓 {t('layer.legend.unlocked')}</div>
          <div>{t('layer.legend.dragHint')}</div>
        </div>
      </div>
    </div>
  )
}

/**
 * Hook for managing layer state
 */
// eslint-disable-next-line react-refresh/only-export-components -- Hook is intentionally exported alongside component
export const useLayerState = () => {
  const [layers, setLayers] = React.useState<LayerConfig[]>(
    DEFAULT_LAYERS.map(l => ({ ...l }))
  )

  const getVisibleLayers = useCallback((): SceneLayer[] => {
    return layers
      .filter(l => l.visible)
      .sort((a, b) => a.zIndex - b.zIndex)
      .map(l => l.id)
  }, [layers])

  const isLayerVisible = useCallback((layerId: SceneLayer): boolean => {
    return layers.find(l => l.id === layerId)?.visible ?? true
  }, [layers])

  const getLayerOpacity = useCallback((layerId: SceneLayer): number => {
    return layers.find(l => l.id === layerId)?.opacity ?? 1.0
  }, [layers])

  return {
    layers,
    setLayers,
    getVisibleLayers,
    isLayerVisible,
    getLayerOpacity,
  }
}
