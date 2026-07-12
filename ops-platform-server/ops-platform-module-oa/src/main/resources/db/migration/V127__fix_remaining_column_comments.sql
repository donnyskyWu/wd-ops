-- V127: Add comments for remaining columns missed by V125/V126


-- ========== oa_company ==========
ALTER TABLE oa_company MODIFY COLUMN business_license_keys text DEFAULT NULL COMMENT '营业执照图片Key列表';

-- ========== oa_personal_wechat_account ==========
ALTER TABLE oa_personal_wechat_account MODIFY COLUMN aochuang_wechat_account_id varchar(64) DEFAULT NULL COMMENT '奥创微信账号ID';
ALTER TABLE oa_personal_wechat_account MODIFY COLUMN aochuang_account_ref_id bigint DEFAULT NULL COMMENT '奥创账号关联ID';
ALTER TABLE oa_personal_wechat_account MODIFY COLUMN aochuang_bind_status varchar(32) NOT NULL DEFAULT 'UNBOUND' COMMENT '奥创绑定状态';
ALTER TABLE oa_personal_wechat_account MODIFY COLUMN aochuang_nickname varchar(200) DEFAULT NULL COMMENT '奥创昵称';
ALTER TABLE oa_personal_wechat_account MODIFY COLUMN aochuang_avatar varchar(512) DEFAULT NULL COMMENT '奥创头像';
ALTER TABLE oa_personal_wechat_account MODIFY COLUMN aochuang_is_alive smallint DEFAULT NULL COMMENT '奥创是否在线';
ALTER TABLE oa_personal_wechat_account MODIFY COLUMN last_device_sync_at timestamp DEFAULT NULL COMMENT '最后设备同步时间';
ALTER TABLE oa_personal_wechat_account MODIFY COLUMN last_friend_sync_at timestamp DEFAULT NULL COMMENT '最后好友同步时间';
ALTER TABLE oa_personal_wechat_account MODIFY COLUMN last_message_sync_at timestamp DEFAULT NULL COMMENT '最后消息同步时间';
ALTER TABLE oa_personal_wechat_account MODIFY COLUMN collect_status varchar(32) DEFAULT NULL COMMENT '采集状态';

-- ========== oa_phone ==========
ALTER TABLE oa_phone MODIFY COLUMN settings_screenshot_key varchar(512) DEFAULT NULL COMMENT '设置截图Key';
ALTER TABLE oa_phone MODIFY COLUMN front_image_key varchar(512) DEFAULT NULL COMMENT '正面图片Key';
ALTER TABLE oa_phone MODIFY COLUMN back_image_key varchar(512) DEFAULT NULL COMMENT '背面图片Key';
ALTER TABLE oa_phone MODIFY COLUMN purchase_batch varchar(64) DEFAULT NULL COMMENT '采购批次';
ALTER TABLE oa_phone MODIFY COLUMN purchase_date date DEFAULT NULL COMMENT '采购日期';
ALTER TABLE oa_phone MODIFY COLUMN purchase_time time DEFAULT NULL COMMENT '采购时间';
ALTER TABLE oa_phone MODIFY COLUMN handler_name varchar(64) DEFAULT NULL COMMENT '处理人名称';
ALTER TABLE oa_phone MODIFY COLUMN device_number varchar(64) DEFAULT NULL COMMENT '设备编号';
ALTER TABLE oa_phone MODIFY COLUMN is_aochuang varchar(8) DEFAULT NULL COMMENT '是否奥创设备';
ALTER TABLE oa_phone MODIFY COLUMN phone_type varchar(32) DEFAULT NULL COMMENT '手机类型';

-- ========== oa_realname ==========
ALTER TABLE oa_realname MODIFY COLUMN id_card_front_key varchar(512) DEFAULT NULL COMMENT '身份证正面图片Key';
ALTER TABLE oa_realname MODIFY COLUMN id_card_back_key varchar(512) DEFAULT NULL COMMENT '身份证背面图片Key';

-- ========== oa_task ==========
ALTER TABLE oa_task MODIFY COLUMN scheduled_start timestamp DEFAULT NULL COMMENT '计划开始时间';
ALTER TABLE oa_task MODIFY COLUMN scheduled_end timestamp DEFAULT NULL COMMENT '计划结束时间';

-- ========== oa_wework_account ==========
ALTER TABLE oa_wework_account MODIFY COLUMN last_health_check_at timestamp DEFAULT NULL COMMENT '最后健康检查时间';

-- ========== sys_dict_data ==========
ALTER TABLE sys_dict_data MODIFY COLUMN color_type varchar(32) DEFAULT 'default' COMMENT '颜色类型';
ALTER TABLE sys_dict_data MODIFY COLUMN remark varchar(512) DEFAULT NULL COMMENT '备注';

-- ========== sys_role ==========
ALTER TABLE sys_role MODIFY COLUMN status varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态';
ALTER TABLE sys_role MODIFY COLUMN remark varchar(512) DEFAULT NULL COMMENT '备注';
ALTER TABLE sys_role MODIFY COLUMN data_scope varchar(32) NOT NULL DEFAULT 'ALL' COMMENT '数据范围';

-- ========== sys_tenant ==========
ALTER TABLE sys_tenant MODIFY COLUMN contact_name varchar(64) DEFAULT NULL COMMENT '联系人名称';
ALTER TABLE sys_tenant MODIFY COLUMN contact_phone varchar(32) DEFAULT NULL COMMENT '联系电话';
ALTER TABLE sys_tenant MODIFY COLUMN contact_email varchar(128) DEFAULT NULL COMMENT '联系邮箱';
ALTER TABLE sys_tenant MODIFY COLUMN expire_time timestamp DEFAULT NULL COMMENT '过期时间';
ALTER TABLE sys_tenant MODIFY COLUMN max_accounts int NOT NULL DEFAULT '10' COMMENT '最大账号数';
ALTER TABLE sys_tenant MODIFY COLUMN remark varchar(512) DEFAULT NULL COMMENT '备注';

-- ========== sys_user ==========
ALTER TABLE sys_user MODIFY COLUMN email varchar(128) DEFAULT NULL COMMENT '邮箱';
ALTER TABLE sys_user MODIFY COLUMN phone_encrypted varchar(256) DEFAULT NULL COMMENT '手机号(加密)';
ALTER TABLE sys_user MODIFY COLUMN phone_hash varchar(64) DEFAULT NULL COMMENT '手机号哈希';
ALTER TABLE sys_user MODIFY COLUMN position varchar(64) DEFAULT NULL COMMENT '职位';
ALTER TABLE sys_user MODIFY COLUMN ip_group_id bigint DEFAULT NULL COMMENT 'IP组ID';
ALTER TABLE sys_user MODIFY COLUMN remark varchar(512) DEFAULT NULL COMMENT '备注';
