import React from 'react'
import { t } from '@/shared/i18n'

export type EditorMode = 'select' | 'place' | 'connect' | 'area'
export type PathSegmentMode = 'LINEAR' | 'BEZIER'
export type TransformMode = 'translate' | 'rotate' | 'scale'

interface EditorToolbarProps {
  mode: EditorMode
  onModeChange: (mode: EditorMode) => void
  transformMode: TransformMode
  onTransformModeChange: (mode: TransformMode) => void
  snapEnabled: boolean
  onSnapEnabledChange: (enabled: boolean) => void
  showGrid: boolean
  onShowGridChange: (visible: boolean) => void
  pathSegmentType: PathSegmentMode
  onPathSegmentTypeChange: (type: PathSegmentMode) => void
  activePathType: 'OHT_PATH' | 'AGV_NETWORK'
  onActivePathTypeChange: (type: 'OHT_PATH' | 'AGV_NETWORK') => void
}

export const EditorToolbar: React.FC<EditorToolbarProps> = ({
  mode,
  onModeChange,
  transformMode,
  onTransformModeChange,
  snapEnabled,
  onSnapEnabledChange,
  showGrid,
  onShowGridChange,
  pathSegmentType,
  onPathSegmentTypeChange,
  activePathType,
  onActivePathTypeChange,
}) => {
  return (
    <div className="absolute top-4 left-1/2 transform -translate-x-1/2 bg-industrial-slate/90 backdrop-blur border border-industrial-steel/30 rounded-lg p-2 z-10 shadow-lg">
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-1 border-r border-gray-600 pr-2 mr-2">
          <button
            type="button"
            onClick={() => onModeChange('select')}
            className={`px-3 py-1.5 rounded text-sm transition-colors ${mode === 'select' ? 'bg-amber-500 text-industrial-dark font-bold' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
          >
            {t('sceneEditor.mode.select')}
          </button>
          <button
            type="button"
            onClick={() => onModeChange('place')}
            className={`px-3 py-1.5 rounded text-sm transition-colors ${mode === 'place' ? 'bg-amber-500 text-industrial-dark font-bold' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
          >
            {t('sceneEditor.mode.place')}
          </button>
          <button
            type="button"
            onClick={() => onModeChange('connect')}
            className={`px-3 py-1.5 rounded text-sm transition-colors ${mode === 'connect' ? 'bg-amber-500 text-industrial-dark font-bold' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
          >
            {t('sceneEditor.mode.connect')}
          </button>
          <button
            type="button"
            onClick={() => onModeChange('area')}
            className={`px-3 py-1.5 rounded text-sm transition-colors ${mode === 'area' ? 'bg-amber-500 text-industrial-dark font-bold' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
          >
            {t('sceneEditor.mode.area')}
          </button>
        </div>

        {mode === 'select' && (
          <div className="flex items-center gap-1 border-r border-gray-600 pr-2 mr-2">
             <button
              type="button"
              onClick={() => onTransformModeChange('translate')}
              className={`px-2 py-1.5 rounded text-sm transition-colors ${transformMode === 'translate' ? 'bg-blue-500 text-white font-bold' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
              title={t('toolbar.transform.moveShortcut')}
            >
              {t('toolbar.transform.move')}
            </button>
            <button
              type="button"
              onClick={() => onTransformModeChange('rotate')}
              className={`px-2 py-1.5 rounded text-sm transition-colors ${transformMode === 'rotate' ? 'bg-blue-500 text-white font-bold' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
              title={t('toolbar.transform.rotateShortcut')}
            >
              {t('toolbar.transform.rotate')}
            </button>
            <button
              type="button"
              onClick={() => onTransformModeChange('scale')}
              className={`px-2 py-1.5 rounded text-sm transition-colors ${transformMode === 'scale' ? 'bg-blue-500 text-white font-bold' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
              title={t('toolbar.transform.scaleShortcut')}
            >
              {t('toolbar.transform.scale')}
            </button>
          </div>
        )}

        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => onShowGridChange(!showGrid)}
            className={`px-3 py-1.5 rounded text-sm transition-colors ${showGrid ? 'bg-emerald-600 text-white' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
          >
            {t('sceneEditor.toolbar.grid')}
          </button>
          <button
            type="button"
            onClick={() => onSnapEnabledChange(!snapEnabled)}
            className={`px-3 py-1.5 rounded text-sm transition-colors ${snapEnabled ? 'bg-emerald-600 text-white' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
          >
            {t('sceneEditor.toolbar.snap')}
          </button>
        </div>

        {mode === 'connect' && (
          <div className="flex items-center gap-2 border-l border-gray-600 pl-2 ml-2">
            <span className="text-xs text-gray-400">{t('sceneEditor.toolbar.pathContext')}</span>
            <button
              type="button"
              onClick={() => onActivePathTypeChange('OHT_PATH')}
              className={`px-2 py-1 rounded text-xs transition-colors ${activePathType === 'OHT_PATH' ? 'bg-amber-500 text-industrial-dark font-bold' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
            >
              {t('sceneEditor.toolbar.pathContext.oht')}
            </button>
            <button
              type="button"
              onClick={() => onActivePathTypeChange('AGV_NETWORK')}
              className={`px-2 py-1 rounded text-xs transition-colors ${activePathType === 'AGV_NETWORK' ? 'bg-amber-500 text-industrial-dark font-bold' : 'bg-industrial-dark text-gray-300 hover:text-white'}`}
            >
              {t('sceneEditor.toolbar.pathContext.agv')}
            </button>
          </div>
        )}

        {mode === 'connect' && (
          <div className="flex items-center gap-2 border-l border-gray-600 pl-2 ml-2">
            <span className="text-xs text-gray-400">{t('sceneEditor.toolbar.segmentType')}</span>
            <button
              type="button"
              onClick={() => onPathSegmentTypeChange('LINEAR')}
              className={`px-2 py-1 rounded text-xs transition-colors ${pathSegmentType === 'LINEAR' ? 'bg-amber-500 text-industrial-dark font-bold' : 'bg-industrial-dark text-gray-300'}`}
            >
              LINEAR
            </button>
            <button
              type="button"
              onClick={() => onPathSegmentTypeChange('BEZIER')}
              className={`px-2 py-1 rounded text-xs transition-colors ${pathSegmentType === 'BEZIER' ? 'bg-amber-500 text-industrial-dark font-bold' : 'bg-industrial-dark text-gray-300'}`}
            >
              BEZIER
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default EditorToolbar
