# 运营数据平台 Vue3 - E2E测试快速开始指南

## 📋 概述

本文档说明如何运行前端E2E自动化测试，验证页面功能是否符合PRD要求。

## 🚀 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 安装Playwright浏览器

```bash
npx playwright install chromium
```

> **注意**：首次安装可能需要几分钟时间下载浏览器

### 3. 启动开发服务器

```bash
npm run dev
```

服务将启动在 http://localhost:3000

### 4. 运行E2E测试

#### 运行所有测试
```bash
npm run test:e2e
```

#### 运行冒烟测试（P0核心功能）
```bash
npm run test:e2e:smoke
```

#### 运行回归测试（P0+P1模块）
```bash
npm run test:e2e:regression
```

#### 以UI模式运行（推荐用于调试）
```bash
npm run test:e2e:ui
```

#### 查看测试报告
```bash
npm run test:e2e:report
```

## 📊 测试覆盖情况

### 已完成的测试用例

| 模块 | 测试文件 | 用例数量 | 状态 |
|------|---------|---------|------|
| 首页仪表盘（简化版） | `tests/dashboard.spec.ts` | 6个 | ✅ 已完成 |
| 首页仪表盘（完整版） | `tests/dashboard.spec.ts` | 8个 | ✅ 已完成 |

**总计**: 14个E2E测试用例

### 测试场景分类

#### 简化版仪表盘测试（6个）
- ✅ DASH-001: KPI卡片数据加载
- ✅ DASH-003: 快捷入口按钮可见
- ✅ DASH-004: 页面标题正确显示
- ✅ DASH-009: 页面布局响应式
- ✅ DASH-010: 页面加载无控制台错误

#### 完整版仪表盘测试（8个）
- ✅ DASH-FULL-001: 欢迎区显示
- ✅ DASH-FULL-002: 5个KPI卡片完整显示
- ✅ DASH-FULL-003: 快捷入口6个图标
- ✅ DASH-FULL-004: 图表区域渲染
- ✅ DASH-FULL-005: 待办事项列表
- ✅ DASH-FULL-006: 预警通知列表
- ✅ DASH-FULL-007: 快捷入口跳转功能
- ✅ DASH-FULL-008: 查看全部按钮

## 🔧 测试配置

### Playwright配置

配置文件：`playwright.config.ts`

```typescript
export default defineConfig({
  testDir: './tests',
  baseURL: 'http://localhost:3000',
  
  // 多浏览器支持
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
  
  // 失败时自动截图和录屏
  use: {
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'on-first-retry',
  },
})
```

### 测试标签系统

使用标签对测试用例进行分类：

- `@smoke`: 冒烟测试（P0核心功能，快速验证）
- `@regression`: 回归测试（P0+P1重要功能）

示例：
```typescript
test.describe('首页仪表盘测试 @smoke @regression', () => {
  // 这个测试套件同时属于冒烟和回归测试
})
```

## 🐛 调试技巧

### 1. 使用UI模式

```bash
npm run test:e2e:ui
```

UI模式提供：
- 🎥 实时看到测试执行
- 🔍 交互式断点调试
- 📸 自动截图对比
- 📹 视频回放

### 2. 查看失败截图

测试失败时，截图保存在：
```
test-results/
├── dashboard-首页仪表盘测试-DASH-001-KPI卡片数据加载-chromium/
│   └── test-failed-1.png
```

### 3. 查看Trace Viewer

```bash
# 运行测试后
npx playwright show-trace test-results/*/trace.zip
```

Trace Viewer提供：
- 完整的测试执行时间线
- DOM快照
- 网络请求
- 控制台日志

### 4. 单测调试

运行单个测试文件：
```bash
npx playwright test tests/dashboard.spec.ts
```

运行单个测试用例：
```bash
npx playwright test tests/dashboard.spec.ts -g "DASH-001"
```

## 📈 测试报告

### HTML报告

运行测试后生成HTML报告：
```bash
npm run test:e2e:report
```

报告包含：
- ✅ 测试通过率
- ⏱️ 执行时间统计
- 📸 失败截图
- 📹 失败视频
- 🔍 Trace详情

### CI/CD集成

GitHub Actions示例：
```yaml
name: E2E Tests
on: [push, pull_request]

jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: 18
      
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npm run test:e2e:smoke
      
      - name: Upload report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: playwright-report/
```

## 🎯 下一步计划

### 待开发的测试模块

根据《前端开发计划与E2E测试方案.md》，后续需要为以下模块编写E2E测试：

| 优先级 | 模块 | 预计用例数 |
|--------|------|-----------|
| P0 | IP组管理 | 5个 |
| P0 | 作者管理 | 5个 |
| P0 | 账号分析 | 5个 |
| P1 | 粉丝分析 | 5个 |
| P1 | 作品分析 | 5个 |
| P1 | SOP管理 | 8个 |
| P1 | 内容管理 | 8个 |
| ... | ... | ... |

**目标**: 
- P0模块：100%覆盖（每个模块至少5个用例）
- P1模块：80%覆盖
- P2模块：50%覆盖

### 测试用例模板

为新模块编写测试时，参考以下模板：

```typescript
import { test, expect } from '@playwright/test'

test.describe('模块名称测试 @smoke @regression', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/module-path')
    await page.waitForLoadState('networkidle')
  })

  test('TEST-001: 列表数据加载', async ({ page }) => {
    // 验证表格存在
    const table = page.locator('.el-table')
    await expect(table).toBeVisible()
    
    // 验证至少有数据行
    const rows = page.locator('.el-table__row')
    await expect(rows.count()).toBeGreaterThan(0)
  })

  test('TEST-002: 新增功能', async ({ page }) => {
    // 点击新增按钮
    await page.click('button:has-text("新增")')
    
    // 填写表单
    await page.fill('input[name="field1"]', '测试数据')
    
    // 提交
    await page.click('button:has-text("确认")')
    
    // 验证成功提示
    await expect(page.locator('.el-message--success')).toBeVisible()
  })
  
  // ... 更多测试用例
})
```

## ❓ 常见问题

### Q1: 测试超时怎么办？

A: 增加超时时间：
```typescript
test.setTimeout(60000) // 60秒
```

### Q2: 元素找不到怎么办？

A: 使用更具体的选择器：
```typescript
// 不推荐
await page.click('button')

// 推荐
await page.click('button:has-text("确认")')
await page.locator('[data-testid="confirm-btn"]').click()
```

### Q3: 如何跳过某个测试？

A: 使用 `test.skip()` 或 `test.only()`:
```typescript
test.skip('暂时跳过的测试', async ({ page }) => {
  // ...
})

test.only('只运行这个测试', async ({ page }) => {
  // ...
})
```

### Q4: Mock数据如何配置？

A: 使用Playwright的route拦截：
```typescript
await page.route('**/api/data', route => {
  route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ data: [] })
  })
})
```

## 📚 相关文档

- [前端开发计划与E2E测试方案.md](./前端开发计划与E2E测试方案.md)
- [Playwright官方文档](https://playwright.dev/)
- [Element Plus组件文档](https://element-plus.org/)

---

**最后更新**: 2026-05-28  
**维护者**: AI Assistant
