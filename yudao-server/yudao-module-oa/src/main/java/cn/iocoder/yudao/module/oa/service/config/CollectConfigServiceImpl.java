package cn.iocoder.yudao.module.oa.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.CollectConfigUpdateReq;
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

import java.util.List;
import java.util.Map;
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

    private final CollectConfigMapper collectConfigMapper;
    private final AccountMapper accountMapper;
    private final AesUtil aesUtil;
    private final DictService dictService;

    @Override
    public PageResult<CollectConfigRespVO> list(String scope, String configName, String platformType,
                                                 String status, Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<CollectConfigDO> wrapper = new LambdaQueryWrapper<CollectConfigDO>()
                .eq(CollectConfigDO::getTenantId, tenantId)
                .eq(CollectConfigDO::getScope, scope)
                .like(StrUtil.isNotBlank(configName), CollectConfigDO::getConfigName, configName)
                .eq(StrUtil.isNotBlank(platformType), CollectConfigDO::getPlatformType, platformType)
                .eq(StrUtil.isNotBlank(status), CollectConfigDO::getStatus, status)
                .orderByDesc(CollectConfigDO::getId);
        Page<CollectConfigDO> page = collectConfigMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<CollectConfigRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-collect", action = "create")
    public Long create(String scope, CollectConfigCreateReq req) {
        validatePlatformType(scope, req.getPlatformType());
        if (CollectConfigScope.INTERNAL.equals(scope)) {
            if (req.getAccountId() == null) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "账号不能为空");
            }
            ConfigTenantSupport.assertAccountInTenant(accountMapper, req.getAccountId());
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
        ConfigTenantSupport.fillUpdate(existing);
        collectConfigMapper.updateById(existing);
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

    private CollectConfigRespVO toResp(CollectConfigDO entity) {
        CollectConfigRespVO vo = new CollectConfigRespVO();
        vo.setId(entity.getId());
        vo.setConfigName(entity.getConfigName());
        vo.setSubType(entity.getSubType());
        vo.setPlatformType(entity.getPlatformType());
        vo.setAccountId(entity.getAccountId());
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
        return vo;
    }
}
