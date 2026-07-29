#!/usr/bin/env python3
"""Export wd schema (DDL only) as UTF-8 without PowerShell pipe corruption.

Why this script exists
----------------------
PowerShell patterns like::

    mysqldump ... wd | Out-File -Encoding utf8 wd-schema.sql
    mysqldump ... wd > wd-schema.sql

re-decode mysqldump's UTF-8 stdout through the console code page (often GBK),
then rewrite UTF-8 (often with BOM). Multi-byte Chinese COMMENT strings become
``出生�?`` / broken quotes — same class of bug as non-utf8mb4 menu import.

Correct approach: ``mysqldump --default-character-set=utf8mb4 --result-file=...``
so the client writes bytes directly to disk (no pipe, no BOM).
"""
from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUT = ROOT / "docs" / "sql" / "wd-schema.sql"


def find_mysqldump() -> str:
    found = shutil.which("mysqldump")
    if found:
        return found
    # Common Windows install path (MySQL 8.4)
    candidates = [
        Path(r"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqldump.exe"),
        Path(r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe"),
    ]
    for c in candidates:
        if c.is_file():
            return str(c)
    raise FileNotFoundError("mysqldump not found in PATH or Program Files")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", default="3306")
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="root")
    parser.add_argument("--database", default="wd")
    parser.add_argument(
        "--out",
        type=Path,
        default=DEFAULT_OUT,
        help="Output .sql path (written by mysqldump -r, UTF-8, no BOM)",
    )
    args = parser.parse_args()

    out: Path = args.out.resolve()
    out.parent.mkdir(parents=True, exist_ok=True)

    mysqldump = find_mysqldump()
    cmd = [
        mysqldump,
        f"-h{args.host}",
        f"-P{args.port}",
        f"-u{args.user}",
        f"-p{args.password}",
        "--default-character-set=utf8mb4",
        "--no-data",
        "--routines",
        "--triggers",
        "--single-transaction",
        "--set-gtid-purged=OFF",
        f"--result-file={out}",
        args.database,
    ]
    print(
        f"Exporting {args.host}/{args.database} -> {out} "
        f"(utf8mb4, --result-file, no PowerShell pipe)"
    )
    proc = subprocess.run(cmd, capture_output=True)
    # mysqldump may print password warning on stderr even on success
    stderr = proc.stderr.decode("utf-8", errors="replace").strip()
    if stderr:
        print(stderr, file=sys.stderr)
    if proc.returncode != 0:
        return proc.returncode

    raw = out.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        out.write_bytes(raw[3:])
        raw = out.read_bytes()
        print("Stripped accidental UTF-8 BOM", file=sys.stderr)

    text = raw.decode("utf-8")  # must be strict
    # Smoke: demo table comments must stay Chinese, not end with '?'
    checks = [
        ("COMMENT '创建者'", "创建者"),
        ("COMMENT '简介'", "简介"),
        ("COMMENT='示例联系人表'", "示例联系人表"),
    ]
    missing = [label for needle, label in checks if needle not in text]
    if missing:
        print(
            "WARN: expected Chinese comments not found after export: "
            + ", ".join(missing),
            file=sys.stderr,
        )
        print(
            "DB may already have corrupted TABLE/COLUMN comments; "
            "export itself used utf8mb4 --result-file.",
            file=sys.stderr,
        )
    else:
        print("OK: sample Chinese COMMENT strings present")

    q_broken = text.count("?,")
    print(f"size={len(raw)} bytes  broken_?,_count={q_broken}  date={date.today()}")
    print("Done. Do NOT re-save via PowerShell Out-File / Set-Content.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
