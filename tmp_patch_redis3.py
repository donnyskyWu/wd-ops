from pathlib import Path
import re
p = Path(r"d:/self/sy/运营数据平台/202606/wd/scripts/integration-config/football-integration-overlay.yml")
text = p.read_text(encoding="utf-8")
text2 = text.replace(
    "      # password: 123456 # local Redis has no password",
    "      password: 123456 # align with member-server jar + local Redis requirepass",
)
p.write_text(text2, encoding="utf-8")
print('ok')
