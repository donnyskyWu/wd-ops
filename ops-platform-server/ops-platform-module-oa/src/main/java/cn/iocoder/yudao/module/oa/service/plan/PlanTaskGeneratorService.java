package cn.iocoder.yudao.module.oa.service.plan;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanPreviewMatchReq;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanPreviewTasksReq;
import cn.iocoder.yudao.module.oa.api.dto.plan.ContentPlanTaskPreviewVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.SopNodeDO;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopNodeMapper;
import cn.iocoder.yudao.module.oa.service.support.FootballSystemUserValidator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanTaskGeneratorService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SopNodeMapper sopNodeMapper;
    private final IpGroupMapper ipGroupMapper;
    private final IpGroupMemberMapper ipGroupMemberMapper;
    private final FootballSystemUserValidator footballSystemUserValidator;

    public List<ContentPlanTaskPreviewVO> preview(ContentPlanPreviewTasksReq req, Long tenantId) {
        List<SopNodeDO> nodes = sopNodeMapper.selectList(new LambdaQueryWrapper<SopNodeDO>()
                .eq(SopNodeDO::getTemplateId, req.getTemplateId())
                .orderByAsc(SopNodeDO::getNodeOrder));
        if (nodes.isEmpty()) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "SOP 模板无节点");
        }

        IpGroupDO ipGroup = ipGroupMapper.selectById(req.getIpGroupId());
        if (ipGroup == null || !tenantId.equals(ipGroup.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "IP 组不存在");
        }

        List<IpGroupMemberDO> members = ipGroupMemberMapper.selectList(new LambdaQueryWrapper<IpGroupMemberDO>()
                .eq(IpGroupMemberDO::getTenantId, tenantId)
                .eq(IpGroupMemberDO::getIpGroupId, req.getIpGroupId()));
        Set<String> memberPositions = members.stream()
                .map(IpGroupMemberDO::getPosition)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());

        Map<Long, String> userNames = loadUserNames(members, ipGroup.getLeaderUserId());

        List<ContentPlanTaskPreviewVO> result = new ArrayList<>();
        for (ContentPlanPreviewMatchReq match : req.getMatches()) {
            LocalDateTime matchStart = resolveMatchStart(match);
            LocalDateTime taskStart = matchStart.minusHours(24);
            for (SopNodeDO node : nodes) {
                ContentPlanTaskPreviewVO vo = new ContentPlanTaskPreviewVO();
                vo.setNodeId(node.getId());
                vo.setNodeName(node.getNodeName());
                vo.setNodeOrder(node.getNodeOrder());
                vo.setExecutorRole(node.getExecutorRole());
                vo.setCompetitionId(match.getCompetitionId());
                vo.setCompetitionName(match.getCompetitionName());
                vo.setScheduledStart(taskStart.format(DT_FMT));
                vo.setScheduledEnd(matchStart.format(DT_FMT));

                String role = StrUtil.blankToDefault(node.getExecutorRole(), "");
                if (StrUtil.isNotBlank(role) && !memberPositions.contains(role)) {
                    vo.setPositionWarning("IP 组内无岗位「" + role + "」成员，已默认 IP 组长");
                }

                AssigneeResolution resolution = resolveAssignee(role, members, ipGroup.getLeaderUserId());
                vo.setAssigneeId(resolution.userId());
                vo.setAssigneeFallback(resolution.fallback());
                vo.setAssigneeName(userNames.get(resolution.userId()));
                result.add(vo);
            }
        }
        result.sort(Comparator
                .comparing(ContentPlanTaskPreviewVO::getCompetitionId, Comparator.nullsLast(String::compareTo))
                .thenComparing(v -> v.getNodeOrder() == null ? 0 : v.getNodeOrder()));
        return result;
    }

    /**
     * 按 SOP 节点 executor_role 匹配 IP 组成员 position（dict_position）；无匹配则回退 IP 组长。
     */
    public AssigneeResolution resolveAssignee(String executorRole, List<IpGroupMemberDO> members, Long leaderUserId) {
        if (StrUtil.isNotBlank(executorRole)) {
            for (IpGroupMemberDO member : members) {
                if (executorRole.equals(member.getPosition())) {
                    return new AssigneeResolution(member.getUserId(), false);
                }
            }
        }
        if (leaderUserId != null) {
            return new AssigneeResolution(leaderUserId, true);
        }
        for (IpGroupMemberDO member : members) {
            if (member.getIsLeader() != null && member.getIsLeader() == 1) {
                return new AssigneeResolution(member.getUserId(), true);
            }
        }
        if (!members.isEmpty()) {
            return new AssigneeResolution(members.get(0).getUserId(), true);
        }
        throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "IP 组无可用成员");
    }

    public LocalDateTime resolveMatchStart(ContentPlanPreviewMatchReq match) {
        if (match.getMatchTimeRaw() != null && match.getMatchTimeRaw() > 0) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(match.getMatchTimeRaw()), ZoneId.systemDefault());
        }
        LocalDate day = LocalDate.now();
        if (StrUtil.isNotBlank(match.getCompetitionName())) {
            int idx = match.getCompetitionName().lastIndexOf('-');
            if (idx > 0 && idx + 1 < match.getCompetitionName().length()) {
                String tail = match.getCompetitionName().substring(idx + 1).trim();
                try {
                    if (tail.length() >= 10) {
                        day = LocalDate.parse(tail.substring(0, 10));
                    }
                } catch (Exception ignored) {
                    // use today
                }
            }
        }
        return day.atTime(20, 0);
    }

    private Map<Long, String> loadUserNames(List<IpGroupMemberDO> members, Long leaderUserId) {
        Set<Long> ids = new HashSet<>();
        for (IpGroupMemberDO member : members) {
            ids.add(member.getUserId());
        }
        if (leaderUserId != null) {
            ids.add(leaderUserId);
        }
        return footballSystemUserValidator.loadNicknames(ids);
    }

    public record AssigneeResolution(Long userId, boolean fallback) {
    }
}
