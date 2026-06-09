package cn.iocoder.yudao.module.oa.service.perf;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.perf.ExportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfExportReq;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfResultVO;
import cn.iocoder.yudao.module.oa.api.dto.perf.PerfTrendVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfRecordDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfRecordMapper;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfResultServiceImpl implements PerfResultService {

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final PerfRecordMapper perfRecordMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<PerfResultVO> list(Long userId, String periodType, String grade, LocalDate startDate,
                                         Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        Long effectiveUserId = resolveUserScope(userId);
        LambdaQueryWrapper<PerfRecordDO> wrapper = new LambdaQueryWrapper<PerfRecordDO>()
                .eq(PerfRecordDO::getTenantId, tenantId)
                .eq(PerfRecordDO::getStatus, "CONFIRMED")
                .eq(effectiveUserId != null, PerfRecordDO::getTargetUserId, effectiveUserId)
                .eq(periodType != null && !periodType.isBlank(), PerfRecordDO::getPeriodType, periodType)
                .eq(grade != null && !grade.isBlank(), PerfRecordDO::getGrade, grade)
                .ge(startDate != null, PerfRecordDO::getPeriodStart, startDate)
                .orderByDesc(PerfRecordDO::getPeriodStart);
        Page<PerfRecordDO> page = perfRecordMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        Map<Long, String> userNames = sysUserMapper.selectBatchIds(
                        page.getRecords().stream().map(PerfRecordDO::getTargetUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(SysUserDO::getId, SysUserDO::getNickname, (a, b) -> a));
        return new PageResult<>(page.getRecords().stream()
                .map(record -> toResultVO(record, userNames))
                .collect(Collectors.toList()), page.getTotal());
    }

    @Override
    public PerfTrendVO trend(Long userId, Integer month) {
        requireTenantId();
        int months = month == null || month <= 0 ? 6 : month;
        List<PerfRecordDO> records = perfRecordMapper.selectList(new LambdaQueryWrapper<PerfRecordDO>()
                .eq(PerfRecordDO::getTargetUserId, userId)
                .eq(PerfRecordDO::getStatus, "CONFIRMED")
                .orderByDesc(PerfRecordDO::getPeriodStart)
                .last("LIMIT " + months));
        PerfTrendVO vo = new PerfTrendVO();
        vo.setUserId(userId);
        vo.setTrends(records.stream().map(record -> {
            PerfTrendVO.TrendPoint point = new PerfTrendVO.TrendPoint();
            point.setPeriod(record.getPeriodStart().format(PERIOD_FMT));
            point.setTotalScore(record.getTotalScore());
            point.setGrade(record.getGrade());
            return point;
        }).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public ExportJobVO export(PerfExportReq req) {
        ExportJobVO vo = new ExportJobVO();
        vo.setJobId("perf-export-" + UUID.randomUUID());
        return vo;
    }

    private PerfResultVO toResultVO(PerfRecordDO record, Map<Long, String> userNames) {
        PerfResultVO vo = new PerfResultVO();
        vo.setId(record.getId());
        vo.setUserId(record.getTargetUserId());
        vo.setUserName(userNames.get(record.getTargetUserId()));
        vo.setPeriodType(record.getPeriodType());
        vo.setPeriodStart(record.getPeriodStart());
        vo.setPeriodEnd(record.getPeriodEnd());
        vo.setTotalScore(record.getTotalScore());
        vo.setGrade(record.getGrade());
        return vo;
    }

    private Long resolveUserScope(Long userId) {
        if (userId != null) {
            return userId;
        }
        var loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return null;
        }
        if (loginUser.getAuthorities().contains("oa:tenant:create")
                || loginUser.getAuthorities().contains("oa:role:list")) {
            return null;
        }
        return loginUser.getUserId();
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }
}
