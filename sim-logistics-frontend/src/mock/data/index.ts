/**
 * Mock data for sim-logistics-frontend.
 *
 * Aligned with docs/frontend-contract.md API contract.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
import type { SceneSummary, SceneDetail, ModelSummary, ModelDetail } from '../types'

/**
 * Mock scenes data
 */
export const MOCK_SCENES: SceneSummary[] = [
  {
    sceneId: 'scene-fab-01',
    name: '半导体晶圆厂 01 号线',
    description: '包含 OHT 轨道、AGV 路网、Stocker 和多台机台的完整仿真场景',
    version: 3,
    createdAt: '2026-01-15T08:00:00Z',
    updatedAt: '2026-02-10T10:30:00Z',
    entityCount: 45,
  },
  {
    sceneId: 'scene-warehouse-01',
    name: '智能仓储物流中心',
    description: '自动化仓库仿真，包含多层货架、AGV 和输送线',
    version: 1,
    createdAt: '2026-01-20T14:00:00Z',
    updatedAt: '2026-02-05T16:20:00Z',
    entityCount: 28,
  },
  {
    sceneId: 'scene-oht-only',
    name: 'OHT 纯轨道测试场景',
    description: '仅包含 OHT 轨道和车辆的简单测试场景',
    version: 5,
    createdAt: '2026-02-01T09:00:00Z',
    updatedAt: '2026-02-09T11:45:00Z',
    entityCount: 12,
  },
  {
    sceneId: 'scene-agv-mixed',
    name: 'AGV 混合交通测试',
    description: 'AGV 车辆与安全区混合的交通场景',
    version: 2,
    createdAt: '2026-02-05T10:00:00Z',
    updatedAt: '2026-02-08T15:30:00Z',
    entityCount: 35,
  },
  {
    sceneId: 'scene-stress-test',
    name: '压力测试场景 - 100 车',
    description: '用于调度器压力测试的高密度场景',
    version: 1,
    createdAt: '2026-02-08T13:00:00Z',
    updatedAt: '2026-02-08T13:00:00Z',
    entityCount: 120,
  },
  {
    sceneId: 'scene-bottleneck-01',
    name: '瓶颈分析演示',
    description: '预设交通瓶颈的场景，用于 KPI 分析功能演示',
    version: 4,
    createdAt: '2026-01-25T11:00:00Z',
    updatedAt: '2026-02-07T14:15:00Z',
    entityCount: 52,
  },
  {
    sceneId: 'scene-tutorial',
    name: '新手教程场景',
    description: '用于引导新用户了解系统功能的简化场景',
    version: 2,
    createdAt: '2026-01-10T08:00:00Z',
    updatedAt: '2026-02-01T09:30:00Z',
    entityCount: 8,
  },
  {
    sceneId: 'scene-empty',
    name: '空白场景模板',
    description: '空场景，用于从零开始创建新仿真',
    version: 1,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    entityCount: 0,
  },
]

/**
 * Mock scene detail
 */
