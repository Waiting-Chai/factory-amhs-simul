/**
 * API types for sim-logistics-frontend.
 *
 * Defines the contract between frontend and backend based on frontend-contract.md
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */

// ============================================================================
// Common Types
// ============================================================================

/**
 * Unified API response envelope
 */
export interface ApiEnvelope<T = unknown> {
  code: string
  message: string
  data: T
  traceId?: string
}

/**
 * API error structure
 *
 * status: HTTP status code (404, 500, etc.)
 * code: Error code from backend envelope or derived from HTTP status
 * message: Human-readable error message
 * traceId: Backend trace ID for debugging
 * details: Additional error details
 * timestamp: Error timestamp
 */
export interface ApiError {
  status?: number
  code: string
  message: string
  traceId?: string
  details?: Record<string, unknown>
  timestamp?: string
}

/**
 * Paged result wrapper
 */
export interface PagedResult<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// ============================================================================
// Position & Geometry (Unit: meters, radians: [-pi, pi])
// ============================================================================

/**
 * 3D Position - unit: meters
 */
export interface Position {
  x: number
  y: number
  z: number
}

/**
 * Rotation - unit: radians, range: [-pi, pi]
 */
export interface Rotation {
  x: number
  y: number
  z: number
}

/**
 * 3D Transform
 */
export interface Transform {
  position: Position
  rotation: Rotation
  scale: Position
  pivot?: Position
}

// ============================================================================
// Scene Types (REQ-WEB-001, REQ-WEB-014, REQ-WEB-015)
// ============================================================================

/**
 * Scene summary for list view
 */
export interface SceneSummary {
  sceneId: string
  name: string
  description?: string
  version: number
  createdAt: string
  updatedAt: string
  entityCount: number
}

/**
 * Scene detail
 */
export interface SceneDetail {
  sceneId: string
  name: string
  description?: string
  version: number
  createdAt: string
  updatedAt: string
  entities: Entity[]
  paths: Path[]
  processSteps?: ProcessStep[]
  safetyZones?: SafetyZone[]
  processFlows?: ProcessFlowBinding[]
}

/**
 * Process step
 */
export interface ProcessStep {
  id: string
  name: string
  description?: string
  targetEntityIds: string[]
  requiredTransportTypes: TransportType[]
  priority: number
  processFlowId: string | null
}

/**
 * Safety zone
 */
export interface SafetyZone {
  id: string
  type: 'rectangle' | 'circle' | 'polygon'
  priority: 'HUMAN_FIRST' | 'VEHICLE_FIRST' | 'FIFO' | 'PRIORITY_BASED'
  maxHumans: number
  maxVehicles: number
  position: { x: number; y: number }
  width?: number
  height?: number
  radius?: number
  points?: { x: number; y: number }[]
}

/**
 * Entity base
 */
export interface Entity {
  id: string
  type: EntityType
  name?: string
  position: Position
  rotation?: Rotation
  properties?: Record<string, unknown>
  supportedTransportTypes?: TransportType[]
}

/**
 * Entity types
 */
export type EntityType =
  | 'OHT_VEHICLE'
  | 'AGV_VEHICLE'
  | 'STOCKER'
  | 'ERACK'
  | 'MANUAL_STATION'
  | 'CONVEYOR'
  | 'OPERATOR'
  | 'MACHINE'
  | 'CONTROL_POINT'
  | 'SAFETY_ZONE'
  | 'BAY'
  | 'CHUTE'

/**
 * Transport-capable entity (with supportedTransportTypes)
 */
export interface TransportCapableEntity extends Entity {
  supportedTransportTypes?: TransportType[]
}

/**
 * Transport types
 */
export type TransportType = 'OHT' | 'AGV' | 'HUMAN' | 'CONVEYOR'

/**
 * Path definition
 */
export interface Path {
  id: string
  type: PathType
  name?: string
  points: PathPoint[]
  segments?: PathSegment[]
  controlPoints?: ControlPoint[]
}

/**
 * Path types
 */
export type PathType = 'OHT_PATH' | 'AGV_NETWORK'

/**
 * Path point
 */
export interface PathPoint {
  id: string
  position: Position
}

/**
 * Path segment (v2.0 - supports LINEAR and BEZIER)
 */
export interface PathSegment {
  id: string
  type: 'LINEAR' | 'BEZIER'
  from: string  // References point ID
  to: string    // References point ID
  c1?: Position // Control point 1 for BEZIER (absolute coordinates, meters)
  c2?: Position // Control point 2 for BEZIER (absolute coordinates, meters)
}

/**
 * Bezier segment (for OHT track editor)
 */
export interface BezierSegment {
  id: string
  type: 'LINEAR' | 'BEZIER'
  from: string
  to: string
  c1: { x: number; y: number }
  c2: { x: number; y: number }
  controlPoints: { id: string; x: number; y: number }[]
}

