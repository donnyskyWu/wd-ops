-- 修正 V44 演示漏斗 9803：该 id 此前已被运行期创建的自定义漏斗占用，
-- 导致 9803 同时存在历史步骤(VIEW/READ/LIKE)与 V44 注入的指标步骤，步骤重复。
-- 这里清除非 V44 注入的残留步骤，仅保留 9831~9835（真实指标步骤）。
DELETE FROM oa_funnel_step WHERE funnel_id = 9803 AND id NOT IN (9831, 9832, 9833, 9834, 9835);
