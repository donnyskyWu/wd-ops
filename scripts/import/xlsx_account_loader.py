#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Parse Douyin/Kuaishou account rows from 0928-style Excel workbooks.

用户 SSOT（2026-09-28 客户确认，覆盖旧 ADR-078 导入映射）：

| 来源 | 系统字段 | 说明 |
|------|----------|------|
| Excel「持有人」/ 快手 L「持有人」、R「持有」 | `holder_user_id`（ParsedAccount.holder_name） | 与表单「持有人」同一语义；user_map.json 姓名 → Football id |
| Excel「运营人」 | `operator_user_id`（ParsedAccount.operator_name） | V206；同上 user_map |
| 合规实名人 | `realname_id` | 导入仍用 Excel 持有人姓名建/链 oa_realname（DOUYIN 需 company+realname 校验） |

禁止：仅写 realname_id 而不解析 holderUserId（当 Excel 有持有人姓名时）。
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any

import pandas as pd

DOUYIN_SHEET_ALIASES = ("20W抖音", "抖音")
KUAISHOU_SHEET = "快手"
SIM_SHEET = "手机卡"

# 用户确认不导入（0928 收窄范围）
IMPORT_NO_COLUMNS = (
    "接蓝改号手机号",
    "卖家微信名称",
    "截图",
    "运营人员费用",
    "运营人费用",
    "登录手机账号",
    "登录手机号",
    "分配小组",
    "分配",
    "备注",
    "购买日期",
    "购买价格",
    "价格",
    "价格.1",
    "收号日期",
)


@dataclass
class ParsedAccount:
    platform: str  # DOUYIN | KUAISHOU
    external_account_id: str
    account_name: str
    holder_name: str | None  # Excel「持有人」/快手「持有」→ holder_user_id + oa_realname 源姓名
    operator_name: str | None  # Excel「运营人」→ operator_user_id（V206，user_map 解析）
    device_no: str | None  # 手机 / 手机编号 / 右侧「手机号」列 -> phone_id
    sim_card_phone: str | None  # 抖音「手机卡」列 11 位 -> sim_card_id（非登录手机账号）
    company_name: str | None
    password_plain: str | None
    short_video_status: str | None
    live_status: str | None
    source_sheet: str
    source_row: int
    block: str  # L | R | single


def clean_text(value) -> str | None:
    if pd.isna(value):
        return None
    s = str(value).strip()
    if not s or s in {"/", "—", "-"}:
        return None
    return s


def clean_phone(value) -> str | None:
    if pd.isna(value):
        return None
    s = str(value).strip()
    if s.endswith(".0"):
        s = s[:-2]
    if "/" in s:
        s = s.split("/")[0].strip()
    digits = re.sub(r"\D", "", s)
    return digits if len(digits) == 11 else None


def clean_device_no(value) -> str | None:
    if pd.isna(value):
        return None
    s = str(value).strip()
    if s.endswith(".0"):
        s = s[:-2]
    if not s or s == "/":
        return None
    return s


def normalize_person_name(name: str | None) -> str | None:
    if not name:
        return None
    s = re.sub(r"[（(].*?[）)]", "", name).strip()
    return s or None


def ks_flag_to_status(flag: str | None, label_when_yes: str) -> str | None:
    t = clean_text(flag)
    if not t:
        return None
    if t in {"是", "Y", "YES", "yes", "1", "1.0"}:
        return label_when_yes
    return t


def load_json_map(path: Path | None) -> dict[str, int]:
    if not path or not path.is_file():
        return {}
    data = json.loads(path.read_text(encoding="utf-8"))
    return {str(k): int(v) for k, v in data.items() if not str(k).startswith("_")}


def resolve_user_id(name: str | None, mapping: dict[str, int]) -> int | None:
    norm = normalize_person_name(name)
    if not norm:
        return None
    if norm in mapping:
        return mapping[norm]
    for key, uid in mapping.items():
        if key in norm or norm in key:
            return uid
    return None


def _pick_sheet(xlsx: Path, aliases: tuple[str, ...]) -> str:
    xl = pd.ExcelFile(xlsx)
    for a in aliases:
        if a in xl.sheet_names:
            return a
    raise ValueError(f"Sheet not found; tried {aliases}, got {xl.sheet_names}")


def _douyin_sim_card_column(df: pd.DataFrame) -> str | None:
    for col in df.columns:
        if str(col).replace("\n", "").strip() == "手机卡":
            return col
    return None


def load_douyin_df(xlsx: Path) -> tuple[str, pd.DataFrame]:
    sheet = _pick_sheet(xlsx, DOUYIN_SHEET_ALIASES)
    df = pd.read_excel(xlsx, sheet_name=sheet, header=0)
    return sheet, df


def load_kuaishou_df(xlsx: Path) -> pd.DataFrame:
    return pd.read_excel(xlsx, sheet_name=KUAISHOU_SHEET, header=1)


def load_sim_df(xlsx: Path) -> pd.DataFrame | None:
    xl = pd.ExcelFile(xlsx)
    if SIM_SHEET not in xl.sheet_names:
        return None
    return pd.read_excel(xlsx, sheet_name=SIM_SHEET)


