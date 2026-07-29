package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.OaAccountExtDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.OaAccountExtMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * wd.oa_account_ext access; each call routed via @DS("master") (ADR-050/051).
 */
@Service
@RequiredArgsConstructor
public class OaAccountExtDataService {

    private final OaAccountExtMapper oaAccountExtMapper;

    @DS("master")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<Long, OaAccountExtDO> loadExtMap(Long tenantId, List<Long> mpIds) {
        if (mpIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return oaAccountExtMapper.selectList(new LambdaQueryWrapper<OaAccountExtDO>()
                        .eq(OaAccountExtDO::getTenantId, tenantId)
                        .in(OaAccountExtDO::getMpAccountId, mpIds))
                .stream()
                .collect(Collectors.toMap(OaAccountExtDO::getMpAccountId, e -> e, (a, b) -> a));
    }

    @DS("master")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OaAccountExtDO findByMpAccountId(Long tenantId, Long mpAccountId) {
        return oaAccountExtMapper.selectOne(new LambdaQueryWrapper<OaAccountExtDO>()
                .eq(OaAccountExtDO::getTenantId, tenantId)
                .eq(OaAccountExtDO::getMpAccountId, mpAccountId)
                .last("LIMIT 1"));
    }

    /** 按 IP 组反查公众号 mp_account_id（数据权限预过滤，避免先分页再过滤导致空页） */
    @DS("master")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Long> listMpAccountIdsByIpGroupIds(Long tenantId, Collection<Long> ipGroupIds) {
        if (tenantId == null || ipGroupIds == null || ipGroupIds.isEmpty()) {
            return Collections.emptyList();
        }
        return oaAccountExtMapper.selectList(new LambdaQueryWrapper<OaAccountExtDO>()
                        .eq(OaAccountExtDO::getTenantId, tenantId)
                        .in(OaAccountExtDO::getIpGroupId, ipGroupIds)
                        .select(OaAccountExtDO::getMpAccountId))
                .stream()
                .map(OaAccountExtDO::getMpAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @DS("master")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void insert(OaAccountExtDO ext) {
        oaAccountExtMapper.insert(ext);
    }

    @DS("master")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void updateById(OaAccountExtDO ext) {
        oaAccountExtMapper.updateById(ext);
    }
}
