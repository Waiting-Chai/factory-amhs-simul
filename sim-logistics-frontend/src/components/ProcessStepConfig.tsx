/**
 * Process Step Configuration component.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-12
 */

import React, { useState, useMemo, useCallback } from 'react'
import { ProcessStep, TransportType, Entity } from '@/types/api'
import { useSceneStore } from '@/store/sceneStore'
import { t, tf } from '@/shared/i18n'

interface ProcessStepConfigProps {
  sceneId: string
  onStepsChange: (steps: ProcessStep[]) => void
}

type EntitySelector = {
  selectedEntityIds: string[]
}

/**
 * Process Step Configuration Component
 */
export const ProcessStepConfig: React.FC<ProcessStepConfigProps> = ({ sceneId: _sceneId, onStepsChange }) => {
  const { currentScene } = useSceneStore()

  // Local state
  const [selectedStepId, setSelectedStepId] = useState<string | null>(null)
  const [entitySelectors, setEntitySelectors] = useState<Map<string, EntitySelector>>(new Map())

  // Get process steps from scene
  const steps = useMemo(() => {
    return currentScene?.processSteps || []
  }, [currentScene])

  // Get available entities from scene
  const availableEntities = useMemo(() => {
    return currentScene?.entities || []
  }, [currentScene])

  // Handle step selection
  const handleStepClick = useCallback((stepId: string) => {
    setSelectedStepId(stepId)
    // Initialize entity selector for this step
    const step = steps.find((s: ProcessStep) => s.id === stepId)
    if (step && !entitySelectors.has(stepId)) {
      const newSelector: EntitySelector = {
        selectedEntityIds: step.targetEntityIds || []
      }
      setEntitySelectors(prev => new Map(prev).set(stepId, newSelector))
    }
  }, [steps, entitySelectors])

  // Add new step
  const handleAddStep = useCallback(() => {
    const newStep: ProcessStep = {
      id: `step-${Date.now()}`,
      name: 'New Process Step',
      description: '',
      targetEntityIds: [],
      requiredTransportTypes: [],
      priority: 1,
      processFlowId: null
    }

    const updatedSteps = [...steps, newStep]
    onStepsChange(updatedSteps)
    handleStepClick(newStep.id)
  }, [steps, onStepsChange, handleStepClick])

  // Add step with single target
  const handleAddSingleTargetStep = useCallback(() => {
    if (availableEntities.length === 0) return

    const newStep: ProcessStep = {
      id: `step-${Date.now()}`,
      name: 'Single Target Step',
      description: '',
      targetEntityIds: [availableEntities[0].id],
      requiredTransportTypes: ['OHT'],
      priority: 1,
      processFlowId: null
    }

    const updatedSteps = [...steps, newStep]
    onStepsChange(updatedSteps)
    handleStepClick(newStep.id)
  }, [steps, availableEntities, onStepsChange, handleStepClick])

  // Add step with multi targets
  const handleAddMultiTargetStep = useCallback(() => {
    if (availableEntities.length < 2) return

    const targetCount = Math.min(3, availableEntities.length)
    const newStep: ProcessStep = {
      id: `step-${Date.now()}`,
      name: 'Multi-Target Step',
      description: '',
      targetEntityIds: availableEntities.slice(0, targetCount).map((e: Entity) => e.id),
      requiredTransportTypes: ['OHT', 'AGV'],
      priority: 1,
      processFlowId: null
    }

    const updatedSteps = [...steps, newStep]
    onStepsChange(updatedSteps)
    handleStepClick(newStep.id)
  }, [steps, availableEntities, onStepsChange, handleStepClick])

  // Delete step
  const handleDeleteStep = useCallback((stepId: string) => {
    const updatedSteps = steps.filter((s: ProcessStep) => s.id !== stepId)
    onStepsChange(updatedSteps)

    // Clear selector
    setEntitySelectors(prev => {
      const newMap = new Map(prev)
      newMap.delete(stepId)
      return newMap
    })

    if (selectedStepId === stepId) {
      setSelectedStepId(null)
    }
  }, [steps, onStepsChange, selectedStepId])

  // Update step property
  const updateStep = useCallback((stepId: string, property: string, value: unknown) => {
    const updatedSteps = steps.map((s: ProcessStep) =>
      s.id === stepId ? { ...s, [property]: value } : s
    )
    onStepsChange(updatedSteps)
  }, [steps, onStepsChange])

  // Handle entity selection change
  const handleSelectorEntityChange = useCallback((stepId: string, entityId: string) => {
    const selector = entitySelectors.get(stepId) || { selectedEntityIds: [] }
    const isSelected = selector.selectedEntityIds.includes(entityId)

    const updatedIds = isSelected
      ? selector.selectedEntityIds.filter((id: string) => id !== entityId)
      : [...selector.selectedEntityIds, entityId]

    const updatedSelector = { selectedEntityIds: updatedIds }
    setEntitySelectors(prev => new Map(prev).set(stepId, updatedSelector))

    // Update step
    updateStep(stepId, 'targetEntityIds', updatedIds)
  }, [entitySelectors, updateStep])

  // Handle transport type toggle
  const handleTransportTypeToggle = useCallback((stepId: string, transportType: TransportType) => {
    const step = steps.find((s: ProcessStep) => s.id === stepId)
    if (!step) return

    const current = step.requiredTransportTypes || []
    const isSelected = current.includes(transportType)
    const updated = isSelected
      ? current.filter((t: TransportType) => t !== transportType)
      : [...current, transportType]

    updateStep(stepId, 'requiredTransportTypes', updated)
  }, [steps, updateStep])

  if (!currentScene) {
    return <div className="flex items-center justify-center p-8">{t('processStep.loading')}</div>
  }

  return (
    <div className="bg-industrial-slate border border-industrial-steel/30 rounded-lg p-4">
      {/* Toolbar */}
      <div className="flex gap-2 mb-4">
        <button
          onClick={() => setSelectedStepId(null)}
          className={`px-3 py-2 rounded-lg transition-colors ${
            selectedStepId === null ? 'bg-amber-500 text-white' : 'bg-industrial-steel text-gray-300 hover:text-white'
          }`}
        >
          {t('processStep.select')}
        </button>
        <button
          onClick={handleAddStep}
          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg transition-colors font-bold"
        >
          {t('processStep.addStep')}
        </button>
        <button
          onClick={handleAddSingleTargetStep}
          disabled={availableEntities.length === 0}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50 font-bold"
        >
          {t('processStep.singleTarget')}
        </button>
        <button
          onClick={handleAddMultiTargetStep}
          disabled={availableEntities.length < 2}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50 font-bold"
        >
          {t('processStep.multiTarget')}
        </button>
      </div>

      {/* Steps list */}
      <div className="space-y-2 max-h-96 overflow-y-auto">
        {steps.map((step: ProcessStep) => {
          const selector = entitySelectors.get(step.id) || { selectedEntityIds: step.targetEntityIds || [] }
          const isSelected = selectedStepId === step.id

          return (
            <div
              key={step.id}
              className={`p-4 rounded-lg border transition-colors cursor-pointer ${
                isSelected ? 'border-amber-500 bg-amber-500/10' : 'border-industrial-steel/30 bg-industrial-dark hover:bg-industrial-steel'
              }`}
            >
              <div className="flex justify-between items-start">
                <div
                  className="flex-1"
                  onClick={() => handleStepClick(step.id)}
                >
                  <input
                    type="text"
                    value={step.name}
                    onChange={(e) => updateStep(step.id, 'name', e.target.value)}
                    className="bg-transparent border-none text-white text-sm font-medium w-full"
                    placeholder={t('processStep.placeholder.name')}
                  />
                  {selector.selectedEntityIds.length > 0 && (
                    <span className="text-xs text-emerald-400 ml-2">
                      {tf('processStep.entitiesCount', { count: selector.selectedEntityIds.length })}
                    </span>
                  )}
                </div>
                <button
                  onClick={() => handleDeleteStep(step.id)}
                  className="text-gray-400 hover:text-red-400 transition-colors ml-2"
                >
                  ✕
                </button>
              </div>

              {/* Expanded editor */}
              {isSelected && (
                <div className="mt-4 space-y-3">
                  {/* Description */}
                  <div>
                    <label className="text-gray-400 text-xs">{t('processStep.label.description')}</label>
                    <input
                      type="text"
                      value={step.description || ''}
                      onChange={(e) => updateStep(step.id, 'description', e.target.value)}
                      className="w-full bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
                      placeholder={t('processStep.placeholder.description')}
                    />
                  </div>

                  {/* Entity selector */}
                  <div>
                    <label className="text-gray-400 text-xs">{t('processStep.label.targetEntities')}</label>
                    <div className="flex flex-wrap gap-2 mt-1">
                      {availableEntities.map((entity: Entity) => {
                        const isSelected = selector.selectedEntityIds.includes(entity.id)

                        return (
                          <button
                            key={entity.id}
                            type="button"
                            onClick={() => handleSelectorEntityChange(step.id, entity.id)}
                            className={`px-2 py-1 rounded text-xs transition-colors ${
                              isSelected
                                ? 'bg-amber-500 text-white'
                                : 'bg-industrial-steel text-gray-300 hover:text-white'
                            }`}
                          >
                            {entity.id}
                          </button>
                        )
                      })}
                    </div>
                  </div>

                  {/* Required transport types */}
                  <div>
                    <label className="text-gray-400 text-xs">{t('processStep.label.requiredTransportTypes')}</label>
                    <div className="flex flex-wrap gap-2 mt-1">
                      {['OHT', 'AGV', 'CONVEYOR', 'STOCKER', 'MACHINE', 'HUMAN', 'ERACK'].map((type: string) => {
                        const isSelected = step.requiredTransportTypes?.includes(type as TransportType)

                        return (
                          <button
                            key={type}
                            type="button"
                            onClick={() => handleTransportTypeToggle(step.id, type as TransportType)}
                            className={`px-2 py-1 rounded text-xs transition-colors ${
                              isSelected
                                ? 'bg-emerald-600 text-white'
                                : 'bg-industrial-steel text-gray-300 hover:text-white'
                            }`}
                          >
                            {type}
                          </button>
                        )
                      })}
                    </div>
                  </div>

                  {/* Priority */}
                  <div className="flex items-center gap-2">
                    <label className="text-gray-400 text-xs">{t('processStep.label.priority')}</label>
                    <input
                      type="number"
                      min="1"
                      max="10"
                      value={step.priority}
                      onChange={(e) => updateStep(step.id, 'priority', parseInt(e.target.value) || 1)}
                      className="w-20 bg-industrial-steel border border-industrial-steel/30 rounded px-2 py-1 text-white text-sm"
                    />
                  </div>
                </div>
              )}
            </div>
          )
        })}
      </div>

      {/* Empty state */}
      {steps.length === 0 && (
        <div className="text-center text-gray-500 py-8">
          No process steps yet. Click "Add Step" to create one.
        </div>
      )}
    </div>
  )
}
