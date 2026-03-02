/**
 * Safety Zone Editor component.
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-12
 */

import React, { useState, useCallback, useRef, useMemo } from 'react'
import { SafetyZone } from '@/types/api'
import { useSceneStore } from '@/store/sceneStore'

interface SafetyZoneEditorProps {
  sceneId: string
  onZonesChange: (zones: SafetyZone[]) => void
}

type ZoneEditor = {
  selectedZone: SafetyZone | null
  isDragging: boolean
  dragHandle: { x: number; y: number } | null
}

/**
 * Safety Zone Editor Component
 */
export const SafetyZoneEditor: React.FC<SafetyZoneEditorProps> = ({ sceneId: _sceneId, onZonesChange }) => {
  const { currentScene } = useSceneStore()

  // Local state
  const [selectedZoneId, setSelectedZoneId] = useState<string | null>(null)
  const [activeTool, setActiveTool] = useState<'select' | 'rectangle' | 'circle' | 'polygon'>('select')
  const [zoneEditors, setZoneEditors] = useState<Map<string, ZoneEditor>>(new Map())

  // Refs
  const canvasRef = useRef<HTMLCanvasElement>(null)

  // Get safety zones from scene
  const zones = useMemo(() => {
    return currentScene?.safetyZones || []
  }, [currentScene])

  // Initialize zone editor
  const initZoneEditor = useCallback((zone: SafetyZone): ZoneEditor => {
    return {
      selectedZone: { ...zone },
      isDragging: false,
      dragHandle: null,
    }
  }, [])

  // Handle tool selection
  const handleToolClick = useCallback((tool: 'select' | 'rectangle' | 'circle' | 'polygon') => {
    setActiveTool(tool)
    setSelectedZoneId(null)
  }, [])

  // Update zone property in editor
  const updateZone = useCallback((zoneId: string, property: string, value: unknown) => {
    const editor = zoneEditors.get(zoneId)
    if (!editor || !editor.selectedZone) return

    const updatedZone = { ...editor.selectedZone, [property]: value }
    const updatedEditor = { ...editor, selectedZone: updatedZone as SafetyZone }
    setZoneEditors(prev => new Map(prev).set(zoneId, updatedEditor))
  }, [zoneEditors])

  // Delete zone
  const handleDeleteZone = useCallback(() => {
    if (!selectedZoneId) return

    const updatedZones = zones.filter((z: SafetyZone) => z.id !== selectedZoneId)
    onZonesChange(updatedZones)

    // Clear editor
    setZoneEditors(prev => {
      const newMap = new Map(prev)
      newMap.delete(selectedZoneId)
      return newMap
    })
    setSelectedZoneId(null)
  }, [selectedZoneId, zones, onZonesChange])

  // Handle canvas click
  const handleCanvasClick = useCallback(() => {
    if (activeTool === 'select') return

    const canvas = canvasRef.current
    if (!canvas) return

    const bounds = canvas.getBoundingClientRect()
    const x = bounds.width / 2
    const y = bounds.height / 2

    const newZone: SafetyZone = {
      id: `zone-${Date.now()}`,
      type: activeTool,
      priority: 'HUMAN_FIRST',
      maxHumans: 10,
      maxVehicles: 0,
      position: { x, y }
    }

    if (activeTool === 'rectangle') {
      newZone.width = 100
      newZone.height = 80
    } else if (activeTool === 'circle') {
      newZone.radius = 50
    } else if (activeTool === 'polygon') {
      newZone.points = [
        { x: x - 50, y: y - 50 },
        { x: x + 50, y: y - 50 },
        { x: x, y: y + 50 }
      ]
    }

    const updatedZones = [...zones, newZone]
    onZonesChange(updatedZones)

    // Initialize editor and select the new zone
    setZoneEditors(prev => new Map(prev).set(newZone.id, initZoneEditor(newZone)))
    setSelectedZoneId(newZone.id)
    setActiveTool('select')
  }, [activeTool, zones, onZonesChange, initZoneEditor])

  // Handle zone selection
  const handleZoneSelect = useCallback((zoneId: string) => {
    setSelectedZoneId(zoneId)
    setActiveTool('select')

    // Initialize editor if not exists
    if (!zoneEditors.has(zoneId)) {
      const zone = zones.find((z: SafetyZone) => z.id === zoneId)
      if (zone) {
        setZoneEditors(prev => new Map(prev).set(zoneId, initZoneEditor(zone)))
      }
    }
  }, [zoneEditors, zones, initZoneEditor])

  // Save changes
  const handleSave = useCallback(() => {
    // Apply all pending changes from editors
    const updatedZones = zones.map((zone: SafetyZone) => {
      const editor = zoneEditors.get(zone.id)
      return editor?.selectedZone || zone
    })

    onZonesChange(updatedZones)

    // Clear editors after save
    setZoneEditors(new Map())
    setActiveTool('select')
    setSelectedZoneId(null)
  }, [zones, zoneEditors, onZonesChange])

  if (!currentScene) {
    return <div className="flex items-center justify-center p-8">Loading scene...</div>
  }

  const selectedEditor = selectedZoneId ? zoneEditors.get(selectedZoneId) : null

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
          onClick={() => handleToolClick('rectangle')}
          className={`px-3 py-2 rounded-lg transition-colors ${
            activeTool === 'rectangle' ? 'bg-amber-500 text-white' : 'bg-industrial-steel text-gray-300 hover:text-white'
          }`}
        >
          Rectangle
        </button>
        <button
          onClick={() => handleToolClick('circle')}
          className={`px-3 py-2 rounded-lg transition-colors ${
            activeTool === 'circle' ? 'bg-amber-500 text-white' : 'bg-industrial-steel text-gray-300 hover:text-white'
          }`}
        >
          Circle
        </button>
        <button
          onClick={() => handleToolClick('polygon')}
          className={`px-3 py-2 rounded-lg transition-colors ${
            activeTool === 'polygon' ? 'bg-amber-500 text-white' : 'bg-industrial-steel text-gray-300 hover:text-white'
          }`}
        >
          Polygon
        </button>
      </div>

      {/* Zone list */}
      <div className="mb-4 max-h-40 overflow-y-auto">
        {zones.map((zone: SafetyZone) => (
          <div
            key={zone.id}
            className={`p-2 rounded mb-1 cursor-pointer ${
              selectedZoneId === zone.id ? 'bg-amber-500/20 border border-amber-500' : 'bg-industrial-dark border border-industrial-steel/30 hover:bg-industrial-steel'
            }`}
            onClick={() => handleZoneSelect(zone.id)}
          >
            <span className="text-white text-sm">{zone.type} ({zone.id})</span>
            <button
              onClick={(e: React.MouseEvent) => {
                e.stopPropagation()
                const updatedZones = zones.filter((z: SafetyZone) => z.id !== zone.id)
                onZonesChange(updatedZones)
                // Clear editor if deleted zone was selected
                if (selectedZoneId === zone.id) {
                  setSelectedZoneId(null)
                }
              }}
              className="text-red-400 hover:text-red-300 text-xs ml-2"
            >
              Delete
            </button>
          </div>
        ))}
        {zones.length === 0 && (
          <div className="text-gray-500 text-sm text-center py-4">
            No safety zones. Click a shape button to create one.
          </div>
        )}
      </div>

      {/* Selected zone editor */}
      {selectedZoneId && selectedEditor && selectedEditor.selectedZone && (
        <div className="bg-industrial-dark border border-industrial-steel/30 rounded-lg p-4 mb-4">
          <div className="grid grid-cols-2 gap-4">
            {/* Position */}
            <div>
              <label className="text-gray-400 text-xs">Position (X, Y)</label>
              <div className="flex gap-2 mt-1">
                <input
                  type="number"
                  value={selectedEditor.selectedZone?.position?.x ?? 0}
                  onChange={(e) => updateZone(selectedZoneId, 'position', {
                    ...(selectedEditor.selectedZone?.position ?? { x: 0, y: 0 }),
                    x: parseFloat(e.target.value) || 0
                  })}
                  className="w-20 bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
                />
                <input
                  type="number"
                  value={selectedEditor.selectedZone?.position?.y ?? 0}
                  onChange={(e) => updateZone(selectedZoneId, 'position', {
                    ...(selectedEditor.selectedZone?.position ?? { x: 0, y: 0 }),
                    y: parseFloat(e.target.value) || 0
                  })}
                  className="w-20 bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
                />
              </div>
            </div>

            {/* Priority Strategy */}
            <div>
              <label className="text-gray-400 text-xs">Priority Strategy</label>
              <select
                value={selectedEditor.selectedZone?.priority ?? 'HUMAN_FIRST'}
                onChange={(e) => updateZone(selectedZoneId, 'priority', e.target.value)}
                className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm mt-1"
              >
                <option value="HUMAN_FIRST">HUMAN_FIRST</option>
                <option value="VEHICLE_FIRST">VEHICLE_FIRST</option>
                <option value="FIFO">FIFO</option>
                <option value="PRIORITY_BASED">PRIORITY_BASED</option>
              </select>
            </div>

            {/* Rectangle specific */}
            {selectedEditor.selectedZone?.type === 'rectangle' && (
              <>
                <div>
                  <label className="text-gray-400 text-xs">Width</label>
                  <input
                    type="number"
                    value={selectedEditor.selectedZone?.width ?? 100}
                    onChange={(e) => updateZone(selectedZoneId, 'width', parseFloat(e.target.value) || 100)}
                    className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm mt-1"
                  />
                </div>
                <div>
                  <label className="text-gray-400 text-xs">Height</label>
                  <input
                    type="number"
                    value={selectedEditor.selectedZone?.height ?? 80}
                    onChange={(e) => updateZone(selectedZoneId, 'height', parseFloat(e.target.value) || 80)}
                    className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm mt-1"
                  />
                </div>
              </>
            )}

            {/* Circle specific */}
            {selectedEditor.selectedZone?.type === 'circle' && (
              <div>
                <label className="text-gray-400 text-xs">Radius</label>
                <input
                  type="number"
                  value={selectedEditor.selectedZone?.radius ?? 50}
                  onChange={(e) => updateZone(selectedZoneId, 'radius', parseFloat(e.target.value) || 50)}
                  className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm mt-1"
                />
              </div>
            )}

            {/* Capacity */}
            <div>
              <label className="text-gray-400 text-xs">Max Humans</label>
              <input
                type="number"
                value={selectedEditor.selectedZone?.maxHumans ?? 0}
                onChange={(e) => updateZone(selectedZoneId, 'maxHumans', parseInt(e.target.value) || 0)}
                className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm mt-1"
              />
            </div>
            <div>
              <label className="text-gray-400 text-xs">Max Vehicles</label>
              <input
                type="number"
                value={selectedEditor.selectedZone?.maxVehicles ?? 0}
                onChange={(e) => updateZone(selectedZoneId, 'maxVehicles', parseInt(e.target.value) || 0)}
                className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm mt-1"
              />
            </div>
          </div>

          {/* Delete button */}
          <div className="flex justify-end mt-4">
            <button
              onClick={handleDeleteZone}
              className="px-3 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors text-sm"
            >
              Delete
            </button>
          </div>
        </div>
      )}

      {/* Canvas */}
      <canvas
        ref={canvasRef}
        width={800}
        height={400}
        className="border border-industrial-steel/30 bg-gray-900 w-full cursor-crosshair"
        onClick={handleCanvasClick}
        style={{ cursor: activeTool === 'select' ? 'default' : 'crosshair' }}
      />

      {/* Zone count */}
      <div className="mt-2 text-gray-400 text-xs text-center">
        {zones.length} safety zone{zones.length !== 1 ? 's' : ''}
      </div>

      {/* Actions */}
      <div className="flex gap-2 mt-4">
        <button
          onClick={handleCanvasClick}
          disabled={activeTool === 'select'}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg transition-colors disabled:opacity-50 font-bold"
        >
          Add Zone
        </button>
        <button
          onClick={handleSave}
          disabled={selectedZoneId === null}
          className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-lg transition-colors disabled:opacity-50 font-bold"
        >
          Save
        </button>
      </div>
    </div>
  )
}
