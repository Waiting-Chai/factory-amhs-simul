import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { ModelPreview } from './ModelPreview'
import { setLocale } from '../../shared/i18n'

describe('ModelPreview', () => {
  beforeEach(() => {
    setLocale('en')
    vi.restoreAllMocks()
  })

  it('有 GLB URL 时渲染 model viewer', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }))

    const { container } = render(
      <ModelPreview
        name="OHT"
        modelUrl="/assets/oht.glb"
      />
    )

    await waitFor(() => {
      expect(container.querySelector('model-viewer')).toBeInTheDocument()
    })
  })

  it('无 URL 时显示占位', async () => {
    render(
      <ModelPreview
        name="OHT"
        modelUrl={null}
      />
    )

    await waitFor(() => {
      expect(screen.getByText('Preview unavailable')).toBeInTheDocument()
    })
  })

  it('加载失败时显示 error fallback', async () => {
    // vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 404 }))
    
    const { container } = render(
      <ModelPreview
        name="OHT"
        modelUrl="/assets/missing.glb"
      />
    )

    // Manually trigger error event as JSDOM model-viewer stub doesn't fetch
    const viewer = container.querySelector('model-viewer')
    if (viewer) {
      await new Promise(resolve => setTimeout(resolve, 0))
      viewer.dispatchEvent(new Event('error'))
    }

    await waitFor(() => {
      expect(screen.getByText('Preview failed')).toBeInTheDocument()
      expect(screen.getByText('Unable to render model file.')).toBeInTheDocument()
    })
  })
})
