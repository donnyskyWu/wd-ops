package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.system.LoginLogVO;
import cn.iocoder.yudao.module.oa.api.dto.system.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 系统日志门面（S3）：列表读 Football system DS；Ops sys_* 表停写。
 */
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final LoginLogAdapter loginLogAdapter;
    private final OperateLogAdapter operateLogAdapter;

    @Override
    public PageResult<OperationLogVO> listOperation(String username, String module, String level,
                                                    String startTime, String endTime,
                                                    Integer pageNo, Integer pageSize) {
        return operateLogAdapter.list(requireTenantId(), username, module, level,
                startTime, endTime, pageNo, pageSize);
    }

    @Override
    public PageResult<LoginLogVO> listLogin(String username, String ip, String status,
                                            String startTime, String endTime,
                                            Integer pageNo, Integer pageSize) {
        return loginLogAdapter.list(requireTenantId(), username, ip, status,
                startTime, endTime, pageNo, pageSize);
    }

    @Override
    public void recordLogin(Long tenantId, Long userId, String username, String ip, String userAgent,
                            String status, String message) {
        // AC-S3-06: Football system-server owns login logs; Ops sys_login_log deprecated for writes.
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return tenantId;
    }
}
