# 前后端联调指南

> 适用版本：S7+
> 目的：让前端和后端在每次发版前都能**真实走通 HTTP** 而不只是单元测试

---

## 1. 一图流

```
   前端 (Vue 3)          后端 (Spring Boot)
   ┌──────────┐          ┌──────────────────┐
   │ axios    │  HTTP    │  Tomcat 8080     │
   │ /api/xxx │ ──────►  │  /oa/...         │
   │          │ ◄──────  │  拦截器+Token    │
   └──────────┘          └──────────────────┘
                                  │
                                  ▼
                            MySQL :3306
                            (Flyway 迁移)
```

## 2. 联调前置条件

| 组件 | 端口 | 检查命令 |
|---|---|---|
| MySQL | 3306 | `mysql -h localhost -u root -p` |
| 后端 | 8080 | `curl http://localhost:8080/oa/auth/login` |
| 前端 | 5173 | 浏览器打开 `http://localhost:5173` |

## 3. 启动顺序

```powershell
# 1. 数据库（一次性）
python db_init.py

# 2. 后端
cd yudao-server
mvn -pl yudao-module-oa spring-boot:run

# 3. 前端
cd yudao-ui-admin-vue3
pnpm install
pnpm dev

# 4. 浏览器
# 打开 http://localhost:5173
# 租户: default
# 账号: admin
# 密码: admin123
```

## 4. 验证清单

### 4.1 登录态
- [ ] 输入 admin/admin123，能跳转到首页
- [ ] Token 存到 localStorage
- [ ] 刷新页面不退出

### 4.2 9 大模块
- [ ] **M1 运营管理** 列表 / 创建 / 编辑 / 删除 / 详情
- [ ] **M2 内容生产** SOP 模板 / 任务 / 多级评审
- [ ] **M3 绩效核算** 模板 / 记录 / 指标 / 自动算分
- [ ] **M4 账号管理** 平台 / 账号 / 状态
- [ ] **M5 财务管理** 订单归因 / 收入分账
- [ ] **M6 手机卡** 设备 / SIM 卡 / 绑定
- [ ] **M7 订单** 列表 / 归因
- [ ] **M8 数据字典** 字典类型 / 字典项
- [ ] **M10 数据采集** 任务 / 执行 / 代理池

### 4.3 跨模块
- [ ] 创建账号 → 自动可见于"作者选择"
- [ ] 创建 SOP 任务 → 完成后能算绩效
- [ ] 创建采集任务 → 触发 → 数据落入 oa_collect_metric → 自动驱动 oa_perf_item_record
- [ ] 字典变更 → 列表页选项自动更新

### 4.4 错误处理
- [ ] 未登录访问 → 401/跳登录
- [ ] 重复字段 → 提示 DICT_VALUE_INVALID / *NOT_FOUND
- [ ] 短时间高频请求 → 提示 RATE_LIMITED

## 5. 自动化脚本

```powershell
# L1 单元
mvn -pl yudao-server/yudao-module-oa test -Dtest=*Test

# L2 集成（MockMvc + 真 MySQL）
mvn -pl yudao-server/yudao-module-oa test -Dtest=*IT

# L3 接口冒烟
python s7_curl_smoke.py

# L4 端到端（含 UI）
python s7_playwright_e2e.py
```

## 6. 常见问题

| 问题 | 排查 |
|---|---|
| 登录 401 | 检查 `oa.auth.token-secret` 与前端 env 一致 |
| CORS 报错 | 后端 `WebMvcConfigurer` 需放行 `http://localhost:5173` |
| 限流触发 | 调整 `oa.auth.login.rate-limit.permits-per-second` |
| Flyway 报错 | 先跑 `python db_init.py` 重新建库 |

## 7. 何时更新本文档

- 新增模块
- 调整 API 路径
- 调整前端端口
- 调整登录态方案
