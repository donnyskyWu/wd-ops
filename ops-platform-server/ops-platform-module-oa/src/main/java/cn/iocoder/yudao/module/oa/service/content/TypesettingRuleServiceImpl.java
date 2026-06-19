package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.content.TypesettingRuleCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.TypesettingRuleUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.TypesettingRuleVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.TypesettingRuleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.content.TypesettingRuleMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TypesettingRuleServiceImpl implements TypesettingRuleService {

    private final TypesettingRuleMapper typesettingRuleMapper;

    @Override
    public PageResult<TypesettingRuleVO> list(String name, String status, Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<TypesettingRuleDO> wrapper = new LambdaQueryWrapper<TypesettingRuleDO>()
                .eq(TypesettingRuleDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(name), TypesettingRuleDO::getName, name)
                .eq(StrUtil.isNotBlank(status), TypesettingRuleDO::getStatus, status)
                .orderByAsc(TypesettingRuleDO::getSort)
                .orderByDesc(TypesettingRuleDO::getId);
        Page<TypesettingRuleDO> page = typesettingRuleMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public List<TypesettingRuleVO> listEnabled() {
        return typesettingRuleMapper.selectList(new LambdaQueryWrapper<TypesettingRuleDO>()
                        .eq(TypesettingRuleDO::getTenantId, requireTenantId())
                        .eq(TypesettingRuleDO::getStatus, "ENABLED")
                        .orderByAsc(TypesettingRuleDO::getSort))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public TypesettingRuleVO getById(Long id) {
        return toVO(requireEntity(id));
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-typesetting", action = "create")
    public Long create(TypesettingRuleCreateReq req) {
        Long tenantId = requireTenantId();
        TypesettingRuleDO entity = new TypesettingRuleDO();
        entity.setTenantId(tenantId);
        entity.setRuleCode(req.getRuleCode().trim());
        entity.setName(req.getName().trim());
        entity.setDescription(req.getDescription());
        entity.setRuleConfig(JSONUtil.toJsonStr(req.getRuleConfig()));
        entity.setSort(req.getSort() != null ? req.getSort() : 0);
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        typesettingRuleMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-typesetting", action = "update")
    public void update(TypesettingRuleUpdateReq req) {
        TypesettingRuleDO existing = requireEntity(req.getId());
        if (StrUtil.isNotBlank(req.getName())) {
            existing.setName(req.getName().trim());
        }
        if (req.getDescription() != null) {
            existing.setDescription(req.getDescription());
        }
        if (req.getRuleConfig() != null) {
            existing.setRuleConfig(JSONUtil.toJsonStr(req.getRuleConfig()));
        }
        if (req.getSort() != null) {
            existing.setSort(req.getSort());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        typesettingRuleMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-typesetting", action = "delete")
    public void delete(Long id) {
        typesettingRuleMapper.deleteById(requireEntity(id).getId());
    }

    private TypesettingRuleDO requireEntity(Long id) {
        TypesettingRuleDO entity = typesettingRuleMapper.selectById(id);
        if (entity == null || !requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return entity;
    }

    private TypesettingRuleVO toVO(TypesettingRuleDO entity) {
        TypesettingRuleVO vo = new TypesettingRuleVO();
        vo.setId(entity.getId());
        vo.setRuleCode(entity.getRuleCode());
        vo.setName(entity.getName());
        vo.setDescription(entity.getDescription());
        if (StrUtil.isNotBlank(entity.getRuleConfig())) {
            vo.setRuleConfig(JSONUtil.parse(entity.getRuleConfig()));
        }
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
