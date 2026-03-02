/**
 * Test setup for sim-logistics-frontend.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'
import '@testing-library/jest-dom'

// Cleanup after each test
afterEach(() => {
  cleanup()
})
