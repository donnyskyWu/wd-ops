package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.system.SysMessageDO;
import cn.iocoder.yudao.module.oa.dal.mysql.system.SysMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
class NotificationPlanStartIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String EXECUTOR = "Bearer dev-token-oa-operator";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysMessageMapper sysMessageMapper;

    @Test
    @DisplayName("计划启动后向执行人写入站内通知")
    void planStartCreatesInAppNotification() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/plan/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planName": "IT-通知测试计划",
                                  "templateId": 9401,
                                  "ipGroupId": 9001,
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30",
                                  "competitions": [
                                    {"competitionId": "cmp-notify", "competitionName": "通知赛事"}
                                  ],
                                  "steps": [
                                    {"nodeId": 9401, "competitionId": "cmp-notify", "assigneeIds": [1003]}
                                  ]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long planId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post("/admin-api/oa/plan/" + planId + "/start")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        Long count = sysMessageMapper.selectCount(new LambdaQueryWrapper<SysMessageDO>()
                .eq(SysMessageDO::getTenantId, 1L)
                .eq(SysMessageDO::getReceiver, "1003")
                .like(SysMessageDO::getTitle, "新任务待执行"));
        assertTrue(count >= 1, "执行人应收到计划启动通知");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/admin-api/oa/system/message/unread-count")
                        .header("Authorization", EXECUTOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNumber());
    }
}
