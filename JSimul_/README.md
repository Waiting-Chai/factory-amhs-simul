# JSimul

Java实现的一个SimPy风格的离散事件仿真核心。使用Java 21和Maven构建。

## 项目概述
JSimul是一个用于离散事件仿真的Java库，提供了与Python SimPy类似的核心功能和API。它专为工厂自动化、物流系统和生产流程仿真而设计，特别适用于半导体制造和自动化物流场景。

## 设计理念

### 组合优于继承
JSimul的核心设计理念是"组合优于继承"，与传统面向对象设计不同，我们避免使用深层继承结构，而是通过接口实现和对象组合来构建功能。这种设计带来以下优势：

1. **灵活性**：组件可以独立变化，不需要修改继承层次结构
2. **可测试性**：各组件可以独立测试，更容易创建模拟对象
3. **可维护性**：减少了类之间的耦合，降低了维护成本
4. **扩展性**：通过实现接口和组合对象，可以轻松添加新功能

### 接口驱动设计
所有核心组件都基于接口设计，如`Environment`、`Event`、`Process`等，提供了清晰的契约和多种实现方式。这种设计使得用户可以根据需要选择不同的实现，或者提供自定义实现。

### 函数式编程元素
JSimul充分利用Java的函数式编程特性，如Lambda表达式和方法引用，使代码更加简洁和表达力强。特别是在进程定义中，可以使用Lambda表达式来定义进程行为。

## 核心模块
- `com.semi.jSimul.core`: 仿真核心功能，包括环境、事件、进程和条件
- `com.semi.jSimul.collections`: 仿真资源集合，包括资源、存储、容器等
- `com.semi.jSimul.examples`: 示例代码，展示如何使用JSimul进行仿真

## 核心功能

### 仿真核心 (com.semi.jSimul.core)
- `Environment`: 事件调度器和仿真运行环境，提供`run()`, `timeout()`, `process()`等方法
- `Event` / `Timeout`: 可等待的事件，支持回调；`Timeout`在延迟后触发
- `Process`: 在任务中运行的用户逻辑，使用`ProcessContext.await(Event)`阻塞等待事件
- `Condition` (`AnyOf` / `AllOf`): 事件组合，支持多个事件的逻辑组合

### 资源集合 (com.semi.jSimul.collections)
- `Resource`, `PriorityResource`, `PreemptiveResource`: 具有容量的共享资源，支持请求/释放操作
- `Store`, `FilterStore`, `PriorityStore`: 项目队列，支持可选的过滤/优先级
- `Container`: 连续数量的存放/获取

## 构建与测试
- 环境要求: JDK 21, Maven 3.9+
- 构建项目: `mvn -q package`
- 运行测试: `mvn test`
- 生成测试覆盖率报告: `mvn jacoco:report`

## 快速开始

### 基本使用示例
```java
import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.Process;

// 创建仿真环境
Environment env = new Environment();

// 创建进程
Process process = env.process(() -> {
    // 等待超时事件
    env.timeout(10);
    // 执行一些操作
    System.out.println("Process completed at time " + env.now());
});

// 运行仿真
env.run();
```

### 资源使用示例
```java
import com.semi.jSimul.core.Environment;
import com.semi.jSimul.core.Process;
import com.semi.jSimul.collections.Resource;

// 创建环境
Environment env = new Environment();

// 创建资源
Resource machine = new Resource(env, 1); // 1台机器

// 创建使用机器的进程
Process job = env.process(() -> {
    // 请求机器
    machine.request();
    try {
        // 使用机器
        env.timeout(5);
    } finally {
        // 释放机器
        machine.release();
    }
});

// 运行仿真
env.run();
```

## 示例项目
示例代码位于`src/main/java/com/semi/jSimul/examples`目录：
- `BasicUsageExample`: 两个进程等待超时的基本示例
- `ResourceUsageExample`: 队列作业请求/释放单台机器的示例
- `ConditionExample`: 演示AnyOf/AllOf组合的示例
- `FlowLineScenario`: 完整的流水线(A->F)场景，车辆移动零件，扫描车辆数量以满足每日目标

运行示例（构建后）:
```bash
mvn -q -DskipTests package
java -cp target/classes com.semi.jSimul.examples.BasicUsageExample
```
替换类名为任何其他示例进行探索。

## 日志记录
示例使用`java.util.logging`发出INFO级别的里程碑事件。可以通过全局日志配置（例如`-Djava.util.logging.config.file=...`）调整运行示例时的详细程度。开发人员可以添加类似的日志记录到进程和回调中，以帮助故障排除。

## 测试覆盖
项目包含全面的单元测试，覆盖所有核心功能和资源类型。测试报告可在`target/site/jacoco/index.html`中查看。

## 项目状态
JSimul是plant-simulation项目的核心仿真模块，专为工厂自动化和生产流程仿真而设计。