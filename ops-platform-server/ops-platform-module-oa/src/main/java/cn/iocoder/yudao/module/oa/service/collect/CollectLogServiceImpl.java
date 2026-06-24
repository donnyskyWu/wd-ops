package cn.iocoder.yudao.module.oa.service.collect;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectLogDetailRespVO;
import cn.iocoder.yudao.module.oa.api.dto.collect.CollectLogRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectLogDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectTaskDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectTaskMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkAccountMapper;
import cn.iocoder.yudao.module.oa.service.config.ConfigTenantSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectLogServiceImpl implements CollectLogService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SOURCE_AOCHUANG = "AOCHUANG_API";
    private static final String SOURCE_WECOM_API = "WECOM_API";
    private static final String PLATFORM_PERSONAL_WECHAT = "WECHAT_PERSONAL";
    private static final String PLATFORM_WEWORK = "WEWORK";

    private final CollectLogMapper collectLogMapper;
    private final CollectTaskMapper collectTaskMapper;
    private final CollectLogResultBuilder collectLogResultBuilder;
    private final AccountMapper accountMapper;
    private final PersonalWechatAccountMapper personalWechatAccountMapper;
    private final WeworkAccountMapper weworkAccountMapper;

    @Override
    public PageResult<CollectLogRespVO> page(Long taskId, String status, String startDate, String endDate,
                                             Integer pageNo, Integer pageSize) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        LambdaQueryWrapper<CollectLogDO> wrapper = new LambdaQueryWrapper<CollectLogDO>()
                .eq(CollectLogDO::getTenantId, tenantId)
                .eq(taskId != null, CollectLogDO::getTaskId, taskId)
                .eq(StrUtil.isNotBlank(status), CollectLogDO::getStatus, status)
                .ge(StrUtil.isNotBlank(startDate), CollectLogDO::getStartAt, parseDateTime(startDate))
                .le(StrUtil.isNotBlank(endDate), CollectLogDO::getStartAt, parseDateTime(endDate))
                .orderByDesc(CollectLogDO::getStartAt);
        Page<CollectLogDO> page = collectLogMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize), wrapper);
        Map<Long, CollectTaskDO> taskById = loadTasksById(page.getRecords());
        List<CollectLogRespVO> list = page.getRecords().stream()
                .map(log -> toResp(log, taskById.get(log.getTaskId())))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public CollectLogDetailRespVO getDetail(Long id) {
        CollectLogDO log = collectLogMapper.selectById(id);
        log = ConfigTenantSupport.getRequiredInTenant(log);
        CollectTaskDO task = collectTaskMapper.selectById(log.getTaskId());
        if (task != null) {
            ConfigTenantSupport.getRequiredInTenant(task);
        }
        return toDetailResp(log, task);
    }

    private CollectLogDetailRespVO toDetailResp(CollectLogDO log, CollectTaskDO task) {
        CollectLogDetailRespVO vo = new CollectLogDetailRespVO();
        CollectLogRespVO base = toResp(log, task);
        vo.setId(base.getId());
        vo.setTaskId(base.getTaskId());
        vo.setTaskName(base.getTaskName());
        vo.setStatus(base.getStatus());
        vo.setStartAt(base.getStartAt());
        vo.setDurationMs(base.getDurationMs());
        vo.setRecordCount(base.getRecordCount());
        vo.setErrorMessage(base.getErrorMessage());
        vo.setRetryCount(base.getRetryCount());
        vo.setEndAt(log.getEndAt());
        vo.setResult(collectLogResultBuilder.parse(log.getResultJson()));
        if (task != null) {
            vo.setPlatformType(task.getPlatformType());
            vo.setAccountId(task.getAccountId());
            vo.setAccountName(resolveAccountName(task));
            vo.setSource(task.getSource());
            vo.setDataType(task.getDataType());
        }
        return vo;
    }

    private String resolveAccountName(CollectTaskDO task) {
        if (task.getAccountId() == null) {
            return null;
        }
        if (isWeworkCollectTask(task.getPlatformType(), task.getSource())) {
            WeworkAccountDO wework = weworkAccountMapper.selectById(task.getAccountId());
            if (wework != null && ConfigTenantSupport.requireTenantId().equals(wework.getTenantId())) {
                return wework.getAccountName();
            }
            return null;
        }
        if (PLATFORM_PERSONAL_WECHAT.equals(task.getPlatformType()) && SOURCE_AOCHUANG.equals(task.getSource())) {
            PersonalWechatAccountDO personal = personalWechatAccountMapper.selectById(task.getAccountId());
            if (personal != null && ConfigTenantSupport.requireTenantId().equals(personal.getTenantId())) {
                return personal.getAccountName();
            }
            return null;
        }
        AccountDO account = accountMapper.selectById(task.getAccountId());
        if (account != null && ConfigTenantSupport.requireTenantId().equals(account.getTenantId())) {
            return account.getAccountName();
        }
        return null;
    }

    private boolean isWeworkCollectTask(String platformType, String source) {
        return PLATFORM_WEWORK.equals(platformType) && SOURCE_WECOM_API.equals(source);
    }

    private LocalDateTime parseDateTime(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return LocalDateTime.parse(text, DATE_TIME);
    }

    private Map<Long, CollectTaskDO> loadTasksById(List<CollectLogDO> records) {
        List<Long> taskIds = records.stream()
                .map(CollectLogDO::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return Map.of();
        }
        return collectTaskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(CollectTaskDO::getId, Function.identity(), (a, b) -> a));
    }

    private CollectLogRespVO toResp(CollectLogDO log, CollectTaskDO task) {
        CollectLogRespVO vo = new CollectLogRespVO();
        vo.setId(log.getId());
        vo.setTaskId(log.getTaskId());
        if (task != null) {
            vo.setTaskName(task.getTaskName());
        }
        vo.setStatus(log.getStatus());
        vo.setStartAt(log.getStartAt());
        vo.setDurationMs(log.getDurationMs());
        vo.setRecordCount(log.getRecordCount());
        vo.setErrorMessage(log.getErrorMessage());
        vo.setRetryCount(log.getRetryCount());
        return vo;
    }
}
