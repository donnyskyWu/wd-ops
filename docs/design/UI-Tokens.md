# UI Design Tokens - 运营数据平台

> **版本**：v2.0 | 2026-06-08
> **设计原则**：专业、高效、一致、可扩展
> **技术栈**：Vue 3 + Element Plus 2.x + ECharts 5.x
> **参考设计系统**：Linear（主参考）+ Stripe（色彩参考）+ Vercel（组件参考）
> **审核来源**：基于 `UI-Design-System.md` v1.0 审核增强

---

## 1. 设计哲学

### 1.1 核心原则

| 原则 | 说明 | 设计映射 |
|------|------|---------|
| **数据驱动** | 一切以数据展示为核心，减少装饰性元素 | 低饱和度背景 + 高对比度数据色 |
| **高效操作** | 最短路径完成任务，减少点击次数 | 固定高度组件 + 明确的主次按钮层级 |
| **一致体验** | 所有模块遵循统一规范 | 本令牌文档为唯一事实源 |
| **渐进披露** | 复杂信息分层展示 | 卡片阴影层级 + 弹窗 z-index 分层 |
| **容错设计** | 操作可撤销，错误有提示 | 语义色彩即时反馈 |

### 1.2 视觉学派定位

本产品属于 **「精密工具（Precision Tool）」学派**：
- 中性色主导，品牌色点缀
- 信息密度优先于留白
- 微妙的阴影和边框建立层级
- 数据可视化使用高辨识度色板

---

## 2. 色彩系统

### 2.1 品牌色

| 用途 | 色值 (HEX) | OKLCh | 变量名 (CSS) | Element Plus 映射 | 使用场景 |
|------|-----------|-------|-------------|------------------|---------|
| **品牌主色** | `#409EFF` | `oklch(70% 0.14 250)` | `--color-brand` | `--el-color-primary` | 主按钮、链接、激活态、图表主系列 |
| **品牌悬停** | `#66B1FF` | `oklch(76% 0.12 250)` | `--color-brand-hover` | `--el-color-primary-light-3` | 按钮 hover、链接 hover |
| **品牌点击** | `#3A8EE6` | `oklch(62% 0.15 250)` | `--color-brand-active` | `--el-color-primary-dark-2` | 按钮 active、按下态 |
| **品牌浅色背景** | `#ECF5FF` | `oklch(95% 0.03 250)` | `--color-brand-bg` | `--el-color-primary-light-9` | 选中行背景、标签背景 |
| **品牌浅色边框** | `#D9ECFF` | `oklch(91% 0.04 250)` | `--color-brand-border` | `--el-color-primary-light-8` | 焦点框、高亮边框 |

### 2.2 语义状态色

| 语义 | 主色 | 悬停 | 背景 | 边框 | 变量前缀 | 使用场景 |
|------|------|------|------|------|---------|---------|
| **成功** | `#67C23A` | `#85CE61` | `#F0F9EB` | `#E1F3D8` | `--color-success-*` | 成功状态、启用标签、上升指标 |
| **警告** | `#E6A23C` | `#EBB563` | `#FDF6EC` | `#FAECD8` | `--color-warning-*` | 警告提示、接口补录标签 |
| **危险** | `#F56C6C` | `#F78989` | `#FEF0F0` | `#FBC4C4` | `--color-danger-*` | 错误、删除、封号补录、下降指标 |
| **信息** | `#909399` | `#A6A9AD` | `#F4F4F5` | `#E9E9EB` | `--color-info-*` | 次要信息、辅助标签 |

### 2.3 中性色阶（9 级）

> 用于文本、边框、背景的系统性灰度。命名遵循 `--color-neutral-{级别}` 格式。

| 级别 | 色值 (HEX) | 变量名 | 用途 | WCAG 对比度（对白底） |
|------|-----------|--------|------|---------------------|
| 900 | `#1D2129` | `--color-neutral-900` | 页面标题、最重要文本 | 15.4:1 ✅ AAA |
| 800 | `#303133` | `--color-neutral-800` | 区块标题、正文强调 | 12.6:1 ✅ AAA |
| 700 | `#4E5969` | `--color-neutral-700` | 常规正文、表单标签 | 7.5:1 ✅ AA |
| 600 | `#606266` | `--color-neutral-600` | 正文描述、表格正文 | 5.8:1 ✅ AA |
| 500 | `#86909C` | `--color-neutral-500` | 辅助文字、图标默认色 | 3.7:1 ✅ AA (大字) |
| 400 | `#A9AEB8` | `--color-neutral-400` | 占位文字、禁用文字 | 2.6:1 ⚠️ 仅大字 |
| 300 | `#C9CDD4` | `--color-neutral-300` | 禁用边框、分割线 | — |
| 200 | `#E5E6EB` | `--color-neutral-200` | 浅边框、表格线 | — |
| 100 | `#F2F3F5` | `--color-neutral-100` | 卡片边框、背景分隔 | — |
| 50 | `#F7F8FA` | `--color-neutral-50` | 表头背景、悬浮背景 | — |
| 25 | `#FBFCFD` | `--color-neutral-25` | 页面背景 | — |

### 2.4 业务色板（数据来源标识）

| 用途 | 色值 | 背景 | 边框 | 变量名 | 标签文字 |
|------|------|------|------|--------|---------|
| **API 数据** | `#409EFF` | `#ECF5FF` | `#D9ECFF` | `--biz-api-*` | "API" |
| **接口补录** | `#E6A23C` | `#FDF6EC` | `#FAECD8` | `--biz-import-exception-*` | "接口补录" |
| **封号补录** | `#F56C6C` | `#FEF0F0` | `#FBC4C4` | `--biz-import-banned-*` | "封号补录" |
| **线下补录** | `#F0B939` | `#FEF7E0` | `#FDE2A0` | `--biz-import-offline-*` | "线下补录" |
| **其他补录** | `#909399` | `#F4F4F5` | `#E9E9EB` | `--biz-import-other-*` | "其他补录" |
| **爆款标记** | `#F56C6C` | `#FEF0F0` | `#FBC4C4` | `--biz-hit-*` | "爆款" |
| **非爆款** | `#C9CDD4` | `#F2F3F5` | `#E5E6EB` | `--biz-miss-*` | "未命中" |

