package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.MpUserMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * shenyu-mp.mp_user access; each call routed via @DS("mp") (ADR-050/051).
 */
@Service
@RequiredArgsConstructor
public class MpUserDataService {

    private static final int SUBSCRIBE_STATUS_ACTIVE = 1;

    private final MpUserMapper mpUserMapper;

    @DS("mp")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Page<MpUserDO> selectPageByAccount(Page<MpUserDO> page, Long tenantId, Long mpAccountId) {
        return mpUserMapper.selectPage(page, new LambdaQueryWrapper<MpUserDO>()
                .eq(MpUserDO::getTenantId, tenantId)
                .eq(MpUserDO::getAccountId, mpAccountId)
                .eq(MpUserDO::getSubscribeStatus, SUBSCRIBE_STATUS_ACTIVE)
                .orderByDesc(MpUserDO::getSubscribeTime)
                .orderByDesc(MpUserDO::getId));
    }
}
