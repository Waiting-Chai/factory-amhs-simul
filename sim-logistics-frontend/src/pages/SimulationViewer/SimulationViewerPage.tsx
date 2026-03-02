/**
 * Simulation Viewer Page - Industrial Control Room Interface
 *
 * Implements 5.4: Simulation Run Module
 * - Run control (start/pause/step/reset)
 * - Speed control (1x ~ 100x)
 * - Time display (simulation + wall clock)
 * - Status panel with entity states
 * - Refresh rate switching
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-27
 */
import React, { useEffect, useCallback, useState, useRef } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useSimulationStore, formatSimulationTime, formatWallClockTime } from '@store/simulationStore'
import type { SimulationStatus, EntityRuntimeStatus, RefreshRate } from '@store/simulationStore'
import { useSceneStore } from '@store/sceneStore'
import { t, type MessageKey } from '@/shared/i18n'

// Valid speed multipliers
const SPEED_OPTIONS = [1, 2, 5, 10, 20, 50, 100]

// Refresh rate options
const REFRESH_RATES: { value: RefreshRate; labelKey: string }[] = [
  { value: 200, labelKey: 'simulation.refreshRate.200ms' },
  { value: 500, labelKey: 'simulation.refreshRate.500ms' },
  { value: 1000, labelKey: 'simulation.refreshRate.1s' },
]

// Simulation run status i18n mapping
const SIMULATION_STATUS_LABEL_MAP: Record<SimulationStatus, string> = {
  IDLE: 'simulation.status.idle',
  RUNNING: 'simulation.status.running',
  PAUSED: 'simulation.status.paused',
  COMPLETED: 'simulation.status.completed',
  ERROR: 'simulation.status.error',
}

// Entity status configuration with i18n keys
const STATUS_CONFIG: Record<EntityRuntimeStatus, { color: string; bgColor: string; borderColor: string; labelKey: string }> = {
  IDLE: { color: '#9ca3af', bgColor: '#1f2937', borderColor: '#374151', labelKey: 'simulation.status.idle' },
  MOVING: { color: '#00d4ff', bgColor: '#083344', borderColor: '#00d4ff', labelKey: 'simulation.status.moving' },
  LOADING: { color: '#fbbf24', bgColor: '#422006', borderColor: '#fbbf24', labelKey: 'simulation.status.loading' },
  UNLOADING: { color: '#10b981', bgColor: '#064e3b', borderColor: '#10b981', labelKey: 'simulation.status.unloading' },
  CHARGING: { color: '#a78bfa', bgColor: '#2e1065', borderColor: '#a78bfa', labelKey: 'simulation.status.charging' },
  BLOCKED: { color: '#f87171', bgColor: '#450a0a', borderColor: '#f87171', labelKey: 'simulation.status.blocked' },
  ERROR: { color: '#ef4444', bgColor: '#450a0a', borderColor: '#ef4444', labelKey: 'simulation.status.error' },
}

/**
 * Simulation control button component
 */
const ControlButton: React.FC<{
  onClick: () => void
  disabled?: boolean
  variant: 'primary' | 'secondary' | 'danger'
  children: React.ReactNode
  title: string
}> = ({ onClick, disabled, variant, children, title }) => {
  const variantStyles = {
    primary: 'bg-[#00d4ff]/10 hover:bg-[#00d4ff]/20 text-[#00d4ff] border-[#00d4ff]/50',
    secondary: 'bg-slate-800/50 hover:bg-slate-700/50 text-slate-300 border-slate-600',
    danger: 'bg-red-900/20 hover:bg-red-900/30 text-red-400 border-red-500/50',
  }

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={`
        px-4 py-2 font-mono text-sm font-medium tracking-wider uppercase
        border transition-all duration-150
        disabled:opacity-40 disabled:cursor-not-allowed
        active:scale-95
        ${variantStyles[variant]}
      `}
    >
      {children}
    </button>
  )
}

/**
 * Time display panel component
 */
const TimePanel: React.FC<{
  label: string
  value: string
  subValue?: string
  accent?: boolean
}> = ({ label, value, subValue, accent }) => (
  <div className={`
    flex flex-col p-3 border
    ${accent
      ? 'bg-[#083344]/50 border-[#00d4ff]/30'
      : 'bg-slate-900/50 border-slate-700/50'
    }
  `}>
    <span className="text-[10px] font-mono tracking-widest text-slate-500 uppercase mb-1">
      {label}
    </span>
    <span className={`
      text-2xl font-mono font-bold tracking-tight
      ${accent ? 'text-[#00d4ff]' : 'text-slate-200'}
    `}>
      {value}
    </span>
    {subValue && (
      <span className="text-xs font-mono text-slate-500 mt-1">
        {subValue}
      </span>
    )}
  </div>
)

/**
 * Status indicator with pulse animation
 */
