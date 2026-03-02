import React, { useEffect, useMemo, useRef, useState } from 'react'
import { t } from '../../shared/i18n'

type PreviewErrorType = 'forbidden' | 'notFound' | 'cors' | 'network' | 'unknown' | 'timeout'

export interface ModelPreviewProps {
  modelUrl?: string | null
  name: string
  className?: string
  onResolveModelUrl?: () => Promise<string | null | undefined>
}

const getErrorMessage = (type: PreviewErrorType): string => {
  if (type === 'timeout') return t('modelLibrary.preview.errorTimeout')
  if (type === 'forbidden') return t('modelLibrary.preview.error403')
  if (type === 'notFound') return t('modelLibrary.preview.error404')
  if (type === 'cors') return t('modelLibrary.preview.errorCors')
  if (type === 'network') return t('modelLibrary.preview.errorNetwork')
  return t('modelLibrary.preview.errorUnknown')
}

export const ModelPreview: React.FC<ModelPreviewProps> = ({
  modelUrl,
  name,
  className = '',
  onResolveModelUrl,
}) => {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const viewerRef = useRef<HTMLElement | null>(null)
  const [isInView, setIsInView] = useState(false)
  const [resolvedUrl, setResolvedUrl] = useState<string | null>(modelUrl ?? null)
  const [isResolvingUrl, setIsResolvingUrl] = useState(false)
  const [isViewerLoading, setIsViewerLoading] = useState(true)
  const [errorType, setErrorType] = useState<PreviewErrorType | null>(null)
  const isResolvingRef = useRef(false)
  const resolveModelUrlRef = useRef(onResolveModelUrl)

  useEffect(() => {
    resolveModelUrlRef.current = onResolveModelUrl
  }, [onResolveModelUrl])

  useEffect(() => {
    const initialUrl = modelUrl ?? null
    setResolvedUrl(initialUrl)
    setErrorType(null)
    setIsViewerLoading(false)
    isResolvingRef.current = false
    setIsResolvingUrl(false)
    if (initialUrl) {
      setIsViewerLoading(true)
    }
  }, [modelUrl])

  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    if (typeof IntersectionObserver === 'undefined') {
      setIsInView(true)
      return
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          setIsInView(true)
          observer.disconnect()
        }
      },
      { rootMargin: '180px' }
    )
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    const resolveModelUrl = resolveModelUrlRef.current
    if (!isInView || resolvedUrl || !resolveModelUrl || isResolvingRef.current || !!errorType) {
      return
    }

    let active = true
    isResolvingRef.current = true
    setIsResolvingUrl(true)
    const resolve = async () => {
      try {
        const nextUrlRaw = await resolveModelUrl()
        const nextUrl = typeof nextUrlRaw === 'string' && nextUrlRaw.trim().length > 0
          ? nextUrlRaw
          : null
        if (active) setResolvedUrl(nextUrl ?? null)
      } catch {
        if (active) setErrorType('network')
      } finally {
        if (active) {
          isResolvingRef.current = false
          setIsResolvingUrl(false)
        }
      }
    }
    void resolve()
    return () => {
      active = false
    }
  }, [isInView, resolvedUrl, errorType])

  const handleLoad = React.useCallback(() => {
    setIsViewerLoading(false)
  }, [])

  const handleError = React.useCallback(() => {
    setIsViewerLoading(false)
    setErrorType((prev) => prev ?? 'unknown')
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
        // Check both 'loaded' and 'modelIsVisible' properties as different versions might behave differently
        const viewer = node as HTMLDivElement & { loaded?: boolean; modelIsVisible?: boolean }
        if (viewer.loaded || viewer.modelIsVisible) {
          handleLoad()
        }
      }
    },
    [handleLoad, handleError]
  )

  // Timeout fallback: 10 seconds
  useEffect(() => {
    if (!isViewerLoading || !resolvedUrl) return
    const timeoutId = setTimeout(() => {
      setIsViewerLoading(false)
      setErrorType((prev) => prev ?? 'timeout')
    }, 10000)
    return () => clearTimeout(timeoutId)
  }, [isViewerLoading, resolvedUrl])

  const showEmpty = isInView && !resolvedUrl && !isResolvingUrl
  const showLoading = isResolvingUrl || (isInView && !!resolvedUrl && isViewerLoading && !errorType)
  const errorText = useMemo(() => (errorType ? getErrorMessage(errorType) : ''), [errorType])

  return (
    <div
      ref={containerRef}
      className={`relative h-48 w-full overflow-hidden bg-industrial-dark/60 ${className}`}
      data-testid="model-preview"
    >
      {isInView && resolvedUrl && !errorType ? (
        <model-viewer
          ref={setViewerRef}
          src={resolvedUrl}
          alt={name}
          loading="lazy"
          reveal="auto"
          auto-rotate
          camera-controls
          interaction-prompt="auto"
          style={{
            display: 'block',
            width: '100%',
            height: '100%',
          }}
          className="block h-full w-full"
          data-testid="model-viewer"
        />
      ) : null}

      {showLoading ? (
        <div className="absolute inset-0 flex items-center justify-center text-xs font-mono text-gray-300">
          {t('modelLibrary.preview.loading')}
        </div>
      ) : null}

      {showEmpty ? (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 text-gray-500">
          <span className="text-xs font-mono">{t('modelLibrary.preview.empty')}</span>
        </div>
      ) : null}

      {errorType ? (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-1 px-3 text-center text-gray-300">
          <span className="text-xs font-mono">{t('modelLibrary.preview.errorTitle')}</span>
          <span className="text-[11px] text-gray-400">{errorText}</span>
        </div>
      ) : null}
    </div>
  )
}

export default ModelPreview
