#!/usr/bin/env python3
"""Generate docs/delivery/OPS-MENU-LIST.md from seed SQL + CSV."""
from __future__ import annotations

import csv
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SQL = ROOT / "scripts/integration-config/seed-oa-system-menu.sql"
CSV = ROOT / "docs/delivery/oa-menu-permission-map.csv"
OUT = ROOT / "docs/delivery/OPS-MENU-LIST.md"

PAT = re.compile(r"VALUES \((\d+), '([^']*)', '([^']*)', (\d+), \d+, (\d+),")

DESC: dict[str, str] = {
    "运营数据": "运营数据分析平台根目录，聚合全部业务模块入口。",
    "作品监测": "内外部作品效果监控与竞品分析模块目录。",
    "内容生产": "内容从计划、创作、审核到发布的标准化全流程模块目录。",
    "数据分析": "指标定义、标准报表、深度分析和可视化大屏模块目录。",
    "数据采集": "保障数据新鲜度与质量的采集任务与质量监控模块目录。",
    "系统管理(OA)": "平台基础治理能力，含参数、字典、日志与消息管理。",
    "绩效核算": "将业务数据转化为可量化绩效评价的考核模块目录。",
    "财务管理": "精确核算账号投入产出的成本与 ROI 模块目录。",
    "账号管理": "系统化管理账号背后公司、实名人、手机/卡及平台账号资产。",
    "运营管理": "围绕 IP 组串联日常运营数据视图的中枢模块目录。",
    "配置管理": "支撑数据采集、阈值规则和 AI 能力的基础配置模块目录。",
    "首页仪表盘": "每日晨会看板，按 IP 组查看核心指标、趋势与待办提醒。",
    "外部账号分析": "监控竞品平台外部账号动态，支撑竞品研究与创作参考。",
    "高粉账号分析": "按粉丝分层识别高粉账号，分析头部账号传播特征。",
    "爆款作品分析": "按阈值规则自动识别爆款作品，分析传播特征指导复制。",
    "IP主题数据": "按 IP 主题维度聚合竞品与内外部内容数据。",
    "低粉账号分析": "识别低粉账号分布，辅助账号运营策略调整。",
    "低分作品分析": "按阈值规则识别低分作品，触发优化提醒与复盘改进。",
    "内容管理": "内容全生命周期管理，含 AI 辅助创作、三级审核与发布。",
    "内容审核": "对待审内容进行初审、复审、终审三级审核流转。",
    "内容知识库": "沉淀优秀内容与创作经验，供团队检索复用。",
    "公推模板库": "管理公众号公推排版模板，支持预览、编辑与导入。",
    "公推模板创建": "公推模板库的新增权限。",
    "公推模板更新": "公推模板库的编辑更新权限。",
    "公推模板删除": "公推模板库的删除权限。",
    "公推模板导入": "公推模板库的批量导入权限。",
    "计划管理": "制定内容运营计划，关联 IP 组或具体账号并跟踪执行。",
    "SOP管理": "自定义标准作业流程，支持并行节点与审核节点配置。",
    "SOP审核": "对提交的 SOP 模板变更进行审核确认。",
    "任务管理": "基于 SOP 创建和分配任务，跟踪各节点完成状态。",
    "自定义查询": "灵活自助分析工具，支持保存模板并由管理员发布。",
    "数据报表": "8 张标准运营报表中心，覆盖账号、产出、成本与预警等场景。",
    "总体财务分析": "公司级财务全景分析，含营收、成本、ROI 与利润视图。",
    "漏斗分析": "预置与自定义转化漏斗分析，洞察各环节转化效率。",
    "指标管理": "定义和配置运营指标口径，支撑报表与绩效计算。",
    "指标分析": "按指标维度深度分析运营数据趋势与对比。",
    "数据大屏": "全屏展示核心 KPI，适合会议室投屏与实时监控。",
    "大屏配置": "配置数据大屏布局、组件与数据源绑定。",
    "采集日志": "查看各平台数据采集执行日志与异常记录。",
    "私域桥接": "配置私域（企微/个微）与平台账号的数据桥接采集。",
    "数据质量": "监控采集成功率、数据完整性与质量异常。",
    "采集任务": "管理各平台定时/手动采集任务的创建与调度。",
    "字典配置": "业务枚举值统一管理，维护全平台字典数据。",
    "登录日志": "记录用户登录行为，支撑安全审计。",
    "操作日志": "记录关键业务操作，支撑合规审计与追溯。",
    "消息管理": "管理系统通知与业务消息的发送与查阅。",
    "系统参数": "全局系统参数配置，控制平台运行行为。",
    "订单归因分析": "分析订单来源归因，关联内容与账号产出效果。",
    "考核执行": "按考核模板自动拉取指标算分，支持人工微调。",
    "绩效结果": "查看历史绩效、个人趋势与等级分布。",
    "考核模板": "按岗位配置考核指标、权重与评分标准。",
    "账号成本管理": "记录账号购买成本与过程成本（租赁/投流/认证等）。",
    "ROI分析": "按公司/IP 组/账号/人员维度分析投资回报率。",
    "公司管理": "管理公司主体，监控公众号注册容量与扩容。",
    "平台账号管理": "统一管理公众号/视频号/抖音/快手/小红书等平台账号。",
    "平台账号查询": "平台账号列表查询权限。",
    "个人账号管理": "管理企微/个微等私域个人账号。",
    "手机管理": "管理运营用手机设备及归属关系。",
    "实名人管理": "管理账号实名人信息，支持中介人佣金关联。",
    "手机卡管理": "管理 SIM 卡及运营商信息，支持跨平台关联查询。",
    "账号分析": "按平台查看账号列表，下钻粉丝与作品详情。",
    "作者管理": "管理内容创作者，关联主推号与运营对主播关系。",
    "人效盘点": "按经办人统计任务完成率、内容产出与 ROI 表现。",
    "粉丝分析": "粉丝增长、画像与 LTV 等多维分析。",
    "内部作品分析": "查看自有内容表现，接口异常时可手工补录数据。",
    "IP组管理": "建立两级 IP 组织架构（大组/小组），是全部业务组织锚点。",
    "AI模型": "配置 AI 辅助创作所用大语言模型连接与参数。",
    "AI提示词": "配置 AI 生成内容的提示词模板与场景。",
    "外部采集配置": "配置竞品外部平台账号与作品的数据采集规则。",
    "外部数据配置": "管理外部数据源连接与接入配置。",
    "内部采集配置": "配置各内部平台（含快手）的数据采集规则。",
    "元数据维护": "维护业务元数据表结构与字段映射关系。",
    "订单采集配置": "配置订单/营收数据的采集来源与同步规则。",
    "阈值规则配置": "配置爆款/低分/高粉/低粉等自动判定阈值。",
}

