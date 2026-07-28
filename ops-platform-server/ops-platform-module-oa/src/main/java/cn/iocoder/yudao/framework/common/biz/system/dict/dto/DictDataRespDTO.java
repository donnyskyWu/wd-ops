package cn.iocoder.yudao.framework.common.biz.system.dict.dto;

import lombok.Data;

/**
 * Vendored subset of Football {@code DictDataRespDTO} for G-DICT-01 Feign dual-run (C-WP3).
 */
@Data
public class DictDataRespDTO {

    private String label;

    private String value;

    private String dictType;

    /** Football {@code CommonStatusEnum}: 0=启用 1=停用 */
    private Integer status;
}
