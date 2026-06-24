package cn.iocoder.yudao.module.oa.service.wechatanalysis;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.PersonalAnalysisDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.PersonalAnalysisListItemVO;

import java.time.LocalDate;

public interface WechatAnalysisPersonalService {

    PageResult<PersonalAnalysisListItemVO> list(Long accountId, String accountName,
                                                LocalDate statDate, Integer pageNo, Integer pageSize);

    PersonalAnalysisDetailVO detail(Long accountId, LocalDate startDate, LocalDate endDate);
}