export const MOCK_SCENE_DETAIL: Record<string, SceneDetail> = {
  'scene-fab-01': {
    sceneId: 'scene-fab-01',
    name: '半导体晶圆厂 01 号线',
    description: '包含 OHT 轨道、AGV 路网、Stocker 和多台机台的完整仿真场景',
    version: 3,
    createdAt: '2026-01-15T08:00:00Z',
    updatedAt: '2026-02-10T10:30:00Z',
    entities: [
      {
        id: 'OHT-01',
        type: 'OHT_VEHICLE',
        name: 'OHT-Crane-01',
        position: { x: 0, y: 0, z: 10 },
        rotation: { x: 0, y: 0, z: 0 },
        properties: { maxLoad: 50, speed: 2.5 },
      },
      {
        id: 'OHT-02',
        type: 'OHT_VEHICLE',
        name: 'OHT-Crane-02',
        position: { x: 50, y: 0, z: 10 },
        rotation: { x: 0, y: 0, z: 0 },
        properties: { maxLoad: 50, speed: 2.5 },
      },
      {
        id: 'AGV-01',
        type: 'AGV_VEHICLE',
        name: 'AGV-Forklift-01',
        position: { x: 0, y: 0, z: 0 },
        rotation: { x: 0, y: 0, z: 0 },
        properties: { battery: 0.9, maxLoad: 100 },
      },
      {
        id: 'STOCKER-01',
        type: 'STOCKER',
        name: 'Stocker-A',
        position: { x: 100, y: 0, z: 0 },
        rotation: { x: 0, y: 0, z: 0 },
        properties: { capacity: 500, slots: 50 },
      },
      {
        id: 'MACHINE-01',
        type: 'MACHINE',
        name: 'Lithography-A',
        position: { x: 50, y: 0, z: 0 },
        rotation: { x: 0, y: 0, z: 0 },
        properties: { processingTime: 50, supportedTransportTypes: ['OHT', 'AGV'] },
      },
    ],
    paths: [
      {
        id: 'TRACK-A',
        type: 'OHT_PATH',
        name: 'Main OHT Track',
        points: [
          { id: 'P1', position: { x: 0, y: 0, z: 10 } },
          { id: 'P2', position: { x: 50, y: 0, z: 10 } },
          { id: 'P3', position: { x: 100, y: 0, z: 10 } },
        ],
        segments: [
          { id: 'S1', type: 'LINEAR', from: 'P1', to: 'P2' },
          { id: 'S2', type: 'LINEAR', from: 'P2', to: 'P3' },
        ],
      },
    ],
    processFlows: [],
  },
}

/**
 * Mock models data
 */
export const MOCK_MODELS: ModelSummary[] = [
  {
    modelId: 'model-oht-crane',
    name: 'OHT Crane v1.0',
    type: 'OHT_VEHICLE',
    defaultVersion: '1.0.0',
    status: 'ACTIVE',
    versions: [],
    thumbnailUrl: '',
    createdAt: '2026-01-10T08:00:00Z',
    updatedAt: '2026-01-10T08:00:00Z',
  },
  {
    modelId: 'model-agv-forklift',
    name: 'AGV Forklift',
    type: 'AGV_VEHICLE',
    defaultVersion: '1.2.0',
    status: 'ACTIVE',
    versions: [],
    thumbnailUrl: '',
    createdAt: '2026-01-12T10:00:00Z',
    updatedAt: '2026-01-15T14:00:00Z',
  },
  {
    modelId: 'model-stock',
    name: 'Automated Stocker',
    type: 'STOCKER',
    defaultVersion: '2.0.0',
    status: 'ACTIVE',
    versions: [],
    thumbnailUrl: '',
    createdAt: '2026-01-08T09:00:00Z',
    updatedAt: '2026-01-20T11:00:00Z',
  },
  {
    modelId: 'model-machine-litho',
    name: 'Lithography Machine',
    type: 'MACHINE',
    defaultVersion: '1.5.0',
    status: 'ACTIVE',
    versions: [],
    thumbnailUrl: '',
    createdAt: '2026-01-05T08:00:00Z',
    updatedAt: '2026-01-25T16:00:00Z',
  },
  {
    modelId: 'model-conveyor',
    name: 'Conveyor Belt',
    type: 'CONVEYOR',
    defaultVersion: '1.0.0',
    status: 'ACTIVE',
    versions: [],
    thumbnailUrl: '',
    createdAt: '2026-01-18T13:00:00Z',
    updatedAt: '2026-01-18T13:00:00Z',
  },
  {
    modelId: 'model-erack',
    name: 'Equipment Rack',
    type: 'ERACK',
    defaultVersion: '1.0.0',
    status: 'ACTIVE',
    versions: [],
    thumbnailUrl: '',
    createdAt: '2026-01-14T10:00:00Z',
    updatedAt: '2026-01-14T10:00:00Z',
  },
  {
    modelId: 'model-bay',
    name: 'Loading Bay',
    type: 'BAY',
    defaultVersion: '1.1.0',
    status: 'ACTIVE',
    versions: [],
    thumbnailUrl: '',
    createdAt: '2026-01-16T11:00:00Z',
    updatedAt: '2026-01-22T15:00:00Z',
  },
  {
    modelId: 'model-chute',
    name: 'Transfer Chute',
    type: 'CHUTE',
    defaultVersion: '1.0.0',
    status: 'ACTIVE',
    versions: [],
    thumbnailUrl: '',
    createdAt: '2026-01-19T09:00:00Z',
    updatedAt: '2026-01-19T09:00:00Z',
  },
]

