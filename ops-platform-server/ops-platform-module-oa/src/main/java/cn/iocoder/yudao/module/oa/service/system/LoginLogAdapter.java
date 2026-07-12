package cn.iocoder.yudao.module.oa.service.system;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.system.LoginLogVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.system.FootballSystemLoginLogDO;
import cn.iocoder.yudao.module.oa.dal.mysql.system.FootballSystemLoginLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录日志 Adapter（S3 / ADR-050 D6）：读 shenyu-system.system_login_log，映射至 Ops LoginLogVO。
 */
@Service
@RequiredArgsConstructor
public class LoginLogAdapter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FootballSystemLoginLogMapper footballSystemLoginLogMapper;

    public PageResult<LoginLogVO> list(Long tenantId, String username, String ip, String status,
                                       String startTime, String endTime,
                                       Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<FootballSystemLoginLogDO> wrapper = new LambdaQueryWrapper<FootballSystemLoginLogDO>()
                .eq(FootballSystemLoginLogDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(username), FootballSystemLoginLogDO::getUsername, username)
                .like(StrUtil.isNotBlank(ip), FootballSystemLoginLogDO::getUserIp, ip)
                .ge(StrUtil.isNotBlank(startTime), FootballSystemLoginLogDO::getCreateTime, parseTime(startTime))
                .le(StrUtil.isNotBlank(endTime), FootballSystemLoginLogDO::getCreateTime, parseTimeEnd(endTime))
                .orderByDesc(FootballSystemLoginLogDO::getId);
        applyStatusFilter(wrapper, status);

        Page<FootballSystemLoginLogDO> page = footballSystemLoginLogMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<LoginLogVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    public long countActiveRows() {
        Long count = footballSystemLoginLogMapper.countActiveRows();
        return count == null ? 0L : count;
    }

    static void applyStatusFilter(LambdaQueryWrapper<FootballSystemLoginLogDO> wrapper, String status) {
        if (StrUtil.isBlank(status)) {
            return;
        }
        if ("SUCCESS".equalsIgnoreCase(status)) {
            wrapper.eq(FootballSystemLoginLogDO::getResult, 0);
        } else if ("FAIL".equalsIgnoreCase(status)) {
            wrapper.ne(FootballSystemLoginLogDO::getResult, 0);
        }
    }

    private LoginLogVO toVO(FootballSystemLoginLogDO row) {
        LoginLogVO vo = new LoginLogVO();
        vo.setId(row.getId());
        vo.setUserName(row.getUsername());
        vo.setIp(row.getUserIp());
        vo.setUserAgent(row.getUserAgent());
        vo.setStatus(mapResultToStatus(row.getResult()));
        vo.setMessage(buildMessage(row));
        if (row.getCreateTime() != null) {
            vo.setCreateTime(row.getCreateTime().format(FMT));
        }
        return vo;
    }

    static String mapResultToStatus(Integer result) {
        return result != null && result == 0 ? "SUCCESS" : "FAIL";
    }

    private String buildMessage(FootballSystemLoginLogDO row) {
        if (StrUtil.isNotBlank(row.getTraceId())) {
            return row.getTraceId();
        }
        if (row.getLogType() != null) {
            return "logType=" + row.getLogType();
        }
        return null;
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
