import re
from pathlib import Path
src = Path(r"d:\self\sy\运营数据平台\202606\wd\football-backend-saas\sql\mysql\ruoyi-vue-pro.sql")
text = src.read_text(encoding="utf-8")
parts = re.split(r"(?=-- Table structure for system_)", text)
chunks = []
for p in parts:
    m = re.match(r"-- Table structure for (system_[^\r\n]+)", p)
    if not m:
        continue
    name = m.group(1).strip()
    if not name.startswith("system_"):
        continue
    chunks.append(p.strip())
out = Path(r"d:\self\sy\运营数据平台\202606\wd\scripts\integration-config\import-football-system-tables.sql")
header = """-- Football (ruoyi-vue-pro) system_* subset for wd integration
-- Source: football-backend-saas/sql/mysql/ruoyi-vue-pro.sql
-- Does NOT touch sys_* / oa_* tables. Idempotent: DROP IF EXISTS per table then recreate+seed.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

"""
footer = "\nSET FOREIGN_KEY_CHECKS = 1;\n"
out.write_text(header + "\n\n".join(chunks) + footer, encoding="utf-8")
print("tables", len(chunks))
print("bytes", out.stat().st_size)
