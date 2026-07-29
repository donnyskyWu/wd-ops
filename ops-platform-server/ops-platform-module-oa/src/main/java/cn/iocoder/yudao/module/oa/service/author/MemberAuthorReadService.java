package cn.iocoder.yudao.module.oa.service.author;

import cn.iocoder.yudao.framework.common.biz.member.author.AuthorApi;
import cn.iocoder.yudao.framework.common.biz.member.author.dto.AuthorSimpleRespDTO;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.author.AuthorUserDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * G-MEM-02 cutover: author read via {@link AuthorApi} Feign（无 member {@code @DS} 回退）。
 */
@Service
@RequiredArgsConstructor
public class MemberAuthorReadService {

    private final AuthorApi authorApi;

    public AuthorUserDO requireById(Long authorUserId, Long tenantId) {
        AuthorUserDO user = loadViaFeign(authorUserId);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return user;
    }

    public String resolveNickname(Long authorUserId) {
        AuthorUserDO user = loadViaFeign(authorUserId);
        return user != null ? user.getNickname() : null;
    }

    public Map<Long, String> loadNicknames(Collection<Long> authorUserIds) {
        if (authorUserIds == null || authorUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return loadByIds(authorUserIds).values().stream()
                .collect(Collectors.toMap(AuthorUserDO::getId, AuthorUserDO::getNickname, (a, b) -> a));
    }

    public Map<Long, AuthorUserDO> loadByIds(Collection<Long> authorUserIds) {
        if (authorUserIds == null || authorUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AuthorSimpleRespDTO> authors = loadBatchViaFeign(authorUserIds);
        return authors.stream()
                .map(this::toAuthorUserDO)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AuthorUserDO::getId, u -> u, (a, b) -> a));
    }

    public Set<Long> listAuthorUserIdsByLinkedUserIds(Collection<Long> userIds, Long tenantId) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> authorIds = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            AuthorUserDO byId = loadViaFeign(userId);
            if (byId != null && Objects.equals(byId.getTenantId(), tenantId)) {
                authorIds.add(byId.getId());
            }
            AuthorUserDO byUserId = loadByLinkedUserIdViaFeign(userId);
            if (byUserId != null && Objects.equals(byUserId.getTenantId(), tenantId)) {
                authorIds.add(byUserId.getId());
            }
        }
        return authorIds;
    }

    private AuthorUserDO loadViaFeign(Long authorUserId) {
        if (authorApi == null || authorUserId == null) {
            throw rpcUnavailable();
        }
        try {
            CommonResult<AuthorSimpleRespDTO> result = authorApi.getAuthor(authorUserId);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return null;
            }
            return toAuthorUserDO(result.getData());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    private AuthorUserDO loadByLinkedUserIdViaFeign(Long userId) {
        if (authorApi == null || userId == null) {
            return null;
        }
        try {
            CommonResult<AuthorSimpleRespDTO> result = authorApi.getAuthorByUserId(userId);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return null;
            }
            return toAuthorUserDO(result.getData());
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<AuthorSimpleRespDTO> loadBatchViaFeign(Collection<Long> authorUserIds) {
        if (authorApi == null) {
            throw rpcUnavailable();
        }
        List<Long> ids = authorUserIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return List.of();
        }
        try {
            CommonResult<List<AuthorSimpleRespDTO>> result = authorApi.getAuthors(ids);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                throw rpcUnavailable();
            }
            return result.getData();
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    private AuthorUserDO toAuthorUserDO(AuthorSimpleRespDTO dto) {
        if (dto == null || dto.getId() == null) {
            return null;
        }
        AuthorUserDO user = new AuthorUserDO();
        user.setId(dto.getId());
        user.setUserId(dto.getUserId());
        user.setNickname(dto.getNickname());
        user.setAvatarUrl(dto.getAvatarUrl());
        user.setStatus(dto.getStatus());
        user.setAuthorLevel(dto.getAuthorLevel());
        user.setTenantId(dto.getTenantId());
        return user;
    }

    private static ServiceException rpcUnavailable() {
        return new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                "作者读服务不可用，请确认 member-server 已启动");
    }
}
