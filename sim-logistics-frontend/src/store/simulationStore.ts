/**
 * Simulation store using Zustand for simulation runtime state.
 *
 * Implements 5.4: Simulation Run Module
 * - Run control (start/pause/step/reset)
 * - Speed control (1x ~ 100x)
 * - Time display (simulation time + wall clock time)
 * - Refresh rate switching
 * - Entity status tracking
 * - KPI data selectors for dashboard integration
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-27
 */
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'

// Simulation run status
export type SimulationStatus = 'IDLE' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'ERROR'

// Entity runtime status for visualization
export type EntityRuntimeStatus = 'IDLE' | 'MOVING' | 'LOADING' | 'UNLOADING' | 'CHARGING' | 'BLOCKED' | 'ERROR'

// Refresh rate options in milliseconds
export type RefreshRate = 200 | 500 | 1000

// Entity runtime state
interface EntityRuntimeState {
  entityId: string
  status: EntityRuntimeStatus
  position?: { x: number; y: number; z: number }
  targetEntityId?: string
  batteryLevel?: number
  currentTaskId?: string
  lastUpdateTime: number
}

// Simulation metrics
interface SimulationMetrics {
  totalTasks: number
  completedTasks: number
  avgThroughput: number
  activeVehicles: number
  blockedVehicles: number
}

// KPI data snapshot for dashboard consumption
export interface SimulationKPISnapshot {
  // Throughput data points (last N intervals)
  throughputData: { timestamp: string; value: number }[]
  // Entity utilization data
  utilizationData: { entityId: string; entityName: string; entityType: string; utilization: number; status: 'idle' | 'active' | 'overloaded' }[]
  // Summary metrics
  summary: {
    totalThroughput: number
    avgUtilization: number
    peakUtilization: number
    activeEntities: number
    totalEntities: number
  }
  // Data source indicator
  isRealtime: boolean
  lastUpdated: string | null
}

/**
 * Simulation store state
 */
interface SimulationState {
  // Run control
  status: SimulationStatus
  speed: number // 1, 2, 5, 10, 20, 50, 100
  refreshRate: RefreshRate

  // Time tracking
  simulationTime: number // in seconds
  wallClockStartTime: number | null
  pausedAt: number | null
  totalPausedTime: number

  // Entity states
  entityStates: Map<string, EntityRuntimeState>

  // Metrics
  metrics: SimulationMetrics

  // Error handling
  error: string | null

  // Actions
  startSimulation: () => void
  pauseSimulation: () => void
  stepSimulation: () => void
  resetSimulation: () => void
  setSpeed: (speed: number) => void
  setRefreshRate: (rate: RefreshRate) => void

  // Entity state management
  updateEntityState: (entityId: string, state: Partial<EntityRuntimeState>) => void
  getEntityStatus: (entityId: string) => EntityRuntimeStatus

  // Metrics update
  updateMetrics: (metrics: Partial<SimulationMetrics>) => void

  // Internal
  tick: () => void
  clearError: () => void
}

// Valid speed multipliers
const VALID_SPEEDS = [1, 2, 5, 10, 20, 50, 100]

/**
 * Simulation store
 */
export const useSimulationStore = create<SimulationState>()(
  devtools(
    (set, get) => ({
      // Initial state
      status: 'IDLE',
      speed: 1,
      refreshRate: 500,
      simulationTime: 0,
      wallClockStartTime: null,
      pausedAt: null,
      totalPausedTime: 0,
      entityStates: new Map(),
      metrics: {
        totalTasks: 0,
        completedTasks: 0,
        avgThroughput: 0,
        activeVehicles: 0,
        blockedVehicles: 0,
      },
      error: null,

      // Start simulation
      startSimulation: () => {
        const { status, pausedAt, totalPausedTime } = get()

        if (status === 'IDLE' || status === 'COMPLETED' || status === 'ERROR') {
          // Fresh start
          set({
            status: 'RUNNING',
            wallClockStartTime: Date.now(),
            pausedAt: null,
            totalPausedTime: 0,
            simulationTime: 0,
            error: null,
          })
        } else if (status === 'PAUSED' && pausedAt !== null) {
          // Resume from pause
          const pauseDuration = Date.now() - pausedAt
          set({
            status: 'RUNNING',
            pausedAt: null,
            totalPausedTime: totalPausedTime + pauseDuration,
          })
        }
      },

      // Pause simulation
      pauseSimulation: () => {
        const { status } = get()
        if (status === 'RUNNING') {
          set({
            status: 'PAUSED',
            pausedAt: Date.now(),
          })
        }
      },

      // Step simulation (single step forward)
      stepSimulation: () => {
        const { status, simulationTime } = get()
        if (status === 'IDLE' || status === 'PAUSED' || status === 'COMPLETED') {
          // Advance simulation by 1 second
          set({ simulationTime: simulationTime + 1 })
        }
      },

      // Reset simulation
      resetSimulation: () => {
        set({
          status: 'IDLE',
          simulationTime: 0,
          wallClockStartTime: null,
          pausedAt: null,
          totalPausedTime: 0,
          entityStates: new Map(),
          metrics: {
            totalTasks: 0,
            completedTasks: 0,
            avgThroughput: 0,
            activeVehicles: 0,
            blockedVehicles: 0,
          },
          error: null,
        })
      },

      // Set simulation speed
      setSpeed: (speed) => {
        if (VALID_SPEEDS.includes(speed)) {
          set({ speed })
        }
      },

      // Set refresh rate
      setRefreshRate: (rate) => {
        set({ refreshRate: rate })
      },

      // Update entity runtime state
      updateEntityState: (entityId, state) => {
        const { entityStates } = get()
        const newStates = new Map(entityStates)
        const existing = newStates.get(entityId) || {
          entityId,
          status: 'IDLE' as EntityRuntimeStatus,
          lastUpdateTime: Date.now(),
        }

        newStates.set(entityId, {
          ...existing,
          entityId,
          lastUpdateTime: Date.now(),
          ...state,
        })

        set({ entityStates: newStates })
      },

      // Get entity status
      getEntityStatus: (entityId) => {
        const { entityStates } = get()
        return entityStates.get(entityId)?.status ?? 'IDLE'
      },

      // Update metrics
      updateMetrics: (metrics) => {
        set((state) => ({
          metrics: { ...state.metrics, ...metrics },
        }))
      },

      // Internal tick (called by simulation loop)
      tick: () => {
        const { status, speed, wallClockStartTime, totalPausedTime } = get()

        if (status !== 'RUNNING' || wallClockStartTime === null) return

        // Calculate elapsed time with speed multiplier
        const now = Date.now()
        const elapsedMs = now - wallClockStartTime - totalPausedTime
        const simulatedSeconds = (elapsedMs / 1000) * speed

        set({ simulationTime: Math.floor(simulatedSeconds) })
      },

      // Clear error
      clearError: () => {
        set({ error: null })
      },
    }),
    { name: 'SimulationStore' }
  )
)

