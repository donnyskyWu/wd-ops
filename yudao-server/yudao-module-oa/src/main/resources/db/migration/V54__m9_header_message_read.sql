-- M9 header message center: unread/read support
ALTER TABLE sys_message ADD COLUMN read_time TIMESTAMP NULL;
CREATE INDEX idx_sys_message_inbox ON sys_message (tenant_id, receiver, status, read_time);

UPDATE sys_user
SET email = 'admin@tenant1.local'
WHERE id = 1001 AND tenant_id = 1 AND (email IS NULL OR email = '');
