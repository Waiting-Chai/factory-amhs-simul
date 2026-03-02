import type React from 'react'

type ModelViewerProps = React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement> & {
  src?: string
  alt?: string
  loading?: 'lazy' | 'eager'
  reveal?: 'auto' | 'interaction' | 'manual'
  'camera-controls'?: boolean
  'auto-rotate'?: boolean
  'camera-orbit'?: string
  'min-camera-orbit'?: string
  'max-camera-orbit'?: string
  'camera-target'?: string
  'interaction-prompt'?: 'auto' | 'none'
}

declare global {
  namespace JSX {
    interface IntrinsicElements {
      'model-viewer': ModelViewerProps
    }
  }
}

export {}