HIDDEN_GROUP: dict[str, str] = {
    "/plan/:id": "内容生产",
    "/analysis/account/:id/detail": "运营管理",
    "/analysis/report/account-alert": "数据分析",
    "/analysis/report/account-status": "数据分析",
    "/analysis/report/cost-allocation": "数据分析",
    "/analysis/report/live-duration": "数据分析",
    "/analysis/report/roi": "数据分析",
    "/analysis/report/team-config": "数据分析",
    "/analysis/report/unified-account": "数据分析",
    "/analysis/report/video-output": "数据分析",
    "/author/:id/dashboard": "运营管理",
    "/collect/task/:id": "数据采集",
    "/company/:id": "账号管理",
    "/content/edit": "内容生产",
    "/content/edit/:id": "内容生产",
    "/finance/cost/edit": "财务管理",
    "/finance/roi/trend": "财务管理",
    "/industry-data": "作品监测",
    "/internal-content/:id": "运营管理",
    "/layout-template/:id": "内容生产",
    "/layout-template/:id/edit": "内容生产",
    "/layout-template/create": "内容生产",
    "/layout-template/import": "内容生产",
    "/perf/order-attribution/roi": "绩效核算",
    "/perf/record/:id": "绩效核算",
    "/perf/result/:userId/trend": "绩效核算",
    "/perf/template/:id": "绩效核算",
    "/platform-account/:id": "账号管理",
    "/realname/:id": "账号管理",
    "/screen/:id": "数据分析",
    "/simcard/:id/linked": "账号管理",
    "/sop/:id/edit": "内容生产",
    "/task/:id/execute": "内容生产",
    "/triple-rel": "账号管理",
    "/wechat-data": "数据分析",
    "/workbench-todos": "首页",
    "/system-role": "系统管理(OA)",
    "/system-user": "系统管理(OA)",
    "/system-tenant": "系统管理(OA)",
}

