package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishOptionsVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentPublishResultVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ContentPublishRecordDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ContentPublishRecordMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.content.publish.PlatformPublishAdapter;
import cn.iocoder.yudao.module.oa.service.content.publish.PlatformPublishResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContentPublishServiceImpl implements ContentPublishService {

    private static final Map<String, String> PLATFORM_LABELS = Map.of(
            "WECHAT_OFFICIAL", "公众号",
            "WECHAT_VIDEO", "视频号",
            "DOUYIN", "抖音",
            "KUAISHOU", "快手",
            "XIAOHONGSHU", "小红书");

    private final ProductionContentMapper productionContentMapper;
    private final AccountMapper accountMapper;
    private final ContentPublishRecordMapper publishRecordMapper;
    private final List<PlatformPublishAdapter> publishAdapters;

    @Override
    public ContentPublishOptionsVO getPublishOptions(Long contentId) {
        ProductionContentDO content = requireContent(contentId);
        if (!"PENDING_PUBLISH".equals(content.getStatus())) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        Long tenantId = content.getTenantId();
        List<AccountDO> accounts = accountMapper.selectList(
                new LambdaQueryWrapper<AccountDO>()
                        .eq(AccountDO::getTenantId, tenantId)
                        .eq(AccountDO::getStatus, "NORMAL")
                        .eq(AccountDO::getPublishEnabled, 1)
                        .orderByAsc(AccountDO::getPlatformType, AccountDO::getId));

        Map<String, ContentPublishOptionsVO.PlatformOption> platformMap = new LinkedHashMap<>();
        for (AccountDO account : accounts) {
            ContentPublishOptionsVO.PlatformOption platform = platformMap.computeIfAbsent(
                    account.getPlatformType(), pt -> {
                        ContentPublishOptionsVO.PlatformOption p = new ContentPublishOptionsVO.PlatformOption();
                        p.setPlatformType(pt);
                        p.setPlatformName(platformLabel(pt));
                        p.setAccounts(new ArrayList<>());
                        return p;
                    });
            ContentPublishOptionsVO.AccountOption opt = new ContentPublishOptionsVO.AccountOption();
            opt.setId(account.getId());
            opt.setAccountName(account.getAccountName());
            opt.setExternalAccountId(account.getExternalAccountId());
            opt.setPublishEnabled(Boolean.TRUE);
            platform.getAccounts().add(opt);
        }

        ContentPublishOptionsVO vo = new ContentPublishOptionsVO();
        vo.setPlatforms(new ArrayList<>(platformMap.values()));
        return vo;
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-content", action = "publish")
    public ContentPublishResultVO publish(Long contentId, ContentPublishReq req) {
        ProductionContentDO content = requireContent(contentId);
        if (!"PENDING_PUBLISH".equals(content.getStatus())) {
            throw new ServiceException(OaErrorCodes.CONTENT_STATUS_INVALID);
        }
        Long tenantId = content.getTenantId();
        String platformType = req.getPlatformType();
        List<Long> accountIds = normalizeAccountIds(req.getAccountIds());
        if (accountIds.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "请至少选择一个发布账号");
        }

        PlatformPublishAdapter adapter = resolveAdapter(platformType);
        List<ContentPublishResultVO.RecordItem> recordItems = new ArrayList<>();
        boolean anyMock = false;
        boolean allSuccess = true;

        for (Long accountId : accountIds) {
            AccountDO account = requirePublishableAccount(tenantId, accountId, platformType);
            PlatformPublishResult adapterResult = adapter.publish(content, account);
            ContentPublishRecordDO record = savePublishRecord(content, account, adapterResult);
            ContentPublishResultVO.RecordItem item = toRecordItem(account, record);
            recordItems.add(item);
            if (!adapterResult.isSuccess()) {
                allSuccess = false;
            } else if (adapterResult.isMock()) {
                anyMock = true;
            }
        }

        if (!allSuccess) {
            throw new ServiceException(OaErrorCodes.CONTENT_PUBLISH_FAILED.getCode(),
                    "部分账号发布失败，请查看发布记录");
        }

        content.setStatus("PUBLISHED");
        content.setPlatformType(platformType);
        content.setPlatformTypesJson(ContentJsonHelper.toPlatformTypesJson(List.of(platformType)));
        content.setAccountId(accountIds.get(0));
        content.setAccountIdsJson(ContentJsonHelper.toAccountIdsJson(accountIds));
        content.setUpdater(TenantContextHolder.getUsername());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.updateById(content);

        ContentPublishResultVO vo = new ContentPublishResultVO();
        vo.setContentId(contentId);
        vo.setStatus("PUBLISHED");
        vo.setMock(anyMock);
        vo.setRecords(recordItems);
        return vo;
    }

    private PlatformPublishAdapter resolveAdapter(String platformType) {
        for (PlatformPublishAdapter adapter : publishAdapters) {
            if (adapter.supports(platformType)) {
                return adapter;
            }
        }
        throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                "暂不支持该平台发布：" + platformType);
    }

    private AccountDO requirePublishableAccount(Long tenantId, Long accountId, String platformType) {
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null || !Objects.equals(account.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!"NORMAL".equals(account.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        if (!platformType.equals(account.getPlatformType())) {
            throw new ServiceException(OaErrorCodes.CONTENT_PLATFORM_MISMATCH);
        }
        if (account.getPublishEnabled() == null || account.getPublishEnabled() != 1) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED.getCode(), "账号未配置发布权限");
        }
        return account;
    }

    private ContentPublishRecordDO savePublishRecord(ProductionContentDO content, AccountDO account,
                                                     PlatformPublishResult result) {
        ContentPublishRecordDO record = new ContentPublishRecordDO();
        record.setTenantId(content.getTenantId());
        record.setContentId(content.getId());
        record.setAccountId(account.getId());
        record.setPlatformType(account.getPlatformType());
        record.setStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
        record.setExternalId(result.getExternalId());
        record.setErrorMessage(result.getErrorMessage());
        record.setPublishedAt(result.isSuccess() ? LocalDateTime.now() : null);
        record.setCreator(TenantContextHolder.getUsername());
        record.setUpdater(TenantContextHolder.getUsername());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        publishRecordMapper.insert(record);
        return record;
    }

    private ContentPublishResultVO.RecordItem toRecordItem(AccountDO account, ContentPublishRecordDO record) {
        ContentPublishResultVO.RecordItem item = new ContentPublishResultVO.RecordItem();
        item.setAccountId(account.getId());
        item.setAccountName(account.getAccountName());
        item.setPlatformType(record.getPlatformType());
        item.setStatus(record.getStatus());
        item.setExternalId(record.getExternalId());
        item.setErrorMessage(record.getErrorMessage());
        item.setPublishedAt(record.getPublishedAt());
        return item;
    }

    private List<Long> normalizeAccountIds(List<Long> accountIds) {
        if (accountIds == null || accountIds.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long id : accountIds) {
            if (id != null) {
                unique.add(id);
            }
        }
        return new ArrayList<>(unique);
    }

    private ProductionContentDO requireContent(Long id) {
        ProductionContentDO entity = productionContentMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        if (!Objects.equals(entity.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private String platformLabel(String platformType) {
        return PLATFORM_LABELS.getOrDefault(platformType, platformType);
    }
}
