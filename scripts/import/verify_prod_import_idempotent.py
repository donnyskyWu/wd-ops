#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Beta/prod-safe idempotency check for prod-sync-20260928 SQL (NOT production by default).

Runs on DB from scripts/integration-config/ops-test-remote.env unless overridden.

Steps:
  1. 01-shenyu-ops-schema-V205-V206.sql × 2 (expect exit 0)
  2. Optional: seed fake company id 99999 (preclean policy test)
  3. 03 preclean + 04 data × N (expect exit 0, no 1062)
  4. Print DOUYIN/KUAISHOU counts; verify fake company survives preclean+import

Usage:
  python scripts/import/verify_prod_import_idempotent.py
  python scripts/import/verify_prod_import_idempotent.py --skip-schema
  python scripts/import/verify_prod_import_idempotent.py --with-preclean --data-runs 2
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEPLOY = ROOT / "docs" / "deploy" / "prod-sync-20260928"
DEFAULT_ENV = ROOT / "scripts" / "integration-config" / "ops-test-remote.env"

SCHEMA_SQL = DEPLOY / "01-shenyu-ops-schema-V205-V206.sql"
PRECLEAN_SQL = DEPLOY / "03-shenyu-ops-preclean-douyin-kuaishou-ONLY.sql"
DATA_SQL = DEPLOY / "04-shenyu-ops-data-import-0928-OPTIONAL.sql"
FAKE_COMPANY_ID = 99999

MYSQL = "mysql"


