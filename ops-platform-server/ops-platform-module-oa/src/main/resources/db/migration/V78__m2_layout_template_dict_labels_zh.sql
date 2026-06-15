-- S-14: Chinese labels for layout template dicts

UPDATE sys_dict_data SET label = '草稿' WHERE dict_type = 'dict_layout_template_status' AND dict_value = 'DRAFT';
UPDATE sys_dict_data SET label = '已启用' WHERE dict_type = 'dict_layout_template_status' AND dict_value = 'ENABLED';
UPDATE sys_dict_data SET label = '已停用' WHERE dict_type = 'dict_layout_template_status' AND dict_value = 'DISABLED';

UPDATE sys_dict_data SET label = '手动创建' WHERE dict_type = 'dict_layout_template_source' AND dict_value = 'MANUAL';
UPDATE sys_dict_data SET label = '链接导入' WHERE dict_type = 'dict_layout_template_source' AND dict_value = 'URL';
UPDATE sys_dict_data SET label = 'Word 导入' WHERE dict_type = 'dict_layout_template_source' AND dict_value = 'DOCX';
UPDATE sys_dict_data SET label = '粘贴导入' WHERE dict_type = 'dict_layout_template_source' AND dict_value = 'PASTE';

UPDATE sys_dict_data SET label = '纯文本' WHERE dict_type = 'dict_content_body_format' AND dict_value = 'PLAIN';
UPDATE sys_dict_data SET label = '富版式' WHERE dict_type = 'dict_content_body_format' AND dict_value = 'LAYOUT';
