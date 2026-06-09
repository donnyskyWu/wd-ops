package cn.iocoder.yudao.module.oa.framework.auth;

import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

public final class DataScopeSupport {

    public static final String ALL = "ALL";
    public static final String IP_GROUP = "IP_GROUP";
    public static final String SELF = "SELF";

    private DataScopeSupport() {
    }

    public static <T> void applyIpGroupScope(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> ipGroupColumn) {
        LoginUser user = LoginUserContext.get();
        if (user == null || !IP_GROUP.equals(user.getDataScope())) {
            return;
        }
        Long ipGroupId = user.getIpGroupId();
        if (ipGroupId != null) {
            wrapper.eq(ipGroupColumn, ipGroupId);
        }
    }
}
