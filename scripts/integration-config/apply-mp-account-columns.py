#!/usr/bin/env python3
"""Add mp-server schema columns missing from local shenyu-mp export (MpAccountDO)."""
import pymysql

HOST = "127.0.0.1"
USER = "root"
PASSWORD = "root"
DATABASE = "shenyu-mp"

ALTERS = [
    "ALTER TABLE mp_account ADD COLUMN attention_status TINYINT NULL DEFAULT 0 COMMENT '关注状态'",
    "ALTER TABLE mp_account ADD COLUMN group_name VARCHAR(255) NULL COMMENT '分组名称'",
    "ALTER TABLE mp_account ADD COLUMN is_share TINYINT NULL DEFAULT 0 COMMENT '是否共享'",
]


def main() -> int:
    conn = pymysql.connect(host=HOST, user=USER, password=PASSWORD, database=DATABASE)
    cur = conn.cursor()
    for sql in ALTERS:
        try:
            cur.execute(sql)
            print(f"OK: {sql}")
        except pymysql.err.OperationalError as e:
            if e.args[0] == 1060:
                print(f"exists: {sql}")
            else:
                raise
    conn.commit()
    conn.close()
    print("shenyu-mp mp_account column patch complete")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
