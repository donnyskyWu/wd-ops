package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelDataVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricPreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.MetricPreviewVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.FunnelDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.FunnelStepDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.bridging.PrivateDomainConversionBridgeDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.AochuangFriendDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.MetricDO;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.FunnelMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.FunnelStepMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.bridging.PrivateDomainConversionBridgeMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.AochuangFriendMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.MetricMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.support.OaTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FunnelServiceImpl implements FunnelService {

    private static final String REVIEW_APPROVED = "APPROVED";
    private static final String IDENTITY_AOCHUANG_FRIEND = "AOCHUANG_FRIEND";
    private static final String IDENTITY_PHONE = "PHONE";
    private static final String IDENTITY_MP_FOLLOWER = "MP_FOLLOWER";

    private final FunnelMapper funnelMapper;
    private final FunnelStepMapper funnelStepMapper;
    private final ContentMapper contentMapper;
    private final FollowerDailyMapper followerDailyMapper;
    private final MetricMapper metricMapper;
    private final AnalyticsMetricService analyticsMetricService;
    private final AochuangFriendMapper aochuangFriendMapper;
    private final PrivateDomainConversionBridgeMapper bridgeMapper;

    @Override
    public PageResult<FunnelVO> list(Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        Page<FunnelDO> page = funnelMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize),
                new LambdaQueryWrapper<FunnelDO>().eq(FunnelDO::getTenantId, tenantId).orderByDesc(FunnelDO::getId));
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M6-funnel", action = "create")
    public Long create(FunnelCreateReq req) {
        Long tenantId = requireTenantId();
        FunnelDO funnel = new FunnelDO();
        funnel.setTenantId(tenantId);
        funnel.setFunnelName(req.getFunnelName());
        funnel.setFunnelType(req.getFunnelType());
        funnel.setStatus(1);
        funnel.setCreator(TenantContextHolder.getUsername());
        funnel.setUpdater(TenantContextHolder.getUsername());
        funnelMapper.insert(funnel);
        for (FunnelCreateReq.FunnelStepReq step : req.getSteps()) {
            FunnelStepDO entity = new FunnelStepDO();
            entity.setFunnelId(funnel.getId());
            entity.setStepOrder(step.getStepOrder());
            entity.setEventCode(step.getEventCode());
            entity.setStepName(step.getStepName());
            entity.setCreator(TenantContextHolder.getUsername());
            entity.setUpdater(TenantContextHolder.getUsername());
            funnelStepMapper.insert(entity);
        }
        return funnel.getId();
    }

    @Override
    public FunnelDataVO getData(Long id, LocalDate startDate, LocalDate endDate, Long ipGroupId, String platformType) {
        FunnelDO funnel = getRequired(id);
        List<FunnelStepDO> steps = funnelStepMapper.selectList(new LambdaQueryWrapper<FunnelStepDO>()
                .eq(FunnelStepDO::getFunnelId, id)
                .orderByAsc(FunnelStepDO::getStepOrder));
        Long tenantId = funnel.getTenantId();
        List<ContentDO> contents = queryContents(tenantId, startDate, endDate, platformType);

        FunnelDataVO vo = new FunnelDataVO();
        vo.setFunnelId(id);
        long base = 0;
        for (int i = 0; i < steps.size(); i++) {
            FunnelStepDO step = steps.get(i);
            FunnelDataVO.FunnelStepDataVO item = new FunnelDataVO.FunnelStepDataVO();
            item.setStepOrder(step.getStepOrder());
            item.setName(step.getStepName() != null ? step.getStepName() : step.getEventCode());
            long count = resolveStepCount(tenantId, step.getEventCode(), contents, startDate, endDate);
            item.setCount(count);
            if (i == 0) {
                base = count;
                item.setConversionRate(base == 0 ? 0.0 : 100.0);
            } else {
                item.setConversionRate(base == 0 ? 0.0 : Math.round(count * 10000.0 / base) / 100.0);
            }
            vo.getSteps().add(item);
        }
        vo.getSteps().sort(Comparator.comparing(FunnelDataVO.FunnelStepDataVO::getStepOrder));
        return vo;
    }

    @Override
    @AuditLog(module = "M6-funnel", action = "export")
    public ExportJobVO export(Long id) {
        getRequired(id);
        return OaTenantSupport.stubExportJob();
    }

    private List<ContentDO> queryContents(Long tenantId, LocalDate startDate, LocalDate endDate, String platformType) {
        LambdaQueryWrapper<ContentDO> wrapper = new LambdaQueryWrapper<ContentDO>()
                .eq(ContentDO::getTenantId, tenantId);
        if (platformType != null && !platformType.isBlank()) {
            wrapper.eq(ContentDO::getPlatformType, platformType);
        }
        if (startDate != null) {
            wrapper.ge(ContentDO::getPublishTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(ContentDO::getPublishTime, endDate.plusDays(1).atStartOfDay());
        }
        return contentMapper.selectList(wrapper);
    }

    private long resolveStepCount(Long tenantId, String eventCode, List<ContentDO> contents,
                                  LocalDate startDate, LocalDate endDate) {
        if (eventCode == null || eventCode.isBlank()) {
            return 0;
        }
        MetricDO metric = metricMapper.selectOne(new LambdaQueryWrapper<MetricDO>()
                .eq(MetricDO::getTenantId, tenantId)
                .eq(MetricDO::getMetricCode, eventCode)
                .last("LIMIT 1"));
        if (metric != null && metric.getMetricFormula() != null && !metric.getMetricFormula().isBlank()) {
            return executeMetricScalar(metric.getMetricFormula());
        }
        String code = eventCode.toUpperCase(Locale.ROOT);
        return switch (code) {
            case "VIEW", "EXPOSURE" -> contents.size();
            case "READ" -> contents.stream().mapToLong(c -> c.getReadCount() == null ? 0 : c.getReadCount()).sum();
            case "LIKE" -> contents.stream().mapToLong(c -> c.getLikeCount() == null ? 0 : c.getLikeCount()).sum();
            case "SHARE", "FORWARD" -> contents.stream().mapToLong(c -> c.getForwardCount() == null ? 0 : c.getForwardCount()).sum();
            case "FOLLOW", "REVISIT" -> followerDailyMapper.selectList(new LambdaQueryWrapper<FollowerDailyDO>()
                            .eq(FollowerDailyDO::getTenantId, tenantId)
                            .ge(startDate != null, FollowerDailyDO::getStatDate, startDate)
                            .le(endDate != null, FollowerDailyDO::getStatDate, endDate)
                            .orderByDesc(FollowerDailyDO::getFollowerCount)
                            .last("LIMIT 1"))
                    .stream().mapToLong(FollowerDailyDO::getFollowerCount).findFirst().orElse(0L);
            case "AOCHUANG_FRIEND" -> countAochuangFriends(tenantId, startDate, endDate);
            case "PD_BRIDGE_APPROVED" -> countApprovedBridges(tenantId, IDENTITY_AOCHUANG_FRIEND, null, startDate, endDate);
            case "PD_BRIDGE_PHONE" -> countApprovedBridges(tenantId, IDENTITY_AOCHUANG_FRIEND, IDENTITY_PHONE, startDate, endDate);
            case "PD_BRIDGE_MP_FOLLOWER" -> countApprovedBridges(tenantId, IDENTITY_AOCHUANG_FRIEND, IDENTITY_MP_FOLLOWER, startDate, endDate);
            default -> contents.size();
        };
    }

    private long countAochuangFriends(Long tenantId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AochuangFriendDO> wrapper = new LambdaQueryWrapper<AochuangFriendDO>()
                .eq(AochuangFriendDO::getTenantId, tenantId);
        applyFriendDateRange(wrapper, startDate, endDate);
        return aochuangFriendMapper.selectCount(wrapper);
    }

    private long countApprovedBridges(Long tenantId, String sourceType, String targetType,
                                      LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<PrivateDomainConversionBridgeDO> wrapper = new LambdaQueryWrapper<PrivateDomainConversionBridgeDO>()
                .eq(PrivateDomainConversionBridgeDO::getTenantId, tenantId)
                .eq(PrivateDomainConversionBridgeDO::getReviewStatus, REVIEW_APPROVED)
                .eq(PrivateDomainConversionBridgeDO::getSourceType, sourceType);
        if (targetType != null) {
            wrapper.eq(PrivateDomainConversionBridgeDO::getTargetType, targetType);
        }
        applyBridgeDateRange(wrapper, startDate, endDate);
        return bridgeMapper.selectCount(wrapper);
    }

    private static void applyFriendDateRange(LambdaQueryWrapper<AochuangFriendDO> wrapper,
                                             LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            wrapper.and(w -> w.ge(AochuangFriendDO::getSyncedAt, start)
                    .or(n -> n.isNull(AochuangFriendDO::getSyncedAt).ge(AochuangFriendDO::getCreateTime, start)));
        }
        if (endDate != null) {
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            wrapper.and(w -> w.lt(AochuangFriendDO::getSyncedAt, end)
                    .or(n -> n.isNull(AochuangFriendDO::getSyncedAt).lt(AochuangFriendDO::getCreateTime, end)));
        }
    }

    private static void applyBridgeDateRange(LambdaQueryWrapper<PrivateDomainConversionBridgeDO> wrapper,
                                             LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            wrapper.and(w -> w.ge(PrivateDomainConversionBridgeDO::getLinkedAt, start)
                    .or(n -> n.isNull(PrivateDomainConversionBridgeDO::getLinkedAt)
                            .ge(PrivateDomainConversionBridgeDO::getCreateTime, start)));
        }
        if (endDate != null) {
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            wrapper.and(w -> w.lt(PrivateDomainConversionBridgeDO::getLinkedAt, end)
                    .or(n -> n.isNull(PrivateDomainConversionBridgeDO::getLinkedAt)
                            .lt(PrivateDomainConversionBridgeDO::getCreateTime, end)));
        }
    }

    private long executeMetricScalar(String formula) {
        MetricPreviewReq req = new MetricPreviewReq();
        req.setMetricFormula(formula);
        MetricPreviewVO preview = analyticsMetricService.preview(req);
        if (preview.getRows() == null || preview.getRows().isEmpty()) {
            return 0L;
        }
        Map<String, Object> row = preview.getRows().get(0);
        for (Object val : row.values()) {
            if (val instanceof Number number) {
                return number.longValue();
            }
        }
        return 0L;
    }

    private FunnelDO getRequired(Long id) {
        FunnelDO entity = funnelMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!Objects.equals(entity.getTenantId(), requireTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private FunnelVO toVO(FunnelDO row) {
        FunnelVO vo = new FunnelVO();
        vo.setId(row.getId());
        vo.setFunnelName(row.getFunnelName());
        vo.setFunnelType(row.getFunnelType());
        vo.setStatus(row.getStatus());
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
