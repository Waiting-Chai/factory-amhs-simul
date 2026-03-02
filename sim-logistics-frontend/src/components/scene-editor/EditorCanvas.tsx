import { useCallback, useMemo, useState, useRef, useEffect } from 'react'
import { Canvas, ThreeEvent, useThree } from '@react-three/fiber'
import { OrbitControls, Grid, GizmoHelper, GizmoViewport, Line, TransformControls } from '@react-three/drei'
import * as THREE from 'three'
import type { Entity, EntityType, Path, PathSegment } from '../../types/api'
import { EntityMesh } from './EntityMesh'

interface EditorCanvasProps {
  entities: Entity[]
  paths: Path[]
  mode: 'select' | 'place' | 'connect' | 'area'
  placingType: EntityType | null
  snapEnabled: boolean
  showGrid: boolean
  selectedEntityIds: string[]
  selectedSegmentIds: string[]
  pendingConnectNodeId: string | null
  pathSegmentType: 'LINEAR' | 'BEZIER'
  transformMode: 'translate' | 'rotate' | 'scale'
  onSelectEntity: (id: string | null) => void
  onSelectEntities: (ids: string[]) => void
  onAddToSelection: (ids: string[]) => void
  onSelectSegment: (id: string | null) => void
  onPlaceEntity: (type: EntityType, position: { x: number; y: number; z: number }) => void
  onConnectNode: (nodeId: string, segmentType: 'LINEAR' | 'BEZIER') => void
  onUpdateEntityTransform: (entity: Entity) => void
  onUpdateSegment: (pathId: string, segment: PathSegment) => void
}

const snapValue = (value: number): number => Math.round(value)

/**
 * Internal component to expose camera via ref for box selection projection.
 * Uses official @react-three/fiber useThree() hook.
 */
const CameraExposer = ({
  cameraRef,
  sizeRef,
}: {
  cameraRef: React.MutableRefObject<THREE.Camera | null>
  sizeRef: React.MutableRefObject<{ width: number; height: number } | null>
}) => {
  const { camera, size } = useThree()

  useEffect(() => {
    cameraRef.current = camera
    sizeRef.current = { width: size.width, height: size.height }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- refs are stable and don't need to be in deps
  }, [camera, size])

  return null
}

const ControlPointHandle = ({
  position,
  color,
  onDragEnd,
}: {
  position: THREE.Vector3
  color: string
  onDragEnd: (pos: THREE.Vector3) => void
}) => {
  const [hovered, setHovered] = useState(false)
  const meshRef = useRef<THREE.Mesh>(null)

  return (
    <>
      <mesh
        ref={meshRef}
        position={position}
        onPointerOver={() => setHovered(true)}
        onPointerOut={() => setHovered(false)}
      >
        <sphereGeometry args={[0.3, 16, 16]} />
        <meshBasicMaterial color={hovered ? '#ffffff' : color} />
      </mesh>
      {meshRef.current && (
        <TransformControls
          object={meshRef.current}
          mode="translate"
          onMouseUp={() => {
            if (meshRef.current) {
              onDragEnd(meshRef.current.position.clone())
            }
          }}
        />
      )}
    </>
  )
}

