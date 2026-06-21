package cn.iocoder.yudao.module.oa.service.personal;

import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 个微采集状态（ADR-045 · M10-AO-S-06，字典 {@code dict_collect_status}）。
 */
@Service
@RequiredArgsConstructor
public class PersonalWechatCollectStatusService {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String PARTIAL = "PARTIAL";

    private final PersonalWechatAccountMapper personalWechatAccountMapper;

    @Transactional
    public void markRunning(Long personalWechatId) {
        updateStatus(personalWechatId, RUNNING);
    }

    @Transactional
    public void markResult(Long personalWechatId, boolean completed) {
        updateStatus(personalWechatId, completed ? SUCCESS : PARTIAL);
    }

    @Transactional
    public void markFailed(Long personalWechatId) {
        updateStatus(personalWechatId, FAILED);
    }

    private void updateStatus(Long personalWechatId, String status) {
        PersonalWechatAccountDO patch = new PersonalWechatAccountDO();
        patch.setId(personalWechatId);
        patch.setCollectStatus(status);
        patch.setUpdater(ConfigTenantSupport.currentUsername());
        patch.setUpdateTime(LocalDateTime.now());
        personalWechatAccountMapper.updateById(patch);
    }
}