### 2.5 图表色板

#### 亮色主题

```javascript
// 8 色高辨识系列，色盲友好（避免红绿相邻）
const chartColorsLight = [
  '#409EFF', // 蓝 - 主系列
  '#52C41A', // 绿 - 次系列（调整饱和度以区分警告色）
  '#FAAD14', // 金 - 第三系列
  '#F56C6C', // 红 - 第四系列
  '#722ED1', // 紫 - 第五系列
  '#13C2C2', // 青 - 第六系列
  '#EB2F96', // 粉 - 第七系列
  '#8C8C8C', // 灰 - 基准线/辅助系列
];
```

#### 暗色主题（数据大屏）

```javascript
// 提高饱和度 + 亮度以适应深色背景
const chartColorsDark = [
  '#5CADFF', // 亮蓝
  '#73D13D', // 亮绿
  '#FFC53D', // 亮金
  '#FF7875', // 亮红
  '#9254DE', // 亮紫
  '#36CFC9', // 亮青
  '#FF85C0', // 亮粉
  '#BFBFBF', // 亮灰
];
```

#### 语义图表色

| 用途 | 亮色 | 暗色 | 说明 |
|------|------|------|------|
| **目标线/基准线** | `#8C8C8C` | `#BFBFBF` | 虚线展示 |
| **当前值** | `#409EFF` | `#5CADFF` | 实线/面积图 |
| **同比/环比** | `#52C41A` | `#73D13D` | 对比系列 |
| **异常标记** | `#F56C6C` | `#FF7875` | 警戒区域 |

### 2.6 暗色主题完整色板（数据大屏）

| 用途 | 色值 | 变量名 |
|------|------|--------|
| **页面背景** | `#0A1628` | `--dark-bg-page` |
| **卡片背景** | `#132238` | `--dark-bg-card` |
| **卡片悬浮** | `#1A2D47` | `--dark-bg-card-hover` |
| **边框** | `#1E3A5F` | `--dark-border` |
| **分割线** | `#243B57` | `--dark-divider` |
| **主文字** | `#E8F0FE` | `--dark-text-primary` |
| **次文字** | `#8BA4C0` | `--dark-text-secondary` |
| **禁用文字** | `#567189` | `--dark-text-disabled` |
| **品牌主色** | `#5CADFF` | `--dark-brand` |
| **品牌浅色背景** | `#1A3A5C` | `--dark-brand-bg` |

---

## 3. 排版系统

### 3.1 字体栈

```css
/* 中文优先字体栈 */
--font-family-sans:
  'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑',
  'Helvetica Neue', Helvetica, Arial, sans-serif;

/* 数字等宽字体（用于指标数字、金额、ID） */
--font-family-mono:
  'SF Mono', 'Roboto Mono', 'Cascadia Code',
  'Menlo', 'Consolas', 'Courier New', monospace;
```

### 3.2 字号阶梯（Type Scale）

> 基于 1.25 比例因子（Major Third），适配桌面端数据密集界面。

| 层级 | 名称 | 字号 | 行高 | 字重 | 字间距 | 变量名 | 使用场景 |
|------|------|------|------|------|--------|--------|---------|
| **T1** | Page Title | 24px | 32px | 600 | 0 | `--text-h1` | 页面标题（页头） |
| **T2** | Section Title | 18px | 26px | 600 | 0 | `--text-h2` | 区块标题、弹窗标题 |
| **T3** | Card Title | 16px | 24px | 600 | 0 | `--text-h3` | 卡片标题、Tab 标签 |
| **T4** | Body | 14px | 22px | 400 | 0 | `--text-body` | 正文、表格正文、按钮文字 |
| **T5** | Small | 13px | 20px | 400 | 0 | `--text-small` | 辅助文字、表单提示 |
| **T6** | Caption | 12px | 18px | 400 | 0 | `--text-caption` | 标签、表头、时间戳、指标标签 |
| **T7** | Metric | 28px | 36px | 700 | -0.5px | `--text-metric` | 指标数字（指标卡） |
| **T8** | Mini | 11px | 16px | 400 | 0.3px | `--text-mini` | 极小文字（角标、徽章） |

### 3.3 数字字体规范

```css
/* 所有指标数字启用等宽数字特性 */
.metric-number,
.amount,
.table-cell-number {
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
  letter-spacing: -0.5px; /* 大数字略收紧 */
}
```

### 3.4 字重规范

| 字重 | 值 | 使用场景 |
|------|-----|---------|
| Regular | 400 | 正文、描述、辅助文字 |
| Medium | 500 | 表格表头、按钮文字 |
| Semibold | 600 | 所有标题（H1-H3）、区块标题 |
| Bold | 700 | 指标数字、强调文字 |

---

## 4. 间距系统

### 4.1 基础间距阶梯（4px 基准网格）

> 所有间距值为 4 的整数倍。

| 层级 | 值 | 变量名 | 用途 |
|------|-----|--------|------|
| **S0** | 2px | `--space-1` | 图标与文字微调、行内元素间距 |
| **S1** | 4px | `--space-2` | 极小间距、标签内边距 |
| **S2** | 8px | `--space-3` | 小间距、表单元素内间距 |
| **S3** | 12px | `--space-4` | 筛选控件间距、紧凑布局 |
| **S4** | 16px | `--space-5` | 常规间距、卡片内边距（Element Plus 默认） |
| **S5** | 20px | `--space-6` | 页面内边距、区块间距（兼容现有规范） |
| **S6** | 24px | `--space-7` | 大间距、页面区块间距 |
| **S7** | 32px | `--space-8` | 超大间距、页面级分组 |
| **S8** | 40px | `--space-9` | 页面级大分组 |
| **S9** | 48px | `--space-10` | 顶栏高度、页头高度 |

