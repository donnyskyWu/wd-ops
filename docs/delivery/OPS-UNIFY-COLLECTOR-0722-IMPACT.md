# unify-collector-api-0722 与 OPS 采集影响分析

日期：2026-07-24  
包：`unify-collector-api-0722.zip`  
对比基线：仓库现有 `unify-collector-api/`  
解压位置（**未覆盖**现网目录）：

| 位置 | 说明 |
|------|------|
| `unify-collector-api-0722-new/unify-collector-api/` | 工作区旁路副本（推荐对照） |
| `D:\self\sy\uca-0722\unify-collector-api\` | ASCII 短路径副本（解压中间产物） |
| `unify-collector-api/` | **已于 2026-07-24 替换为 0722**（见 §7） |

解压说明：zip 含 macOS `__MACOSX`、`.venv`、符号链接等，Windows 全量解压易失败；已用 Python 选择性解压（跳过 `.venv` / `.tmp` / `__MACOSX`），源码与文档足够对比。

---

## 1. Changelog 摘要（相对现有目录）

### 未变（对 OPS 联调友好）

- `README.md`、`.env.example`、`Dockerfile`、`requirements.txt`、`pyproject.toml`、`run.py`、`app/router.py` **内容一致**
- 认证方式仍为 `Authorization: Bearer $API_TOKEN`
- OPS 已用的 **路径字面量无增删**（`/api/v1/accounts/*`、`/api/v1/auth/*`、`/api/v1/internal/{douyin,kuaishou,wechat-mp,wechat-channels,xiaohongshu,bilibili}/*`、`/api/v1/external/kuaishou/*` 等）
- 主导入接口 `POST /api/v1/accounts/import` **未见去重改写**（与 OPS `UnifiedCollectorApiClient#importAccount` 对齐）

### 新增能力（多为增强 / 外部竞品向）

| 类别 | 内容 |
|------|------|
| 抖音搜索稳态 | `platforms/douyin/search_service.py`、`sign_engine.py`；外部搜索改走 Playwright 浏览器内签名 + 账号池/冷却 |
| 快手搜索 | `browser_engine.py`、`search_service.py`；新增 `search-user`、cookie 登录分前台/创作者后台 |
| 公众号采集队列 | `collection_service` / `task_queue` / `author_subscriptions` / `account_pool`；启动时拉起 worker + 作者调度 |
| 公众号外部 API | `external_router` 新增作者订阅/采集/任务查询/本地文章搜索等（前缀 `/api/v1/external/wechat-mp`） |
| 平台字面量扩展 | `douyin_front`、`kuaishou_front`、`xiaohongshu_cp`（模型/导入 cookie 探活） |
| 保活 | 抖音/快手/小红书前台保活策略；公众号探活改首页校验并默认 12h 间隔（`WECHAT_MP_KEEPALIVE_INTERVAL_SECONDS`） |
| 运维文档 | `SETUP_3PLATFORM.md`（视频号三端 + `wx_channels_download`） |
| 连接池 | `app/core/http_pool.py`、`playwright_pool.py` |
| QR | `qr_manager` 大幅增强；**强制 headless=True**（即使旧配置写 headed） |

### 行为变更（需关注，多数非 OPS 主路径）

1. **`POST /api/v1/accounts/import-cookie`**：同平台已有 `active/relogin_needed` 账号时 **复用 account_id 更新 credential**，并写 `credential_version`；主路径 `/import` 未改。
2. **公众号 internal 登录态检查**：由「有 cookie/token 即 logged_in」改为真实探活；可能返回 `logged_in: null`（暂时无法确认）。
3. **抖音/快手外部搜索**：实现与依赖浏览器签名变化，响应包装仍走既有 external 路由，但稳定性/字段细节需实机验证。
4. **QR**：服务端强制无头浏览器；Docker/无显示环境更友好，本地依赖有头调试时需知悉。

### 配置 / Docker / Auth

- `.env.example` 无新必填项；auth 仍 `API_TOKEN`
- `app/config.py` 仅增可选：`WECHAT_MP_KEEPALIVE_INTERVAL_SECONDS`（默认 43200）
- Dockerfile 未改；README「130 API」目录未同步写明全部新 external 路由（文档滞后于代码）

文件统计（跳过 venv/tmp 等）：约 **+28 / -2 / ~29 changed**（中文手册文件名在 zip 侧编码异常，表现为「删除旧名 + 乱码新名」，实质为同内容文档打包问题）。

---

## 2. OPS 当前如何调用 collector

| OPS 组件 | 作用 |
|----------|------|
| `oa.unified-collector.*`（`UnifiedCollectorProperties`） | `base-url` / `api-token` / `timeout-ms` / `stub` |
| `UnifiedCollectorApiClient` | HTTP：账号 import/health/relogin、扫码 auth、各平台 **internal** 粉丝/作品/文章 |
| `UnifiedCollectorAdapter` + 各 `*SyncService` | Channel-A 同步入 OA 表 |
| `CollectorCredentialBuilder` | OA 平台 → collector：`WECHAT_OFFICIAL→wechat_mp`、`DOUYIN→douyin`、`KUAISHOU→kuaishou` 等（**非** `*_front`） |
| `CollectorQrLoginSupport` | 扫码平台映射同上 |
| `ExternalCollectorApiClient` | 目前主要 `GET /api/v1/external/kuaishou/user-videos` |

默认联调：`http://127.0.0.1:8000` + Bearer `test-key-2026`。

---

## 3. Breaking？OPS 要不要改代码？

### 结论：**现有 OPS Channel-A 主链路无需为 0722 改代码**

- 已用 endpoint 路径保持兼容；auth / env 契约不变。
- OPS 走 `/accounts/import` + internal 同步，不走本次大改的 `import-cookie` / external 搜索 / 作者订阅 API。
- 平台映射仍用 `douyin` / `kuaishou`（创作者侧），与新增 `*_front` 搜索池分离。

### 风险与联调注意（非代码必改）

| 风险 | 说明 | 建议 |
|------|------|------|
| 公众号探活更严 | keepalive/登录检查更真实，失效账号更快标过期 | 升级后跑公众号粉丝/文章同步冒烟 |
| QR 强制无头 | 服务器部署更稳；本机 headed 调试行为变化 | 重跑 `M10ApiCollectorQrLogin*` / 人工扫码 |
| Playwright 内存 | 搜索/签名引擎常驻浏览器；README 提示勿多 worker | 单进程 + 容器水平扩展；观察内存 |
| 外部搜索语义 | 若后续 OPS 用抖音/快手 search | 单独契约测试字段与错误码 |
| 新 external 公众号作者 API | OPS 尚未接入 | 产品若要「竞品公众号订阅采集」再开 Slice |

### 若要吃到新能力，OPS 可选后续工作

1. **竞品/搜索**：对接 `/api/v1/external/douyin/search-*`、快手 `search-user`、公众号 `/api/v1/external/wechat-mp/authors*`（需 Spec/Slice，禁止推断字段）。
2. **前台 Cookie 池**：若搜索要稳定，需运营导入 `douyin_front` / `kuaishou_front` 账号（与现有创作者绑定分离）。
3. **视频号三端**：按 `SETUP_3PLATFORM.md` 部署 `wx_channels_download`，与 OA `wechat_channels` internal 联调。

---

## 4. 建议下一步

1. **暂不原地替换** `unify-collector-api/`；用旁路目录完成对比与冒烟后再切换。
2. 旁路启动 0722（新建 venv，勿用 zip 内损坏的 `.venv`），对齐 `API_TOKEN`，设 `oa.unified-collector.stub: false`。
3. 冒烟清单（P0）：
   - `POST /api/v1/accounts/import` + health
   - 扫码 `auth/qrcode` + poll（至少抖音/公众号之一）
   - internal：公众号 follower/article、抖音 follower-stats/video-list、快手 video-list、视频号 follower-stats
   - （若用）external 快手 `user-videos`
4. 冒烟通过后：备份现目录 → 用 0722 源码替换（保留本地 `.env` / `data/`）→ 再跑一轮 OPS IT/联调。
5. README/手册与新 external 路由同步更新交由 collector 侧；OPS 侧更新部署指南时注明 0722 增强点即可。

---

## 5. 替换决策

| 决策 | 建议 |
|------|------|
| 现在是否覆盖 `unify-collector-api/`？ | **否**（分析完成前已按此执行） |
| 是否可升级？ | **可以**，对现有 OPS 为兼容增强升级；先冒烟再替换 |
| OPS 代码是否阻塞？ | **不阻塞**；无强制改 `UnifiedCollectorApiClient` |
| 新功能接入？ | 另开 Spec/Slice，勿在本期 Gate 外推断实现 |

---

## 6. 旁路冒烟报告（2026-07-24）

### 环境

| 项 | 值 |
|----|----|
| CWD | `unify-collector-api-0722-new/unify-collector-api`（旁路；未改现网 `unify-collector-api/`） |
| 端口 | **8001**（`.env` `PORT=8001`；现网目录仍为 8000，本次未占用/未替换） |
| 启动 | `.venv_new\Scripts\python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8001` |
| 鉴权 | Bearer `API_TOKEN`（旁路 `.env` 内单独配置；**未覆盖**现网 `.env`） |

### 冒烟结果表（OPS Channel-A）

| 用例 | 方法/路径 | HTTP | 判定 | 说明 |
|------|-----------|------|------|------|
| livez | GET `/livez` | 200 | PASS | |
| docs | GET `/docs` | 200 | PASS | |
| openapi | GET `/openapi.json` | 200 | PASS | |
| health | GET `/api/health` | 200 | PASS | components: database/qr/patrol ok |
| 无 Token | GET `/api/v1/accounts` | 401 | PASS | 鉴权生效 |
| 有 Token | GET `/api/v1/accounts` | 200 | PASS | |
| accounts health | GET `/api/v1/accounts/health` | 200 | PASS | OPS 使用 |
| accounts relogin | GET `/api/v1/accounts/relogin` | 200 | PASS | OPS 使用 |
| import | POST `/api/v1/accounts/import` | 200 | PASS | 最小 douyin cookie → `account_id` |
| import 后 profile | GET `/api/v1/internal/douyin/accounts/{id}/profile` | 200 | PASS | |
| internal douyin video-list / follower-stats | GET + `account_id` | 404 biz | PASS | OpenAPI 有路由；缺账号业务 404 |
| internal kuaishou video-list / follower-stats | GET + `account_id` | 404 biz | PASS | 同上 |
| internal wechat-mp follower-stats / article-list | GET | 401 / 422 | PASS | 路由存在（非路径 404） |
| internal wechat-channels video-list / follower-stats | GET | 404 biz | PASS | OpenAPI 有路由 |
| internal xiaohongshu follower-stats | GET | 404 biz | PASS | OpenAPI 有路由 |
| internal bilibili user/me | GET | 200 | PASS | |
| external kuaishou user-videos | GET | 502 | WARN | 路由存在；上游/凭据失败，非 404 |
| QR start | POST `/api/v1/auth/qrcode` | 200 | PASS | 返回 `qrcode_base64` |
| QR poll | GET `/api/v1/auth/poll` | 200 | PASS | `status=pending` |
| QR cancel | DELETE `/api/v1/auth/qrcode` | 200 | PASS | |

### 合并建议

| 决策 | 结论 |
|------|------|
| 是否可合并/升级替换现网 collector？ | **建议：是（条件合并）** — Channel-A 关键路径（import / accounts health·relogin / auth QR / 各平台 internal 路由）冒烟通过 |
| 现在是否立刻覆盖 `unify-collector-api/`？ | **仍否** — 本次仅旁路验证；覆盖前需：备份现网 `.env`+`data/`、对齐 `API_TOKEN`、切 OPS `base-url` 到新实例后再做一次联调 |
| 残留风险 | ① external 快手 `user-videos` 本次 502（网络/Cookie，需真实凭据复测）；② QR 强制 headless（见上文影响分析）；③ 旁路生成了 smoke 测试账号，合并前注意 `data/` 是否一并迁移 |

**一句话**：旁路 8001 冒烟对 OPS Channel-A **通过**，可进入「备份后替换」；**在替换完成前不要动现网目录**。

---

## 7. 合并完成记录（2026-07-24）

用户确认后，已将旁路冒烟通过的 **0722** 包原地替换进现网目录。

| 项 | 值 |
|----|----|
| 源 | `unify-collector-api-0722-new/unify-collector-api/` |
| 目标 | `unify-collector-api/` |
| 备份 | `scripts/logs/unify-collector-backup-20260724/`（含 `.env`、`data/`） |
| 保留 | 替换后已恢复备份的 `.env` + `data/`；`API_TOKEN` 对齐 OPS `oa.unified-collector.api-token` = `test-key-2026`；`PORT=8000` |
| 启动 | 现网树 `:8000`（使用旁路已验证的 `.venv_new` 解释器跑 `uvicorn app.main:app`） |
| 嵌套 git | `unify-collector-api/` 为独立 git（`main` ahead origin 1，替换后工作区约 90+ 变更）；**未在本步骤提交/推送** |

### 替换后 :8000 快速复冒烟

| 用例 | 结果 |
|------|------|
| GET `/livez` | 200 PASS |
| GET `/api/health` | 200 PASS |
| GET `/api/v1/accounts`（无 Token） | 401 PASS |
| GET `/api/v1/accounts`（Bearer OPS token） | 200 PASS |
| GET `/api/v1/accounts/health` | 200 PASS |
| POST `/api/v1/accounts/import`（douyin 最小 cookie） | 200 PASS（返回 `account_id`） |

**状态**：0722 **已合并进** `unify-collector-api/`；旁路目录可保留作对照，现网默认以 `:8000` 为准。