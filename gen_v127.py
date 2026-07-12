#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate V127 migration script for remaining columns without comments."""

import pymysql

conn = pymysql.connect(
    host='101.37.161.136',
    port=3306,
    user='shenyu',
    password='Zhangwu+123456',
    database='wd',
    charset='utf8mb4'
)

# Specific column comments for the remaining uncovered columns
col_comments = {
    ('oa_company', 'business_license_keys'): '营业执照图片Key列表',
    ('oa_personal_wechat_account', 'aochuang_account_ref_id'): '奥创账号关联ID',
    ('oa_personal_wechat_account', 'aochuang_avatar'): '奥创头像',
    ('oa_personal_wechat_account', 'aochuang_bind_status'): '奥创绑定状态',
    ('oa_personal_wechat_account', 'aochuang_is_alive'): '奥创是否在线',
    ('oa_personal_wechat_account', 'aochuang_nickname'): '奥创昵称',
    ('oa_personal_wechat_account', 'aochuang_wechat_account_id'): '奥创微信账号ID',
    ('oa_personal_wechat_account', 'collect_status'): '采集状态',
    ('oa_personal_wechat_account', 'last_device_sync_at'): '最后设备同步时间',
    ('oa_personal_wechat_account', 'last_friend_sync_at'): '最后好友同步时间',
    ('oa_personal_wechat_account', 'last_message_sync_at'): '最后消息同步时间',
    ('oa_phone', 'back_image_key'): '背面图片Key',
    ('oa_phone', 'device_number'): '设备编号',
    ('oa_phone', 'front_image_key'): '正面图片Key',
    ('oa_phone', 'handler_name'): '处理人名称',
    ('oa_phone', 'is_aochuang'): '是否奥创设备',
    ('oa_phone', 'phone_type'): '手机类型',
    ('oa_phone', 'purchase_batch'): '采购批次',
    ('oa_phone', 'purchase_date'): '采购日期',
    ('oa_phone', 'purchase_time'): '采购时间',
    ('oa_phone', 'settings_screenshot_key'): '设置截图Key',
    ('oa_realname', 'id_card_back_key'): '身份证背面图片Key',
    ('oa_realname', 'id_card_front_key'): '身份证正面图片Key',
    ('oa_task', 'scheduled_end'): '计划结束时间',
    ('oa_task', 'scheduled_start'): '计划开始时间',
    ('oa_wework_account', 'last_health_check_at'): '最后健康检查时间',
    ('sys_dict_data', 'color_type'): '颜色类型',
    ('sys_dict_data', 'remark'): '备注',
    ('sys_role', 'data_scope'): '数据范围',
    ('sys_role', 'remark'): '备注',
    ('sys_role', 'status'): '状态',
    ('sys_tenant', 'contact_email'): '联系邮箱',
    ('sys_tenant', 'contact_name'): '联系人名称',
    ('sys_tenant', 'contact_phone'): '联系电话',
    ('sys_tenant', 'expire_time'): '过期时间',
    ('sys_tenant', 'max_accounts'): '最大账号数',
    ('sys_tenant', 'remark'): '备注',
    ('sys_user', 'email'): '邮箱',
    ('sys_user', 'ip_group_id'): 'IP组ID',
    ('sys_user', 'phone_encrypted'): '手机号(加密)',
    ('sys_user', 'phone_hash'): '手机号哈希',
    ('sys_user', 'position'): '职位',
    ('sys_user', 'remark'): '备注',
}

cursor = conn.cursor()

sql_lines = []
sql_lines.append('-- V127: Add comments for remaining columns missed by V125/V126')
sql_lines.append('')

cursor.execute("""
    SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='wd'
    AND TABLE_NAME != 'flyway_schema_history'
    AND (COLUMN_COMMENT='' OR COLUMN_COMMENT IS NULL)
    ORDER BY TABLE_NAME, ORDINAL_POSITION
""")

rows = cursor.fetchall()
current_table = None

for table_name, col_name, col_type, is_nullable, col_default, extra in rows:
    key = (table_name, col_name)
    comment = col_comments.get(key)
    if comment is None:
        # Generate a fallback comment
        comment = col_name
    
    if table_name != current_table:
        sql_lines.append('')
        sql_lines.append(f'-- ========== {table_name} ==========')
        current_table = table_name
    
    # Build MODIFY COLUMN
    if 'auto_increment' in (extra or ''):
        sql_lines.append(
            f"ALTER TABLE {table_name} MODIFY COLUMN {col_name} {col_type} NOT NULL AUTO_INCREMENT COMMENT '{comment}';"
        )
    elif 'DEFAULT_GENERATED' in (extra or ''):
        if is_nullable == 'YES':
            sql_lines.append(
                f"ALTER TABLE {table_name} MODIFY COLUMN {col_name} {col_type} DEFAULT CURRENT_TIMESTAMP COMMENT '{comment}';"
            )
        else:
            sql_lines.append(
                f"ALTER TABLE {table_name} MODIFY COLUMN {col_name} {col_type} NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '{comment}';"
            )
    else:
        if is_nullable == 'NO':
            if col_default is not None:
                sql_lines.append(
                    f"ALTER TABLE {table_name} MODIFY COLUMN {col_name} {col_type} NOT NULL DEFAULT '{col_default}' COMMENT '{comment}';"
                )
            else:
                sql_lines.append(
                    f"ALTER TABLE {table_name} MODIFY COLUMN {col_name} {col_type} NOT NULL COMMENT '{comment}';"
                )
        else:
            if col_default is not None:
                sql_lines.append(
                    f"ALTER TABLE {table_name} MODIFY COLUMN {col_name} {col_type} DEFAULT '{col_default}' COMMENT '{comment}';"
                )
            else:
                sql_lines.append(
                    f"ALTER TABLE {table_name} MODIFY COLUMN {col_name} {col_type} DEFAULT NULL COMMENT '{comment}';"
                )

cursor.close()
conn.close()

output_path = r'd:\self\sy\运营数据平台\202606\wd\ops-platform-server\ops-platform-module-oa\src\main\resources\db\migration\V127__fix_remaining_column_comments.sql'
with open(output_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(sql_lines) + '\n')

print(f"Generated {output_path}")
print(f"Total lines: {len(sql_lines)}")