### 4.2 组件内边距映射

| 组件 | 内边距 | 变量应用 |
|------|--------|---------|
| 按钮 | 垂直 8px / 水平 16px | `var(--space-3) var(--space-5)` |
| 指标卡 | 20px | `var(--space-6)` |
| 卡片 | 20px | `var(--space-6)` |
| 弹窗 | 20px | `var(--space-6)` |
| 表格单元格 | 垂直 12px / 水平 16px | `var(--space-4) var(--space-5)` |
| 筛选区 | 16px | `var(--space-5)` |
| 表单字段组 | 20px | `var(--space-6)` |

### 4.3 页面间距

| 区域 | 值 | 说明 |
|------|-----|------|
| 页面内容区左右 | 20px | 主内容区与浏览器边缘 |
| 页面内容区最大宽度 | 1400px | 居中对齐 |
| 页面内区块间距 | 20px | 卡片与卡片之间 |
| 页面上下边距 | 20px | 内容区与顶/底部 |

---

## 5. 圆角 & 阴影

### 5.1 圆角系统

| 层级 | 值 | 变量名 | 使用场景 |
|------|-----|--------|---------|
| **None** | 0 | `--radius-none` | 图片、全屏面板 |
| **XS** | 2px | `--radius-xs` | 标签 Tag、徽章 Badge |
| **SM** | 4px | `--radius-sm` | 按钮、输入框、表格单元格、卡片、图表容器 |
| **MD** | 8px | `--radius-md` | 弹窗 Dialog、下拉菜单 Dropdown |
| **LG** | 12px | `--radius-lg` | 抽屉 Drawer、侧边弹窗 |
| **XL** | 16px | `--radius-xl` | 首页快捷入口卡片、指标卡 |
| **Full** | 9999px | `--radius-full` | 头像、开关 Switch、单选点 |

### 5.2 阴影系统

| 层级 | 值 | 变量名 | 使用场景 |
|------|-----|--------|---------|
| **S1 微阴影** | `0 1px 2px rgba(0,0,0,0.04)` | `--shadow-1` | 卡片默认态（替代 Element Plus 默认） |
| **S2 轻阴影** | `0 2px 8px rgba(0,0,0,0.06)` | `--shadow-2` | 卡片悬停态、指标卡 |
| **S3 中阴影** | `0 4px 16px rgba(0,0,0,0.08)` | `--shadow-3` | 下拉菜单、日期选择器面板 |
| **S4 重阴影** | `0 8px 24px rgba(0,0,0,0.12)` | `--shadow-4` | 弹窗 Dialog、气泡卡片 Popover |
| **S5 超阴影** | `0 12px 40px rgba(0,0,0,0.16)` | `--shadow-5` | 抽屉 Drawer、全屏模态 |
| **S6 Toast** | `0 6px 16px rgba(0,0,0,0.12)` | `--shadow-toast` | 消息提示 Toast |

> **暗色主题阴影调整**：使用 `rgba(0,0,0,0.3)` ~ `rgba(0,0,0,0.6)` 增强对比。

### 5.3 Z-Index 层级规范

| 层级 | 值 | 变量名 | 使用场景 |
|------|-----|--------|---------|
| **Base** | 0 | `--z-base` | 默认层 |
| **Dropdown** | 1000 | `--z-dropdown` | 下拉菜单、日期面板、选择器面板 |
| **Fixed Header** | 1001 | `--z-header` | 顶栏（固定定位） |
| **Sidebar** | 1002 | `--z-sidebar` | 侧边菜单 |
| **Affix** | 1003 | `--z-affix` | 吸顶筛选条 |
| **Overlay** | 2000 | `--z-overlay` | 遮罩层 |
| **Modal** | 2001 | `--z-modal` | 弹窗 Dialog |
| **Drawer** | 2002 | `--z-drawer` | 抽屉 Drawer |
| **Message** | 3000 | `--z-message` | Toast/Message 通知 |
| **Tooltip** | 4000 | `--z-tooltip` | 工具提示 |

---

## 6. 组件规范

### 6.1 按钮

| 类型 | 背景 | 边框 | 文字色 | 悬停背景 | 悬停边框 | 使用场景 |
|------|------|------|--------|---------|---------|---------|
| **Primary** | `#409EFF` | 无 | `#FFFFFF` | `#66B1FF` | 无 | 主操作（新建、提交、查询） |
| **Default** | `#FFFFFF` | `1px solid #DCDFE6` | `#606266` | `#ECF5FF` | `1px solid #B3D8FF` | 次操作（取消、重置） |
| **Text** | 透明 | 无 | `#606266` | `#F2F3F5` | 无 | 行内操作（编辑、删除） |
| **Link** | 透明 | 无 | `#409EFF` | 透明 | 无 | 跳转、查看详情 |
| **Danger** | `#F56C6C` | 无 | `#FFFFFF` | `#F78989` | 无 | 删除、禁用等危险操作 |

**按钮尺寸**：

| 尺寸 | 高度 | 内边距 | 字号 | 圆角 | 变量名 |
|------|------|--------|------|------|--------|
| **Large** | 40px | 0 20px | 14px | 4px | `--btn-lg` |
| **Default** | 32px | 0 16px | 14px | 4px | `--btn-default` |
| **Small** | 28px | 0 12px | 13px | 4px | `--btn-sm` |

**按钮状态**：

| 状态 | 表现 |
|------|------|
| 默认 | 正常色值 |
| 悬停 | 背景色 lighten 8% |
| 按下 | 背景色 darken 8% |
| 禁用 | 背景 `#F2F3F5`，文字 `#A9AEB8`，`cursor: not-allowed` |
| 加载中 | 按钮内显示 loading 图标 + `pointer-events: none` |

### 6.2 表单输入框

