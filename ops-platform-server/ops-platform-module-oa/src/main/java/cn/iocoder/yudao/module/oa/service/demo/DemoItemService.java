package cn.iocoder.yudao.module.oa.service.demo;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.demo.OaDemoItemDO;
import cn.iocoder.yudao.module.oa.dal.mysql.demo.OaDemoItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoItemService {

    private final OaDemoItemMapper oaDemoItemMapper;

    public OaDemoItemDO getRequired(Long id) {
        OaDemoItemDO item = oaDemoItemMapper.selectById(id);
        if (item == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.equals(item.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return item;
    }
}
