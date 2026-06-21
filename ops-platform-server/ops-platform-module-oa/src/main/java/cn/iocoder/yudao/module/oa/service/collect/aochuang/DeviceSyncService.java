package cn.iocoder.yudao.module.oa.service.collect.aochuang;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.personal.AochuangPendingDeviceVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatBindDeviceReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatCreateAndBindReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatSyncDevicesRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AoCreateAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AoCreateAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import cn.iocoder.yudao.module.oa.service.personal.PersonalWechatCollectStatusService;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangApiException;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangWechatAccountDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 奥创设备同步与绑定（ADR-045 · M10-AO-S-03）。
 */
@Service
@RequiredArgsConstructor
public class DeviceSyncService {

    private static final String BIND_AUTO = "AUTO";
    private static final String BIND_MANUAL = "MANUAL";

    private final AochuangAdapter aochuangAdapter;
    private final AochuangDeviceMatcher deviceMatcher;
    private final AoCreateAccountMapper aoCreateAccountMapper;
    private final PersonalWechatAccountMapper personalWechatAccountMapper;

    @Transactional
    @AuditLog(module = "M10-device-sync", action = "sync-devices")
    public PersonalWechatSyncDevicesRespVO syncDevices(Long aoCreateAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        aochuangAdapter.requireTenantApi();

        List<AoCreateAccountDO> accounts = loadEnabledAccounts(tenantId, aoCreateAccountId);
        if (accounts.isEmpty()) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "无可用奥创账号，请先配置并启用");
        }

        List<PersonalWechatAccountDO> personalRows = personalWechatAccountMapper.selectList(
                new LambdaQueryWrapper<PersonalWechatAccountDO>()
                        .eq(PersonalWechatAccountDO::getTenantId, tenantId));

        PersonalWechatSyncDevicesRespVO resp = new PersonalWechatSyncDevicesRespVO();
        List<AochuangPendingDeviceVO> pendingDevices = new ArrayList<>();
        int autoBound = 0;
        int updated = 0;

        for (AoCreateAccountDO account : accounts) {
            List<AochuangWechatAccountDTO> devices;
            try {
                devices = aochuangAdapter.listWechatAccounts(account.getId());
                account.setConnStatus("OK");
            } catch (AochuangApiException ex) {
                account.setConnStatus(ex.getConnStatus());
                ConfigTenantSupport.fillUpdate(account);
                aoCreateAccountMapper.updateById(account);
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), ex.getMessage());
            }

            account.setLastDeviceSyncAt(LocalDateTime.now());
            ConfigTenantSupport.fillUpdate(account);
            aoCreateAccountMapper.updateById(account);

            for (AochuangWechatAccountDTO device : devices) {
                PersonalWechatAccountDO existingBound = findByDeviceId(tenantId, device.getWechatAccountId());
                if (existingBound != null) {
                    if (BIND_MANUAL.equals(existingBound.getAochuangBindStatus())) {
                        updateSnapshotOnly(existingBound, device);
                    } else {
                        applyBinding(existingBound, device, account, BIND_AUTO);
                    }
                    updated++;
                    continue;
                }

                Optional<PersonalWechatAccountDO> matched = deviceMatcher.findAutoMatch(device, personalRows);
                if (matched.isPresent()) {
                    PersonalWechatAccountDO row = matched.get();
                    if (BIND_MANUAL.equals(row.getAochuangBindStatus())
                            && StrUtil.isNotBlank(row.getAochuangWechatAccountId())) {
                        updateSnapshotOnly(row, device);
                        updated++;
                        continue;
                    }
                    applyBinding(row, device, account, BIND_AUTO);
                    autoBound++;
                    continue;
                }

                AochuangPendingDeviceVO pending = toPending(device, account);
                Optional<AochuangDeviceMatcher.MatchSuggestion> suggestion =
                        deviceMatcher.findBestSuggestion(device, personalRows);
                suggestion.ifPresent(s -> {
                    pending.setSuggestedPersonalWechatId(s.getPersonalWechatId());
                    pending.setFuzzyScore(s.getScore());
                });
                pendingDevices.add(pending);
            }
        }

        resp.setAutoBoundCount(autoBound);
        resp.setUpdatedSnapshotCount(updated);
        resp.setPendingCount(pendingDevices.size());
        resp.setPendingDevices(pendingDevices);
        return resp;
    }

    @Transactional
    @AuditLog(module = "M10-device-sync", action = "bind-device")
    public void bindDevice(Long personalWechatId, PersonalWechatBindDeviceReq req) {
        PersonalWechatAccountDO row = requirePersonalWechat(personalWechatId);
        AoCreateAccountDO aoAccount = aochuangAdapter.requireAoCreateAccount(req.getAochuangAccountRefId());
        assertDeviceIdUnique(row.getTenantId(), req.getAochuangWechatAccountId(), personalWechatId);

        String bindStatus = StrUtil.blankToDefault(req.getBindStatus(), BIND_MANUAL);
        row.setAochuangWechatAccountId(req.getAochuangWechatAccountId());
        row.setAochuangAccountRefId(aoAccount.getId());
        row.setAochuangBindStatus(bindStatus);
        if (StrUtil.isNotBlank(req.getAochuangNickname())) {
            row.setAochuangNickname(req.getAochuangNickname());
        }
        if (StrUtil.isNotBlank(req.getAochuangAvatar())) {
            row.setAochuangAvatar(req.getAochuangAvatar());
        }
        if (req.getAochuangIsAlive() != null) {
            row.setAochuangIsAlive(req.getAochuangIsAlive());
        }
        row.setLastDeviceSyncAt(LocalDateTime.now());
        row.setUpdater(ConfigTenantSupport.currentUsername());
        row.setUpdateTime(LocalDateTime.now());
        personalWechatAccountMapper.updateById(row);
    }

    @Transactional
    @AuditLog(module = "M10-device-sync", action = "create-and-bind")
    public Long createAndBindDevice(PersonalWechatCreateAndBindReq req) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        AoCreateAccountDO aoAccount = aochuangAdapter.requireAoCreateAccount(req.getAochuangAccountRefId());
        assertWechatIdUnique(tenantId, req.getWechatId(), null);
        assertDeviceIdUnique(tenantId, req.getAochuangWechatAccountId(), null);

        PersonalWechatAccountDO entity = new PersonalWechatAccountDO();
        entity.setTenantId(tenantId);
        entity.setAccountName(req.getAccountName());
        entity.setWechatId(req.getWechatId());
        entity.setContactPhone(StrUtil.blankToDefault(req.getContactPhone(), null));
        entity.setStatus("ENABLED");
        entity.setAochuangWechatAccountId(req.getAochuangWechatAccountId());
        entity.setAochuangAccountRefId(aoAccount.getId());
        entity.setAochuangBindStatus(BIND_MANUAL);
        entity.setAochuangNickname(req.getAochuangNickname());
        entity.setAochuangAvatar(req.getAochuangAvatar());
        entity.setAochuangIsAlive(req.getAochuangIsAlive());
        entity.setLastDeviceSyncAt(LocalDateTime.now());
        entity.setCollectStatus(PersonalWechatCollectStatusService.PENDING);
        ConfigTenantSupport.fillCreate(entity);
        personalWechatAccountMapper.insert(entity);
        return entity.getId();
    }

    private List<AoCreateAccountDO> loadEnabledAccounts(Long tenantId, Long aoCreateAccountId) {
        LambdaQueryWrapper<AoCreateAccountDO> wrapper = new LambdaQueryWrapper<AoCreateAccountDO>()
                .eq(AoCreateAccountDO::getTenantId, tenantId)
                .eq(AoCreateAccountDO::getStatus, "ENABLED");
        if (aoCreateAccountId != null) {
            wrapper.eq(AoCreateAccountDO::getId, aoCreateAccountId);
        }
        return aoCreateAccountMapper.selectList(wrapper);
    }

    private PersonalWechatAccountDO findByDeviceId(Long tenantId, String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            return null;
        }
        return personalWechatAccountMapper.selectOne(new LambdaQueryWrapper<PersonalWechatAccountDO>()
                .eq(PersonalWechatAccountDO::getTenantId, tenantId)
                .eq(PersonalWechatAccountDO::getAochuangWechatAccountId, deviceId)
                .last("LIMIT 1"));
    }

    private void applyBinding(PersonalWechatAccountDO row, AochuangWechatAccountDTO device,
                              AoCreateAccountDO account, String bindStatus) {
        assertDeviceIdUnique(row.getTenantId(), device.getWechatAccountId(), row.getId());
        row.setAochuangWechatAccountId(device.getWechatAccountId());
        row.setAochuangAccountRefId(account.getId());
        row.setAochuangBindStatus(bindStatus);
        updateSnapshotFields(row, device);
        row.setLastDeviceSyncAt(LocalDateTime.now());
        row.setUpdater(ConfigTenantSupport.currentUsername());
        row.setUpdateTime(LocalDateTime.now());
        personalWechatAccountMapper.updateById(row);
    }

    private void updateSnapshotOnly(PersonalWechatAccountDO row, AochuangWechatAccountDTO device) {
        updateSnapshotFields(row, device);
        row.setLastDeviceSyncAt(LocalDateTime.now());
        row.setUpdater(ConfigTenantSupport.currentUsername());
        row.setUpdateTime(LocalDateTime.now());
        personalWechatAccountMapper.updateById(row);
    }

    private void updateSnapshotFields(PersonalWechatAccountDO row, AochuangWechatAccountDTO device) {
        if (StrUtil.isNotBlank(device.getNickname())) {
            row.setAochuangNickname(device.getNickname());
        }
        if (StrUtil.isNotBlank(device.getAvatar())) {
            row.setAochuangAvatar(device.getAvatar());
        }
        if (device.getIsAlive() != null) {
            row.setAochuangIsAlive(device.getIsAlive());
        }
    }

    private AochuangPendingDeviceVO toPending(AochuangWechatAccountDTO device, AoCreateAccountDO account) {
        AochuangPendingDeviceVO vo = new AochuangPendingDeviceVO();
        vo.setAochuangWechatAccountId(device.getWechatAccountId());
        vo.setWechatId(device.getWechatId());
        vo.setAlias(device.getAlias());
        vo.setNickname(device.getNickname());
        vo.setAvatar(device.getAvatar());
        vo.setIsAlive(device.getIsAlive());
        vo.setAochuangAccountRefId(account.getId());
        vo.setAochuangAccountName(account.getAccountName());
        return vo;
    }

    private PersonalWechatAccountDO requirePersonalWechat(Long id) {
        PersonalWechatAccountDO entity = personalWechatAccountMapper.selectById(id);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private void assertDeviceIdUnique(Long tenantId, String deviceId, Long excludeId) {
        if (StrUtil.isBlank(deviceId)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "奥创设备 ID 不能为空");
        }
        LambdaQueryWrapper<PersonalWechatAccountDO> wrapper = new LambdaQueryWrapper<PersonalWechatAccountDO>()
                .eq(PersonalWechatAccountDO::getTenantId, tenantId)
                .eq(PersonalWechatAccountDO::getAochuangWechatAccountId, deviceId);
        if (excludeId != null) {
            wrapper.ne(PersonalWechatAccountDO::getId, excludeId);
        }
        if (personalWechatAccountMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY.getCode(), "奥创设备已绑定其他个微档案");
        }
    }

    private void assertWechatIdUnique(Long tenantId, String wechatId, Long excludeId) {
        LambdaQueryWrapper<PersonalWechatAccountDO> wrapper = new LambdaQueryWrapper<PersonalWechatAccountDO>()
                .eq(PersonalWechatAccountDO::getTenantId, tenantId)
                .eq(PersonalWechatAccountDO::getWechatId, wechatId);
        if (excludeId != null) {
            wrapper.ne(PersonalWechatAccountDO::getId, excludeId);
        }
        if (personalWechatAccountMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }
    }
}
