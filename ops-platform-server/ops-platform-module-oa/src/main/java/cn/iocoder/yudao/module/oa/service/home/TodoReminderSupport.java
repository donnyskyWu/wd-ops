package cn.iocoder.yudao.module.oa.service.home;

import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 待办提醒统一收口：业务状态变更后刷新首页缓存，并关闭已处理的审核提醒消息。
 */
@Service
@RequiredArgsConstructor
public class TodoReminderSupport {

    private final HomeDashboardService homeDashboardService;
    private final NotificationService notificationService;

    public void onContentReviewSubmitted(ProductionContentDO content) {
        refreshTenant(content);
    }

    public void onContentReviewApproved(ProductionContentDO content, String completedStage, boolean allStagesDone) {
        refreshTenant(content);
        if (content == null) {
            return;
        }
        if (allStagesDone) {
            notificationService.dismissAllContentReviewReminders(content);
        } else {
            notificationService.dismissContentReviewReminders(content, completedStage);
        }
    }

    public void onContentReviewRejected(ProductionContentDO content) {
        refreshTenant(content);
        if (content != null) {
            notificationService.dismissAllContentReviewReminders(content);
        }
    }

    public void onSopReviewStateChanged(Long tenantId, Long taskId) {
        if (tenantId != null) {
            homeDashboardService.refreshTenant(tenantId);
        }
        if (tenantId != null && taskId != null) {
            notificationService.dismissSopReviewReminders(tenantId, taskId);
        }
    }

    public void onImportReviewStateChanged(Long tenantId) {
        if (tenantId != null) {
            homeDashboardService.refreshTenant(tenantId);
        }
    }

    private void refreshTenant(ProductionContentDO content) {
        if (content != null && content.getTenantId() != null) {
            homeDashboardService.refreshTenant(content.getTenantId());
        }
    }
}
