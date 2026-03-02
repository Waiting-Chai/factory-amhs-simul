/**
 * OHT Track Editor component.
 *
 * @author shentw
 * @version 1.2
 * @since 2026-02-12
 */

import React, { useState, useCallback, useRef, useMemo } from 'react'
import { Path, SimpleControlPoint } from '@/types/api'
import { useSceneStore } from '@/store/sceneStore'

interface OHTTrackEditorProps {
  sceneId: string
  onPathsChange: (paths: Path[]) => void
}

/**
 * OHT Track Editor Component
 */
export const OHTTrackEditor: React.FC<OHTTrackEditorProps> = ({ sceneId: _sceneId, onPathsChange }) => {
  const { currentScene } = useSceneStore()

  // Local state
  const [selectedPathId, setSelectedPathId] = useState<string | null>(null)
  const [activeTool, setActiveTool] = useState<'select' | 'segment' | 'controlPoint'>('select')

  // Refs
  const canvasRef = useRef<HTMLCanvasElement>(null)

  // Get OHT paths from scene
  const paths = useMemo(() => {
    const scenePaths = currentScene?.paths || []
    // Filter only OHT_PATH type paths
    return scenePaths.filter((p): p is Path => p.type === 'OHT_PATH')
  }, [currentScene])

  // Handle tool selection
  const handleToolClick = useCallback((tool: 'select' | 'segment' | 'controlPoint') => {
    setActiveTool(tool)
    setSelectedPathId(null)
  }, [])

  // Handle path selection
  const handlePathClick = useCallback((pathId: string) => {
    setSelectedPathId(pathId)
  }, [])

  // Add new path
  const handleAddPath = useCallback(() => {
    const newPath: Path = {
      id: `path-${Date.now()}`,
      name: `OHT Path ${paths.length + 1}`,
      type: 'OHT_PATH',
      points: [],
      segments: [],
      controlPoints: []
    }

    const updatedPaths = [...paths, newPath]
    onPathsChange(updatedPaths)
    handlePathClick(newPath.id)
  }, [paths, onPathsChange, handlePathClick])

  // Delete path
  const handleDeletePath = useCallback((pathId: string) => {
    const updatedPaths = paths.filter((p: Path) => p.id !== pathId)
    onPathsChange(updatedPaths)

    if (selectedPathId === pathId) {
      setSelectedPathId(null)
    }
  }, [paths, onPathsChange, selectedPathId])

  // Save changes
  const handleSave = useCallback(() => {
    setActiveTool('select')
    setSelectedPathId(null)
  }, [])

  // Handle canvas mouse events
  const handleCanvasMouseDown = useCallback((e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!canvasRef.current) return

    const rect = canvasRef.current.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top

    if (activeTool === 'select') {
      // Select path on click
      const clickedPath = paths.find((p: Path) => {
        return (p.controlPoints as SimpleControlPoint[] | undefined)?.some((cp) => {
          if (!cp) return false
          const dx = cp.x - x
          const dy = cp.y - y
          return Math.sqrt(dx * dx + dy * dy) < 20
        }) || false
      })

      if (clickedPath) {
        handlePathClick(clickedPath.id)
      }
    }
  }, [activeTool, paths, handlePathClick])

  if (!currentScene) {
    return <div className="flex items-center justify-center p-8">Loading scene...</div>
  }

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
          onClick={() => handleToolClick('segment')}
          className={`px-3 py-2 rounded-lg transition-colors ${
            activeTool === 'segment' ? 'bg-amber-500 text-white' : 'bg-industrial-steel text-gray-300 hover:text-white'
          }`}
        >
          Edit Segment
        </button>
      </div>

      {/* Path list */}
      <div className="mb-4 max-h-32 overflow-y-auto">
        {paths.map((path: Path) => (
          <div
            key={path.id}
            className={`p-2 rounded mb-1 cursor-pointer flex justify-between items-center ${
              selectedPathId === path.id ? 'bg-amber-500/20 border border-amber-500' : 'bg-industrial-dark border border-industrial-steel/30 hover:bg-industrial-steel'
            }`}
            onClick={() => handlePathClick(path.id)}
          >
            <span className="text-white text-sm">{path.name || path.id}</span>
            <button
              onClick={(e: React.MouseEvent) => {
                e.stopPropagation()
                handleDeletePath(path.id)
              }}
              className="text-red-400 hover:text-red-300 text-xs"
            >
              Delete
            </button>
          </div>
        ))}
        {paths.length === 0 && (
          <div className="text-gray-500 text-sm text-center py-2">
            No OHT paths. Click "Add Path" to create one.
          </div>
        )}
      </div>

      {/* Canvas */}
      <canvas
        ref={canvasRef}
        width={800}
        height={400}
        className="border border-industrial-steel/30 bg-gray-900 w-full cursor-crosshair"
        onMouseDown={handleCanvasMouseDown}
        style={{ cursor: activeTool === 'select' ? 'default' : 'crosshair' }}
      />

      {/* Path count */}
      <div className="mt-2 text-gray-400 text-xs text-center">
        {paths.length} OHT path{paths.length !== 1 ? 's' : ''}
      </div>

      {/* Actions */}
      <div className="flex gap-2 mt-4">
        <button
          onClick={handleAddPath}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg transition-colors font-bold"
        >
          Add Path
        </button>
        <button
          onClick={handleSave}
          disabled={selectedPathId === null}
          className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-lg transition-colors disabled:opacity-50 font-bold"
        >
          Save
        </button>
      </div>
    </div>
  )
}
