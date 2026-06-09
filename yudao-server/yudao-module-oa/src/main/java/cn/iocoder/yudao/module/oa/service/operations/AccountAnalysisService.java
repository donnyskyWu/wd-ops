package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.AccountAnalysisVO;

public interface AccountAnalysisService {

    PageResult<AccountAnalysisVO> list(String platform, Long ipGroupId, String keyword,
                                       Integer pageNo, Integer pageSize);
}
