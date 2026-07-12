#!/usr/bin/env python3
"""Quick localhost DB probe for post-S4 signoff."""
import pymysql

conn = pymysql.connect(host="127.0.0.1", user="root", password="root")
cur = conn.cursor()
checks = [
    ("author_user", "SELECT COUNT(*) FROM `shenyu-member`.author_user"),
    ("mp_account", "SELECT COUNT(*) FROM `shenyu-mp`.mp_account"),
    ("system_login_log", "SELECT COUNT(*) FROM `shenyu-system`.system_login_log"),
    ("oa_author_exists", "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='wd' AND table_name='oa_author'"),
    ("flyway_v132", "SELECT COUNT(*) FROM wd.flyway_schema_history WHERE version='132'"),
]
for name, sql in checks:
    cur.execute(sql)
    print(f"{name}={cur.fetchone()[0]}")
conn.close()
