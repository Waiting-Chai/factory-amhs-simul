# Backend Module Instructions (`sim-logistics-web`)

## Scope
- 本文件仅适用于 `sim-logistics-web` 模块。
- 若与仓库根 `AGENTS.md` 冲突，以更严格规则为准。

## 核心职责
- 仅负责后端接口、业务逻辑、持久化适配、联调支撑。
- 不负责页面视觉、CSS、动画、布局设计。

## 前后端协作约束（MANDATORY）
- 任何前端联动改动前，必须先读取：
  - `docs/frontend-contract.md`
- 后端接口改动必须同步更新契约文档，再实施代码。
- 禁止在未更新契约的情况下变更：
  - API 路径
  - 请求/响应字段
  - 错误码语义

## 与前端模块边界
- 本模块仅承接后端实现，不承担页面视觉设计决策。
- 前端对接必须基于 `docs/frontend-contract.md`，禁止后端擅自漂移接口契约。

## 代码与测试要求
- Java 版本固定 21。
- 注释必须为英文（Javadoc + inline comment）。
- 所有对外接口改动需有测试覆盖：
  - 单元测试
  - 至少一条接口契约验证

## 提交规范
- 提交前缀仅使用：`feat|fix|refactor|style|chore|doc`
- 格式：`<type>: <中文描述>`
- 描述必须清晰说明“修改了什么、为何修改”。
