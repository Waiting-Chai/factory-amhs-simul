import React, { useMemo } from 'react'
import type { EntityType } from '@/types/api'
import { t } from '@/shared/i18n'
import { useModelStore } from '@/store/modelStore'
import { ModelPreview } from '@components/model/ModelPreview'
import { resolveModelAssetUrl } from '@/utils/modelAssetUrl'

interface PalettePanelProps {
  selectedType: EntityType | null
  onSelectType: (type: EntityType) => void
}

// Category definition with icon and i18n key
interface CategoryDef {
  key: string
  labelKey: string
  types: EntityType[]
  icon: React.ReactNode
}

// SVG icons for each category
const VehicleIcon = () => (
  <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <rect x="3" y="8" width="18" height="8" rx="2" />
    <circle cx="7" cy="18" r="2" />
    <circle cx="17" cy="18" r="2" />
  </svg>
)

const EquipmentIcon = () => (
  <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <rect x="4" y="4" width="16" height="16" rx="2" />
    <line x1="9" y1="9" x2="15" y2="9" />
    <line x1="9" y1="12" x2="15" y2="12" />
    <line x1="9" y1="15" x2="12" y2="15" />
  </svg>
)

const TopologyIcon = () => (
  <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <circle cx="6" cy="12" r="3" />
    <circle cx="18" cy="12" r="3" />
    <line x1="9" y1="12" x2="15" y2="12" />
  </svg>
)

const SafetyIcon = () => (
  <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M12 2L4 6v6c0 5.5 3.5 10 8 11.5 4.5-1.5 8-6 8-11.5V6l-8-4z" />
  </svg>
)

// Category definitions
const CATEGORIES: CategoryDef[] = [
  {
    key: 'vehicles',
    labelKey: 'sceneEditor.palette.category.vehicles',
    types: ['OHT_VEHICLE', 'AGV_VEHICLE'],
    icon: <VehicleIcon />,
  },
  {
    key: 'equipment',
    labelKey: 'sceneEditor.palette.category.equipment',
    types: ['MACHINE', 'STOCKER', 'CONVEYOR'],
    icon: <EquipmentIcon />,
  },
  {
    key: 'topology',
    labelKey: 'sceneEditor.palette.category.topology',
    types: ['CONTROL_POINT'],
    icon: <TopologyIcon />,
  },
  {
    key: 'safety',
    labelKey: 'sceneEditor.palette.category.safety',
    types: ['SAFETY_ZONE'],
    icon: <SafetyIcon />,
  },
]

interface PaletteItemViewModel {
  type: EntityType
  displayName: string
  previewUrl: string | null
  fileUrl: string | null
  hasBoundModel: boolean
  fallbackKind: 'box' | 'sphere' | 'cylinder' | 'rect'
  statusLabelKey: 'sceneEditor.palette.modelBound' | 'sceneEditor.palette.modelFallback'
  fallbackColor: string
}

const getEntityLabel = (type: EntityType): string => {
  const keyMap: Partial<Record<EntityType, string>> = {
    OHT_VEHICLE: 'sceneEditor.palette.ohtVehicle',
    AGV_VEHICLE: 'sceneEditor.palette.agvVehicle',
    MACHINE: 'sceneEditor.palette.machine',
    STOCKER: 'sceneEditor.palette.stocker',
    CONTROL_POINT: 'sceneEditor.palette.node',
    CONVEYOR: 'sceneEditor.palette.conveyor',
    SAFETY_ZONE: 'sceneEditor.palette.safetyZone',
  }
  const key = keyMap[type]
  return key ? t(key as never) : type
}

const getFallbackConfig = (type: EntityType): { kind: PaletteItemViewModel['fallbackKind']; color: string } => {
  switch (type) {
    case 'OHT_VEHICLE':
    case 'AGV_VEHICLE':
      return { kind: 'box', color: '#f59e0b' } // Amber-500
    case 'CONTROL_POINT':
      return { kind: 'sphere', color: '#3b82f6' } // Blue-500
    case 'SAFETY_ZONE':
      return { kind: 'rect', color: '#ef4444' } // Red-500
    case 'MACHINE':
    case 'STOCKER':
      return { kind: 'box', color: '#10b981' } // Emerald-500
    case 'CONVEYOR':
      return { kind: 'cylinder', color: '#6366f1' } // Indigo-500
    default:
      return { kind: 'box', color: '#9ca3af' } // Gray-400
  }
}