| 属性 | 默认 | 聚焦 | 错误 | 禁用 |
|------|------|------|------|------|
| 高度 | 32px | 32px | 32px | 32px |
| 边框 | `1px solid #DCDFE6` | `1px solid #409EFF` | `1px solid #F56C6C` | `1px solid #E5E6EB` |
| 背景 | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#F7F8FA` |
| 圆角 | 4px | 4px | 4px | 4px |
| 内边距 | 0 12px | 0 12px | 0 12px | 0 12px |
| 文字色 | `#303133` | `#303133` | `#303133` | `#A9AEB8` |
| 占位色 | `#A9AEB8` | `#A9AEB8` | `#A9AEB8` | `#C9CDD4` |

**输入框尺寸变体**：

| 尺寸 | 高度 | 字号 | 使用场景 |
|------|------|------|---------|
| Large | 40px | 14px | 搜索框、重要输入 |
| Default | 32px | 14px | 表单默认 |
| Small | 28px | 13px | 表格内编辑、筛选区 |

### 6.3 表格

| 属性 | 值 | 变量名 |
|------|-----|--------|
| 表头背景 | `#F7F8FA` | `--table-header-bg` |
| 表头文字 | `#86909C`，12px，500 | `--table-header-text` |
| 表头高度 | 40px | `--table-header-height` |
| 行高（默认） | 48px | `--table-row-height` |
| 行高（紧凑） | 40px | `--table-row-height-compact` |
| 行边框 | `1px solid #E5E6EB` | `--table-border` |
| 悬停行背景 | `#F7F8FA` | `--table-row-hover-bg` |
| 选中行背景 | `#ECF5FF` | `--table-row-selected-bg` |
| 斑马线背景 | `#FBFCFD` | `--table-stripe-bg` |
| 单元格内边距 | 12px 16px | `--table-cell-padding` |
| 固定列阴影 | `box-shadow: 2px 0 4px rgba(0,0,0,0.05)` | `--table-fixed-shadow` |

### 6.4 卡片

| 属性 | 值 |
|------|-----|
| 背景 | `#FFFFFF` |
| 边框 | `1px solid #E5E6EB` |
| 圆角 | 4px |
| 阴影（默认） | `0 1px 2px rgba(0,0,0,0.04)` |
| 阴影（悬停） | `0 2px 8px rgba(0,0,0,0.06)` |
| 内边距 | 20px |
| 标题字号 | 16px / 600 |
| 标题下间距 | 16px |

### 6.5 指标卡

```
┌─────────────────────────┐
│ 总作者数          ← 12px #86909C
│ 1,234             ← 28px #1D2129 / 700 / tabular-nums
│ ↑ 12% 较上月      ← 12px #67C23A
└─────────────────────────┘
```

| 属性 | 值 |
|------|-----|
| 默认尺寸 | 240px × 100px |
| 背景 | `#FFFFFF` |
| 边框 | 无 |
| 圆角 | 16px |
| 阴影（默认） | `0 1px 2px rgba(0,0,0,0.04)` |
| 阴影（悬停） | `0 2px 8px rgba(0,0,0,0.06)` |
| 标签字号 | 12px，`#86909C` |
| 数字字号 | 28px，`#1D2129`，700 |
| 趋势字号 | 12px，上升 `#67C23A`，下降 `#F56C6C` |

### 6.6 标签（Tag）

| 类型 | 文字色 | 背景 | 边框 | 圆角 | 内边距 |
|------|--------|------|------|------|--------|
| 默认 | `#86909C` | `#F4F4F5` | 无 | 2px | 2px 8px |
| 成功 | `#67C23A` | `#F0F9EB` | 无 | 2px | 2px 8px |
| 警告 | `#E6A23C` | `#FDF6EC` | 无 | 2px | 2px 8px |
| 危险 | `#F56C6C` | `#FEF0F0` | 无 | 2px | 2px 8px |
| 品牌 | `#409EFF` | `#ECF5FF` | 无 | 2px | 2px 8px |
| 描边 | `#606266` | `#FFFFFF` | `1px solid #DCDFE6` | 2px | 2px 8px |

### 6.7 弹窗 / 抽屉

| 属性 | 弹窗 Dialog | 抽屉 Drawer |
|------|------------|-------------|
| 宽度 | 520px / 720px / 900px | 600px / 800px |
| 圆角 | 8px | 12px（左侧） |
| 阴影 | `0 8px 24px rgba(0,0,0,0.12)` | `0 12px 40px rgba(0,0,0,0.16)` |
| 遮罩 | `rgba(0,0,0,0.5)` | `rgba(0,0,0,0.5)` |
| 标题 | 18px / 600 / `#1D2129` | 18px / 600 / `#1D2129` |
| 内边距 | 20px | 20px |
| 底部间距 | 16px | 16px |
| 出现动画 | fade + scale，200ms | slide-right，300ms |

### 6.8 面包屑

| 属性 | 值 |
|------|-----|
| 字号 | 14px |
| 文字色 | `#86909C` |
| 当前页文字色 | `#303133` |
| 分隔符 | `/`（`#C9CDD4`） |
| 链接悬停色 | `#409EFF` |
| 下间距 | 16px |

### 6.9 分页器

| 属性 | 值 |
|------|-----|
| 高度 | 32px |
| 字号 | 14px |
| 页码按钮尺寸 | 32px × 32px |
| 页码按钮圆角 | 4px |
| 当前页背景 | `#409EFF` |
| 当前页文字 | `#FFFFFF` |
| 悬停背景 | `#F7F8FA` |
| 禁用文字色 | `#C9CDD4` |
| 每页条数选择器 | 与输入框同规格 |
| 总条数文字 | `#86909C`，14px |

### 6.10 通知（Message / Toast）

| 类型 | 图标 | 背景 | 边框 | 文字色 | 圆角 | 阴影 |
|------|------|------|------|--------|------|------|
| 成功 | ✓ | `#F0F9EB` | `#E1F3D8` | `#67C23A` | 4px | `--shadow-toast` |
| 警告 | ⚠ | `#FDF6EC` | `#FAECD8` | `#E6A23C` | 4px | `--shadow-toast` |
| 错误 | ✕ | `#FEF0F0` | `#FBC4C4` | `#F56C6C` | 4px | `--shadow-toast` |
| 信息 | ⓘ | `#ECF5FF` | `#D9ECFF` | `#409EFF` | 4px | `--shadow-toast` |

