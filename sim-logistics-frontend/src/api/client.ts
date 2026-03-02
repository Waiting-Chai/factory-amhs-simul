/**
 * HTTP API client for sim-logistics-frontend.
 *
 * Handles REST API communication with backend, following frontend-contract.md
 * Supports mock mode for development without backend via axios adapter.
 *
 * @author shentw
 * @version 2.1
 * @since 2026-02-10
 */
import axios, { AxiosInstance, AxiosError, AxiosResponse } from 'axios'
import { USE_MOCK } from '../mock/config'
import { createMockAdapter } from '../mock/client'
import type {
  ApiEnvelope,
  ApiError,
} from '../types'

// Read API base URL from environment, fallback to default
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

/**
 * Create axios instance with default config
 */
const createClient = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: BASE_URL,
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  // Apply mock adapter at axios adapter level (NOT monkey patch)
  if (USE_MOCK) {
    const mockAdapter = createMockAdapter()
    instance.defaults.adapter = mockAdapter

    console.log('%c[API] mode=mock', 'color: #10b981; font-weight: bold;')
    console.log(`%c[API] baseURL=${BASE_URL}`, 'color: #10b981; font-weight: bold;')
  } else {
    console.log('%c[API] mode=backend', 'color: #3b82f6; font-weight: bold;')
    console.log(`%c[API] baseURL=${BASE_URL}`, 'color: #3b82f6; font-weight: bold;')

    // Real backend: add request/response interceptors
    instance.interceptors.request.use(
      (config) => {
        // Add auth token if available
        const token = localStorage.getItem('auth_token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // Response interceptor
    instance.interceptors.response.use(
      (response: AxiosResponse<ApiEnvelope>) => {
        return response
      },
      (error: AxiosError<ApiEnvelope>) => {
        if (error.response) {
          // Server responded with error
          const status = error.response.status
          const responseData = error.response.data

          // Try to extract code/message from backend envelope, fallback to HTTP status
          let code = responseData?.code
          let message = responseData?.message

          // Fallback: derive code from HTTP status if envelope missing
          if (!code) {
            if (status === 404) {
              code = 'NOT_FOUND'
            } else if (status === 401) {
              code = 'UNAUTHORIZED'
            } else if (status === 403) {
              code = 'FORBIDDEN'
            } else {
              code = `HTTP_${status}` || 'UNKNOWN_ERROR'
            }
          }

          // Fallback message
          if (!message) {
            message = responseData?.message || error.message || `HTTP ${status} error`
          }

          const apiError: ApiError = {
            status,
            code,
            message,
            details: responseData?.data as Record<string, unknown>,
            traceId: responseData?.traceId,
            timestamp: new Date().toISOString(),
          }
          return Promise.reject(apiError)
        } else if (error.request) {
          // Request made but no response
          const apiError: ApiError = {
            code: 'NETWORK_ERROR',
            message: 'Network error - please check your connection',
            timestamp: new Date().toISOString(),
          }
          return Promise.reject(apiError)
        } else {
          // Request setup error
          const apiError: ApiError = {
            code: 'REQUEST_ERROR',
            message: error.message || 'Failed to make request',
            timestamp: new Date().toISOString(),
          }
          return Promise.reject(apiError)
        }
      }
    )
  }

  return instance
}

/**
 * API client instance
 */
export const apiClient = createClient()

/**
 * Helper to extract data from ApiEnvelope
 */
export const unwrapEnvelope = <T>(response: AxiosResponse<ApiEnvelope<T>>): T => {
  return response.data.data
}

/**
 * Helper to handle blob responses (e.g., file download)
 */
export const downloadBlob = async (url: string, filename?: string): Promise<void> => {
  const response = await apiClient.get(url, {
    responseType: 'blob',
  })

  const blob = new Blob([response.data])
  const downloadUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = filename || 'download'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(downloadUrl)
}

/**
 * Helper to upload file as FormData
 */
export const uploadFile = async (
  url: string,
  file: File,
  onProgress?: (progress: number) => void
): Promise<AxiosResponse<ApiEnvelope>> => {
  const formData = new FormData()
  formData.append('file', file)

  return apiClient.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent) => {
      if (progressEvent.total && onProgress) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(progress)
      }
    },
  })
}

export default apiClient
