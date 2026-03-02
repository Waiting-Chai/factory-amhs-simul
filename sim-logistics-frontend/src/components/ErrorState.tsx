/**
 * Error state component for displaying API errors.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import React from 'react'
import { useI18n } from '../shared/i18n/useI18n'

interface ErrorStateProps {
  title?: string
  message: string
  onRetry?: () => void
  actionLabel?: string
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title,
  message,
  onRetry,
  actionLabel,
}) => {
  const { t } = useI18n()
  const resolvedTitle = title ?? t('common.error')
  const resolvedActionLabel = actionLabel ?? t('common.retry')

  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] px-4">
      <div className="text-center">
        {/* Error Icon */}
        <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-900/30 mb-4">
          <svg
            className="w-8 h-8 text-red-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </div>

        {/* Error Title */}
        <h3 className="text-xl font-semibold text-white mb-2">{resolvedTitle}</h3>

        {/* Error Message */}
        <p className="text-gray-400 mb-6 max-w-md">{message}</p>

        {/* Retry Button */}
        {onRetry && (
          <button
            onClick={onRetry}
            className="inline-flex items-center px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
          >
            <svg
              className="w-4 h-4 mr-2"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
              />
            </svg>
            {resolvedActionLabel}
          </button>
        )}
      </div>
    </div>
  )
}

/**
 * Loading state component
 */
export const LoadingState: React.FC<{ message?: string }> = ({ message }) => {
  const { t } = useI18n()
  const resolvedMessage = message ?? t('common.loading')

  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] px-4">
      <div className="text-center">
        {/* Spinner */}
        <div className="inline-flex items-center justify-center w-12 h-12 mb-4">
          <svg
            className="animate-spin w-12 h-12 text-blue-500"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            />
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            />
          </svg>
        </div>

        {/* Loading Message */}
        <p className="text-gray-400">{resolvedMessage}</p>
      </div>
    </div>
  )
}

/**
 * Empty state component
 */
export const EmptyState: React.FC<{
  title?: string
  message?: string
  illustration?: React.ReactNode
}> = ({
  title,
  message,
  illustration,
}) => {
  const { t } = useI18n()
  const resolvedTitle = title ?? t('common.noData')
  const resolvedMessage = message ?? t('common.emptyMessage')

  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] px-4">
      <div className="text-center">
        {/* Illustration or Icon */}
        {illustration || (
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-800/50 mb-4">
            <svg
              className="w-8 h-8 text-gray-500"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
              />
            </svg>
          </div>
        )}

        {/* Title */}
        <h3 className="text-xl font-semibold text-white mb-2">{resolvedTitle}</h3>

        {/* Message */}
        <p className="text-gray-400 max-w-md">{resolvedMessage}</p>
      </div>
    </div>
  )
}

export default ErrorState
