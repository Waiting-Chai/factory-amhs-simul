# sim-logistics-frontend

独立前端模块，用于物流仿真系统的可视化界面。

## 技术栈

- React 18
- TypeScript
- Vite
- Tailwind CSS
- Zustand (状态管理)
- React Router
- Three.js (@react-three/fiber, @react-three/drei)
- Chart.js
- Axios

## 开发指南

### 安装依赖

```bash
npm install
```

如遇网络问题，可使用 npmmirror 镜像：

```bash
npm install --registry=https://registry.npmmirror.com
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 代码检查

```bash
npm run lint
npm run lint:fix
```

### 运行测试

```bash
npm run test
npm run test:ui
npm run test:coverage
```

## 项目结构

```
sim-logistics-frontend/
├── src/
│   ├── api/          # API 服务层
│   ├── assets/       # 静态资源
│   ├── components/   # 通用组件
│   ├── hooks/        # 自定义 hooks
│   ├── pages/        # 页面组件
│   ├── store/        # Zustand 状态管理
│   ├── test/         # 测试配置
│   ├── types/        # TypeScript 类型定义
│   ├── utils/        # 工具函数
│   ├── main.tsx      # 应用入口
│   └── index.css     # 全局样式
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── tailwind.config.js
```

## API 契约

严格遵循 `docs/frontend-contract.md` 中定义的 REST API 契约：

- 场景管理：`/api/v1/scenes`
- 模型库：`/api/v1/models`
- 仿真控制：`/api/v1/simulations`
- 任务管理：`/api/v1/tasks`
- KPI 指标：`/api/v1/metrics`
- 配置中心：`/api/v1/config`

## 模块隔离

本前端模块完全独立于后端 Java 代码，仅通过 HTTP API 与后端通信。

- 前端代码位于 `sim-logistics-frontend/`
- 后端代码位于 `sim-logistics-web/`
- 两者仅通过 `/api/v1` REST API 交互

## 设计风格

采用工业实用主义美学：

- 深色工业配色方案（dark slate, steel blue, amber accents）
- 高对比度状态指示器
- 网格化卡片布局
- 微妙的动画效果

---

@author shentw
@version 1.0
@since 2026-02-10