| 属性 | 值 |
|------|-----|
| 持续时间 | 成功 2s，其他 3s |
| 出现动画 | fade + slide-down，200ms |
| 最大宽度 | 380px |
| 位置 | 顶部居中，距顶 20px |

---

## 7. 图表规范

### 7.1 ECharts 亮色主题配置

```javascript
const echartsThemeLight = {
  color: ['#409EFF', '#52C41A', '#FAAD14', '#F56C6C', '#722ED1', '#13C2C2', '#EB2F96', '#8C8C8C'],
  backgroundColor: 'transparent',
  textStyle: {
    fontFamily: "'PingFang SC', 'Helvetica Neue', sans-serif",
    fontSize: 12,
    color: '#606266'
  },
  title: {
    textStyle: {
      fontSize: 14,
      fontWeight: 600,
      color: '#1D2129'
    }
  },
  grid: {
    left: 60,
    right: 30,
    top: 30,
    bottom: 40
  },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(29,33,41,0.9)',
    borderColor: 'transparent',
    textStyle: { color: '#FFFFFF', fontSize: 12 },
    padding: [8, 12],
    extraCssText: 'border-radius: 4px; box-shadow: 0 4px 16px rgba(0,0,0,0.12);'
  },
  xAxis: {
    axisLine: { lineStyle: { color: '#E5E6EB' } },
    axisTick: { show: false },
    axisLabel: { color: '#86909C', fontSize: 12 },
    splitLine: { lineStyle: { color: '#F2F3F5', type: 'dashed' } }
  },
  yAxis: {
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#86909C', fontSize: 12 },
    splitLine: { lineStyle: { color: '#F2F3F5', type: 'dashed' } }
  },
  legend: {
    itemWidth: 12,
    itemHeight: 8,
    textStyle: { color: '#606266', fontSize: 12 }
  }
};
```

### 7.2 ECharts 暗色主题配置（数据大屏）

```javascript
const echartsThemeDark = {
  color: ['#5CADFF', '#73D13D', '#FFC53D', '#FF7875', '#9254DE', '#36CFC9', '#FF85C0', '#BFBFBF'],
  backgroundColor: 'transparent',
  textStyle: {
    fontFamily: "'PingFang SC', 'Helvetica Neue', sans-serif",
    fontSize: 14,
    color: '#8BA4C0'
  },
  title: {
    textStyle: {
      fontSize: 16,
      fontWeight: 600,
      color: '#E8F0FE'
    }
  },
  grid: {
    left: 60,
    right: 30,
    top: 30,
    bottom: 40
  },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(19,34,56,0.95)',
    borderColor: '#1E3A5F',
    textStyle: { color: '#E8F0FE', fontSize: 13 },
    padding: [10, 14],
    extraCssText: 'border-radius: 4px; box-shadow: 0 8px 24px rgba(0,0,0,0.3);'
  },
  xAxis: {
    axisLine: { lineStyle: { color: '#1E3A5F' } },
    axisTick: { show: false },
    axisLabel: { color: '#8BA4C0', fontSize: 13 },
    splitLine: { lineStyle: { color: '#243B57', type: 'dashed' } }
  },
  yAxis: {
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#8BA4C0', fontSize: 13 },
    splitLine: { lineStyle: { color: '#243B57', type: 'dashed' } }
  },
  legend: {
    itemWidth: 14,
    itemHeight: 10,
    textStyle: { color: '#8BA4C0', fontSize: 13 }
  }
};
```

### 7.3 图表容器规范

| 属性 | 亮色 | 暗色 |
|------|------|------|
| 容器背景 | `#FFFFFF` | `#132238` |
| 容器边框 | `1px solid #E5E6EB` | `1px solid #1E3A5F` |
| 容器圆角 | 4px | 4px |
| 容器内边距 | 16px | 16px |
| 默认高度 | 320px | 320px |
| 小图表高度 | 240px | 240px |
| 大图表高度 | 400px | 400px |

### 7.4 图表类型选择矩阵

| 场景 | 图表类型 | 示例 |
|------|---------|------|
| 时间趋势 | 折线图（面积可选） | 粉丝趋势、内容发布趋势 |
| 对比分析 | 柱状图 | 各平台账号数量对比 |
| 占比分布 | 饼图/环形图 | 平台分布、内容类型分布 |
| 排名 | 水平条形图 | 作者排行、IP 组排行 |
| 转化漏斗 | 漏斗图 | 关注→互动→转化漏斗 |
| 多维对比 | 雷达图 | 人效多维指标 |
| 目标完成 | 仪表盘 | SOP 完成率、绩效等级 |
| 关系网络 | 力导向图 | 三方关联图谱 |

---

## 8. 响应式断点

### 8.1 断点定义

| 断点 | 宽度 | 说明 | 支持情况 |
|------|------|------|---------|
| xs | < 768px | 手机 | Out of Scope |
| sm | ≥ 768px | 平板 | Out of Scope |
| md | ≥ 992px | 小屏桌面 | 最低支持 |
| lg | ≥ 1200px | 标准桌面 | 主要适配 |
| xl | ≥ 1920px | 大屏桌面 | 增强适配 |

### 8.2 各断点适配规则

| 组件 | md (992px) | lg (1200px) | xl (1920px) |
|------|-----------|-------------|-------------|
| 侧边菜单 | 收起（64px） | 展开（200px） | 展开（200px） |
| 指标卡列数 | 2 列 | 4 列 | 4 列 |
| 图表区布局 | 堆叠（单列） | 并排（2 列） | 并排（2 列） |
| 快捷入口 | 4 列 | 8 列 | 8 列 |
| 内容区最大宽度 | 100% | 1400px（居中） | 1400px（居中） |
| 表格列数 | 默认 | 默认 | 可增加可见列 |

