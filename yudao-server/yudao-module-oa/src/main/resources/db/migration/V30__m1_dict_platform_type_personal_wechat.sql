-- V30: 补 dict_platform_type.个微（spec: PRD-M1-运营管理 §4.3 TAB-001）
-- S-R6-TODO4：产品确认个微算"平台"维度（不是 dict_account_type），
-- value 统一约定 WECHAT_PERSONAL（与 WECHAT_OFFICIAL / WECHAT_VIDEO 同前缀命名）
-- 用于：账号分析 Tab、粉丝分析平台筛选、DictSelect 自动拉取（前端 0 代码改动）
-- 注：当前 seed 中无个微账号数据；上线后由 oa_account 表中 platform_type=WECHAT_PERSONAL 的种子补全

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status)
SELECT 'dict_platform_type', '个微', 'WECHAT_PERSONAL', 7, 'ENABLED'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE dict_type = 'dict_platform_type' AND dict_value = 'WECHAT_PERSONAL'
);