const PathMesh = ({
  path,
  selectedSegmentIds,
  onSelectSegment,
  onUpdateSegment
}: {
  path: Path
  selectedSegmentIds: Set<string>
  onSelectSegment: (id: string | null) => void
  onUpdateSegment: (pathId: string, segment: PathSegment) => void
}) => {
  if (!path.segments?.length) return null
  const pointsMap = new Map(path.points.map((point) => [point.id, point.position]))

  return (
    <group>
      {path.segments.map((segment) => {
        const from = pointsMap.get(segment.from)
        const to = pointsMap.get(segment.to)
        if (!from || !to) return null

        const isSelected = selectedSegmentIds.has(segment.id)

        if (segment.type === 'BEZIER' && segment.c1 && segment.c2) {
          const curve = new THREE.CubicBezierCurve3(
            new THREE.Vector3(from.x, from.y, from.z),
            new THREE.Vector3(segment.c1.x, segment.c1.y, segment.c1.z),
            new THREE.Vector3(segment.c2.x, segment.c2.y, segment.c2.z),
            new THREE.Vector3(to.x, to.y, to.z)
          )
          const points = curve.getPoints(32)

          return (
            <group key={segment.id}>
              <Line
                points={points}
                color={isSelected ? '#ffffff' : '#facc15'}
                lineWidth={isSelected ? 4 : 2}
                onClick={(e) => {
                  e.stopPropagation()
                  onSelectSegment(segment.id)
                }}
              />
              {isSelected && (
                <>
                  <Line
                    points={[
                      new THREE.Vector3(from.x, from.y, from.z),
                      new THREE.Vector3(segment.c1.x, segment.c1.y, segment.c1.z)
                    ]}
                    color="#666"
                    lineWidth={1}
                    dashed
                  />
                  <Line
                    points={[
                      new THREE.Vector3(to.x, to.y, to.z),
                      new THREE.Vector3(segment.c2.x, segment.c2.y, segment.c2.z)
                    ]}
                    color="#666"
                    lineWidth={1}
                    dashed
                  />
                  <ControlPointHandle
                    position={new THREE.Vector3(segment.c1.x, segment.c1.y, segment.c1.z)}
                    color="#ff0000"
                    onDragEnd={(pos) => {
                      const newC1 = { x: pos.x, y: pos.y, z: pos.z }
                      // Enforce Y=0 for AGV_NETWORK (checked by path type)
                      if (path.type === 'AGV_NETWORK') {
                        newC1.y = 0
                      }
                      onUpdateSegment(path.id, { ...segment, c1: newC1 })
                    }}
                  />
                  <ControlPointHandle
                    position={new THREE.Vector3(segment.c2.x, segment.c2.y, segment.c2.z)}
                    color="#00ff00"
                    onDragEnd={(pos) => {
                      const newC2 = { x: pos.x, y: pos.y, z: pos.z }
                      // Enforce Y=0 for AGV_NETWORK
                      if (path.type === 'AGV_NETWORK') {
                        newC2.y = 0
                      }
                      onUpdateSegment(path.id, { ...segment, c2: newC2 })
                    }}
                  />
                </>
              )}
            </group>
          )
        }

        const points = [
          new THREE.Vector3(from.x, from.y, from.z),
          new THREE.Vector3(to.x, to.y, to.z),
        ]

        return (
          <Line
            key={segment.id}
            points={points}
            color={isSelected ? '#ffffff' : '#3b82f6'}
            lineWidth={isSelected ? 4 : 2}
            onClick={(e) => {
              e.stopPropagation()
              onSelectSegment(segment.id)
            }}
          />
        )
      })}
    </group>
  )
}

