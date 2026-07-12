package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import cn.iocoder.yudao.module.oa.service.author.MemberAuthorReadService;
import cn.iocoder.yudao.module.oa.service.ipgroup.IpGroupAccessSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 内容管理读范围：非 ALL 数据权限用户仅可见本人创建或任务指派给自己的内容。
 * 审核队列（待审状态 + 具备审核列表权限）不受此限制。
 */
@Component
@RequiredArgsConstructor
public class ContentDataScopeSupport {

    private static final String STATUS_PENDING_FIRST_REVIEW = "PENDING_FIRST_REVIEW";
    private static final String STATUS_PENDING_SECOND_REVIEW = "PENDING_SECOND_REVIEW";
    private static final String STATUS_PENDING_FINAL_REVIEW = "PENDING_FINAL_REVIEW";

    private final IpGroupAccessSupport ipGroupAccessSupport;
    private final TaskMapper taskMapper;
    private final MemberAuthorReadService memberAuthorReadService;
    private final ContentReviewConfigService contentReviewConfigService;

    public void applyListScope(LambdaQueryWrapper<ProductionContentDO> wrapper, Long tenantId, String status) {
        if (ipGroupAccessSupport.hasUnrestrictedIpGroupAccess()) {
            return;
        }
        Long userId = TenantContextHolder.getUserId();
        if (isReviewQueueStatus(status) && hasReviewListAccess(userId, status)) {
            return;
        }
        applyOwnContentFilter(wrapper, tenantId);
    }

    public void assertReadable(ProductionContentDO content, Long tenantId) {
        if (ipGroupAccessSupport.hasUnrestrictedIpGroupAccess()) {
            return;
        }
        Long userId = TenantContextHolder.getUserId();
        if (isReviewQueueStatus(content.getStatus()) && hasReviewListAccess(userId, content.getStatus())) {
            assertReviewReadable(content, userId);
            return;
        }
        if (!isOwnContent(content, tenantId)) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
    }

    private void assertReviewReadable(ProductionContentDO content, Long userId) {
        if (!STATUS_PENDING_FIRST_REVIEW.equals(content.getStatus())
                || contentReviewConfigService.hasLevel1FullAccess(userId)) {
            return;
        }
        if (content.getIpGroupId() == null) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
        List<Long> ledGroupIds = contentReviewConfigService.listIpGroupIdsLedByUser(userId);
        if (!ledGroupIds.contains(content.getIpGroupId())) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
    }

    private void applyOwnContentFilter(LambdaQueryWrapper<ProductionContentDO> wrapper, Long tenantId) {
        Set<Long> userIds = ipGroupAccessSupport.resolveMembershipUserIds(tenantId);
        if (userIds.isEmpty()) {
            wrapper.eq(ProductionContentDO::getId, -1L);
            return;
        }
        List<Long> assignedTaskIds = taskMapper.selectList(new LambdaQueryWrapper<TaskDO>()
                        .eq(TaskDO::getTenantId, tenantId)
                        .in(TaskDO::getAssigneeId, userIds)
                        .select(TaskDO::getId))
                .stream()
                .map(TaskDO::getId)
                .toList();
        Set<Long> authorIds = memberAuthorReadService.listAuthorUserIdsByLinkedUserIds(userIds, tenantId);
        wrapper.and(w -> {
            w.in(ProductionContentDO::getCreatorUserId, userIds);
            if (!authorIds.isEmpty()) {
                w.or().in(ProductionContentDO::getAuthorId, authorIds);
            }
            if (!assignedTaskIds.isEmpty()) {
                w.or().in(ProductionContentDO::getTaskId, assignedTaskIds);
            }
        });
    }

    private boolean isOwnContent(ProductionContentDO content, Long tenantId) {
        Set<Long> userIds = ipGroupAccessSupport.resolveMembershipUserIds(tenantId);
        if (userIds.isEmpty()) {
            return false;
        }
        if (content.getCreatorUserId() != null && userIds.contains(content.getCreatorUserId())) {
            return true;
        }
        Set<Long> authorIds = memberAuthorReadService.listAuthorUserIdsByLinkedUserIds(userIds, tenantId);
        if (content.getAuthorId() != null && authorIds.contains(content.getAuthorId())) {
            return true;
        }
        if (content.getTaskId() == null) {
            return false;
        }
        TaskDO task = taskMapper.selectById(content.getTaskId());
        return task != null
                && Objects.equals(task.getTenantId(), tenantId)
                && task.getAssigneeId() != null
                && userIds.contains(task.getAssigneeId());
    }

    private boolean isReviewQueueStatus(String status) {
        return STATUS_PENDING_FIRST_REVIEW.equals(status)
                || STATUS_PENDING_SECOND_REVIEW.equals(status)
                || STATUS_PENDING_FINAL_REVIEW.equals(status);
    }

    private boolean hasReviewListAccess(Long userId, String status) {
        if (STATUS_PENDING_FIRST_REVIEW.equals(status)) {
            return contentReviewConfigService.hasLevel1ListAccess(userId);
        }
        if (STATUS_PENDING_SECOND_REVIEW.equals(status) || STATUS_PENDING_FINAL_REVIEW.equals(status)) {
            return contentReviewConfigService.hasLevel2ListAccess(userId);
        }
        return false;
    }
}
