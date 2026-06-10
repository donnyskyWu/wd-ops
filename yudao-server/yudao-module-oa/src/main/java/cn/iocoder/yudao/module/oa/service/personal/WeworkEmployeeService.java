package cn.iocoder.yudao.module.oa.service.personal;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkEmployeeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkEmployeeRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkEmployeeUpdateReq;

public interface WeworkEmployeeService {

    PageResult<WeworkEmployeeRespVO> list(Long weworkAccountId, String nickname, String weworkUserId,
                                          String status, Integer pageNo, Integer pageSize);

    WeworkEmployeeRespVO get(Long id);

    Long create(WeworkEmployeeCreateReq req);

    void update(WeworkEmployeeUpdateReq req);

    void delete(Long id);
}
