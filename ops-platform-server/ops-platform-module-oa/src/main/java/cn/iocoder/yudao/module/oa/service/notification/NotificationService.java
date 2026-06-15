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

    void notifyExternalWorkAlert(Long tenantId, ExternalWorkDO work, NotificationEventType eventType);

    void notifyInternalWorkHit(Long tenantId, ContentDO work);

    void notifyFollowerAlert(Long tenantId, AccountDO account, long followerCount,
                             LocalDate statDate, NotificationEventType eventType);
}
