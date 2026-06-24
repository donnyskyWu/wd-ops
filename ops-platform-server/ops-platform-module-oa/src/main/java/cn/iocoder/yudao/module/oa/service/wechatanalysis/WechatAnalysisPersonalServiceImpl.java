package cn.iocoder.yudao.module.oa.service.wechatanalysis;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.PersonalAnalysisDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.PersonalAnalysisListItemVO;
import cn.iocoder.yudao.module.oa.api.dto.wechatanalysis.PersonalDailyStatsItemVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatDailyStatsDO;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatDailyStatsMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WechatAnalysisPersonalServiceImpl implements WechatAnalysisPersonalService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PersonalWechatAccountMapper personalWechatAccountMapper;
    private final PersonalWechatDailyStatsMapper personalWechatDailyStatsMapper;

    @Override
    public PageResult<PersonalAnalysisListItemVO> list(Long accountId, String accountName,
                                                       LocalDate statDate, Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LocalDate targetDate = statDate != null ? statDate : LocalDate.now();

        LambdaQueryWrapper<PersonalWechatAccountDO> wrapper = new LambdaQueryWrapper<PersonalWechatAccountDO>()
                .eq(PersonalWechatAccountDO::getTenantId, tenantId)
                .eq(accountId != null, PersonalWechatAccountDO::getId, accountId)
                .like(StrUtil.isNotBlank(accountName), PersonalWechatAccountDO::getAccountName, accountName)
                .orderByDesc(PersonalWechatAccountDO::getId);

        Page<PersonalWechatAccountDO> page = personalWechatAccountMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);

        List<PersonalAnalysisListItemVO> list = page.getRecords().stream()
                .map(account -> toListItem(account, targetDate))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public PersonalAnalysisDetailVO detail(Long accountId, LocalDate startDate, LocalDate endDate) {
        PersonalWechatAccountDO account = ConfigTenantSupport.getRequiredInTenant(
                personalWechatAccountMapper.selectById(accountId));
        Long tenantId = account.getTenantId();

        LambdaQueryWrapper<PersonalWechatDailyStatsDO> wrapper = new LambdaQueryWrapper<PersonalWechatDailyStatsDO>()
                .eq(PersonalWechatDailyStatsDO::getTenantId, tenantId)
                .eq(PersonalWechatDailyStatsDO::getPersonalWechatId, accountId)
                .ge(startDate != null, PersonalWechatDailyStatsDO::getStatDate, startDate)
                .le(endDate != null, PersonalWechatDailyStatsDO::getStatDate, endDate)
                .orderByDesc(PersonalWechatDailyStatsDO::getStatDate);

        List<PersonalDailyStatsItemVO> dailyStats = personalWechatDailyStatsMapper.selectList(wrapper).stream()
                .map(this::toStatsItem)
                .collect(Collectors.toList());

        PersonalAnalysisDetailVO vo = new PersonalAnalysisDetailVO();
        vo.setAccountId(account.getId());
        vo.setAccountName(account.getAccountName());
        vo.setWechatId(account.getWechatId());
        vo.setCollectStatus(account.getCollectStatus());
        vo.setAochuangBindStatus(account.getAochuangBindStatus());
        vo.setDailyStats(dailyStats);
        return vo;
    }

    private PersonalAnalysisListItemVO toListItem(PersonalWechatAccountDO account, LocalDate statDate) {
        PersonalWechatDailyStatsDO stats = findStatsOnDate(account.getTenantId(), account.getId(), statDate);
        if (stats == null) {
            stats = findLatestStats(account.getTenantId(), account.getId());
        }

        PersonalAnalysisListItemVO vo = new PersonalAnalysisListItemVO();
        vo.setAccountId(account.getId());
        vo.setAccountName(account.getAccountName());
        if (stats != null) {
            vo.setTotalFriends(stats.getTotalFriends());
            vo.setNewFriends(stats.getNewFriends());
            vo.setMessagesSent(stats.getMessagesSent());
            if (stats.getStatDate() != null) {
                vo.setStatDate(stats.getStatDate().format(DATE_FMT));
            }
        } else {
            vo.setTotalFriends(0);
            vo.setNewFriends(0);
            vo.setMessagesSent(0);
        }
        return vo;
    }

    private PersonalWechatDailyStatsDO findStatsOnDate(Long tenantId, Long personalWechatId, LocalDate statDate) {
        return personalWechatDailyStatsMapper.selectOne(new LambdaQueryWrapper<PersonalWechatDailyStatsDO>()
                .eq(PersonalWechatDailyStatsDO::getTenantId, tenantId)
                .eq(PersonalWechatDailyStatsDO::getPersonalWechatId, personalWechatId)
                .eq(PersonalWechatDailyStatsDO::getStatDate, statDate)
                .last("LIMIT 1"));
    }

    private PersonalWechatDailyStatsDO findLatestStats(Long tenantId, Long personalWechatId) {
        return personalWechatDailyStatsMapper.selectOne(new LambdaQueryWrapper<PersonalWechatDailyStatsDO>()
                .eq(PersonalWechatDailyStatsDO::getTenantId, tenantId)
                .eq(PersonalWechatDailyStatsDO::getPersonalWechatId, personalWechatId)
                .orderByDesc(PersonalWechatDailyStatsDO::getStatDate)
                .last("LIMIT 1"));
    }

    private PersonalDailyStatsItemVO toStatsItem(PersonalWechatDailyStatsDO entity) {
        PersonalDailyStatsItemVO vo = new PersonalDailyStatsItemVO();
        if (entity.getStatDate() != null) {
            vo.setStatDate(entity.getStatDate().format(DATE_FMT));
        }
        vo.setTotalFriends(entity.getTotalFriends());
        vo.setNewFriends(entity.getNewFriends());
        vo.setDeletedFriends(entity.getDeletedFriends());
        vo.setMessagesSent(entity.getMessagesSent());
        vo.setMessagesReceived(entity.getMessagesReceived());
        vo.setGroupCount(entity.getGroupCount());
        return vo;
    }
}
