package cn.iocoder.yudao.module.oa.service.metadata;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import org.springframework.stereotype.Component;

@Component
public class MetadataAuthSupport {

    public static final String SUPER_ADMIN_ROLE = "ROLE_OA_ADMIN";

    public void requireSuperAdmin() {
        LoginUser user = LoginUserContext.get();
        if (user == null || user.getAuthorities() == null
                || !user.getAuthorities().contains(SUPER_ADMIN_ROLE)) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN.getCode(), "仅超级管理员可删除元数据");
        }
    }
}
