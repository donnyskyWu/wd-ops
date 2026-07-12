package cn.iocoder.yudao.module.oa.service.system;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.OperationLogVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.system.FootballSystemOperateLogDO;
import cn.iocoder.yudao.module.oa.dal.mysql.system.FootballSystemOperateLogMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.FootballSystemUserLookupMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志 Adapter（S3 / ADR-050 D6）：读 shenyu-system.system_operate_log V2，映射至 Ops OperationLogVO。
 */
@Service
@RequiredArgsConstructor
public class OperateLogAdapter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FootballSystemOperateLogMapper footballSystemOperateLogMapper;
    private final FootballSystemUserLookupMapper footballSystemUserLookupMapper;

    public PageResult<OperationLogVO> list(Long tenantId, String username, String module, String level,
                                           String startTime, String endTime,
                                           Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<FootballSystemOperateLogDO> wrapper = new LambdaQueryWrapper<FootballSystemOperateLogDO>()
                .eq(FootballSystemOperateLogDO::getTenantId, tenantId)
                .eq(StrUtil.isNotBlank(module), FootballSystemOperateLogDO::getType, module)
                .ge(StrUtil.isNotBlank(startTime), FootballSystemOperateLogDO::getCreateTime, parseTime(startTime))
                .le(StrUtil.isNotBlank(endTime), FootballSystemOperateLogDO::getCreateTime, parseTimeEnd(endTime))
                .orderByDesc(FootballSystemOperateLogDO::getId);
        applyLevelFilter(wrapper, level);
        if (!applyUsernameFilter(wrapper, tenantId, username)) {
            return PageResult.empty();
        }

        Page<FootballSystemOperateLogDO> page = footballSystemOperateLogMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<OperationLogVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    public long countActiveRows() {
        Long count = footballSystemOperateLogMapper.countActiveRows();
        return count == null ? 0L : count;
    }

    private boolean applyUsernameFilter(LambdaQueryWrapper<FootballSystemOperateLogDO> wrapper,
                                        Long tenantId, String username) {
        if (StrUtil.isBlank(username)) {
            return true;
        }
        List<Long> userIds = footballSystemUserLookupMapper.selectUserIdsByUsernameLike(tenantId, username);
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }
        wrapper.in(FootballSystemOperateLogDO::getUserId, userIds);
        return true;
    }

    static void applyLevelFilter(LambdaQueryWrapper<FootballSystemOperateLogDO> wrapper, String level) {
        if (StrUtil.isBlank(level)) {
            return;
        }
        if ("ERROR".equalsIgnoreCase(level)) {
            wrapper.eq(FootballSystemOperateLogDO::getSuccess, false);
        } else if ("INFO".equalsIgnoreCase(level) || "WARN".equalsIgnoreCase(level)) {
            wrapper.eq(FootballSystemOperateLogDO::getSuccess, true);
        }
    }

    private OperationLogVO toVO(FootballSystemOperateLogDO row) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(row.getId());
        vo.setUserName(resolveUserName(row.getUserId()));
        vo.setModule(row.getType());
        vo.setAction(StrUtil.blankToDefault(row.getSubType(), row.getAction()));
        vo.setLevel(Boolean.TRUE.equals(row.getSuccess()) ? "INFO" : "ERROR");
        vo.setContent(row.getAction());
        vo.setMethod(row.getRequestMethod());
        vo.setParams(buildParams(row));
        vo.setResponse(row.getExtra());
        vo.setIp(row.getUserIp());
        vo.setStatus(Boolean.TRUE.equals(row.getSuccess()) ? "SUCCESS" : "FAIL");
        if (row.getCreateTime() != null) {
            vo.setCreateTime(row.getCreateTime().format(FMT));
        }
        return vo;
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        String username = footballSystemUserLookupMapper.selectUsernameById(userId);
        return StrUtil.blankToDefault(username, String.valueOf(userId));
    }

    private String buildParams(FootballSystemOperateLogDO row) {
        if (StrUtil.isNotBlank(row.getRequestUrl()) && StrUtil.isNotBlank(row.getExtra())) {
            return row.getRequestUrl() + " | " + row.getExtra();
        }
        return StrUtil.blankToDefault(row.getRequestUrl(), row.getExtra());
    }

    private LocalDateTime parseTime(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if (text.length() == 10) {
            return LocalDateTime.parse(text + " 00:00:00", FMT);
        }
        return LocalDateTime.parse(text, FMT);
    }

    private LocalDateTime parseTimeEnd(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if (text.length() == 10) {
            return LocalDateTime.parse(text + " 23:59:59", FMT);
        }
        return LocalDateTime.parse(text, FMT);
    }
}
