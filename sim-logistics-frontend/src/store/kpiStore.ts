/**
 * KPI store using Zustand for KPI dashboard state management.
 *
 * Implements 5.5: KPI Dashboard Module
 * - Throughput metrics (line chart data)
 * - Utilization metrics (bar chart data)
 * - Hot spot placeholder data
 * - Data source mode (simulation realtime vs mock fallback)
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-27
 */
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import { getSimulationKPISnapshot, hasActiveSimulation } from './simulationStore'
import { useTaskStore } from './taskStore'

// Throughput data point for line chart
export interface ThroughputDataPoint {
  timestamp: string // ISO timestamp or formatted time string
  value: number // tasks completed per interval
}

// Entity utilization data for bar chart
export interface EntityUtilization {
  entityId: string
  entityName: string
  entityType: string
  utilization: number // percentage 0-100
  status: 'idle' | 'active' | 'overloaded'
}

// Hot spot data placeholder (for future heatmap implementation)
export interface HotSpotData {
  areaId: string
  areaName: string
  intensity: number // 0-100
  coordinates: { x: number; z: number }
}

// KPI summary metrics
export interface KPISummary {
  totalThroughput: number
  avgUtilization: number
  peakUtilization: number
  activeEntities: number
  totalEntities: number
}

// Data source type
export type KPIDataSource = 'simulation' | 'mock'

/**
 * KPI store state interface
 */
interface KPIState {
  // Throughput data (last N data points for line chart)
  throughputData: ThroughputDataPoint[]
  maxThroughputPoints: number

  // Entity utilization data (bar chart)
  utilizationData: EntityUtilization[]

  // Hot spot placeholder data
  hotSpotData: HotSpotData[]

  // Summary metrics
  summary: KPISummary

  // Data source indicator
  dataSource: KPIDataSource

  // Loading state
  isLoading: boolean
  error: string | null

  // Last update time
  lastUpdated: string | null

  // Actions
  addThroughputPoint: (point: ThroughputDataPoint) => void
  setUtilizationData: (data: EntityUtilization[]) => void
  setHotSpotData: (data: HotSpotData[]) => void
  updateSummary: (summary: Partial<KPISummary>) => void
  setLoading: (loading: boolean) => void
  setError: (error: string | null) => void
  refreshData: () => void
  reset: () => void

  // Data source actions
  updateFromSimulation: (entityInfo?: Map<string, { name: string; type: string }>) => void
  getDataSource: () => KPIDataSource
}