### 8.3 数据大屏分辨率

| 属性 | 值 |
|------|-----|
| 设计分辨率 | 1920 × 1080 |
| 自动刷新间隔 | 5 分钟 |
| 字号缩放 | +20%（相对 Web 端） |
| 图表主题 | 暗色主题 |

---

## 9. 动画与缓动

### 9.1 动画时长

| 场景 | 动画 | 时长 | 缓动函数 |
|------|------|------|---------|
| 弹窗出现 | fade + scale | 200ms | `cubic-bezier(0.4, 0, 0.2, 1)` |
| 抽屉滑入 | slide-right | 300ms | `cubic-bezier(0.4, 0, 0.2, 1)` |
| 折叠展开 | slide | 200ms | `cubic-bezier(0.4, 0, 0.2, 1)` |
| 列表刷新 | fade | 150ms | `cubic-bezier(0.4, 0, 1, 1)` |
| 按钮点击 | scale 0.98 | 100ms | `cubic-bezier(0.4, 0, 0.2, 1)` |
| Tooltip | fade | 150ms | `cubic-bezier(0.4, 0, 0.2, 1)` |
| 页面切换 | fade | 200ms | `cubic-bezier(0.4, 0, 0.2, 1)` |
| Toast 出现 | fade + slide-down | 200ms | `cubic-bezier(0.4, 0, 0.2, 1)` |
| Toast 消失 | fade | 150ms | `cubic-bezier(0.4, 0, 1, 1)` |

### 9.2 缓动函数变量

```css
:root {
  --ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);    /* 通用进出 */
  --ease-out: cubic-bezier(0, 0, 0.2, 1);          /* 出现动画 */
  --ease-in: cubic-bezier(0.4, 0, 1, 1);           /* 消失动画 */
  --ease-bounce: cubic-bezier(0.34, 1.56, 0.64, 1); /* 弹性（仅按钮点击） */
}
```

---

## 10. 快捷键规范

| 功能 | 快捷键 | 说明 |
|------|--------|------|
| 全局搜索 | `Ctrl + K` | 打开命令面板/搜索 |
| 保存表单 | `Ctrl + S` | 当前表单提交 |
| 刷新数据 | `Ctrl + R` | 重新请求当前页数据 |
| 取消弹窗 | `Esc` | 关闭当前弹窗/抽屉 |
| 新建 | `Ctrl + N` | 当前页新建操作 |

---

## 11. 与 Element Plus 映射关系

### 11.1 CSS 变量覆盖映射

> 在 Element Plus 初始化时，通过 `:root` 覆盖以下变量，确保与令牌一致：

```css
:root {
  /* ── 品牌色 ── */
  --el-color-primary: #409EFF;
  --el-color-primary-light-3: #66B1FF;
  --el-color-primary-light-5: #A0CFFF;
  --el-color-primary-light-7: #C6E2FF;
  --el-color-primary-light-8: #D9ECFF;
  --el-color-primary-light-9: #ECF5FF;
  --el-color-primary-dark-2: #3A8EE6;

  /* ── 语义色 ── */
  --el-color-success: #67C23A;
  --el-color-success-light-3: #85CE61;
  --el-color-success-light-5: #A8D68B;
  --el-color-success-light-7: #C9E8B5;
  --el-color-success-light-8: #D9F0C8;
  --el-color-success-light-9: #F0F9EB;

  --el-color-warning: #E6A23C;
  --el-color-warning-light-3: #EBB563;
  --el-color-warning-light-5: #F0C78A;
  --el-color-warning-light-7: #F5DAB1;
  --el-color-warning-light-8: #FAECD8;
  --el-color-warning-light-9: #FDF6EC;

  --el-color-danger: #F56C6C;
  --el-color-danger-light-3: #F78989;
  --el-color-danger-light-5: #F9A7A7;
  --el-color-danger-light-7: #FBC4C4;
  --el-color-danger-light-8: #FDE2E2;
  --el-color-danger-light-9: #FEF0F0;

  --el-color-info: #909399;
  --el-color-info-light-3: #A6A9AD;
  --el-color-info-light-5: #BDBEBF;
  --el-color-info-light-7: #D3D4D5;
  --el-color-info-light-8: #E9E9EB;
  --el-color-info-light-9: #F4F4F5;

  /* ── 文字色 ── */
  --el-text-color-primary: #1D2129;
  --el-text-color-regular: #606266;
  --el-text-color-secondary: #86909C;
  --el-text-color-placeholder: #A9AEB8;
  --el-text-color-disabled: #C9CDD4;

  /* ── 边框色 ── */
  --el-border-color: #DCDFE6;
  --el-border-color-light: #E5E6EB;
  --el-border-color-lighter: #F2F3F5;
  --el-border-color-extra-light: #F7F8FA;

  /* ── 背景色 ── */
  --el-bg-color: #FFFFFF;
  --el-bg-color-page: #F7F8FA;
  --el-bg-color-overlay: #FFFFFF;
  --el-mask-color: rgba(0, 0, 0, 0.5);

  /* ── 圆角 ── */
  --el-border-radius-base: 4px;
  --el-border-radius-small: 2px;
  --el-border-radius-round: 9999px;

  /* ── 阴影 ── */
  --el-box-shadow-light: 0 2px 8px rgba(0, 0, 0, 0.06);
  --el-box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  --el-box-shadow-dark: 0 8px 24px rgba(0, 0, 0, 0.12);

  /* ── 字号 ── */
  --el-font-size-extra-large: 20px;
  --el-font-size-large: 18px;
  --el-font-size-medium: 16px;
  --el-font-size-base: 14px;
  --el-font-size-small: 13px;
  --el-font-size-extra-small: 12px;

  /* ── 间距 ── */
  --el-component-size-large: 40px;
  --el-component-size: 32px;
  --el-component-size-small: 28px;

  /* ── 过渡 ── */
  --el-transition-duration: 0.2s;
  --el-transition-duration-fast: 0.15s;
}
```

