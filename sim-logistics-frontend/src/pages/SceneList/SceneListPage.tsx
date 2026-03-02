/**
 * Scene List Page - 5.1 Project/Scene Management Module
 *
 * Implements scene management with CRUD operations, search, filtering,
 * import/export, and draft auto-save functionality.
 *
 * Design direction: Industrial utility aesthetic
 * - Dark industrial color scheme with amber accents
 * - Grid-based card layout for scene cards
 * - High contrast status indicators
 * - Generous spacing and subtle animations
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import React, { useEffect, useCallback, useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import type { SceneSummary } from '../../types'
import { useSceneStore } from '@store/sceneStore'
import { useToastStore } from '@store/toastStore'
import { ErrorState, LoadingState, EmptyState } from '@components/ErrorState'

/**
 * Scene card component
 */
const SceneCard: React.FC<{
  scene: SceneSummary
  onDelete: (id: string) => void
  onCopy: (id: string) => void
  onExport: (id: string) => void
}> = ({ scene, onDelete, onCopy, onExport }) => {
  const [showActions, setShowActions] = useState(false)

  return (
    <div
      className="group relative bg-gradient-to-br from-industrial-slate to-industrial-dark border border-industrial-steel/30 rounded-lg overflow-hidden transition-all duration-300 hover:border-amber-500/50 hover:shadow-lg hover:shadow-amber-500/10"
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => setShowActions(false)}
    >
      {/* Status indicator */}
      <div className="absolute top-3 right-3">
        <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
      </div>

      {/* Card content */}
      <div className="p-6">
        <h3 className="text-xl font-display font-bold text-white mb-2 line-clamp-2">
          {scene.name}
        </h3>

        {scene.description && (
          <p className="text-gray-400 text-sm mb-4 line-clamp-2">
            {scene.description}
          </p>
        )}

        <div className="flex items-center gap-4 text-xs text-gray-500 font-mono">
          <span>v{scene.version}</span>
          <span>{scene.entityCount} entities</span>
        </div>
      </div>

      {/* Action bar */}
      <div
        className={`absolute bottom-0 left-0 right-0 bg-industrial-dark/95 backdrop-blur border-t border-industrial-steel/30 transition-transform duration-300 ${
          showActions ? 'translate-y-0' : 'translate-y-full'
        }`}
      >
        <div className="flex items-center justify-around p-3">
          <Link
            to={`/scenes/${scene.sceneId}/edit`}
            className="text-amber-500 hover:text-amber-400 transition-colors text-sm font-medium"
          >
            Edit
          </Link>
          <button
            onClick={() => onCopy(scene.sceneId)}
            className="text-gray-400 hover:text-white transition-colors text-sm"
          >
            Copy
          </button>
          <button
            onClick={() => onExport(scene.sceneId)}
            className="text-gray-400 hover:text-white transition-colors text-sm"
          >
            Export
          </button>
          <button
            onClick={() => onDelete(scene.sceneId)}
            className="text-red-500 hover:text-red-400 transition-colors text-sm"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Scene list page component
 */
const SceneListPage: React.FC = () => {
  const {
    scenes,
    pagination,
    isLoading,
    error,
    searchQuery,
    filterType,
    fetchScenes,
    deleteScene,
    copyScene,
    exportScene,
    importScene,
    setSearchQuery,
    setFilterType,
    clearError,
  } = useSceneStore()
  const { addToast } = useToastStore()
  const safeScenes = useMemo(() => (Array.isArray(scenes) ? scenes : []), [scenes])

  const [showImportDialog, setShowImportDialog] = useState(false)
  const [importFile, setImportFile] = useState<File | null>(null)
  const [importError, setImportError] = useState<string | null>(null)

  // Initial load
  useEffect(() => {
    fetchScenes()
  }, [fetchScenes])

  // Handle delete with confirmation
  const handleDelete = useCallback(
    async (id: string) => {
      const scene = safeScenes.find((s) => s.sceneId === id)
      if (!scene) return

      if (confirm(`Delete scene "${scene.name}"? This action cannot be undone.`)) {
        try {
          await deleteScene(id)
          addToast('success', `Scene "${scene.name}" deleted successfully`)
        } catch (error) {
          addToast('error', 'Failed to delete scene')
        }
      }
    },
    [safeScenes, deleteScene, addToast]
  )

  // Handle copy
  const handleCopy = useCallback(
    async (id: string) => {
      try {
        await copyScene(id)
        addToast('success', 'Scene copied successfully')
      } catch (error) {
        addToast('error', 'Failed to copy scene')
      }
    },
    [copyScene, addToast]
  )

  // Handle export
  const handleExport = useCallback(
    async (id: string) => {
      const scene = safeScenes.find((s) => s.sceneId === id)
      try {
        await exportScene(id)
        addToast('success', `Scene "${scene?.name || id}" exported successfully`)
      } catch (error) {
        addToast('error', 'Failed to export scene')
      }
    },
    [safeScenes, exportScene, addToast]
  )

  // Handle import
  const handleImport = useCallback(async () => {
    if (!importFile) return

    setImportError(null)

    // Validate file type
    if (!importFile.name.endsWith('.json')) {
      setImportError('Please select a JSON file')
      return
    }

    // Validate file size (max 10MB)
    if (importFile.size > 10 * 1024 * 1024) {
      setImportError('File size exceeds 10MB limit')
      return
    }

    try {
      await importScene(importFile)
      addToast('success', 'Scene imported successfully')
      setShowImportDialog(false)
      setImportFile(null)
      setImportError(null)
    } catch (error) {
      setImportError('Failed to import scene. Please check the file format.')
      addToast('error', 'Failed to import scene')
    }
  }, [importFile, importScene, addToast])

  return (
    <div className="min-h-screen bg-gradient-to-br from-industrial-dark via-industrial-slate to-industrial-blue">
      {/* Header */}
      <header className="border-b border-industrial-steel/30 bg-industrial-dark/50 backdrop-blur sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-display font-bold text-white tracking-tight">
                Scenes
              </h1>
              <p className="text-gray-500 text-sm mt-1">
                Manage simulation scenarios
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={() => setShowImportDialog(true)}
                className="px-4 py-2 bg-industrial-steel hover:bg-industrial-blue text-white rounded-lg transition-colors text-sm font-medium"
              >
                Import JSON
              </button>
              <Link
                to="/scenes/new"
                className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors text-sm font-bold"
              >
                New Scene
              </Link>
            </div>
          </div>

          {/* Search and filters */}
          <div className="flex items-center gap-4 mt-4">
            <div className="flex-1 max-w-md">
              <input
                type="text"
                placeholder="Search scenes..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-4 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-amber-500/50 transition-colors"
              />
            </div>

            <select
              value={filterType || ''}
              onChange={(e) => setFilterType(e.target.value || null)}
              className="px-4 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors"
            >
              <option value="">All Types</option>
              <option value="MANUFACTURING">Manufacturing</option>
              <option value="LOGISTICS">Logistics</option>
              <option value="WAREHOUSE">Warehouse</option>
            </select>
          </div>
        </div>
      </header>

      {/* Error banner */}
      {error && (
        <div className="max-w-7xl mx-auto px-6 mt-6">
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
      )}

      {/* Scene grid */}
      <main className="max-w-7xl mx-auto px-6 py-8">
        {isLoading ? (
          <LoadingState message="Loading scenes..." />
        ) : error ? (
          <ErrorState
            title="Failed to Load Scenes"
            message={error}
            onRetry={() => {
              clearError()
              fetchScenes()
            }}
          />
        ) : safeScenes.length === 0 ? (
          <EmptyState
            title="No scenes found"
            message="Create your first simulation scenario to get started."
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {safeScenes.map((scene) => (
              <SceneCard
                key={scene.sceneId}
                scene={scene}
                onDelete={handleDelete}
                onCopy={handleCopy}
                onExport={handleExport}
              />
            ))}
          </div>
        )}

        {/* Pagination - uses 1-based page numbers */}
        {!isLoading && safeScenes.length > 0 && pagination.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 mt-8">
            <button
              onClick={() => fetchScenes(pagination.page - 1)}
              disabled={pagination.page <= 1}
              className="px-3 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm"
            >
              Previous
            </button>

            <span className="text-gray-400 text-sm">
              Page {pagination.page} of {pagination.totalPages}
            </span>

            <button
              onClick={() => fetchScenes(pagination.page + 1)}
              disabled={pagination.page >= pagination.totalPages}
              className="px-3 py-2 bg-industrial-slate border border-industrial-steel/30 rounded-lg text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors text-sm"
            >
              Next
            </button>
          </div>
        )}
      </main>

      {/* Import dialog */}
      {showImportDialog && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur flex items-center justify-center z-50">
          <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-display font-bold text-white mb-4">
              Import Scene
            </h2>

            <div className="mb-4">
              <label className="block text-gray-400 text-sm mb-2">
                Select JSON file
              </label>
              <input
                type="file"
                accept=".json"
                onChange={(e) => {
                  setImportFile(e.target.files?.[0] || null)
                  setImportError(null)
                }}
                className="w-full px-4 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white focus:outline-none focus:border-amber-500/50 transition-colors file:mr-4 file:py-1 file:px-3 file:rounded-none file:border-0 file:text-sm file:font-medium file:bg-amber-500 file:text-industrial-dark hover:file:bg-amber-600"
              />
              {importFile && (
                <p className="text-gray-500 text-xs mt-2">
                  Selected: {importFile.name} ({(importFile.size / 1024).toFixed(1)} KB)
                </p>
              )}
              {importError && (
                <p className="text-red-400 text-xs mt-2">{importError}</p>
              )}
            </div>

            <div className="flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowImportDialog(false)
                  setImportFile(null)
                  setImportError(null)
                }}
                className="px-4 py-2 text-gray-400 hover:text-white transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleImport}
                disabled={!importFile}
                className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors font-bold disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Import
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default SceneListPage
