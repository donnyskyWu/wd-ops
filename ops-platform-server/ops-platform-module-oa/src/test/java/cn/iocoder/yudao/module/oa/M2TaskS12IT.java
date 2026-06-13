package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.ipgroup.IpGroupMemberDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.sop.TaskDO;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMemberMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * S-12: 任务执行页 + 任务-内容关联（AC-M2-002-5~7）
 */
@AutoConfigureMockMvc
class M2TaskS12IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    /** ADR-003：dev-token-oa-admin → userId 1001 */
    private static final long ADMIN_USER_ID = 1001L;
    private static final long IP_GROUP_ID = 9001L;
    private static final String PLAN_BASE = "/admin-api/oa/plan";
    private static final String TASK_BASE = "/admin-api/oa/task";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ProductionContentMapper productionContentMapper;

    @Autowired
    private IpGroupMemberMapper ipGroupMemberMapper;

    @BeforeEach
    void ensureAdminInIpGroup() {
        Long count = ipGroupMemberMapper.selectCount(
                new LambdaQueryWrapper<IpGroupMemberDO>()
                        .eq(IpGroupMemberDO::getTenantId, 1L)
                        .eq(IpGroupMemberDO::getIpGroupId, IP_GROUP_ID)
                        .eq(IpGroupMemberDO::getUserId, ADMIN_USER_ID));
        if (count == null || count == 0) {
            IpGroupMemberDO member = new IpGroupMemberDO();
            member.setTenantId(1L);
            member.setIpGroupId(IP_GROUP_ID);
            member.setUserId(ADMIN_USER_ID);
            member.setPosition("OPERATOR");
            member.setIsLeader(0);
            member.setCreator("it-s12");
            member.setUpdater("it-s12");
            member.setCreateTime(LocalDateTime.now());
            member.setUpdateTime(LocalDateTime.now());
            ipGroupMemberMapper.insert(member);
        }
    }

    @Test
    @DisplayName("M2-S-12: 执行上下文（AC-M2-002-5）")
    void executeContextReturned() throws Exception {
        Long taskId = prepareContentGenTask();

        mockMvc.perform(get(TASK_BASE + "/" + taskId + "/execute")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.nodeType").value("CONTENT_GENERATION"))
                .andExpect(jsonPath("$.data.competitionId").value("cmp-001"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("M2-S-12: 内容生成完成门禁 2008（AC-M2-002-6）")
    void contentGenCompleteGate() throws Exception {
        Long taskId = prepareContentGenTask();
        startExecute(taskId);

        mockMvc.perform(post(TASK_BASE + "/" + taskId + "/execute/complete")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(2008));
    }

    @Test
    @DisplayName("M2-S-12: 内容 COMPLETED 后正常完成（AC-M2-002-7）")
    void contentGenNormalComplete() throws Exception {
        Long taskId = prepareContentGenTask();
        startExecute(taskId);
        linkCompletedContent(taskId);

        mockMvc.perform(post(TASK_BASE + "/" + taskId + "/execute/complete")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        TaskDO task = taskMapper.selectById(taskId);
        org.junit.jupiter.api.Assertions.assertEquals("DONE", task.getStatus());
    }

    @Test
    @DisplayName("M2-S-12: 执行说明与附件只读（BLK-M2-008/007）")
    void executeInstructionAndAttachments() throws Exception {
        Long taskId = prepareContentGenTask();

        mockMvc.perform(get(TASK_BASE + "/" + taskId + "/execute")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.executionInstruction").value("根据赛事素材撰写短视频文案，注意品牌调性与赛事关键词。"))
                .andExpect(jsonPath("$.data.attachments.length()").value(1))
                .andExpect(jsonPath("$.data.attachments[0].name").value("品牌规范示例.pdf"));
    }

    private Long prepareContentGenTask() throws Exception {
        MvcResult createResult = mockMvc.perform(post(PLAN_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planName": "IT-S12-任务执行",
                                  "templateId": 9401,
                                  "ipGroupId": 9001,
                                  "startDate": "2026-06-01",
                                  "endDate": "2026-06-30",
                                  "competitions": [
                                    {"competitionId": "cmp-001", "competitionName": "SEED-春季赛事"}
                                  ],
                                  "steps": [
                                    {"nodeId": 9401, "competitionId": "cmp-001", "assigneeIds": [1001]},
                                    {"nodeId": 9402, "competitionId": "cmp-001", "assigneeIds": [1001]},
                                    {"nodeId": 9403, "competitionId": "cmp-001", "assigneeIds": [1001]}
                                  ]
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long planId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(post(PLAN_BASE + "/" + planId + "/start")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        TaskDO task = taskMapper.selectOne(new LambdaQueryWrapper<TaskDO>()
                .eq(TaskDO::getPlanId, planId)
                .eq(TaskDO::getNodeId, 9402L)
                .last("LIMIT 1"));
        org.junit.jupiter.api.Assertions.assertNotNull(task);
        return task.getId();
    }

    private void startExecute(Long taskId) throws Exception {
        mockMvc.perform(get(TASK_BASE + "/" + taskId + "/execute")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));
    }

    private void linkCompletedContent(Long taskId) {
        TaskDO task = taskMapper.selectById(taskId);
        ProductionContentDO content = new ProductionContentDO();
        content.setTenantId(task.getTenantId());
        content.setTaskId(taskId);
        content.setCompetitionId(task.getCompetitionId());
        content.setTitle("IT-S12-关联内容");
        content.setBody("测试正文");
        content.setCreatorUserId(ADMIN_USER_ID);
        content.setAccountId(9101L);
        content.setPlatformType("DOUYIN");
        content.setContentType("ARTICLE");
        content.setStatus("COMPLETED");
        content.setAiGenerated(0);
        content.setCreator("it");
        content.setUpdater("it");
        content.setCreateTime(LocalDateTime.now());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.insert(content);
    }
}
