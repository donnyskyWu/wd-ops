# IP业务月达成 Beta 环境验证报告

**日期** 2026-08-21 · **环境** Gateway `:48080` / Frontend `:5777` · admin/admin123 tenant=1 · 查询月份 `2026-08`

## 结论

**前后端逻辑与此前说明一致，执行正确。** L/R/S/V 有数值（含 0）；M~Q、T~U 固定 `—`（MVP 未接入）。

## API 验证

脚本：`python scripts/integration-config/smoke_private_domain_report_api.py docs/delivery/e2e-artifacts/PRIVATE-DOMAIN-VERIFY-BETA-20260821`

| 端点 | 结果 |
|------|------|
| authors | code=0 · 4 作者 |
| monthly-achievement?month=2026-08 | code=0 · 4 行 |
| weekly-funnel | code=0 · 12 行 |

## UI E2E 验证

Spec：`football-front/apps/web-ele/tests/monthly-achievement-verify.spec.ts`

| 项 | 结果 |
|----|------|
| 页面加载 + 查询 | ✅ |
| 浏览器内 API（含 X-Tenant-Id） | code=0 · 4 行 |
| 截图 | `03-monthly-achievement-beta.png` |

## L~Q / R~V 实测（首行：解说员浩南）

| 列 | API 值 | 页面显示 | 判定 |
|----|--------|----------|------|
| L 新会员注册 | 1 | **1** | ✅ 已接入，有数据 |
| M 注册率 | null | **—** | ✅ MVP 未做 |
| N~Q | null | **—** | ✅ MVP 未做 |
| R 新粉付费人数 | 0 | **0** | ✅ 已接入，无订单 |
| S 首单付费率 | 0 | **0.00%** | ✅ 已接入（L>0 时显示） |
| T~U | null | **—** | ✅ MVP 未做 |
| V 开单金额 | 0 | **0.00** | ✅ 已接入，无订单 |

其余 3 作者：L=0 → S 显示 **—**（分母为 0，无法算率）；R/V 为 **0 / 0.00**。

## 备注

- 当前 ops 进程 profile 为 `local`（非 dev-test-beta）；API 仍正常返回 4 行。
- 付费相关列全 0：`pay_all_order` 在 2026-08 无该作者已支付订单。
