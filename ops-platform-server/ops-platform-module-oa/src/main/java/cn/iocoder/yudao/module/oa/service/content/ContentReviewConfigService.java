package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewConfigVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysRoleDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysRoleMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysUserTokenMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.service.system.ParamService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentReviewConfigService {

    public static final String KEY_LEVEL1_ENABLED = "content.review.level1.enabled";
    public static final String KEY_LEVEL2_ENABLED = "content.review.level2.enabled";
    public static final String KEY_LEVEL1_ROLE = "content.review.level1.role";
    public static final String KEY_LEVEL2_ROLE = "content.review.level2.role";

    private static final String DEFAULT_LEVEL1_ROLE = "OPS_LEADER";
    private static final String DEFAULT_LEVEL2_ROLE = "DEPT_HEAD";
    /** IP 组长：除系统角色外，也匹配内容所属 IP 组的组长用户 */
    private static final String IP_GROUP_LEADER_ROLE = "OPS_LEADER";

    private final ParamService paramService;
    private final SysUserTokenMapper sysUserTokenMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final IpGroupMapper ipGroupMapper;

    public ContentReviewConfigVO getConfig() {
        Long tenantId = TenantContextHolder.getTenantId();
        ContentReviewConfigVO vo = new ContentReviewConfigVO();
        vo.setLevel1Enabled(paramService.getBoolean(tenantId, KEY_LEVEL1_ENABLED, true));
        vo.setLevel2Enabled(paramService.getBoolean(tenantId, KEY_LEVEL2_ENABLED, true));
        vo.setLevel1Role(paramService.getString(tenantId, KEY_LEVEL1_ROLE, DEFAULT_LEVEL1_ROLE));
        vo.setLevel2Role(paramService.getString(tenantId, KEY_LEVEL2_ROLE, DEFAULT_LEVEL2_ROLE));
        return vo;
    }

    public String resolveInitialReviewStatus() {
        ContentReviewConfigVO config = getConfig();
        if (config.isLevel1Enabled()) {
            return "PENDING_FIRST_REVIEW";
        }
        if (config.isLevel2Enabled()) {
            return "PENDING_SECOND_REVIEW";
        }
        return "PENDING_PUBLISH";
    }

    public String resolveSubmitStage() {
        ContentReviewConfigVO config = getConfig();
        if (config.isLevel1Enabled()) {
            return "FIRST_REVIEW";
        }
        if (config.isLevel2Enabled()) {
            return "SECOND_REVIEW";
        }
        return "SUBMIT";
    }

    public String resolveNextStatusAfterApprove(String currentStatus, String stage) {
        ContentReviewConfigVO config = getConfig();
        if ("FIRST_REVIEW".equals(stage) && "PENDING_FIRST_REVIEW".equals(currentStatus)) {
            return config.isLevel2Enabled() ? "PENDING_SECOND_REVIEW" : "PENDING_PUBLISH";
        }
        if ("SECOND_REVIEW".equals(stage) && "PENDING_SECOND_REVIEW".equals(currentStatus)) {
            return "PENDING_PUBLISH";
        }
        if ("FINAL_REVIEW".equals(stage) && "PENDING_FINAL_REVIEW".equals(currentStatus)) {
            return "PENDING_PUBLISH";
        }
        return null;
    }

    /** 是否可查看一级审核队列（含 IP 组长仅看自己组内内容的情况） */
    public boolean hasLevel1ListAccess(Long userId) {
        if (userId == null) {
            return false;
        }
        ContentReviewConfigVO config = getConfig();
        if (!config.isLevel1Enabled()) {
            return false;
        }
        String level1Role = config.getLevel1Role();
        if (IP_GROUP_LEADER_ROLE.equals(level1Role)) {
            return !listIpGroupIdsLedByUser(userId).isEmpty();
        }
        return hasRole(userId, level1Role);
    }

    /** 一级审核是否可查看全部待审内容（非仅 IP 组范围） */
    public boolean hasLevel1FullAccess(Long userId) {
        if (userId == null) {
            return false;
        }
        String level1Role = getConfig().getLevel1Role();
        if (IP_GROUP_LEADER_ROLE.equals(level1Role)) {
            return false;
        }
        return hasRole(userId, level1Role);
    }

    /** 是否可查看二级审核队列 */
    public boolean hasLevel2ListAccess(Long userId) {
        if (userId == null) {
            return false;
        }
        ContentReviewConfigVO config = getConfig();
        if (!config.isLevel2Enabled()) {
            return false;
        }
        return hasRole(userId, config.getLevel2Role());
    }

    public List<Long> listIpGroupIdsLedByUser(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return Collections.emptyList();
        }
        return ipGroupMapper.selectList(new LambdaQueryWrapper<IpGroupDO>()
                        .eq(IpGroupDO::getTenantId, tenantId)
                        .eq(IpGroupDO::getLeaderUserId, userId)
                        .eq(IpGroupDO::getStatus, 1))
                .stream()
                .map(IpGroupDO::getId)
                .collect(Collectors.toList());
    }

    public boolean canReview(Long userId, ProductionContentDO content, String stage) {
        if (userId == null) {
            return false;
        }
        ContentReviewConfigVO config = getConfig();
        if ("FIRST_REVIEW".equals(stage)) {
            String level1Role = config.getLevel1Role();
            if (IP_GROUP_LEADER_ROLE.equals(level1Role)) {
                return isIpGroupLeader(userId, content);
            }
            return hasRole(userId, level1Role);
        }
        if ("SECOND_REVIEW".equals(stage)) {
            return hasRole(userId, config.getLevel2Role());
        }
        if ("FINAL_REVIEW".equals(stage)) {
            return hasRole(userId, config.getLevel2Role());
        }
        return false;
    }

    public String resolveStageRoleCode(String stage) {
        ContentReviewConfigVO config = getConfig();
        if ("FIRST_REVIEW".equals(stage)) {
            return config.getLevel1Role();
        }
        if ("SECOND_REVIEW".equals(stage) || "FINAL_REVIEW".equals(stage)) {
            return config.getLevel2Role();
        }
        return null;
    }

    public String resolveReviewRoleLabel(String roleCode) {
        if (StrUtil.isBlank(roleCode)) {
            return "待审核";
        }
        if (IP_GROUP_LEADER_ROLE.equals(roleCode)) {
            return "IP组长";
        }
        Long tenantId = TenantContextHolder.getTenantId();
        SysRoleDO role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRoleDO>()
                .eq(SysRoleDO::getTenantId, tenantId)
                .eq(SysRoleDO::getCode, roleCode)
                .last("LIMIT 1"));
        if (role != null && StrUtil.isNotBlank(role.getName())) {
            return role.getName();
        }
        return roleCode;
    }

    public List<String> listEligibleReviewerNames(ProductionContentDO content, String stage) {
        String roleCode = resolveStageRoleCode(stage);
        if (StrUtil.isBlank(roleCode)) {
            return Collections.emptyList();
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> userIds = new LinkedHashSet<>();
        if ("FIRST_REVIEW".equals(stage) && IP_GROUP_LEADER_ROLE.equals(roleCode)) {
            if (content.getIpGroupId() != null) {
                IpGroupDO ipGroup = ipGroupMapper.selectById(content.getIpGroupId());
                if (ipGroup != null && ipGroup.getLeaderUserId() != null) {
                    userIds.add(ipGroup.getLeaderUserId());
                }
            }
        } else {
            for (SysUserDO user : sysUserTokenMapper.selectUsersByRoleCode(tenantId, roleCode)) {
                if (user.getId() != null) {
                    userIds.add(user.getId());
                }
            }
        }
        List<String> names = new ArrayList<>();
        for (Long userId : userIds) {
            SysUserDO user = sysUserMapper.selectById(userId);
            if (user != null) {
                String name = displayUserName(user);
                if (StrUtil.isNotBlank(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /** 待审步骤：查询可审核用户 ID（角色用户 + 一级 OPS_LEADER 时含 IP 组长） */
    public List<Long> listEligibleReviewerUserIds(ProductionContentDO content, String stage) {
        String roleCode = resolveStageRoleCode(stage);
        if (StrUtil.isBlank(roleCode)) {
            return Collections.emptyList();
        }
        Long tenantId = TenantContextHolder.getTenantId();
        Set<Long> userIds = new LinkedHashSet<>();
        if ("FIRST_REVIEW".equals(stage) && IP_GROUP_LEADER_ROLE.equals(roleCode)) {
            if (content.getIpGroupId() != null) {
                IpGroupDO ipGroup = ipGroupMapper.selectById(content.getIpGroupId());
                if (ipGroup != null && ipGroup.getLeaderUserId() != null) {
                    userIds.add(ipGroup.getLeaderUserId());
                }
            }
        } else {
            for (SysUserDO user : sysUserTokenMapper.selectUsersByRoleCode(tenantId, roleCode)) {
                if (user.getId() != null) {
                    userIds.add(user.getId());
                }
            }
        }
        return new ArrayList<>(userIds);
    }

    public String formatReviewerDisplay(String roleLabel, List<String> userNames) {
        if (StrUtil.isBlank(roleLabel)) {
            return userNames == null || userNames.isEmpty() ? null : String.join("、", userNames);
        }
        if (userNames == null || userNames.isEmpty()) {
            return roleLabel;
        }
        return roleLabel + "：" + String.join("、", userNames);
    }

    public String formatReviewerDisplay(String roleLabel, String userName) {
        if (StrUtil.isBlank(userName)) {
            return roleLabel;
        }
        if (StrUtil.isBlank(roleLabel)) {
            return userName;
        }
        return roleLabel + "：" + userName;
    }

    private String displayUserName(SysUserDO user) {
        if (user == null) {
            return null;
        }
        return StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private boolean hasRole(Long userId, String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return false;
        }
        return sysUserTokenMapper.selectRolesByUserId(userId).stream()
                .anyMatch(role -> roleCode.equals(role.getCode()));
    }

    private boolean isIpGroupLeader(Long userId, ProductionContentDO content) {
        if (content.getIpGroupId() == null) {
            return false;
        }
        IpGroupDO ipGroup = ipGroupMapper.selectById(content.getIpGroupId());
        return ipGroup != null && Objects.equals(ipGroup.getLeaderUserId(), userId);
    }
}
