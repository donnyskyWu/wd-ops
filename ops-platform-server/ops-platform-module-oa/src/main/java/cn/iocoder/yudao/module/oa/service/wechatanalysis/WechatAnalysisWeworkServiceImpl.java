package cn.iocoder.yudao.module.oa.service.wechatanalysis;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.WeworkAnalysisDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.WeworkAnalysisListItemVO;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.WeworkDailyStatsItemVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WeworkDailyStatsDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WeworkDailyStatsMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkAccountMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WechatAnalysisWeworkServiceImpl implements WechatAnalysisWeworkService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WeworkAccountMapper weworkAccountMapper;
    private final WeworkDailyStatsMapper weworkDailyStatsMapper;

    @Override
    public PageResult<WeworkAnalysisListItemVO> list(Long accountId, String accountName,
                                                     LocalDate statDate, Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LocalDate targetDate = statDate != null ? statDate : LocalDate.now();

        LambdaQueryWrapper<WeworkAccountDO> wrapper = new LambdaQueryWrapper<WeworkAccountDO>()
                .eq(WeworkAccountDO::getTenantId, tenantId)
                .eq(accountId != null, WeworkAccountDO::getId, accountId)
                .like(StrUtil.isNotBlank(accountName), WeworkAccountDO::getAccountName, accountName)
                .orderByDesc(WeworkAccountDO::getId);

        Page<WeworkAccountDO> page = weworkAccountMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);

        List<WeworkAnalysisListItemVO> list = page.getRecords().stream()
                .map(account -> toListItem(account, targetDate))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public WeworkAnalysisDetailVO detail(Long accountId, LocalDate startDate, LocalDate endDate) {
        WeworkAccountDO account = ConfigTenantSupport.getRequiredInTenant(weworkAccountMapper.selectById(accountId));
        Long tenantId = account.getTenantId();

        LambdaQueryWrapper<WeworkDailyStatsDO> wrapper = new LambdaQueryWrapper<WeworkDailyStatsDO>()
                .eq(WeworkDailyStatsDO::getTenantId, tenantId)
                .eq(WeworkDailyStatsDO::getWeworkAccountId, accountId)
                .ge(startDate != null, WeworkDailyStatsDO::getStatDate, startDate)
                .le(endDate != null, WeworkDailyStatsDO::getStatDate, endDate)
                .orderByDesc(WeworkDailyStatsDO::getStatDate);

        List<WeworkDailyStatsItemVO> dailyStats = weworkDailyStatsMapper.selectList(wrapper).stream()
                .map(this::toStatsItem)
                .collect(Collectors.toList());

        WeworkAnalysisDetailVO vo = new WeworkAnalysisDetailVO();
        vo.setAccountId(account.getId());
        vo.setAccountName(account.getAccountName());
        vo.setCorpId(account.getCorpId());
        vo.setConnStatus(account.getConnStatus());
        vo.setDailyStats(dailyStats);
        return vo;
    }

    private WeworkAnalysisListItemVO toListItem(WeworkAccountDO account, LocalDate statDate) {
        WeworkDailyStatsDO stats = findStatsOnDate(account.getTenantId(), account.getId(), statDate);
        if (stats == null) {
            stats = findLatestStats(account.getTenantId(), account.getId());
        }

        WeworkAnalysisListItemVO vo = new WeworkAnalysisListItemVO();
        vo.setAccountId(account.getId());
        vo.setAccountName(account.getAccountName());
        if (stats != null) {
            vo.setTotalFriends(stats.getTotalFriends());
            vo.setTodayFriendInteractions(stats.getTodayFriendInteractions());
            vo.setTodayMessagesSent(stats.getTodayMessagesSent());
            if (stats.getStatDate() != null) {
                vo.setStatDate(stats.getStatDate().format(DATE_FMT));
            }
        } else {
            vo.setTotalFriends(0);
            vo.setTodayFriendInteractions(0);
            vo.setTodayMessagesSent(0);
        }
        return vo;
    }

    private WeworkDailyStatsDO findStatsOnDate(Long tenantId, Long weworkAccountId, LocalDate statDate) {
        return weworkDailyStatsMapper.selectOne(new LambdaQueryWrapper<WeworkDailyStatsDO>()
                .eq(WeworkDailyStatsDO::getTenantId, tenantId)
                .eq(WeworkDailyStatsDO::getWeworkAccountId, weworkAccountId)
                .eq(WeworkDailyStatsDO::getStatDate, statDate)
                .last("LIMIT 1"));
    }

    private WeworkDailyStatsDO findLatestStats(Long tenantId, Long weworkAccountId) {
        return weworkDailyStatsMapper.selectOne(new LambdaQueryWrapper<WeworkDailyStatsDO>()
                .eq(WeworkDailyStatsDO::getTenantId, tenantId)
                .eq(WeworkDailyStatsDO::getWeworkAccountId, weworkAccountId)
                .orderByDesc(WeworkDailyStatsDO::getStatDate)
                .last("LIMIT 1"));
    }

    private WeworkDailyStatsItemVO toStatsItem(WeworkDailyStatsDO entity) {
        WeworkDailyStatsItemVO vo = new WeworkDailyStatsItemVO();
        if (entity.getStatDate() != null) {
            vo.setStatDate(entity.getStatDate().format(DATE_FMT));
        }
        vo.setTotalFriends(entity.getTotalFriends());
        vo.setTodayFriendInteractions(entity.getTodayFriendInteractions());
        vo.setTodayMessagesSent(entity.getTodayMessagesSent());
        LocalDateTime syncedAt = entity.getSyncedAt();
        if (syncedAt != null) {
            vo.setSyncedAt(syncedAt.format(DT_FMT));
        }
        return vo;
    }
}
