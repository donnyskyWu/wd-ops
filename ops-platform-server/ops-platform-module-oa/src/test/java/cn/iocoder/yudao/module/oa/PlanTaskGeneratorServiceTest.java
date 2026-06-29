package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.service.plan.PlanTaskGeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanTaskGeneratorServiceTest {

    private final PlanTaskGeneratorService service = new PlanTaskGeneratorService(null, null, null, null);

    @Test
    @DisplayName("岗位匹配：OPERATOR 成员优先")
    void resolveAssignee_byPosition() {
        IpGroupMemberDO op = member(1003L, "OPERATOR", 0);
        IpGroupMemberDO anchor = member(1004L, "ANCHOR", 0);
        PlanTaskGeneratorService.AssigneeResolution r =
                service.resolveAssignee("OPERATOR", List.of(op, anchor), 1002L);
        assertEquals(1003L, r.userId());
        assertTrue(!r.fallback());
    }

    @Test
    @DisplayName("岗位无匹配：回退 IP 组长")
    void resolveAssignee_fallbackLeader() {
        IpGroupMemberDO op = member(1003L, "OPERATOR", 0);
        PlanTaskGeneratorService.AssigneeResolution r =
                service.resolveAssignee("OPS_LEADER", List.of(op), 1002L);
        assertEquals(1002L, r.userId());
        assertTrue(r.fallback());
    }

    private static IpGroupMemberDO member(long userId, String position, int isLeader) {
        IpGroupMemberDO m = new IpGroupMemberDO();
        m.setUserId(userId);
        m.setPosition(position);
        m.setIsLeader(isLeader);
        return m;
    }
}
