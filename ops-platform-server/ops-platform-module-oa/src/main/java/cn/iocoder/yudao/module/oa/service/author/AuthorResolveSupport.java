package cn.iocoder.yudao.module.oa.service.author;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.OaAuthorExtDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupAnchorRelDO;
import cn.iocoder.yudao.module.oa.dal.mysql.author.OaAuthorExtMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupAnchorRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 跨库作者解析（author_user_id 语义，ADR-051 §4.4）。
 */
@Service
@RequiredArgsConstructor
public class AuthorResolveSupport {

    private final OaAuthorExtMapper oaAuthorExtMapper;
    private final IpGroupAnchorRelMapper ipGroupAnchorRelMapper;
    private final MemberAuthorReadService memberAuthorReadService;

    public AuthorUserDO requireAuthorUser(Long authorUserId, Long tenantId) {
        return memberAuthorReadService.requireById(authorUserId, tenantId);
    }

    public void assertAuthorInIpGroup(Long authorUserId, Long ipGroupId, Long tenantId) {
        AuthorUserDO user = requireAuthorUser(authorUserId, tenantId);
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        if (ipGroupId != null && !isAuthorBoundToIpGroup(authorUserId, ipGroupId, tenantId)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "作者不属于所选 IP 组");
        }
    }

    public Long findFirstAuthorUserId(Long ipGroupId, Long tenantId) {
        List<IpGroupAnchorRelDO> rels = ipGroupAnchorRelMapper.selectList(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                .eq(IpGroupAnchorRelDO::getTenantId, tenantId)
                .eq(IpGroupAnchorRelDO::getIpGroupId, ipGroupId)
                .orderByAsc(IpGroupAnchorRelDO::getId));
        for (IpGroupAnchorRelDO rel : rels) {
            AuthorUserDO author = memberAuthorReadService.loadByIds(List.of(rel.getAnchorUserId()))
                    .get(rel.getAnchorUserId());
            if (author != null && Objects.equals(author.getTenantId(), tenantId) && isActiveAuthor(author)) {
                return rel.getAnchorUserId();
            }
        }
        return null;
    }

    /**
     * IP 组下可选作者 SSOT：{@code oa_ip_group_anchor_rel}（与 IP 组管理「关联作者」一致）。
     */
    public boolean isAuthorBoundToIpGroup(Long authorUserId, Long ipGroupId, Long tenantId) {
        if (authorUserId == null || ipGroupId == null) {
            return false;
        }
        Long count = ipGroupAnchorRelMapper.selectCount(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                .eq(IpGroupAnchorRelDO::getTenantId, tenantId)
                .eq(IpGroupAnchorRelDO::getIpGroupId, ipGroupId)
                .eq(IpGroupAnchorRelDO::getAnchorUserId, authorUserId));
        return count != null && count > 0;
    }

    private boolean isActiveAuthor(AuthorUserDO author) {
        return author.getStatus() == null || author.getStatus() == 0;
    }

    public Long getPrimaryMpAccountId(Long authorUserId) {
        OaAuthorExtDO ext = oaAuthorExtMapper.selectById(authorUserId);
        return ext != null ? ext.getPrimaryMpAccountId() : null;
    }

    public String resolveNickname(Long authorUserId) {
        return memberAuthorReadService.resolveNickname(authorUserId);
    }

    /**
     * 作者展示用 IP 组：取 {@code oa_ip_group_anchor_rel} 中最早绑定的一条（ADR-055）。
     */
    public Long resolveDisplayIpGroupId(Long authorUserId, Long tenantId) {
        if (authorUserId == null) {
            return null;
        }
        IpGroupAnchorRelDO rel = ipGroupAnchorRelMapper.selectOne(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                .eq(IpGroupAnchorRelDO::getTenantId, tenantId)
                .eq(IpGroupAnchorRelDO::getAnchorUserId, authorUserId)
                .orderByAsc(IpGroupAnchorRelDO::getId)
                .last("LIMIT 1"));
        return rel != null ? rel.getIpGroupId() : null;
    }

    /**
     * 批量解析作者展示 IP 组（每个作者取最早 anchor_rel）。
     */
    public Map<Long, Long> loadDisplayIpGroupIdByAuthor(Long tenantId, Collection<Long> authorUserIds) {
        if (authorUserIds == null || authorUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<IpGroupAnchorRelDO> rels = ipGroupAnchorRelMapper.selectList(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                .eq(IpGroupAnchorRelDO::getTenantId, tenantId)
                .in(IpGroupAnchorRelDO::getAnchorUserId, authorUserIds)
                .orderByAsc(IpGroupAnchorRelDO::getId));
        Map<Long, Long> map = new LinkedHashMap<>();
        for (IpGroupAnchorRelDO rel : rels) {
            map.putIfAbsent(rel.getAnchorUserId(), rel.getIpGroupId());
        }
        return map;
    }

    /**
     * Active author count for dashboard metrics (ADR-055: anchor_rel SSOT when scoped by IP group).
     */
    public long countActiveAuthors(Long tenantId, Collection<Long> ipGroupIds) {
        boolean scoped = ipGroupIds != null && !ipGroupIds.isEmpty();
        if (!scoped) {
            // G-MEM-02 阻塞：无 tenant 级 count RPC；以 anchor_rel 绑定作者数为代理指标
            List<IpGroupAnchorRelDO> rels = ipGroupAnchorRelMapper.selectList(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                    .eq(IpGroupAnchorRelDO::getTenantId, tenantId));
            Set<Long> authorUserIds = rels.stream()
                    .map(IpGroupAnchorRelDO::getAnchorUserId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (authorUserIds.isEmpty()) {
                return 0L;
            }
            return memberAuthorReadService.loadByIds(authorUserIds).values().stream()
                    .filter(this::isActiveAuthor)
                    .count();
        }
        List<IpGroupAnchorRelDO> rels = ipGroupAnchorRelMapper.selectList(new LambdaQueryWrapper<IpGroupAnchorRelDO>()
                .eq(IpGroupAnchorRelDO::getTenantId, tenantId)
                .in(IpGroupAnchorRelDO::getIpGroupId, ipGroupIds));
        Set<Long> authorUserIds = rels.stream()
                .map(IpGroupAnchorRelDO::getAnchorUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (authorUserIds.isEmpty()) {
            return 0L;
        }
        return memberAuthorReadService.loadByIds(authorUserIds).values().stream()
                .filter(this::isActiveAuthor)
                .count();
    }
}
