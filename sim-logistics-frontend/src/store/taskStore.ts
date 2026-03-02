/**
 * Task store using Zustand for task management state.
 *
 * Implements 5.6: Task Management Module (Frontend Local MVP)
 * - Task CRUD operations
 * - Task status transitions
 * - Task statistics for KPI integration
 *
 * Note: This is a frontend-only MVP. No backend API calls.
 * Data is stored in memory and will be lost on page refresh.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-03-02
 */
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'

// Task status enum
export type TaskStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'

// Task priority enum
export type TaskPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

// Task entity
export interface Task {
  id: string
  name: string
  priority: TaskPriority
  sourceEntityId?: string
  targetEntityId?: string
  status: TaskStatus
  createdAt: string
  updatedAt: string
}

// Task statistics
export interface TaskStatistics {
  total: number
  pending: number
  running: number
  completed: number
  failed: number
}

/**
 * Task store state interface
 */
interface TaskState {
  // Tasks data
  tasks: Task[]

  // Loading state
  isLoading: boolean
  error: string | null

  // Actions
  createTask: (task: { name: string; priority: TaskPriority; sourceEntityId?: string; targetEntityId?: string }) => Task
  startTask: (id: string) => void
  completeTask: (id: string) => void
  failTask: (id: string) => void
  deleteTask: (id: string) => void
  resetTasks: () => void

  // Selectors
  getTaskById: (id: string) => Task | undefined
  getStatistics: () => TaskStatistics
  getTasksByStatus: (status: TaskStatus) => Task[]
}

// Generate unique ID
const generateId = (): string => {
  return `task-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

/**
 * Task store implementation
 */
export const useTaskStore = create<TaskState>()(
  devtools(
    (set, get) => ({
      // Initial state
      tasks: [],
      isLoading: false,
      error: null,

      // Create a new task
      createTask: (taskData) => {
        const now = new Date().toISOString()
        const newTask: Task = {
          id: generateId(),
          ...taskData,
          status: 'PENDING',
          createdAt: now,
          updatedAt: now,
        }

        set((state) => ({
          tasks: [...state.tasks, newTask],
        }))

        return newTask
      },

      // Start a task (PENDING -> RUNNING)
      startTask: (id) => {
        set((state) => ({
          tasks: state.tasks.map((task) =>
            task.id === id && task.status === 'PENDING'
              ? { ...task, status: 'RUNNING', updatedAt: new Date().toISOString() }
              : task
          ),
        }))
      },

      // Complete a task (RUNNING -> COMPLETED)
      completeTask: (id) => {
        set((state) => ({
          tasks: state.tasks.map((task) =>
            task.id === id && task.status === 'RUNNING'
              ? { ...task, status: 'COMPLETED', updatedAt: new Date().toISOString() }
              : task
          ),
        }))
      },

      // Fail a task (RUNNING -> FAILED)
      failTask: (id) => {
        set((state) => ({
          tasks: state.tasks.map((task) =>
            task.id === id && task.status === 'RUNNING'
              ? { ...task, status: 'FAILED', updatedAt: new Date().toISOString() }
              : task
          ),
        }))
      },

      // Delete a task
      deleteTask: (id) => {
        set((state) => ({
          tasks: state.tasks.filter((task) => task.id !== id),
        }))
      },

      // Reset all tasks
      resetTasks: () => {
        set({ tasks: [] })
      },

      // Get task by ID
      getTaskById: (id) => {
        return get().tasks.find((task) => task.id === id)
      },

      // Get task statistics
      getStatistics: () => {
        const tasks = get().tasks
        return {
          total: tasks.length,
          pending: tasks.filter((t) => t.status === 'PENDING').length,
          running: tasks.filter((t) => t.status === 'RUNNING').length,
          completed: tasks.filter((t) => t.status === 'COMPLETED').length,
          failed: tasks.filter((t) => t.status === 'FAILED').length,
        }
      },

      // Get tasks by status
      getTasksByStatus: (status) => {
        return get().tasks.filter((task) => task.status === status)
      },
    }),
    { name: 'TaskStore' }
  )
)

export default useTaskStore
