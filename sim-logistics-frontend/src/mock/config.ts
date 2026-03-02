/**
 * Mock environment configuration for sim-logistics-frontend.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */

// Mock feature flag - controlled by environment variable
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

// Mock delay configuration (ms) - simulates network latency
export const MOCK_DELAY = {
  MIN: 100,
  MAX: 500,
}

// Mock error rate (0-1) - for testing error states
export const MOCK_ERROR_RATE = 0.0

// Random delay helper
export const randomDelay = (): number => {
  return Math.floor(Math.random() * (MOCK_DELAY.MAX - MOCK_DELAY.MIN) + MOCK_DELAY.MIN)
}

// Should return error (based on error rate)
export const shouldError = (): boolean => {
  return Math.random() < MOCK_ERROR_RATE
}
