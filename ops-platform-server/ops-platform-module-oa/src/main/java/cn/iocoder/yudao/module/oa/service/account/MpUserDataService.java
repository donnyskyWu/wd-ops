package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpUserDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * G-MP-01 阻塞：Football {@code MpUserApi} 无按 accountId 分页读粉丝 RPC。
 * member/mp multidb 已移除 — 调用方将收到 503 直至 Football 补齐 RPC。
 */
@Service
@RequiredArgsConstructor
public class MpUserDataService {

    public Page<MpUserDO> selectPageByAccount(Page<MpUserDO> page, Long tenantId, Long mpAccountId) {
        throw new ServiceException(OaErrorCodes.MP_USER_READ_RPC_MISSING);
    }
}
