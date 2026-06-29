package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.account.MpFollowerRespVO;

public interface WechatMpFollowerQueryService {

    PageResult<MpFollowerRespVO> pageByAccount(Long accountId, Integer pageNo, Integer pageSize);
}
