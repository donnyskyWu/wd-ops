package cn.iocoder.yudao.framework.common.enums;

/**
 * RPC constants vendored from Football / yudao-common (AL-05).
 * Keep {@link #SYSTEM_NAME} aligned with Football system-server spring.application.name.
 */
public interface RpcConstants {

    String RPC_API_PREFIX = "/rpc-api";

    String SYSTEM_NAME = "system-server";

    String SYSTEM_PREFIX = RPC_API_PREFIX + "/system";

    String MEMBER_NAME = "member-server";

    String MEMBER_PREFIX = RPC_API_PREFIX + "/member";

    String MP_NAME = "mp-server";

    String MP_PREFIX = RPC_API_PREFIX + "/mp";

    String PAY_NAME = "pay-server";

    String PAY_PREFIX = RPC_API_PREFIX + "/pay";

    String INFRA_NAME = "infra-server";

    String INFRA_PREFIX = RPC_API_PREFIX + "/infra";
}
