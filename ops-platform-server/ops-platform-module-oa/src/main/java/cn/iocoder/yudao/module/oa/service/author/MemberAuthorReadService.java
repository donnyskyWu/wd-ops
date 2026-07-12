package cn.iocoder.yudao.module.oa.service.author;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorUserDO;
import cn.iocoder.yudao.module.oa.dal.mysql.author.AuthorUserMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Member DB reads outside master {@code @Transactional} so {@code @DS("member")} routing works (ADR-051).
 */
@Service
@RequiredArgsConstructor
public class MemberAuthorReadService {

    private final AuthorUserMapper authorUserMapper;

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthorUserDO requireById(Long authorUserId, Long tenantId) {
        AuthorUserDO user = authorUserMapper.selectById(authorUserId);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return user;
    }

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String resolveNickname(Long authorUserId) {
        AuthorUserDO user = authorUserMapper.selectById(authorUserId);
        return user != null ? user.getNickname() : null;
    }

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<Long, String> loadNicknames(Collection<Long> authorUserIds) {
        if (authorUserIds == null || authorUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return authorUserMapper.selectBatchIds(authorUserIds).stream()
                .collect(Collectors.toMap(AuthorUserDO::getId, AuthorUserDO::getNickname, (a, b) -> a));
    }

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Set<Long> listAuthorUserIdsByLinkedUserIds(Collection<Long> userIds, Long tenantId) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptySet();
        }
        return authorUserMapper.selectList(new LambdaQueryWrapper<AuthorUserDO>()
                        .eq(AuthorUserDO::getTenantId, tenantId)
                        .and(w -> w.in(AuthorUserDO::getId, userIds).or().in(AuthorUserDO::getUserId, userIds)))
                .stream()
                .map(AuthorUserDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public long countActiveAuthors(Long tenantId) {
        Long count = authorUserMapper.selectCount(new LambdaQueryWrapper<AuthorUserDO>()
                .eq(AuthorUserDO::getTenantId, tenantId)
                .eq(AuthorUserDO::getStatus, 0));
        return count == null ? 0L : count;
    }
}
