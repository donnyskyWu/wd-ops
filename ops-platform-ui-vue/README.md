# 运营数据分析平台 - Vue3版本

## 技术栈

- **前端框架**: Vue 3.4 + Composition API
- **构建工具**: Vite 5
- **UI组件库**: Element Plus 2.6
- **状态管理**: Pinia 2.1
- **路由管理**: Vue Router 4.3
- **HTTP客户端**: Axios 1.6
- **图表库**: ECharts 5.5 + vue-echarts 6.6
- **样式预处理**: Sass
- **语言**: TypeScript 5.4

## 项目结构

```
ops-platform-ui-vue/
├── src/
│   ├── api/              # API接口
│   ├── components/       # 公共组件
│   ├── router/          # 路由配置
│   ├── stores/          # Pinia状态管理
│   ├── styles/          # 全局样式
│   ├── types/           # TypeScript类型定义
│   ├── utils/           # 工具函数
│   ├── views/           # 页面组件
│   │   ├── operations/  # 运营管理模块
│   │   ├── production/  # 内容生产模块
│   │   ├── performance/ # 绩效核算模块
│   │   ├── internal/    # 内部管理模块
│   │   ├── finance/     # 财务管理模块
│   │   ├── analysis/    # 数据分析模块
│   │   ├── monitor/     # 作品监测模块
│   │   ├── system/      # 系统管理模块
│   │   └── data-collection/ # 数据采集模块
│   ├── App.vue
│   └── main.ts
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## 快速开始

### 安装依赖

```bash
npm install
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

### 预览生产构建

```bash
npm run preview
```

## 开发规范

严格遵循以下规范：
- PRD v9.0 完整需求文档
- AI开发规范 - 全局开发规范
- 各模块页面规格文档

## 模块清单

### 1. 概览
- ✅ 首页仪表盘

### 2. 运营管理 (7个模块)
- IP组管理
- 作者管理
- 账号分析
- 粉丝分析
- 作品分析
- 内部内容分析
- 人效盘点

### 3. 内容生产 (5个模块)
- SOP管理
- 计划管理
- 任务管理
- 内容管理
- 内容知识库

### 4. 绩效核算 (4个模块)
- 考核模板
- 考核执行
- 绩效结果
- 订单归因分析

### 5. 内部管理 (7个模块)
- 公司管理
- 实名人管理
- 手机管理
- 手机卡管理
- 内部平台账号管理
- 个人账号管理
- 三方关联统计

### 6. 财务管理 (3个模块)
- 账号成本管理
- ROI分析
- 总体财务分析

### 7. 数据分析 (8个模块)
- 指标管理
- 数据报表
- 总体财务分析
- 漏斗分析
- 自定义查询
- 数据大屏
- 大屏配置
- 微信数据分析

### 8. 作品监测 (5个模块)
- 爆款分析
- 低分分析
- 外部作品监控
- 高粉/低粉账号
- IP主题与行业数据

### 9. 数据采集 (2个模块)
- 采集任务管理
- 数据质量

### 10. 系统管理 (4个模块)
- 配置管理
- 角色权限
- 用户管理
- 操作日志

**总计**: 42个功能模块

## Mock数据

所有模块使用Mock数据进行展示，确保界面效果符合PRD要求。

## 注意事项

1. 本项目严格按照PRD v9.0和AI开发规范实现
2. 使用Element Plus组件库，符合管理系统设计规范
3. 采用深色侧边栏 + 浅色内容区布局
4. 所有API路径遵循 `/admin-api/oa` 前缀规范
5. 分页参数统一使用 `pageNo/pageSize`
6. TypeScript接口字段使用 camelCase
7. 错误码范围 1001-1999

## 版本信息

- 版本: v1.0.0
- 创建日期: 2026-05-28
- 技术栈: Vue 3 + Element Plus + TypeScript
