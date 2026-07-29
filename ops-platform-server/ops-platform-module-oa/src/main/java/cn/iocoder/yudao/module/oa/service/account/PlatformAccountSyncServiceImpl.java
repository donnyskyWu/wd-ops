package cn.iocoder.yudao.module.oa.service.account;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountRespVO;
import cn.iocoder.yudao.module.oa.api.dto.account.AccountUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.OaAccountExtDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.company.CompanyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.mysql.company.CompanyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameMapper;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import cn.iocoder.yudao.module.oa.service.auth.OpsDataScopeSupport;
import cn.iocoder.yudao.module.oa.service.support.FootballSystemUserValidator;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformAccountSyncServiceImpl implements PlatformAccountSyncService {

    private static final String PLATFORM_WECHAT_OFFICIAL = "WECHAT_OFFICIAL";
    private static final String SYNC_SYNCED = "SYNCED";
    private static final String SYNC_ERROR = "ERROR";

    private final MpAccountDataService mpAccountDataService;
    private final OaAccountExtDataService oaAccountExtDataService;
    private final CompanyMapper companyMapper;
    private final RealnameMapper realnameMapper;
    private final IpGroupMapper ipGroupMapper;
    private final FootballSystemUserValidator footballSystemUserValidator;
    private final AesUtil aesUtil;
    private final OpsDataScopeSupport opsDataScopeSupport;
    private final WechatOfficialAccountResolver wechatOfficialAccountResolver;

    @Override
    public PageResult<AccountRespVO> listWechatOfficial(String accountName, Long companyId, Long realnameId,
                                                        String status, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<MpAccountDO> wrapper = new LambdaQueryWrapper<MpAccountDO>()
                .eq(MpAccountDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(accountName), MpAccountDO::getName, accountName)
                .orderByDesc(MpAccountDO::getId);
        if (StrUtil.isNotBlank(status)) {
            int mpStatus = "NORMAL".equals(status) || "ENABLED".equals(status) ? 0 : 1;
            wrapper.eq(MpAccountDO::getStatus, mpStatus);
        }

        // 非 admin：先按 oa_account_ext.ip_group_id ∈ 所在IP组（成员∪组长）缩小 mp_account，再分页
        if (!opsDataScopeSupport.isOaTenantAdmin(LoginUserContext.get())) {
            Set<Long> groupIds = opsDataScopeSupport.narrowIpGroupIds(null);
            if (groupIds != null && groupIds.size() == 1 && groupIds.contains(-1L)) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
            if (groupIds != null) {
                List<Long> scopedMpIds = oaAccountExtDataService.listMpAccountIdsByIpGroupIds(tenantId, groupIds);
                if (scopedMpIds.isEmpty()) {
                    return new PageResult<>(Collections.emptyList(), 0L);
                }
                wrapper.in(MpAccountDO::getId, scopedMpIds);
            }
        }

        Page<MpAccountDO> page = mpAccountDataService.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);

        List<Long> mpIds = page.getRecords().stream().map(MpAccountDO::getId).toList();
        Map<Long, OaAccountExtDO> extMap = oaAccountExtDataService.loadExtMap(tenantId, mpIds);

        List<MpAccountDO> scopedRecords = page.getRecords();
        if (companyId != null || realnameId != null) {
            scopedRecords = scopedRecords.stream()
                    .filter(mp -> {
                        OaAccountExtDO ext = extMap.get(mp.getId());
                        if (ext == null) {
                            return companyId == null && realnameId == null;
                        }
                        if (companyId != null && !Objects.equals(ext.getCompanyId(), companyId)) {
                            return false;
                        }
                        return realnameId == null || Objects.equals(ext.getRealnameId(), realnameId);
                    })
                    .toList();
        }

        Map<Long, String> companyNames = loadCompanyNames(extMap.values());
        Map<Long, String> realNames = loadRealNames(extMap.values());

        List<AccountRespVO> list = scopedRecords.stream()
                .map(mp -> toResp(mp, extMap.get(mp.getId()), companyNames, realNames))
                .collect(Collectors.toList());
        // company/realname 仍为页内过滤时 total 取当前页结果；无附加过滤时用 DB 分页 total
        long total = (companyId != null || realnameId != null) ? list.size() : page.getTotal();
        return new PageResult<>(list, total);
    }

    @Override
    public AccountRespVO getWechatOfficial(Long mpAccountId) {
        Long tenantId = requireTenantId();
        AccountDO readable = wechatOfficialAccountResolver.requireTenantAccount(mpAccountId, tenantId);
        opsDataScopeSupport.assertAccountReadable(readable);
        MpAccountDO mp = mpAccountDataService.requireById(mpAccountId, tenantId);
        OaAccountExtDO ext = oaAccountExtDataService.findByMpAccountId(tenantId, mpAccountId);
        return toResp(mp, ext, loadCompanyNames(ext), loadRealNames(ext));
    }

    @Override
    public Long createWechatOfficial(AccountCreateReq req) {
        Long tenantId = requireTenantId();
        MpAccountDO mp = new MpAccountDO();
        mp.setTenantId(tenantId);
        mp.setName(req.getAccountName());
        mp.setAccount(req.getExternalAccountId());
        mp.setAppId(req.getAppId());
        if (StrUtil.isNotBlank(req.getAppSecret())) {
            mp.setAppSecret(aesUtil.encrypt(req.getAppSecret()));
        }
        mp.setRemark(null);
        mp.setStatus(mapOpsStatusToMp(req.getStatus()));
        mp.setCreator(TenantContextHolder.getUsername());
        mp.setUpdater(TenantContextHolder.getUsername());
        mp.setCreateTime(LocalDateTime.now());
        mp.setUpdateTime(LocalDateTime.now());
        mpAccountDataService.insert(mp);

        try {
            OaAccountExtDO ext = buildExtFromCreate(mp.getId(), tenantId, req);
            oaAccountExtDataService.insert(ext);
        } catch (Exception ex) {
            markExtError(tenantId, mp.getId(), ex.getMessage());
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "公众号扩展写入失败: " + ex.getMessage());
        }
        return mp.getId();
    }

    @Override
    public void updateWechatOfficial(AccountUpdateReq req) {
        Long tenantId = requireTenantId();
        OaAccountExtDO ext = oaAccountExtDataService.findByMpAccountId(tenantId, req.getId());
        MpAccountDO mp = mpAccountDataService.requireById(req.getId(), tenantId);
        if (req.getAccountName() != null) {
            mp.setName(req.getAccountName());
        }
        if (req.getAppId() != null) {
            mp.setAppId(req.getAppId());
        }
        if (StrUtil.isNotBlank(req.getAppSecret())) {
            mp.setAppSecret(aesUtil.encrypt(req.getAppSecret()));
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            mp.setStatus(mapOpsStatusToMp(req.getStatus()));
        }
        mp.setUpdater(TenantContextHolder.getUsername());
        mp.setUpdateTime(LocalDateTime.now());
        mpAccountDataService.updateById(mp);

        if (ext == null) {
            ext = new OaAccountExtDO();
            ext.setTenantId(tenantId);
            ext.setMpAccountId(mp.getId());
            ext.setPlatformType(PLATFORM_WECHAT_OFFICIAL);
            ext.setSyncStatus(SYNC_SYNCED);
            ext.setCreator(TenantContextHolder.getUsername());
            ext.setCreateTime(LocalDateTime.now());
        }
        applyExtFromUpdate(ext, req);
        ext.setUpdater(TenantContextHolder.getUsername());
        ext.setUpdateTime(LocalDateTime.now());
        ext.setSyncStatus(SYNC_SYNCED);
        ext.setSyncError(null);
        if (ext.getId() == null) {
            oaAccountExtDataService.insert(ext);
        } else {
            oaAccountExtDataService.updateById(ext);
        }
    }

    private OaAccountExtDO buildExtFromCreate(Long mpAccountId, Long tenantId, AccountCreateReq req) {
        OaAccountExtDO ext = new OaAccountExtDO();
        ext.setTenantId(tenantId);
        ext.setMpAccountId(mpAccountId);
        ext.setPlatformType(PLATFORM_WECHAT_OFFICIAL);
        ext.setCompanyId(req.getCompanyId());
        ext.setRealnameId(req.getRealnameId());
        ext.setIntermediaryId(req.getIntermediaryId());
        ext.setIpGroupId(req.getIpGroupId());
        ext.setPhoneId(req.getPhoneId());
        ext.setSimCardId(req.getSimCardId());
        if (StrUtil.isNotBlank(req.getCookie())) {
            ext.setCookieEncrypted(aesUtil.encrypt(req.getCookie()));
        }
        if (StrUtil.isNotBlank(req.getMpToken())) {
            ext.setMpTokenEncrypted(aesUtil.encrypt(req.getMpToken()));
        }
        ext.setTrademarkName(req.getTrademarkName());
        ext.setQualificationType(req.getQualificationType());
        ext.setUsageStatus(req.getUsageStatus());
        ext.setAdminUserId(resolveStorableAdminUserId(req.getAdminUserId(), tenantId));
        ext.setSyncStatus(SYNC_SYNCED);
        ext.setCreator(TenantContextHolder.getUsername());
        ext.setUpdater(TenantContextHolder.getUsername());
        ext.setCreateTime(LocalDateTime.now());
        ext.setUpdateTime(LocalDateTime.now());
        return ext;
    }

    private void applyExtFromUpdate(OaAccountExtDO ext, AccountUpdateReq req) {
        if (req.getCompanyId() != null) {
            ext.setCompanyId(req.getCompanyId());
        }
        if (req.getRealnameId() != null) {
            ext.setRealnameId(req.getRealnameId());
        }
        if (req.getIntermediaryId() != null) {
            ext.setIntermediaryId(req.getIntermediaryId());
        }
        if (req.getIpGroupId() != null) {
            ext.setIpGroupId(req.getIpGroupId());
        }
        if (req.getPhoneId() != null) {
            ext.setPhoneId(req.getPhoneId());
        }
        if (req.getSimCardId() != null) {
            ext.setSimCardId(req.getSimCardId());
        }
        if (req.getCookie() != null) {
            ext.setCookieEncrypted(StrUtil.isBlank(req.getCookie()) ? null : aesUtil.encrypt(req.getCookie()));
        }
        if (req.getMpToken() != null) {
            ext.setMpTokenEncrypted(StrUtil.isBlank(req.getMpToken()) ? null : aesUtil.encrypt(req.getMpToken()));
        }
        if (req.getTrademarkName() != null) {
            ext.setTrademarkName(req.getTrademarkName());
        }
        if (req.getQualificationType() != null) {
            ext.setQualificationType(req.getQualificationType());
        }
        if (req.getUsageStatus() != null) {
            ext.setUsageStatus(req.getUsageStatus());
        }
        if (req.getAdminUserId() != null) {
            ext.setAdminUserId(resolveStorableAdminUserId(req.getAdminUserId(), ext.getTenantId()));
        }
    }

    private AccountRespVO toResp(MpAccountDO mp, OaAccountExtDO ext,
                                 Map<Long, String> companyNames, Map<Long, String> realNames) {
        AccountRespVO vo = new AccountRespVO();
        vo.setId(mp.getId());
        vo.setPlatformType(PLATFORM_WECHAT_OFFICIAL);
        vo.setAccountName(mp.getName());
        vo.setExternalAccountId(mp.getAccount());
        vo.setAppId(mp.getAppId());
        vo.setHasAppSecret(StrUtil.isNotBlank(mp.getAppSecret()));
        vo.setStatus(mp.getStatus() != null && mp.getStatus() == 0 ? "NORMAL" : "DISABLED");
        vo.setCreateTime(mp.getCreateTime());
        vo.setLinkedAt(mp.getCreateTime());

        if (ext != null) {
            vo.setCompanyId(ext.getCompanyId());
            vo.setCompanyName(companyNames.get(ext.getCompanyId()));
            vo.setRealnameId(ext.getRealnameId());
            vo.setRealName(realNames.get(ext.getRealnameId()));
            vo.setIntermediaryId(ext.getIntermediaryId());
            vo.setIpGroupId(ext.getIpGroupId());
            vo.setPhoneId(ext.getPhoneId());
            vo.setSimCardId(ext.getSimCardId());
            vo.setHasCookie(StrUtil.isNotBlank(ext.getCookieEncrypted()));
            vo.setHasMpToken(StrUtil.isNotBlank(ext.getMpTokenEncrypted()));
            vo.setTrademarkName(ext.getTrademarkName());
            vo.setQualificationType(ext.getQualificationType());
            vo.setUsageStatus(ext.getUsageStatus());
            vo.setAdminUserId(ext.getAdminUserId());
            if (ext.getIpGroupId() != null) {
                IpGroupDO group = ipGroupMapper.selectById(ext.getIpGroupId());
                if (group != null) {
                    vo.setIpGroupName(group.getGroupName());
                }
            }
            if (ext.getAdminUserId() != null) {
                vo.setAdminUserName(footballSystemUserValidator.resolveDisplayName(ext.getAdminUserId()));
            }
        }
        return vo;
    }

    private Map<Long, String> loadCompanyNames(Iterable<OaAccountExtDO> exts) {
        List<Long> ids = streamIds(exts, OaAccountExtDO::getCompanyId);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return companyMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CompanyDO::getId, CompanyDO::getCompanyName, (a, b) -> a));
    }

    private Map<Long, String> loadRealNames(Iterable<OaAccountExtDO> exts) {
        List<Long> ids = streamIds(exts, OaAccountExtDO::getRealnameId);
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return realnameMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(RealnameDO::getId, RealnameDO::getRealName, (a, b) -> a));
    }

    private Map<Long, String> loadCompanyNames(OaAccountExtDO ext) {
        return ext == null ? Collections.emptyMap() : loadCompanyNames(List.of(ext));
    }

    private Map<Long, String> loadRealNames(OaAccountExtDO ext) {
        return ext == null ? Collections.emptyMap() : loadRealNames(List.of(ext));
    }

    private List<Long> streamIds(Iterable<OaAccountExtDO> exts, java.util.function.Function<OaAccountExtDO, Long> getter) {
        List<Long> ids = new java.util.ArrayList<>();
        for (OaAccountExtDO ext : exts) {
            if (ext != null && getter.apply(ext) != null) {
                ids.add(getter.apply(ext));
            }
        }
        return ids.stream().distinct().toList();
    }

    private void markExtError(Long tenantId, Long mpAccountId, String message) {
        OaAccountExtDO err = new OaAccountExtDO();
        err.setTenantId(tenantId);
        err.setMpAccountId(mpAccountId);
        err.setPlatformType(PLATFORM_WECHAT_OFFICIAL);
        err.setSyncStatus(SYNC_ERROR);
        err.setSyncError(StrUtil.sub(message, 0, 512));
        err.setCreator(TenantContextHolder.getUsername());
        err.setUpdater(TenantContextHolder.getUsername());
        err.setCreateTime(LocalDateTime.now());
        err.setUpdateTime(LocalDateTime.now());
        try {
            oaAccountExtDataService.insert(err);
        } catch (Exception ignored) {
            // best-effort
        }
    }

    private int mapOpsStatusToMp(String status) {
        if (StrUtil.isBlank(status) || "NORMAL".equals(status) || "ENABLED".equals(status)) {
            return 0;
        }
        return 1;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }

    private Long resolveStorableAdminUserId(Long adminUserId, Long tenantId) {
        if (adminUserId == null) {
            return null;
        }
        footballSystemUserValidator.assertEnabledInTenant(adminUserId, tenantId, "管理员用户不存在");
        return footballSystemUserValidator.resolveStorableUserId(adminUserId, tenantId);
    }
}
