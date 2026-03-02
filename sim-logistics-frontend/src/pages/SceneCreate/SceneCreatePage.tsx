/**
 * Scene Create Page - 5.1 Project/Scene Management Module.
 *
 * Implements new scene creation with form validation.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import React, { useState, useCallback } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useSceneStore } from '@store/sceneStore'
import { useToastStore } from '@store/toastStore'
import { LoadingState } from '@components/ErrorState'
import { useI18n } from '../../shared/i18n/useI18n'

/**
 * Scene create page component
 */
const SceneCreatePage: React.FC = () => {
  const navigate = useNavigate()
  const { addToast } = useToastStore()
  const { t, tf } = useI18n()

  const { createScene, isLoading } = useSceneStore()

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [errors, setErrors] = useState<{ name?: string }>({})

  // Validate form
  const validateForm = useCallback((): boolean => {
    const newErrors: { name?: string } = {}

    if (!name.trim()) {
      newErrors.name = t('sceneCreate.validation.nameRequired')
    } else if (name.length < 3) {
      newErrors.name = t('sceneCreate.validation.nameMinLength')
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }, [name, t])

  // Handle create
  const handleCreate = useCallback(async () => {
    if (!validateForm()) {
      return
    }

    try {
      const scene = await createScene({
        name: name.trim(),
        description: description.trim() || undefined,
      })

      addToast('success', tf('sceneCreate.toast.created', { name: scene.name }))
      navigate(`/scenes/${scene.sceneId}/edit`)
    } catch (err) {
      addToast('error', t('sceneCreate.toast.createFailed'))
    }
  }, [name, description, validateForm, createScene, addToast, navigate, t, tf])

  // Handle cancel
  const handleCancel = useCallback(() => {
    if (name || description) {
      if (confirm(t('sceneCreate.confirm.cancel'))) {
        navigate('/scenes')
      }
    } else {
      navigate('/scenes')
    }
  }, [name, description, navigate, t])

  return (
    <div className="min-h-screen bg-gradient-to-br from-industrial-dark via-industrial-slate to-industrial-blue">
      {/* Header */}
      <header className="border-b border-industrial-steel/30 bg-industrial-dark/50 backdrop-blur sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <Link
                to="/scenes"
                className="text-gray-400 hover:text-white transition-colors"
              >
                ←
              </Link>
              <div>
                <h1 className="text-2xl font-display font-bold text-white">
                  {t('sceneCreate.title')}
                </h1>
                <p className="text-gray-500 text-sm">
                  {t('sceneCreate.subtitle')}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={handleCancel}
                className="px-4 py-2 text-gray-400 hover:text-white transition-colors"
              >
                {t('sceneCreate.cancel')}
              </button>
              <button
                onClick={handleCreate}
                disabled={isLoading || !name.trim()}
                className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-industrial-dark rounded-lg transition-colors font-bold disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? t('sceneCreate.creating') : t('sceneCreate.create')}
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Loading overlay */}
      {isLoading && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur flex items-center justify-center z-50">
          <LoadingState message={t('sceneCreate.creatingOverlay')} />
        </div>
      )}

      {/* Form content */}
      <main className="max-w-3xl mx-auto px-6 py-8">
        <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-6">
          <h2 className="text-xl font-display font-bold text-white mb-6">
            {t('sceneCreate.infoTitle')}
          </h2>

          <div className="space-y-6">
            {/* Name field */}
            <div>
              <label className="block text-gray-400 text-sm mb-2">
                {t('sceneCreate.nameLabel')}
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => {
                  setName(e.target.value)
                  if (errors.name) setErrors({})
                }}
                className={`w-full px-4 py-2 bg-industrial-dark border rounded-lg text-white placeholder-gray-500 focus:outline-none transition-colors ${
                  errors.name
                    ? 'border-red-500/50 focus:border-red-500/50'
                    : 'border-industrial-steel/30 focus:border-amber-500/50'
                }`}
                placeholder={t('sceneCreate.namePlaceholder')}
              />
              {errors.name && (
                <p className="text-red-400 text-xs mt-1">{errors.name}</p>
              )}
            </div>

            {/* Description field */}
            <div>
              <label className="block text-gray-400 text-sm mb-2">
                {t('sceneCreate.descriptionLabel')}
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={6}
                className="w-full px-4 py-2 bg-industrial-dark border border-industrial-steel/30 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-amber-500/50 transition-colors resize-none"
                placeholder={t('sceneCreate.descriptionPlaceholder')}
              />
            </div>

            {/* Help text */}
            <div className="pt-6 border-t border-industrial-steel/30">
              <p className="text-gray-500 text-sm">
                {t('sceneCreate.helpText')}
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}

export default SceneCreatePage
