package cn.iocoder.yudao.module.oa.service.collect.external;

/**
 * Channel-D · 外部竞品采集 Adapter（ADR-052 · M10-EXT-P0 骨架）。
 *
 * <p>与 {@link cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorAdapter}
 *（Channel-A · 自有账号）对称；不经 {@code oa_collector_account_bind}。
 *
 * <p>运营凭账号（Cookie/Token）经租户级 {@code oa_tenant_collector_credential} 解析（ADR-052 §3.4），
 * 不经 {@code oa_collect_config} 或任务行内嵌密钥。
 */
public interface ExternalCollectorAdapter {

    /**
     * 按 M8 外部配置 ID 执行指定 dataType 采集。
     *
     * @param collectConfigId     {@code oa_collect_config.id}，须 {@code scope=EXTERNAL} 且 {@code sub_type=account}
     * @param dataType            {@code dict_collect_data_type} 以 {@code EXT_} 前缀的外部枚举
     * @param credentialProfile   租户凭账号 profile，可空（默认 {@code default}）
     * @return 本次写入/更新行数
     */
    int execute(Long collectConfigId, String dataType, String credentialProfile);
}
