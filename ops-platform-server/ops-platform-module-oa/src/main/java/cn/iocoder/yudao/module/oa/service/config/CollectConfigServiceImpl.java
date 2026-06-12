package cn.iocoder.yudao.module.oa.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.CollectConfigDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.config.CollectConfigMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.dict.DictService;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectConfigServiceImpl implements CollectConfigService {

    private static final String MASK = "******";

    private static final Map<String, String> PLATFORM_DICT_BY_SCOPE = Map.of(
            CollectConfigScope.INTERNAL, "dict_platform_type",
            CollectConfigScope.EXTERNAL, "dict_third_platform",
            CollectConfigScope.GENERAL, "dict_ecom_platform"
    );

    /** Legacy seed sub_type values grouped under the platform tab. */
    private static final Set<String> INTERNAL_PLATFORM_LEGACY_SUB_TYPES = Set.of(
            "ACCOUNT_METRICS", "CONTENT_METRICS", "LIVE_METRICS"
    );

    private final CollectConfigMapper collectConfigMapper;
    private final AccountMapper accountMapper;
    private final AesUtil aesUtil;
    private final DictService dictService;

    @Override
    public PageResult<CollectConfigRespVO> list(String scope, String configName, String subType, String platformType,
                                                 String status, Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<CollectConfigDO> wrapper = new LambdaQueryWrapper<CollectConfigDO>()
                .eq(CollectConfigDO::getTenantId, tenantId)
                .eq(CollectConfigDO::getScope, scope)
                .like(StrUtil.isNotBlank(configName), CollectConfigDO::getConfigName, configName)
                .eq(StrUtil.isNotBlank(platformType), CollectConfigDO::getPlatformType, platformType)
                .eq(StrUtil.isNotBlank(status), CollectConfigDO::getStatus, status)
                .orderByDesc(CollectConfigDO::getId);
        applySubTypeFilter(wrapper, scope, subType);
        Page<CollectConfigDO> page = collectConfigMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        Map<Long, AccountDO> accountById = loadAccountsById(page.getRecords());
        List<CollectConfigRespVO> list = page.getRecords().stream()
                .map(entity -> toResp(entity, accountById.get(entity.getAccountId())))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-collect", action = "create")
    public Long create(String scope, CollectConfigCreateReq req) {
        validatePlatformType(scope, req.getPlatformType());
        if (CollectConfigScope.INTERNAL.equals(scope)) {
            if (req.getAccountId() != null) {
                ConfigTenantSupport.assertAccountInTenant(accountMapper, req.getAccountId());
                enrichFromAccount(req);
            } else if (StrUtil.isBlank(req.getAccountIdentifier())) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "账号标识不能为空");
            }
        }
        if (CollectConfigScope.EXTERNAL.equals(scope)) {
            if (StrUtil.isBlank(req.getSubType())) {
                req.setSubType("account");
            }
        }

        CollectConfigDO entity = new CollectConfigDO();
        entity.setScope(scope);
        applyCreate(entity, req);
        ConfigTenantSupport.fillCreate(entity);
        collectConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-collect", action = "update")
    public void update(String scope, CollectConfigUpdateReq req) {
        CollectConfigDO existing = getRequiredInScope(scope, req.getId());
        if (StrUtil.isNotBlank(req.getPlatformType())) {
            validatePlatformType(scope, req.getPlatformType());
            existing.setPlatformType(req.getPlatformType());
        }
        if (req.getAccountId() != null) {
            ConfigTenantSupport.assertAccountInTenant(accountMapper, req.getAccountId());
            existing.setAccountId(req.getAccountId());
            AccountDO account = accountMapper.selectById(req.getAccountId());
            if (account != null) {
                if (StrUtil.isBlank(req.getConfigName())) {
                    existing.setConfigName(account.getAccountName());
                }
                if (StrUtil.isBlank(req.getAccountIdentifier())) {
                    existing.setAccountIdentifier(account.getExternalAccountId());
                }
            }
        }
        if (StrUtil.isNotBlank(req.getConfigName())) {
            existing.setConfigName(req.getConfigName());
        }
        if (req.getSubType() != null) {
            existing.setSubType(req.getSubType());
        }
        if (StrUtil.isNotBlank(req.getCollectFrequency())) {
            existing.setCollectFrequency(req.getCollectFrequency());
        }
        if (StrUtil.isNotBlank(req.getCollectMethod())) {
            existing.setCollectMethod(req.getCollectMethod());
        }
        if (req.getApiUrl() != null) {
            existing.setApiUrl(req.getApiUrl());
        }
        if (StrUtil.isNotBlank(req.getApiKey())) {
            existing.setApiKeyEncrypted(aesUtil.encrypt(req.getApiKey()));
        }
        if (req.getRequestMethod() != null) {
            existing.setRequestMethod(req.getRequestMethod());
        }
        if (req.getRequestParams() != null) {
            existing.setRequestParams(req.getRequestParams());
        }
        if (req.getResponseMapping() != null) {
            existing.setResponseMapping(req.getResponseMapping());
        }
        if (req.getCollectFields() != null) {
            existing.setCollectFields(req.getCollectFields());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        applyExtendedUpdate(existing, req);
        ConfigTenantSupport.fillUpdate(existing);
        collectConfigMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-collect", action = "toggle-status")
    public void toggleStatus(String scope, Long id, String status) {
        CollectConfigDO existing = getRequiredInScope(scope, id);
        existing.setStatus(status);
        ConfigTenantSupport.fillUpdate(existing);
        collectConfigMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-collect", action = "test-connection")
    public boolean testConnection(String scope, Long id) {
        CollectConfigDO existing = getRequiredInScope(scope, id);
        if (CollectConfigScope.GENERAL.equals(scope)) {
            if (StrUtil.isBlank(existing.getDbHost()) || StrUtil.isBlank(existing.getDbName())) {
                return false;
            }
            existing.setConnStatus("CONNECTED");
        } else {
            if (StrUtil.isBlank(existing.getApiUrl()) && StrUtil.isBlank(existing.getDbHost())) {
                return false;
            }
            existing.setConnStatus("CONNECTED");
        }
        ConfigTenantSupport.fillUpdate(existing);
        collectConfigMapper.updateById(existing);
        return true;
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-collect", action = "import-external")
    public cn.iocoder.yudao.module.oa.api.dto.config.ImportResultVO importExternalAccounts(String csvContent) {
        cn.iocoder.yudao.module.oa.api.dto.config.ImportResultVO result =
                new cn.iocoder.yudao.module.oa.api.dto.config.ImportResultVO();
        if (StrUtil.isBlank(csvContent)) {
            return result;
        }
        String[] lines = csvContent.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || (i == 0 && line.toLowerCase().contains("platform"))) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 3) {
                result.setFailCount(result.getFailCount() + 1);
                result.getFailReasons().add("行" + (i + 1) + ": 列数不足");
                continue;
            }
            try {
                CollectConfigCreateReq req = new CollectConfigCreateReq();
                req.setPlatformType(parts[0].trim());
                req.setConfigName(parts[1].trim());
                req.setAccountIdentifier(parts[2].trim());
                req.setSubType("account");
                req.setStatus("ENABLED");
                create(CollectConfigScope.EXTERNAL, req);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.setFailCount(result.getFailCount() + 1);
                result.getFailReasons().add("行" + (i + 1) + ": " + e.getMessage());
            }
        }
        return result;
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-collect", action = "delete")
    public void delete(String scope, Long id) {
        CollectConfigDO existing = getRequiredInScope(scope, id);
        collectConfigMapper.deleteById(existing.getId());
    }

    private CollectConfigDO getRequiredInScope(String scope, Long id) {
        CollectConfigDO entity = collectConfigMapper.selectById(id);
        ConfigTenantSupport.getRequiredInTenant(entity);
        if (!scope.equals(entity.getScope())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return entity;
    }

    private void applyCreate(CollectConfigDO entity, CollectConfigCreateReq req) {
        entity.setConfigName(req.getConfigName());
        entity.setSubType(req.getSubType());
        entity.setPlatformType(req.getPlatformType());
        entity.setAccountId(req.getAccountId());
        entity.setCollectFrequency(StrUtil.blankToDefault(req.getCollectFrequency(), "DAILY"));
        entity.setCollectMethod(req.getCollectMethod());
        entity.setApiUrl(req.getApiUrl());
        if (StrUtil.isNotBlank(req.getApiKey())) {
            entity.setApiKeyEncrypted(aesUtil.encrypt(req.getApiKey()));
        }
        entity.setRequestMethod(req.getRequestMethod());
        entity.setRequestParams(req.getRequestParams());
        entity.setResponseMapping(req.getResponseMapping());
        entity.setCollectFields(req.getCollectFields());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setRemark(req.getRemark());
        applyExtendedCreate(entity, req);
    }

    private void applyExtendedCreate(CollectConfigDO entity, CollectConfigCreateReq req) {
        entity.setAccountIdentifier(req.getAccountIdentifier());
        entity.setAppId(req.getAppId());
        if (StrUtil.isNotBlank(req.getAppSecret())) {
            entity.setAppSecretEncrypted(aesUtil.encrypt(req.getAppSecret()));
        }
        entity.setCookie(req.getCookie());
        if (StrUtil.isNotBlank(req.getAuthToken())) {
            entity.setAuthTokenEncrypted(aesUtil.encrypt(req.getAuthToken()));
        }
        entity.setFieldMapping(req.getFieldMapping());
        entity.setIsLive(Boolean.TRUE.equals(req.getIsLive()) ? 1 : 0);
        entity.setDbHost(req.getDbHost());
        entity.setDbPort(req.getDbPort());
        entity.setDbName(req.getDbName());
        entity.setDbUsername(req.getDbUsername());
        if (StrUtil.isNotBlank(req.getDbPassword())) {
            entity.setDbPasswordEncrypted(aesUtil.encrypt(req.getDbPassword()));
        }
        entity.setTableName(StrUtil.blankToDefault(req.getTableName(), "pay_all_order"));
        entity.setSyncMode(StrUtil.blankToDefault(req.getSyncMode(), "INCREMENTAL"));
        entity.setConnStatus("DISCONNECTED");
    }

    private void applyExtendedUpdate(CollectConfigDO entity, CollectConfigUpdateReq req) {
        if (req.getAccountIdentifier() != null) {
            entity.setAccountIdentifier(req.getAccountIdentifier());
        }
        if (req.getAppId() != null) {
            entity.setAppId(req.getAppId());
        }
        if (StrUtil.isNotBlank(req.getAppSecret())) {
            entity.setAppSecretEncrypted(aesUtil.encrypt(req.getAppSecret()));
        }
        if (req.getCookie() != null) {
            entity.setCookie(req.getCookie());
        }
        if (StrUtil.isNotBlank(req.getAuthToken())) {
            entity.setAuthTokenEncrypted(aesUtil.encrypt(req.getAuthToken()));
        }
        if (req.getFieldMapping() != null) {
            entity.setFieldMapping(req.getFieldMapping());
        }
        if (req.getIsLive() != null) {
            entity.setIsLive(Boolean.TRUE.equals(req.getIsLive()) ? 1 : 0);
        }
        if (req.getDbHost() != null) {
            entity.setDbHost(req.getDbHost());
        }
        if (req.getDbPort() != null) {
            entity.setDbPort(req.getDbPort());
        }
        if (req.getDbName() != null) {
            entity.setDbName(req.getDbName());
        }
        if (req.getDbUsername() != null) {
            entity.setDbUsername(req.getDbUsername());
        }
        if (StrUtil.isNotBlank(req.getDbPassword())) {
            entity.setDbPasswordEncrypted(aesUtil.encrypt(req.getDbPassword()));
        }
        if (req.getTableName() != null) {
            entity.setTableName(req.getTableName());
        }
        if (StrUtil.isNotBlank(req.getSyncMode())) {
            entity.setSyncMode(req.getSyncMode());
        }
    }

    private void applySubTypeFilter(LambdaQueryWrapper<CollectConfigDO> wrapper, String scope, String subType) {
        if (StrUtil.isBlank(subType)) {
            return;
        }
        if (CollectConfigScope.INTERNAL.equals(scope) && "platform".equals(subType)) {
            wrapper.and(w -> w.eq(CollectConfigDO::getSubType, subType)
                    .or().in(CollectConfigDO::getSubType, INTERNAL_PLATFORM_LEGACY_SUB_TYPES)
                    .or().isNull(CollectConfigDO::getSubType));
            return;
        }
        wrapper.eq(CollectConfigDO::getSubType, subType);
    }

    private void validatePlatformType(String scope, String platformType) {
        if (StrUtil.isBlank(platformType)) {
            return;
        }
        String dictType = PLATFORM_DICT_BY_SCOPE.get(scope);
        if (dictType != null && !dictService.isValidValue(dictType, platformType)) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID);
        }
    }

    private Map<Long, AccountDO> loadAccountsById(List<CollectConfigDO> records) {
        List<Long> accountIds = records.stream()
                .map(CollectConfigDO::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return accountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(AccountDO::getId, Function.identity(), (a, b) -> a));
    }

    private void enrichFromAccount(CollectConfigCreateReq req) {
        AccountDO account = accountMapper.selectById(req.getAccountId());
        if (account == null) {
            return;
        }
        if (StrUtil.isBlank(req.getConfigName())) {
            req.setConfigName(account.getAccountName());
        }
        if (StrUtil.isBlank(req.getAccountIdentifier())) {
            req.setAccountIdentifier(account.getExternalAccountId());
        }
    }

    private CollectConfigRespVO toResp(CollectConfigDO entity, AccountDO account) {
        CollectConfigRespVO vo = new CollectConfigRespVO();
        vo.setId(entity.getId());
        vo.setConfigName(entity.getConfigName());
        vo.setSubType(entity.getSubType());
        vo.setPlatformType(entity.getPlatformType());
        vo.setAccountId(entity.getAccountId());
        vo.setAccountIdentifier(entity.getAccountIdentifier());
        if (account != null) {
            vo.setAccountName(account.getAccountName());
            if (StrUtil.isBlank(vo.getAccountIdentifier())) {
                vo.setAccountIdentifier(account.getExternalAccountId());
            }
        }
        vo.setCollectFrequency(entity.getCollectFrequency());
        vo.setCollectMethod(entity.getCollectMethod());
        vo.setApiUrl(entity.getApiUrl());
        vo.setApiKeyMasked(StrUtil.isNotBlank(entity.getApiKeyEncrypted()) ? MASK : null);
        vo.setRequestMethod(entity.getRequestMethod());
        vo.setRequestParams(entity.getRequestParams());
        vo.setResponseMapping(entity.getResponseMapping());
        vo.setCollectFields(entity.getCollectFields());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setAppId(entity.getAppId());
        vo.setAppSecretMasked(StrUtil.isNotBlank(entity.getAppSecretEncrypted()) ? MASK : null);
        vo.setCookie(entity.getCookie());
        vo.setAuthTokenMasked(StrUtil.isNotBlank(entity.getAuthTokenEncrypted()) ? MASK : null);
        vo.setFieldMapping(entity.getFieldMapping());
        vo.setIsLive(entity.getIsLive() != null && entity.getIsLive() == 1);
        vo.setDbHost(entity.getDbHost());
        vo.setDbPort(entity.getDbPort());
        vo.setDbName(entity.getDbName());
        vo.setDbUsername(entity.getDbUsername());
        vo.setDbPasswordMasked(StrUtil.isNotBlank(entity.getDbPasswordEncrypted()) ? MASK : null);
        vo.setTableName(entity.getTableName());
        vo.setSyncMode(entity.getSyncMode());
        vo.setConnStatus(entity.getConnStatus());
        return vo;
    }
}
