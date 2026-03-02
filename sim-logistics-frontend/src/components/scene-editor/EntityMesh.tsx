import { Suspense, useEffect, useMemo, useRef, useState } from 'react'
import { TransformControls, useGLTF } from '@react-three/drei'
import { ErrorBoundary } from 'react-error-boundary'
import type { Entity } from '../../types/api'
import * as THREE from 'three'
import { useEntityModel, type ResolvedEntityModel } from '../../hooks/useEntityModel'
import { useGlbDiagStore } from '@store/glbDiagStore'

interface EntityMeshProps {
  entity: Entity
  selected: boolean
  pendingConnect: boolean
  transformMode: 'translate' | 'rotate' | 'scale'
  onSelect: (entity: Entity, shiftKey: boolean) => void
  onUpdateEntity: (entity: Entity) => void
  onDraggingChange: (dragging: boolean) => void
  onPointerOverStateChange?: (isOver: boolean) => void
}

const getEntityColor = (type: Entity['type']): string => {
  switch (type) {
    case 'OHT_VEHICLE': return '#f59e0b'
    case 'AGV_VEHICLE': return '#10b981'
    case 'SAFETY_ZONE': return '#ef4444'
    case 'CONTROL_POINT': return '#22d3ee'
    case 'STOCKER': return '#3b82f6'
    case 'ERACK': return '#8b5cf6'
    case 'MANUAL_STATION': return '#ec4899'
    case 'CONVEYOR': return '#6366f1'
    case 'MACHINE': return '#14b8a6'
    default: return '#60a5fa'
  }
}

const PlaceholderGeometry = ({ type }: { type: Entity['type'] }) => {
  const color = useMemo(() => getEntityColor(type), [type])
  
  // Align with PalettePanel fallback logic
  let geometry
  switch (type) {
    case 'CONTROL_POINT':
      geometry = (
        <mesh>
          <sphereGeometry args={[0.22, 16, 16]} />
          <meshStandardMaterial color={color} />
        </mesh>
      )
      break
    case 'CONVEYOR':
      geometry = (
        <mesh rotation={[0, 0, Math.PI / 2]}>
          <cylinderGeometry args={[0.4, 0.4, 1, 16]} />
          <meshStandardMaterial color={color} />
        </mesh>
      )
      break
    case 'SAFETY_ZONE':
      geometry = (
        <mesh rotation={[-Math.PI / 2, 0, 0]}>
          <planeGeometry args={[1, 1]} />
          <meshStandardMaterial color={color} />
        </mesh>
      )
      break
    default:
      geometry = (
        <mesh>
          <boxGeometry args={[0.8, 0.8, 0.8]} />
          <meshStandardMaterial color={color} />
        </mesh>
      )
  }

  return geometry
}

const ModelContent = ({ url, metadata, entityId, modelId, versionId }: {
  url: string
  metadata?: ResolvedEntityModel['metadata']
  entityId: string
  modelId: string
  versionId: string
}) => {
  const { scene } = useGLTF(url)
  const loggedRef = useRef(false)

  useEffect(() => {
    useGlbDiagStore.getState().pushDiag('GLTF_LOAD_START', { url })
  }, [url])

  const clonedScene = useMemo(() => {
    if (!scene) return null
    return scene.clone()
  }, [scene])

  // Calculate and apply transform based on metadata
  const transformData = useMemo(() => {
    if (!metadata) return null

    const scale = metadata.transform?.scale ?? { x: 1, y: 1, z: 1 }
    const anchor = metadata.anchorPoint ?? { x: 0, y: 0, z: 0 }
    // Task B: Apply rotation from metadata (radians)
    const rotation = metadata.transform?.rotation ?? { x: 0, y: 0, z: 0 }

    return {
      scale: [scale.x, scale.y, scale.z] as [number, number, number],
      anchor: [-anchor.x, -anchor.y, -anchor.z] as [number, number, number],
      rotation: [rotation.x, rotation.y, rotation.z] as [number, number, number]
    }
  }, [metadata])

  // Optional auto-normalization (disabled by default)
  const autoNormalize = useMemo(() => {
    return localStorage.getItem('editor.autoNormalizeModel') === '1'
  }, [])

  // Compute bounding box and log diagnostics
  useEffect(() => {
    if (!clonedScene) return

    const box = new THREE.Box3().setFromObject(clonedScene)
    const size = box.getSize(new THREE.Vector3())
    const rawBBox = { width: size.x, height: size.y, depth: size.z }

    const appliedScale = metadata?.transform?.scale ?? { x: 1, y: 1, z: 1 }
    const appliedRotation = metadata?.transform?.rotation ?? { x: 0, y: 0, z: 0 }
    const anchor = metadata?.anchorPoint ?? { x: 0, y: 0, z: 0 }

    // Estimate final world size
    const finalWorldSize = {
      width: rawBBox.width * appliedScale.x,
      height: rawBBox.height * appliedScale.y,
      depth: rawBBox.depth * appliedScale.z
    }

    // Log only once per model load
    if (!loggedRef.current) {
      loggedRef.current = true
      useGlbDiagStore.getState().pushDiag('MODEL_TRANSFORM_APPLIED', {
        entityId,
        modelId,
        versionId,
        details: {
          rawBBox,
          appliedScale,
          appliedRotation,
          anchor,
          finalWorldSize,
          autoNormalize
        }
      })
    }
  }, [clonedScene, metadata, entityId, modelId, versionId, autoNormalize])

  // Task D: Load success
  // Only log once per url
  useEffect(() => {
    if (!clonedScene) return
    useGlbDiagStore.getState().pushDiag('GLTF_LOAD_SUCCESS', { url })
  }, [url, clonedScene])

  if (!clonedScene) {
    return null
  }

  // Build transform chain:
  // 1. Anchor offset (position sub anchor or group inner offset)
  // 2. Scale from metadata
  // 3. Rotation from metadata (optional)
  const anchorOffset = transformData?.anchor ?? [0, 0, 0]
  const scaleValue = transformData?.scale ?? [1, 1, 1]
  const rotationValue = transformData?.rotation ?? [0, 0, 0]

  return (
    <group position={anchorOffset} scale={scaleValue} rotation={rotationValue}>
      <primitive object={clonedScene} />
    </group>
  )
}

