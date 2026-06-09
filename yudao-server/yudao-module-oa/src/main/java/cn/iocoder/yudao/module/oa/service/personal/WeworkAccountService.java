package cn.iocoder.yudao.module.oa.service.personal;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkUpdateReq;

public interface WeworkAccountService {

    PageResult<WeworkRespVO> list(String accountName, String corpId, String status,
                                  Integer pageNo, Integer pageSize);

    WeworkRespVO get(Long id);

    Long create(WeworkCreateReq req);

    void update(WeworkUpdateReq req);

    void delete(Long id);
}
