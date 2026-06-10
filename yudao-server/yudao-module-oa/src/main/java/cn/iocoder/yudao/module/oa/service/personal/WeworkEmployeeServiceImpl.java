package cn.iocoder.yudao.module.oa.service.personal;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkEmployeeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkEmployeeRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkEmployeeUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkEmployeeDO;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkEmployeeMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeworkEmployeeServiceImpl implements WeworkEmployeeService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WeworkEmployeeMapper weworkEmployeeMapper;
    private final WeworkAccountMapper weworkAccountMapper;

    @Override
    public PageResult<WeworkEmployeeRespVO> list(Long weworkAccountId, String nickname, String weworkUserId,
                                                 String status, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<WeworkEmployeeDO> wrapper = new LambdaQueryWrapper<WeworkEmployeeDO>()
                .eq(WeworkEmployeeDO::getTenantId, tenantId)
                .eq(weworkAccountId != null, WeworkEmployeeDO::getWeworkAccountId, weworkAccountId)
                .like(StrUtil.isNotBlank(nickname), WeworkEmployeeDO::getNickname, nickname)
                .like(StrUtil.isNotBlank(weworkUserId), WeworkEmployeeDO::getWeworkUserId, weworkUserId)
                .eq(StrUtil.isNotBlank(status), WeworkEmployeeDO::getStatus, status)
                .orderByDesc(WeworkEmployeeDO::getId);
        Page<WeworkEmployeeDO> page = weworkEmployeeMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<WeworkEmployeeRespVO> list = page.getRecords().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public WeworkEmployeeRespVO get(Long id) {
        return toResp(getRequiredInTenant(id));
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-wework-employee", action = "create")
    public Long create(WeworkEmployeeCreateReq req) {
        Long tenantId = requireTenantId();
        assertWeworkAccountInTenant(req.getWeworkAccountId(), tenantId);
        assertWeworkUserIdUnique(tenantId, req.getWeworkAccountId(), req.getWeworkUserId(), null);

        WeworkEmployeeDO entity = new WeworkEmployeeDO();
        entity.setTenantId(tenantId);
        entity.setWeworkAccountId(req.getWeworkAccountId());
        entity.setNickname(req.getNickname());
        entity.setWeworkUserId(req.getWeworkUserId());
        entity.setPhone(normalizePhone(req.getPhone()));
        entity.setDepartment(req.getDepartment());
        entity.setPosition(req.getPosition());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        weworkEmployeeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-wework-employee", action = "update")
    public void update(WeworkEmployeeUpdateReq req) {
        WeworkEmployeeDO existing = getRequiredInTenant(req.getId());
        Long tenantId = requireTenantId();
        if (!req.getWeworkUserId().equals(existing.getWeworkUserId())) {
            assertWeworkUserIdUnique(tenantId, existing.getWeworkAccountId(), req.getWeworkUserId(), req.getId());
        }
        existing.setNickname(req.getNickname());
        existing.setWeworkUserId(req.getWeworkUserId());
        existing.setPhone(normalizePhone(req.getPhone()));
        existing.setDepartment(req.getDepartment());
        existing.setPosition(req.getPosition());
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        weworkEmployeeMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-wework-employee", action = "delete")
    public void delete(Long id) {
        getRequiredInTenant(id);
        weworkEmployeeMapper.deleteById(id);
    }

    private WeworkEmployeeRespVO toResp(WeworkEmployeeDO entity) {
        WeworkEmployeeRespVO vo = new WeworkEmployeeRespVO();
        vo.setId(entity.getId());
        vo.setWeworkAccountId(entity.getWeworkAccountId());
        vo.setNickname(entity.getNickname());
        vo.setWeworkUserId(entity.getWeworkUserId());
        vo.setPhone(entity.getPhone());
        vo.setDepartment(entity.getDepartment());
        vo.setPosition(entity.getPosition());
        vo.setStatus(entity.getStatus());
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(entity.getCreateTime().format(DT_FMT));
        }
        return vo;
    }

    private WeworkEmployeeDO getRequiredInTenant(Long id) {
        WeworkEmployeeDO entity = weworkEmployeeMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private void assertWeworkAccountInTenant(Long weworkAccountId, Long tenantId) {
        WeworkAccountDO account = weworkAccountMapper.selectById(weworkAccountId);
        if (account == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联企微应用配置不存在");
        }
        if (!tenantId.equals(account.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        if ("DISABLED".equals(account.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED.getCode(), "关联企微应用配置已停用");
        }
    }

    private void assertWeworkUserIdUnique(Long tenantId, Long weworkAccountId, String weworkUserId, Long excludeId) {
        LambdaQueryWrapper<WeworkEmployeeDO> wrapper = new LambdaQueryWrapper<WeworkEmployeeDO>()
                .eq(WeworkEmployeeDO::getTenantId, tenantId)
                .eq(WeworkEmployeeDO::getWeworkAccountId, weworkAccountId)
                .eq(WeworkEmployeeDO::getWeworkUserId, weworkUserId);
        if (excludeId != null) {
            wrapper.ne(WeworkEmployeeDO::getId, excludeId);
        }
        if (weworkEmployeeMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
    }

    private String normalizePhone(String phone) {
        return StrUtil.blankToDefault(phone, null);
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }
}