const StatusIndicator: React.FC<{
  status: EntityRuntimeStatus
  count: number
}> = ({ status, count }) => {
  const config = STATUS_CONFIG[status]

  return (
    <div
      className="flex items-center justify-between p-2 border transition-colors"
      style={{
        backgroundColor: config.bgColor,
        borderColor: config.borderColor,
      }}
    >
      <div className="flex items-center gap-2">
        <div
          className="w-2 h-2 rounded-full animate-pulse"
          style={{ backgroundColor: config.color }}
        />
        <span
          className="text-xs font-mono font-medium tracking-wider"
          style={{ color: config.color }}
        >
          {t(config.labelKey as MessageKey)}
        </span>
      </div>
      <span
        className="text-sm font-mono font-bold"
        style={{ color: config.color }}
      >
        {count}
      </span>
    </div>
  )
}

/**
 * Speed slider component
 */
const SpeedSlider: React.FC<{
  value: number
  onChange: (speed: number) => void
  disabled: boolean
}> = ({ value, onChange, disabled }) => (
  <div className="flex flex-col gap-2">
    <div className="flex items-center justify-between">
      <span className="text-xs font-mono tracking-widest text-slate-500 uppercase">
        {t('simulation.speed')}
      </span>
      <span className="text-sm font-mono font-bold text-[#00d4ff]">
        {value}x
      </span>
    </div>
    <input
      type="range"
      min={0}
      max={SPEED_OPTIONS.length - 1}
      value={SPEED_OPTIONS.indexOf(value)}
      onChange={(e) => onChange(SPEED_OPTIONS[parseInt(e.target.value)])}
      disabled={disabled}
      className="w-full h-1 bg-slate-700 rounded-none appearance-none cursor-pointer
        [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3
        [&::-webkit-slider-thumb]:bg-[#00d4ff] [&::-webkit-slider-thumb]:cursor-pointer
        disabled:opacity-40 disabled:cursor-not-allowed"
    />
    <div className="flex justify-between text-[10px] font-mono text-slate-600">
      {SPEED_OPTIONS.map(s => <span key={s}>{s}x</span>)}
    </div>
  </div>
)

/**
 * Refresh rate selector
 */
const RefreshRateSelector: React.FC<{
  value: RefreshRate
  onChange: (rate: RefreshRate) => void
}> = ({ value, onChange }) => (
  <div className="flex flex-col gap-2">
    <span className="text-xs font-mono tracking-widest text-slate-500 uppercase">
      {t('simulation.refreshRate')}
    </span>
    <div className="flex gap-1">
      {REFRESH_RATES.map(({ value: rateValue, labelKey }) => (
        <button
          key={rateValue}
          onClick={() => onChange(rateValue)}
          className={`
            flex-1 px-2 py-1 text-xs font-mono font-medium tracking-wider
            border transition-all duration-150
            ${value === rateValue
              ? 'bg-[#00d4ff]/20 text-[#00d4ff] border-[#00d4ff]/50'
              : 'bg-slate-800/50 text-slate-400 border-slate-700 hover:border-slate-500'
            }
          `}
        >
          {t(labelKey as MessageKey)}
        </button>
      ))}
    </div>
  </div>
)

/**
 * Simulation Viewer Page
 */
const SimulationViewerPage: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const [wallClockTime, setWallClockTime] = useState(new Date())
  const tickIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const {
    status,
    speed,
    refreshRate,
    simulationTime,
    entityStates,
    metrics,
    startSimulation,
    pauseSimulation,
    stepSimulation,
    resetSimulation,
    setSpeed,
    setRefreshRate,
    tick,
  } = useSimulationStore()

  const { currentScene, fetchScene, isLoading: isSceneLoading, error: sceneError } = useSceneStore()

  // Load scene data
  useEffect(() => {
    if (id) {
      fetchScene(id)
    }
  }, [id, fetchScene])

  // Scene loading state
  const isSceneLoadingState = isSceneLoading && !currentScene
  const hasSceneError = sceneError && !currentScene
  const isEmptyScene = !isSceneLoadingState && !hasSceneError && (!currentScene || currentScene.entities.length === 0)

  // Simulation tick loop
  useEffect(() => {
    if (status === 'RUNNING') {
      tickIntervalRef.current = setInterval(() => {
        tick()
        setWallClockTime(new Date())
      }, refreshRate)
    } else {
      if (tickIntervalRef.current) {
        clearInterval(tickIntervalRef.current)
        tickIntervalRef.current = null
      }
    }

    return () => {
      if (tickIntervalRef.current) {
        clearInterval(tickIntervalRef.current)
      }
    }
  }, [status, refreshRate, tick])

  // Count entities by status
  const statusCounts = React.useMemo(() => {
    const counts: Record<EntityRuntimeStatus, number> = {
      IDLE: 0, MOVING: 0, LOADING: 0, UNLOADING: 0, CHARGING: 0, BLOCKED: 0, ERROR: 0,
    }
    entityStates.forEach(state => {
      counts[state.status]++
    })
    return counts
  }, [entityStates])

  const handleRunClick = useCallback(() => {
    if (status === 'RUNNING') {
      pauseSimulation()
    } else {
      startSimulation()
    }
  }, [status, startSimulation, pauseSimulation])

  const handleStepClick = useCallback(() => {
    if (status !== 'RUNNING') {
      stepSimulation()
    }
  }, [status, stepSimulation])

  const handleResetClick = useCallback(() => {
    if (window.confirm(t('simulation.confirmReset'))) {
      resetSimulation()
    }
  }, [resetSimulation])

  const isRunning = status === 'RUNNING'

  // Loading state
  if (isSceneLoadingState) {
    return (
      <div className="h-screen flex flex-col bg-[#0a0e14] text-slate-200 overflow-hidden">
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <div className="flex items-center justify-center gap-3 mb-4">
              <svg className="animate-spin h-6 w-6 text-[#00d4ff]" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              <span className="text-xl font-mono text-slate-400">{t('simulation.viewer.loadingScene')}</span>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // Error state
  if (hasSceneError) {
    return (
      <div className="h-screen flex flex-col bg-[#0a0e14] text-slate-200 overflow-hidden">
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <svg className="w-16 h-16 mx-auto mb-4 text-red-400/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <div className="text-xl font-mono text-red-400 mb-2">
              {t('simulation.viewer.sceneNotFound')}
            </div>
            <div className="text-sm font-mono text-slate-500 mb-6">
              {t('simulation.viewer.sceneNotFoundDesc')}
            </div>
            <div className="flex gap-3 justify-center">
              <button
                onClick={() => id && fetchScene(id)}
                className="px-4 py-2 bg-slate-800 text-slate-300 border border-slate-600 hover:border-[#00d4ff]/50 font-mono text-sm transition-colors"
              >
                {t('common.retry')}
              </button>
              <Link
                to="/simulation"
                className="px-4 py-2 bg-[#00d4ff]/10 text-[#00d4ff] border border-[#00d4ff]/50 font-mono text-sm hover:bg-[#00d4ff]/20 transition-colors"
              >
                {t('simulation.viewer.backToSelection')}
              </Link>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // Empty scene state (scene loaded but has no entities)
  if (isEmptyScene) {
    return (
      <div className="h-screen flex flex-col bg-[#0a0e14] text-slate-200 overflow-hidden">
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <svg className="w-16 h-16 mx-auto mb-4 text-slate-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
            </svg>
            <div className="text-xl font-mono text-slate-500 mb-2">
              {t('simulation.viewer.emptySceneTitle')}
            </div>
            <div className="text-sm font-mono text-slate-600 mb-6">
              {t('simulation.viewer.emptySceneDesc')}
            </div>
            <Link
              to="/simulation"
              className="px-4 py-2 bg-[#00d4ff]/10 text-[#00d4ff] border border-[#00d4ff]/50 font-mono text-sm hover:bg-[#00d4ff]/20 transition-colors"
            >
              {t('simulation.viewer.backToSelection')}
            </Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="h-screen flex flex-col bg-[#0a0e14] text-slate-200 overflow-hidden">
      {/* Header */}
      <header className="flex-shrink-0 border-b border-slate-800 bg-[#0d1117]">
        <div className="flex items-center justify-between px-6 py-3">
          <div className="flex items-center gap-4">
            <Link
              to="/scenes"
              className="text-slate-500 hover:text-slate-300 transition-colors font-mono text-sm"
            >
              &larr;
            </Link>
            <div>
              <h1 className="text-lg font-mono font-bold tracking-tight text-slate-100">
                {t('simulation.title')}
              </h1>
              <p className="text-xs font-mono text-slate-600">
                {currentScene?.name || id}
              </p>
            </div>
          </div>

          {/* Status badge */}
          <div className={`
            px-3 py-1 border font-mono text-xs tracking-widest uppercase
            ${status === 'RUNNING'
              ? 'bg-[#00d4ff]/10 text-[#00d4ff] border-[#00d4ff]/30'
              : status === 'PAUSED'
              ? 'bg-amber-500/10 text-amber-400 border-amber-500/30'
              : 'bg-slate-800 text-slate-400 border-slate-700'
            }
          `}>
            {t(SIMULATION_STATUS_LABEL_MAP[status] as MessageKey)}
          </div>
        </div>
      </header>

      {/* Main content */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left panel - Controls */}
        <aside className="w-72 flex-shrink-0 border-r border-slate-800 bg-[#0d1117]/80 flex flex-col">
          {/* Run controls */}
          <div className="p-4 border-b border-slate-800">
            <div className="text-xs font-mono tracking-widest text-slate-500 uppercase mb-3">
              {t('simulation.runControl')}
            </div>
            <div className="grid grid-cols-2 gap-2">
              <ControlButton
                onClick={handleRunClick}
                variant="primary"
                title={isRunning ? t('simulation.pause') : t('simulation.start')}
              >
                {isRunning ? t('simulation.control.pause') : t('simulation.control.start')}
              </ControlButton>
              <ControlButton
                onClick={handleStepClick}
                disabled={isRunning}
                variant="secondary"
                title={t('simulation.step')}
              >
                {t('simulation.control.step')}
              </ControlButton>
              <ControlButton
                onClick={handleResetClick}
                variant="danger"
                title={t('simulation.reset')}
              >
                {t('simulation.control.reset')}
              </ControlButton>
            </div>
          </div>

          {/* Speed control */}
          <div className="p-4 border-b border-slate-800">
            <SpeedSlider
              value={speed}
              onChange={setSpeed}
              disabled={status === 'IDLE'}
            />
          </div>

          {/* Refresh rate */}
          <div className="p-4 border-b border-slate-800">
            <RefreshRateSelector
              value={refreshRate}
              onChange={setRefreshRate}
            />
          </div>

          {/* Metrics */}
          <div className="p-4 flex-1 overflow-auto">
            <div className="text-xs font-mono tracking-widest text-slate-500 uppercase mb-3">
              {t('simulation.metrics')}
            </div>
            <div className="space-y-2">
              <div className="flex justify-between text-sm font-mono">
                <span className="text-slate-500">{t('simulation.totalTasks')}</span>
                <span className="text-slate-300">{metrics.totalTasks}</span>
              </div>
              <div className="flex justify-between text-sm font-mono">
                <span className="text-slate-500">{t('simulation.completedTasks')}</span>
                <span className="text-emerald-400">{metrics.completedTasks}</span>
              </div>
              <div className="flex justify-between text-sm font-mono">
                <span className="text-slate-500">{t('simulation.throughput')}</span>
                <span className="text-[#00d4ff]">{metrics.avgThroughput.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-sm font-mono">
                <span className="text-slate-500">{t('simulation.activeVehicles')}</span>
                <span className="text-[#00d4ff]">{metrics.activeVehicles}</span>
              </div>
              <div className="flex justify-between text-sm font-mono">
                <span className="text-slate-500">{t('simulation.blockedVehicles')}</span>
                <span className="text-red-400">{metrics.blockedVehicles}</span>
              </div>
            </div>
          </div>
        </aside>

        {/* Center - Simulation canvas */}
        <main className="flex-1 flex flex-col overflow-hidden">
          {/* Time bar */}
          <div className="flex-shrink-0 flex border-b border-slate-800 bg-[#0d1117]/60">
            <TimePanel
              label={t('simulation.simTime')}
              value={formatSimulationTime(simulationTime)}
              accent
            />
            <TimePanel
              label={t('simulation.wallClock')}
              value={formatWallClockTime(wallClockTime)}
            />
          </div>

          {/* Canvas area */}
          <div className="flex-1 relative bg-[#0a0e14]">
            {/* Grid overlay */}
            <div
              className="absolute inset-0 opacity-5 pointer-events-none"
              style={{
                backgroundImage: `
                  linear-gradient(to right, #1e293b 1px, transparent 1px),
                  linear-gradient(to bottom, #1e293b 1px, transparent 1px)
                `,
                backgroundSize: '40px 40px',
              }}
            />

            {/* Simulation placeholder */}
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="text-center">
                <div className="text-4xl font-mono font-light text-slate-700 mb-4">
                  {formatSimulationTime(simulationTime)}
                </div>
                <div className="text-sm font-mono text-slate-600">
                  {status === 'IDLE'
                    ? t('simulation.pressStart')
                    : t('simulation.running')
                  }
                </div>
              </div>
            </div>
          </div>
        </main>

        {/* Right panel - Status */}
        <aside className="w-56 flex-shrink-0 border-l border-slate-800 bg-[#0d1117]/80 flex flex-col">
          <div className="p-4 border-b border-slate-800">
            <div className="text-xs font-mono tracking-widest text-slate-500 uppercase">
              {t('simulation.entityStatus')}
            </div>
          </div>
          <div className="flex-1 p-3 space-y-1 overflow-auto">
            {Object.keys(STATUS_CONFIG).map((statusKey) => (
              <StatusIndicator
                key={statusKey}
                status={statusKey as EntityRuntimeStatus}
                count={statusCounts[statusKey as EntityRuntimeStatus]}
              />
            ))}
          </div>
        </aside>
      </div>
    </div>
  )
}

export default SimulationViewerPage
