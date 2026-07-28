package cn.iocoder.yudao.module.oa.service.notification;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.monitor.ExternalWorkDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;

import java.time.LocalDate;

public interface NotificationService {

    void notifyPlanStarted(Long tenantId, Long planId, String planName);

    void notifyContentReviewSubmit(ProductionContentDO content, String reviewStage);

    void notifyContentReviewApproved(ProductionContentDO content);

    /** 关闭指定审核阶段的内容待审提醒（站内信）。 */
    void dismissContentReviewReminders(ProductionContentDO content, String reviewStage);

    /** 关闭该内容全部待审提醒（驳回或终审通过）。 */
    void dismissAllContentReviewReminders(ProductionContentDO content);

    /** 关闭 SOP 任务待审提醒（当前无独立通知，预留统一入口）。 */
    void dismissSopReviewReminders(Long tenantId, Long taskId);

    void notifyExternalWorkAlert(Long tenantId, ExternalWorkDO work, NotificationEventType eventType);

    void notifyInternalWorkHit(Long tenantId, ContentDO work);

    void notifyFollowerAlert(Long tenantId, AccountDO account, long followerCount,
                             LocalDate statDate, NotificationEventType eventType);
}
