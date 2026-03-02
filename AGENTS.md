
## Language & comments
- Communicate in Chinese. please!!!!
- Code comments MUST be in English (Javadoc + inline comments).

## Frontend 设计技能约束
- 只要涉及前端页面设计/绘制，必须使用 `frontend-design` skills。

## 多 Agent 协作约束（MANDATORY）
- `codex` / `claude code` / `glm` 均可执行前端与后端代码实现、接口联调与问题修复。
- 涉及前端页面设计/绘制时，必须使用 `frontend-design` skills。
- 涉及前后端联动时，必须优先对齐 `docs/frontend-contract.md`。
- 不允许任何 Agent 在未更新契约的情况下擅自修改：
  - API 路径
  - 请求/响应字段语义
  - 错误码语义

## 统一契约文档约束（MANDATORY）
- 当任务涉及前后端联动、接口对齐、页面联调时，必须优先阅读并遵守：
  - `docs/frontend-contract.md`
- 任何 Agent 在实现前必须先输出“接口对齐清单”（路径、方法、字段、错误码语义）并与契约一致。
- 若发现契约缺失或冲突，必须先提出待确认项，不得自行发明接口后直接落地。
- 页面与联调流程（强制）：
  1. 先维护并冻结 `docs/frontend-contract.md`
  2. 再执行页面与业务实现
  3. 最后完成联调回归与测试验证

## 自动识别与目录约束
- 在仓库根目录任务，默认先读根 `AGENTS.md`。
- 在子模块执行任务时，若存在更近路径的 `AGENTS.md`，优先遵守更近文件，再继承根规则。
- 推荐在以下目录增设局部约束文件（如后续启用）：
  - `sim-logistics-web/AGENTS.md`（后端约束）
  - `sim-logistics-frontend/AGENTS.md`（前端约束）

## 交付与验收约束
- 前端/后端联动交付必须附带：
  - 页面或接口改动清单
  - 契约对齐结果
  - 未触碰项声明（若有）
- 联调交付必须附带：
  - 契约对齐结果
  - 构建/测试结果（至少 lint/build/test 或等价验证）

## Java version
- This project targets Java 21. Do not use APIs/language features beyond Java 21.

## Editing workflow
- When modifying code, prefer patch-style edits (minimal diffs).
- Before applying changes, show the exact diff (unified diff) for review.

## New class header (MANDATORY)
When creating ANY new Java class/interface/enum, add this Javadoc at the top of the type:

/**
 * <One-line purpose summary>.
 *
 * @author shentw
 * @version 1.0
 * @since YYYY-MM-DD
 */

Rules:
- @author is ALWAYS "shentw".
- @since is TODAY's date.
- If the agent is unsure of today's date, it MUST read system date from terminal (e.g., `date +%F`) and use that value.
- 如果让你提交git， 永远都要符合git的提交标准； 使用feat\refactor\style\chore\doc\fix这几个关键词， 然后“:”后面紧跟中文描述， 要将明白修改了什么， 新增了什么；

<skills_system priority="1">

## Available Skills

<!-- SKILLS_TABLE_START -->
<usage>
When users ask you to perform tasks, check if any of the available skills below can help complete the task more effectively. Skills provide specialized capabilities and domain knowledge.

How to use skills:
- Invoke: Bash("openskills read <skill-name>")
- The skill content will load with detailed instructions on how to complete the task
- Base directory provided in output for resolving bundled resources (references/, scripts/, assets/)

Usage notes:
- Only use skills listed in <available_skills> below
- Do not invoke a skill that is already loaded in your context
- Each skill invocation is stateless
</usage>

<available_skills>

<skill>
<name>algorithmic-art</name>
<description>Creating algorithmic art using p5.js with seeded randomness and interactive parameter exploration. Use this when users request creating art using code, generative art, algorithmic art, flow fields, or particle systems. Create original algorithmic art rather than copying existing artists' work to avoid copyright violations.</description>
<location>project</location>
</skill>

<skill>
<name>brand-guidelines</name>
<description>Applies Anthropic's official brand colors and typography to any sort of artifact that may benefit from having Anthropic's look-and-feel. Use it when brand colors or style guidelines, visual formatting, or company design standards apply.</description>
<location>project</location>
</skill>

<skill>
<name>canvas-design</name>
<description>Create beautiful visual art in .png and .pdf documents using design philosophy. You should use this skill when the user asks to create a poster, piece of art, design, or other static piece. Create original visual designs, never copying existing artists' work to avoid copyright violations.</description>
<location>project</location>
</skill>