export const PalettePanel: React.FC<PalettePanelProps> = ({ selectedType, onSelectType }) => {
  const { models, modelVersionsByModelId } = useModelStore()

  const itemsByCategory = useMemo(() => {
    const result = new Map<string, PaletteItemViewModel[]>()

    CATEGORIES.forEach(category => {
      const categoryItems: PaletteItemViewModel[] = category.types.map(type => {
        // Find default or first available model for this type
        const model = models.find(m => m.type === type && m.status === 'ACTIVE')
        const fallback = getFallbackConfig(type)

        // Resolve fileUrl from versions if available
        let fileUrl: string | null = null
        if (model) {
          const versions = modelVersionsByModelId[model.modelId]
          if (versions) {
            const defaultVer = model.defaultVersion
              ? versions.find(v => v.versionId === model.defaultVersion || v.version === model.defaultVersion)
              : null

            const targetVer = defaultVer || versions.find(v => v.fileUrl && v.status === 'ACTIVE') || versions[0]

            if (targetVer?.fileUrl) {
              fileUrl = resolveModelAssetUrl(targetVer.fileUrl)
            }
          }
        }

        return {
          type,
          displayName: getEntityLabel(type),
          previewUrl: model?.thumbnailUrl ? resolveModelAssetUrl(model.thumbnailUrl) : null,
          fileUrl,
          hasBoundModel: !!model,
          fallbackKind: fallback.kind,
          fallbackColor: fallback.color,
          statusLabelKey: model ? 'sceneEditor.palette.modelBound' : 'sceneEditor.palette.modelFallback'
        }
      })

      result.set(category.key, categoryItems)
    })

    return result
  }, [models, modelVersionsByModelId])

  return (
    <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-4 h-full flex flex-col">
      <h3 className="text-white text-sm font-semibold mb-3">{t('sceneEditor.palette.title')}</h3>
      <p className="text-gray-500 text-xs mb-3">{t('sceneEditor.palette.hint')}</p>
      <div className="space-y-4 overflow-y-auto flex-1 pr-1">
        {CATEGORIES.map((category) => {
          const items = itemsByCategory.get(category.key) || []
          return (
            <div key={category.key} className="space-y-2">
              {/* Category Header */}
              <div className="flex items-center gap-2 text-gray-400 text-xs font-medium uppercase tracking-wider">
                <span className="text-amber-500">{category.icon}</span>
                <span>{t(category.labelKey as never)}</span>
              </div>

              {/* Category Items */}
              <div className="space-y-2">
                {items.map((item) => (
                  <button
                    key={item.type}
                    type="button"
                    onClick={() => onSelectType(item.type)}
                    className={`w-full text-left p-2 rounded transition-all border ${
                      selectedType === item.type
                        ? 'bg-industrial-dark border-amber-500 ring-1 ring-amber-500'
                        : 'bg-industrial-dark border-industrial-steel/30 hover:border-gray-500'
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      {/* Preview Area */}
                      <div className="w-12 h-12 rounded bg-black/20 flex-shrink-0 flex items-center justify-center overflow-hidden border border-white/5 relative">
                        {(item.previewUrl || item.fileUrl) ? (
                          <div className="absolute inset-0">
                            <ModelPreview
                              modelUrl={item.previewUrl || item.fileUrl || undefined}
                              name={item.displayName}
                              className="w-full h-full"
                            />
                          </div>
                        ) : (
                          /* Fallback Visualization */
                          <div className="w-full h-full flex items-center justify-center">
                            <div
                              className="w-6 h-6 rounded-sm shadow-sm"
                              style={{
                                backgroundColor: item.fallbackColor,
                                borderRadius: item.fallbackKind === 'sphere' ? '50%' : '2px'
                              }}
                            />
                          </div>
                        )}
                      </div>

                      {/* Info Area */}
                      <div className="flex-1 min-w-0 py-0.5">
                        <div className={`font-medium text-xs truncate ${selectedType === item.type ? 'text-amber-500' : 'text-gray-200'}`}>
                          {item.displayName}
                        </div>
                        <div className="flex items-center gap-1.5 mt-1">
                          <span className={`w-1.5 h-1.5 rounded-full ${item.hasBoundModel ? 'bg-emerald-500' : 'bg-gray-500'}`} />
                          <span className="text-[10px] text-gray-400 truncate">
                            {t(item.statusLabelKey)}
                          </span>
                        </div>
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default PalettePanel
