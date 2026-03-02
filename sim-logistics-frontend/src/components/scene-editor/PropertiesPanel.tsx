import React from 'react'
import type { Entity, TransportType, PathSegment } from '@/types/api'
import { t } from '@/shared/i18n'

interface PropertiesPanelProps {
  selectedEntity: Entity | null
  selectedSegment: { pathId: string; segment: PathSegment } | null
  onUpdateEntity: (entity: Entity) => void
  onDeleteEntity: (id: string) => void
  onDeleteSegment: (pathId: string, segmentId: string) => void
}

const TRANSPORT_TYPES: TransportType[] = ['OHT', 'AGV', 'HUMAN', 'CONVEYOR']
const TRANSPORT_CAPABLE_TYPES = new Set(['STOCKER', 'ERACK', 'MANUAL_STATION', 'MACHINE', 'BAY', 'CHUTE'])

const clampRad = (value: number): number => {
  const min = -Math.PI
  const max = Math.PI
  if (value < min) return min
  if (value > max) return max
  return value
}

const asNumber = (value: string, fallback = 0): number => {
  const parsed = Number.parseFloat(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

export const PropertiesPanel: React.FC<PropertiesPanelProps> = ({
  selectedEntity,
  selectedSegment,
  onUpdateEntity,
  onDeleteEntity,
  onDeleteSegment,
}) => {
  if (selectedSegment) {
    return (
      <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-4 space-y-4">
        <div>
          <h3 className="text-white text-sm font-semibold">{t('sceneEditor.properties.title')}</h3>
          <p className="text-xs text-gray-500 mt-1">{selectedSegment.segment.id}</p>
        </div>

        <div>
          <label className="block text-xs text-gray-400 mb-1">{t('sceneEditor.properties.type')}</label>
          <input
            type="text"
            disabled
            value={selectedSegment.segment.type}
            className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded text-sm text-gray-400"
          />
        </div>

        <button
          onClick={() => onDeleteSegment(selectedSegment.pathId, selectedSegment.segment.id)}
          className="w-full mt-4 px-3 py-2 bg-red-500/10 hover:bg-red-500/20 text-red-500 rounded border border-red-500/30 text-sm transition-colors"
        >
          {t('sceneEditor.properties.deleteSegment')}
        </button>
      </div>
    )
  }

  if (!selectedEntity) {
    return (
      <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-4">
        <h3 className="text-white text-sm font-semibold mb-2">{t('sceneEditor.properties.title')}</h3>
        <p className="text-xs text-gray-500">{t('sceneEditor.properties.empty')}</p>
      </div>
    )
  }

  const supportsTransport = TRANSPORT_CAPABLE_TYPES.has(selectedEntity.type)
  const zoneShape = String(selectedEntity.properties?.shape ?? 'RECTANGLE')
  const zoneWidth = Number(selectedEntity.properties?.width ?? 6)
  const zoneHeight = Number(selectedEntity.properties?.height ?? 4)
  const zoneRadius = Number(selectedEntity.properties?.radius ?? 4)

  return (
    <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-4 space-y-4">
      <div>
        <h3 className="text-white text-sm font-semibold">{t('sceneEditor.properties.title')}</h3>
        <p className="text-xs text-gray-500 mt-1">{selectedEntity.id}</p>
      </div>

      <div>
        <label className="block text-xs text-gray-400 mb-1">{t('sceneEditor.properties.name')}</label>
        <input
          type="text"
          value={selectedEntity.name ?? ''}
          onChange={(e) => onUpdateEntity({ ...selectedEntity, name: e.target.value })}
          className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded text-sm text-white"
        />
      </div>

      <div>
        <label className="block text-xs text-gray-400 mb-1">{t('sceneEditor.properties.type')}</label>
        <input
          type="text"
          disabled
          value={selectedEntity.type}
          className="w-full px-3 py-2 bg-industrial-dark border border-industrial-steel/30 rounded text-sm text-gray-400"
        />
      </div>

      <div className="grid grid-cols-2 gap-2">
        <div>
          <label className="block text-xs text-gray-400 mb-1">{t('sceneEditor.properties.positionX')}</label>
          <input
            type="number"
            step="0.1"
            value={selectedEntity.position.x}
            onChange={(e) => onUpdateEntity({
              ...selectedEntity,
              position: { ...selectedEntity.position, x: asNumber(e.target.value, selectedEntity.position.x) },
            })}
            className="w-full px-2 py-1 bg-industrial-dark border border-industrial-steel/30 rounded text-sm text-white"
          />
        </div>
        <div>
          <label className="block text-xs text-gray-400 mb-1">{t('sceneEditor.properties.positionZ')}</label>
          <input
            type="number"
            step="0.1"
            value={selectedEntity.position.z}
            onChange={(e) => onUpdateEntity({
              ...selectedEntity,
              position: { ...selectedEntity.position, z: asNumber(e.target.value, selectedEntity.position.z) },
            })}
            className="w-full px-2 py-1 bg-industrial-dark border border-industrial-steel/30 rounded text-sm text-white"
          />
        </div>
        <div className="col-span-2">
          <label className="block text-xs text-gray-400 mb-1">{t('sceneEditor.properties.rotationY')}</label>
          <input
            type="number"
            step="0.1"
            min={-Math.PI}
            max={Math.PI}
            value={selectedEntity.rotation?.y ?? 0}
            onBlur={(e) => {
              // Clamp on blur to ensure value is within range
              const val = asNumber(e.target.value, selectedEntity.rotation?.y ?? 0)
              const clamped = clampRad(val)
              if (val !== clamped) {
                onUpdateEntity({
                  ...selectedEntity,
                  rotation: {
                    ...(selectedEntity.rotation ?? { x: 0, y: 0, z: 0 }),
                    y: clamped,
                  },
                })
              }
            }}
            onChange={(e) => onUpdateEntity({
              ...selectedEntity,
              rotation: {
                ...(selectedEntity.rotation ?? { x: 0, y: 0, z: 0 }),
                y: clampRad(asNumber(e.target.value, selectedEntity.rotation?.y ?? 0)),
              },
            })}
            className="w-full px-2 py-1 bg-industrial-dark border border-industrial-steel/30 rounded text-sm text-white"
          />
        </div>
      </div>

      {supportsTransport ? (
        <div>
          <label className="block text-xs text-gray-400 mb-2">{t('sceneEditor.properties.transportTypes')}</label>
          <div className="flex flex-wrap gap-2">
            {TRANSPORT_TYPES.map((item) => {
              const current = selectedEntity.supportedTransportTypes ?? []
              const checked = current.includes(item)
              return (
                <button
                  key={item}
                  type="button"
                  onClick={() => {
                    const next = checked
                      ? current.filter((x) => x !== item)
                      : [...current, item]
                    onUpdateEntity({ ...selectedEntity, supportedTransportTypes: next })
                  }}
                  className={`px-2 py-1 rounded text-xs transition-colors ${checked ? 'bg-emerald-600 text-white' : 'bg-industrial-dark text-gray-300'}`}
                >
                  {item}
                </button>
              )
            })}
          </div>
        </div>
      ) : null}

      {selectedEntity.type === 'SAFETY_ZONE' ? (
        <div className="space-y-2">
          <label className="block text-xs text-gray-400">{t('sceneEditor.properties.zoneShape')}</label>
          <select
            value={zoneShape}
            onChange={(e) => onUpdateEntity({
              ...selectedEntity,
              properties: {
                ...(selectedEntity.properties ?? {}),
                shape: e.target.value,
              },
            })}
            className="w-full px-2 py-1 bg-industrial-dark border border-industrial-steel/30 rounded text-sm text-white"
          >
            <option value="RECTANGLE">RECTANGLE</option>
            <option value="CIRCLE">CIRCLE</option>
            <option value="POLYGON">POLYGON</option>
          </select>

          {zoneShape === 'CIRCLE' ? (
            <div>
              <label className="block text-xs text-gray-400 mb-1">{t('sceneEditor.properties.zoneRadius')}</label>
              <input
                type="number"
                step="0.5"
                value={zoneRadius}
                onChange={(e) => onUpdateEntity({
                  ...selectedEntity,
                  properties: {
                    ...(selectedEntity.properties ?? {}),
                    radius: asNumber(e.target.value, zoneRadius),
                  },
                })}
                className="w-full px-2 py-1 bg-industrial-dark border border-industrial-steel/30 rounded text-sm text-white"
              />
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs text-gray-400 mb-1">{t('sceneEditor.properties.zoneWidth')}</label>
                <input
                  type="number"
                  step="0.5"
                  value={zoneWidth}
                  onChange={(e) => onUpdateEntity({
                    ...selectedEntity,
                    properties: {
                      ...(selectedEntity.properties ?? {}),
                      width: asNumber(e.target.value, zoneWidth),
                    },
                  })}
                  className="w-full px-2 py-1 bg-industrial-dark border border-industrial-steel/30 rounded text-sm text-white"
                />
              </div>
              <div>
                <label className="block text-xs text-gray-400 mb-1">{t('sceneEditor.properties.zoneHeight')}</label>
                <input
                  type="number"
                  step="0.5"
                  value={zoneHeight}
                  onChange={(e) => onUpdateEntity({
                    ...selectedEntity,
                    properties: {
                      ...(selectedEntity.properties ?? {}),
                      height: asNumber(e.target.value, zoneHeight),
                    },
                  })}
                  className="w-full px-2 py-1 bg-industrial-dark border border-industrial-steel/30 rounded text-sm text-white"
                />
              </div>
            </div>
          )}
        </div>
      ) : null}

      <button
        type="button"
        onClick={() => onDeleteEntity(selectedEntity.id)}
        className="w-full px-3 py-2 bg-red-600/20 hover:bg-red-600/30 text-red-300 rounded text-sm"
      >
        {t('sceneEditor.properties.delete')}
      </button>
    </div>
  )
}

export default PropertiesPanel