### 11.2 组件映射表

| UI 组件 | Element Plus 组件 | 令牌定制点 |
|---------|------------------|-----------|
| 按钮 | `el-button` | 圆角 4px、高度映射、主色覆盖 |
| 输入框 | `el-input` | 边框色、聚焦色、圆角 4px |
| 选择器 | `el-select` | 同输入框 + 下拉阴影 |
| 表格 | `el-table` | 表头背景、行高、边框色 |
| 分页 | `el-pagination` | 当前页背景、按钮尺寸 |
| 标签 | `el-tag` | 圆角 2px、背景/文字色 |
| 弹窗 | `el-dialog` | 圆角 8px、阴影、标题字号 |
| 抽屉 | `el-drawer` | 圆角 12px、宽度 |
| 卡片 | `el-card` | 阴影、圆角 4px、边框 |
| 消息 | `el-message` | Toast 样式覆盖 |
| 日期选择 | `el-date-picker` | 同输入框 + 面板阴影 |
| 树形选择 | `el-tree` | 节点高亮色 |
| 表单 | `el-form` | 标签字号、错误色 |
| 面包屑 | `el-breadcrumb` | 字号、分隔符色 |
| 开关 | `el-switch` | 品牌色覆盖 |
| 滑块 | `el-slider` | 品牌色覆盖 |
| 加载 | `el-loading` | 主色覆盖 |
| 空状态 | `el-empty` | 图标色、文字字号 |
| Tooltip | `el-tooltip` | 背景色、圆角 |
| Popover | `el-popover` | 阴影、圆角 |

### 11.3 自定义组件规范（项目级）

以下组件为项目自定义，不在 Element Plus 内置范围：

| 组件 | 基础组件 | 规范要点 |
|------|---------|---------|
| `<IpGroupTreeSelect />` | `el-tree-select` | 仅显示启用 + 有权限节点 |
| `<AccountSelect />` | `el-select` + 远端搜索 | 强关联校验、联动清空 |
| `<RealNameSelect />` | `el-select` + 远端搜索 | 脱敏显示：`张*三 (138****8000)` |
| `<PhoneSelect />` | `el-select` + 远端搜索 | 脱敏显示、联动 |
| `<SimCardSelect />` | `el-select` + 远端搜索 | 联动 phoneId |
| `<CompanySelect />` | `el-select` + 远端搜索 | 状态过滤 |
| `<UserSelect />` | `el-select` + 远端搜索 | 联动 position、ipGroupId |
| `<DictSelect dict-type="xxx" />` | `el-select` | 自动加载字典项 |

**选择器通用规范**：
- 禁用手动输入（`filterable` 可选，但 `allow-create` 禁止）
- 必填显示红色 `*`
- 远端搜索（输入 ≥ 1 字符触发）
- 默认显示"名称 + 关键标识"
- 联动时上游变化 → 下游清空 + 重新拉取

---

## 12. 完整 CSS 变量清单

> 可直接复制为项目级 `:root` 定义。

```css
:root {
  /* ═══════════════════════════════════════
     品牌色
     ═══════════════════════════════════════ */
  --color-brand: #409EFF;
  --color-brand-hover: #66B1FF;
  --color-brand-active: #3A8EE6;
  --color-brand-bg: #ECF5FF;
  --color-brand-border: #D9ECFF;

  /* ═══════════════════════════════════════
     语义色
     ═══════════════════════════════════════ */
  --color-success: #67C23A;
  --color-success-hover: #85CE61;
  --color-success-bg: #F0F9EB;
  --color-success-border: #E1F3D8;

  --color-warning: #E6A23C;
  --color-warning-hover: #EBB563;
  --color-warning-bg: #FDF6EC;
  --color-warning-border: #FAECD8;

  --color-danger: #F56C6C;
  --color-danger-hover: #F78989;
  --color-danger-bg: #FEF0F0;
  --color-danger-border: #FBC4C4;

  --color-info: #909399;
  --color-info-hover: #A6A9AD;
  --color-info-bg: #F4F4F5;
  --color-info-border: #E9E9EB;

  /* ═══════════════════════════════════════
     中性色阶
     ═══════════════════════════════════════ */
  --color-neutral-900: #1D2129;
  --color-neutral-800: #303133;
  --color-neutral-700: #4E5969;
  --color-neutral-600: #606266;
  --color-neutral-500: #86909C;
  --color-neutral-400: #A9AEB8;
  --color-neutral-300: #C9CDD4;
  --color-neutral-200: #E5E6EB;
  --color-neutral-100: #F2F3F5;
  --color-neutral-50: #F7F8FA;
  --color-neutral-25: #FBFCFD;

  /* ═══════════════════════════════════════
     背景色
     ═══════════════════════════════════════ */
  --bg-page: #F7F8FA;
  --bg-card: #FFFFFF;
  --bg-header: #FFFFFF;
  --bg-sidebar: #304156;

  /* ═══════════════════════════════════════
     文字色（语义化别名）
     ═══════════════════════════════════════ */
  --text-primary: #1D2129;
  --text-regular: #606266;
  --text-secondary: #86909C;
  --text-placeholder: #A9AEB8;
  --text-disabled: #C9CDD4;

  /* ═══════════════════════════════════════
     边框色（语义化别名）
     ═══════════════════════════════════════ */
  --border-color: #DCDFE6;
  --border-light: #E5E6EB;
  --border-lighter: #F2F3F5;

  /* ═══════════════════════════════════════
     阴影
     ═══════════════════════════════════════ */
  --shadow-1: 0 1px 2px rgba(0, 0, 0, 0.04);
  --shadow-2: 0 2px 8px rgba(0, 0, 0, 0.06);
  --shadow-3: 0 4px 16px rgba(0, 0, 0, 0.08);
  --shadow-4: 0 8px 24px rgba(0, 0, 0, 0.12);
  --shadow-5: 0 12px 40px rgba(0, 0, 0, 0.16);
  --shadow-toast: 0 6px 16px rgba(0, 0, 0, 0.12);

  /* ═══════════════════════════════════════
     圆角
     ═══════════════════════════════════════ */
  --radius-none: 0;
  --radius-xs: 2px;
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-full: 9999px;

  /* ═══════════════════════════════════════
     Z-Index
     ═══════════════════════════════════════ */
  --z-base: 0;
  --z-dropdown: 1000;
  --z-header: 1001;
  --z-sidebar: 1002;
  --z-affix: 1003;
  --z-overlay: 2000;
  --z-modal: 2001;
  --z-drawer: 2002;
  --z-message: 3000;
  --z-tooltip: 4000;

  /* ═══════════════════════════════════════
     间距
     ═══════════════════════════════════════ */
  --space-1: 2px;
  --space-2: 4px;
  --space-3: 8px;
  --space-4: 12px;
  --space-5: 16px;
  --space-6: 20px;
  --space-7: 24px;
  --space-8: 32px;
  --space-9: 40px;
  --space-10: 48px;

  /* ═══════════════════════════════════════
     字体
     ═══════════════════════════════════════ */
  --font-family-sans: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  --font-family-mono: 'SF Mono', 'Roboto Mono', 'Cascadia Code', 'Menlo', 'Consolas', 'Courier New', monospace;

  --text-h1: 600 24px/32px var(--font-family-sans);
  --text-h2: 600 18px/26px var(--font-family-sans);
  --text-h3: 600 16px/24px var(--font-family-sans);
  --text-body: 400 14px/22px var(--font-family-sans);
  --text-small: 400 13px/20px var(--font-family-sans);
  --text-caption: 400 12px/18px var(--font-family-sans);
  --text-metric: 700 28px/36px var(--font-family-sans);
  --text-mini: 400 11px/16px var(--font-family-sans);

  /* ═══════════════════════════════════════
     缓动
     ═══════════════════════════════════════ */
  --ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-out: cubic-bezier(0, 0, 0.2, 1);
  --ease-in: cubic-bezier(0.4, 0, 1, 1);
  --ease-bounce: cubic-bezier(0.34, 1.56, 0.64, 1);

  --duration-fast: 0.1s;
  --duration-normal: 0.15s;
  --duration-slow: 0.2s;
  --duration-slower: 0.3s;
}
```

