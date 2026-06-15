package cn.iocoder.yudao.module.oa.controller.dev;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.dev.DingTalkStatusVO;
import cn.iocoder.yudao.module.oa.api.dto.dev.DingTalkTestSendReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkProperties;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkRobotClient;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkRobotProperties;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkWorkNotifyClient;
import cn.iocoder.yudao.module.oa.framework.notification.NotificationProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * dev profile 钉钉推送诊断与测试（勿用于生产）。
 * 生产环境业务通知由 {@link cn.iocoder.yudao.module.oa.service.notification.NotificationServiceImpl} 自动触发。
 */
@Profile("dev")
@RestController
@RequestMapping("/admin-api/oa/dev/dingtalk")
@RequiredArgsConstructor
public class DingTalkDevController {

    private final DingTalkRobotClient dingTalkRobotClient;
    private final DingTalkRobotProperties robotProperties;
    private final DingTalkWorkNotifyClient dingTalkWorkNotifyClient;
    private final DingTalkProperties dingTalkProperties;
    private final NotificationProperties notificationProperties;
    private final SysUserMapper sysUserMapper;

    @GetMapping("/status")
    public CommonResult<DingTalkStatusVO> status() {
        DingTalkStatusVO vo = new DingTalkStatusVO();
        boolean workReady = dingTalkWorkNotifyClient.isEnabled();
        boolean robotReady = dingTalkRobotClient.isEnabled();

        vo.setWorkNotifyEnabled(workReady);
        vo.setWorkNotifySkipReason(dingTalkWorkNotifyClient.getSkipReason());
        vo.setAgentIdConfigured(dingTalkProperties.getAgentId() != null);
        vo.setRobotFallbackEnabled(robotProperties.isEnabled());
        vo.setRobotId(StrUtil.blankToDefault(robotProperties.getRobotId(), ""));
        vo.setWebhookConfigured(StrUtil.isNotBlank(robotProperties.getWebhookUrl()));
        vo.setSecretConfigured(StrUtil.isNotBlank(robotProperties.getSecret()));
        vo.setPlatformBaseUrlConfigured(StrUtil.isNotBlank(notificationProperties.getPlatformBaseUrl()));

        if (workReady) {
            vo.setPrimaryChannel("work_notify");
            vo.setSendEnabled(true);
            vo.setSkipReason(null);
        } else if (robotReady) {
            vo.setPrimaryChannel("robot_webhook");
            vo.setSendEnabled(true);
            vo.setSkipReason(dingTalkWorkNotifyClient.getSkipReason());
        } else {
            vo.setPrimaryChannel("none");
            vo.setSendEnabled(false);
            vo.setSkipReason(StrUtil.blankToDefault(
                    dingTalkWorkNotifyClient.getSkipReason(),
                    dingTalkRobotClient.getSkipReason()));
        }
        return CommonResult.success(vo);
    }

    @PostMapping("/test-work-send")
    public CommonResult<Map<String, Object>> testWorkSend(@Valid @RequestBody DingTalkTestSendReq req) {
        String skipReason = dingTalkWorkNotifyClient.getSkipReason();
        if (skipReason != null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), skipReason);
        }
        SysUserDO user = resolveTargetUser(req.getUserId());
        String title = "【运营平台】通知测试";
        String markdown = "这是一条 dev 环境**工作通知**测试。\n\n"
                + "接收人：" + user.getNickname() + "（userId=" + user.getId() + "）";
        String link = StrUtil.removeSuffix(notificationProperties.getPlatformBaseUrl(), "/");
        if (StrUtil.isNotBlank(link)) {
            markdown += "\n\n[打开运营平台](" + link + ")";
        }
        JSONObject apiResult = dingTalkWorkNotifyClient.sendMarkdown(user.getDingUserId(), title, markdown);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("channel", "work_notify");
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("dingUserId", user.getDingUserId());
        data.put("sent", true);
        data.put("errcode", apiResult.getInt("errcode", 0));
        data.put("errmsg", apiResult.getStr("errmsg", "ok"));
        JSONObject result = apiResult.getJSONObject("result");
        if (result != null && result.get("task_id") != null) {
            data.put("taskId", result.get("task_id"));
        } else if (apiResult.get("task_id") != null) {
            data.put("taskId", apiResult.get("task_id"));
        }
        data.put("requestId", apiResult.getStr("request_id"));
        data.put("raw", apiResult);
        return CommonResult.success(data);
    }

    @PostMapping("/test-send")
    public CommonResult<Map<String, Object>> testSend(@Valid @RequestBody DingTalkTestSendReq req) {
        if (dingTalkWorkNotifyClient.isEnabled()) {
            return testWorkSend(req);
        }
        String skipReason = dingTalkRobotClient.getSkipReason();
        if (skipReason != null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), skipReason);
        }
        SysUserDO user = resolveTargetUser(req.getUserId());
        String title = "【运营平台】通知测试";
        String markdown = "这是一条 dev 环境**机器人降级**测试消息。\n\n"
                + "接收人：" + user.getNickname()
                + "（userId=" + user.getId() + "）\n\n@"
                + user.getDingUserId();
        List<String> atUserIds = new ArrayList<>();
        atUserIds.add(user.getDingUserId());
        dingTalkRobotClient.sendMarkdown(title, markdown, atUserIds);
        return CommonResult.success(Map.of(
                "channel", "robot_webhook",
                "userId", user.getId(),
                "nickname", user.getNickname(),
                "dingUserId", user.getDingUserId(),
                "sent", true));
    }

    private SysUserDO resolveTargetUser(Long userId) {
        Long tenantId = TenantContextHolder.getTenantId();
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (StrUtil.isBlank(user.getDingUserId())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "用户 " + user.getNickname() + " 无 ding_user_id，请先同步钉钉用户");
        }
        return user;
    }
}
