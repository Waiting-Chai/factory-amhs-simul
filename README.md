# plant-simulation

## 项目概述

**plant-simulation** 是一个专注于工业系统仿真的综合项目，旨在为**工厂自动化、物流系统与生产流程**提供高质量的仿真解决方案。
项目基于**离散事件仿真（DES）**技术，帮助用户分析、优化并验证复杂工业系统的设计与运营策略。

系统特别适用于：

* 半导体制造
* 自动化立体仓库与物流系统
* 生产线调度与吞吐量优化
* 工厂产能分析与流程改善

项目提供可扩展、高性能的仿真引擎，支持精确建模与决策优化。

---

## 模块介绍

### JSimul_ 模块（核心仿真引擎）

**JSimul_** 是本项目的核心模块，一个基于 **SimPy 风格** 的 Java 离散事件仿真框架，实现了全套事件驱动机制与资源模型。

#### 主要特性

* **SimPy兼容 API**
  风格与 Python SimPy 类似，降低学习成本、方便迁移。
* **离散事件调度器**
  精确模拟系统在事件之间的动态变化。
* **丰富的资源模型**
  支持 `Resource` / `Store` / `Container` 等工业仿真常用结构。
* **进程模型（Process）**
  仿真主体以轻量协程实现，支持高并发逻辑模拟。
* **条件组合（Condition）机制**
  可实现复杂同步、等待与事件组合逻辑。

#### 核心包结构

* **仿真核心（com.semi.jSimul.core）**

    * `Environment`：仿真环境与事件调度器
    * `Event` / `Timeout`：可等待事件模型
    * `Process`：用户逻辑进程
    * `Condition`：事件组合、同步工具

* **资源集合（com.semi.jSimul.collections）**

    * `Resource` / `PriorityResource` / `PreemptiveResource`
    * `Store` / `FilterStore` / `PriorityStore`
    * `Container`（连续量模型）

#### 质量保证

* 全量单元测试覆盖所有核心功能
* 支持 JaCoCo 覆盖率报告
* 保证仿真结果的正确性与稳定性

---

## 开发规范

### 技术栈

* **JDK 21**
* **Maven**
* **JUnit 5**
* **JaCoCo 覆盖率分析**

### 设计原则

#### 1. 组合优于继承（Favor Composition Over Inheritance）

项目核心均采用组合与接口，而非继承层级结构：

* 更灵活：组件可独立变化
* 更易测试：组件可被 Mock
* 更易维护：降低耦合
* 更易扩展：天然支持插件式设计
* 更高复用性：同一组件可用于不同上下文

#### 2. 接口驱动设计

所有核心功能均以接口形式暴露，优点：

* 方便用户自定义
* 不限制实现方式
* 适配依赖注入
* 易于测试与扩展

#### 3. 利用 Java 的函数式特性

仿真过程支持 Lambda，例如：

```java
env.process(() -> {
    yield env.timeout(10);
});
```

使代码更优雅、更易读。

### 代码质量标准

* 公共 API 必须有 JavaDoc
* 核心功能单测覆盖率≥ 80%
* 遵循 Google Java Style
* 使用 Checkstyle 等静态检查工具
* 定期代码审查，保证架构一致性

---

## 项目结构
```
plant-simulation/
├── JSimul_/                    # 核心仿真引擎模块
│   ├── src/
│   │   ├── main/
│   │   │   └── java/           # Java 源码
│   │   └── test/
│   │       └── java/           # 单元测试代码
│   ├── target/                 # 构建输出
│   ├── pom.xml                 # 模块 Maven 配置
│   └── README.md               # 模块文档
│
├── doc/                        # 项目文档
│   └── mvn/
│       └── sim_setting.xml
│
├── README.md                   # 项目主文档
├── .gitignore
└── pom.xml                     # 主项目 Maven 配置
```

---

## 快速开始

### 环境要求

* JDK ≥ 21
* Maven ≥ 3.9

### 构建项目

```bash
git clone https://git.semi-tech.com/delivery/automatic-logistics/software/simulation/plant-simulation.git
cd plant-simulation

# 构建项目
mvn clean package

# 运行测试
mvn test
```

### 运行示例

```bash
cd JSimul_
mvn -q -DskipTests package

java -cp target/classes com.semi.jSimul.examples.BasicUsageExample
```

---

## 贡献指南

1. Fork 本仓库
2. 创建新分支

   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. 提交代码
4. 推送到仓库
5. 提交 Merge Request

请确保遵守项目规范并提供必要的测试。

---
