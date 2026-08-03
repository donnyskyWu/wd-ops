# -*- coding: utf-8 -*-
"""Apply V168 SQL to Beta shenyu-ops (idempotent)."""
from pathlib import Path
import re
import sys

try:
    import pymysql
except ImportError:
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pymysql", "-q"])
    import pymysql

ROOT = Path(__file__).resolve().parents[4]
env_path = ROOT / "scripts" / "integration-config" / "ops-test-remote.env"
vals = {}
for line in env_path.read_text(encoding="utf-8").splitlines():
    line = line.strip()
    if not line or line.startswith("#") or "=" not in line:
        continue
    k, v = line.split("=", 1)
    vals[k] = v

sql_path = (
    ROOT
    / "football-backend-saas"
    / "football-module-ops"
    / "football-module-ops-server"
    / "src"
    / "main"
    / "resources"
    / "db"
    / "migration"
    / "V168__ai_content_chat_prompt_by_document_type.sql"
)
sql = sql_path.read_text(encoding="utf-8")

conn = pymysql.connect(
    host=vals["OPS_TEST_DB_HOST"],
    port=int(vals.get("OPS_TEST_DB_PORT", "3306")),
    user=vals["OPS_TEST_MASTER_USER"],
    password=vals["OPS_TEST_MASTER_PASSWORD"],
    database=vals.get("OPS_TEST_MASTER_DB", "shenyu-ops"),
    charset="utf8mb4",
    autocommit=True,
)
# Split on semicolon at end of statements; keep SET/@ vars
statements = []
buf = []
for line in sql.splitlines():
    if line.strip().startswith("--"):
        continue
    buf.append(line)
    if line.rstrip().endswith(";"):
        stmt = "\n".join(buf).strip()
        if stmt:
            statements.append(stmt.rstrip(";"))
        buf = []
if buf and "".join(buf).strip():
    statements.append("\n".join(buf).strip().rstrip(";"))

cur = conn.cursor()
ok = 0
for stmt in statements:
    cur.execute(stmt)
    ok += 1

cur.execute(
    """
    SELECT id, document_type, template_name, LEFT(prompt_content, 40), CHAR_LENGTH(prompt_content)
    FROM oa_ai_prompt_config
    WHERE tenant_id=1 AND scene='AI_CONTENT_CHAT' AND deleted=0
    ORDER BY document_type IS NULL, document_type, id
    """
)
rows = cur.fetchall()
out = Path(__file__).parent / "db-seed-result.txt"
lines = [f"executed_statements={ok}", "id\tdocument_type\ttemplate_name\tprefix\tchars"]
for r in rows:
    lines.append(f"{r[0]}\t{r[1]}\t{r[2]}\t{r[3]}\t{r[4]}")
out.write_text("\n".join(lines) + "\n", encoding="utf-8")

# Ensure flyway history has V168 so restart won't double-fail on checksum if already applied
cur.execute("SHOW TABLES LIKE 'flyway_schema_history'")
if cur.fetchone():
    cur.execute("SELECT version, checksum, success FROM flyway_schema_history WHERE version='168'")
    existing = cur.fetchone()
    if not existing:
        cur.execute("SELECT COALESCE(MAX(installed_rank),0)+1 FROM flyway_schema_history")
        rank = cur.fetchone()[0]
        # Leave checksum null-ish: better let Flyway compute on first run.
        # Insert success=1 with description so Flyway skips if we already applied manually.
        # Actually Flyway validates checksum - safer NOT to insert and let Flyway run idempotent SQL.
        out.write_text(out.read_text(encoding="utf-8") + f"flyway_168_existing=False rank_next={rank} (not inserted; Flyway will run idempotent V168)\n", encoding="utf-8")
    else:
        out.write_text(out.read_text(encoding="utf-8") + f"flyway_168_existing={existing}\n", encoding="utf-8")

cur.close()
conn.close()
print("OK wrote", out)