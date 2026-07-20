#!/usr/bin/env python3
"""Add member-server schema columns missing from local shenyu-member export."""
import pymysql

HOST = "127.0.0.1"
USER = "root"
PASSWORD = "root"
DATABASE = "shenyu-member"

ALTERS = [
    "ALTER TABLE author_user ADD COLUMN access_mode INT NULL AFTER ban_push",
    "ALTER TABLE author_user ADD COLUMN report_name VARCHAR(255) NULL AFTER access_mode",
    "ALTER TABLE author_user ADD COLUMN device_types JSON NULL AFTER report_name",
    "ALTER TABLE author_article ADD COLUMN has_free_code INT NULL DEFAULT 0 COMMENT '是否有免费码' AFTER view_count",
    # Required for system-server assign-user-role → MemberUserApi.getUserByMobile
    "ALTER TABLE member_user ADD COLUMN referrer_type TINYINT NULL DEFAULT 0 COMMENT '推荐人类型' AFTER referrer",
    "ALTER TABLE member_user ADD COLUMN lock_fans_type TINYINT NULL DEFAULT 0 COMMENT '锁粉类型' AFTER lock_fans_id",
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
    print("shenyu-member column patch complete")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
