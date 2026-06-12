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
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfItemRecordDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfRecordDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.perf.PerfTemplateDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfItemRecordMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfRecordMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.perf.PerfTemplateMapper;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerfResultServiceImpl implements PerfResultService {

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter JOIN_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal DEFAULT_BASE_SCORE = new BigDecimal("60");

    private final PerfRecordMapper perfRecordMapper;
    private final PerfItemRecordMapper perfItemRecordMapper;
    private final PerfTemplateMapper perfTemplateMapper;
    private final SysUserMapper sysUserMapper;
    private final IpGroupMapper ipGroupMapper;

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
        Long tenantId = requireTenantId();
        int months = month == null || month <= 0 ? 6 : month;
        SysUserDO user = requireUserInTenant(userId, tenantId);
        List<PerfRecordDO> records = perfRecordMapper.selectList(new LambdaQueryWrapper<PerfRecordDO>()
                .eq(PerfRecordDO::getTenantId, tenantId)
                .eq(PerfRecordDO::getTargetUserId, userId)
                .eq(PerfRecordDO::getStatus, "CONFIRMED")
                .orderByDesc(PerfRecordDO::getPeriodStart)
                .last("LIMIT " + months));
        Map<Long, PerfTemplateDO> templates = loadTemplates(records);
        Map<Long, List<PerfItemRecordDO>> itemsByRecord = loadItemRecords(records);
        String dept = resolveDeptName(user.getIpGroupId());

        PerfTrendVO.UserInfo userInfo = new PerfTrendVO.UserInfo();
        userInfo.setId(userId);
        userInfo.setName(user.getNickname());
        userInfo.setPosition(user.getPosition());
        userInfo.setDept(dept);
        userInfo.setJoinAt(user.getCreateTime() != null ? user.getCreateTime().toLocalDate().format(JOIN_FMT) : null);

        List<PerfTrendVO.TrendPoint> points = records.stream()
                .map(record -> toTrendPoint(record, templates.get(record.getTemplateId()),
                        itemsByRecord.getOrDefault(record.getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        PerfTrendVO vo = new PerfTrendVO();
        vo.setUserId(userId);
        vo.setUserName(user.getNickname());
        vo.setPosition(user.getPosition());
        vo.setDept(dept);
        vo.setJoinAt(userInfo.getJoinAt());
        vo.setUserInfo(userInfo);
        vo.setTrends(points);
        vo.setPoints(points);
        return vo;
    }

    @Override
    @AuditLog(module = "M3-perf", action = "export-result")
    public ExportJobVO export(PerfExportReq req) {
        ExportJobVO vo = new ExportJobVO();
        vo.setJobId("perf-export-" + UUID.randomUUID());
        return vo;
    }

    private PerfTrendVO.TrendPoint toTrendPoint(PerfRecordDO record, PerfTemplateDO template,
                                                List<PerfItemRecordDO> itemRecords) {
        BigDecimal bonus = itemRecords.stream()
                .map(PerfItemRecordDO::getManualAdjustment)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = record.getTotalScore() != null ? record.getTotalScore() : BigDecimal.ZERO;
        PerfTrendVO.TrendPoint point = new PerfTrendVO.TrendPoint();
        point.setPeriod(record.getPeriodStart().format(PERIOD_FMT));
        point.setTotalScore(total);
        point.setScore(total);
        point.setGrade(record.getGrade());
        point.setTemplateName(template != null ? template.getTemplateName() : null);
        point.setBaseScore(DEFAULT_BASE_SCORE);
        point.setMetricScore(total);
        point.setBonusScore(bonus);
        point.setStatus("已发布");
        return point;
    }

    private Map<Long, PerfTemplateDO> loadTemplates(List<PerfRecordDO> records) {
        List<Long> templateIds = records.stream().map(PerfRecordDO::getTemplateId).distinct().toList();
        if (templateIds.isEmpty()) {
            return Map.of();
        }
        return perfTemplateMapper.selectBatchIds(templateIds).stream()
                .collect(Collectors.toMap(PerfTemplateDO::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, List<PerfItemRecordDO>> loadItemRecords(List<PerfRecordDO> records) {
        List<Long> recordIds = records.stream().map(PerfRecordDO::getId).toList();
        if (recordIds.isEmpty()) {
            return Map.of();
        }
        return perfItemRecordMapper.selectList(new LambdaQueryWrapper<PerfItemRecordDO>()
                        .in(PerfItemRecordDO::getRecordId, recordIds))
                .stream()
                .collect(Collectors.groupingBy(PerfItemRecordDO::getRecordId));
    }

    private String resolveDeptName(Long ipGroupId) {
        if (ipGroupId == null) {
            return "-";
        }
        IpGroupDO group = ipGroupMapper.selectById(ipGroupId);
        return group != null ? group.getGroupName() : "-";
    }

    private SysUserDO requireUserInTenant(Long userId, Long tenantId) {
        SysUserDO user = sysUserMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return user;
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
