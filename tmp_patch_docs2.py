# -*- coding: utf-8 -*-
from pathlib import Path

def load(p):
    raw = p.read_bytes()
    bom = raw.startswith(b"\xef\xbb\xbf")
    text = raw.decode("utf-8-sig" if bom else "utf-8")
    return text, bom

def save(p, text, bom):
    p.write_text(text, encoding="utf-8-sig" if bom else "utf-8")

prog = Path(r"d:/self/sy/运营数据平台/202606/wd/docs/delivery/INTEGRATION-PROGRESS.md")
text, bom = load(prog)
lines = text.splitlines()
new_lines = []
for line in lines:
    if line.startswith("| **S1-B**"):
        line = "| **S1-B** 鉴权与对齐 | 🔵 **当前** | 本地 Nacos Docker 脚本、fail-soft；**`system_*` 租户库已导入 `wd`** | **登录仍 500**：member-server :48087 未起（Feign `AuthorApi`） | `scripts/start-integration-system.ps1` |"
    elif "| **system-server** | 48081 |" in line:
        line = "| **system-server** | 48081 | ✅ UP | Gateway tenant API `code=0`；登录依赖 member-server |"
    elif "| **oa-server** | 48094 |" in line:
        line = "| **oa-server** | 48094 | ✅ UP（fail-soft） | discovery.namespace **`local`**（`application-dev-nacos-local.yml`） |"
    elif "| **football-front** | 5777 |" in line:
        line = "| **football-front** | 5777 | ✅ 壳层 200；**bootstrap 绿** | 经 Vite 代理 tenant/simple-list `code=0` |"
    elif line.startswith("**未验证通过**"):
        line = "**5777 bootstrap 已通过**（2026-07-03）；**登录仍未通过**（`/admin-api/system/auth/login` → `code=500`；system-server 日志 Feign member-server :48087 Connection refused）。"
    elif line.startswith("**剩余**"):
        line = "**剩余**：member-server :48087、infra-server :48082、mp-server 未稳定 UP（阻塞 login 与访问日志 RPC）"
    elif line.startswith("**验证**") and "503" in line and "复测" not in line:
        continue  # drop stale line
    new_lines.append(line)

# dedupe adjacent blank from removed line - ok
text = "\n".join(new_lines) + "\n"
text = text.replace("> **最后更新**：2026-06-11", "> **最后更新**：2026-07-03")
save(prog, text, bom)

s0 = Path(r"d:/self/sy/运营数据平台/202606/wd/docs/delivery/INTEGRATION-S0-Football-Ops.md")
text2, bom2 = load(s0)
lines2 = []
for line in text2.splitlines():
    if "一页速览" in line and "INTEGRATION-PROGRESS" in line:
        line = "> **一页速览**：[INTEGRATION-PROGRESS.md](./INTEGRATION-PROGRESS.md) · 更新：2026-07-03"
    elif line.startswith("| **S1-B** 结构对齐"):
        line = "| **S1-B** 结构对齐 | 🔵 进行中 | **`system_*`→`wd` 导入完成**；5777 bootstrap 绿 | login 500（member :48087） | `start-integration-system.ps1` |"
    elif line.startswith("| **S2** M9"):
        line = "| **S2** M9 + 菜单 | ⬜ 待开始 | — | member/infra 微服务 | CSV + Flyway seed |"
    elif line.startswith("| **S4** 5777"):
        line = "| **S4** 5777 壳 | 🟡 部分 | `.env` → Gateway | **登录未绿**（auth/login code=500） | member-server + 前端登录 smoke |"
    elif line.startswith("**5777 状态**"):
        line = "**5777 状态（2026-07-03）**：壳层可开；**bootstrap 绿**（tenant API code=0）；**登录未通过**（需 member-server）。DDL/启动脚本见 [INTEGRATION-PROGRESS §9](./INTEGRATION-PROGRESS.md#9-s1-b-状态快照2026-07-03)。"
    lines2.append(line)
save(s0, "\n".join(lines2) + "\n", bom2)
print("done")
