import { create } from 'zustand'

export type DiagEventType = 
  | 'MODELS_LOADED'
  | 'SCENE_ENTITIES_SNAPSHOT'
  | 'PRELOAD_MODEL_VERSIONS_START'
  | 'PRELOAD_MODEL_VERSIONS_DONE'
  | 'PRELOAD_MODEL_VERSIONS_FAILED'
  | 'ENTITY_RESOLVED'
  | 'ENTITY_RESOLVE_FAILED'
  | 'ENTITY_MESH_MOUNTED'
  | 'ENTITY_MESH_NO_URL'
  | 'GLTF_LOAD_START'
  | 'GLTF_LOAD_SUCCESS'
  | 'GLTF_LOAD_FAILED'
  | 'MODEL_TRANSFORM_APPLIED'
  | 'API_WARNING'

export interface DiagEvent {
  id: string
  at: string // ISO timestamp
  type: DiagEventType
  sceneId?: string
  entityId?: string
  modelId?: string
  versionId?: string
  url?: string
  details?: Record<string, unknown>
}

interface DiagStore {
  events: DiagEvent[]
  isOpen: boolean
  
  pushDiag: (type: DiagEventType, payload: Omit<DiagEvent, 'id' | 'at' | 'type'>) => void
  toggleOpen: () => void
  clear: () => void
}

export const useGlbDiagStore = create<DiagStore>((set) => ({
  events: [],
  isOpen: import.meta.env.DEV, // Default open in DEV
  
  pushDiag: (type, payload) => {
    // Also log to console for redundancy
    const level = type.includes('FAILED') ? 'error' : 'info'
    console[level](`[GLB-DIAG] ${type}`, payload)

    set((state) => {
      const newEvent: DiagEvent = {
        id: crypto.randomUUID(),
        at: new Date().toISOString(),
        type,
        ...payload
      }
      
      // Keep max 200 events
      const newEvents = [newEvent, ...state.events].slice(0, 200)
      return { events: newEvents }
    })
  },
  
  toggleOpen: () => set((state) => ({ isOpen: !state.isOpen })),
  
  clear: () => set({ events: [] })
}))