// Mock data generators for demo purposes
const generateMockThroughputData = (): ThroughputDataPoint[] => {
  const now = Date.now()
  const points: ThroughputDataPoint[] = []
  for (let i = 11; i >= 0; i--) {
    const timestamp = new Date(now - i * 5000).toLocaleTimeString('en-US', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
    points.push({
      timestamp,
      value: Math.floor(Math.random() * 50) + 10, // 10-60 tasks per interval
    })
  }
  return points
}

const generateMockUtilizationData = (): EntityUtilization[] => {
  const entities = [
    { id: 'oht-001', name: 'OHT-001', type: 'OHT_VEHICLE' },
    { id: 'oht-002', name: 'OHT-002', type: 'OHT_VEHICLE' },
    { id: 'agv-001', name: 'AGV-001', type: 'AGV_VEHICLE' },
    { id: 'agv-002', name: 'AGV-002', type: 'AGV_VEHICLE' },
    { id: 'machine-001', name: 'EQP-001', type: 'MACHINE' },
    { id: 'stocker-001', name: 'STK-001', type: 'STOCKER' },
  ]

  return entities.map((entity) => {
    const utilization = Math.floor(Math.random() * 100)
    return {
      entityId: entity.id,
      entityName: entity.name,
      entityType: entity.type,
      utilization,
      status: utilization < 30 ? 'idle' : utilization > 80 ? 'overloaded' : 'active',
    }
  })
}

const generateMockHotSpotData = (): HotSpotData[] => {
  return [
    { areaId: 'area-1', areaName: 'Loading Zone A', intensity: 85, coordinates: { x: 10, z: 20 } },
    { areaId: 'area-2', areaName: 'Transfer Point B', intensity: 72, coordinates: { x: 30, z: 15 } },
    { areaId: 'area-3', areaName: 'Queue Area C', intensity: 45, coordinates: { x: 50, z: 40 } },
    { areaId: 'area-4', areaName: 'Charging Station', intensity: 30, coordinates: { x: 20, z: 60 } },
  ]
}

/**
 * KPI store implementation
 */
export const useKPIStore = create<KPIState>()(
  devtools(
    (set, get) => ({
      // Initial state with mock data
      throughputData: generateMockThroughputData(),
      maxThroughputPoints: 20,
      utilizationData: generateMockUtilizationData(),
      hotSpotData: generateMockHotSpotData(),
      summary: {
        totalThroughput: 1250,
        avgUtilization: 67.5,
        peakUtilization: 92,
        activeEntities: 5,
        totalEntities: 6,
      },
      dataSource: 'mock', // Default to mock, will switch to simulation when active
      isLoading: false,
      error: null,
      lastUpdated: new Date().toISOString(),

      // Add a new throughput data point (rolling window)
      addThroughputPoint: (point) => {
        const { throughputData, maxThroughputPoints } = get()
        const newData = [...throughputData, point]
        if (newData.length > maxThroughputPoints) {
          newData.shift()
        }
        set({ throughputData: newData, lastUpdated: new Date().toISOString() })
      },

      // Set utilization data
      setUtilizationData: (data) => {
        set({ utilizationData: data, lastUpdated: new Date().toISOString() })
      },

      // Set hot spot data
      setHotSpotData: (data) => {
        set({ hotSpotData: data, lastUpdated: new Date().toISOString() })
      },

      // Update summary metrics
      updateSummary: (summary) => {
        set((state) => ({
          summary: { ...state.summary, ...summary },
          lastUpdated: new Date().toISOString(),
        }))
      },

      // Set loading state
      setLoading: (loading) => {
        set({ isLoading: loading })
      },

      // Set error state
      setError: (error) => {
        set({ error })
      },

      // Refresh all data (prioritize simulation data, fallback to mock with task stats)
      refreshData: () => {
        // First check if simulation has data
        if (hasActiveSimulation()) {
          get().updateFromSimulation()
          return
        }

        // Get task statistics for integration
        const taskStats = useTaskStore.getState().getStatistics()

        // Fallback to mock data, but update summary with task stats
        set({
          throughputData: generateMockThroughputData(),
          utilizationData: generateMockUtilizationData(),
          hotSpotData: generateMockHotSpotData(),
          summary: {
            totalThroughput: taskStats.completed, // Use completed tasks from taskStore
            avgUtilization: 67.5,
            peakUtilization: 92,
            activeEntities: taskStats.running, // Use running tasks as active entities
            totalEntities: taskStats.total,
          },
          dataSource: 'mock',
          lastUpdated: new Date().toISOString(),
          error: null,
        })
      },

      // Update from simulation store
      updateFromSimulation: (entityInfo) => {
        const snapshot = getSimulationKPISnapshot(entityInfo)

        if (snapshot) {
          set({
            throughputData: snapshot.throughputData,
            utilizationData: snapshot.utilizationData,
            summary: snapshot.summary,
            dataSource: 'simulation',
            lastUpdated: snapshot.lastUpdated,
            error: null,
          })
        } else {
          // No simulation data, fallback to mock
          set({
            throughputData: generateMockThroughputData(),
            utilizationData: generateMockUtilizationData(),
            dataSource: 'mock',
            lastUpdated: new Date().toISOString(),
          })
        }
      },

      // Get current data source
      getDataSource: () => {
        return get().dataSource
      },

      // Reset to initial state
      reset: () => {
        set({
          throughputData: generateMockThroughputData(),
          utilizationData: generateMockUtilizationData(),
          hotSpotData: generateMockHotSpotData(),
          summary: {
            totalThroughput: 0,
            avgUtilization: 0,
            peakUtilization: 0,
            activeEntities: 0,
            totalEntities: 0,
          },
          dataSource: 'mock',
          isLoading: false,
          error: null,
          lastUpdated: null,
        })
      },
    }),
    { name: 'KPIStore' }
  )
)

export default useKPIStore
