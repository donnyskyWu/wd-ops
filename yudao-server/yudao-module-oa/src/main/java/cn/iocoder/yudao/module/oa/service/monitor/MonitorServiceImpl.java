package cn.iocoder.yudao.module.oa.service.monitor;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.monitor.ExternalWorkVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.monitor.ExternalWorkDO;
import cn.iocoder.yudao.module.oa.dal.mysql.monitor.ExternalWorkMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private static final long HIT_PLAY_THRESHOLD = 1_000_000L;
    private static final BigDecimal LOW_SCORE_THRESHOLD = new BigDecimal("0.20");

    private final ExternalWorkMapper externalWorkMapper;

    @Override
    public PageResult<ExternalWorkVO> externalList(String platformType, Long ipGroupId, String industry,
                                                   LocalDate startDate, LocalDate endDate,
                                                   Integer pageNum, Integer pageSize) {
        return query(buildBaseWrapper(platformType, ipGroupId, industry, startDate, endDate), pageNum, pageSize);
    }

    @Override
    public PageResult<ExternalWorkVO> hitList(String platformType, Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                              Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ExternalWorkDO> wrapper = buildBaseWrapper(platformType, ipGroupId, null, startDate, endDate);
        wrapper.ge(ExternalWorkDO::getPlayCount, HIT_PLAY_THRESHOLD);
        return query(wrapper, pageNum, pageSize);
    }

    @Override
    public PageResult<ExternalWorkVO> lowScoreList(String platformType, Long ipGroupId, LocalDate startDate, LocalDate endDate,
                                                   Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ExternalWorkDO> wrapper = buildBaseWrapper(platformType, ipGroupId, null, startDate, endDate);
        wrapper.lt(ExternalWorkDO::getCompletionRate, LOW_SCORE_THRESHOLD);
        return query(wrapper, pageNum, pageSize);
    }

    @Override
    public PageResult<ExternalWorkVO> highFollowerList(Long ipGroupId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ExternalWorkDO> wrapper = buildBaseWrapper(null, ipGroupId, null, null, null);
        wrapper.orderByDesc(ExternalWorkDO::getPlayCount);
        return query(wrapper, pageNum, pageSize);
    }

    @Override
    public PageResult<ExternalWorkVO> lowFollowerList(Long ipGroupId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ExternalWorkDO> wrapper = buildBaseWrapper(null, ipGroupId, null, null, null);
        wrapper.orderByAsc(ExternalWorkDO::getPlayCount);
        return query(wrapper, pageNum, pageSize);
    }

    @Override
    public Map<String, Object> ipTheme(Long id) {
        List<ExternalWorkDO> works = externalWorkMapper.selectList(new LambdaQueryWrapper<ExternalWorkDO>()
                .eq(ExternalWorkDO::getTenantId, requireTenantId())
                .eq(ExternalWorkDO::getIpGroupId, id));
        Map<String, Object> result = new HashMap<>();
        result.put("ipGroupId", id);
        result.put("workCount", works.size());
        result.put("totalPlay", works.stream().mapToLong(w -> w.getPlayCount() == null ? 0 : w.getPlayCount()).sum());
        result.put("topTitles", works.stream().limit(5).map(ExternalWorkDO::getTitle).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, Object> industryStats(Long id) {
        List<ExternalWorkDO> works = externalWorkMapper.selectList(new LambdaQueryWrapper<ExternalWorkDO>()
                .eq(ExternalWorkDO::getTenantId, requireTenantId())
                .eq(ExternalWorkDO::getAccountId, id));
        Map<String, Object> result = new HashMap<>();
        result.put("accountId", id);
        result.put("industries", works.stream().map(ExternalWorkDO::getIndustry).distinct().collect(Collectors.toList()));
        result.put("workCount", works.size());
        return result;
    }

    private PageResult<ExternalWorkVO> query(LambdaQueryWrapper<ExternalWorkDO> wrapper, Integer pageNum, Integer pageSize) {
        Page<ExternalWorkDO> page = externalWorkMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        return new PageResult<>(page.getRecords().stream().map(this::toVO).collect(Collectors.toList()), page.getTotal());
    }

    private LambdaQueryWrapper<ExternalWorkDO> buildBaseWrapper(String platformType, Long ipGroupId, String industry,
                                                              LocalDate startDate, LocalDate endDate) {
        return new LambdaQueryWrapper<ExternalWorkDO>()
                .eq(ExternalWorkDO::getTenantId, requireTenantId())
                .eq(ExternalWorkDO::getIsExternal, 1)
                .eq(platformType != null, ExternalWorkDO::getPlatformType, platformType)
                .eq(ipGroupId != null, ExternalWorkDO::getIpGroupId, ipGroupId)
                .eq(industry != null, ExternalWorkDO::getIndustry, industry)
                .ge(startDate != null, ExternalWorkDO::getPublishTime, startDate == null ? null : startDate.atStartOfDay())
                .le(endDate != null, ExternalWorkDO::getPublishTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(ExternalWorkDO::getPublishTime);
    }

    private ExternalWorkVO toVO(ExternalWorkDO row) {
        ExternalWorkVO vo = new ExternalWorkVO();
        vo.setId(row.getId());
        vo.setAccountId(row.getAccountId());
        vo.setPlatformType(row.getPlatformType());
        vo.setTitle(row.getTitle());
        vo.setWorkUrl(row.getWorkUrl());
        vo.setPlayCount(row.getPlayCount());
        vo.setCompletionRate(row.getCompletionRate());
        vo.setLikeCount(row.getLikeCount());
        vo.setPublishTime(row.getPublishTime());
        vo.setIndustry(row.getIndustry());
        vo.setIpGroupId(row.getIpGroupId());
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
