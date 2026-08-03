# -*- coding: utf-8 -*-
import re

p = open(
    r"d:\self\sy\运营数据平台\202606\wd\docs\delivery\e2e-artifacts\_oa_cmdline.txt",
    encoding="utf-8",
).read().strip().strip('"')
print("len", len(p))
print("has spring-boot", "spring-boot" in p)
m = re.search(r"spring\.profiles\.active=([^\s\"\\]+)", p)
print("profiles", m.group(1) if m else None)
m2 = re.search(r"([\w.]+Application)\b", p)
print("main", m2.group(1) if m2 else None)
print("HEAD", p[:400])
print("TAIL", p[-500:])
