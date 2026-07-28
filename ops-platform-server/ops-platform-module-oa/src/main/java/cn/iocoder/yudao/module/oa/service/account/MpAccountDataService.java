package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.biz.mp.user.MpAccountInfoApi;
import cn.iocoder.yudao.framework.common.biz.mp.user.dto.MpAccountDTO;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpAccountDO;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * G-MP-01 cutover: Feign-only via {@link MpAccountInfoApi}（无 mp {@code @DS} 回退）。
 * 限制：wrapper 含 SQL {@code IN} 时不支持 Feign 分页，调用方须改用 id 列表 Feign get 或简化查询。
 */
@Service
@RequiredArgsConstructor
public class MpAccountDataService {

    private final MpAccountInfoApi mpAccountInfoApi;

    public Page<MpAccountDO> selectPage(Page<MpAccountDO> page, Wrapper<MpAccountDO> wrapper) {
        Page<MpAccountDO> feignPage = loadPageViaFeign(page, wrapper);
        if (feignPage != null) {
            return feignPage;
        }
        throw unsupportedQuery(wrapper);
    }

    public MpAccountDO selectById(Long id) {
        return loadViaFeign(id);
    }

    public MpAccountDO requireById(Long id, Long tenantId) {
        MpAccountDO mp = selectById(id);
        if (mp == null || !Objects.equals(mp.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return mp;
    }

    public void insert(MpAccountDO mp) {
        insertViaFeign(mp);
    }

    public void updateById(MpAccountDO mp) {
        updateViaFeign(mp);
    }

    public MpAccountDO selectByAppId(Long tenantId, String appId) {
        MpAccountDO mp = loadByAppIdViaFeign(appId, tenantId);
        if (mp == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return mp;
    }

    Page<MpAccountDO> loadPageViaFeign(Page<MpAccountDO> page, Wrapper<MpAccountDO> wrapper) {
        if (mpAccountInfoApi == null || page == null) {
            throw rpcUnavailable();
        }
        PageFeignParams query = extractPageFeignParams(wrapper);
        if (!query.supported()) {
            return null;
        }
        try {
            int pageNo = (int) page.getCurrent();
            int pageSize = (int) page.getSize();
            CommonResult<PageResult<MpAccountDTO>> result = mpAccountInfoApi.getAccountPage(
                    pageNo, pageSize, query.name(), query.appId(), query.authorId(), query.status(), query.type());
            if (result == null || !result.isSuccess() || result.getData() == null) {
                throw rpcUnavailable();
            }
            PageResult<MpAccountDTO> data = result.getData();
            Long tenantId = TenantContextHolder.getTenantId();
            List<MpAccountDO> records = data.getList() == null
                    ? List.of()
                    : data.getList().stream().map(dto -> toDo(dto, tenantId)).collect(Collectors.toList());
            Page<MpAccountDO> feignPage = new Page<>(pageNo, pageSize);
            feignPage.setRecords(records);
            feignPage.setTotal(data.getTotal() == null ? records.size() : data.getTotal());
            return feignPage;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    MpAccountDO loadViaFeign(Long id) {
        if (mpAccountInfoApi == null || id == null) {
            throw rpcUnavailable();
        }
        try {
            CommonResult<MpAccountDTO> result = mpAccountInfoApi.getAccount(id);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
            }
            return toDo(result.getData(), TenantContextHolder.getTenantId());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    MpAccountDO loadByAppIdViaFeign(String appId, Long tenantId) {
        if (mpAccountInfoApi == null || appId == null) {
            throw rpcUnavailable();
        }
        try {
            CommonResult<MpAccountDTO> result = mpAccountInfoApi.getMpAccountByAppId(appId);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return null;
            }
            MpAccountDO mp = toDo(result.getData(), tenantId);
            if (tenantId != null && !Objects.equals(tenantId, mp.getTenantId())) {
                return null;
            }
            return mp;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    void insertViaFeign(MpAccountDO mp) {
        if (mpAccountInfoApi == null) {
            throw rpcUnavailable();
        }
        try {
            CommonResult<Long> result = mpAccountInfoApi.createAccount(toDto(mp));
            if (result == null || !result.isSuccess() || result.getData() == null) {
                throw rpcUnavailable();
            }
            mp.setId(result.getData());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    void updateViaFeign(MpAccountDO mp) {
        if (mpAccountInfoApi == null || mp.getId() == null) {
            throw rpcUnavailable();
        }
        try {
            CommonResult<Boolean> result = mpAccountInfoApi.updateAccount(toDto(mp));
            if (result == null || !result.isSuccess() || !Boolean.TRUE.equals(result.getData())) {
                throw rpcUnavailable();
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    private static final Pattern WRAPPER_PARAM_PATTERN =
            Pattern.compile("#\\{\\s*ew\\.paramNameValuePairs\\.(\\w+)\\s*}", Pattern.CASE_INSENSITIVE);

    record PageFeignParams(String name, String appId, Long authorId, Integer status, Integer type, boolean supported) {
    }

    static PageFeignParams extractPageFeignParams(Wrapper<MpAccountDO> wrapper) {
        if (wrapper == null) {
            return new PageFeignParams(null, null, null, null, null, true);
        }
        String sql = wrapper.getSqlSegment();
        if (sql != null && sql.toUpperCase().contains(" IN ")) {
            return new PageFeignParams(null, null, null, null, null, false);
        }
        String normalizedSql = sql == null ? null : sql.replaceAll("\\s+", " ");
        String name = null;
        String appId = null;
        Long authorId = null;
        Integer status = null;
        Integer type = null;
        Map<String, Object> params = wrapper instanceof AbstractWrapper<?, ?, ?>
                ? ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs()
                : null;
        if (normalizedSql != null && params != null) {
            Matcher matcher = WRAPPER_PARAM_PATTERN.matcher(normalizedSql);
            while (matcher.find()) {
                Object val = params.get(matcher.group(1));
                if (val == null) {
                    continue;
                }
                String before = normalizedSql.substring(0, matcher.start()).toLowerCase();
                if (containsColumnPredicate(before, "name like")) {
                    name = stripLikeWildcards(String.valueOf(val));
                } else if (containsColumnPredicate(before, "app_id =")) {
                    appId = String.valueOf(val);
                } else if (containsColumnPredicate(before, "bind_author_id =")) {
                    authorId = toLong(val);
                } else if (containsColumnPredicate(before, "status =")) {
                    status = toInteger(val);
                } else if (containsColumnPredicate(before, "type =")) {
                    type = toInteger(val);
                }
            }
        }
        return new PageFeignParams(name, appId, authorId, status, type, true);
    }

    private static boolean containsColumnPredicate(String beforeSql, String predicate) {
        String trimmed = beforeSql.trim();
        return trimmed.endsWith(predicate) || trimmed.endsWith(predicate + " ");
    }

    static String stripLikeWildcards(String val) {
        if (val == null) {
            return null;
        }
        if (val.length() >= 2 && val.startsWith("%") && val.endsWith("%")) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    private static Long toLong(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Long l) {
            return l;
        }
        if (val instanceof Number n) {
            return n.longValue();
        }
        return Long.valueOf(String.valueOf(val));
    }

    private static Integer toInteger(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Integer i) {
            return i;
        }
        if (val instanceof Number n) {
            return n.intValue();
        }
        return Integer.valueOf(String.valueOf(val));
    }

    static MpAccountDTO toDto(MpAccountDO mp) {
        MpAccountDTO dto = new MpAccountDTO();
        dto.setId(mp.getId());
        dto.setName(mp.getName());
        dto.setAccount(mp.getAccount());
        dto.setAppId(mp.getAppId());
        dto.setAppSecret(mp.getAppSecret());
        dto.setToken(mp.getToken());
        dto.setRemark(mp.getRemark());
        dto.setStatus(mp.getStatus());
        dto.setBindAuthorId(mp.getBindAuthorId());
        return dto;
    }

    static MpAccountDO toDo(MpAccountDTO dto, Long tenantId) {
        MpAccountDO mp = new MpAccountDO();
        mp.setId(dto.getId());
        mp.setName(dto.getName());
        mp.setAccount(dto.getAccount());
        mp.setAppId(dto.getAppId());
        mp.setAppSecret(dto.getAppSecret());
        mp.setToken(dto.getToken());
        mp.setRemark(dto.getRemark());
        mp.setStatus(dto.getStatus());
        mp.setBindAuthorId(dto.getBindAuthorId());
        mp.setTenantId(tenantId);
        return mp;
    }

    private static ServiceException rpcUnavailable() {
        return new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                "公众号服务不可用，请确认 mp-server 已启动");
    }

    private static ServiceException unsupportedQuery(Wrapper<MpAccountDO> wrapper) {
        String hint = wrapper != null && wrapper.getSqlSegment() != null
                && wrapper.getSqlSegment().toUpperCase().contains(" IN ")
                ? "含 IN 条件的分页暂不支持 Feign，请改用 id 精确查询"
                : "分页查询无法映射至 MpAccountInfoApi";
        return new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), hint);
    }
}