/**
 * Control point
 */
export interface ControlPoint {
  id: string
  at: string    // References point ID
  capacity: number
  priority?: number
}

/**
 * Process flow binding
 */
export interface ProcessFlowBinding {
  flowId: string
  flowVersion: string
  entryPointId: string
  enabled: boolean
  priority: number
  triggerCondition?: TriggerCondition
}

/**
 * Trigger condition
 */
export interface TriggerCondition {
  entryPointId: string
  materialType?: string
  materialGrade?: string
  materialBatch?: string
  materialAttributes?: Record<string, string>
  processTag?: string
  allowedProcessTags?: string[]
}

/**
 * Scene draft (REQ-WEB-015, REQ-WEB-025)
 */
export interface SceneDraftPayload {
  sceneId: string
  content: SceneDetail
  savedAt: string
  version: number
}

/**
 * Scene draft save result
 */
export interface SceneDraftSaveResult {
  success: boolean
  savedAt: string
}

/**
 * Scene copy result
 */
export interface SceneCopyResult {
  newSceneId: string
  name: string
  version: number
}

/**
 * Scene import result
 */
export interface SceneImportResult {
  sceneId: string
  name: string
  version: number
  warnings?: string[]
}

// ============================================================================
// Model Types (REQ-WEB-009, REQ-WEB-010)
// ============================================================================

/**
 * Model summary
 */
export interface ModelSummary {
  modelId: string
  name: string
  type: ModelType
  defaultVersion: string
  status: ModelStatus
  versions: ModelVersionSummary[]
  thumbnailUrl?: string
  createdAt: string
  updatedAt: string
}

/**
 * Model types
 */
export type ModelType =
  | 'OHT_VEHICLE'
  | 'AGV_VEHICLE'
  | 'STOCKER'
  | 'ERACK'
  | 'MANUAL_STATION'
  | 'CONVEYOR'
  | 'OPERATOR'
  | 'MACHINE'
  | 'BAY'
  | 'CHUTE'
  | 'SAFETY_ZONE'

/**
 * Model detail
 */
export interface ModelDetail {
  modelId: string
  name: string
  type: ModelType
  description?: string
  defaultVersion: string
  versions: ModelVersion[]
  createdAt: string
  updatedAt: string
}

/**
 * Model version summary
 */
export interface ModelVersionSummary {
  versionId: string
  version: string
  isDefault: boolean
  status: ModelStatus
  fileSize: number
  createdAt: string
}

/**
 * Model version detail
 */
export interface ModelVersion {
  versionId: string
  version: string
  isDefault: boolean
  status: ModelStatus
  fileSize: number
  fileUrl: string
  thumbnailUrl?: string
  metadata: ModelMetadata
  createdAt: string
}

/**
 * Model status
 */
export type ModelStatus = 'ACTIVE' | 'DISABLED' | 'PENDING'

/**
 * Model metadata (REQ-WEB-019)
 */
export interface ModelMetadata {
  type: ModelType
  version: string
  dimensions: {
    width: number   // meters
    height: number  // meters
    depth: number   // meters
  }
  anchorPoint: Position  // meters
  transform?: {
    scale: Position
    rotation: Rotation  // radians, [-pi, pi]
    pivot?: Position    // meters
  }
}

/**
 * Model upload result
 */
export interface ModelUploadResult {
  modelId: string
  versionId: string
  version: string
  fileUrl: string
}

/**
 * Entity model binding (REQ-WEB-010)
 */
export interface EntityModelBinding {
  entityId: string
  modelId: string
  versionId: string
}

// ============================================================================
// Simulation Types (REQ-WEB-001)
// ============================================================================

/**
 * Simulation request
 */
export interface SimulationRequest {
  sceneId: string
  config: SimulationConfig
  name?: string
}

/**
 * Simulation config
 */
export interface SimulationConfig {
  duration: number      // seconds (simulation time)
  timeScale: number     // 1.0 = real-time, 10.0 = 10x speed
  seed?: number
}

/**
 * Simulation detail
 */
export interface SimulationDetail {
  simulationId: string
  sceneId: string
  name?: string
  status: SimulationStatus
  config: SimulationConfig
  startedAt?: string
  stoppedAt?: string
  simulatedTime: number  // seconds
}

/**
 * Simulation status
 */
export type SimulationStatus =
  | 'CREATED'
  | 'RUNNING'
  | 'PAUSED'
  | 'STOPPED'
  | 'COMPLETED'
  | 'FAILED'

/**
 * Simulation speed request
 */
export interface SetSpeedRequest {
  timeScale: number
}

// ============================================================================
// Entity Runtime Types (REQ-WEB-001)
// ============================================================================

