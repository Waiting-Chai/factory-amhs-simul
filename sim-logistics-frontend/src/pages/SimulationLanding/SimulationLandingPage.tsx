/**
 * Simulation Landing Page - Scene selection for simulation run.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-27
 */
import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useSceneStore } from '@store/sceneStore'
import { t } from '@/shared/i18n'

/**
 * Simulation Landing Page - select a scene to run simulation
 */
const SimulationLandingPage: React.FC = () => {
  const navigate = useNavigate()
  const { scenes, fetchScenes, isLoading, error } = useSceneStore()
  const [searchTerm, setSearchTerm] = useState('')

  useEffect(() => {
    fetchScenes(1)
  }, [fetchScenes])

  const filteredScenes = scenes.filter(scene =>
    scene.name.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const handleSceneSelect = (sceneId: string) => {
    navigate(`/simulation/${sceneId}`)
  }

  return (
    <div className="min-h-screen bg-[#0a0e14] text-slate-200 p-6">
      <header className="mb-6">
        <div className="flex items-center gap-4 mb-4">
          <Link
            to="/scenes"
            className="text-slate-500 hover:text-slate-300 transition-colors font-mono text-sm"
          >
            &larr; {t('simulation.landing.backToScenes')}
          </Link>
        </div>
        <h1 className="text-2xl font-mono font-bold tracking-tight text-slate-100">
          {t('simulation.landing.title')}
        </h1>
        <p className="text-sm font-mono text-slate-500 mt-1">
          {t('simulation.landing.subtitle')}
        </p>
      </header>

      {/* Search */}
      <div className="mb-6">
        <input
          type="text"
          placeholder={t('simulation.landing.searchPlaceholder')}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full max-w-md px-4 py-2 bg-slate-900 border border-slate-700
            text-slate-200 font-mono text-sm focus:outline-none focus:border-[#00d4ff]/50"
        />
      </div>

      {/* Scene list */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="flex items-center gap-3 text-slate-500 font-mono">
            <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            <span>{t('common.loading')}</span>
          </div>
        </div>
      ) : error ? (
        <div className="flex flex-col items-center justify-center py-12 gap-4">
          <div className="text-red-400 font-mono text-center">
            <svg className="w-12 h-12 mx-auto mb-3 text-red-400/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <p className="text-sm">{t('common.error')}</p>
            <p className="text-xs text-slate-500 mt-1">{error}</p>
          </div>
          <button
            onClick={() => fetchScenes(1)}
            className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 font-mono text-sm border border-slate-700 transition-colors"
          >
            {t('common.retry')}
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredScenes.map((scene) => (
            <div
              key={scene.sceneId}
              onClick={() => handleSceneSelect(scene.sceneId)}
              onKeyDown={(e) => e.key === 'Enter' && handleSceneSelect(scene.sceneId)}
              role="button"
              tabIndex={0}
              className="p-4 bg-slate-900/50 border border-slate-700 hover:border-[#00d4ff]/50
                cursor-pointer transition-colors"
            >
              <div className="flex justify-between items-start mb-2">
                <h3 className="font-mono font-bold text-slate-200">{scene.name}</h3>
                <span className="text-xs font-mono text-slate-500">v{scene.version}</span>
              </div>
              <p className="text-xs font-mono text-slate-500 mb-3 line-clamp-2">
                {scene.description || t('simulation.landing.noDescription')}
              </p>
              <div className="flex justify-between items-center">
                <span className="text-xs font-mono text-slate-600">
                  {t('simulation.landing.entities')}: {scene.entityCount || 0}
                </span>
                <span className="text-xs font-mono text-[#00d4ff]">
                  {t('simulation.landing.go')} &rarr;
                </span>
              </div>
            </div>
          ))}
          {filteredScenes.length === 0 && (
            <div className="col-span-full text-center py-12 text-slate-500 font-mono">
              {t('simulation.landing.noScenes')}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default SimulationLandingPage