/**
 * Format simulation time to HH:MM:SS
 */
export const formatSimulationTime = (seconds: number): string => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

/**
 * Format wall clock time to HH:MM:SS
 */
export const formatWallClockTime = (date: Date): string => {
  return date.toLocaleTimeString('en-US', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

/**
 * Get status color for entity visualization
 */
export const getStatusColor = (status: EntityRuntimeStatus): string => {
  switch (status) {
    case 'IDLE': return '#6b7280' // gray
    case 'MOVING': return '#3b82f6' // blue
    case 'LOADING': return '#f59e0b' // amber
    case 'UNLOADING': return '#10b981' // emerald
    case 'CHARGING': return '#8b5cf6' // purple
    case 'BLOCKED': return '#ef4444' // red
    case 'ERROR': return '#dc2626' // dark red
    default: return '#6b7280'
  }
}

/**
 * KPI data selector for dashboard integration.
 * Returns a snapshot of simulation metrics formatted for KPI dashboard consumption.
 *
 * @param entityInfo - Optional entity info map (entityId -> { name, type }) for utilization data
 * @returns SimulationKPISnapshot or null if no active simulation
 */
export const getSimulationKPISnapshot = (
  entityInfo?: Map<string, { name: string; type: string }>
): SimulationKPISnapshot | null => {
  const state = useSimulationStore.getState()
  const { status, metrics, entityStates } = state

  // Return null if no active simulation session
  if (status === 'IDLE') {
    return null
  }

  // Calculate utilization data from entity states
  const utilizationData: SimulationKPISnapshot['utilizationData'] = []
  let totalUtilization = 0
  let activeCount = 0
  let peakUtilization = 0

  entityStates.forEach((entityState, entityId) => {
    const info = entityInfo?.get(entityId) || { name: entityId, type: 'UNKNOWN' }

    // Calculate utilization based on status (simplified model)
    let utilization = 0
    let statusLabel: 'idle' | 'active' | 'overloaded' = 'idle'

    switch (entityState.status) {
      case 'MOVING':
      case 'LOADING':
      case 'UNLOADING':
        utilization = 85
        statusLabel = 'active'
        activeCount++
        break
      case 'CHARGING':
        utilization = 30
        statusLabel = 'idle'
        break
      case 'BLOCKED':
      case 'ERROR':
        utilization = 100
        statusLabel = 'overloaded'
        activeCount++
        break
      default:
        utilization = 10
        statusLabel = 'idle'
    }

    utilizationData.push({
      entityId,
      entityName: info.name,
      entityType: info.type,
      utilization,
      status: statusLabel,
    })

    totalUtilization += utilization
    peakUtilization = Math.max(peakUtilization, utilization)
  })

  const totalEntities = entityStates.size
  const avgUtilization = totalEntities > 0 ? totalUtilization / totalEntities : 0

  // Generate throughput data (simplified: use current throughput as latest point)
  const now = new Date()
  const timestamp = now.toLocaleTimeString('en-US', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })

  const throughputData = [
    { timestamp, value: metrics.avgThroughput }
  ]

  return {
    throughputData,
    utilizationData,
    summary: {
      totalThroughput: metrics.completedTasks,
      avgUtilization: Math.round(avgUtilization * 10) / 10,
      peakUtilization: Math.round(peakUtilization),
      activeEntities: activeCount,
      totalEntities,
    },
    isRealtime: status === 'RUNNING',
    lastUpdated: now.toISOString(),
  }
}

/**
 * Check if simulation has active session for KPI data source
 */
export const hasActiveSimulation = (): boolean => {
  const { status } = useSimulationStore.getState()
  return status !== 'IDLE'
}

export default useSimulationStore
