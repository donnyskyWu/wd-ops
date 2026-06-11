package cn.iocoder.yudao.module.oa.service.system;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.system.LoginLogVO;
import cn.iocoder.yudao.module.oa.api.dto.system.OperationLogVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.system.SysLoginLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.system.SysOperationLogDO;
import cn.iocoder.yudao.module.oa.dal.mysql.system.SysLoginLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.SysOperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysOperationLogMapper sysOperationLogMapper;
    private final SysLoginLogMapper sysLoginLogMapper;

    @Override
    public PageResult<OperationLogVO> listOperation(String username, String module, String level,
                                                    String startTime, String endTime,
                                                    Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysOperationLogDO> wrapper = new LambdaQueryWrapper<SysOperationLogDO>()
                .eq(SysOperationLogDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(username), SysOperationLogDO::getUsername, username)
                .eq(StrUtil.isNotBlank(module), SysOperationLogDO::getModule, module)
                .eq(StrUtil.isNotBlank(level), SysOperationLogDO::getLevel, level)
                .ge(StrUtil.isNotBlank(startTime), SysOperationLogDO::getCreateTime, parseTime(startTime))
                .le(StrUtil.isNotBlank(endTime), SysOperationLogDO::getCreateTime, parseTimeEnd(endTime))
                .orderByDesc(SysOperationLogDO::getId);
        Page<SysOperationLogDO> page = sysOperationLogMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<OperationLogVO> list = page.getRecords().stream().map(this::toOperationVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public PageResult<LoginLogVO> listLogin(String username, String ip, String status,
                                            String startTime, String endTime,
                                            Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<SysLoginLogDO> wrapper = new LambdaQueryWrapper<SysLoginLogDO>()
                .eq(SysLoginLogDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(username), SysLoginLogDO::getUsername, username)
                .like(StrUtil.isNotBlank(ip), SysLoginLogDO::getIp, ip)
                .eq(StrUtil.isNotBlank(status), SysLoginLogDO::getStatus, status)
                .ge(StrUtil.isNotBlank(startTime), SysLoginLogDO::getCreateTime, parseTime(startTime))
                .le(StrUtil.isNotBlank(endTime), SysLoginLogDO::getCreateTime, parseTimeEnd(endTime))
                .orderByDesc(SysLoginLogDO::getId);
        Page<SysLoginLogDO> page = sysLoginLogMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<LoginLogVO> list = page.getRecords().stream().map(this::toLoginVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public void recordLogin(Long tenantId, Long userId, String username, String ip, String userAgent,
                            String status, String message) {
        SysLoginLogDO row = new SysLoginLogDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setUsername(username);
        row.setIp(ip);
        row.setUserAgent(userAgent);
        row.setStatus(status);
        row.setMessage(message);
        row.setCreateTime(LocalDateTime.now());
        sysLoginLogMapper.insert(row);
    }

    private OperationLogVO toOperationVO(SysOperationLogDO row) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(row.getId());
        vo.setUserName(row.getUsername());
        vo.setModule(row.getModule());
        vo.setAction(row.getAction());
        vo.setLevel(row.getLevel());
        vo.setContent(row.getContent());
        vo.setMethod(row.getMethod());
        vo.setParams(row.getRequestParams());
        vo.setResponse(row.getResponseBody());
        vo.setIp(row.getIp());
        vo.setStatus(row.getStatus());
        if (row.getCreateTime() != null) {
            vo.setCreateTime(row.getCreateTime().format(FMT));
        }
        return vo;
    }

    private LoginLogVO toLoginVO(SysLoginLogDO row) {
        LoginLogVO vo = new LoginLogVO();
        vo.setId(row.getId());
        vo.setUserName(row.getUsername());
        vo.setIp(row.getIp());
        vo.setUserAgent(row.getUserAgent());
        vo.setStatus(row.getStatus());
        vo.setMessage(row.getMessage());
        if (row.getCreateTime() != null) {
            vo.setCreateTime(row.getCreateTime().format(FMT));
        }
        return vo;
    }

    private LocalDateTime parseTime(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if (text.length() == 10) {
            return LocalDateTime.parse(text + " 00:00:00", FMT);
        }
        return LocalDateTime.parse(text, FMT);
    }

    private LocalDateTime parseTimeEnd(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if (text.length() == 10) {
            return LocalDateTime.parse(text + " 23:59:59", FMT);
        }
        return LocalDateTime.parse(text, FMT);
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return tenantId;
    }
}
