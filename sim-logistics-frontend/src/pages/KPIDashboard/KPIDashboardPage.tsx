/**
 * KPI Dashboard Page - Industrial Control Room Interface
 *
 * Implements 5.5: KPI Dashboard Module
 * - Throughput line chart (real-time task completion)
 * - Utilization bar chart (entity utilization rates)
 * - Hot spot view placeholder (for heatmap in future iteration)
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-27
 */
import React, { useEffect, useCallback, useRef } from 'react'
import { Link } from 'react-router-dom'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler,
  ChartOptions,
} from 'chart.js'
import { Line, Bar } from 'react-chartjs-2'
import { useKPIStore, type EntityUtilization } from '@store/kpiStore'
import { t } from '@/shared/i18n'

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler
)

// Chart colors following industrial control room theme
const CHART_COLORS = {
  primary: '#00d4ff', // Cyan accent
  primaryFill: 'rgba(0, 212, 255, 0.1)',
  secondary: '#10b981', // Emerald
  warning: '#f59e0b', // Amber
  danger: '#ef4444', // Red
  grid: 'rgba(148, 163, 184, 0.1)',
  text: '#94a3b8',
  textLight: '#64748b',
}

// Status color mapping for utilization bars
const getStatusColor = (status: EntityUtilization['status']): string => {
  switch (status) {
    case 'idle':
      return CHART_COLORS.textLight
    case 'active':
      return CHART_COLORS.primary
    case 'overloaded':
      return CHART_COLORS.warning
    default:
      return CHART_COLORS.text
  }
}

/**
 * Empty state placeholder component
 */
const EmptyState: React.FC<{ message: string }> = ({ message }) => (
  <div className="flex-1 flex items-center justify-center">
    <div className="text-center">
      <svg
        className="w-12 h-12 mx-auto mb-3 text-slate-600"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={1}
          d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
        />
      </svg>
      <div className="text-sm font-mono text-slate-500">{message}</div>
    </div>
  </div>
)

/**
 * Throughput line chart component
 */
const ThroughputChart: React.FC = () => {
  const { throughputData } = useKPIStore()

  // Empty state
  if (throughputData.length === 0) {
    return <EmptyState message={t('kpi.emptyThroughput')} />
  }

  const data = {
    labels: throughputData.map((d) => d.timestamp),
    datasets: [
      {
        label: t('kpi.tasksCompleted'),
        data: throughputData.map((d) => d.value),
        borderColor: CHART_COLORS.primary,
        backgroundColor: CHART_COLORS.primaryFill,
        fill: true,
        tension: 0.3,
        pointRadius: 2,
        pointHoverRadius: 5,
        pointBackgroundColor: CHART_COLORS.primary,
        pointBorderColor: '#0a0e14',
        pointBorderWidth: 2,
      },
    ],
  }

  const options: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 300,
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        backgroundColor: '#1e293b',
        titleColor: CHART_COLORS.text,
        bodyColor: CHART_COLORS.primary,
        borderColor: '#334155',
        borderWidth: 1,
        titleFont: {
          family: 'monospace',
          size: 11,
        },
        bodyFont: {
          family: 'monospace',
          size: 12,
          weight: 'bold',
        },
        padding: 10,
        displayColors: false,
      },
    },
    scales: {
      x: {
        grid: {
          color: CHART_COLORS.grid,
        },
        ticks: {
          color: CHART_COLORS.textLight,
          font: {
            family: 'monospace',
            size: 10,
          },
          maxRotation: 0,
        },
      },
      y: {
        beginAtZero: true,
        grid: {
          color: CHART_COLORS.grid,
        },
        ticks: {
          color: CHART_COLORS.textLight,
          font: {
            family: 'monospace',
            size: 10,
          },
        },
      },
    },
    interaction: {
      intersect: false,
      mode: 'index',
    },
  }

  return <Line data={data} options={options} />
}

/**
 * Utilization bar chart component
 */
