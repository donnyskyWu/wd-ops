-- S-15: M2 layout style library (96-equivalent workbench)

CREATE TABLE IF NOT EXISTS oa_layout_style (
    id                  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    style_code          VARCHAR(64)  NOT NULL,
    name                VARCHAR(100) NOT NULL,
    category            VARCHAR(30)  NOT NULL COMMENT 'HEADING/BODY/IMAGE_TEXT/GUIDE/DIVIDER',
    tags                VARCHAR(200) NULL,
    html_snippet        LONGTEXT     NOT NULL,
    thumbnail_file_id   BIGINT       NULL,
    sort                INT          NOT NULL DEFAULT 0,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
    creator             VARCHAR(64)  DEFAULT 'system',
    create_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             VARCHAR(64)  DEFAULT 'system',
    update_time         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    UNIQUE KEY uk_oa_layout_style_code (tenant_id, style_code),
    KEY idx_oa_layout_style_tenant (tenant_id),
    KEY idx_oa_layout_style_category (tenant_id, category, status)
);

INSERT INTO sys_dict_type (type, name, status) VALUES
('dict_layout_style_category', 'Layout style category', 'ENABLED'),
('dict_layout_style_status', 'Layout style status', 'ENABLED')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_dict_data (dict_type, label, dict_value, sort, status) VALUES
('dict_layout_style_category', '标题', 'HEADING', 1, 'ENABLED'),
('dict_layout_style_category', '正文', 'BODY', 2, 'ENABLED'),
('dict_layout_style_category', '图文', 'IMAGE_TEXT', 3, 'ENABLED'),
('dict_layout_style_category', '引导', 'GUIDE', 4, 'ENABLED'),
('dict_layout_style_category', '分隔', 'DIVIDER', 5, 'ENABLED'),
('dict_layout_style_status', '已启用', 'ENABLED', 1, 'ENABLED'),
('dict_layout_style_status', '已停用', 'DISABLED', 2, 'ENABLED')
ON DUPLICATE KEY UPDATE label = VALUES(label);

INSERT INTO sys_permission (id, code, name, module, creator, updater) VALUES
(50, 'oa:layout-style:query', 'Layout style query', 'M2', 's15', 's15'),
(51, 'oa:layout-style:create', 'Layout style create', 'M2', 's15', 's15'),
(52, 'oa:layout-style:update', 'Layout style update', 'M2', 's15', 's15'),
(53, 'oa:layout-style:delete', 'Layout style delete', 'M2', 's15', 's15')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 50), (1, 51), (1, 52), (1, 53)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- Seed ~30 styles for tenant 1
INSERT INTO oa_layout_style (tenant_id, style_code, name, category, tags, html_snippet, sort, status, creator, updater) VALUES
(1, 'H2-CENTER-GREEN', '居中绿色大标题', 'HEADING', '标题,居中,赛事', '<h2 style="text-align:center;font-size:20px;font-weight:bold;color:#07c160;margin:24px 0 16px;">标题文字</h2>', 10, 'ENABLED', 'system', 'system'),
(1, 'H2-LEFT-BOLD', '左对齐粗体标题', 'HEADING', '标题,左对齐', '<h2 style="font-size:18px;font-weight:bold;color:#1a1a1a;margin:20px 0 12px;border-left:4px solid #07c160;padding-left:12px;">标题文字</h2>', 11, 'ENABLED', 'system', 'system'),
(1, 'H3-ACCENT', '小标题强调', 'HEADING', '小标题', '<h3 style="font-size:16px;font-weight:bold;color:#333;margin:16px 0 8px;">小标题</h3>', 12, 'ENABLED', 'system', 'system'),
(1, 'H2-CENTER-WHITE', '居中白字标题', 'HEADING', '标题,深色背景', '<h2 style="text-align:center;font-size:20px;font-weight:bold;color:#ffffff;background:#07c160;padding:12px 16px;border-radius:4px;">标题文字</h2>', 13, 'ENABLED', 'system', 'system'),
(1, 'H2-UNDERLINE', '下划线标题', 'HEADING', '标题,简约', '<h2 style="font-size:18px;font-weight:bold;color:#1a1a1a;border-bottom:2px solid #07c160;padding-bottom:8px;margin:20px 0 12px;">标题文字</h2>', 14, 'ENABLED', 'system', 'system'),
(1, 'H2-NUMBERED', '序号标题', 'HEADING', '标题,序号', '<p style="font-size:18px;font-weight:bold;color:#07c160;margin:20px 0 12px;"><span style="background:#07c160;color:#fff;padding:2px 8px;border-radius:4px;margin-right:8px;">01</span>标题文字</p>', 15, 'ENABLED', 'system', 'system'),