<skill>
<name>doc-coauthoring</name>
<description>Guide users through a structured workflow for co-authoring documentation. Use when user wants to write documentation, proposals, technical specs, decision docs, or similar structured content. This workflow helps users efficiently transfer context, refine content through iteration, and verify the doc works for readers. Trigger when user mentions writing docs, creating proposals, drafting specs, or similar documentation tasks.</description>
<location>project</location>
</skill>

<skill>
<name>docx</name>
<description>"Comprehensive document creation, editing, and analysis with support for tracked changes, comments, formatting preservation, and text extraction. When Claude needs to work with professional documents (.docx files) for: (1) Creating new documents, (2) Modifying or editing content, (3) Working with tracked changes, (4) Adding comments, or any other document tasks"</description>
<location>project</location>
</skill>

<skill>
<name>frontend-design</name>
<description>Create distinctive, production-grade frontend interfaces with high design quality. Use this skill when the user asks to build web components, pages, artifacts, posters, or applications (examples include websites, landing pages, dashboards, React components, HTML/CSS layouts, or when styling/beautifying any web UI). Generates creative, polished code and UI design that avoids generic AI aesthetics.</description>
<location>project</location>
</skill>

<skill>
<name>internal-comms</name>
<description>A set of resources to help me write all kinds of internal communications, using the formats that my company likes to use. Claude should use this skill whenever asked to write some sort of internal communications (status reports, leadership updates, 3P updates, company newsletters, FAQs, incident reports, project updates, etc.).</description>
<location>project</location>
</skill>

<skill>
<name>mcp-builder</name>
<description>Guide for creating high-quality MCP (Model Context Protocol) servers that enable LLMs to interact with external services through well-designed tools. Use when building MCP servers to integrate external APIs or services, whether in Python (FastMCP) or Node/TypeScript (MCP SDK).</description>
<location>project</location>
</skill>

<skill>
<name>pdf</name>
<description>Comprehensive PDF manipulation toolkit for extracting text and tables, creating new PDFs, merging/splitting documents, and handling forms. When Claude needs to fill in a PDF form or programmatically process, generate, or analyze PDF documents at scale.</description>
<location>project</location>
</skill>

<skill>
<name>pptx</name>
<description>"Presentation creation, editing, and analysis. When Claude needs to work with presentations (.pptx files) for: (1) Creating new presentations, (2) Modifying or editing content, (3) Working with layouts, (4) Adding comments or speaker notes, or any other presentation tasks"</description>
<location>project</location>
</skill>

<skill>
<name>skill-creator</name>
<description>Guide for creating effective skills. This skill should be used when users want to create a new skill (or update an existing skill) that extends Claude's capabilities with specialized knowledge, workflows, or tool integrations.</description>
<location>project</location>
</skill>

<skill>
<name>slack-gif-creator</name>
<description>Knowledge and utilities for creating animated GIFs optimized for Slack. Provides constraints, validation tools, and animation concepts. Use when users request animated GIFs for Slack like "make me a GIF of X doing Y for Slack."</description>
<location>project</location>
</skill>

<skill>
<name>template</name>
<description>Replace with description of the skill and when Claude should use it.</description>
<location>project</location>
</skill>

<skill>
<name>theme-factory</name>
<description>Toolkit for styling artifacts with a theme. These artifacts can be slides, docs, reportings, HTML landing pages, etc. There are 10 pre-set themes with colors/fonts that you can apply to any artifact that has been creating, or can generate a new theme on-the-fly.</description>
<location>project</location>
</skill>

<skill>
<name>web-artifacts-builder</name>
<description>Suite of tools for creating elaborate, multi-component claude.ai HTML artifacts using modern frontend web technologies (React, Tailwind CSS, shadcn/ui). Use for complex artifacts requiring state management, routing, or shadcn/ui components - not for simple single-file HTML/JSX artifacts.</description>
<location>project</location>
</skill>

<skill>
<name>webapp-testing</name>
<description>Toolkit for interacting with and testing local web applications using Playwright. Supports verifying frontend functionality, debugging UI behavior, capturing browser screenshots, and viewing browser logs.</description>
<location>project</location>
</skill>

<skill>
<name>xlsx</name>
<description>"Comprehensive spreadsheet creation, editing, and analysis with support for formulas, formatting, data analysis, and visualization. When Claude needs to work with spreadsheets (.xlsx, .xlsm, .csv, .tsv, etc) for: (1) Creating new spreadsheets with formulas and formatting, (2) Reading or analyzing data, (3) Modify existing spreadsheets while preserving formulas, (4) Data analysis and visualization in spreadsheets, or (5) Recalculating formulas"</description>
<location>project</location>
</skill>

</available_skills>
<!-- SKILLS_TABLE_END -->

</skills_system>
