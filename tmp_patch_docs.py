# -*- coding: utf-8 -*-
from pathlib import Path
import re

root = Path(r"d:/self/sy/运营数据平台/202606/wd/docs/delivery")
prog = root / "INTEGRATION-PROGRESS.md"
s0 = root / "INTEGRATION-S0-Football-Ops.md"

def read(p):
    raw = p.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        return raw.decode("utf-8-sig"), True
    return raw.decode("utf-8"), False

def write(p, text, had_bom):
    enc = "utf-8-sig" if had_bom else "utf-8"
    p.write_text(text, encoding=enc)

text, bom = read(prog)

# Update last updated date in header if present
text = re.sub(r"> \*\*最后更新\*\*：[^\n]+", "> **最后更新**：2026-07-03", text, count=1)

# Section 3 S1-B row - replace blocker text
text = text.replace(
    "| **S1-B** 鉴权与对齐 | 🔵 **当前** | 本地 Nacos Docker 脚本、fail-soft 联调 | **5777 登录/bootstrap 503**；system-server 栈未齐；Nacos `local` vs `dev` 不一致 | 拉起 system/member/mp + Redis；对齐 namespace |",
    "| **S1-B** 鉴权与对齐 | 🔵 **当前** | 本地 Nacos Docker 脚本、fail-soft；**`system_*` 租户库已导入 `wd`** | **登录仍 500**：member-server :48087 未起（Feign `AuthorApi`）；infra :48082 日志 RPC 可选 | `scripts/start-integration-system.ps1` 拉起 member/mp/infra |",
)

# Service matrix updates (partial line replacements)
replacements = [
    ("| **system-server** | 48081 | ❌ 未稳定运行 | bootstrap `/admin-api/system/**` 503 典型 |",
     "| **system-server** | 48081 | ✅ UP | Gateway tenant API `code=0`；登录依赖 member-server |"),
    ("| **oa-server** | 48094 | ✅ UP（fail-soft） | 注册到 namespace **`dev`**（与 Gateway **`local`** 不一致） |",
     "| **oa-server** | 48094 | ✅ UP（fail-soft） | discovery.namespace **`local`**（`application-dev-nacos-local.yml`） |"),
    ("| **football-front** | 5777 | ⚠️ 壳层 200；bootstrap 失败 | 缺 system-server 栈 |",
     "| **football-front** | 5777 | ✅ 壳层 200；**bootstrap 绿** | 经 Vite 代理 tenant/simple-list `code=0` |"),
]
for a,b in replacements:
    if a in text:
        text = text.replace(a,b)

# Section 5 - refresh blocker list intro
old5 = "**未验证通过**：5777 登录页不可用，**待环境修复**。"
new5 = "**5777 bootstrap 已通过**（2026-07-03）；**登录仍未通过**（`/admin-api/system/auth/login` 返回 `code=500`，system-server 日志：Feign → member-server :48087 Connection refused）。"
if old5 in text:
    text = text.replace(old5, new5)

# Section 9 verification block
old9 = "**验证**：48080 tenant HTTP 200 但 body code=503；48094 UP；5777 页面可开；system API 仍 503。"
new9 = """**验证（2026-07-03 复测）**：48080/48081/48094/5777 端口 LISTEN；Gateway `tenant/simple-list` 与 `get-id-by-name`（URL 编码）均 **HTTP 200 + code=0**；5777 代理 tenant **code=0**；login **code=500**（member-server 未起）。

**Football `system_*` DDL/数据导入（租户库阻塞已解除）**：已写入 **`101.37.161.136:3306/wd`**。运行时脚本：`scripts/start-football-system.ps1`、`scripts/start-integration-system.ps1`；Nacos 覆盖配置目录 **`scripts/integration-config/`**（推送 `scripts/push-integration-config-to-nacos.ps1`）。"""

if old9 in text:
    text = text.replace(old9, new9)
else:
    # append if pattern changed
    if "**Football `system_*` DDL" not in text:
        text = text.rstrip() + "\n\n" + new9 + "\n"

# Section 9 remaining line
text = text.replace(
    "**剩余**：Nacos/Redis/Gateway/oa/5777 壳层 UP；system/member/mp DOWN（wx.appId、im.appId、taskExecutor 等待配置）",
    "**剩余**：member-server :48087、infra-server :48082、mp-server 未稳定 UP（阻塞 login 与访问日志 RPC）",
)

write(prog, text, bom)

# INTEGRATION-S0 section 6.2
text2, bom2 = read(s0)
text2 = re.sub(r"> \*\*一页速览\*\*：\[INTEGRATION-PROGRESS\.md\]\(\./INTEGRATION-PROGRESS\.md\) · 更新[^\n]+",
               "> **一页速览**：[INTEGRATION-PROGRESS.md](./INTEGRATION-PROGRESS.md) · 更新：2026-07-03", text2, count=1)

text2 = text2.replace(
    "| **S1-B** 结构对齐 | 🔵 进行中 | `football-module-oa/` 方案（§3.2）；fail-soft 本地路径 | — | 新建 sibling 工程 |",
    "| **S1-B** 结构对齐 | 🔵 进行中 | **`system_*`→`wd` 导入完成**；5777 bootstrap 绿 | login 500（member :48087） | `start-integration-system.ps1` |",
)
text2 = text2.replace(
    "| **S2** M9 + 菜单 | ⬜ 待开始 | — | **system-server 栈未就绪** | CSV + Flyway seed |",
    "| **S2** M9 + 菜单 | ⬜ 待开始 | — | member/infra 微服务 | CSV + Flyway seed |",
)
text2 = text2.replace(
    "| **S4** 5777 壳 | ⬜ 待开始 | `.env` → Gateway | **5777 bootstrap 503（未验证修复）** | system + Gateway + 前端 smoke |",
    "| **S4** 5777 壳 | 🟡 部分 | `.env` → Gateway | **登录未绿**（auth/login code=500） | member-server + 前端登录 smoke |",
)
text2 = text2.replace(
    "**5777 状态**：Vite 壳层可开；登录/bootstrap **未通过**（Gateway 无 system-server 不可用）。",
    "**5777 状态（2026-07-03）**：壳层可开；**bootstrap 绿**（tenant API code=0）；**登录未通过**（需 member-server）。DDL 导入脚本索引见 [INTEGRATION-PROGRESS §9](./INTEGRATION-PROGRESS.md#9-s1-b-状态快照2026-07-03)。",
)

write(s0, text2, bom2)
print("updated", prog.name, s0.name)