(1, 'P-STANDARD', '标准正文', 'BODY', '正文,默认', '<p style="font-size:16px;color:#333;line-height:1.75;margin-bottom:16px;text-align:justify;">正文段落内容</p>', 20, 'ENABLED', 'system', 'system'),
(1, 'P-INDENT', '首行缩进正文', 'BODY', '正文,缩进', '<p style="font-size:16px;color:#333;line-height:1.75;margin-bottom:16px;text-indent:2em;">正文段落内容</p>', 21, 'ENABLED', 'system', 'system'),
(1, 'P-HIGHLIGHT', '高亮正文', 'BODY', '正文,强调', '<p style="font-size:16px;color:#333;line-height:1.75;margin-bottom:16px;background:#f0fff4;padding:12px 16px;border-radius:4px;">正文段落内容</p>', 22, 'ENABLED', 'system', 'system'),
(1, 'P-CENTER', '居中正文', 'BODY', '正文,居中', '<p style="font-size:16px;color:#666;line-height:1.75;margin-bottom:16px;text-align:center;">正文段落内容</p>', 23, 'ENABLED', 'system', 'system'),
(1, 'P-SMALL', '小号说明文字', 'BODY', '说明,脚注', '<p style="font-size:14px;color:#999;line-height:1.6;margin-bottom:12px;">说明文字内容</p>', 24, 'ENABLED', 'system', 'system'),
(1, 'P-BOLD-LEAD', '加粗导语', 'BODY', '导语,加粗', '<p style="font-size:16px;color:#333;line-height:1.75;margin-bottom:16px;font-weight:bold;">导语段落内容</p>', 25, 'ENABLED', 'system', 'system'),
(1, 'P-QUOTE-INLINE', '引用段落', 'BODY', '引用', '<blockquote style="font-size:15px;color:#666;background:#f7f7f7;border-left:4px solid #07c160;padding:12px 16px;margin:16px 0;line-height:1.6;">引用内容</blockquote>', 26, 'ENABLED', 'system', 'system'),
(1, 'P-LIST-ITEM', '列表项', 'BODY', '列表', '<p style="font-size:16px;color:#333;line-height:1.75;margin-bottom:8px;padding-left:16px;"><span style="color:#07c160;margin-right:8px;">●</span>列表项内容</p>', 27, 'ENABLED', 'system', 'system'),

(1, 'IT-IMAGE-TOP', '上图下文', 'IMAGE_TEXT', '图文,上图', '<p style="margin:16px 0;"><img src="" style="width:100%;max-width:100%;height:auto;border-radius:4px;" alt=""/></p><p style="font-size:16px;color:#333;line-height:1.75;margin-bottom:16px;">图片说明文字</p>', 30, 'ENABLED', 'system', 'system'),
(1, 'IT-IMAGE-CAPTION', '带标题图片', 'IMAGE_TEXT', '图文,标题', '<section style="margin:16px 0;"><p style="font-size:14px;color:#07c160;font-weight:bold;margin-bottom:8px;">图片标题</p><img src="" style="width:100%;max-width:100%;height:auto;border-radius:4px;" alt=""/></section>', 31, 'ENABLED', 'system', 'system'),
(1, 'IT-SIDE-BY-SIDE', '左右图文', 'IMAGE_TEXT', '图文,并排', '<section style="display:flex;gap:12px;margin:16px 0;align-items:flex-start;"><img src="" style="width:40%;max-width:40%;height:auto;border-radius:4px;" alt=""/><p style="flex:1;font-size:15px;color:#333;line-height:1.75;">图文并排说明</p></section>', 32, 'ENABLED', 'system', 'system'),
(1, 'IT-FULL-WIDTH', '全宽图片', 'IMAGE_TEXT', '图文,全宽', '<p style="margin:16px -16px;"><img src="" style="width:100%;max-width:100%;height:auto;" alt=""/></p>', 33, 'ENABLED', 'system', 'system'),
(1, 'IT-ROUND-AVATAR', '圆形头像图文', 'IMAGE_TEXT', '头像,作者', '<section style="display:flex;gap:12px;margin:16px 0;align-items:center;"><img src="" style="width:48px;height:48px;border-radius:50%;object-fit:cover;" alt=""/><div><p style="font-size:15px;font-weight:bold;color:#333;margin:0;">作者名称</p><p style="font-size:13px;color:#999;margin:4px 0 0;">作者简介</p></div></section>', 34, 'ENABLED', 'system', 'system'),
(1, 'IT-CARD', '卡片图文', 'IMAGE_TEXT', '卡片', '<section style="border:1px solid #e5e5e5;border-radius:8px;overflow:hidden;margin:16px 0;"><img src="" style="width:100%;height:auto;" alt=""/><p style="padding:12px 16px;font-size:15px;color:#333;margin:0;">卡片描述</p></section>', 35, 'ENABLED', 'system', 'system'),

