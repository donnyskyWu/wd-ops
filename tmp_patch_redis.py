from pathlib import Path
import re
p = Path(r"d:/self/sy/运营数据平台/202606/wd/scripts/integration-config/football-integration-overlay.yml")
text = p.read_text(encoding="utf-8")
block = """    redis:
      host: 127.0.0.1
      port: 6379
      database: 0
      password: \"\"
      # password: 123456 # local Redis has no password
"""
text2, n = re.subn(
    r"    redis:\n      host: 127\.0\.0\.1\n      port: 6379\n      database: 0\n(?:      .*\n)*",
    block,
    text,
    count=1,
)
if n != 1:
    raise SystemExit(f"redis block not patched, n={n}")
p.write_text(text2, encoding="utf-8")
print("ok")
