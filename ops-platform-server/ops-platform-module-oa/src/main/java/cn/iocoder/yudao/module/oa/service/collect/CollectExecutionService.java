package cn.iocoder.yudao.module.oa.service.collect;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncFriendsReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncMessagesReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncMessagesRespVO;
import cn.iocoder.yudao.module.oa.config.CollectProperties;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.service.personal.PersonalWechatAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 采集执行入口（M10-COL-S-03 前为 stub；后续切片按 source 路由 Adapter）。
 */
@Service
@RequiredArgsConstructor
public class CollectExecutionService {

    private static final String SOURCE_AOCHUANG = "AOCHUANG_API";
    private static final String PLATFORM_PERSONAL_WECHAT = "WECHAT_PERSONAL";

    private final CollectProperties collectProperties;
    private final PersonalWechatAccountService personalWechatAccountService;

    public CollectExecutionResult execute(CollectTaskDO task) {
        if (collectProperties.getStub().isForceFail()) {
            return CollectExecutionResult.failure("模拟采集 API 失败");
        }
        if (SOURCE_AOCHUANG.equals(task.getSource())
                && PLATFORM_PERSONAL_WECHAT.equals(task.getPlatformType())) {
            return executeAochuangPersonalWechat(task);
        }
        // TODO M10-API/WECOM: 按 task.source 调用对应 Adapter
        return CollectExecutionResult.success(0);
    }

    private CollectExecutionResult executeAochuangPersonalWechat(CollectTaskDO task) {
        if (task.getAccountId() == null) {
            return CollectExecutionResult.failure("采集任务未配置个微账号");
        }
        try {
            personalWechatAccountService.syncFriends(task.getAccountId(), new PersonalWechatSyncFriendsReq());
            PersonalWechatSyncMessagesRespVO messages = personalWechatAccountService.syncMessages(
                    task.getAccountId(), new PersonalWechatSyncMessagesReq());
            return CollectExecutionResult.success(messages.getSyncedCount());
        } catch (ServiceException ex) {
            return CollectExecutionResult.failure(ex.getMessage());
        }
    }
}
