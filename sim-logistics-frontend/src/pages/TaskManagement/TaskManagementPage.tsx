/**
 * Task Management Page - Industrial Control Room Interface
 *
 * Implements 5.6: Task Management Module (Frontend Local MVP)
 * - Task creation form
 * - Task list with status badges
 * - Task status transitions
 *
 * @author shentw
 * @version 1.0
 * @since 2026-03-02
 */
import React, { useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { useTaskStore, type Task, type TaskStatus, type TaskPriority } from '@store/taskStore'
import { t, type MessageKey } from '@/shared/i18n'

// Status badge colors
const STATUS_CONFIG: Record<TaskStatus, { bg: string; text: string; border: string }> = {
  PENDING: { bg: 'bg-slate-500/20', text: 'text-slate-400', border: 'border-slate-500/30' },
  RUNNING: { bg: 'bg-[#00d4ff]/20', text: 'text-[#00d4ff]', border: 'border-[#00d4ff]/30' },
  COMPLETED: { bg: 'bg-emerald-500/20', text: 'text-emerald-400', border: 'border-emerald-500/30' },
  FAILED: { bg: 'bg-red-500/20', text: 'text-red-400', border: 'border-red-500/30' },
}

// Priority badge colors
const PRIORITY_CONFIG: Record<TaskPriority, { bg: string; text: string; border: string }> = {
  LOW: { bg: 'bg-slate-500/10', text: 'text-slate-500', border: 'border-slate-500/20' },
  NORMAL: { bg: 'bg-slate-500/20', text: 'text-slate-400', border: 'border-slate-500/30' },
  HIGH: { bg: 'bg-amber-500/20', text: 'text-amber-400', border: 'border-amber-500/30' },
  URGENT: { bg: 'bg-red-500/20', text: 'text-red-400', border: 'border-red-500/30' },
}

/**
 * Task status badge component
 */
const StatusBadge: React.FC<{ status: TaskStatus }> = ({ status }) => {
  const config = STATUS_CONFIG[status]
  const statusKeyMap: Record<TaskStatus, MessageKey> = {
    PENDING: 'task.status.pending',
    RUNNING: 'task.status.running',
    COMPLETED: 'task.status.completed',
    FAILED: 'task.status.failed',
  }
  return (
    <span
      className={`
        px-2 py-0.5 text-xs font-mono tracking-wider uppercase border
        ${config.bg} ${config.text} ${config.border}
      `}
    >
      {t(statusKeyMap[status])}
    </span>
  )
}

/**
 * Priority badge component
 */
const PriorityBadge: React.FC<{ priority: TaskPriority }> = ({ priority }) => {
  const config = PRIORITY_CONFIG[priority]
  const priorityKeyMap: Record<TaskPriority, MessageKey> = {
    LOW: 'task.priority.low',
    NORMAL: 'task.priority.normal',
    HIGH: 'task.priority.high',
    URGENT: 'task.priority.urgent',
  }
  return (
    <span
      className={`
        px-2 py-0.5 text-xs font-mono tracking-wider uppercase border
        ${config.bg} ${config.text} ${config.border}
      `}
    >
      {t(priorityKeyMap[priority])}
    </span>
  )
}

/**
 * Task creation form component
 */
const TaskCreationForm: React.FC = () => {
  const { createTask } = useTaskStore()
  const [name, setName] = useState('')
  const [priority, setPriority] = useState<TaskPriority>('NORMAL')
  const [sourceEntityId, setSourceEntityId] = useState('')
  const [targetEntityId, setTargetEntityId] = useState('')

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault()
      if (!name.trim()) return

      createTask({
        name: name.trim(),
        priority,
        sourceEntityId: sourceEntityId.trim() || undefined,
        targetEntityId: targetEntityId.trim() || undefined,
      })

      // Reset form
      setName('')
      setPriority('NORMAL')
      setSourceEntityId('')
      setTargetEntityId('')
    },
    [createTask, name, priority, sourceEntityId, targetEntityId]
  )

  return (
    <form onSubmit={handleSubmit} className="p-4 border-b border-slate-800 bg-[#0d1117]/60">
      <div className="text-xs font-mono tracking-widest text-slate-500 uppercase mb-3">
        {t('task.createTitle')}
      </div>

      <div className="grid grid-cols-4 gap-3">
        {/* Task name */}
        <div className="col-span-2">
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={t('task.namePlaceholder')}
            className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-slate-200 text-sm font-mono focus:border-[#00d4ff]/50 focus:outline-none"
          />
        </div>

        {/* Priority */}
        <div>
          <select
            value={priority}
            onChange={(e) => setPriority(e.target.value as TaskPriority)}
            className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-slate-200 text-sm font-mono focus:border-[#00d4ff]/50 focus:outline-none"
          >
            <option value="LOW">{t('task.priority.low')}</option>
            <option value="NORMAL">{t('task.priority.normal')}</option>
            <option value="HIGH">{t('task.priority.high')}</option>
            <option value="URGENT">{t('task.priority.urgent')}</option>
          </select>
        </div>

        {/* Submit button */}
        <div>
          <button
            type="submit"
            disabled={!name.trim()}
            className="w-full px-3 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-mono transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {t('task.createButton')}
          </button>
        </div>
      </div>

      {/* Optional entity fields */}
      <div className="grid grid-cols-2 gap-3 mt-3">
        <div>
          <input
            type="text"
            value={sourceEntityId}
            onChange={(e) => setSourceEntityId(e.target.value)}
            placeholder={t('task.sourceEntityPlaceholder')}
            className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-slate-400 text-sm font-mono focus:border-[#00d4ff]/50 focus:outline-none"
          />
        </div>
        <div>
          <input
            type="text"
            value={targetEntityId}
            onChange={(e) => setTargetEntityId(e.target.value)}
            placeholder={t('task.targetEntityPlaceholder')}
            className="w-full px-3 py-2 bg-slate-900 border border-slate-700 text-slate-400 text-sm font-mono focus:border-[#00d4ff]/50 focus:outline-none"
          />
        </div>
      </div>
    </form>
  )
}

/**
 * Task list item component
 */
const TaskListItem: React.FC<{ task: Task }> = ({ task }) => {
  const { startTask, completeTask, failTask, deleteTask } = useTaskStore()

  const handleStart = useCallback(() => {
    startTask(task.id)
  }, [startTask, task.id])

  const handleComplete = useCallback(() => {
    completeTask(task.id)
  }, [completeTask, task.id])

  const handleFail = useCallback(() => {
    failTask(task.id)
  }, [failTask, task.id])

  const handleDelete = useCallback(() => {
    deleteTask(task.id)
  }, [deleteTask, task.id])

  return (
    <div className="p-3 border-b border-slate-800 hover:bg-slate-900/50 transition-colors">
      <div className="flex items-center justify-between">
        {/* Left: Task info */}
        <div className="flex items-center gap-3">
          <StatusBadge status={task.status} />
          <PriorityBadge priority={task.priority} />
          <span className="text-sm font-mono text-slate-200">{task.name}</span>
        </div>

        {/* Right: Actions */}
        <div className="flex items-center gap-2">
          {task.status === 'PENDING' && (
            <button
              onClick={handleStart}
              className="px-2 py-1 text-xs font-mono text-[#00d4ff] border border-[#00d4ff]/30 hover:bg-[#00d4ff]/10 transition-colors"
            >
              {t('task.action.start')}
            </button>
          )}
          {task.status === 'RUNNING' && (
            <>
              <button
                onClick={handleComplete}
                className="px-2 py-1 text-xs font-mono text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/10 transition-colors"
              >
                {t('task.action.complete')}
              </button>
              <button
                onClick={handleFail}
                className="px-2 py-1 text-xs font-mono text-red-400 border border-red-500/30 hover:bg-red-500/10 transition-colors"
              >
                {t('task.action.fail')}
              </button>
            </>
          )}
          <button
            onClick={handleDelete}
            className="px-2 py-1 text-xs font-mono text-slate-500 border border-slate-600 hover:bg-slate-700 transition-colors"
          >
            {t('task.action.delete')}
          </button>
        </div>
      </div>

      {/* Task details */}
      <div className="mt-2 flex items-center gap-4 text-xs font-mono text-slate-600">
        <span>ID: {task.id}</span>
        {task.sourceEntityId && <span>{t('task.from')}: {task.sourceEntityId}</span>}
        {task.targetEntityId && <span>{t('task.to')}: {task.targetEntityId}</span>}
        <span>{t('task.createdAt')}: {new Date(task.createdAt).toLocaleTimeString()}</span>
      </div>
    </div>
  )
}

/**
 * Task statistics bar component
 */
const TaskStatisticsBar: React.FC = () => {
  const { getStatistics, resetTasks } = useTaskStore()
  const stats = getStatistics()

  return (
    <div className="flex-shrink-0 border-b border-slate-800 bg-[#0d1117]/60">
      <div className="flex items-center justify-between px-4 py-3">
        <div className="flex items-center gap-6">
          <div className="text-xs font-mono text-slate-400">
            {t('task.statistics.total')}: <span className="text-slate-200">{stats.total}</span>
          </div>
          <div className="text-xs font-mono text-slate-400">
            {t('task.statistics.pending')}: <span className="text-slate-400">{stats.pending}</span>
          </div>
          <div className="text-xs font-mono text-slate-400">
            {t('task.statistics.running')}: <span className="text-[#00d4ff]">{stats.running}</span>
          </div>
          <div className="text-xs font-mono text-slate-400">
            {t('task.statistics.completed')}: <span className="text-emerald-400">{stats.completed}</span>
          </div>
          <div className="text-xs font-mono text-slate-400">
            {t('task.statistics.failed')}: <span className="text-red-400">{stats.failed}</span>
          </div>
        </div>

        <button
          onClick={resetTasks}
          className="px-3 py-1 text-xs font-mono text-slate-500 border border-slate-600 hover:bg-slate-700 transition-colors"
        >
          {t('task.resetAll')}
        </button>
      </div>
    </div>
  )
}

/**
 * Empty state component
 */
const EmptyState: React.FC = () => (
  <div className="flex-1 flex items-center justify-center">
    <div className="text-center">
      <svg
        className="w-16 h-16 mx-auto mb-4 text-slate-600"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={1}
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
        />
      </svg>
      <div className="text-sm font-mono text-slate-500">{t('task.emptyTitle')}</div>
      <div className="text-xs font-mono text-slate-600 mt-1">{t('task.emptyDesc')}</div>
    </div>
  </div>
)

/**
 * Task Management Page
 */
const TaskManagementPage: React.FC = () => {
  const { tasks } = useTaskStore()

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
                {t('task.title')}
              </h1>
              <p className="text-xs font-mono text-slate-600">
                {t('task.subtitle')}
              </p>
            </div>
          </div>

          {/* Local MVP indicator */}
          <div className="flex items-center gap-2 px-2 py-1 border border-amber-500/30 bg-amber-500/10 text-amber-400 text-xs font-mono">
            <div className="w-2 h-2 rounded-full bg-amber-400" />
            <span>{t('task.localMode')}</span>
          </div>
        </div>
      </header>

      {/* Statistics bar */}
      <TaskStatisticsBar />

      {/* Task creation form */}
      <TaskCreationForm />

      {/* Task list */}
      <div className="flex-1 overflow-y-auto">
        {tasks.length > 0 ? (
          tasks.map((task) => <TaskListItem key={task.id} task={task} />)
        ) : (
          <EmptyState />
        )}
      </div>
    </div>
  )
}

export default TaskManagementPage
