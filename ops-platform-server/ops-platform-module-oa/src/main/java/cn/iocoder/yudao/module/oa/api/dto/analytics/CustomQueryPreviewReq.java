package cn.iocoder.yudao.module.oa.api.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomQueryPreviewReq {

    @NotBlank
    private String sqlText;

    /** 分页页码，从 1 开始；不传则沿用大屏/内部调用默认 LIMIT 100 */
    private Integer pageNum;

    /** 每页条数；与 pageNum 同时传入时启用服务端分页 */
    private Integer pageSize;
}
