package cn.iocoder.yudao.module.oa.service.author;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.OaAuthorExtDO;
import cn.iocoder.yudao.module.oa.dal.mysql.author.OaAuthorExtMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 跨库作者解析（author_user_id 语义，ADR-051 §4.4）。
 */
@Service
@RequiredArgsConstructor
public class AuthorResolveSupport {

    private final OaAuthorExtMapper oaAuthorExtMapper;
    private final MemberAuthorReadService memberAuthorReadService;

    public AuthorUserDO requireAuthorUser(Long authorUserId, Long tenantId) {
        return memberAuthorReadService.requireById(authorUserId, tenantId);
    }

    public void assertAuthorInIpGroup(Long authorUserId, Long ipGroupId, Long tenantId) {
        AuthorUserDO user = requireAuthorUser(authorUserId, tenantId);
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        OaAuthorExtDO ext = oaAuthorExtMapper.selectById(authorUserId);
        if (ipGroupId != null && (ext == null || !Objects.equals(ext.getIpGroupId(), ipGroupId))) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "作者不属于所选 IP 组");
        }
    }

    public Long findFirstAuthorUserId(Long ipGroupId, Long tenantId) {
        OaAuthorExtDO ext = oaAuthorExtMapper.selectOne(new LambdaQueryWrapper<OaAuthorExtDO>()
                .eq(OaAuthorExtDO::getTenantId, tenantId)
                .eq(OaAuthorExtDO::getIpGroupId, ipGroupId)
                .eq(OaAuthorExtDO::getStatus, 1)
                .orderByAsc(OaAuthorExtDO::getAuthorUserId)
                .last("LIMIT 1"));
        return ext != null ? ext.getAuthorUserId() : null;
    }

    public Long getPrimaryMpAccountId(Long authorUserId) {
        OaAuthorExtDO ext = oaAuthorExtMapper.selectById(authorUserId);
        return ext != null ? ext.getPrimaryMpAccountId() : null;
    }

    public String resolveNickname(Long authorUserId) {
        return memberAuthorReadService.resolveNickname(authorUserId);
    }

    /**
     * Active author count for dashboard metrics (member SSOT + ext ip_group filter).
     */
    public long countActiveAuthors(Long tenantId, java.util.Collection<Long> ipGroupIds) {
        boolean scoped = ipGroupIds != null && !ipGroupIds.isEmpty();
        if (scoped) {
            return oaAuthorExtMapper.selectCount(new LambdaQueryWrapper<OaAuthorExtDO>()
                    .eq(OaAuthorExtDO::getTenantId, tenantId)
                    .eq(OaAuthorExtDO::getStatus, 1)
                    .in(OaAuthorExtDO::getIpGroupId, ipGroupIds));
        }
        return memberAuthorReadService.countActiveAuthors(tenantId);
    }
}