---

## 13. 暗色主题 CSS 变量

```css
[data-theme='dark'] {
  --bg-page: #0A1628;
  --bg-card: #132238;
  --bg-card-hover: #1A2D47;
  --bg-header: #132238;
  --bg-sidebar: #0D1B2A;

  --color-brand: #5CADFF;
  --color-brand-hover: #7EC0FF;
  --color-brand-active: #4A9AED;
  --color-brand-bg: #1A3A5C;
  --color-brand-border: #1E3A5F;

  --text-primary: #E8F0FE;
  --text-regular: #8BA4C0;
  --text-secondary: #8BA4C0;
  --text-placeholder: #567189;
  --text-disabled: #3D556E;

  --border-color: #1E3A5F;
  --border-light: #243B57;
  --border-lighter: #1A2D47;

  --shadow-1: 0 1px 2px rgba(0, 0, 0, 0.3);
  --shadow-2: 0 2px 8px rgba(0, 0, 0, 0.35);
  --shadow-3: 0 4px 16px rgba(0, 0, 0, 0.4);
  --shadow-4: 0 8px 24px rgba(0, 0, 0, 0.45);
  --shadow-5: 0 12px 40px rgba(0, 0, 0, 0.5);
  --shadow-toast: 0 6px 16px rgba(0, 0, 0, 0.4);

  --color-success: #73D13D;
  --color-warning: #FFC53D;
  --color-danger: #FF7875;
  --color-info: #BFBFBF;
}
```

---

## 14. 设计走查清单（v2.0 增强版）

- [ ] 所有按钮文案与 UX 规格一致
- [ ] 色彩体系完整应用（品牌色/中性色阶/语义色/业务色）
- [ ] 字体体系完整应用（字号阶梯/字重/行高/等宽数字）
- [ ] 间距体系完整应用（4px 网格，所有间距为 4 的倍数）
- [ ] 圆角阶梯正确映射（XS=Tag, SM=按钮/卡片, MD=弹窗, LG=抽屉, XL=指标卡）
- [ ] 阴影 6 级正确使用（卡片→Toast→下拉→弹窗→抽屉）
- [ ] z-index 层级无冲突（顶栏/侧栏/弹窗/Toast）
- [ ] 状态矩阵 4 种状态（空/加载/错误/无权限）已实现
- [ ] 选择器组件禁用手动输入
- [ ] 敏感数据脱敏展示
- [ ] 数据来源标签颜色正确
- [ ] 图表配色使用规范色板
- [ ] 暗色主题（大屏）配色完整
- [ ] 响应式断点适配（仅 Web 端 md+）
- [ ] 动画缓动函数统一使用 `--ease-in-out`
- [ ] CSS 变量统一定义（含 Element Plus 映射）
- [ ] WCAG AA 对比度通过（正文 ≥ 4.5:1，大字 ≥ 3:1）
- [ ] 所有自定义选择器组件遵循联动清空规范

---

## 附录：变更日志

| 版本 | 日期 | 更新内容 |
|------|------|---------|
| v1.0 | 2026-06-08 | 初始 UI-Design-System.md |
| v2.0 | 2026-06-08 | 基于审核增强：补充圆角阶梯、阴影 6 级、z-index 层级、缓动函数、表单控件规范、暗色主题完整变量集、图表暗色配置、Element Plus 完整映射、WCAG 对比度检查 |

---

*本文档为运营数据平台设计令牌唯一事实源。与 UX 规格冲突时以 UX 规格为准（并开 ADR 修订本文档）。*
*技术约束参考：`docs/engineering/TECH-CONSTRAINTS.md` | 全局规范参考：`docs/engineering/GLOBAL-CONVENTIONS.md`*