export function EntityMesh({
  entity,
  selected,
  pendingConnect,
  transformMode,
  onSelect,
  onUpdateEntity,
  onDraggingChange,
  onPointerOverStateChange,
}: EntityMeshProps) {
  const resolved = useEntityModel(entity)
  const [objectRef, setObjectRef] = useState<THREE.Group | null>(null)
  const isTransformable = selected && entity.type !== 'CONTROL_POINT' && entity.type !== 'SAFETY_ZONE'

  const ringColor = pendingConnect ? '#facc15' : '#ffffff'

  // Task C: Mount logging
  useEffect(() => {
    useGlbDiagStore.getState().pushDiag('ENTITY_MESH_MOUNTED', {
      entityId: entity.id,
      details: {
        entityType: entity.type,
        hasModelUrl: !!resolved?.url
      }
    })

    if (!resolved?.url) {
      useGlbDiagStore.getState().pushDiag('ENTITY_MESH_NO_URL', {
        entityId: entity.id,
        details: { entityType: entity.type }
      })
    }
  }, [entity.id, entity.type, resolved?.url])

  const handleTransformEnd = () => {
    if (!objectRef) return
    onUpdateEntity({
      ...entity,
      position: { x: objectRef.position.x, y: objectRef.position.y, z: objectRef.position.z },
      rotation: { x: objectRef.rotation.x, y: objectRef.rotation.y, z: objectRef.rotation.z },
    })
  }

  return (
    <>
      <group
        ref={setObjectRef}
        position={[entity.position.x, entity.position.y, entity.position.z]}
        rotation={[entity.rotation?.x ?? 0, entity.rotation?.y ?? 0, entity.rotation?.z ?? 0]}
        onClick={(event) => {
          event.stopPropagation()
          onSelect(entity, event.nativeEvent.shiftKey)
        }}
        onPointerOver={(event) => {
          event.stopPropagation()
          onPointerOverStateChange?.(true)
        }}
        onPointerOut={(event) => {
          event.stopPropagation()
          onPointerOverStateChange?.(false)
        }}
      >
        <ErrorBoundary
          fallbackRender={({ error }: { error: unknown }) => {
            useGlbDiagStore.getState().pushDiag('GLTF_LOAD_FAILED', {
              url: resolved?.url || 'unknown',
              details: { error: error instanceof Error ? error.message : String(error) }
            })
            return <PlaceholderGeometry type={entity.type} />
          }}
        >
          <Suspense fallback={<PlaceholderGeometry type={entity.type} />}>
            {resolved?.url ? (
              <ModelContent
                url={resolved.url}
                metadata={resolved.metadata}
                entityId={entity.id}
                modelId={resolved.modelId}
                versionId={resolved.versionId}
              />
            ) : (
              <PlaceholderGeometry type={entity.type} />
            )}
          </Suspense>
        </ErrorBoundary>

        {selected || pendingConnect ? (
          <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.02, 0]}>
            <ringGeometry args={[0.45, 0.55, 32]} />
            <meshBasicMaterial color={ringColor} transparent opacity={0.9} />
          </mesh>
        ) : null}
      </group>

      {isTransformable && objectRef ? (
        <TransformControls
          object={objectRef}
          mode={transformMode}
          onMouseDown={() => onDraggingChange(true)}
          onMouseUp={() => {
            onDraggingChange(false)
            handleTransformEnd()
          }}
        />
      ) : null}
    </>
  )
}
