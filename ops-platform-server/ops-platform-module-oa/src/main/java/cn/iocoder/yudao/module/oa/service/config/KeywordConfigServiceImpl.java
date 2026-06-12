package cn.iocoder.yudao.module.oa.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.KeywordConfigCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.KeywordConfigRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.KeywordConfigUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.KeywordConfigDO;
import cn.iocoder.yudao.module.oa.dal.mysql.config.KeywordConfigMapper;
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
public class KeywordConfigServiceImpl implements KeywordConfigService {

    private final KeywordConfigMapper keywordConfigMapper;

    @Override
    public PageResult<KeywordConfigRespVO> list(String platform, String keyword, String status,
                                                Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<KeywordConfigDO> wrapper = new LambdaQueryWrapper<KeywordConfigDO>()
                .eq(KeywordConfigDO::getTenantId, tenantId)
                .eq(StrUtil.isNotBlank(platform), KeywordConfigDO::getPlatform, platform)
                .like(StrUtil.isNotBlank(keyword), KeywordConfigDO::getKeyword, keyword)
                .eq(StrUtil.isNotBlank(status), KeywordConfigDO::getStatus, status)
                .orderByDesc(KeywordConfigDO::getId);
        Page<KeywordConfigDO> page = keywordConfigMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<KeywordConfigRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-keyword", action = "create")
    public Long create(KeywordConfigCreateReq req) {
        KeywordConfigDO entity = new KeywordConfigDO();
        entity.setPlatform(req.getPlatform());
        entity.setKeyword(req.getKeyword());
        entity.setMatchType(StrUtil.blankToDefault(req.getMatchType(), "FUZZY"));
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        ConfigTenantSupport.fillCreate(entity);
        keywordConfigMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-keyword", action = "update")
    public void update(KeywordConfigUpdateReq req) {
        KeywordConfigDO existing = getRequired(req.getId());
        if (StrUtil.isNotBlank(req.getPlatform())) {
            existing.setPlatform(req.getPlatform());
        }
        if (StrUtil.isNotBlank(req.getKeyword())) {
            existing.setKeyword(req.getKeyword());
        }
        if (StrUtil.isNotBlank(req.getMatchType())) {
            existing.setMatchType(req.getMatchType());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        ConfigTenantSupport.fillUpdate(existing);
        keywordConfigMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-keyword", action = "delete")
    public void delete(Long id) {
        KeywordConfigDO existing = getRequired(id);
        keywordConfigMapper.deleteById(existing.getId());
    }

    private KeywordConfigDO getRequired(Long id) {
        return ConfigTenantSupport.getRequiredInTenant(keywordConfigMapper.selectById(id));
    }

    private KeywordConfigRespVO toResp(KeywordConfigDO entity) {
        KeywordConfigRespVO vo = new KeywordConfigRespVO();
        vo.setId(entity.getId());
        vo.setPlatform(entity.getPlatform());
        vo.setKeyword(entity.getKeyword());
        vo.setMatchType(entity.getMatchType());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
