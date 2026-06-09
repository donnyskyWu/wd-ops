# 前端测试访问指南

## ✅ 前端已启动

**URL**: http://localhost:3003

**项目**: `ops-platform-ui-vue`（Vue 3 + Vite 5 + Element Plus + Pinia + TypeScript）

**模式**: `VITE_USE_MOCK=true`（开 Mock 数据，不依赖后端）
> 切换到真实后端：编辑 `.env.development` 把 `VITE_USE_MOCK` 改为 `false`，重启 `npm run dev`

---

## 登录信息

- **无需登录**：当前版本未接入登录页，直接进入主应用
- **租户**：默认 default
- **数据**：所有列表/图表均为 Mock 数据（结构与后端 DTO 完全一致）

---

## 9 大模块路由清单（共 40+ 页面）

### 0. 首页仪表盘
| 路由 | 页面 | 说明 |
|---|---|---|
| `/dashboard` | 首页仪表盘 | 9 大模块 KPI 汇总、图表 |

### 1. 运营管理（M1）
| 路由 | 页面 | 测试要点 |
|---|---|---|
| `/ip-group` | IP 组管理 | 树形结构、增删改、成员管理 |
| `/author` | 作者管理 | 列表、关联实名人 |
| `/account-analysis` | 账号分析 | 趋势图、Top 榜 |
| `/fans-analysis` | 粉丝分析 | 增长曲线、画像 |
| `/works-analysis` | 作品分析 | 数据汇总 |
| `/internal-content` | 内部内容分析 | |
| `/efficiency` | 人效盘点 | |

### 2. 内容生产（M2）
| 路由 | 页面 | 测试要点 |
|---|---|---|
| `/sop` | SOP 管理 | 模板列表 |
| `/sop/:id/edit` | 编辑 SOP 模板 | LogicFlow 拖拽 |
| `/task` | 任务管理 | 多级审核 |
| `/content` | 内容管理 | 列表/详情 |
| `/knowledge` | 内容知识库 | |
| `/plan` | 计划管理 | |

### 3. 绩效核算（M3）
| 路由 | 页面 | 测试要点 |
|---|---|---|
| `/perf-template` | 考核模板 | |
| `/perf-execution` | 考核执行 | 状态机 |
| `/perf-result` | 绩效结果 | 分数/等级 |
| `/order-attribution` | 订单归因分析 | ROI |

### 4. 内部管理（M6）
| 路由 | 页面 |
|---|---|
| `/company` | 公司管理 |
| `/realname` | 实名人管理 |
| `/phone` | 手机管理 |
| `/simcard` | 手机卡管理 |
| `/internal-account` | 内部平台账号 |
| `/personal-account` | 个人账号 |
| `/triple-rel` | 三方关联统计 |

### 5. 数据分析
| 路由 | 页面 |
|---|---|
| `/metric` | 指标管理 |
| `/metric-analysis` | 指标分析 |
| `/data-report` | 数据报表 |
| `/custom-query` | 自定义查询 |
| `/data-screen` | 数据大屏 |
| `/screen-config` | 大屏配置 |
| `/funnel-analysis` | 漏斗分析 |
| `/wechat-data` | 微信数据分析 |
| `/industry-data` | 行业数据 |

### 6. 财务管理（M5）
| 路由 | 页面 |
|---|---|
| `/financial-analysis` | 总体财务分析 |
| `/account-cost` | 账号成本管理 |
| `/roi-analysis` | ROI 分析 |

### 7. 作品监测
| 路由 | 页面 |
|---|---|
| `/external-account` | 外部账号分析 |
| `/low-score` | 低分作品分析 |
| `/hot-works` | 爆款作品分析 |
| `/high-fans-account` | 高粉账号分析 |
| `/low-fans-account` | 低粉账号分析 |
| `/ip-theme` | IP 主题数据 |
| `/fans-account` | 粉丝分层分析 |

### 8. 数据采集（M10）
| 路由 | 页面 |
|---|---|
| `/config-internal-collect` | 内部采集配置 |
| `/config-external-collect` | 外部采集配置 |
| `/config-external-data` | 外部数据配置 |
| `/config-order-collect` | 订单采集配置 |
| `/config-threshold` | 阈值规则 |
| `/config-ai-model` | AI 模型 |
| `/config-ai-prompt` | AI 提示词 |

### 9. 系统管理（M9）
| 路由 | 页面 |
|---|---|
| `/system-user` | 用户管理 |
| `/system-role` | 角色权限 |
| `/system-tenant` | 租户管理 |
| `/system-param` | 系统参数 |
| `/system-dict` | 字典配置 |
| `/system-log` | 日志管理 |
| `/system-message` | 消息管理 |

---

## 重点测试建议

按你的话"我来进行测试验证"，**最值得优先验证**的页面：

1. **`/dashboard`** — 9 大模块汇总是否正常渲染
2. **`/ip-group`** — 树形组件 + 增删改（最复杂 UI）
3. **`/sop/:id/edit`** — LogicFlow 拖拽流程图
4. **`/perf-template`** — 考核模板表单（指标配置 + 权重）
5. **`/realname`** — 实名人管理（带关联关系）

---

## 切换到真实后端

```bash
# 1. 启动后端（如果还没启）
cd yudao-server
mvn -pl yudao-module-oa spring-boot:run

# 2. 改 .env
# VITE_USE_MOCK=false

# 3. 重启前端
cd ops-platform-ui-vue
npm run dev
```

后端跑起来后，登录页可见：
- 租户: default
- 账号: admin
- 密码: admin123

---

## 已知问题

1. **Mock 数据不持久** — 刷新页面会重置（设计如此）
2. **LogicFlow 拖拽需要宽度** — 建议浏览器宽度 ≥ 1280px
3. **ECharts 图表自适应** — 拖窗口大小时可能有 100ms 延迟

---

**祝测试顺利！如发现 UI/交互问题，记录路由 + 现象，我直接修。**
