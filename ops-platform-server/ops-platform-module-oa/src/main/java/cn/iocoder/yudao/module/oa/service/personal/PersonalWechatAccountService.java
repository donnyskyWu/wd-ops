package cn.iocoder.yudao.module.oa.service.personal;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatApiConfigReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatBindDeviceReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatCreateAndBindReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncDevicesReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncDevicesRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncFriendsReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncFriendsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncMessagesReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncMessagesRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatDailyStatsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.AochuangFriendRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.AochuangMessageRespVO;

import java.time.LocalDate;
import java.util.List;

public interface PersonalWechatAccountService {

    PageResult<PersonalWechatRespVO> list(String accountName, String wechatId, String status,
                                          Integer pageNo, Integer pageSize);

    PersonalWechatRespVO get(Long id);

    Long create(PersonalWechatCreateReq req);

    void update(PersonalWechatUpdateReq req);

    void delete(Long id);

    void saveApiConfig(PersonalWechatApiConfigReq req);

    PersonalWechatRespVO getApiConfig(Long id);

    PersonalWechatSyncDevicesRespVO syncDevices(PersonalWechatSyncDevicesReq req);

    void bindDevice(Long id, PersonalWechatBindDeviceReq req);

    Long createAndBindDevice(PersonalWechatCreateAndBindReq req);

    PersonalWechatSyncFriendsRespVO syncFriends(Long id, PersonalWechatSyncFriendsReq req);

    PageResult<AochuangFriendRespVO> listFriends(Long id, String nickname, Integer pageNo, Integer pageSize);

    PersonalWechatSyncMessagesRespVO syncMessages(Long id, PersonalWechatSyncMessagesReq req);

    PageResult<AochuangMessageRespVO> listMessages(Long id, String aochuangFriendId,
                                                   Integer pageNo, Integer pageSize);

    List<PersonalWechatDailyStatsRespVO> listDailyStats(Long id, LocalDate startDate, LocalDate endDate);
}
