package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.Data;

import java.util.List;

/**
 * 内容趋势详情（spec: API-M1-运营管理 §4.5 / FR-M1-006）
 */
@Data
public class ContentTrendDetailVO {

    /** 内容 ID */
    private Long contentId;

    /** 内容标题 */
    private String title;

    /** 趋势数据系列（按 statDate 升序） */
    private List<ContentTrendPointVO> series;
}
