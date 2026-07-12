package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.OaAccountExtDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.OaAccountExtMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
