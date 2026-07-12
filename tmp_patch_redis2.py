from pathlib import Path
import re
p = Path(r"d:/self/sy/运营数据平台/202606/wd/scripts/integration-config/football-integration-overlay.yml")
text = p.read_text(encoding="utf-8")
text2 = re.sub(r"\n      password: \"\"\n", "\n", text)
text2 = text2.replace("      # password: 123456 # local Redis has no password\n", "      # password: 123456 # local Redis has no password\n")
p.write_text(text2, encoding="utf-8")
print('removed empty password')