/**
 * Mock model detail
 */
export const MOCK_MODEL_DETAIL: Record<string, ModelDetail> = {
  'model-oht-crane': {
    modelId: 'model-oht-crane',
    name: 'OHT Crane v1.0',
    type: 'OHT_VEHICLE',
    description: 'Overhead Hoist Transport crane for semiconductor fab',
    defaultVersion: '1.0.0',
    versions: [
      {
        versionId: 'v-1-0-0',
        version: '1.0.0',
        isDefault: true,
        status: 'ACTIVE',
        fileSize: 2048576,
        fileUrl: 'https://modelviewer.dev/shared-assets/models/Astronaut.glb',
        thumbnailUrl: '/mock/thumbnails/oht-crane.png',
        metadata: {
          type: 'OHT_VEHICLE',
          version: '1.0.0',
          dimensions: { width: 2.5, height: 1.2, depth: 1.0 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        },
        createdAt: '2026-01-10T08:00:00Z',
      },
    ],
    createdAt: '2026-01-10T08:00:00Z',
    updatedAt: '2026-01-10T08:00:00Z',
  },
  'model-agv-forklift': {
    modelId: 'model-agv-forklift',
    name: 'AGV Forklift',
    type: 'AGV_VEHICLE',
    description: 'Automated Guided Vehicle for material transport',
    defaultVersion: '1.2.0',
    versions: [
      {
        versionId: 'v-1-2-0',
        version: '1.2.0',
        isDefault: true,
        status: 'ACTIVE',
        fileSize: 1572864,
        fileUrl: 'https://modelviewer.dev/shared-assets/models/NeilArmstrong.glb',
        thumbnailUrl: '',
        metadata: {
          type: 'AGV_VEHICLE',
          version: '1.2.0',
          dimensions: { width: 1.8, height: 1.5, depth: 2.0 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        },
        createdAt: '2026-01-12T10:00:00Z',
      },
    ],
    createdAt: '2026-01-12T10:00:00Z',
    updatedAt: '2026-01-15T14:00:00Z',
  },
  'model-stock': {
    modelId: 'model-stock',
    name: 'Automated Stocker',
    type: 'STOCKER',
    description: 'Automated storage and retrieval system',
    defaultVersion: '2.0.0',
    versions: [
      {
        versionId: 'v-2-0-0',
        version: '2.0.0',
        isDefault: true,
        status: 'ACTIVE',
        fileSize: 3145728,
        fileUrl: 'https://modelviewer.dev/shared-assets/models/CesiumMan.glb',
        thumbnailUrl: '',
        metadata: {
          type: 'STOCKER',
          version: '2.0.0',
          dimensions: { width: 3.0, height: 5.0, depth: 2.5 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        },
        createdAt: '2026-01-08T09:00:00Z',
      },
    ],
    createdAt: '2026-01-08T09:00:00Z',
    updatedAt: '2026-01-20T11:00:00Z',
  },
  'model-machine-litho': {
    modelId: 'model-machine-litho',
    name: 'Lithography Machine',
    type: 'MACHINE',
    description: 'Semiconductor lithography equipment',
    defaultVersion: '1.5.0',
    versions: [
      {
        versionId: 'v-1-5-0',
        version: '1.5.0',
        isDefault: true,
        status: 'ACTIVE',
        fileSize: 2621440,
        fileUrl: 'https://modelviewer.dev/shared-assets/models/CesiumMilkTruck.glb',
        thumbnailUrl: '',
        metadata: {
          type: 'MACHINE',
          version: '1.5.0',
          dimensions: { width: 2.4, height: 2.0, depth: 3.1 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        },
        createdAt: '2026-01-05T08:00:00Z',
      },
    ],
    createdAt: '2026-01-05T08:00:00Z',
    updatedAt: '2026-01-25T16:00:00Z',
  },
  'model-conveyor': {
    modelId: 'model-conveyor',
    name: 'Conveyor Belt',
    type: 'CONVEYOR',
    description: 'Automated material handling conveyor system',
    defaultVersion: '1.0.0',
    versions: [
      {
        versionId: 'v-1-0-0',
        version: '1.0.0',
        isDefault: true,
        status: 'ACTIVE',
        fileSize: 1048576,
        fileUrl: 'https://modelviewer.dev/shared-assets/models/CesiumAir.glb',
        thumbnailUrl: '',
        metadata: {
          type: 'CONVEYOR',
          version: '1.0.0',
          dimensions: { width: 5.0, height: 0.5, depth: 1.0 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        },
        createdAt: '2026-01-18T13:00:00Z',
      },
    ],
    createdAt: '2026-01-18T13:00:00Z',
    updatedAt: '2026-01-18T13:00:00Z',
  },
  'model-erack': {
    modelId: 'model-erack',
    name: 'Equipment Rack',
    type: 'ERACK',
    description: 'Electronic equipment rack for fab tools',
    defaultVersion: '1.0.0',
    versions: [
      {
        versionId: 'v-1-0-0',
        version: '1.0.0',
        isDefault: true,
        status: 'ACTIVE',
        fileSize: 786432,
        fileUrl: 'https://modelviewer.dev/shared-assets/models/CesiumBalloon.glb',
        thumbnailUrl: '',
        metadata: {
          type: 'ERACK',
          version: '1.0.0',
          dimensions: { width: 1.2, height: 2.2, depth: 0.8 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        },
        createdAt: '2026-01-14T10:00:00Z',
      },
    ],
    createdAt: '2026-01-14T10:00:00Z',
    updatedAt: '2026-01-14T10:00:00Z',
  },
  'model-bay': {
    modelId: 'model-bay',
    name: 'Loading Bay',
    type: 'BAY',
    description: 'Material loading/unloading bay area',
    defaultVersion: '1.1.0',
    versions: [
      {
        versionId: 'v-1-1-0',
        version: '1.1.0',
        isDefault: true,
        status: 'ACTIVE',
        fileSize: 524288,
        fileUrl: 'https://modelviewer.dev/shared-assets/models/CesiumDrone.glb',
        thumbnailUrl: '',
        metadata: {
          type: 'BAY',
          version: '1.1.0',
          dimensions: { width: 4.0, height: 1.0, depth: 4.0 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        },
        createdAt: '2026-01-16T11:00:00Z',
      },
    ],
    createdAt: '2026-01-16T11:00:00Z',
    updatedAt: '2026-01-22T15:00:00Z',
  },
  'model-chute': {
    modelId: 'model-chute',
    name: 'Transfer Chute',
    type: 'CHUTE',
    description: 'Gravity-fed material transfer chute',
    defaultVersion: '1.0.0',
    versions: [
      {
        versionId: 'v-1-0-0',
        version: '1.0.0',
        isDefault: true,
        status: 'ACTIVE',
        fileSize: 393216,
        fileUrl: 'https://modelviewer.dev/shared-assets/models/CesiumHeart.glb',
        thumbnailUrl: '',
        metadata: {
          type: 'CHUTE',
          version: '1.0.0',
          dimensions: { width: 1.5, height: 2.0, depth: 1.5 },
          anchorPoint: { x: 0, y: 0, z: 0 },
        },
        createdAt: '2026-01-19T09:00:00Z',
      },
    ],
    createdAt: '2026-01-19T09:00:00Z',
    updatedAt: '2026-01-19T09:00:00Z',
  },
}

/**
 * In-memory storage for mock data modifications
 */
export const mockStore = {
  scenes: [...MOCK_SCENES],
  sceneDetails: { ...MOCK_SCENE_DETAIL },
  models: [...MOCK_MODELS],
  modelDetails: { ...MOCK_MODEL_DETAIL },
  drafts: new Map<string, unknown>(), // sceneId -> draft content
  bindings: new Map<string, unknown>(), // sceneId -> bindings

  reset() {
    this.scenes = [...MOCK_SCENES]
    this.sceneDetails = { ...MOCK_SCENE_DETAIL }
    this.models = [...MOCK_MODELS]
    this.modelDetails = { ...MOCK_MODEL_DETAIL }
    this.drafts.clear()
    this.bindings.clear()
  },
}
