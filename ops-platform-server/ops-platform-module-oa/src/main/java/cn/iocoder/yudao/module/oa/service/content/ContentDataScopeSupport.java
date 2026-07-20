package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.service.ipgroup.IpGroupAccessSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 内容管理读范围：6117 非 admin 仅本人创建；6118 待审队列按审核资格过滤。
 */
@Component
@RequiredArgsConstructor
public class ContentDataScopeSupport {

    private static final String STATUS_PENDING_FIRST_REVIEW = "PENDING_FIRST_REVIEW";
    private static final String STATUS_PENDING_SECOND_REVIEW = "PENDING_SECOND_REVIEW";
    private static final String STATUS_PENDING_FINAL_REVIEW = "PENDING_FINAL_REVIEW";

    private final IpGroupAccessSupport ipGroupAccessSupport;
    private final ContentReviewConfigService contentReviewConfigService;

    public void applyListScope(LambdaQueryWrapper<ProductionContentDO> wrapper, Long tenantId, String status) {
        if (ipGroupAccessSupport.hasUnrestrictedIpGroupAccess()) {
            return;
        }
        Long userId = TenantContextHolder.getUserId();
        if (isReviewQueueStatus(status) && hasReviewListAccess(userId, status)) {
            applyReviewListScope(wrapper, status, userId);
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

    /** 6118：列表 SQL 下推 — 一级 IP 组长仅看自己管辖组内待审内容 */
    private void applyReviewListScope(LambdaQueryWrapper<ProductionContentDO> wrapper, String status, Long userId) {
        if (STATUS_PENDING_FIRST_REVIEW.equals(status)
                && !contentReviewConfigService.hasLevel1FullAccess(userId)) {
            List<Long> ledGroupIds = contentReviewConfigService.listIpGroupIdsLedByUser(userId);
            if (ledGroupIds.isEmpty()) {
                wrapper.eq(ProductionContentDO::getId, -1L);
            } else {
                wrapper.in(ProductionContentDO::getIpGroupId, ledGroupIds);
            }
        }
    }

    private void assertReviewReadable(ProductionContentDO content, Long userId) {
        String stage = resolveReviewStage(content.getStatus());
        if (stage == null || !contentReviewConfigService.canReview(userId, content, stage)) {
            throw new ServiceException(OaErrorCodes.FORBIDDEN);
        }
    }

    /** 6117 P1：非 admin 仅 creator_user_id = 本人（membership userIds 桥接） */
    private void applyOwnContentFilter(LambdaQueryWrapper<ProductionContentDO> wrapper, Long tenantId) {
        Set<Long> userIds = ipGroupAccessSupport.resolveMembershipUserIds(tenantId);
        if (userIds.isEmpty()) {
            wrapper.eq(ProductionContentDO::getId, -1L);
            return;
        }
        wrapper.in(ProductionContentDO::getCreatorUserId, userIds);
    }

    private boolean isOwnContent(ProductionContentDO content, Long tenantId) {
        Set<Long> userIds = ipGroupAccessSupport.resolveMembershipUserIds(tenantId);
        return content.getCreatorUserId() != null && userIds.contains(content.getCreatorUserId());
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

    private String resolveReviewStage(String status) {
        if (STATUS_PENDING_FIRST_REVIEW.equals(status)) {
            return "FIRST_REVIEW";
        }
        if (STATUS_PENDING_SECOND_REVIEW.equals(status)) {
            return "SECOND_REVIEW";
        }
        if (STATUS_PENDING_FINAL_REVIEW.equals(status)) {
            return "FINAL_REVIEW";
        }
        return null;
    }
}
