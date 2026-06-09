package cn.iocoder.yudao.module.oa.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AiModelConfigUpdateReq;
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
        entity.setModelType(req.getModelType());
        entity.setApiEndpoint(req.getApiEndpoint());
        if (StrUtil.isNotBlank(req.getApiKey())) {
            entity.setApiKeyEncrypted(aesUtil.encrypt(req.getApiKey()));
        }
        entity.setMaxTokens(req.getMaxTokens());
        entity.setTemperature(req.getTemperature());
        entity.setTopP(req.getTopP());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setRemark(req.getRemark());
        ConfigTenantSupport.fillCreate(entity);
        aiModelConfigMapper.insert(entity);
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
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-ai-model", action = "delete")
    public void delete(Long id) {
        AiModelConfigDO existing = getRequired(id);
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
        vo.setModelType(entity.getModelType());
        vo.setApiEndpoint(entity.getApiEndpoint());
        vo.setApiKeyMasked(StrUtil.isNotBlank(entity.getApiKeyEncrypted()) ? MASK : null);
        vo.setMaxTokens(entity.getMaxTokens());
        vo.setTemperature(entity.getTemperature());
        vo.setTopP(entity.getTopP());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