def parse_douyin_rows(df: pd.DataFrame, sheet_name: str) -> list[ParsedAccount]:
    sim_col = _douyin_sim_card_column(df)
    out: list[ParsedAccount] = []
    seen: set[str] = set()
    for idx, row in df.iterrows():
        ext_id = clean_text(row.get("抖音ID"))
        if not ext_id or ext_id in seen:
            continue
        seen.add(ext_id)
        pwd = clean_text(row.get("抖音密码"))
        sim_phone = clean_phone(row.get(sim_col)) if sim_col else None
        out.append(
            ParsedAccount(
                platform="DOUYIN",
                external_account_id=ext_id,
                account_name=clean_text(row.get("抖音名称")) or ext_id,
                holder_name=clean_text(row.get("持有人")),
                operator_name=clean_text(row.get("运营人")),
                device_no=clean_device_no(row.get("手机")),
                sim_card_phone=sim_phone,
                company_name=clean_text(row.get("账号认证主体")),
                password_plain=pwd,
                short_video_status=clean_text(row.get("短视频状态")),
                live_status=clean_text(row.get("直播状态")),
                source_sheet=sheet_name,
                source_row=int(idx) + 2,
                block="single",
            )
        )
    return out


def parse_kuaishou_rows(df: pd.DataFrame) -> list[ParsedAccount]:
    out: list[ParsedAccount] = []
    seen: set[str] = set()

    def add(
        *,
        ext_id: str,
        account_name: str | None,
        holder: str | None,
        device: str | None,
        pwd: str | None,
        sv: str | None,
        lv: str | None,
        row_idx: int,
        block: str,
    ) -> None:
        if not ext_id or ext_id in seen:
            return
        seen.add(ext_id)
        out.append(
            ParsedAccount(
                platform="KUAISHOU",
                external_account_id=ext_id,
                account_name=account_name or ext_id,
                holder_name=holder,
                operator_name=None,
                device_no=device,
                sim_card_phone=None,
                company_name=None,
                password_plain=pwd,
                short_video_status=sv,
                live_status=lv,
                source_sheet=KUAISHOU_SHEET,
                source_row=row_idx + 3,
                block=block,
            )
        )

    for idx, row in df.iterrows():
        left_id = clean_text(row.get("快手ID"))
        right_id = clean_text(row.get("ID"))
        left_sv = ks_flag_to_status(row.get("仅发短视频"), "仅发短视频")
        left_lv = ks_flag_to_status(row.get("短+直播"), "短+直播")
        if left_id:
            add(
                ext_id=left_id,
                account_name=clean_text(row.get("快手账号昵称")),
                holder=clean_text(row.get("持有人")),
                device=clean_device_no(row.get("手机编号")),
                pwd=clean_text(row.get("密码")),
                sv=left_sv,
                lv=left_lv,
                row_idx=int(idx),
                block="L",
            )
        if right_id:
            add(
                ext_id=right_id,
                account_name=clean_text(row.get("持有")),
                holder=clean_text(row.get("持有")),
                device=clean_device_no(row.get("手机号")),
                pwd=clean_text(row.get("密码.1")),
                sv=None,
                lv=None,
                row_idx=int(idx),
                block="R",
            )
    return out


def parse_workbook(xlsx: Path) -> dict[str, Any]:
    dy_sheet, dy_df = load_douyin_df(xlsx)
    ks_df = load_kuaishou_df(xlsx)
    sim_df = load_sim_df(xlsx)
    douyin = parse_douyin_rows(dy_df, dy_sheet)
    kuaishou = parse_kuaishou_rows(ks_df)
    return {
        "douyin_sheet": dy_sheet,
        "douyin_rows_raw": len(dy_df),
        "douyin_accounts": douyin,
        "kuaishou_rows_raw": len(ks_df),
        "kuaishou_accounts": kuaishou,
        "has_sim_sheet": sim_df is not None,
        "sim_rows": len(sim_df) if sim_df is not None else 0,
    }


def sample_mapping(parsed: dict[str, Any]) -> dict[str, Any]:
    dy = parsed["douyin_accounts"]
    ks = parsed["kuaishou_accounts"]
    return {
        "douyin_example": asdict(dy[0]) if dy else None,
        "kuaishou_example": asdict(ks[0]) if ks else None,
    }


def self_test(xlsx: Path) -> None:
    parsed = parse_workbook(xlsx)
    dy_n = len(parsed["douyin_accounts"])
    ks_n = len(parsed["kuaishou_accounts"])
    assert dy_n >= 85, f"expected ~91 douyin accounts, got {dy_n}"
    assert ks_n >= 25, f"expected ~27 kuaishou accounts, got {ks_n}"
    assert parsed["douyin_sheet"] == "20W抖音"
    ex = sample_mapping(parsed)
    assert ex["douyin_example"]["external_account_id"] == "beitang999"
    assert "ip_group_label" not in ex["douyin_example"]
    assert "login_phone" not in ex["douyin_example"]
    assert ex["kuaishou_example"]["block"] == "L"
    print("self_test OK", {"douyin": dy_n, "kuaishou": ks_n, "samples": ex})


if __name__ == "__main__":
    import sys

    path = Path(sys.argv[1] if len(sys.argv) > 1 else r"d:\self\sy\文档\抖音号_快手号分配(0928).xlsx")
    self_test(path)
