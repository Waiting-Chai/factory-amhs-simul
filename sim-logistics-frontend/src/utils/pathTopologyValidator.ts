import { t } from '@/shared/i18n'
import type { SceneDetail } from '@/types/api'

export interface TopologyValidationResult {
  isValid: boolean
  error?: string
}

export const validatePathTopology = (scene: SceneDetail): TopologyValidationResult => {
  const { paths } = scene

  if (!paths || paths.length === 0) {
    return { isValid: true }
  }

  for (const path of paths) {
    if (!path.segments || path.segments.length === 0) {
      continue // Empty path is valid but ignored
    }

    const pointIds = new Set(path.points.map(p => p.id))
    const segmentKeys = new Set<string>()
    const usedPoints = new Set<string>()

    for (const segment of path.segments) {
      // Rule 1: from/to must exist in points
      if (!pointIds.has(segment.from) || !pointIds.has(segment.to)) {
        return {
          isValid: false,
          error: t('sceneEditor.validation.missingPoint')
        }
      }

      usedPoints.add(segment.from)
      usedPoints.add(segment.to)

      // Rule 3: No self-loop
      if (segment.from === segment.to) {
        return {
          isValid: false,
          error: t('sceneEditor.validation.selfLoop')
        }
      }

      // Rule 2: No duplicate directed segments
      const key = `${segment.from}-${segment.to}-${segment.type}`
      if (segmentKeys.has(key)) {
        return {
          isValid: false,
          error: t('sceneEditor.validation.duplicateSegment')
        }
      }
      segmentKeys.add(key)
    }

    // Rule 4: Check for orphan points (points not used in any segment)
    for (const pointId of pointIds) {
      if (!usedPoints.has(pointId)) {
        return {
          isValid: false,
          error: t('sceneEditor.validation.orphanPoint')
        }
      }
    }
  }

  return { isValid: true }
}
