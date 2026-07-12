package cn.iocoder.yudao.module.oa.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountSyncRemoteReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountSyncResultVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountTestConnectionRespVO;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateAccountUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AochuangRemoteAccountRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AoCreateAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AoCreateApiDO;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AoCreateAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AoCreateApiMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangAccountDTO;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangApiClient;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangApiException;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangWechatAccountDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AoCreateAccountServiceImpl implements AoCreateAccountService {

    private final AoCreateAccountMapper aoCreateAccountMapper;
    private final AoCreateApiMapper aoCreateApiMapper;
    private final AochuangApiClient aochuangApiClient;

    @Override
    public PageResult<AoCreateAccountRespVO> list(String accountName, String status, Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<AoCreateAccountDO> wrapper = new LambdaQueryWrapper<AoCreateAccountDO>()
                .eq(AoCreateAccountDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(accountName), AoCreateAccountDO::getAccountName, accountName)
                .eq(StrUtil.isNotBlank(status), AoCreateAccountDO::getStatus, status)
                .orderByDesc(AoCreateAccountDO::getId);
        Page<AoCreateAccountDO> page = aoCreateAccountMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<AoCreateAccountRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-aocreate-account", action = "create")
    public Long create(AoCreateAccountCreateReq req) {
        AoCreateApiDO api = requireTenantApi();
        assertUniqueAochuangAccountId(null, req.getAochuangAccountId());
        AoCreateAccountDO entity = new AoCreateAccountDO();
        entity.setAocreateApiId(api.getId());
        entity.setAccountName(req.getAccountName());
        entity.setAochuangAccountId(req.getAochuangAccountId());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setConnStatus("DISCONNECTED");
        ConfigTenantSupport.fillCreate(entity);
        aoCreateAccountMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-aocreate-account", action = "update")
    public void update(AoCreateAccountUpdateReq req) {
        AoCreateAccountDO existing = getRequired(req.getId());
        assertUniqueAochuangAccountId(existing.getId(), req.getAochuangAccountId());
        existing.setAccountName(req.getAccountName());
        existing.setAochuangAccountId(req.getAochuangAccountId());
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        ConfigTenantSupport.fillUpdate(existing);
        aoCreateAccountMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-aocreate-account", action = "delete")
    public void delete(Long id) {
        AoCreateAccountDO existing = getRequired(id);
        aoCreateAccountMapper.deleteById(existing.getId());
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-aocreate-account", action = "test-connection")
    public AoCreateAccountTestConnectionRespVO testConnection(Long id) {
        AoCreateAccountDO account = getRequired(id);
        AoCreateApiDO api = requireApiForAccount(account);
        AoCreateAccountTestConnectionRespVO resp = new AoCreateAccountTestConnectionRespVO();
        try {
            List<AochuangWechatAccountDTO> devices = aochuangApiClient.listWechatAccounts(api, account.getAochuangAccountId());
            resp.setSuccess(true);
            resp.setDeviceCount(devices.size());
            resp.setConnStatus("OK");
            resp.setMessage("连接成功，发现 " + devices.size() + " 个设备");
            account.setConnStatus("OK");
        } catch (AochuangApiException ex) {
            resp.setSuccess(false);
            resp.setDeviceCount(0);
            resp.setConnStatus(ex.getConnStatus());
            resp.setMessage(ex.getMessage());
            account.setConnStatus(ex.getConnStatus());
        } catch (Exception ex) {
            resp.setSuccess(false);
            resp.setDeviceCount(0);
            resp.setConnStatus("DISCONNECTED");
            resp.setMessage("连接失败: " + ex.getMessage());
            account.setConnStatus("DISCONNECTED");
        }
        ConfigTenantSupport.fillUpdate(account);
        aoCreateAccountMapper.updateById(account);
        return resp;
    }

    @Override
    public List<AochuangRemoteAccountRespVO> listRemoteSubAccounts(String lastUpdateTime) {
        AoCreateApiDO api = requireTenantApi();
        Map<String, AoCreateAccountDO> localByAoId = loadLocalAccountMap();
        return aochuangApiClient.listAccounts(api, lastUpdateTime).stream()
                .filter(dto -> dto.getAccountType() != null
                        && dto.getAccountType() == AochuangApiClient.ACCOUNT_TYPE_SUB)
                .map(dto -> toRemoteResp(dto, localByAoId.get(dto.getAccountId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M10-aocreate-account", action = "sync-remote")
    public AoCreateAccountSyncResultVO syncRemoteSubAccounts(AoCreateAccountSyncRemoteReq req) {
        AoCreateAccountSyncResultVO result = new AoCreateAccountSyncResultVO();
        AoCreateApiDO api = requireTenantApi();
        List<AochuangAccountDTO> remoteSubs = aochuangApiClient.listAccounts(api, req.getLastUpdateTime()).stream()
                .filter(dto -> dto.getAccountType() != null
                        && dto.getAccountType() == AochuangApiClient.ACCOUNT_TYPE_SUB)
                .toList();
        Map<String, AochuangAccountDTO> remoteById = remoteSubs.stream()
                .collect(Collectors.toMap(AochuangAccountDTO::getAccountId, Function.identity(), (a, b) -> a));

        Set<String> targetIds = resolveSyncTargetIds(req, remoteSubs);
        if (targetIds.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "请指定要同步的子账号或选择全部同步");
        }

        Map<String, AoCreateAccountDO> localByAoId = loadLocalAccountMap();
        for (String aoId : targetIds) {
            AochuangAccountDTO remote = remoteById.get(aoId);
            if (remote == null) {
                result.setFailCount(result.getFailCount() + 1);
                result.getFailReasons().add("远程子账号不存在: " + aoId);
                continue;
            }
            AoCreateAccountDO existing = localByAoId.get(aoId);
            if (existing != null) {
                String remoteName = resolveAccountName(remote);
                if (!StrUtil.equals(existing.getAccountName(), remoteName)) {
                    existing.setAccountName(remoteName);
                    ConfigTenantSupport.fillUpdate(existing);
                    aoCreateAccountMapper.updateById(existing);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } else {
                    result.setSkipCount(result.getSkipCount() + 1);
                }
                continue;
            }
            AoCreateAccountDO entity = new AoCreateAccountDO();
            entity.setAocreateApiId(api.getId());
            entity.setAccountName(resolveAccountName(remote));
            entity.setAochuangAccountId(aoId);
            entity.setStatus("ENABLED");
            entity.setConnStatus("DISCONNECTED");
            ConfigTenantSupport.fillCreate(entity);
            aoCreateAccountMapper.insert(entity);
            result.setSuccessCount(result.getSuccessCount() + 1);
        }
        return result;
    }

    private Set<String> resolveSyncTargetIds(AoCreateAccountSyncRemoteReq req, List<AochuangAccountDTO> remoteSubs) {
        if (Boolean.TRUE.equals(req.getSyncAll())) {
            return remoteSubs.stream().map(AochuangAccountDTO::getAccountId).collect(Collectors.toSet());
        }
        if (req.getAochuangAccountIds() == null || req.getAochuangAccountIds().isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(req.getAochuangAccountIds());
    }

    private Map<String, AoCreateAccountDO> loadLocalAccountMap() {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        return aoCreateAccountMapper.selectList(new LambdaQueryWrapper<AoCreateAccountDO>()
                        .eq(AoCreateAccountDO::getTenantId, tenantId))
                .stream()
                .collect(Collectors.toMap(AoCreateAccountDO::getAochuangAccountId, Function.identity(), (a, b) -> a));
    }

    private AochuangRemoteAccountRespVO toRemoteResp(AochuangAccountDTO dto, AoCreateAccountDO local) {
        AochuangRemoteAccountRespVO vo = new AochuangRemoteAccountRespVO();
        vo.setAccountId(dto.getAccountId());
        vo.setUserName(dto.getUserName());
        vo.setAccountType(dto.getAccountType());
        vo.setStatus(dto.getStatus());
        vo.setDepartment(dto.getDepartment());
        vo.setUpdateDate(dto.getUpdateDate());
        vo.setSynced(local != null);
        if (local != null) {
            vo.setLocalId(local.getId());
        }
        return vo;
    }

    private String resolveAccountName(AochuangAccountDTO remote) {
        if (StrUtil.isNotBlank(remote.getUserName())) {
            return remote.getUserName();
        }
        return remote.getAccountId();
    }

    private AoCreateAccountDO getRequired(Long id) {
        AoCreateAccountDO entity = aoCreateAccountMapper.selectById(id);
        return ConfigTenantSupport.getRequiredInTenant(entity);
    }

    private AoCreateApiDO requireTenantApi() {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        AoCreateApiDO api = aoCreateApiMapper.selectOne(new LambdaQueryWrapper<AoCreateApiDO>()
                .eq(AoCreateApiDO::getTenantId, tenantId)
                .last("LIMIT 1"));
        if (api == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "请先配置奥创接口凭证");
        }
        return api;
    }

    private AoCreateApiDO requireApiForAccount(AoCreateAccountDO account) {
        AoCreateApiDO api = aoCreateApiMapper.selectById(account.getAocreateApiId());
        if (api == null || !ConfigTenantSupport.requireTenantId().equals(api.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "奥创接口凭证不存在");
        }
        return api;
    }

    private void assertUniqueAochuangAccountId(Long excludeId, String aochuangAccountId) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<AoCreateAccountDO> wrapper = new LambdaQueryWrapper<AoCreateAccountDO>()
                .eq(AoCreateAccountDO::getTenantId, tenantId)
                .eq(AoCreateAccountDO::getAochuangAccountId, aochuangAccountId);
        if (excludeId != null) {
            wrapper.ne(AoCreateAccountDO::getId, excludeId);
        }
        if (aoCreateAccountMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY.getCode(), "奥创账号 ID 已存在");
        }
    }

    private AoCreateAccountRespVO toResp(AoCreateAccountDO entity) {
        AoCreateAccountRespVO vo = new AoCreateAccountRespVO();
        vo.setId(entity.getId());
        vo.setAocreateApiId(entity.getAocreateApiId());
        vo.setAccountName(entity.getAccountName());
        vo.setAochuangAccountId(entity.getAochuangAccountId());
        vo.setStatus(entity.getStatus());
        vo.setConnStatus(entity.getConnStatus());
        vo.setLastDeviceSyncAt(entity.getLastDeviceSyncAt());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
