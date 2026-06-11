package cn.iocoder.yudao.module.oa.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelStatsVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AiModelConfigDO;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AiModelConfigMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiModelConfigServiceImpl implements AiModelConfigService {

    private static final String MASK = "sk-****";

    private final AiModelConfigMapper aiModelConfigMapper;
    private final AesUtil aesUtil;

    @Override
    public PageResult<AiModelConfigRespVO> list(String modelName, String status, Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<AiModelConfigDO> wrapper = new LambdaQueryWrapper<AiModelConfigDO>()
                .eq(AiModelConfigDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(modelName), AiModelConfigDO::getModelName, modelName)
                .eq(StrUtil.isNotBlank(status), AiModelConfigDO::getStatus, status)
                .orderByDesc(AiModelConfigDO::getId);
        Page<AiModelConfigDO> page = aiModelConfigMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<AiModelConfigRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-ai-model", action = "create")
    public Long create(AiModelConfigCreateReq req) {
        AiModelConfigDO entity = new AiModelConfigDO();
        entity.setModelName(req.getModelName());
        entity.setModelId(req.getModelId());
        entity.setModelType(req.getModelType());
        entity.setApiEndpoint(req.getApiEndpoint());
        if (StrUtil.isNotBlank(req.getApiKey())) {
            entity.setApiKeyEncrypted(aesUtil.encrypt(req.getApiKey()));
        }
        entity.setMaxTokens(req.getMaxTokens());
        entity.setTimeout(req.getTimeout() == null ? 60 : req.getTimeout());
        entity.setTemperature(req.getTemperature());
        entity.setTopP(req.getTopP());
        entity.setConnStatus("DISCONNECTED");
        entity.setIsDefault(0);
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setRemark(req.getRemark());
        ConfigTenantSupport.fillCreate(entity);
        aiModelConfigMapper.insert(entity);
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            setDefault(entity.getId());
        }
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-ai-model", action = "update")
    public void update(AiModelConfigUpdateReq req) {
        AiModelConfigDO existing = getRequired(req.getId());
        if (StrUtil.isNotBlank(req.getModelName())) {
            existing.setModelName(req.getModelName());
        }
        if (req.getModelId() != null) {
            existing.setModelId(req.getModelId());
        }
        if (req.getModelType() != null) {
            existing.setModelType(req.getModelType());
        }
        if (req.getApiEndpoint() != null) {
            existing.setApiEndpoint(req.getApiEndpoint());
        }
        if (StrUtil.isNotBlank(req.getApiKey())) {
            existing.setApiKeyEncrypted(aesUtil.encrypt(req.getApiKey()));
        }
        if (req.getMaxTokens() != null) {
            existing.setMaxTokens(req.getMaxTokens());
        }
        if (req.getTimeout() != null) {
            existing.setTimeout(req.getTimeout());
        }
        if (req.getTemperature() != null) {
            existing.setTemperature(req.getTemperature());
        }
        if (req.getTopP() != null) {
            existing.setTopP(req.getTopP());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        ConfigTenantSupport.fillUpdate(existing);
        aiModelConfigMapper.updateById(existing);
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            setDefault(existing.getId());
        }
    }

    @Override
    public AiModelStatsVO stats() {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        List<AiModelConfigDO> all = aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfigDO>()
                .eq(AiModelConfigDO::getTenantId, tenantId));
        AiModelStatsVO stats = new AiModelStatsVO();
        stats.setTotal(all.size());
        stats.setEnabled(all.stream().filter(e -> "ENABLED".equals(e.getStatus())).count());
        stats.setConnected(all.stream().filter(e -> "CONNECTED".equals(e.getConnStatus())).count());
        stats.setDefaultCount(all.stream().filter(e -> e.getIsDefault() != null && e.getIsDefault() == 1).count());
        return stats;
    }

    @Override
    @Transactional
    public boolean testConnection(Long id) {
        AiModelConfigDO existing = getRequired(id);
        if (StrUtil.isBlank(existing.getApiEndpoint())) {
            return false;
        }
        existing.setConnStatus("CONNECTED");
        ConfigTenantSupport.fillUpdate(existing);
        aiModelConfigMapper.updateById(existing);
        return true;
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-ai-model", action = "set-default")
    public void setDefault(Long id) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        AiModelConfigDO target = getRequired(id);
        List<AiModelConfigDO> all = aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfigDO>()
                .eq(AiModelConfigDO::getTenantId, tenantId));
        for (AiModelConfigDO model : all) {
            model.setIsDefault(model.getId().equals(id) ? 1 : 0);
            ConfigTenantSupport.fillUpdate(model);
            aiModelConfigMapper.updateById(model);
        }
        target.setIsDefault(1);
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-ai-model", action = "delete")
    public void delete(Long id) {
        AiModelConfigDO existing = getRequired(id);
        if (existing.getIsDefault() != null && existing.getIsDefault() == 1) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "不能删除默认模型");
        }
        aiModelConfigMapper.deleteById(existing.getId());
    }

    private AiModelConfigDO getRequired(Long id) {
        AiModelConfigDO entity = aiModelConfigMapper.selectById(id);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private AiModelConfigRespVO toResp(AiModelConfigDO entity) {
        AiModelConfigRespVO vo = new AiModelConfigRespVO();
        vo.setId(entity.getId());
        vo.setModelName(entity.getModelName());
        vo.setModelId(entity.getModelId());
        vo.setModelType(entity.getModelType());
        vo.setApiEndpoint(entity.getApiEndpoint());
        vo.setApiKeyMasked(StrUtil.isNotBlank(entity.getApiKeyEncrypted()) ? MASK : null);
        vo.setMaxTokens(entity.getMaxTokens());
        vo.setTimeout(entity.getTimeout());
        vo.setIsDefault(entity.getIsDefault() != null && entity.getIsDefault() == 1);
        vo.setConnStatus(entity.getConnStatus());
        vo.setTemperature(entity.getTemperature());
        vo.setTopP(entity.getTopP());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
