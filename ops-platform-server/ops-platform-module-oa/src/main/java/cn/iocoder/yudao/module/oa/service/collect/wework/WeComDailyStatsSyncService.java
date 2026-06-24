package cn.iocoder.yudao.module.oa.service.collect.wework;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.WeworkDailyStatsDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkEmployeeDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.WeworkDailyStatsMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkEmployeeMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 企微日聚合写入（M10-WECOM-S-01 · ADR-048 Q1=A）。
 */
@Service
@RequiredArgsConstructor
public class WeComDailyStatsSyncService {

    private static final String STATUS_ENABLED = "ENABLED";

    private final WeworkAccountMapper weworkAccountMapper;
    private final WeworkEmployeeMapper weworkEmployeeMapper;
    private final WeworkDailyStatsMapper weworkDailyStatsMapper;
    private final WeComApiClient weComApiClient;
    private final AesUtil aesUtil;

    @Transactional
    public int syncDailyStats(Long weworkAccountId) {
        WeworkAccountDO account = requireWeworkAccount(weworkAccountId);
        List<WeworkEmployeeDO> employees = listEnabledEmployees(weworkAccountId);
        if (employees.isEmpty()) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "请先配置企微员工档案");
        }

        String secret = decryptSecret(account.getSecretEncrypted());
        String accessToken = weComApiClient.getAccessToken(account.getCorpId(), secret);

        int totalFriends = 0;
        List<String> userids = new ArrayList<>(employees.size());
        for (WeworkEmployeeDO employee : employees) {
            if (StrUtil.isBlank(employee.getWeworkUserId())) {
                continue;
            }
            userids.add(employee.getWeworkUserId());
            totalFriends += weComApiClient.listExternalContactIds(accessToken, employee.getWeworkUserId()).size();
        }
        if (userids.isEmpty()) {
            throw new ServiceException(2022, "企微员工缺少 wework_user_id");
        }

        LocalDate statDate = LocalDate.now();
        int chatCnt = 0;
        int messageCnt = 0;
        List<WeComUserBehaviorData> behaviorRows = weComApiClient.getUserBehaviorData(
                accessToken, userids, statDate, statDate);
        for (WeComUserBehaviorData row : behaviorRows) {
            chatCnt += row.getChatCnt();
            messageCnt += row.getMessageCnt();
        }

        LocalDateTime syncedAt = LocalDateTime.now();
        Long tenantId = ConfigTenantSupport.requireTenantId();
        WeworkDailyStatsDO existing = weworkDailyStatsMapper.selectOne(
                new LambdaQueryWrapper<WeworkDailyStatsDO>()
                        .eq(WeworkDailyStatsDO::getTenantId, tenantId)
                        .eq(WeworkDailyStatsDO::getWeworkAccountId, weworkAccountId)
                        .eq(WeworkDailyStatsDO::getStatDate, statDate));
        if (existing == null) {
            WeworkDailyStatsDO entity = new WeworkDailyStatsDO();
            entity.setWeworkAccountId(weworkAccountId);
            entity.setStatDate(statDate);
            entity.setTotalFriends(totalFriends);
            entity.setTodayFriendInteractions(chatCnt);
            entity.setTodayMessagesSent(messageCnt);
            entity.setSyncedAt(syncedAt);
            ConfigTenantSupport.fillCreate(entity);
            weworkDailyStatsMapper.insert(entity);
        } else {
            existing.setTotalFriends(totalFriends);
            existing.setTodayFriendInteractions(chatCnt);
            existing.setTodayMessagesSent(messageCnt);
            existing.setSyncedAt(syncedAt);
            ConfigTenantSupport.fillUpdate(existing);
            weworkDailyStatsMapper.updateById(existing);
        }
        return 1;
    }

    private List<WeworkEmployeeDO> listEnabledEmployees(Long weworkAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        return weworkEmployeeMapper.selectList(new LambdaQueryWrapper<WeworkEmployeeDO>()
                .eq(WeworkEmployeeDO::getTenantId, tenantId)
                .eq(WeworkEmployeeDO::getWeworkAccountId, weworkAccountId)
                .eq(WeworkEmployeeDO::getStatus, STATUS_ENABLED));
    }

    private WeworkAccountDO requireWeworkAccount(Long weworkAccountId) {
        WeworkAccountDO entity = weworkAccountMapper.selectById(weworkAccountId);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private String decryptSecret(String secretEncrypted) {
        if (StrUtil.isBlank(secretEncrypted)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "企微应用 Secret 未配置");
        }
        return aesUtil.decrypt(secretEncrypted);
    }
}
