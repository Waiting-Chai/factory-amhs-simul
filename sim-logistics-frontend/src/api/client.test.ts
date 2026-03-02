/**
 * API client tests.
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-10
 */
import { describe, it, expect, vi } from 'vitest'
import { apiClient } from './client'

// Mock axios to avoid real network requests
vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
      post: vi.fn(() => Promise.resolve({ data: { data: {} } })),
      put: vi.fn(() => Promise.resolve({ data: { data: {} } })),
      delete: vi.fn(() => Promise.resolve({ data: { data: {} } })),
      defaults: {
        baseURL: '/api/v1',
        timeout: 30000,
        headers: { 'Content-Type': 'application/json' },
      },
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
    })),
  },
}))

describe('ApiClient', () => {
  it('should be defined', () => {
    expect(apiClient).toBeDefined()
  })

  it('should have correct default config', () => {
    expect(apiClient.defaults.baseURL).toBe('/api/v1')
    expect(apiClient.defaults.timeout).toBe(30000)
    expect(apiClient.defaults.headers['Content-Type']).toBe('application/json')
  })

  it('should make GET request successfully', async () => {
    const response = await apiClient.get('/scenes', {
      params: { page: 1, pageSize: 1 },
    })

    expect(response.data).toBeDefined()
  })
})