/**
 * Entity state
 */
export type EntityState =
  | 'IDLE'
  | 'MOVING'
  | 'LOADING'
  | 'UNLOADING'
  | 'CHARGING'
  | 'BLOCKED'
  | 'ERROR'

/**
 * Entity runtime state
 */
export interface EntityRuntimeState {
  id: string
  type: EntityType
  state: EntityState
  position: Position
  rotation?: Rotation
  currentTask?: string
  path?: string[]
  battery?: number
  load?: number
  speed?: number
  utilization?: number
}

/**
 * Vehicle runtime state
 */
export interface VehicleState extends EntityRuntimeState {
  type: 'OHT_VEHICLE' | 'AGV_VEHICLE'
  battery: number
  currentLoad: number
  maxLoad: number
}

// ============================================================================
// Task Types (REQ-WEB-001, REQ-WEB-007)
// ============================================================================

/**
 * Task priority
 */
export type TaskPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'

/**
 * Task status
 */
export type TaskStatus = 'PENDING' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

/**
 * Task detail
 */
export interface TaskDetail {
  taskId: string
  type: TaskType
  priority: TaskPriority
  status: TaskStatus
  source: string
  destination: string
  cargo?: Cargo
  deadline?: number
  createdAt: string
  assignedVehicleId?: string
  estimatedCompletion?: string
}

/**
 * Task types
 */
export type TaskType = 'TRANSPORT' | 'LOADING' | 'UNLOADING' | 'CHARGING' | 'MAINTENANCE'

/**
 * Cargo
 */
export interface Cargo {
  type: string
  weight: number
  properties?: Record<string, unknown>
}

/**
 * Task request (for manual creation)
 */
export interface TaskRequest {
  type: TaskType
  priority: TaskPriority
  source: string
  destination: string
  cargo?: Cargo
  deadline?: number
}

/**
 * Priority set request
 */
export interface SetPriorityRequest {
  priority: TaskPriority
}

// ============================================================================
// KPI & Metrics Types (REQ-WEB-001, REQ-KPI-001~007)
// ============================================================================

/**
 * Metrics summary
 */
export interface MetricsSummary {
  simulatedTime: number
  throughput: ThroughputMetrics
  utilization: UtilizationMetrics
  wip: WIPMetrics
  energy: EnergyMetrics
}

/**
 * Throughput metrics
 */
export interface ThroughputMetrics {
  tasksCompleted: number
  tasksPerHour: number
  materialThroughput: number
  byType?: Record<string, number>
}

/**
 * Utilization metrics
 */
export interface UtilizationMetrics {
  average: number
  byEntity: Record<string, number>
  distribution: {
    low: number      // < 0.4
    medium: number   // 0.4 - 0.7
    high: number     // > 0.7
  }
}

/**
 * WIP metrics
 */
export interface WIPMetrics {
  total: number
  byLocation: Record<string, number>
}

/**
 * Energy metrics
 */
export interface EnergyMetrics {
  total: number
  byVehicle: Record<string, number>
}

/**
 * Metrics history query
 */
export interface MetricsHistoryQuery {
  from: number  // simulation time (seconds)
  to: number    // simulation time (seconds)
  interval?: number  // aggregation interval (seconds)
}

/**
 * Metrics history response
 */
export interface MetricsHistoryResponse {
  interval: number
  points: MetricsDataPoint[]
}

/**
 * Metrics data point
 */
export interface MetricsDataPoint {
  time: number
  throughput?: ThroughputMetrics
  utilization?: UtilizationMetrics
}

/**
 * Bottleneck report
 */
export interface BottleneckReport {
  bottlenecks: Bottleneck[]
  generatedAt: string
}

/**
 * Bottleneck item
 */
export interface Bottleneck {
  resourceId: string
  type: 'RESOURCE' | 'PATH'
  severity: 'HIGH' | 'MEDIUM' | 'LOW'
  utilization: number
  avgWaitTime: number
  recommendation?: string
}

// ============================================================================
// Config Types (REQ-WEB-016, REQ-WEB-017, REQ-WEB-018)
// ============================================================================

/**
 * Config category
 */
export type ConfigCategory =
  | 'traffic'
  | 'scheduler'
  | 'simulation'
  | 'web'
  | 'model'
  | 'draft'
  | 'distribution'

/**
 * Config item
 */
export interface ConfigItem {
  key: string
  value: string
  category: ConfigCategory
  description?: string
  defaultValue?: string
  updatedAt?: string
}

/**
 * Config update request
 */
export interface ConfigUpdateRequest {
  value: string
}

// ============================================================================
// WebSocket Types (REQ-WEB-002)
// ============================================================================

/**
 * WebSocket message (client -> server or server -> client)
 */