(1, 'G-TIP-BOX', '提示引导框', 'GUIDE', '引导,提示', '<section style="background:#fff7e6;border:1px solid #ffd591;border-radius:8px;padding:16px;margin:16px 0;"><p style="font-size:15px;color:#d48806;font-weight:bold;margin:0 0 8px;">💡 温馨提示</p><p style="font-size:15px;color:#333;line-height:1.75;margin:0;">引导说明内容</p></section>', 40, 'ENABLED', 'system', 'system'),
(1, 'G-CTA-BUTTON', '行动号召', 'GUIDE', '引导,按钮', '<p style="text-align:center;margin:24px 0;"><span style="display:inline-block;background:#07c160;color:#fff;font-size:16px;padding:12px 32px;border-radius:24px;">立即参与</span></p>', 41, 'ENABLED', 'system', 'system'),
(1, 'G-READ-MORE', '阅读更多引导', 'GUIDE', '引导,阅读', '<p style="text-align:center;font-size:15px;color:#07c160;margin:24px 0;">▼ 继续阅读 ▼</p>', 42, 'ENABLED', 'system', 'system'),
(1, 'G-QR-PLACEHOLDER', '二维码引导', 'GUIDE', '引导,二维码', '<section style="text-align:center;margin:24px 0;padding:16px;background:#f7f7f7;border-radius:8px;"><p style="font-size:48px;margin:0;">📱</p><p style="font-size:14px;color:#666;margin:8px 0 0;">长按识别二维码</p></section>', 43, 'ENABLED', 'system', 'system'),
(1, 'G-END-SIGN', '文末签名', 'GUIDE', '引导,签名', '<section style="text-align:center;margin:32px 0 16px;padding-top:16px;border-top:1px dashed #e5e5e5;"><p style="font-size:14px;color:#999;margin:0;">— END —</p><p style="font-size:13px;color:#ccc;margin:8px 0 0;">感谢阅读</p></section>', 44, 'ENABLED', 'system', 'system'),
(1, 'G-HIGHLIGHT-BOX', '重点引导框', 'GUIDE', '引导,重点', '<section style="background:linear-gradient(135deg,#07c160 0%,#06ae56 100%);border-radius:8px;padding:20px;margin:16px 0;"><p style="font-size:16px;color:#fff;font-weight:bold;margin:0 0 8px;">重点提示</p><p style="font-size:15px;color:rgba(255,255,255,0.9);line-height:1.75;margin:0;">重点说明内容</p></section>', 45, 'ENABLED', 'system', 'system'),

(1, 'D-SIMPLE', '简单分隔线', 'DIVIDER', '分隔', '<hr style="border:none;border-top:1px solid #e5e5e5;margin:24px 0;"/>', 50, 'ENABLED', 'system', 'system'),
(1, 'D-DOTTED', '虚线分隔', 'DIVIDER', '分隔,虚线', '<hr style="border:none;border-top:1px dashed #ccc;margin:24px 0;"/>', 51, 'ENABLED', 'system', 'system'),
(1, 'D-ORNAMENT', '装饰分隔', 'DIVIDER', '分隔,装饰', '<p style="text-align:center;color:#ccc;margin:24px 0;font-size:14px;letter-spacing:8px;">· · ·</p>', 52, 'ENABLED', 'system', 'system'),
(1, 'D-GRADIENT', '渐变分隔', 'DIVIDER', '分隔,渐变', '<hr style="border:none;height:2px;background:linear-gradient(to right,transparent,#07c160,transparent);margin:24px 0;"/>', 53, 'ENABLED', 'system', 'system')
ON DUPLICATE KEY UPDATE name = VALUES(name);
