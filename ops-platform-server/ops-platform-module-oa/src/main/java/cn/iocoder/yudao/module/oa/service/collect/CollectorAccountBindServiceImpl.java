package cn.iocoder.yudao.module.oa.service.collect;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorAccountBindSaveReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CollectorAccountBindServiceImpl implements CollectorAccountBindService {

    private static final String DEFAULT_BIND_STATUS = "PENDING";

    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final AccountMapper accountMapper;

    @Override
    public CollectorAccountBindRespVO getByOaAccountId(Long oaAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        ConfigTenantSupport.assertAccountInTenant(accountMapper, oaAccountId);
        CollectorAccountBindDO entity = collectorAccountBindMapper.selectOne(
                new LambdaQueryWrapper<CollectorAccountBindDO>()
                        .eq(CollectorAccountBindDO::getTenantId, tenantId)
                        .eq(CollectorAccountBindDO::getOaAccountId, oaAccountId));
        return entity == null ? null : toResp(entity);
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-collector-bind", action = "save")
    public Long saveOrUpdate(CollectorAccountBindSaveReq req) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        AccountDO account = accountMapper.selectById(req.getOaAccountId());
        if (account == null || !tenantId.equals(account.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "账号不存在");
        }
        if (!Objects.equals(req.getPlatformType(), account.getPlatformType())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "平台类型与账号不一致");
        }

        CollectorAccountBindDO existingByOa = findByOaAccountId(tenantId, req.getOaAccountId());
        CollectorAccountBindDO existingByCollector = findByCollectorAccountId(tenantId, req.getCollectorAccountId());
        if (existingByCollector != null
                && (existingByOa == null || !existingByCollector.getId().equals(existingByOa.getId()))) {
            throw new ServiceException(2021, "Collector 账号已绑定其他业务账号");
        }

        String bindStatus = StrUtil.blankToDefault(req.getBindStatus(), DEFAULT_BIND_STATUS);
        LocalDateTime now = LocalDateTime.now();

        if (existingByOa == null) {
            CollectorAccountBindDO entity = new CollectorAccountBindDO();
            entity.setOaAccountId(req.getOaAccountId());
            entity.setCollectorAccountId(req.getCollectorAccountId());
            entity.setPlatformType(req.getPlatformType());
            entity.setBindStatus(bindStatus);
            entity.setConnStatus(req.getConnStatus());
            entity.setLastBindAt(now);
            ConfigTenantSupport.fillCreate(entity);
            collectorAccountBindMapper.insert(entity);
            return entity.getId();
        }

        existingByOa.setCollectorAccountId(req.getCollectorAccountId());
        existingByOa.setPlatformType(req.getPlatformType());
        existingByOa.setBindStatus(bindStatus);
        if (req.getConnStatus() != null) {
            existingByOa.setConnStatus(req.getConnStatus());
        }
        existingByOa.setLastBindAt(now);
        ConfigTenantSupport.fillUpdate(existingByOa);
        collectorAccountBindMapper.updateById(existingByOa);
        return existingByOa.getId();
    }

    private CollectorAccountBindDO findByOaAccountId(Long tenantId, Long oaAccountId) {
        return collectorAccountBindMapper.selectOne(new LambdaQueryWrapper<CollectorAccountBindDO>()
                .eq(CollectorAccountBindDO::getTenantId, tenantId)
                .eq(CollectorAccountBindDO::getOaAccountId, oaAccountId));
    }

    @Override
    @Transactional
    public void updateConnStatus(Long oaAccountId, String connStatus, LocalDateTime healthCheckAt) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        ConfigTenantSupport.assertAccountInTenant(accountMapper, oaAccountId);
        CollectorAccountBindDO existing = findByOaAccountId(tenantId, oaAccountId);
        if (existing == null) {
            return;
        }
        existing.setConnStatus(connStatus);
        existing.setLastHealthCheckAt(healthCheckAt);
        ConfigTenantSupport.fillUpdate(existing);
        collectorAccountBindMapper.updateById(existing);
    }

    private CollectorAccountBindDO findByCollectorAccountId(Long tenantId, String collectorAccountId) {
        return collectorAccountBindMapper.selectOne(new LambdaQueryWrapper<CollectorAccountBindDO>()
                .eq(CollectorAccountBindDO::getTenantId, tenantId)
                .eq(CollectorAccountBindDO::getCollectorAccountId, collectorAccountId));
    }

    private CollectorAccountBindRespVO toResp(CollectorAccountBindDO entity) {
        CollectorAccountBindRespVO vo = new CollectorAccountBindRespVO();
        vo.setId(entity.getId());
        vo.setOaAccountId(entity.getOaAccountId());
        vo.setCollectorAccountId(entity.getCollectorAccountId());
        vo.setPlatformType(entity.getPlatformType());
        vo.setBindStatus(entity.getBindStatus());
        vo.setConnStatus(entity.getConnStatus());
        vo.setLastBindAt(entity.getLastBindAt());
        vo.setLastHealthCheckAt(entity.getLastHealthCheckAt());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
