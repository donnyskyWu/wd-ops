package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.MpAccountMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * shenyu-mp.mp_account access; each call routed via @DS("mp") (ADR-050/051).
 */
@Service
@RequiredArgsConstructor
public class MpAccountDataService {

    private final MpAccountMapper mpAccountMapper;

    @DS("mp")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Page<MpAccountDO> selectPage(Page<MpAccountDO> page, LambdaQueryWrapper<MpAccountDO> wrapper) {
        return mpAccountMapper.selectPage(page, wrapper);
    }

    @DS("mp")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MpAccountDO selectById(Long id) {
        return mpAccountMapper.selectById(id);
    }

    @DS("mp")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MpAccountDO requireById(Long id, Long tenantId) {
        MpAccountDO mp = mpAccountMapper.selectById(id);
        if (mp == null || !Objects.equals(mp.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return mp;
    }

    @DS("mp")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void insert(MpAccountDO mp) {
        mpAccountMapper.insert(mp);
    }

    @DS("mp")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void updateById(MpAccountDO mp) {
        mpAccountMapper.updateById(mp);
    }

    @DS("mp")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MpAccountDO selectByAppId(Long tenantId, String appId) {
        return mpAccountMapper.selectOne(new LambdaQueryWrapper<MpAccountDO>()
                .eq(MpAccountDO::getTenantId, tenantId)
                .eq(MpAccountDO::getAppId, appId)
                .last("LIMIT 1"));
    }
}
