package cn.iocoder.yudao.module.oa.service.notification;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.monitor.ExternalWorkDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopNodeDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.system.SysMessageDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.system.SysNotificationEventDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopNodeMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.SysMessageMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.SysNotificationEventMapper;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkRobotClient;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkWorkNotifyClient;
import cn.iocoder.yudao.module.oa.framework.notification.NotificationProperties;
import cn.iocoder.yudao.module.oa.service.content.ContentReviewConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String MSG_PREFIX = "【运营平台】";

    private final SysMessageMapper sysMessageMapper;
    private final SysNotificationEventMapper notificationEventMapper;
    private final SysUserMapper sysUserMapper;
    private final IpGroupMapper ipGroupMapper;
    private final TaskMapper taskMapper;
    private final SopNodeMapper sopNodeMapper;
    private final DingTalkRobotClient dingTalkRobotClient;
    private final DingTalkWorkNotifyClient dingTalkWorkNotifyClient;
    private final ContentReviewConfigService contentReviewConfigService;
    private final NotificationProperties notificationProperties;
    private final AccountMapper accountMapper;

    @Override
    public void notifyPlanStarted(Long tenantId, Long planId, String planName) {
        List<TaskDO> tasks = taskMapper.selectList(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getTenantId, tenantId)
                .eq(TaskDO::getPlanId, planId)
                .eq(TaskDO::getStatus, "PENDING"));
        for (TaskDO task : tasks) {
            if (task.getAssigneeId() == null) {
                continue;
            }
            String bizKey = "task:" + task.getId() + ":PENDING";
            if (!tryClaimEvent(tenantId, NotificationEventType.TASK_PENDING, bizKey, task.getAssigneeId())) {
                continue;
            }
            String nodeName = resolveNodeName(task.getNodeId());
            String title = MSG_PREFIX + "新任务待执行";
            String content = String.format(
                    "内容计划「%s」已启动。\n\n您的任务「%s」已进入待执行状态，请尽快处理。",
                    safe(planName), safe(nodeName));
            String link = platformLink("/task/my");
            send(tenantId, task.getAssigneeId(), NotificationEventType.TASK_PENDING, "BUSINESS",
                    title, content, link, bizKey);
        }
    }

    @Override
    public void notifyContentReviewSubmit(ProductionContentDO content, String reviewStage) {
        if (content == null || StrUtil.isBlank(reviewStage)) {
            return;
        }
        List<Long> reviewerIds = contentReviewConfigService.listEligibleReviewerUserIds(content, reviewStage);
        String stageLabel = stageLabel(reviewStage);
        String title = MSG_PREFIX + "内容待审核";
        String contentText = String.format(
                "内容「%s」已提交%s。\n\n请及时登录平台完成审核。",
                safe(content.getTitle()), stageLabel);
        String link = platformLink("/content/review");
        String bizKeyPrefix = "content:" + content.getId() + ":review:" + reviewStage + ":";
        for (Long reviewerId : reviewerIds) {
            String bizKey = bizKeyPrefix + reviewerId;
            if (!tryClaimEvent(content.getTenantId(), NotificationEventType.CONTENT_REVIEW_SUBMIT,
                    bizKey, reviewerId)) {
                continue;
            }
            send(content.getTenantId(), reviewerId, NotificationEventType.CONTENT_REVIEW_SUBMIT, "BUSINESS",
                    title, contentText, link, bizKey);
        }
    }

    @Override
    public void notifyContentReviewApproved(ProductionContentDO content) {
        if (content == null || content.getCreatorUserId() == null) {
            return;
        }
        String bizKey = "content:" + content.getId() + ":approved";
        if (!tryClaimEvent(content.getTenantId(), NotificationEventType.CONTENT_REVIEW_APPROVED,
                bizKey, content.getCreatorUserId())) {
            return;
        }
        String title = MSG_PREFIX + "内容审核通过";
        String contentText = String.format(
                "您创建的内容「%s」已通过全部审核。\n\n可进入发布流程进行后续操作。",
                safe(content.getTitle()));
        String link = platformLink("/content/production");
        send(content.getTenantId(), content.getCreatorUserId(), NotificationEventType.CONTENT_REVIEW_APPROVED,
                "BUSINESS", title, contentText, link, bizKey);
    }

    @Override
    public void notifyExternalWorkAlert(Long tenantId, ExternalWorkDO work, NotificationEventType eventType) {
        if (work == null || work.getIpGroupId() == null) {
            return;
        }
        Long leaderId = resolveIpGroupLeader(tenantId, work.getIpGroupId());
        if (leaderId == null) {
            return;
        }
        String bizKey = "external-work:" + work.getId() + ":" + eventType.name();
        if (!tryClaimEvent(tenantId, eventType, bizKey, leaderId)) {
            return;
        }
        String title = eventType == NotificationEventType.WORK_HIT
                ? MSG_PREFIX + "爆款作品预警"
                : MSG_PREFIX + "低分作品预警";
        String detail = eventType == NotificationEventType.WORK_HIT
                ? String.format("外部作品「%s」播放量达 %s，已触发爆款预警，请关注。",
                safe(work.getTitle()), formatCount(work.getPlayCount()))
                : String.format("外部作品「%s」完播率 %s 低于阈值，已触发低分预警，请关注。",
                safe(work.getTitle()), formatRate(work.getCompletionRate()));
        String link = platformLink("/monitor/external");
        send(tenantId, leaderId, eventType, "ALERT", title, detail, link, bizKey);
    }

    @Override
    public void notifyInternalWorkHit(Long tenantId, ContentDO work) {
        if (work == null || work.getAccountId() == null) {
            return;
        }
        AccountDO account = accountMapper.selectById(work.getAccountId());
        if (account == null || account.getIpGroupId() == null) {
            return;
        }
        Long leaderId = resolveIpGroupLeader(tenantId, account.getIpGroupId());
        if (leaderId == null) {
            return;
        }
        String bizKey = "internal-work:" + work.getId() + ":HIT";
        if (!tryClaimEvent(tenantId, NotificationEventType.WORK_HIT, bizKey, leaderId)) {
            return;
        }
        String title = MSG_PREFIX + "爆款作品预警";
        String accountLabel = StrUtil.isNotBlank(account.getAccountName())
                ? account.getAccountName() : "-";
        String detail = String.format("账号「%s」下的作品「%s」已标记为爆款，请关注。",
                safe(accountLabel), safe(work.getTitle()));
        String link = platformLink("/operations/content-analysis");
        send(tenantId, leaderId, NotificationEventType.WORK_HIT, "ALERT", title, detail, link, bizKey);
    }

    @Override
    public void notifyFollowerAlert(Long tenantId, AccountDO account, long followerCount,
                                    LocalDate statDate, NotificationEventType eventType) {
        if (account == null || account.getIpGroupId() == null || statDate == null) {
            return;
        }
        Long leaderId = resolveIpGroupLeader(tenantId, account.getIpGroupId());
        if (leaderId == null) {
            return;
        }
        String bizKey = "account:" + account.getId() + ":" + eventType.name() + ":" + statDate;
        if (!tryClaimEvent(tenantId, eventType, bizKey, leaderId)) {
            return;
        }
        String title = eventType == NotificationEventType.ACCOUNT_HIGH_FANS
                ? MSG_PREFIX + "高粉账号预警"
                : MSG_PREFIX + "低粉账号预警";
        String trend = eventType == NotificationEventType.ACCOUNT_HIGH_FANS ? "偏高" : "偏低";
        String detail = String.format(
                "账号「%s」粉丝数 %s（统计日 %s），粉丝规模%s，请关注。",
                safe(account.getAccountName()), formatCount(followerCount), statDate, trend);
        String link = platformLink("/monitor/follower");
        send(tenantId, leaderId, eventType, "ALERT", title, detail, link, bizKey);
    }

    private void send(Long tenantId, Long userId, NotificationEventType eventType, String category,
                      String title, String plainContent, String link, String bizKey) {
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            log.warn("Skip notification, user not in tenant: userId={} tenantId={}", userId, tenantId);
            return;
        }
        String receiver = String.valueOf(userId);
        String inAppContent = plainContent;
        if (StrUtil.isNotBlank(link)) {
            inAppContent = plainContent + "\n链接: " + link;
        }
        saveInAppMessage(tenantId, receiver, category, title, inAppContent);
        pushDingTalk(user, title, plainContent, link);
        log.info("Notification sent: event={} bizKey={} userId={} channel=work_notify|robot_fallback",
                eventType, bizKey, userId);
    }

    private void saveInAppMessage(Long tenantId, String receiver, String category,
                                  String title, String content) {
        SysMessageDO row = new SysMessageDO();
        row.setTenantId(tenantId);
        row.setTitle(title);
        row.setCategory(category);
        row.setChannel("IN_APP,DINGTALK");
        row.setReceiver(receiver);
        row.setContent(content);
        row.setStatus("SENT");
        row.setSendTime(LocalDateTime.now());
        row.setCreator("system");
        row.setUpdater("system");
        row.setCreateTime(LocalDateTime.now());
        row.setUpdateTime(LocalDateTime.now());
        sysMessageMapper.insert(row);
    }

    /**
     * 优先钉钉工作通知（asyncsend_v2，点对点）；失败或未配置时降级群机器人 Webhook。
     */
    private void pushDingTalk(SysUserDO user, String title, String plainContent, String link) {
        if (tryPushWorkNotification(user, title, plainContent, link)) {
            return;
        }
        pushRobotWebhookFallback(user, title, plainContent, link);
    }

    private boolean tryPushWorkNotification(SysUserDO user, String title, String plainContent, String link) {
        if (!dingTalkWorkNotifyClient.isEnabled()) {
            return false;
        }
        if (StrUtil.isBlank(user.getDingUserId())) {
            log.warn("DingTalk work notify skipped for user {} ({}): no ding_user_id",
                    user.getId(), user.getNickname());
            return false;
        }
        try {
            String markdown = buildMarkdownBody(plainContent, link);
            dingTalkWorkNotifyClient.sendMarkdown(user.getDingUserId(), title, markdown);
            log.debug("DingTalk work notify sent to user {} ({})", user.getId(), user.getNickname());
            return true;
        } catch (Exception e) {
            log.warn("DingTalk work notify failed for user {} ({}), fallback to robot: {}",
                    user.getId(), user.getNickname(), e.getMessage());
            return false;
        }
    }

    /** 可选降级：群机器人 Webhook，工作通知不可用时才尝试。 */
    private void pushRobotWebhookFallback(SysUserDO user, String title, String plainContent, String link) {
        String skipReason = dingTalkRobotClient.getSkipReason();
        if (skipReason != null) {
            log.debug("DingTalk robot fallback skipped for user {} ({}): {}",
                    user.getId(), user.getNickname(), skipReason);
            return;
        }
        try {
            String markdown = buildMarkdownBody(plainContent, link);
            List<String> atUserIds = new ArrayList<>();
            if (StrUtil.isNotBlank(user.getDingUserId())) {
                markdown = markdown + "\n\n@" + user.getDingUserId();
                atUserIds.add(user.getDingUserId());
            }
            dingTalkRobotClient.sendMarkdown(title, markdown, atUserIds);
            log.debug("DingTalk robot fallback sent for user {} ({})", user.getId(), user.getNickname());
        } catch (Exception e) {
            log.warn("DingTalk robot fallback failed for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private String buildMarkdownBody(String plainContent, String link) {
        StringBuilder markdown = new StringBuilder(plainContent.replace("\n", "\n\n"));
        if (StrUtil.isNotBlank(link)) {
            markdown.append("\n\n[查看详情](").append(link).append(")");
        }
        return markdown.toString();
    }

    private boolean tryClaimEvent(Long tenantId, NotificationEventType eventType,
                                    String bizKey, Long recipientUserId) {
        SysNotificationEventDO row = new SysNotificationEventDO();
        row.setTenantId(tenantId);
        row.setEventType(eventType.name());
        row.setBizKey(bizKey);
        row.setRecipientUserId(recipientUserId);
        row.setCreateTime(LocalDateTime.now());
        try {
            notificationEventMapper.insert(row);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    private Long resolveIpGroupLeader(Long tenantId, Long ipGroupId) {
        IpGroupDO group = ipGroupMapper.selectById(ipGroupId);
        if (group == null || !tenantId.equals(group.getTenantId()) || group.getLeaderUserId() == null) {
            return null;
        }
        return group.getLeaderUserId();
    }

    private String resolveNodeName(Long nodeId) {
        if (nodeId == null) {
            return "任务";
        }
        SopNodeDO node = sopNodeMapper.selectById(nodeId);
        return node != null && StrUtil.isNotBlank(node.getNodeName()) ? node.getNodeName() : "任务";
    }

    private String stageLabel(String stage) {
        return switch (stage) {
            case "FIRST_REVIEW" -> "一级审核";
            case "SECOND_REVIEW" -> "二级审核";
            case "FINAL_REVIEW" -> "终审";
            default -> "审核";
        };
    }

    private String platformLink(String path) {
        String base = StrUtil.removeSuffix(notificationProperties.getPlatformBaseUrl(), "/");
        if (StrUtil.isBlank(base)) {
            return "";
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return base + normalizedPath;
    }

    private String safe(String value) {
        return StrUtil.blankToDefault(value, "-");
    }

    private String formatCount(Long value) {
        return value == null ? "-" : String.format("%,d", value);
    }

    private String formatCount(long value) {
        return String.format("%,d", value);
    }

    private String formatRate(BigDecimal rate) {
        if (rate == null) {
            return "-";
        }
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }

}
