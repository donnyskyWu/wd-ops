package cn.iocoder.yudao.module.oa.service.personal;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatDailyStatsRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatDailyStatsDO;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatDailyStatsMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalWechatDailyStatsServiceImpl implements PersonalWechatDailyStatsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PersonalWechatDailyStatsMapper personalWechatDailyStatsMapper;
    private final PersonalWechatAccountMapper personalWechatAccountMapper;

    @Override
    public List<PersonalWechatDailyStatsRespVO> listDailyStats(Long personalWechatId,
                                                               LocalDate startDate, LocalDate endDate) {
        requirePersonalWechat(personalWechatId);
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<PersonalWechatDailyStatsDO> wrapper = new LambdaQueryWrapper<PersonalWechatDailyStatsDO>()
                .eq(PersonalWechatDailyStatsDO::getTenantId, tenantId)
                .eq(PersonalWechatDailyStatsDO::getPersonalWechatId, personalWechatId)
                .ge(startDate != null, PersonalWechatDailyStatsDO::getStatDate, startDate)
                .le(endDate != null, PersonalWechatDailyStatsDO::getStatDate, endDate)
                .orderByDesc(PersonalWechatDailyStatsDO::getStatDate);
        return personalWechatDailyStatsMapper.selectList(wrapper).stream()
                .map(this::toResp)
                .collect(Collectors.toList());
    }

    @Override
    public PersonalWechatDailyStatsDO findLatestStats(Long personalWechatId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        return personalWechatDailyStatsMapper.selectOne(new LambdaQueryWrapper<PersonalWechatDailyStatsDO>()
                .eq(PersonalWechatDailyStatsDO::getTenantId, tenantId)
                .eq(PersonalWechatDailyStatsDO::getPersonalWechatId, personalWechatId)
                .orderByDesc(PersonalWechatDailyStatsDO::getStatDate)
                .last("LIMIT 1"));
    }

    @Override
    public PersonalWechatDailyStatsDO findStatsOnDate(Long personalWechatId, LocalDate statDate) {
        if (statDate == null) {
            return findLatestStats(personalWechatId);
        }
        Long tenantId = ConfigTenantSupport.requireTenantId();
        return personalWechatDailyStatsMapper.selectOne(new LambdaQueryWrapper<PersonalWechatDailyStatsDO>()
                .eq(PersonalWechatDailyStatsDO::getTenantId, tenantId)
                .eq(PersonalWechatDailyStatsDO::getPersonalWechatId, personalWechatId)
                .eq(PersonalWechatDailyStatsDO::getStatDate, statDate)
                .last("LIMIT 1"));
    }

    @Override
    public void upsertFriendStats(PersonalWechatAccountDO personal, LocalDate statDate, int friendCount) {
        Long tenantId = personal.getTenantId();
        LocalDateTime now = LocalDateTime.now();
        PersonalWechatDailyStatsDO existing = personalWechatDailyStatsMapper.selectOne(
                new LambdaQueryWrapper<PersonalWechatDailyStatsDO>()
                        .eq(PersonalWechatDailyStatsDO::getTenantId, tenantId)
                        .eq(PersonalWechatDailyStatsDO::getPersonalWechatId, personal.getId())
                        .eq(PersonalWechatDailyStatsDO::getStatDate, statDate)
                        .last("LIMIT 1"));
        if (existing == null) {
            PersonalWechatDailyStatsDO entity = new PersonalWechatDailyStatsDO();
            entity.setTenantId(tenantId);
            entity.setPersonalWechatId(personal.getId());
            entity.setStatDate(statDate);
            entity.setTotalFriends(friendCount);
            entity.setNewFriends(0);
            entity.setDeletedFriends(0);
            entity.setMessagesSent(0);
            entity.setMessagesReceived(0);
            entity.setGroupCount(0);
            ConfigTenantSupport.fillCreate(entity);
            personalWechatDailyStatsMapper.insert(entity);
            return;
        }
        existing.setTotalFriends(friendCount);
        existing.setUpdater(ConfigTenantSupport.currentUsername());
        existing.setUpdateTime(now);
        personalWechatDailyStatsMapper.updateById(existing);
    }

    private PersonalWechatDailyStatsRespVO toResp(PersonalWechatDailyStatsDO entity) {
        PersonalWechatDailyStatsRespVO vo = new PersonalWechatDailyStatsRespVO();
        vo.setPersonalWechatId(entity.getPersonalWechatId());
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

    private PersonalWechatAccountDO requirePersonalWechat(Long id) {
        PersonalWechatAccountDO entity = personalWechatAccountMapper.selectById(id);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }
}
