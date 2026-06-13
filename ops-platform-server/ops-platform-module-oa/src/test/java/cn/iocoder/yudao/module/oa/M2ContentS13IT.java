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
 * S-13: 任务驱动内容编辑（AC-M2-003-6~10）
 */
@AutoConfigureMockMvc
class M2ContentS13IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final long ADMIN_USER_ID = 1001L;
    private static final long IP_GROUP_ID = 9001L;
    private static final long AUTHOR_ID = 9101L;
    private static final String CONTENT_BASE = "/admin-api/oa/content";
    private static final String USER_BASE = "/admin-api/oa/user";
    private static final String PLAN_BASE = "/admin-api/oa/plan";

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
            member.setCreator("it-s13");
            member.setUpdater("it-s13");
            member.setCreateTime(LocalDateTime.now());
            member.setUpdateTime(LocalDateTime.now());
            ipGroupMemberMapper.insert(member);
        }
    }

    @Test
    @DisplayName("M2-S-13: 文档类型必填（AC-M2-003-6 / TC-M2-003-13）")
    void documentTypeRequiredForArticle() throws Exception {
        Long taskIdMissing = prepareContentGenTask();
        Long taskIdOk = prepareContentGenTask();

        mockMvc.perform(post(CONTENT_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-S13-缺文档类型",
                                  "contentType": "ARTICLE",
                                  "creatorUserId": %d,
                                  "body": "正文",
                                  "taskId": %d,
                                  "ipGroupId": %d,
                                  "authorId": %d
                                }
                                """.formatted(ADMIN_USER_ID, taskIdMissing, IP_GROUP_ID, AUTHOR_ID)))
                .andExpect(jsonPath("$.code").value(1400));

        mockMvc.perform(post(CONTENT_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-S13-短视频文案",
                                  "contentType": "ARTICLE",
                                  "documentType": "SHORT_VIDEO_SCRIPT",
                                  "creatorUserId": %d,
                                  "body": "文案正文",
                                  "taskId": %d,
                                  "ipGroupId": %d,
                                  "authorId": %d
                                }
                                """.formatted(ADMIN_USER_ID, taskIdOk, IP_GROUP_ID, AUTHOR_ID)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @DisplayName("M2-S-13: 短视频引用文案（AC-M2-003-7 / TC-M2-003-14）")
    void scriptRefReturnsCompletedScript() throws Exception {
        seedCompletedScript("cmp-ref-001", "引用文案正文 IT-S13");

        mockMvc.perform(get(CONTENT_BASE + "/script-ref")
                        .param("competitionId", "cmp-ref-001")
                        .param("documentType", "SHORT_VIDEO_SCRIPT")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.body").value("引用文案正文 IT-S13"));
    }

    @Test
    @DisplayName("M2-S-13: IP 组作者默认（AC-M2-003-8 / TC-M2-003-15）")
    void userIpGroupsWithAuthor() throws Exception {
        mockMvc.perform(get(USER_BASE + "/ip-groups")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].ipGroupId").value((int) IP_GROUP_ID))
                .andExpect(jsonPath("$.data[0].authorId").value((int) AUTHOR_ID));
    }

    @Test
    @DisplayName("M2-S-13: 任务驱动创作强制使用任务 IP 组")
    void taskDrivenContentUsesTaskIpGroup() throws Exception {
        Long taskId = prepareContentGenTask();
        TaskDO task = taskMapper.selectById(taskId);
        org.junit.jupiter.api.Assertions.assertNotNull(task.getIpGroupId());

        MvcResult result = mockMvc.perform(post(CONTENT_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-S13-任务IP组",
                                  "contentType": "ARTICLE",
                                  "documentType": "POST_MATCH_REVIEW",
                                  "creatorUserId": %d,
                                  "body": "正文",
                                  "taskId": %d,
                                  "ipGroupId": 9999,
                                  "authorId": %d
                                }
                                """.formatted(ADMIN_USER_ID, taskId, AUTHOR_ID)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long contentId = JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get(CONTENT_BASE + "/by-task")
                        .param("taskId", String.valueOf(taskId))
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ipGroupId").value(task.getIpGroupId().intValue()));

        ProductionContentDO content = productionContentMapper.selectById(contentId);
        org.junit.jupiter.api.Assertions.assertEquals(task.getIpGroupId(), content.getIpGroupId());
    }

    @Test
    @DisplayName("M2-S-13: by-task 对齐任务 IP 组（修复历史错误 ip_group_id）")
    void byTaskAlignsStaleIpGroupFromTask() throws Exception {
        Long taskId = prepareContentGenTask();
        TaskDO task = taskMapper.selectById(taskId);
        org.junit.jupiter.api.Assertions.assertNotNull(task.getIpGroupId());

        ProductionContentDO stale = new ProductionContentDO();
        stale.setTenantId(task.getTenantId());
        stale.setTaskId(taskId);
        stale.setCompetitionId(task.getCompetitionId());
        stale.setTitle("IT-S13-错位IP组");
        stale.setBody("正文");
        stale.setCreatorUserId(ADMIN_USER_ID);
        stale.setContentType("ARTICLE");
        stale.setDocumentType("POST_MATCH_REVIEW");
        stale.setStatus("DRAFT");
        stale.setAiGenerated(0);
        stale.setIpGroupId(92001L);
        stale.setCreator("it");
        stale.setUpdater("it");
        stale.setCreateTime(LocalDateTime.now());
        stale.setUpdateTime(LocalDateTime.now());
        productionContentMapper.insert(stale);

        mockMvc.perform(get(CONTENT_BASE + "/by-task")
                        .param("taskId", String.valueOf(taskId))
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.ipGroupId").value(task.getIpGroupId().intValue()));

        ProductionContentDO fixed = productionContentMapper.selectById(stale.getId());
        org.junit.jupiter.api.Assertions.assertEquals(task.getIpGroupId(), fixed.getIpGroupId());
    }

    @Test
    @DisplayName("M2-S-13: 保存与确认（AC-M2-003-9 / TC-M2-003-16）")
    void saveDraftAndConfirm() throws Exception {
        Long taskId = prepareContentGenTask();
        Long contentId = createArticleContent(taskId);

        mockMvc.perform(get(CONTENT_BASE + "/by-task")
                        .param("taskId", String.valueOf(taskId))
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(post(CONTENT_BASE + "/" + contentId + "/confirm")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        ProductionContentDO content = productionContentMapper.selectById(contentId);
        org.junit.jupiter.api.Assertions.assertEquals("COMPLETED", content.getStatus());
    }

    @Test
    @DisplayName("M2-S-13: 短视频最终视频默认（AC-M2-003-10 / TC-M2-003-17）")
    void shortVideoFinalUrlFromGenerated() throws Exception {
        Long taskId = prepareContentGenTaskForVideo();
        Long contentId = createShortVideoContent(taskId);

        mockMvc.perform(post(CONTENT_BASE + "/" + contentId + "/generate")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.generatedVideoUrl").exists());

        mockMvc.perform(post(CONTENT_BASE + "/" + contentId + "/confirm")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        ProductionContentDO content = productionContentMapper.selectById(contentId);
        org.junit.jupiter.api.Assertions.assertNotNull(content.getFinalVideoUrl());
        org.junit.jupiter.api.Assertions.assertEquals(content.getGeneratedVideoUrl(), content.getFinalVideoUrl());
    }

    private Long prepareContentGenTask() throws Exception {
        return prepareContentGenTaskInternal(false);
    }

    private Long prepareContentGenTaskForVideo() throws Exception {
        return prepareContentGenTaskInternal(true);
    }

    private Long prepareContentGenTaskInternal(boolean uniquePlan) throws Exception {
        String planName = uniquePlan ? "IT-S13-短视频计划-" + System.nanoTime() : "IT-S13-文档计划-" + System.nanoTime();
        MvcResult createResult = mockMvc.perform(post(PLAN_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planName": "%s",
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
                                """.formatted(planName)))
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

    private Long createArticleContent(Long taskId) throws Exception {
        MvcResult result = mockMvc.perform(post(CONTENT_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-S13-确认测试",
                                  "contentType": "ARTICLE",
                                  "documentType": "POST_MATCH_REVIEW",
                                  "creatorUserId": %d,
                                  "body": "复盘正文",
                                  "taskId": %d,
                                  "ipGroupId": %d,
                                  "authorId": %d
                                }
                                """.formatted(ADMIN_USER_ID, taskId, IP_GROUP_ID, AUTHOR_ID)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private Long createShortVideoContent(Long taskId) throws Exception {
        MvcResult result = mockMvc.perform(post(CONTENT_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-S13-短视频",
                                  "contentType": "SHORT_VIDEO",
                                  "creatorUserId": %d,
                                  "body": "",
                                  "taskId": %d,
                                  "ipGroupId": %d,
                                  "authorId": %d
                                }
                                """.formatted(ADMIN_USER_ID, taskId, IP_GROUP_ID, AUTHOR_ID)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private void seedCompletedScript(String competitionId, String body) {
        ProductionContentDO content = new ProductionContentDO();
        content.setTenantId(1L);
        content.setTitle("IT-S13-种子文案");
        content.setBody(body);
        content.setCreatorUserId(ADMIN_USER_ID);
        content.setAccountId(9006L);
        content.setPlatformType("DOUYIN");
        content.setContentType("ARTICLE");
        content.setDocumentType("SHORT_VIDEO_SCRIPT");
        content.setCompetitionId(competitionId);
        content.setStatus("COMPLETED");
        content.setAiGenerated(0);
        content.setCreator("it-s13");
        content.setUpdater("it-s13");
        content.setCreateTime(LocalDateTime.now());
        content.setUpdateTime(LocalDateTime.now());
        productionContentMapper.insert(content);
    }
}
