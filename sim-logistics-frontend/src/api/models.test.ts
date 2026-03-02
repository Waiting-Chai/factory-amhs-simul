/**
 * modelsApi transform payload contract tests.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { modelsApi } from './models'

const postMock = vi.fn()

vi.mock('./client', () => ({
  default: {
    post: (...args: unknown[]) => postMock(...args),
  },
  unwrapEnvelope: (response: { data: { data: unknown } }) => response.data.data,
}))

describe('modelsApi transform payload contract', () => {
  beforeEach(() => {
    postMock.mockReset()
  })

  it('shouldSendObjectTransformInUploadPayload', async () => {
    postMock.mockResolvedValueOnce({
      data: {
        data: {
          modelId: 'model-1',
          versionId: 'version-1',
          version: '1.0.0',
          fileUrl: '/files/model.glb',
        },
      },
    })

    const file = new File(['glb'], 'model.glb', { type: 'model/gltf-binary' })
    await modelsApi.upload(
      file,
      'test-model',
      'MACHINE',
      {
        type: 'MACHINE',
        version: '1.0.0',
        dimensions: { width: 1, height: 1, depth: 1 },
        anchorPoint: { x: 0, y: 0, z: 0 },
        transform: {
          scale: { x: 1, y: 2, z: 3 },
          rotation: { x: 0.1, y: 0.2, z: 0.3 },
          pivot: { x: 4, y: 5, z: 6 },
        },
      }
    )

    const metadata = JSON.parse(String((postMock.mock.calls[0][1] as FormData).get('metadata')))
    expect(metadata.transform.scale).toEqual({ x: 1, y: 2, z: 3 })
    expect(metadata.transform.rotation).toEqual({ x: 0.1, y: 0.2, z: 0.3 })
    expect(metadata.transform.pivot).toEqual({ x: 4, y: 5, z: 6 })
  })

  it('shouldSendObjectTransformInVersionUploadPayload', async () => {
    postMock.mockResolvedValueOnce({
      data: {
        data: {
          versionId: 'version-2',
          version: '1.1.0',
          isDefault: false,
          status: 'ACTIVE',
          fileSize: 128,
          fileUrl: '/files/model-v2.glb',
          transform: {
            scale: { x: 1, y: 1, z: 1 },
            rotation: { x: 0, y: 0, z: 0 },
            pivot: { x: 0, y: 0, z: 0 },
          },
          createdAt: '2026-02-11T00:00:00',
        },
      },
    })

    const file = new File(['glb'], 'model-v2.glb', { type: 'model/gltf-binary' })
    await modelsApi.uploadVersion(
      'model-1',
      file,
      {
        type: 'MACHINE',
        version: '1.1.0',
        dimensions: { width: 1, height: 1, depth: 1 },
        anchorPoint: { x: 0, y: 0, z: 0 },
        transform: {
          scale: { x: 1, y: 1, z: 1 },
          rotation: { x: 0, y: 0, z: 0 },
          pivot: { x: 0, y: 0, z: 0 },
        },
      }
    )

    const metadata = JSON.parse(String((postMock.mock.calls[0][1] as FormData).get('metadata')))
    expect(metadata.transform.scale).toEqual({ x: 1, y: 1, z: 1 })
    expect(metadata.transform.rotation).toEqual({ x: 0, y: 0, z: 0 })
    expect(metadata.transform.pivot).toEqual({ x: 0, y: 0, z: 0 })
  })
})