export interface WSMessage {
  type: WSMessageType
  seq?: number
  simTime?: number
  ts?: number
  requestId?: string
  payload?: unknown
}

/**
 * WebSocket message types
 */
export type WSMessageType =
  // Server -> Client
  | 'state.update'
  | 'entity.update'
  | 'kpi.update'
  | 'task.update'
  | 'control.ack'
  | 'paused'
  | 'resumed'
  | 'completed'
  | 'error'
  | 'heartbeat'
  | 'snapshot'
  // Client -> Server
  | 'control.start'
  | 'control.pause'
  | 'control.resume'
  | 'control.stop'
  | 'task.create'
  | 'task.cancel'
  | 'entity.update'
  | 'subscribe'
  | 'set_speed'

/**
 * Snapshot payload (initial state)
 */
export interface SnapshotPayload {
  simulationId: string
  simulatedTime: number
  status: SimulationStatus
  timeScale: number
  entities: EntityRuntimeState[]
}

/**
 * Entity update payload
 */
export interface EntityUpdatePayload {
  entities: EntityUpdate[]
  batch?: EntityUpdate[]  // For batch mode (timeScale > 10x)
}

/**
 * Entity update
 */
export interface EntityUpdate {
  entityId: string
  changes: Partial<EntityRuntimeState>
}

/**
 * KPI update payload
 */
export interface KPIUpdatePayload {
  metrics: MetricsSummary
}

/**
 * Task update payload
 */
export interface TaskUpdatePayload {
  taskId: string
  status: TaskStatus
  assignedVehicleId?: string
}

/**
 * Subscribe payload
 */
export interface SubscribePayload {
  filters?: {
    entityTypes?: EntityType[]
    eventTypes?: string[]
  }
}

/**
 * Set speed payload
 */
export interface SetSpeedPayload {
  timeScale: number
}

// ============================================================================
// Event Types (REQ-WEB-021)
// ============================================================================

/**
 * Event level
 */
export type EventLevel = 'INFO' | 'WARNING' | 'ERROR'

/**
 * Simulation event
 */
export interface SimulationEvent {
  eventId: string
  timestamp: number  // simulation time
  level: EventLevel
  type: string
  message: string
  details?: Record<string, unknown>
  wallClockTime?: string
}

// ============================================================================
// Revisions Types (REQ-WEB-023)
// ============================================================================

/**
 * Replay state query
 */
export interface ReplayStateQuery {
  time: number  // simulation time (seconds)
}

/**
 * Replay state response
 */
export interface ReplayStateResponse {
  time: number
  entities: EntityRuntimeState[]
  metrics: MetricsSummary
}

// ============================================================================
// Draft Types (REQ-WEB-025)
// ============================================================================

/**
 * Draft auto-save config
 */
export const DRAFT_AUTO_SAVE_INTERVAL = 60 * 1000  // 1 minute

/**
 * Draft de-dupe key
 */
export interface DraftKey {
  sceneId: string
  content: string
}

// ============================================================================
// Missing type exports for Phase 5.3 Scene Editor
// ============================================================================

/**
 * Simple path data for OHT tracks
 */
export interface SimplePath {
  id: string
  name: string
  type: 'OHT_PATH'
  segments: SimpleSegment[]
  controlPoints: SimpleControlPoint[]
  points: PathPoint[]
}

/**
 * Simple segment for OHT tracks
 */
export interface SimpleSegment {
  id: string
  type: 'LINEAR' | 'BEZIER'
  from: string
  to: string
  c1: { x: number; y: number }
  c2: { x: number; y: number }
}

/**
 * Simple control point for OHT tracks
 */
export interface SimpleControlPoint {
  id: string
  x: number
  y: number
}

/**
 * OHT Path type
 */
export type OHT_PATH_TYPE = 'LINEAR' | 'BEZIER'

/**
 * OHT Track interface
 */
export interface OHTTrack {
  id: string
  name: string
  type: 'OHT_PATH'
  segments?: SimpleSegment[]
  controlPoints?: SimpleControlPoint[]
}

/**
 * Process step interface
 */
export interface ProcessStep {
  id: string
  name: string
  description?: string
  targetEntityIds: string[]
  requiredTransportTypes: TransportType[]
  priority: number
  processFlowId: string | null
}

/**
 * Safety zone interface
 */
export interface SafetyZone {
  id: string
  type: 'rectangle' | 'circle' | 'polygon'
  priority: 'HUMAN_FIRST' | 'VEHICLE_FIRST' | 'FIFO' | 'PRIORITY_BASED'
  maxHumans: number
  maxVehicles: number
  position: { x: number; y: number }
  width?: number
  height?: number
  radius?: number
  points?: { x: number; y: number }[]
}

// ============================================================================
// Event Types (REQ-WEB-021)
// ============================================================================
