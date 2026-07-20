#!/usr/bin/env python3
"""Data permission E2E probe (OpsDataScope + M4AccountIpGroupScope acceptance)."""
from __future__ import annotations

import json
import sys
import time
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:48094"
TENANT = "1"
ADMIN = "dev-token-oa-admin"
OPERATOR = "dev-token-oa-operator"


def call(method: str, path: str, token: str, body: dict | None = None) -> tuple[int, dict | str]:
    headers = {
        "Authorization": f"Bearer {token}",
        "X-Tenant-Id": TENANT,
        "Content-Type": "application/json",
    }
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(f"{BASE}{path}", data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode()
            try:
                return resp.status, json.loads(raw)
            except json.JSONDecodeError:
                return resp.status, raw
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        try:
            return exc.code, json.loads(raw)
        except json.JSONDecodeError:
            return exc.code, raw


def ip_groups_in_list(body: dict) -> set[int]:
    items = (body.get("data") or {}).get("list") or []
    return {x.get("ipGroupId") for x in items if x.get("ipGroupId") is not None}


def tree_child_ids(body: dict) -> set[int]:
    data = body.get("data") or []
    ids: set[int] = set()
    for root in data:
        for child in root.get("children") or []:
            if child.get("id") is not None:
                ids.add(int(child["id"]))
    return ids


def main() -> int:
    checks: list[dict] = []

    def record(name: str, passed: bool, detail: str, extra: dict | None = None) -> None:
        row = {"name": name, "pass": passed, "detail": detail}
        if extra:
            row.update(extra)
        checks.append(row)
        mark = "PASS" if passed else "FAIL"
        print(f"[{mark}] {name}: {detail}")

    # 1. Admin sees all DOUYIN accounts (multi IP groups)
    _, admin_list = call("GET", "/admin-api/oa/account/list?platformType=DOUYIN", ADMIN)
    admin_groups = ip_groups_in_list(admin_list) if isinstance(admin_list, dict) else set()
    admin_total = (admin_list.get("data") or {}).get("total") if isinstance(admin_list, dict) else None
    record(
        "admin_list_multi_groups",
        isinstance(admin_list, dict)
        and admin_list.get("code") == 0
        and (admin_total or 0) >= 5
        and len(admin_groups) >= 2,
        f"code={admin_list.get('code') if isinstance(admin_list, dict) else admin_list}, total={admin_total}, groups={sorted(admin_groups)}",
    )

    # 2. Operator only sees IP group 9001 accounts
    _, op_list = call("GET", "/admin-api/oa/account/list?platformType=DOUYIN", OPERATOR)
    op_groups = ip_groups_in_list(op_list) if isinstance(op_list, dict) else set()
    record(
        "operator_list_group_9001_only",
        isinstance(op_list, dict)
        and op_list.get("code") == 0
        and 9001 in op_groups
        and 9002 not in op_groups,
        f"code={op_list.get('code') if isinstance(op_list, dict) else op_list}, groups={sorted(op_groups)}",
    )

    # 3. Cross-group get -> 403
    _, op_get_other = call("GET", "/admin-api/oa/account/get?id=9006", OPERATOR)
    record(
        "operator_get_other_group_403",
        isinstance(op_get_other, dict) and op_get_other.get("code") == 403,
        f"code={op_get_other.get('code') if isinstance(op_get_other, dict) else op_get_other}",
    )

    # 4. Operator can read own group account
    _, op_get_own = call("GET", "/admin-api/oa/account/get?id=9001", OPERATOR)
    own_group = (op_get_own.get("data") or {}).get("ipGroupId") if isinstance(op_get_own, dict) else None
    record(
        "operator_get_own_group",
        isinstance(op_get_own, dict) and op_get_own.get("code") == 0 and own_group == 9001,
        f"code={op_get_own.get('code') if isinstance(op_get_own, dict) else op_get_own}, ipGroupId={own_group}",
    )

    # 5. Create without ipGroupId -> 1400
    _, create_no_group = call(
        "POST",
        "/admin-api/oa/account/create",
        ADMIN,
        {
            "platformType": "DOUYIN",
            "accountName": "缺IP组",
            "externalAccountId": f"dy_no_ip_{int(time.time())}",
            "companyId": 9001,
            "realnameId": 9001,
            "status": "NORMAL",
        },
    )
    record(
        "create_without_ip_group_1400",
        isinstance(create_no_group, dict) and create_no_group.get("code") == 1400,
        f"code={create_no_group.get('code') if isinstance(create_no_group, dict) else create_no_group}",
    )

    # 6. Operator bind own group 9001 -> 0 (forceReplace: realname 9003 may be bound by prior probe runs)
    _, create_ok = call(
        "POST",
        "/admin-api/oa/account/create",
        OPERATOR,
        {
            "platformType": "DOUYIN",
            "accountName": "专员绑定9001",
            "externalAccountId": f"dy_op_9001_{int(time.time())}",
            "companyId": 9001,
            "realnameId": 9003,
            "ipGroupId": 9001,
            "status": "NORMAL",
            "forceReplace": True,
            "reason": "E2E data scope probe rebinding realname",
        },
    )
    record(
        "operator_bind_group_9001",
        isinstance(create_ok, dict) and create_ok.get("code") == 0,
        f"code={create_ok.get('code') if isinstance(create_ok, dict) else create_ok}",
    )

    # 7. Operator bind other group 9002 -> 1504
    _, create_denied = call(
        "POST",
        "/admin-api/oa/account/create",
        OPERATOR,
        {
            "platformType": "DOUYIN",
            "accountName": "专员越权9002",
            "externalAccountId": f"dy_op_9002_{int(time.time())}",
            "companyId": 9001,
            "realnameId": 9001,
            "ipGroupId": 9002,
            "status": "NORMAL",
        },
    )
    record(
        "operator_bind_group_9002_denied",
        isinstance(create_denied, dict) and create_denied.get("code") == 1504,
        f"code={create_denied.get('code') if isinstance(create_denied, dict) else create_denied}",
    )

    # 8. Accessible-tree filtered
    _, admin_tree = call("GET", "/admin-api/oa/ip-group/accessible-tree", ADMIN)
    _, op_tree = call("GET", "/admin-api/oa/ip-group/accessible-tree", OPERATOR)
    admin_ids = tree_child_ids(admin_tree) if isinstance(admin_tree, dict) else set()
    op_ids = tree_child_ids(op_tree) if isinstance(op_tree, dict) else set()
    record(
        "accessible_tree_admin_broad",
        isinstance(admin_tree, dict) and admin_tree.get("code") == 0 and len(admin_ids) >= 2,
        f"children={sorted(admin_ids)}",
    )
    record(
        "accessible_tree_operator_scoped",
        isinstance(op_tree, dict)
        and op_tree.get("code") == 0
        and 9001 in op_ids
        and 9002 not in op_ids,
        f"children={sorted(op_ids)}",
    )

    # 9. Operator funnel/metric list scoped to self (Phase 2)
    _, op_funnel = call("GET", "/admin-api/oa/funnel/list?pageNum=1&pageSize=50", OPERATOR)
    funnel_items = (op_funnel.get("data") or {}).get("list") or [] if isinstance(op_funnel, dict) else []
    record(
        "operator_funnel_list_self_only",
        isinstance(op_funnel, dict)
        and op_funnel.get("code") == 0
        and not any(x.get("id") == 9801 for x in funnel_items),
        f"code={op_funnel.get('code') if isinstance(op_funnel, dict) else op_funnel}, count={len(funnel_items)}",
    )

    _, op_task = call("GET", "/admin-api/oa/task/list?pageNum=1&pageSize=50", OPERATOR)
    _, op_my = call("GET", "/admin-api/oa/task/my-tasks?pageNum=1&pageSize=50", OPERATOR)
    op_task_total = (op_task.get("data") or {}).get("total") if isinstance(op_task, dict) else None
    op_my_total = (op_my.get("data") or {}).get("total") if isinstance(op_my, dict) else None
    record(
        "operator_task_list_matches_my_tasks",
        isinstance(op_task, dict)
        and isinstance(op_my, dict)
        and op_task.get("code") == 0
        and op_my.get("code") == 0
        and op_task_total == op_my_total,
        f"list_total={op_task_total}, my_tasks_total={op_my_total}",
    )

    passed = sum(1 for c in checks if c["pass"])
    print(f"\n=== {passed}/{len(checks)} passed ===")
    return 0 if passed == len(checks) else 1


if __name__ == "__main__":
    sys.exit(main())
