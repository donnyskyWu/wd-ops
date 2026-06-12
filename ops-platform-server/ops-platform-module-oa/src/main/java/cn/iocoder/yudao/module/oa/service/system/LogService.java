package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.LoginLogVO;
import cn.iocoder.yudao.module.oa.api.dto.system.OperationLogVO;

public interface LogService {

    PageResult<OperationLogVO> listOperation(String username, String module, String level,
                                             String startTime, String endTime,
                                             Integer pageNo, Integer pageSize);

    PageResult<LoginLogVO> listLogin(String username, String ip, String status,
                                     String startTime, String endTime,
                                     Integer pageNo, Integer pageSize);

    void recordLogin(Long tenantId, Long userId, String username, String ip, String userAgent,
                     String status, String message);
}
