import { t } from '@/shared/i18n'
import type { SceneDetail, Entity } from '@/types/api'

export interface SemanticValidationResult {
  isValid: boolean
  error?: string
  target?: {
    type: 'path' | 'segment' | 'point' | 'entity'
    id: string
    pathId?: string
  }
}

const EPSILON = 1e-6

const isZero = (value: number) => Math.abs(value) < EPSILON

const isSnappedToControlPoint = (entity: Entity, controlPoints: Entity[]): boolean => {
  for (const cp of controlPoints) {
    const dx = entity.position.x - cp.position.x
    const dy = entity.position.y - cp.position.y
    const dz = entity.position.z - cp.position.z
    // Simple distance check squared
    const distSq = dx * dx + dy * dy + dz * dz
    if (distSq <= EPSILON) {
      return true
    }
  }
  return false
}

export const validateNetworkSemantic = (scene: SceneDetail): SemanticValidationResult => {
  const { paths, entities } = scene

  // 1. Check Path Semantics
  for (const path of paths) {
    if (path.type === 'AGV_NETWORK') {
      // Check points Y=0
      for (const point of path.points) {
        if (!isZero(point.position.y)) {
          return {
            isValid: false,
            error: t('sceneEditor.validation.agvPointYInvalid'),
            target: { type: 'point', id: point.id, pathId: path.id }
          }
        }
      }

      // Check bezier control points Y=0
      if (path.segments) {
        for (const segment of path.segments) {
          if (segment.type === 'BEZIER' && segment.c1 && segment.c2) {
            if (!isZero(segment.c1.y) || !isZero(segment.c2.y)) {
              return {
                isValid: false,
                error: t('sceneEditor.validation.agvBezierYInvalid'),
                target: { type: 'segment', id: segment.id, pathId: path.id }
              }
            }
          }
        }
      }
    }
    // OHT_PATH allows y > 0, so no check needed for now
  }

  // 2. Check Vehicle Snapping
  // Note: AGV y=0 constraint is enforced during placement phase in SceneEditorPage.tsx
  // Here we maintain strict 3D snapping validation for consistency.
  const controlPoints = entities.filter(e => e.type === 'CONTROL_POINT')
  for (const entity of entities) {
    if (entity.type === 'AGV_VEHICLE' || entity.type === 'OHT_VEHICLE') {
      if (!isSnappedToControlPoint(entity, controlPoints)) {
        return {
          isValid: false,
          error: t('sceneEditor.validation.vehicleNotSnapped'),
          target: { type: 'entity', id: entity.id }
        }
      }
    }
  }

  return { isValid: true }
}
