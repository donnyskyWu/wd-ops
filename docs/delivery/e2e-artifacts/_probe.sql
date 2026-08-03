SELECT COUNT(*) AS cnt FROM oa_production_content;
SELECT id,title,creator_user_id,ip_group_id,status,tenant_id,deleted FROM oa_production_content ORDER BY id DESC LIMIT 10;
SELECT COUNT(*) AS alive FROM oa_production_content WHERE deleted=0;
SELECT id, name, status, tenant_id FROM oa_ip_group WHERE deleted=0 AND tenant_id=1 ORDER BY id DESC LIMIT 5;
SELECT COUNT(*) AS mem_cnt FROM oa_ip_group_member WHERE deleted=0 AND tenant_id=1;
SELECT id,user_id,ip_group_id FROM oa_ip_group_member WHERE deleted=0 AND tenant_id=1 AND (user_id=1 OR user_id=1749825673829120001) LIMIT 20;
SELECT label, value FROM system_dict_data WHERE dict_type='dict_content_type' AND deleted=0 LIMIT 20;