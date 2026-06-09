package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentStatsVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentAnalysisServiceImpl implements ContentAnalysisService {

    private final ContentMapper contentMapper;
    private final AccountMapper accountMapper;

    @Override
    public PageResult<ContentAnalysisVO> list(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                              Long accountId, String platformType, Boolean isHit,
                                              Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId);
        if (accountIds.isEmpty()) {
            return PageResult.empty();
        }

        LambdaQueryWrapper<ContentDO> wrapper = buildContentWrapper(tenantId, accountIds, startDate, endDate, platformType, isHit);
        Page<ContentDO> page = contentMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public ContentStatsVO stats(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId) {
        Long tenantId = requireTenantId();
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId);
        ContentStatsVO vo = new ContentStatsVO();
        if (accountIds.isEmpty()) {
            return vo;
        }
        List<ContentDO> rows = contentMapper.selectList(
                buildContentWrapper(tenantId, accountIds, startDate, endDate, null, null));
        vo.setTotalCount(rows.size());
        vo.setHitCount((int) rows.stream().filter(c -> c.getIsHit() != null && c.getIsHit() == 1).count());
        long totalRead = rows.stream().mapToLong(c -> c.getReadCount() == null ? 0L : c.getReadCount()).sum();
        vo.setTotalRead(totalRead);
        vo.setAvgRead(rows.isEmpty() ? 0L : totalRead / rows.size());
        return vo;
    }

    private LambdaQueryWrapper<ContentDO> buildContentWrapper(Long tenantId, Set<Long> accountIds,
                                                              LocalDate startDate, LocalDate endDate,
                                                              String platformType, Boolean isHit) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId)
                .in(ContentDO::getAccountId, accountIds)
                .eq(platformType != null, ContentDO::getPlatformType, platformType)
                .eq(isHit != null, ContentDO::getIsHit, Boolean.TRUE.equals(isHit) ? 1 : 0)
                .ge(start != null, ContentDO::getPublishTime, start)
                .le(end != null, ContentDO::getPublishTime, end)
                .orderByDesc(ContentDO::getPublishTime);
        return wrapper;
    }

    private Set<Long> resolveAccountIds(Long tenantId, Long ipGroupId, Long accountId) {
        if (accountId != null) {
            AccountDO account = accountMapper.selectById(accountId);
            if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
                return Collections.emptySet();
            }
            return Set.of(accountId);
        }
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId);
        DataScopeSupport.applyIpGroupScope(wrapper, AccountDO::getIpGroupId);
        return accountMapper.selectList(wrapper).stream().map(AccountDO::getId).collect(Collectors.toSet());
    }

    private ContentAnalysisVO toVO(ContentDO content) {
        ContentAnalysisVO vo = new ContentAnalysisVO();
        vo.setId(content.getId());
        vo.setAccountId(content.getAccountId());
        AccountDO account = accountMapper.selectById(content.getAccountId());
        if (account != null) {
            vo.setAccountName(account.getAccountName());
        }
        vo.setTitle(content.getTitle());
        vo.setPlatformType(content.getPlatformType());
        vo.setContentType(content.getContentType());
        vo.setPublishTime(content.getPublishTime());
        vo.setReadCount(content.getReadCount());
        vo.setLikeCount(content.getLikeCount());
        vo.setCommentCount(content.getCommentCount());
        vo.setForwardCount(content.getForwardCount());
        vo.setIsHit(content.getIsHit() != null && content.getIsHit() == 1);
        vo.setDataSource(content.getDataSource());
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
