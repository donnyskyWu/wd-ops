package cn.iocoder.yudao.module.oa.service.personal;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkAccountMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.util.AesUtil;
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
public class WeworkAccountServiceImpl implements WeworkAccountService {

    private static final String MASK = "****";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WeworkAccountMapper weworkAccountMapper;
    private final AesUtil aesUtil;

    @Override
    public PageResult<WeworkRespVO> list(String accountName, String corpId, String status,
                                         Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<WeworkAccountDO> wrapper = new LambdaQueryWrapper<WeworkAccountDO>()
                .eq(WeworkAccountDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(accountName), WeworkAccountDO::getAccountName, accountName)
                .like(StrUtil.isNotBlank(corpId), WeworkAccountDO::getCorpId, corpId)
                .eq(StrUtil.isNotBlank(status), WeworkAccountDO::getStatus, status)
                .orderByDesc(WeworkAccountDO::getId);
        Page<WeworkAccountDO> page = weworkAccountMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<WeworkRespVO> list = page.getRecords().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public WeworkRespVO get(Long id) {
        return toResp(getRequiredInTenant(id));
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-wework", action = "create")
    public Long create(WeworkCreateReq req) {
        Long tenantId = requireTenantId();
        assertCorpAgentUnique(tenantId, req.getCorpId(), req.getAgentId(), null);

        WeworkAccountDO entity = new WeworkAccountDO();
        entity.setTenantId(tenantId);
        entity.setAccountName(req.getAccountName());
        entity.setCorpId(req.getCorpId());
        entity.setAgentId(req.getAgentId());
        entity.setSecretEncrypted(aesUtil.encrypt(req.getSecret()));
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        weworkAccountMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-wework", action = "update")
    public void update(WeworkUpdateReq req) {
        WeworkAccountDO existing = getRequiredInTenant(req.getId());
        Long tenantId = requireTenantId();
        String corpId = StrUtil.blankToDefault(req.getCorpId(), existing.getCorpId());
        String agentId = StrUtil.blankToDefault(req.getAgentId(), existing.getAgentId());
        if (!corpId.equals(existing.getCorpId()) || !agentId.equals(existing.getAgentId())) {
            assertCorpAgentUnique(tenantId, corpId, agentId, req.getId());
            existing.setCorpId(corpId);
            existing.setAgentId(agentId);
        }
        if (StrUtil.isNotBlank(req.getAccountName())) {
            existing.setAccountName(req.getAccountName());
        }
        if (StrUtil.isNotBlank(req.getSecret())) {
            existing.setSecretEncrypted(aesUtil.encrypt(req.getSecret()));
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        weworkAccountMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-wework", action = "delete")
    public void delete(Long id) {
        getRequiredInTenant(id);
        weworkAccountMapper.deleteById(id);
    }

    private WeworkRespVO toResp(WeworkAccountDO entity) {
        WeworkRespVO vo = new WeworkRespVO();
        vo.setId(entity.getId());
        vo.setAccountName(entity.getAccountName());
        vo.setCorpId(entity.getCorpId());
        vo.setAgentId(entity.getAgentId());
        vo.setSecret(StrUtil.isNotBlank(entity.getSecretEncrypted()) ? MASK : null);
        vo.setStatus(entity.getStatus());
        vo.setConnStatus(entity.getConnStatus());
        if (entity.getLastHealthCheckAt() != null) {
            vo.setLastHealthCheckAt(entity.getLastHealthCheckAt().format(DT_FMT));
        }
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(entity.getCreateTime().format(DT_FMT));
        }
        return vo;
    }

    private WeworkAccountDO getRequiredInTenant(Long id) {
        WeworkAccountDO entity = weworkAccountMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private void assertCorpAgentUnique(Long tenantId, String corpId, String agentId, Long excludeId) {
        LambdaQueryWrapper<WeworkAccountDO> wrapper = new LambdaQueryWrapper<WeworkAccountDO>()
                .eq(WeworkAccountDO::getTenantId, tenantId)
                .eq(WeworkAccountDO::getCorpId, corpId)
                .eq(WeworkAccountDO::getAgentId, agentId);
        if (excludeId != null) {
            wrapper.ne(WeworkAccountDO::getId, excludeId);
        }
        if (weworkAccountMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }
}
