package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerAnalysisVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.FollowerStatsVO;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
        String normDimension = normalizeDimension(null);
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
        List<FollowerAnalysisVO> records = page.getRecords().stream()
                .map(this::toAnalysisVO)
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal());
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

    @Override
    public FollowerStatsVO stats(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                                 Long accountId, String platformType, String dimension) {
        Long tenantId = requireTenantId();
        String normDimension = normalizeDimension(dimension);
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId, platformType);
        FollowerStatsVO vo = new FollowerStatsVO();
        vo.setTotalFollowers(0L);
        vo.setNewFollowers(0L);
        vo.setUnfollowers(0L);
        vo.setNetFollowers(0L);
        vo.setGrowthRate(BigDecimal.ZERO);
        if (accountIds.isEmpty()) {
            return vo;
        }
        // S-R6-B2：WEEK / MONTH 时，按聚合键在 service 层做内存分组
        List<FollowerDailyDO> rows = loadRows(tenantId, accountIds, startDate, endDate, normDimension);
        if (rows.isEmpty()) {
            return vo;
        }
        long newSum = 0L, unSum = 0L, netSum = 0L, maxFollower = 0L;
        for (FollowerDailyDO r : rows) {
            if (r.getNewFollower() != null) newSum += r.getNewFollower();
            if (r.getUnfollowCount() != null) unSum += r.getUnfollowCount();
            if (r.getNetGrowth() != null) netSum += r.getNetGrowth();
            if (r.getFollowerCount() != null && r.getFollowerCount() > maxFollower) {
                maxFollower = r.getFollowerCount();
            }
        }
        vo.setTotalFollowers(maxFollower);
        vo.setNewFollowers(newSum);
        vo.setUnfollowers(unSum);
        vo.setNetFollowers(netSum);
        // 增长率 = 净增 / (区间首日粉丝总数)  = 净增 / (峰值粉丝数 - 净增)
        long prevTotal = maxFollower - netSum;
        if (prevTotal > 0) {
            vo.setGrowthRate(BigDecimal.valueOf(netSum)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(prevTotal), 4, RoundingMode.HALF_UP));
        }
        return vo;
    }

    /**
     * S-R6-B2：dimension 归一化。null / 空 / 未知 → DAY（按日聚合=不聚合）。
     * 入参接受 day/week/month（前端 enum 小写）；DAY/WEEK/MONTH（DictSelect 真值）也行。
     */
    private String normalizeDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) return "DAY";
        String v = dimension.trim().toUpperCase(Locale.ROOT);
        return switch (v) {
            case "WEEK", "W" -> "WEEK";
            case "MONTH", "M" -> "MONTH";
            default -> "DAY";
        };
    }

    /**
     * S-R6-B2：根据 dimension 拉取/分组日行。
     * DAY：直接 selectList；WEEK/MONTH：在 service 层按 (year, week/month) key 聚合。
     */
    private List<FollowerDailyDO> loadRows(Long tenantId, Set<Long> accountIds,
                                           LocalDate startDate, LocalDate endDate, String normDimension) {
        LambdaQueryWrapper<FollowerDailyDO> wrapper = new LambdaQueryWrapper<FollowerDailyDO>()
                .eq(FollowerDailyDO::getTenantId, tenantId)
                .in(FollowerDailyDO::getAccountId, accountIds)
                .ge(startDate != null, FollowerDailyDO::getStatDate, startDate)
                .le(endDate != null, FollowerDailyDO::getStatDate, endDate)
                .orderByAsc(FollowerDailyDO::getStatDate);
        List<FollowerDailyDO> raw = followerDailyMapper.selectList(wrapper);
        if ("DAY".equals(normDimension)) {
            return raw;
        }
        // WEEK / MONTH：按 (accountId, periodKey) 分组聚合（sum new/un/net + max follower）
        WeekFields wf = WeekFields.of(Locale.getDefault());
        Map<String, FollowerDailyDO> bucket = new java.util.LinkedHashMap<>();
        for (FollowerDailyDO r : raw) {
            String key = r.getAccountId() + "|" + (("WEEK".equals(normDimension))
                    ? r.getStatDate().get(wf.weekBasedYear()) + "-W" + r.getStatDate().get(wf.weekOfWeekBasedYear())
                    : r.getStatDate().getYear() + "-" + r.getStatDate().getMonthValue());
            FollowerDailyDO agg = bucket.get(key);
            if (agg == null) {
                bucket.put(key, r);
                continue;
            }
            agg.setNewFollower((agg.getNewFollower() == null ? 0 : agg.getNewFollower()) + (r.getNewFollower() == null ? 0 : r.getNewFollower()));
            agg.setUnfollowCount((agg.getUnfollowCount() == null ? 0 : agg.getUnfollowCount()) + (r.getUnfollowCount() == null ? 0 : r.getUnfollowCount()));
            agg.setNetGrowth((agg.getNetGrowth() == null ? 0 : agg.getNetGrowth()) + (r.getNetGrowth() == null ? 0 : r.getNetGrowth()));
            if (r.getFollowerCount() != null && (agg.getFollowerCount() == null || r.getFollowerCount() > agg.getFollowerCount())) {
                agg.setFollowerCount(r.getFollowerCount());
            }
        }
        return new java.util.ArrayList<>(bucket.values());
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

    @Override
    public byte[] exportCsv(LocalDate startDate, LocalDate endDate, Long ipGroupId,
                            Long accountId, String platformType, String dimension) {
        Long tenantId = requireTenantId();
        String normDimension = normalizeDimension(dimension);
        Set<Long> accountIds = resolveAccountIds(tenantId, ipGroupId, accountId, platformType);
        if (accountIds.isEmpty()) {
            return toCsv(java.util.Collections.emptyList()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        List<FollowerDailyDO> rows = loadRows(tenantId, accountIds, startDate, endDate, normDimension);
        // 按 statDate desc, accountId asc 排序输出
        rows.sort((a, b) -> {
            int c = b.getStatDate().compareTo(a.getStatDate());
            return c != 0 ? c : a.getAccountId().compareTo(b.getAccountId());
        });
        return toCsv(rows).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * S-R6-B3：CSV 序列化。表头按 spec AC-M1-004-2 字段顺序。
     * 单元格值若包含 , " 或换行 → 包裹双引号 + 转义内嵌双引号。
     */
    private String toCsv(List<FollowerDailyDO> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // UTF-8 BOM，Excel 打开不乱码
        sb.append("statDate,accountId,accountName,ipGroupName,followerCount,newFollower,unfollowCount,netGrowth,growthRate\n");
        for (FollowerDailyDO r : rows) {
            AccountDO account = accountMapper.selectById(r.getAccountId());
            String accountName = account == null ? "" : account.getAccountName();
            String ipGroupName = "";
            if (account != null && account.getIpGroupId() != null) {
                IpGroupDO group = ipGroupMapper.selectById(account.getIpGroupId());
                if (group != null) ipGroupName = group.getGroupName();
            }
            sb.append(csv(r.getStatDate() == null ? "" : r.getStatDate().toString())).append(',')
              .append(r.getAccountId() == null ? "" : r.getAccountId()).append(',')
              .append(csv(accountName)).append(',')
              .append(csv(ipGroupName)).append(',')
              .append(r.getFollowerCount() == null ? "" : r.getFollowerCount()).append(',')
              .append(r.getNewFollower() == null ? "" : r.getNewFollower()).append(',')
              .append(r.getUnfollowCount() == null ? "" : r.getUnfollowCount()).append(',')
              .append(r.getNetGrowth() == null ? "" : r.getNetGrowth()).append(',')
              .append(r.getGrowthRate() == null ? "" : r.getGrowthRate()).append('\n');
        }
        return sb.toString();
    }

    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
