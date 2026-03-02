import React, { useEffect, useMemo, useRef, useState } from 'react'
import { t } from '../../shared/i18n'

type ViewerErrorType = 'error' | 'timeout'

export interface ModelViewerModalProps {
  open: boolean
  modelName: string
  modelUrl: string | null
  onClose: () => void
}

export const ModelViewerModal: React.FC<ModelViewerModalProps> = ({
  open,
  modelName,
  modelUrl,
  onClose,
}) => {
  const viewerRef = useRef<HTMLElement | null>(null)
  const [isViewerLoading, setIsViewerLoading] = useState(false)
  const [errorType, setErrorType] = useState<ViewerErrorType | null>(null)

  const hasModelUrl = useMemo(
    () => typeof modelUrl === 'string' && modelUrl.trim().length > 0,
    [modelUrl]
  )

  useEffect(() => {
    if (!open) {
      setIsViewerLoading(false)
      setErrorType(null)
      return
    }

    if (modelUrl === null) {
      setIsViewerLoading(true)
      setErrorType(null)
      return
    }

    if (!hasModelUrl) {
      setIsViewerLoading(false)
      setErrorType('error')
      return
    }

    setIsViewerLoading(true)
    setErrorType(null)
  }, [open, modelUrl, hasModelUrl])

  const handleLoad = React.useCallback(() => {
    setIsViewerLoading(false)
  }, [])

  const handleError = React.useCallback(() => {
    setIsViewerLoading(false)
    setErrorType((prev) => prev ?? 'error')
  }, [])

  const setViewerRef = React.useCallback(
    (node: HTMLElement | null) => {
      // Cleanup previous listeners if node is changing
      if (viewerRef.current && viewerRef.current !== node) {
        viewerRef.current.removeEventListener('load', handleLoad)
        viewerRef.current.removeEventListener('error', handleError)
      }

      viewerRef.current = node

      if (node) {
        node.addEventListener('load', handleLoad)
        node.addEventListener('error', handleError)

        // Check if already loaded (race condition fix)
        const viewer = node as HTMLDivElement & { loaded?: boolean; modelIsVisible?: boolean }
        if (viewer.loaded || viewer.modelIsVisible) {
          handleLoad()
        }
      }
    },
    [handleLoad, handleError]
  )

  useEffect(() => {
    if (!open || !isViewerLoading) return
    const timeoutId = window.setTimeout(() => {
      setIsViewerLoading(false)
      setErrorType((prev) => prev ?? 'timeout')
    }, 10000)
    return () => window.clearTimeout(timeoutId)
  }, [open, isViewerLoading])

  useEffect(() => {
    if (!open) return
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [open, onClose])

  if (!open) return null

  const errorText = errorType === 'timeout'
    ? t('modelLibrary.viewer.timeout')
    : t('modelLibrary.viewer.error')

  return (
    <div
      className="fixed inset-0 z-[60] bg-black/80 backdrop-blur-sm flex items-center justify-center p-4"
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div className="w-[98vw] h-[94vh] max-w-none rounded-xl border border-industrial-steel/40 bg-industrial-slate shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-industrial-steel/30">
          <h2 className="text-lg font-display font-bold text-white truncate">
            {t('modelLibrary.viewer.title')}: {modelName}
          </h2>
          <button
            onClick={onClose}
            className="px-3 py-1.5 rounded bg-industrial-steel hover:bg-industrial-blue text-white text-sm transition-colors"
          >
            {t('modelLibrary.viewer.close')}
          </button>
        </div>

        <div className="relative h-[calc(94vh-64px)] bg-industrial-dark/70">
          {hasModelUrl ? (
            <model-viewer
              ref={setViewerRef}
              src={modelUrl ?? undefined}
              alt={modelName}
              camera-controls
              auto-rotate
              camera-target="0m 0m 0m"
              camera-orbit="0deg 75deg 55%"
              min-camera-orbit="auto auto 40%"
              max-camera-orbit="auto auto 220%"
              interaction-prompt="auto"
              style={{
                display: 'block',
                width: '100%',
                height: '100%',
              }}
              className="block h-full w-full"
            />
          ) : null}

          {isViewerLoading ? <div className="absolute inset-0 flex items-center justify-center text-sm text-gray-200">{t('modelLibrary.viewer.loading')}</div> : null}
          {errorType ? <div className="absolute inset-0 flex items-center justify-center text-sm text-gray-200">{errorText}</div> : null}
        </div>
      </div>
    </div>
  )
}

export default ModelViewerModal
