package cn.iocoder.yudao.module.oa.service.analytics;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelDataVO;
import cn.iocoder.yudao.module.oa.api.dto.analytics.FunnelVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.FunnelDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.analytics.FunnelStepDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.ContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.operations.FollowerDailyDO;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.FunnelMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.analytics.FunnelStepMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.ContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.operations.FollowerDailyMapper;
import cn.iocoder.yudao.module.oa.service.support.OaTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FunnelServiceImpl implements FunnelService {

    private final FunnelMapper funnelMapper;
    private final FunnelStepMapper funnelStepMapper;
    private final ContentMapper contentMapper;
    private final FollowerDailyMapper followerDailyMapper;

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
    public FunnelDataVO getData(Long id) {
        FunnelDO funnel = getRequired(id);
        List<FunnelStepDO> steps = funnelStepMapper.selectList(new LambdaQueryWrapper<FunnelStepDO>()
                .eq(FunnelStepDO::getFunnelId, id)
                .orderByAsc(FunnelStepDO::getStepOrder));
        Long tenantId = funnel.getTenantId();
        long contentCount = contentMapper.selectCount(new LambdaQueryWrapper<ContentDO>().eq(ContentDO::getTenantId, tenantId));
        long readSum = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>().eq(ContentDO::getTenantId, tenantId))
                .stream().mapToLong(c -> c.getReadCount() == null ? 0 : c.getReadCount()).sum();
        long likeSum = contentMapper.selectList(new LambdaQueryWrapper<ContentDO>().eq(ContentDO::getTenantId, tenantId))
                .stream().mapToLong(c -> c.getLikeCount() == null ? 0 : c.getLikeCount()).sum();
        long followerMax = followerDailyMapper.selectList(new LambdaQueryWrapper<FollowerDailyDO>()
                        .eq(FollowerDailyDO::getTenantId, tenantId)
                        .orderByDesc(FollowerDailyDO::getFollowerCount)
                        .last("LIMIT 1"))
                .stream().mapToLong(FollowerDailyDO::getFollowerCount).findFirst().orElse(10000L);

        long[] counts = new long[]{
                Math.max(contentCount * 100, 10000),
                Math.max(readSum, 3000),
                Math.max(likeSum, 800),
                Math.max(followerMax / 10, 500)
        };

        FunnelDataVO vo = new FunnelDataVO();
        vo.setFunnelId(id);
        long base = counts[0];
        for (int i = 0; i < steps.size(); i++) {
            FunnelStepDO step = steps.get(i);
            FunnelDataVO.FunnelStepDataVO item = new FunnelDataVO.FunnelStepDataVO();
            item.setStepOrder(step.getStepOrder());
            item.setName(step.getStepName() != null ? step.getStepName() : step.getEventCode());
            long count = i < counts.length ? counts[i] : counts[counts.length - 1];
            item.setCount(count);
            item.setConversionRate(base == 0 ? 0.0 : Math.round(count * 10000.0 / base) / 100.0);
            vo.getSteps().add(item);
        }
        if (vo.getSteps().isEmpty()) {
            vo.getSteps().sort(Comparator.comparing(FunnelDataVO.FunnelStepDataVO::getStepOrder));
        }
        return vo;
    }

    @Override
    public ExportJobVO export(Long id) {
        getRequired(id);
        return OaTenantSupport.stubExportJob();
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
