package cn.iocoder.yudao.module.oa.service.collect.aochuang;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AoCreateAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AoCreateApiDO;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AoCreateAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AoCreateApiMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangApiClient;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangFriendPageResult;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangMessagePageResult;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangWechatAccountDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 奥创通道 Adapter（Channel-B · ADR-045 · M10-AO-S-02）。
 */
@Component
@RequiredArgsConstructor
public class AochuangAdapter {

    private final AoCreateAccountMapper aoCreateAccountMapper;
    private final AoCreateApiMapper aoCreateApiMapper;
    private final AochuangApiClient aochuangApiClient;

    public List<AochuangWechatAccountDTO> listWechatAccounts(Long aoCreateAccountId) {
        AoCreateAccountDO account = requireAoCreateAccount(aoCreateAccountId);
        AoCreateApiDO api = requireApiForAccount(account);
        return aochuangApiClient.listWechatAccounts(api, account.getAochuangAccountId());
    }

    public AochuangFriendPageResult listFriends(Long aoCreateAccountId, String wechatAccountId,
                                                String cursor, int limit) {
        AoCreateAccountDO account = requireAoCreateAccount(aoCreateAccountId);
        AoCreateApiDO api = requireApiForAccount(account);
        return aochuangApiClient.listFriends(api, wechatAccountId, cursor, limit);
    }

    public AochuangMessagePageResult listFriendMessages(Long aoCreateAccountId, String wechatAccountId,
                                                        String cursor, int limit) {
        AoCreateAccountDO account = requireAoCreateAccount(aoCreateAccountId);
        AoCreateApiDO api = requireApiForAccount(account);
        return aochuangApiClient.listFriendMessages(api, wechatAccountId, cursor, limit);
    }

    public AoCreateAccountDO requireAoCreateAccount(Long aoCreateAccountId) {
        AoCreateAccountDO entity = aoCreateAccountMapper.selectById(aoCreateAccountId);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private AoCreateApiDO requireApiForAccount(AoCreateAccountDO account) {
        AoCreateApiDO api = aoCreateApiMapper.selectById(account.getAocreateApiId());
        if (api == null || !ConfigTenantSupport.requireTenantId().equals(api.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "奥创接口凭证不存在");
        }
        return api;
    }

    public AoCreateApiDO requireTenantApi() {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        AoCreateApiDO api = aoCreateApiMapper.selectOne(new LambdaQueryWrapper<AoCreateApiDO>()
                .eq(AoCreateApiDO::getTenantId, tenantId)
                .last("LIMIT 1"));
        if (api == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "请先配置奥创接口凭证");
        }
        return api;
    }
}
