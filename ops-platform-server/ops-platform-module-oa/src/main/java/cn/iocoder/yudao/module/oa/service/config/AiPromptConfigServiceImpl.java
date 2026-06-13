package cn.iocoder.yudao.module.oa.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.AiPromptConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AiPromptConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AiPromptConfigUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AiPromptConfigDO;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AiPromptConfigMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPromptConfigServiceImpl implements AiPromptConfigService {

    private final AiPromptConfigMapper aiPromptConfigMapper;

    @Override
    public PageResult<AiPromptConfigRespVO> list(String templateName, String scene, String status,
                                                 Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<AiPromptConfigDO> wrapper = new LambdaQueryWrapper<AiPromptConfigDO>()
                .eq(AiPromptConfigDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(templateName), AiPromptConfigDO::getTemplateName, templateName)
                .eq(StrUtil.isNotBlank(scene), AiPromptConfigDO::getScene, scene)
                .eq(StrUtil.isNotBlank(status), AiPromptConfigDO::getStatus, status)
                .orderByDesc(AiPromptConfigDO::getId);
        Page<AiPromptConfigDO> page = aiPromptConfigMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<AiPromptConfigRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-ai-prompt", action = "create")
    public Long create(AiPromptConfigCreateReq req) {
        AiPromptConfigDO entity = new AiPromptConfigDO();
        entity.setTemplateName(req.getTemplateName());
        entity.setVersion("v1");
        entity.setScene(req.getScene());
        entity.setContentType(req.getContentType());
        entity.setDocumentType(req.getDocumentType());
        entity.setPromptContent(req.getPromptContent());
        entity.setVariableDesc(req.getVariableDesc());
        entity.setTemperature(req.getTemperature());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setRemark(req.getRemark());
        ConfigTenantSupport.fillCreate(entity);
        aiPromptConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-ai-prompt", action = "update")
    public void update(AiPromptConfigUpdateReq req) {
        AiPromptConfigDO existing = getRequired(req.getId());
        if (StrUtil.isNotBlank(req.getTemplateName())) {
            existing.setTemplateName(req.getTemplateName());
        }
        if (req.getScene() != null) {
            existing.setScene(req.getScene());
        }
        if (req.getContentType() != null) {
            existing.setContentType(req.getContentType());
        }
        if (req.getDocumentType() != null) {
            existing.setDocumentType(req.getDocumentType());
        }
        if (StrUtil.isNotBlank(req.getPromptContent())) {
            existing.setPromptContent(req.getPromptContent());
        }
        if (req.getVariableDesc() != null) {
            existing.setVariableDesc(req.getVariableDesc());
        }
        if (req.getTemperature() != null) {
            existing.setTemperature(req.getTemperature());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        if (StrUtil.isNotBlank(req.getPromptContent()) || StrUtil.isNotBlank(req.getTemplateName())) {
            existing.setVersion(bumpVersion(existing.getVersion()));
        }
        ConfigTenantSupport.fillUpdate(existing);
        aiPromptConfigMapper.updateById(existing);
    }

    @Override
    public AiPromptConfigRespVO get(Long id) {
        return toResp(getRequired(id));
    }

    private String bumpVersion(String current) {
        if (StrUtil.isBlank(current) || !current.startsWith("v")) {
            return "v2";
        }
        try {
            int num = Integer.parseInt(current.substring(1));
            return "v" + (num + 1);
        } catch (NumberFormatException e) {
            return "v2";
        }
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-ai-prompt", action = "delete")
    public void delete(Long id) {
        AiPromptConfigDO existing = getRequired(id);
        aiPromptConfigMapper.deleteById(existing.getId());
    }

    private AiPromptConfigDO getRequired(Long id) {
        AiPromptConfigDO entity = aiPromptConfigMapper.selectById(id);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private AiPromptConfigRespVO toResp(AiPromptConfigDO entity) {
        AiPromptConfigRespVO vo = new AiPromptConfigRespVO();
        vo.setId(entity.getId());
        vo.setTemplateName(entity.getTemplateName());
        vo.setVersion(entity.getVersion());
        vo.setScene(entity.getScene());
        vo.setContentType(entity.getContentType());
        vo.setDocumentType(entity.getDocumentType());
        vo.setPromptContent(entity.getPromptContent());
        vo.setVariableDesc(entity.getVariableDesc());
        vo.setTemperature(entity.getTemperature());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
