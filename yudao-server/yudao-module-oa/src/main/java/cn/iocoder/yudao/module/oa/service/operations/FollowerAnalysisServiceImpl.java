package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerTrendVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.framework.auth.DataScopeSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowerAnalysisServiceImpl implements FollowerAnalysisService {

    private final FollowerDailyMapper followerDailyMapper;
    private final AccountMapper accountMapper;
    private final IpGroupMapper ipGroupMapper;

    @Override
    public PageResult<FollowerAnalysisVO> list(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                               Long accountId, String platformType, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId, platformType);
        if (accountIds.isEmpty()) {
            return PageResult.empty();
        }

        LambdaQueryWrapper<FollowerDailyDO> wrapper = new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, tenantId)
                .in(FollowerDailyDO::getAccountId, accountIds)
                .ge(startDate != null, FollowerDailyDO::getStatDate, startDate)
                .le(endDate != null, FollowerDailyDO::getStatDate, endDate)
                .orderByDesc(FollowerDailyDO::getStatDate);
        Page<FollowerDailyDO> page = followerDailyMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toAnalysisVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public List<FollowerTrendVO> trend(LocalDate startDate, LocalDate endDate, Long ipGroupId, Long accountId) {
        Long tenantId = requireTenantId();
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId, null);
        if (accountIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FollowerDailyDO> wrapper = new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, tenantId)
                .in(FollowerDailyDO::getAccountId, accountIds)
                .ge(startDate != null, FollowerDailyDO::getStatDate, startDate)
                .le(endDate != null, FollowerDailyDO::getStatDate, endDate)
                .orderByAsc(FollowerDailyDO::getStatDate);
        return followerDailyMapper.selectList(wrapper).stream().map(this::toTrendVO).collect(Collectors.toList());
    }

    private Set<Long> resolveAccountIds(Long tenantId, Long ipGroupId, Long accountId, String platformType) {
        if (accountId != null) {
            AccountDO account = accountMapper.selectById(accountId);
            if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
                return Collections.emptySet();
            }
            return Set.of(accountId);
        }
        LambdaQueryWrapper<AccountDO> wrapper = new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .eq(ipGroupId != null, AccountDO::getIpGroupId, ipGroupId)
                .eq(platformType != null, AccountDO::getPlatformType, platformType);
        DataScopeSupport.applyIpGroupScope(wrapper, AccountDO::getIpGroupId);
        return accountMapper.selectList(wrapper).stream().map(AccountDO::getId).collect(Collectors.toSet());
    }

    private FollowerAnalysisVO toAnalysisVO(FollowerDailyDO row) {
        FollowerAnalysisVO vo = new FollowerAnalysisVO();
        vo.setStatDate(row.getStatDate());
        vo.setAccountId(row.getAccountId());
        fillAccountInfo(vo, row.getAccountId());
        vo.setFollowerCount(row.getFollowerCount());
        vo.setNewFollower(row.getNewFollower());
        vo.setUnfollowCount(row.getUnfollowCount());
        vo.setNetGrowth(row.getNetGrowth());
        vo.setGrowthRate(row.getGrowthRate());
        return vo;
    }

    private FollowerTrendVO toTrendVO(FollowerDailyDO row) {
        FollowerTrendVO vo = new FollowerTrendVO();
        vo.setTimePeriod(row.getStatDate().toString());
        AccountDO account = accountMapper.selectById(row.getAccountId());
        if (account != null) {
            vo.setAccountName(account.getAccountName());
            if (account.getIpGroupId() != null) {
                IpGroupDO group = ipGroupMapper.selectById(account.getIpGroupId());
                if (group != null) {
                    vo.setIpGroupName(group.getGroupName());
                }
            }
        }
        vo.setFollowerCount(row.getFollowerCount());
        vo.setNewFollower(row.getNewFollower());
        vo.setUnfollowCount(row.getUnfollowCount());
        vo.setNetGrowth(row.getNetGrowth());
        vo.setGrowthRate(row.getGrowthRate());
        return vo;
    }

    private void fillAccountInfo(FollowerAnalysisVO vo, Long accountId) {
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null) {
            return;
        }
        vo.setAccountName(account.getAccountName());
        if (account.getIpGroupId() != null) {
            IpGroupDO group = ipGroupMapper.selectById(account.getIpGroupId());
            if (group != null) {
                vo.setIpGroupName(group.getGroupName());
            }
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
