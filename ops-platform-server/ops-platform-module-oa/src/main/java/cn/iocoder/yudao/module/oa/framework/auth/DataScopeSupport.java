package cn.iocoder.yudao.module.oa.framework.auth;

import cn.iocoder.yudao.module.oa.service.auth.OpsDataScopeSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

public final class DataScopeSupport {

    public static final String ALL = "ALL";
    public static final String IP_GROUP = "IP_GROUP";
    public static final String SELF = "SELF";

    private DataScopeSupport() {
    }

    /**
     * 非 admin：ip_group_id IN memberIpGroupIds；空集合 → id=-1（fail-closed）。
     * SELF 档同样执行成员组过滤。
     */
    public static <T> void applyIpGroupScope(LambdaQueryWrapper<T> wrapper, SFunction<T, Long> ipGroupColumn) {
        OpsDataScopeSupport support = OpsDataScopeSupport.getInstance();
        if (support != null) {
            support.applyIpGroupIdIn(wrapper, ipGroupColumn);
        }
    }
}
