# Frontend Contract Baseline

- 文档版本: v1.1
- 适用范围: 前后端联调（Phase 5+）
- 目标: 固化前端业务契约，确保多 Agent 并行协作不冲突

## 1. 协作职责（MANDATORY）

- `codex` / `claude code` / `glm` 均可负责：
  - 前端页面实现与交互实现
  - API 契约实现与对齐
  - 数据收集与渲染逻辑
  - Zustand/store 业务状态流
  - 异常处理、联调与测试
- 涉及页面设计/绘制时必须遵守 `frontend-design` skills 约束。

## 2. 不可变约束（Do Not Change）

1. API 前缀固定：`/api/v1`
2. 统一 Envelope：
   - `code: string`
   - `message: string`
   - `data: T`
   - `traceId?: string`
3. `GET /scenes/{id}/draft`：
   - `404` 表示“无草稿”，前端返回 `null`
4. `GET /scenes/{id}/export`：
   - 返回 `blob` 文件流，不走 envelope
5. 模型启停接口：
   - `PATCH /models/{id}/enable`
   - `PATCH /models/{id}/disable`
6. 绑定查询接口：
   - `GET /scenes/{sceneId}/bindings` 返回 `ApiEnvelope<EntityModelBinding[]>`（`data` 直接数组）

## 3. API 契约清单

### 3.1 场景模块

- `GET /api/v1/scenes` -> `ApiEnvelope<PagedResult<SceneSummary>>`
- `GET /api/v1/scenes/{id}` -> `ApiEnvelope<SceneDetail>`
- `POST /api/v1/scenes` -> `ApiEnvelope<SceneDetail>`
- `PUT /api/v1/scenes/{id}` -> `ApiEnvelope<SceneDetail>`
- `DELETE /api/v1/scenes/{id}` -> `204`
- `POST /api/v1/scenes/{id}/copy` -> `ApiEnvelope<SceneCopyResult>`
- `POST /api/v1/scenes/import` -> `ApiEnvelope<SceneImportResult>`
- `GET /api/v1/scenes/{id}/export` -> `blob`
- `GET /api/v1/scenes/{id}/draft` -> `ApiEnvelope<SceneDraftPayload>` / `404 => null`
- `POST /api/v1/scenes/{id}/draft` -> `ApiEnvelope<SceneDraftSaveResult>`
- `DELETE /api/v1/scenes/{id}/draft` -> `204`
- 分页语义（强制）：
  - 后端 API `page` 参数为 **0-based**（第一页=0，第二页=1，...）
  - 后端响应中的 `page` 字段保持 0-based
  - 前端责任：
    - UI 层可使用 1-based 展示（用户友好：Page 1, 2, 3...）
    - 请求前必须显式转换：`backend_page = frontend_page - 1`
    - 响应后必须显式转换：`frontend_page = backend_page + 1`
    - Store 中的 pagination.page 应为 1-based（UI 状态）
  - 测试保护：
    - 后端测试断言 page=0 返回第一页
    - 前端测试断言 UI 显示不出现 "Page 0"

### 3.2 模型模块

- `GET /api/v1/models` -> `ApiEnvelope<PagedResult<ModelSummary>>`
- `GET /api/v1/models/{id}` -> `ApiEnvelope<ModelDetail>`
- `POST /api/v1/models/upload` -> `ApiEnvelope<ModelUploadResult>`
- `PUT /api/v1/models/{id}` -> `ApiEnvelope<ModelDetail>`
- `POST /api/v1/models/{id}/versions` -> `ApiEnvelope<ModelVersion>`
- `PUT /api/v1/models/{id}/versions/{versionId}` -> `ApiEnvelope<ModelVersion>`
- `PATCH /api/v1/models/{id}/enable` -> `204|200`
- `PATCH /api/v1/models/{id}/disable` -> `204|200`
- `DELETE /api/v1/models/{id}` -> `204`
- metadata.transform 契约（强制）：
  - 唯一标准格式（上传模型 + 上传版本统一）：
    - `scale: { x: number, y: number, z: number }`
    - `rotation: { x: number, y: number, z: number }`
    - `pivot: { x: number, y: number, z: number }`
  - 请求示例（multipart `metadata` JSON）：
```json
{
  "type": "MACHINE",
  "version": "1.0.0",
  "size": { "width": 2.4, "height": 2.0, "depth": 3.1 },
  "anchor": { "x": 0.0, "y": 0.0, "z": 0.0 },
  "transform": {
    "scale": { "x": 1.0, "y": 1.0, "z": 1.0 },
    "rotation": { "x": 0.0, "y": 0.0, "z": 0.0 },
    "pivot": { "x": 0.0, "y": 0.0, "z": 0.0 }
  }
}
```
  - 非法格式语义：
    - 返回 `BAD_REQUEST`，HTTP `400`
    - message: `metadata/transform格式错误`
  - 历史兼容（仅后端兼容，不作为前端新格式）：
    - `transform.scale: [x, y, z]` 可被后端转换为对象

### 3.3 绑定模块

- `GET /api/v1/scenes/{sceneId}/bindings` -> `ApiEnvelope<EntityModelBinding[]>`
- `PUT /api/v1/scenes/{sceneId}/bindings/{entityId}` -> `204|200`

## 4. 数据语义约束

- 坐标单位：`m`
- 角度单位：`rad`，范围 `[-pi, pi]`
- 时间语义：
  - 仿真时间与墙钟时间必须分离展示与存储
  - 不允许混用

## 5. Store 与渲染责任边界

- View 层（可由任一工程 Agent 修改）：
  - 页面布局、样式、视觉组件拆分
- 业务层（可由任一工程 Agent 修改，但必须按契约）：
  - `api/*`
  - `store/*`
  - `types/*` 的业务字段语义
  - 数据转换与异常处理逻辑

## 6. 变更流程（强制）

1. 任一 Agent 先更新本契约
2. 再执行 UI 与业务改动
3. 最后执行联调回归与测试

## 7. 验收门禁

- 前端：`lint/build/test` 通过
- 联调：核心链路通过
  - 场景列表/编辑/导入导出/草稿
  - 模型列表/上传/启停/版本
  - 绑定关系查询与更新
