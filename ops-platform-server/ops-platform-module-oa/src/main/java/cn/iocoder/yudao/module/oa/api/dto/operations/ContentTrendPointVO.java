package cn.iocoder.yudao.module.oa.api.dto.operations;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 内容趋势单日数据点
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentTrendPointVO {

    /** 日期 */
    private LocalDate date;

    /** 阅读量 */
    private Long readCount;

    /** 播放量 */
    private Long playCount;
}