HIDDEN_DESC: dict[str, str] = {
    "/plan/:id": "查看内容运营计划详情与关联任务执行情况。",
    "/analysis/account/:id/detail": "下钻查看单个账号的粉丝、作品等详细分析数据。",
    "/analysis/report/account-alert": "账号异常预警汇总报表（认证到期/封禁等）。",
    "/analysis/report/account-status": "各平台账号状态监控与异常预警报表。",
    "/analysis/report/cost-allocation": "按 IP 组/账号类型/成本类型进行成本分摊报表。",
    "/analysis/report/live-duration": "各主播/IP 组直播时长与开播情况统计报表。",
    "/analysis/report/roi": "各 IP 组/账号投资回报率分析报表。",
    "/analysis/report/team-config": "各 IP 组组织架构与人员配置报表。",
    "/analysis/report/unified-account": "整合各平台账号的统一视图报表。",
    "/analysis/report/video-output": "各主播/IP 组短视频产出趋势统计报表。",
    "/author/:id/dashboard": "单个作者的核心指标看板与内容产出概览。",
    "/collect/task/:id": "编辑采集任务配置、调度规则与数据源绑定。",
    "/company/:id": "查看公司主体详情、公众号容量与关联账号。",
    "/content/edit": "新建内容并调用 AI 辅助创作，提交审核流转。",
    "/content/edit/:id": "编辑已有内容草稿，修改后重新提交审核。",
    "/finance/cost/edit": "录入账号购买成本或过程成本明细。",
    "/finance/roi/trend": "查看 ROI 历史趋势与多维度对比分析。",
    "/industry-data": "按行业维度聚合竞品与外部内容数据。",
    "/internal-content/:id": "查看单篇内部作品的传播效果与互动详情。",
    "/layout-template/:id": "预览公推模板排版效果。",
    "/layout-template/:id/edit": "编辑已有公推模板的排版与样式。",
    "/layout-template/create": "新建公推排版模板。",
    "/layout-template/import": "批量导入公推模板文件。",
    "/perf/order-attribution/roi": "订单归因维度的 ROI 深度分析页。",
    "/perf/record/:id": "查看单次考核执行详情与各指标得分。",
    "/perf/result/:userId/trend": "查看指定人员的个人绩效历史趋势。",
    "/perf/template/:id": "编辑考核模板的指标、权重与评分标准。",
    "/platform-account/:id": "查看平台账号详情及关联实名人、IP 组等信息。",
    "/realname/:id": "查看实名人详情、关联账号与中介人信息。",
    "/screen/:id": "按配置 ID 打开指定数据大屏全屏视图。",
    "/simcard/:id/linked": "跨平台查询 SIM 卡关联的手机与账号信息。",
    "/sop/:id/edit": "编辑 SOP 模板节点、并行关系与审核配置。",
    "/task/:id/execute": "执行 SOP 任务各节点，提交内容与审核材料。",
    "/triple-rel": "管理微信-视频号-企微三方关联统计与配置。",
    "/wechat-data": "微信公众号专项数据分析与阅读互动统计。",
    "/workbench-todos": "查看全部待办事项（待审核、逾期任务、账号预警等）。",
    "/system-role": "角色权限管理（M9，由 Football 系统服务承载，OPS 侧隐藏）。",
    "/system-user": "用户管理（M9，由 Football 系统服务承载，OPS 侧隐藏）。",
    "/system-tenant": "租户管理（M9，由 Football 系统服务承载，OPS 侧隐藏）。",
}


def parse_menus() -> dict[int, dict]:
    menus: dict[int, dict] = {}
    for line in SQL.read_text(encoding="utf-8").splitlines():
        if "INSERT INTO system_menu" not in line:
            continue
        m = PAT.search(line)
        if not m:
            continue
        mid, name, perm, typ, parent = m.groups()
        menus[int(mid)] = {
            "id": int(mid),
            "name": name,
            "permission": perm,
            "type": int(typ),
            "parent_id": int(parent),
        }
    return menus


def ancestors(mid: int, menus: dict[int, dict]) -> list[str]:
    chain: list[str] = []
    while mid in menus and menus[mid]["parent_id"] != 0:
        pid = menus[mid]["parent_id"]
        chain.insert(0, menus[pid]["name"])
        mid = pid
    return chain


def menu_code(mid: int, perm: str) -> str:
    return f"{mid} / {perm}" if perm else str(mid)


def level_columns(mid: int, menus: dict[int, dict]) -> tuple[str, str, str]:
    m = menus[mid]

    if m["parent_id"] == 0:
        return m["name"], "-", "-"
    if m["type"] == 1:
        return "运营数据", m["name"], "-"
    if m["type"] == 3:
        parent = menus[m["parent_id"]]
        grp = ancestors(m["parent_id"], menus)
        folder = grp[-1] if grp else "-"
        return "运营数据", folder, f"{parent['name']}·{m['name']}"

    anc = ancestors(mid, menus)
    if mid == 6168:
        return "运营数据", m["name"], "-"
    folder = anc[-1] if anc else "-"
    return "运营数据", folder, m["name"]


