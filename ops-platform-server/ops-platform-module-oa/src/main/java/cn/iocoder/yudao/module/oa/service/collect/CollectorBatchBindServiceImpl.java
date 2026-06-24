package cn.iocoder.yudao.module.oa.service.collect;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorBatchBindImportRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectorBatchBindImportRespVO.CollectorBatchBindImportItemVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectorAccountBindDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectorAccountBindMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.collect.unified.CollectorCredentialBuilder;
import cn.iocoder.yudao.module.oa.service.collect.unified.UnifiedCollectorAdapter;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CollectorBatchBindServiceImpl implements CollectorBatchBindService {

    private static final Set<String> CHANNEL_A_PLATFORMS = Set.of(
            "WECHAT_OFFICIAL", "WECHAT_VIDEO", "DOUYIN", "KUAISHOU", "XIAOHONGSHU", "BILIBILI");

    private final AccountMapper accountMapper;
    private final CollectorAccountBindMapper collectorAccountBindMapper;
    private final CollectorCredentialBuilder credentialBuilder;
    private final UnifiedCollectorAdapter unifiedCollectorAdapter;

    @Override
    @Transactional
    @AuditLog(module = "M10-collector-bind", action = "batch-import")
    public CollectorBatchBindImportRespVO batchImport() {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        Set<Long> boundAccountIds = loadBoundAccountIds(tenantId);

        List<AccountDO> candidates = accountMapper.selectList(new LambdaQueryWrapper<AccountDO>()
                .eq(AccountDO::getTenantId, tenantId)
                .in(AccountDO::getPlatformType, CHANNEL_A_PLATFORMS)
                .notIn(!boundAccountIds.isEmpty(), AccountDO::getId, boundAccountIds));

        CollectorBatchBindImportRespVO resp = new CollectorBatchBindImportRespVO();
        resp.setScanned(candidates.size());

        for (AccountDO account : candidates) {
            if (!hasImportableCredentials(account)) {
                resp.setSkipped(resp.getSkipped() + 1);
                resp.getItems().add(item(account, "SKIPPED", "凭证不完整或平台不支持"));
                continue;
            }
            try {
                unifiedCollectorAdapter.bindAccount(account.getId());
                resp.setImported(resp.getImported() + 1);
                resp.getItems().add(item(account, "IMPORTED", "绑定成功"));
            } catch (ServiceException ex) {
                resp.setFailed(resp.getFailed() + 1);
                resp.getItems().add(item(account, "FAILED", ex.getMessage()));
            } catch (Exception ex) {
                resp.setFailed(resp.getFailed() + 1);
                resp.getItems().add(item(account, "FAILED", "绑定失败: " + ex.getMessage()));
            }
        }
        return resp;
    }

    private Set<Long> loadBoundAccountIds(Long tenantId) {
        List<CollectorAccountBindDO> binds = collectorAccountBindMapper.selectList(
                new LambdaQueryWrapper<CollectorAccountBindDO>()
                        .eq(CollectorAccountBindDO::getTenantId, tenantId)
                        .select(CollectorAccountBindDO::getOaAccountId));
        Set<Long> ids = new HashSet<>();
        for (CollectorAccountBindDO bind : binds) {
            ids.add(bind.getOaAccountId());
        }
        return ids;
    }

    private boolean hasImportableCredentials(AccountDO account) {
        if (account == null || StrUtil.isBlank(account.getPlatformType())) {
            return false;
        }
        if (!CHANNEL_A_PLATFORMS.contains(account.getPlatformType())) {
            return false;
        }
        try {
            credentialBuilder.buildCredential(account);
            return true;
        } catch (ServiceException ex) {
            return false;
        }
    }

    private CollectorBatchBindImportItemVO item(AccountDO account, String result, String message) {
        CollectorBatchBindImportItemVO item = new CollectorBatchBindImportItemVO();
        item.setOaAccountId(account.getId());
        item.setPlatformType(account.getPlatformType());
        item.setResult(result);
        item.setMessage(message);
        return item;
    }
}
