package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.account.DouyinFollowerRespVO;

public interface DouyinFollowerQueryService {

    PageResult<DouyinFollowerRespVO> pageByAccount(Long accountId, Integer pageNo, Integer pageSize);
}
