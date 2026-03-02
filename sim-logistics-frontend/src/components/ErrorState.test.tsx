/**
 * ErrorState tests.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ErrorState, LoadingState, EmptyState } from './ErrorState'
import { setLocale } from '../shared/i18n'

describe('ErrorState i18n', () => {
  beforeEach(() => {
    localStorage.clear()
    setLocale('en')
  })

  it('shouldRenderDefaultTextsInEnglish', () => {
    render(<ErrorState message="Network failed" onRetry={() => undefined} />)
    expect(screen.getByText('Error')).toBeInTheDocument()
    expect(screen.getByText('Retry')).toBeInTheDocument()
  })

  it('shouldRenderLoadingAndEmptyDefaultTextsFromI18n', () => {
    render(
      <>
        <LoadingState />
        <EmptyState />
      </>
    )
    expect(screen.getByText('Loading...')).toBeInTheDocument()
    expect(screen.getByText('No data found')).toBeInTheDocument()
    expect(screen.getByText('There are no items to display.')).toBeInTheDocument()
  })
})