def load_env(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    if not path.is_file():
        return out
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        k, v = line.split("=", 1)
        out[k.strip()] = v.strip()
    return out


def mysql_base_cmd(host: str, port: int, user: str, password: str, database: str) -> list[str]:
    return [
        MYSQL,
        f"-h{host}",
        f"-P{port}",
        f"-u{user}",
        f"-p{password}",
        "--default-character-set=utf8mb4",
        database,
    ]


def run_sql_file(
    cmd_base: list[str],
    sql_path: Path,
    label: str,
) -> tuple[int, str, str]:
    text = sql_path.read_text(encoding="utf-8")
    print(f"\n=== {label} ===")
    print(f"    file: {sql_path.name} ({sql_path.stat().st_size} bytes)")
    proc = subprocess.run(
        cmd_base,
        input=text.encode("utf-8"),
        capture_output=True,
    )
    stderr = proc.stderr.decode("utf-8", errors="replace")
    stdout = proc.stdout.decode("utf-8", errors="replace")
    status = "OK" if proc.returncode == 0 else "FAIL"
    print(f"    exit_code: {proc.returncode} ({status})")
    if stderr.strip():
        # mysql often prints notices to stderr
        for line in stderr.strip().splitlines()[:20]:
            print(f"    stderr: {line}")
        if stderr.count("\n") > 20:
            print("    stderr: ... (truncated)")
    if proc.returncode != 0 and stdout.strip():
        print(f"    stdout: {stdout.strip()[:500]}")
    if re.search(r"1062|Duplicate entry", stderr + stdout, re.I):
        print("    *** DETECTED 1062 / Duplicate entry ***")
    return proc.returncode, stdout, stderr


def run_query(cmd_base: list[str], sql: str) -> tuple[int, str]:
    proc = subprocess.run(
        cmd_base,
        input=(sql + "\n").encode("utf-8"),
        capture_output=True,
    )
    out = proc.stdout.decode("utf-8", errors="replace")
    err = proc.stderr.decode("utf-8", errors="replace")
    if proc.returncode != 0:
        print(f"Query failed ({proc.returncode}): {err or out}")
    return proc.returncode, out.strip()


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify prod-sync SQL idempotency on test DB")
    parser.add_argument("--env-file", type=Path, default=DEFAULT_ENV)
    parser.add_argument("--host", default=None)
    parser.add_argument("--port", type=int, default=None)
    parser.add_argument("--user", default=None)
    parser.add_argument("--password", default=None)
    parser.add_argument("--database", default=None)
    parser.add_argument(
        "--skip-schema",
        action="store_true",
        help="Skip 01 schema runs (only test 04 double-run)",
    )
    parser.add_argument(
        "--data-runs",
        type=int,
        default=3,
        help="How many times to run 04 data SQL (default 3)",
    )
    parser.add_argument(
        "--with-preclean",
        action="store_true",
        help="Run 03 before each 04 (production re-import抖快 flow)",
    )
    parser.add_argument(
        "--seed-fake-company",
        action="store_true",
        help=f"Insert oa_company id={FAKE_COMPANY_ID} before import; assert still present after",
    )
    args = parser.parse_args()

    env = load_env(args.env_file)
    host = args.host or env.get("OPS_TEST_DB_HOST") or env.get("OPS_WD_TEST_HOST", "127.0.0.1")
    port = args.port or int(env.get("OPS_TEST_DB_PORT") or env.get("OPS_WD_TEST_PORT", "3306"))
    user = args.user or env.get("OPS_TEST_MASTER_USER") or env.get("OPS_WD_TEST_USERNAME", "root")
    password = args.password or env.get("OPS_TEST_MASTER_PASSWORD") or env.get(
        "OPS_WD_TEST_PASSWORD", "root"
    )
    database = args.database or env.get("OPS_TEST_MASTER_DB") or env.get(
        "OPS_WD_TEST_DATABASE", "shenyu-ops"
    )

    print("Target (explicit test env only — do NOT point at prod without intent):")
    print(f"  {host}:{port}/{database} as {user}")

    paths = [SCHEMA_SQL, DATA_SQL]
    if args.with_preclean:
        paths.append(PRECLEAN_SQL)
    for p in paths:
        if not p.is_file():
            print(f"Missing SQL: {p}", file=sys.stderr)
            return 1

    cmd = mysql_base_cmd(host, port, user, password, database)

    # connectivity
    rc, ping = run_query(cmd, "SELECT 1 AS ok;")
    if rc != 0 or "1" not in ping:
        print("Cannot connect to MySQL.", file=sys.stderr)
        return 1
    print(f"Connected: {ping}")

    results: list[tuple[str, int]] = []

    if not args.skip_schema:
        for i in (1, 2):
            rc, _, combined = run_sql_file(cmd, SCHEMA_SQL, f"01 schema run #{i}")
            results.append((f"01-schema-run-{i}", rc))
            if rc != 0:
                return 1
            if re.search(r"1062|Duplicate entry", combined, re.I):
                print("Schema run reported duplicate key (unexpected).")
                return 1

    if args.seed_fake_company:
        seed_sql = f"""
INSERT INTO oa_company (id, tenant_id, company_name, credit_code, status, creator, updater)
VALUES ({FAKE_COMPANY_ID}, 1, 'PRECLEAN-POLICY-TEST', '91IMPORTFAKE999990', 'ENABLED', 'verify-preclean-policy', 'verify-preclean-policy')
ON DUPLICATE KEY UPDATE company_name='PRECLEAN-POLICY-TEST', updater='verify-preclean-policy';
"""
        rc, _ = run_query(cmd, seed_sql)
        if rc != 0:
            return 1
        print(f"\nSeeded fake company id={FAKE_COMPANY_ID} for preclean policy check.")

    for i in range(1, args.data_runs + 1):
        if args.with_preclean:
            rc, _, combined = run_sql_file(cmd, PRECLEAN_SQL, f"03 preclean run #{i}")
            results.append((f"03-preclean-run-{i}", rc))
            if rc != 0:
                return 1
            if re.search(r"1062|Duplicate entry", combined, re.I):
                print(f"03 run #{i} hit duplicate key (unexpected).")
                return 1
        rc, _, combined = run_sql_file(cmd, DATA_SQL, f"04 data import run #{i}")
        results.append((f"04-data-run-{i}", rc))
        if rc != 0:
            return 1
        if re.search(r"1062|Duplicate entry", combined, re.I):
            print(f"04 run #{i} hit duplicate key — idempotency FAILED.")
            return 1

    print("\n=== Post-run counts (tenant_id=1) ===")
    count_sql = """
SELECT platform_type, COUNT(*) AS cnt
FROM oa_account
WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU')
GROUP BY platform_type
ORDER BY platform_type;
"""
    rc, counts = run_query(cmd, count_sql)
    if rc == 0:
        print(counts or "(no rows)")

    sample_sql = """
SELECT id, platform_type, external_account_id, holder_user_id, updater
FROM oa_account
WHERE tenant_id = 1 AND platform_type IN ('DOUYIN', 'KUAISHOU')
ORDER BY id
LIMIT 5;
"""
    rc, sample = run_query(cmd, sample_sql)
    if rc == 0:
        print("\nSample accounts (first 5 by id):")
        print(sample or "(empty)")

    import_tag_sql = """
SELECT COUNT(*) AS import_tag_accounts
FROM oa_account
WHERE tenant_id = 1 AND updater = 'xlsx-import-20260928';
"""
    rc, tag_cnt = run_query(cmd, import_tag_sql)
    if rc == 0:
        print(f"\nAccounts with updater xlsx-import-20260928: {tag_cnt}")

    print("\n=== Summary ===")
    all_ok = True
    for name, code in results:
        mark = "PASS" if code == 0 else "FAIL"
        print(f"  {name}: exit {code} ({mark})")
        if code != 0:
            all_ok = False

    if args.seed_fake_company:
        rc, fake_row = run_query(
            cmd,
            f"SELECT id, company_name FROM oa_company WHERE tenant_id=1 AND id={FAKE_COMPANY_ID};",
        )
        if rc != 0 or str(FAKE_COMPANY_ID) not in fake_row:
            print(f"\nFAIL: fake company id={FAKE_COMPANY_ID} missing after preclean/import.")
            return 1
        print(f"\nOK: fake company id={FAKE_COMPANY_ID} still present ({fake_row.strip()}).")

    if all_ok:
        print("\nAll runs passed (no 1062 detected).")
        return 0
    print("\nSome runs failed.")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