export function EditorCanvas({
  entities,
  paths,
  mode,
  placingType,
  snapEnabled,
  showGrid,
  selectedEntityIds,
  selectedSegmentIds,
  pendingConnectNodeId,
  pathSegmentType,
  transformMode,
  onSelectEntity,
  onSelectEntities,
  onAddToSelection,
  onSelectSegment,
  onPlaceEntity,
  onConnectNode,
  onUpdateEntityTransform,
  onUpdateSegment,
}: EditorCanvasProps) {
  const [orbitEnabled, setOrbitEnabled] = useState(true)

  // Box selection state
  const [isBoxSelecting, setIsBoxSelecting] = useState(false)
  const [boxStart, setBoxStart] = useState<{ x: number; y: number } | null>(null)
  const [boxEnd, setBoxEnd] = useState<{ x: number; y: number } | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  // Camera refs for box selection projection (populated by CameraExposer inside Canvas)
  const cameraRef = useRef<THREE.Camera | null>(null)
  const sizeRef = useRef<{ width: number; height: number } | null>(null)

  // Track if pointer is over an entity (set by EntityMesh onPointerOver/Out)
  const pointerOverEntityRef = useRef(false)

  // Suppress ground click after box selection to prevent clearing selection
  const suppressNextGroundClickRef = useRef(false)

  // Minimum drag distance to qualify as box selection (in pixels)
  const BOX_SELECTION_THRESHOLD = 5

  const selectedSet = useMemo(() => new Set(selectedEntityIds), [selectedEntityIds])
  const selectedSegmentSet = useMemo(() => new Set(selectedSegmentIds), [selectedSegmentIds])

  // Create a stable entity key for dependency comparison
  const entityKey = useMemo(() => entities.map(e => e.id).join(','), [entities])

  // Reset pointerOverEntityRef when entities change to prevent stale state
  useEffect(() => {
    pointerOverEntityRef.current = false
  }, [entityKey])

  const handleEntityClick = useCallback((entity: Entity, shiftKey: boolean) => {
    if (mode === 'connect') {
      if (entity.type === 'CONTROL_POINT') {
        onConnectNode(entity.id, pathSegmentType)
      }
      onSelectEntity(entity.id)
      return
    }

    // Shift+click for incremental selection
    if (shiftKey) {
      if (selectedSet.has(entity.id)) {
        // Deselect if already selected
        const newIds = selectedEntityIds.filter(id => id !== entity.id)
        onSelectEntities(newIds)
      } else {
        onAddToSelection([entity.id])
      }
    } else {
      onSelectEntity(entity.id)
    }
  }, [mode, onConnectNode, onSelectEntity, pathSegmentType, selectedSet, selectedEntityIds, onSelectEntities, onAddToSelection])

  // Box selection handlers
  const handleMouseDown = useCallback((event: React.MouseEvent) => {
    // Only start box selection in select mode with left button
    if (mode !== 'select' || event.button !== 0) return
    // Don't start box selection if shift is held (reserved for incremental click selection)
    if (event.shiftKey) return
    // Don't start if pointer is over an entity (use ref to avoid closure issues)
    if (pointerOverEntityRef.current) return

    const rect = containerRef.current?.getBoundingClientRect()
    if (!rect) return

    setBoxStart({ x: event.clientX - rect.left, y: event.clientY - rect.top })
    setBoxEnd({ x: event.clientX - rect.left, y: event.clientY - rect.top })
    setIsBoxSelecting(true)
  }, [mode])

  const handleMouseMove = useCallback((event: React.MouseEvent) => {
    if (!isBoxSelecting || !boxStart) return

    const rect = containerRef.current?.getBoundingClientRect()
    if (!rect) return

    setBoxEnd({ x: event.clientX - rect.left, y: event.clientY - rect.top })
  }, [isBoxSelecting, boxStart])

  const handleMouseUp = useCallback((event: React.MouseEvent) => {
    // Always reset pointer state on mouse up to prevent stale state
    const resetPointerState = () => {
      pointerOverEntityRef.current = false
    }

    if (!isBoxSelecting || !boxStart || !boxEnd) {
      setIsBoxSelecting(false)
      setBoxStart(null)
      setBoxEnd(null)
      resetPointerState()
      return
    }

    // Calculate box bounds
    const minX = Math.min(boxStart.x, boxEnd.x)
    const maxX = Math.max(boxStart.x, boxEnd.x)
    const minY = Math.min(boxStart.y, boxEnd.y)
    const maxY = Math.max(boxStart.y, boxEnd.y)

    // Only process if box has meaningful size (avoid treating single click as box)
    if (maxX - minX < BOX_SELECTION_THRESHOLD && maxY - minY < BOX_SELECTION_THRESHOLD) {
      setIsBoxSelecting(false)
      setBoxStart(null)
      setBoxEnd(null)
      resetPointerState()
      return
    }

    // Get camera and size from refs (populated by CameraExposer)
    const camera = cameraRef.current
    const size = sizeRef.current

    if (!camera || !size) {
      setIsBoxSelecting(false)
      setBoxStart(null)
      setBoxEnd(null)
      resetPointerState()
      return
    }

    // Find entities within the box using screen projection
    const selectedIds: string[] = []
    entities.forEach(entity => {
      // Project entity world position to normalized device coordinates
      const pos = new THREE.Vector3(entity.position.x, entity.position.y, entity.position.z)
      pos.project(camera)

      // Convert NDC to screen coordinates relative to container
      const screenX = (pos.x + 1) / 2 * size.width
      const screenY = (-pos.y + 1) / 2 * size.height

      // Check if within selection box
      if (screenX >= minX && screenX <= maxX && screenY >= minY && screenY <= maxY) {
        selectedIds.push(entity.id)
      }
    })

    if (selectedIds.length > 0) {
      // Suppress next ground click to prevent clearing selection
      suppressNextGroundClickRef.current = true

      if (event.shiftKey) {
        // Shift: add to existing selection (union)
        onAddToSelection(selectedIds)
      } else {
        // No shift: replace selection
        onSelectEntities(selectedIds)
      }
    } else if (!event.shiftKey) {
      // Clicked empty area without shift: clear selection
      onSelectEntity(null)
    }

    setIsBoxSelecting(false)
    setBoxStart(null)
    setBoxEnd(null)
    resetPointerState()
  }, [isBoxSelecting, boxStart, boxEnd, entities, onSelectEntities, onAddToSelection, onSelectEntity])

  const handleGroundClick = useCallback((event: ThreeEvent<MouseEvent>) => {
    // Suppress ground click after box selection to prevent clearing selection
    if (suppressNextGroundClickRef.current) {
      suppressNextGroundClickRef.current = false
      return
    }

    // Don't handle ground click while box selecting
    if (isBoxSelecting) {
      return
    }

    if (mode === 'select') {
      // Clear selection when clicking ground
      onSelectEntity(null)
      onSelectSegment(null)
      return
    }

    if (mode !== 'place' || !placingType) return

    const rawX = event.point.x
    const rawZ = event.point.z
    const nextPosition = {
      x: snapEnabled ? snapValue(rawX) : rawX,
      y: 0,
      z: snapEnabled ? snapValue(rawZ) : rawZ,
    }
    onPlaceEntity(placingType, nextPosition)
  }, [mode, placingType, snapEnabled, onPlaceEntity, onSelectEntity, onSelectSegment, isBoxSelecting])

  return (
    <div
      ref={containerRef}
      className="w-full h-full bg-[#1a1a2e] relative"
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
    >
      <Canvas
        camera={{ position: [12, 10, 12], fov: 45 }}
        onPointerMissed={() => {
          // Reset pointer state when pointer misses all objects
          pointerOverEntityRef.current = false

          // Note: box selection handles clearing via mouseUp
          // This is a fallback for non-box-select scenarios
          if (mode === 'select' && !isBoxSelecting) {
            onSelectEntity(null)
          }
        }}
      >
        {/* Expose camera to parent via ref for box selection */}
        <CameraExposer cameraRef={cameraRef} sizeRef={sizeRef} />

        <color attach="background" args={['#1a1a2e']} />
        <ambientLight intensity={0.6} />
        <directionalLight position={[10, 14, 10]} intensity={1.1} />

        <OrbitControls makeDefault enabled={orbitEnabled && !isBoxSelecting} />
        {showGrid ? (
          <Grid
            infiniteGrid
            cellSize={1}
            cellThickness={0.5}
            sectionSize={5}
            sectionThickness={1}
            fadeDistance={80}
          />
        ) : null}

        <GizmoHelper alignment="bottom-right" margin={[80, 80]}>
          <GizmoViewport axisColors={['#ef4444', '#22c55e', '#3b82f6']} labelColor="white" />
        </GizmoHelper>

        <mesh
          rotation={[-Math.PI / 2, 0, 0]}
          position={[0, 0, 0]}
          onClick={handleGroundClick}
          visible={false}
        >
          <planeGeometry args={[400, 400]} />
          <meshBasicMaterial transparent opacity={0} />
        </mesh>

        {paths.map((path) => (
          <PathMesh
            key={path.id}
            path={path}
            selectedSegmentIds={selectedSegmentSet}
            onSelectSegment={onSelectSegment}
            onUpdateSegment={onUpdateSegment}
          />
        ))}

        {entities.map((entity) => (
          <EntityMesh
            key={entity.id}
            entity={entity}
            selected={selectedSet.has(entity.id)}
            pendingConnect={pendingConnectNodeId === entity.id}
            transformMode={transformMode}
            onSelect={handleEntityClick}
            onUpdateEntity={onUpdateEntityTransform}
            onDraggingChange={(dragging) => setOrbitEnabled(!dragging)}
            onPointerOverStateChange={(isOver) => {
              pointerOverEntityRef.current = isOver
            }}
          />
        ))}
      </Canvas>

      {/* Box selection rectangle overlay */}
      {isBoxSelecting && boxStart && boxEnd && (
        <div
          className="absolute pointer-events-none border-2 border-amber-500 bg-amber-500/10"
          style={{
            left: Math.min(boxStart.x, boxEnd.x),
            top: Math.min(boxStart.y, boxEnd.y),
            width: Math.abs(boxEnd.x - boxStart.x),
            height: Math.abs(boxEnd.y - boxStart.y),
          }}
        />
      )}
    </div>
  )
}
