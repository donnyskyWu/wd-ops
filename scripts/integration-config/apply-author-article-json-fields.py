#!/usr/bin/env python3
"""Repair shenyu-member.author_article JSON columns for member-server JacksonTypeHandler."""
import pymysql

HOST = "127.0.0.1"
USER = "root"
PASSWORD = "root"
DATABASE = "shenyu-member"

REPAIRS = [
    # privilege_types: scalar "2" -> "[2]" (List<Integer>)
    """
    UPDATE author_article
    SET privilege_types = CONCAT('[', privilege_types, ']'),
        update_time = NOW()
    WHERE privilege_types IS NOT NULL
      AND privilege_types NOT LIKE '[%'
      AND deleted = 0
    """,
    # match_scheme: empty string -> NULL (List<MatchBaseVO>)
    """
    UPDATE author_article
    SET match_scheme = NULL,
        update_time = NOW()
    WHERE match_scheme IS NOT NULL
      AND TRIM(match_scheme) = ''
      AND deleted = 0
    """,
]


def main() -> int:
    conn = pymysql.connect(host=HOST, user=USER, password=PASSWORD, database=DATABASE)
    cur = conn.cursor()
    total = 0
    for sql in REPAIRS:
        cur.execute(sql)
        affected = cur.rowcount
        total += affected
        print(f"OK rows={affected}: {sql.strip().splitlines()[0]}")
    conn.commit()
    conn.close()
    print(f"shenyu-member author_article JSON repair complete (rows={total})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
