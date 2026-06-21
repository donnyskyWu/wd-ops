package cn.iocoder.yudao.module.oa.service.personal;

import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatDailyStatsRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatDailyStatsDO;

import java.time.LocalDate;
import java.util.List;

public interface PersonalWechatDailyStatsService {

    List<PersonalWechatDailyStatsRespVO> listDailyStats(Long personalWechatId, LocalDate startDate, LocalDate endDate);

    PersonalWechatDailyStatsDO findLatestStats(Long personalWechatId);

    PersonalWechatDailyStatsDO findStatsOnDate(Long personalWechatId, LocalDate statDate);

    void upsertFriendStats(PersonalWechatAccountDO personal, LocalDate statDate, int friendCount);
}
