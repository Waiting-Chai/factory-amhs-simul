/**
 * Toast notification store using Zustand.
 *
 * Manages toast notifications for user feedback.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'
import type { Toast, ToastType } from '../components/Toast'

interface ToastState {
  toasts: Toast[]
  addToast: (type: ToastType, message: string) => void
  dismissToast: (id: string) => void
}

export const useToastStore = create<ToastState>()(
  devtools(
    (set) => ({
      toasts: [],
      addToast: (type, message) => {
        const id = `toast-${Date.now()}-${Math.random()}`
        set((state) => ({
          toasts: [...state.toasts, { id, type, message }],
        }))
      },
      dismissToast: (id) => {
        set((state) => ({
          toasts: state.toasts.filter((t) => t.id !== id),
        }))
      },
    }),
    { name: 'ToastStore' }
  )
)