const UtilizationChart: React.FC = () => {
  const { utilizationData } = useKPIStore()

  // Empty state
  if (utilizationData.length === 0) {
    return <EmptyState message={t('kpi.emptyUtilization')} />
  }

  const data = {
    labels: utilizationData.map((d) => d.entityName),
    datasets: [
      {
        label: t('kpi.utilization'),
        data: utilizationData.map((d) => d.utilization),
        backgroundColor: utilizationData.map((d) => getStatusColor(d.status)),
        borderColor: utilizationData.map((d) => getStatusColor(d.status)),
        borderWidth: 1,
        borderRadius: 2,
        barThickness: 24,
      },
    ],
  }

  const options: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 300,
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        backgroundColor: '#1e293b',
        titleColor: CHART_COLORS.text,
        bodyColor: CHART_COLORS.primary,
        borderColor: '#334155',
        borderWidth: 1,
        titleFont: {
          family: 'monospace',
          size: 11,
        },
        bodyFont: {
          family: 'monospace',
          size: 12,
          weight: 'bold',
        },
        padding: 10,
        displayColors: false,
        callbacks: {
          label: (context) => `${context.parsed.x}%`,
        },
      },
    },
    indexAxis: 'y' as const,
    scales: {
      x: {
        beginAtZero: true,
        max: 100,
        grid: {
          color: CHART_COLORS.grid,
        },
        ticks: {
          color: CHART_COLORS.textLight,
          font: {
            family: 'monospace',
            size: 10,
          },
          callback: (value) => `${value}%`,
        },
      },
      y: {
        grid: {
          display: false,
        },
        ticks: {
          color: CHART_COLORS.text,
          font: {
            family: 'monospace',
            size: 11,
            weight: 'bold',
          },
        },
      },
    },
  }

  return <Bar data={data} options={options} />
}

/**
 * Hot spot view placeholder component
 */
