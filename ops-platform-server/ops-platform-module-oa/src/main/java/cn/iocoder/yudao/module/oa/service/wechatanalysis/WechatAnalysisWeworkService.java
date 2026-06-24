package cn.iocoder.yudao.module.oa.service.wechatanalysis;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.WeworkAnalysisDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.WeworkAnalysisListItemVO;

import java.time.LocalDate;

public interface WechatAnalysisWeworkService {

    PageResult<WeworkAnalysisListItemVO> list(Long accountId, String accountName,
                                              LocalDate statDate, Integer pageNo, Integer pageSize);

    WeworkAnalysisDetailVO detail(Long accountId, LocalDate startDate, LocalDate endDate);
}
