# DINGTALK 系统参数 category 1503 修复 — E2E 报告 2026-08-02

## 结果：**PASS**

| 检查项 | 结果 |
|--------|------|
| 按 category=DINGTALK 列表 | PASS（8/8 参数） |
| update dingtalk.enabled | PASS code=0 |
| update dingtalk.client-id | PASS code=0 |
| update dingtalk.client-secret | PASS code=0 |
| update dingtalk.corp-id | PASS code=0 |
| update dingtalk.agent-id | PASS code=0 |
| update dingtalk.robot.enabled | PASS code=0 |
| update dingtalk.robot.webhook-url | PASS code=0 |
| update dingtalk.robot.secret | PASS code=0 |
| GET /ops/dev/dingtalk/status | PASS code=0 |

## 根因

保存 `dingtalk.client-id` 报 **category: 字典值不合法（1503）**：

1. **V170** 已在 `sys_param` 写入 8 个钉钉参数（`category=DINGTALK`）。
2. **`@InDict("dict_param_category")`**（`ParamUpdateReq.category`）经 Feign 读 **shenyu-system.system_dict_data**，不是 ops 本地表。
3. Beta DB 中 `dict_param_category` 仅有 BASIC / COLLECT / AI / NOTIFICATION / CONTENT_REVIEW，**缺少 DINGTALK**。
4. 原 **V171** 先 `INSERT INTO sys_dict_data`，但该表在 **V163** 已归档（beta 仅余 `archive_sys_dict_data`），迁移无法执行；Flyway 停在 V167，V171 从未生效。
5. V171 的 cross-DB 同步依赖 `sys_dict_data` 源行，beta 远程库又无 cross-DB grant。

## 修复

| 文件 | 变更 |
|------|------|
| `V171__param_category_dingtalk_content_review.sql` | 重写：直接向 `shenyu-system.system_dict_data` 幂等插入 DINGTALK + CONTENT_REVIEW |
| `scripts/integration-config/apply_v171_param_category.py` | Beta/远程库手工 apply 脚本（shenyu-system 凭证） |

Beta 已执行 apply 脚本，插入 `dict_param_category / DINGTALK`（id=3738）。

## 前端

`ParamManage.vue` 无 FE 阻断：编辑时 `category` 来自行数据，`paramKey` 只读，BOOLEAN 下拉已就绪，hyphen 键名校验已放宽。无需 FE 改动。

## 复现命令

```powershell
# Beta 远程库补 dict（若 Flyway V171 未跑通）
python scripts/integration-config/apply_v171_param_category.py

# E2E 冒烟（ops-server :48094 + system-server :48081）
python docs/delivery/e2e-artifacts/DINGTALK-PARAM-E2E-20260802/smoke_dingtalk_param_update.py
```

## 用户操作

- **Beta 环境**：已 apply，**无需重启**；刷新页面即可保存钉钉配置。
- **本地 multidb**：重启 ops-server 让 Flyway V171 执行，或运行 `apply_v171_param_category.py`。
- **DictService 缓存**：`@InDict` 校验不走 5 分钟 list 缓存，apply 后立即可用。

## 产物

- `RESULTS.json` — 全量检查结果
- `00-login.json` / `param-dingtalk-list.json` / `update-*.json` / `dingtalk-status.json`
