/**
 * Transport Type Validator utility functions.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-12
 */

import { ProcessStep, TransportType, Entity } from '@/types/api'
import { tf } from '@/shared/i18n'

/**
 * Result of transport type compatibility check
 */
export interface TransportTypeCompatibilityResult {
  compatible: TransportType[]
  missing: TransportType[]
  conflicts: TransportType[]
}

/**
 * Get compatible transport types for entities in a step
 */
export const getCompatibleTransportTypes = (
  step: ProcessStep,
  allEntities: Entity[]
): TransportTypeCompatibilityResult => {
  if (!step || !step.targetEntityIds || step.targetEntityIds.length === 0) {
    return { compatible: [], missing: [], conflicts: [] }
  }

  const targetEntities = allEntities.filter((e: Entity) => step.targetEntityIds.includes(e.id))

  // Get all transport types required by target entities
  const requiredTypes = new Set<TransportType>()
  targetEntities.forEach((entity: Entity) => {
    if (entity.supportedTransportTypes) {
      entity.supportedTransportTypes.forEach((type: TransportType) => requiredTypes.add(type))
    }
  })

  // Get all transport types provided by source entities in step
  const providedTypes = new Set<TransportType>()
  step.targetEntityIds.forEach((id: string) => {
    const entity = allEntities.find((e: Entity) => e.id === id)
    if (entity && entity.supportedTransportTypes) {
      entity.supportedTransportTypes.forEach((type: TransportType) => providedTypes.add(type))
    }
  })

  // Find intersection: types that are required by target but NOT provided by source
  const missingTypes = [...requiredTypes].filter((type: TransportType) => !providedTypes.has(type))

  return {
    compatible: Array.from(requiredTypes),
    missing: missingTypes,
    conflicts: missingTypes
  }
}

/**
 * Check transport type compatibility when saving scene
 * Returns error message if incompatible, null if compatible
 */
export const validateTransportTypeCompatibility = (
  steps: ProcessStep[],
  allEntities: Entity[]
): { errorMessage?: string; isCompatible: boolean } => {
  for (const step of steps) {
    const result = getCompatibleTransportTypes(step, allEntities)

    if (result.conflicts.length > 0) {
      return {
        isCompatible: false,
        errorMessage: tf('validation.transportTypeConflict', {
          stepName: step.name,
          missingTypes: result.missing.join(', ')
        })
      }
    }
  }

  return { isCompatible: true, errorMessage: undefined }
}
