package cn.iocoder.yudao.module.oa.service.collect;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectTaskCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectTaskRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectTaskUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectTaskMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectTaskServiceImpl implements CollectTaskService {

    private static final String MASK = "******";
    private static final String DEFAULT_STATUS = "PENDING";

    private static final String PLATFORM_PERSONAL_WECHAT = "WECHAT_PERSONAL";
    private static final String SOURCE_AOCHUANG = "AOCHUANG_API";

    private final CollectTaskMapper collectTaskMapper;
    private final AccountMapper accountMapper;
    private final PersonalWechatAccountMapper personalWechatAccountMapper;
    private final AesUtil aesUtil;
    private final CollectRunService collectRunService;

    @Override
    public PageResult<CollectTaskRespVO> page(String name, String platformType, String method,
                                              String frequency, String status,
                                              Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<CollectTaskDO> wrapper = new LambdaQueryWrapper<CollectTaskDO>()
                .eq(CollectTaskDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(name), CollectTaskDO::getTaskName, name)
                .eq(StrUtil.isNotBlank(platformType), CollectTaskDO::getPlatformType, platformType)
                .eq(StrUtil.isNotBlank(method), CollectTaskDO::getMethod, method)
                .eq(StrUtil.isNotBlank(frequency), CollectTaskDO::getFrequency, frequency)
                .eq(StrUtil.isNotBlank(status), CollectTaskDO::getStatus, status)
                .orderByDesc(CollectTaskDO::getId);
        Page<CollectTaskDO> page = collectTaskMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        Map<Long, AccountDO> accountById = loadAccountsById(page.getRecords());
        List<CollectTaskRespVO> list = page.getRecords().stream()
                .map(entity -> toResp(entity, resolveAccountName(entity, accountById)))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public CollectTaskRespVO get(Long id) {
        CollectTaskDO entity = getRequiredInTenant(id);
        return toResp(entity, resolveAccountName(entity));
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-collect-task", action = "create")
    public Long create(CollectTaskCreateReq req) {
        validateCron(req.getCron());
        assertCollectTargetInTenant(req.getPlatformType(), req.getSource(), req.getAccountId());
        CollectTaskDO entity = new CollectTaskDO();
        applyCreate(entity, req);
        ConfigTenantSupport.fillCreate(entity);
        collectTaskMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-collect-task", action = "update")
    public void update(CollectTaskUpdateReq req) {
        validateCron(req.getCron());
        CollectTaskDO existing = getRequiredInTenant(req.getId());
        assertCollectTargetInTenant(req.getPlatformType(), req.getSource(), req.getAccountId());
        applyUpdate(existing, req);
        ConfigTenantSupport.fillUpdate(existing);
        collectTaskMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-collect-task", action = "delete")
    public void delete(Long id) {
        CollectTaskDO existing = getRequiredInTenant(id);
        collectTaskMapper.deleteById(existing.getId());
    }

    @Override
    @AuditLog(module = "M10-collect-task", action = "run")
    public void run(Long id) {
        getRequiredInTenant(id);
        collectRunService.run(id);
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-collect-task", action = "update-status")
    public void updateStatus(Long id, String status) {
        CollectTaskDO existing = getRequiredInTenant(id);
        if (StrUtil.isBlank(status)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "状态不能为空");
        }
        existing.setStatus(status);
        ConfigTenantSupport.fillUpdate(existing);
        collectTaskMapper.updateById(existing);
    }

    private void applyCreate(CollectTaskDO entity, CollectTaskCreateReq req) {
        entity.setTaskName(req.getName());
        entity.setPlatformType(req.getPlatformType());
        entity.setAccountId(req.getAccountId());
        entity.setMethod(req.getMethod());
        entity.setSource(req.getSource());
        entity.setFrequency(req.getFrequency());
        entity.setCron(req.getCron());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), DEFAULT_STATUS));
        entity.setRunCount(0);
        entity.setFailCount(0);
        if (StrUtil.isNotBlank(req.getApiConfig())) {
            entity.setApiConfigEncrypted(aesUtil.encrypt(req.getApiConfig()));
        }
    }

    private void applyUpdate(CollectTaskDO entity, CollectTaskUpdateReq req) {
        entity.setTaskName(req.getName());
        entity.setPlatformType(req.getPlatformType());
        entity.setAccountId(req.getAccountId());
        entity.setMethod(req.getMethod());
        entity.setSource(req.getSource());
        entity.setFrequency(req.getFrequency());
        entity.setCron(req.getCron());
        entity.setStatus(req.getStatus());
        if (StrUtil.isNotBlank(req.getApiConfig())) {
            entity.setApiConfigEncrypted(aesUtil.encrypt(req.getApiConfig()));
        }
    }

    private CollectTaskDO getRequiredInTenant(Long id) {
        CollectTaskDO entity = collectTaskMapper.selectById(id);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private void validateCron(String cron) {
        if (StrUtil.isBlank(cron)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "cron 表达式不能为空");
        }
        if (!CronExpression.isValidExpression(cron)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "cron 表达式非法");
        }
    }

    private Map<Long, AccountDO> loadAccountsById(List<CollectTaskDO> records) {
        List<Long> accountIds = records.stream()
                .map(CollectTaskDO::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return accountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(AccountDO::getId, Function.identity(), (a, b) -> a));
    }

    private void assertCollectTargetInTenant(String platformType, String source, Long accountId) {
        if (PLATFORM_PERSONAL_WECHAT.equals(platformType) && SOURCE_AOCHUANG.equals(source)) {
            PersonalWechatAccountDO personal = personalWechatAccountMapper.selectById(accountId);
            ConfigTenantSupport.getRequiredInTenant(personal);
            return;
        }
        ConfigTenantSupport.assertAccountInTenant(accountMapper, accountId);
    }

    private String resolveAccountName(CollectTaskDO entity) {
        if (PLATFORM_PERSONAL_WECHAT.equals(entity.getPlatformType())
                && SOURCE_AOCHUANG.equals(entity.getSource())) {
            PersonalWechatAccountDO personal = personalWechatAccountMapper.selectById(entity.getAccountId());
            if (personal != null && ConfigTenantSupport.requireTenantId().equals(personal.getTenantId())) {
                return personal.getAccountName();
            }
            return null;
        }
        AccountDO account = accountMapper.selectById(entity.getAccountId());
        return account != null ? account.getAccountName() : null;
    }

    private String resolveAccountName(CollectTaskDO entity, Map<Long, AccountDO> accountById) {
        if (PLATFORM_PERSONAL_WECHAT.equals(entity.getPlatformType())
                && SOURCE_AOCHUANG.equals(entity.getSource())) {
            return resolveAccountName(entity);
        }
        AccountDO account = accountById.get(entity.getAccountId());
        return account != null ? account.getAccountName() : null;
    }

    private CollectTaskRespVO toResp(CollectTaskDO entity, String accountName) {
        CollectTaskRespVO vo = new CollectTaskRespVO();
        vo.setId(entity.getId());
        vo.setName(entity.getTaskName());
        vo.setPlatformType(entity.getPlatformType());
        vo.setAccountId(entity.getAccountId());
        vo.setAccountName(accountName);
        vo.setMethod(entity.getMethod());
        vo.setSource(entity.getSource());
        vo.setFrequency(entity.getFrequency());
        vo.setCron(entity.getCron());
        vo.setApiConfig(maskIfPresent(entity.getApiConfigEncrypted()));
        vo.setStatus(entity.getStatus());
        vo.setLastRunAt(entity.getLastRunAt());
        vo.setNextRunAt(entity.getNextRunAt());
        vo.setRunCount(entity.getRunCount());
        vo.setFailCount(entity.getFailCount());
        vo.setCreatedAt(entity.getCreateTime());
        vo.setUpdatedAt(entity.getUpdateTime());
        return vo;
    }

    private String maskIfPresent(String encrypted) {
        return StrUtil.isNotBlank(encrypted) ? MASK : null;
    }
}