def main() -> None:
    menus = parse_menus()
    rows: list[dict] = []

    for mid in sorted(menus):
        m = menus[mid]
        l1, l2, l3 = level_columns(mid, menus)
        code = menu_code(m["id"], m["permission"])
        desc = DESC.get(m["name"], f"{m['name']}功能页面。")
        if m["type"] == 3:
            desc += "（按钮权限，菜单不可见）"
        rows.append(
            {
                "section": "system_menu",
                "sort": mid,
                "code": code,
                "l1": l1,
                "l2": l2,
                "l3": l3,
                "desc": desc,
            }
        )

    seed_titles = {m["name"] for m in menus.values() if m["type"] == 2}
    hidden_idx = 90000

    with CSV.open(encoding="utf-8-sig") as f:
        for r in csv.DictReader(f):
            path = r["route_path"]
            title = r["menu_title"]
            group = r["parent_group"] or HIDDEN_GROUP.get(path, "")
            perm = r["permission"]
            hide = r["hide_in_menu"] == "Y"
            in_layout = r["in_layout"] == "Y"
            excluded = r.get("excluded_m9", "N") == "Y"

            is_seed_visible = (not hide) and in_layout and title in seed_titles
            if is_seed_visible:
                continue

            if group:
                l1, l2, l3 = "运营数据", group, title or "-"
            elif title:
                l1, l2, l3 = "运营数据", title, "-"
            else:
                l1, l2, l3 = "运营数据", "-", path

            code = f"[隐藏] {path}"
            if perm:
                code += f" / {perm}"

            desc = HIDDEN_DESC.get(path, f"{title or path}详情或子页面。")
            if excluded:
                if "（M9" not in desc:
                    desc += "（M9 系统服务，OPS 侧隐藏）"
            else:
                desc += "（隐藏路由，不在侧栏菜单）"

            hidden_idx += 1
            rows.append(
                {
                    "section": "hidden",
                    "sort": hidden_idx,
                    "code": code,
                    "l1": l1,
                    "l2": l2,
                    "l3": l3,
                    "desc": desc,
                }
            )

    lines = [
        "# OPS 完整菜单列表",
        "",
        "> 生成来源：`seed-oa-system-menu.sql`（system_menu 6100–6174）+ `oa-menu-permission-map.csv`（隐藏/详情路由）",
        "> 编号规则：可见菜单 = `菜单ID / 权限码`；隐藏路由 = `[隐藏] 路由路径 / 权限码`",
        "> 角色与数据权限列留空，供业务方自行梳理。",
        "",
        f"**菜单条目总数：{len(rows)}**（system_menu {sum(1 for r in rows if r['section']=='system_menu')} + 隐藏路由 {sum(1 for r in rows if r['section']=='hidden')}）",
        "",
        "| 菜单编号 | 一级菜单 | 二级菜单 | 三级菜单(如有) | 业务描述 | 角色 | 数据权限 |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]

    for r in rows:
        lines.append(
            f"| {r['code']} | {r['l1']} | {r['l2']} | {r['l3']} | {r['desc']} | - | - |"
        )

    lines.extend(
        [
            "",
            "## 编号说明",
            "",
            "| 类型 | 编号格式 | 说明 |",
            "| --- | --- | --- |",
            "| 根目录/分组 | `6100` | type=1 目录节点，无 `oa:*` 权限码 |",
            "| 页面菜单 | `6111 / oa:external-account:list` | type=2 页面，ID 6100–6999 保留给 OPS |",
            "| 按钮权限 | `6170 / oa:layout-template:create` | type=3 按钮，visible=0 不可见 |",
            "| 隐藏路由 | `[隐藏] /content/edit / oa:content:list` | 前端路由存在但未写入 system_menu |",
            "",
            "## 一级分组索引（6101–6110）",
            "",
            "| ID | 分组 | 权限前缀示例 |",
            "| --- | --- | --- |",
            "| 6101 | 作品监测 | `oa:hot-works:*` |",
            "| 6102 | 内容生产 | `oa:content:*` `oa:sop:*` |",
            "| 6103 | 数据分析 | `oa:report:*` `oa:metric:*` |",
            "| 6104 | 数据采集 | `oa:collect:*` |",
            "| 6105 | 系统管理(OA) | `oa:dict:*` `oa:log:*` |",
            "| 6106 | 绩效核算 | `oa:perf:*` |",
            "| 6107 | 财务管理 | `oa:cost:*` `oa:roi:*` |",
            "| 6108 | 账号管理 | `oa:company:*` `oa:platform-account:*` |",
            "| 6109 | 运营管理 | `oa:ip-group:*` `oa:author:*` |",
            "| 6110 | 配置管理 | `oa:config:*` `oa:metadata:*` |",
        ]
    )

    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {OUT} ({len(rows)} rows)")


if __name__ == "__main__":
    main()
