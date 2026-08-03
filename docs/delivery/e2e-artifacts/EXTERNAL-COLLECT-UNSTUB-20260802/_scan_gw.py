# -*- coding: utf-8 -*-
from pathlib import Path
import re

p = Path(r"d:\self\sy\运营数据平台\202606\wd\scripts\logs\gateway-integration.log")
raw = p.read_bytes()[-8_000_000:].replace(b"\x00", b"")
text = raw.decode("utf-8", "replace")

idx = 0
hits = 0
while True:
    i = text.find("external-collect", idx)
    if i < 0:
        break
    window = text[max(0, i - 2000) : i + 1000]
    ts = re.findall(r"2026-08-02T20:\d{2}:\d{2}", window)
    codes = re.findall(r'"(?:code|resultCode|status)"\s*:\s*(\d+)', window)
    msgs = re.findall(r'"(?:msg|resultMsg|message)"\s*:\s*"([^"]{0,80})"', window)
    urls = re.findall(r'"requestUrl"\s*:\s*"([^"]+)"', window)
    interesting = any(t[11:16] >= "20:40" for t in ts) if ts else False
    if interesting or (hits < 3 and ("500" in codes or any("系统" in m for m in msgs))):
        print("---")
        print("ts", ts[:4])
        print("urls", urls[:3])
        print("codes", codes[:8])
        print("msgs", msgs[:8])
        hits += 1
        if hits >= 25:
            break
    idx = i + 1
print("printed", hits)