const HotSpotPlaceholder: React.FC = () => {
  const { hotSpotData } = useKPIStore()

  return (
    <div className="relative w-full h-full bg-[#0a0e14] border border-slate-800 rounded overflow-hidden">
      {/* Grid overlay */}
      <div
        className="absolute inset-0 opacity-10 pointer-events-none"
        style={{
          backgroundImage: `
            linear-gradient(to right, #334155 1px, transparent 1px),
            linear-gradient(to bottom, #334155 1px, transparent 1px)
          `,
          backgroundSize: '30px 30px',
        }}
      />

      {/* Placeholder content */}
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <div className="text-slate-600 mb-2">
          <svg
            className="w-12 h-12"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1}
              d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
            />
          </svg>
        </div>
        <div className="text-sm font-mono text-slate-500 tracking-wide">
          {t('kpi.heatMapComingSoon')}
        </div>
      </div>

      {/* Hot spot indicators (simple placeholder visualization) */}
      <div className="absolute bottom-4 left-4 right-4">
        <div className="text-xs font-mono text-slate-600 tracking-widest uppercase mb-2">
          {t('kpi.hotSpots')}
        </div>
        <div className="flex flex-wrap gap-2">
          {hotSpotData.slice(0, 4).map((spot) => (
            <div
              key={spot.areaId}
              className="flex items-center gap-1.5 px-2 py-1 bg-slate-900/80 border border-slate-700"
            >
              <div
                className="w-2 h-2 rounded-full"
                style={{
                  backgroundColor:
                    spot.intensity > 70
                      ? CHART_COLORS.danger
                      : spot.intensity > 40
                      ? CHART_COLORS.warning
                      : CHART_COLORS.secondary,
                }}
              />
              <span className="text-[10px] font-mono text-slate-400">
                {spot.areaName}
              </span>
              <span
                className="text-[10px] font-mono font-bold"
                style={{
                  color:
                    spot.intensity > 70
                      ? CHART_COLORS.danger
                      : spot.intensity > 40
                      ? CHART_COLORS.warning
                      : CHART_COLORS.secondary,
                }}
              >
                {spot.intensity}%
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

/**
 * Metric card component
 */
const MetricCard: React.FC<{
  label: string
  value: string | number
  unit?: string
  accent?: boolean
  trend?: 'up' | 'down' | 'stable'
}> = ({ label, value, unit, accent, trend }) => (
  <div
    className={`
      flex flex-col p-3 border
      ${accent
        ? 'bg-[#083344]/50 border-[#00d4ff]/30'
        : 'bg-slate-900/50 border-slate-700/50'
      }
    `}
  >
    <span className="text-[10px] font-mono tracking-widest text-slate-500 uppercase mb-1">
      {label}
    </span>
    <div className="flex items-baseline gap-1">
      <span
        className={`
          text-xl font-mono font-bold tracking-tight
          ${accent ? 'text-[#00d4ff]' : 'text-slate-200'}
        `}
      >
        {value}
      </span>
      {unit && (
        <span className="text-xs font-mono text-slate-500">{unit}</span>
      )}
      {trend && (
        <span
          className={`
            ml-1 text-[10px]
            ${trend === 'up' ? 'text-emerald-400' : trend === 'down' ? 'text-red-400' : 'text-slate-500'}
          `}
        >
          {trend === 'up' ? '↑' : trend === 'down' ? '↓' : '→'}
        </span>
      )}
    </div>
  </div>
)

/**
 * KPI Dashboard Page
 */
const KPIDashboardPage: React.FC = () => {
  const { summary, lastUpdated, refreshData, isLoading, error, dataSource } = useKPIStore()
  const refreshIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // Auto-refresh data every 5 seconds (with immediate first refresh)
  useEffect(() => {
    // Immediately refresh on mount to sync with simulation state
    refreshData()

    // Then start interval refresh
    refreshIntervalRef.current = setInterval(() => {
      refreshData()
    }, 5000)

    return () => {
      if (refreshIntervalRef.current) {
        clearInterval(refreshIntervalRef.current)
      }
    }
  }, [refreshData])

  const handleManualRefresh = useCallback(() => {
    refreshData()
  }, [refreshData])

  // Data source indicator config
  const dataSourceConfig = {
    simulation: {
      label: t('kpi.dataSource.simulation'),
      bgColor: 'bg-[#00d4ff]/10',
      textColor: 'text-[#00d4ff]',
      borderColor: 'border-[#00d4ff]/30',
      dotColor: 'bg-[#00d4ff]',
    },
    mock: {
      label: t('kpi.dataSource.mock'),
      bgColor: 'bg-amber-500/10',
      textColor: 'text-amber-400',
      borderColor: 'border-amber-500/30',
      dotColor: 'bg-amber-400',
    },
  }

  const currentDataSourceConfig = dataSourceConfig[dataSource]

  // Error state
  if (error) {
    return (
      <div className="h-screen flex flex-col bg-[#0a0e14] text-slate-200 overflow-hidden">
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <svg
              className="w-16 h-16 mx-auto mb-4 text-red-400/50"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
            <div className="text-xl font-mono text-red-400 mb-2">
              {t('kpi.errorTitle')}
            </div>
            <div className="text-sm font-mono text-slate-500 mb-6">
              {error}
            </div>
            <button
              onClick={handleManualRefresh}
              disabled={isLoading}
              className="px-4 py-2 bg-slate-800 text-slate-300 border border-slate-600 hover:border-[#00d4ff]/50 font-mono text-sm transition-colors disabled:opacity-50"
            >
              {isLoading ? t('common.loading') : t('common.retry')}
            </button>
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
                {t('kpi.title')}
              </h1>
              <p className="text-xs font-mono text-slate-600">
                {t('kpi.subtitle')}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            {/* Data source indicator */}
            <div
              className={`
                flex items-center gap-2 px-2 py-1 border font-mono text-xs
                ${currentDataSourceConfig.bgColor}
                ${currentDataSourceConfig.textColor}
                ${currentDataSourceConfig.borderColor}
              `}
            >
              <div className={`w-2 h-2 rounded-full ${currentDataSourceConfig.dotColor} ${dataSource === 'simulation' ? 'animate-pulse' : ''}`} />
              <span>{currentDataSourceConfig.label}</span>
            </div>

            {/* Last updated */}
            {lastUpdated && (
              <span className="text-xs font-mono text-slate-600">
                {t('kpi.lastUpdated')}:{' '}
                {new Date(lastUpdated).toLocaleTimeString('en-US', {
                  hour12: false,
                })}
              </span>
            )}

            {/* Refresh button */}
            <button
              onClick={handleManualRefresh}
              disabled={isLoading}
              className={`
                px-3 py-1 text-xs font-mono tracking-wider uppercase
                border border-slate-700 text-slate-400
                hover:border-[#00d4ff]/50 hover:text-[#00d4ff]
                transition-all duration-150
                disabled:opacity-50 disabled:cursor-not-allowed
              `}
              title={t('kpi.refresh')}
            >
              {isLoading ? t('common.loading') : t('kpi.refresh')}
            </button>
          </div>
        </div>
      </header>

      {/* Summary metrics bar */}
      <div className="flex-shrink-0 border-b border-slate-800 bg-[#0d1117]/60">
        <div className="grid grid-cols-5 gap-px bg-slate-800">
          <MetricCard
            label={t('kpi.totalThroughput')}
            value={summary.totalThroughput.toLocaleString()}
            unit={t('kpi.tasks')}
            accent
            trend="up"
          />
          <MetricCard
            label={t('kpi.avgUtilization')}
            value={summary.avgUtilization.toFixed(1)}
            unit="%"
            trend="stable"
          />
          <MetricCard
            label={t('kpi.peakUtilization')}
            value={summary.peakUtilization}
            unit="%"
            trend="up"
          />
          <MetricCard
            label={t('kpi.activeEntities')}
            value={summary.activeEntities}
            unit={`/ ${summary.totalEntities}`}
          />
          <MetricCard
            label={t('kpi.efficiency')}
            value={(summary.avgUtilization * 0.95).toFixed(1)}
            unit="%"
            accent
            trend="up"
          />
        </div>
      </div>

      {/* Main content */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left panel - Throughput chart */}
        <div className="flex-1 flex flex-col border-r border-slate-800">
          <div className="flex-shrink-0 px-4 py-3 border-b border-slate-800 bg-[#0d1117]/60">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-[#00d4ff] animate-pulse" />
                <span className="text-xs font-mono tracking-widest text-slate-500 uppercase">
                  {t('kpi.throughputTrend')}
                </span>
              </div>
              <span className="text-xs font-mono text-slate-600">
                {t('kpi.realtime')}
              </span>
            </div>
          </div>
          <div className="flex-1 p-4">
            <ThroughputChart />
          </div>
        </div>

        {/* Right panel - Utilization and Hot spot */}
        <div className="w-[380px] flex flex-col">
          {/* Utilization chart */}
          <div className="flex-1 flex flex-col border-b border-slate-800">
            <div className="flex-shrink-0 px-4 py-3 border-b border-slate-800 bg-[#0d1117]/60">
              <span className="text-xs font-mono tracking-widest text-slate-500 uppercase">
                {t('kpi.entityUtilization')}
              </span>
            </div>
            <div className="flex-1 p-4">
              <UtilizationChart />
            </div>
          </div>

          {/* Hot spot placeholder */}
          <div className="flex-1 flex flex-col">
            <div className="flex-shrink-0 px-4 py-3 border-b border-slate-800 bg-[#0d1117]/60">
              <span className="text-xs font-mono tracking-widest text-slate-500 uppercase">
                {t('kpi.hotSpotView')}
              </span>
            </div>
            <div className="flex-1 p-4">
              <HotSpotPlaceholder />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default KPIDashboardPage
