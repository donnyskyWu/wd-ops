package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.service.content.publish.PlatformPublishResult;
import cn.iocoder.yudao.module.oa.service.content.publish.WechatOfficialPublishAdapter;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * P2-M2-PUB-02/03：公众号 draft/add → PUBLISHED_DRAFT → freepublish/submit → FORMALLY_PUBLISHED。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class M2WechatOfficialPublishIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String OPERATOR = "Bearer dev-token-oa-operator";
    private static final String LEADER = "Bearer dev-token-oa-leader";
    private static final String TENANT = "1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AesUtil aesUtil;

    @Autowired
    private WechatOfficialPublishAdapter wechatOfficialPublishAdapter;

    @Test
    @DisplayName("P2-M2-PUB-02: 公众号发布为草稿 → PUBLISHED_DRAFT（stub API）")
    void publishWechatOfficialToDraftBox() throws Exception {
        prepareCertifiedWechatAccount();

        JsonPath jsonPath = JsonPath.compile("$");
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/content/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-公众号草稿发布",
                                  "contentType": "SHORT_VIDEO",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "creatorUserId": 1003,
                                  "ipGroupId": 9001,
                                  "competitionId": "cmp-001",
                                  "competitionName": "SEED-春季赛事",
                                  "body": "IT 公众号正文",
                                  "aiGenerated": 0
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long contentId = jsonPath.parse(createResult.getResponse().getContentAsString())
                .read("$.data", Long.class);

        submitAndApprove(contentId);

        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/publish-draft")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"platformType": "WECHAT_OFFICIAL", "accountIds": [9001]}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED_DRAFT"))
                .andExpect(jsonPath("$.data.mock").value(false))
                .andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.containsString("草稿箱")))
                .andExpect(jsonPath("$.data.records[0].externalId").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].status").value("SUCCESS"));

        mockMvc.perform(get("/admin-api/oa/content/" + contentId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED_DRAFT"));
    }

    @Test
    @DisplayName("P2-M2-PUB-03: 已发布草稿 → 正式发布 → FORMALLY_PUBLISHED")
    void formalPublishAfterDraft() throws Exception {
        prepareCertifiedWechatAccount();
        Long contentId = createAndPublishDraft();

        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/formal-publish")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("FORMALLY_PUBLISHED"))
                .andExpect(jsonPath("$.data.records[0].publishId").isNotEmpty())
                .andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.containsString("publish_id")));

        mockMvc.perform(get("/admin-api/oa/content/" + contentId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.status").value("FORMALLY_PUBLISHED"));
    }

    @Test
    @DisplayName("待发布内容可转知识库")
    void transferKnowledgeFromPendingPublish() throws Exception {
        prepareCertifiedWechatAccount();
        JsonPath jsonPath = JsonPath.compile("$");
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/content/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-待发布转知识库",
                                  "contentType": "SHORT_VIDEO",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "creatorUserId": 1003,
                                  "ipGroupId": 9001,
                                  "competitionId": "cmp-001",
                                  "competitionName": "SEED-春季赛事",
                                  "body": "正文",
                                  "aiGenerated": 0
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long contentId = jsonPath.parse(createResult.getResponse().getContentAsString())
                .read("$.data", Long.class);
        submitAndApprove(contentId);

        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/transfer-to-knowledge")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.knowledgeId").isNumber());

        mockMvc.perform(get("/admin-api/oa/content/" + contentId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.data.transferredToKnowledge").value(1));
    }

    @Test
    @DisplayName("未认证公众号发布失败")
    void publishFailsWithoutOfficialCredentials() {
        AccountDO account = accountMapper.selectById(9002L);
        account.setUsageStatus("REGISTERED");
        account.setAppId(null);
        account.setAppSecretEncrypted(null);
        accountMapper.updateById(account);

        ProductionContentDO content = new ProductionContentDO();
        content.setId(1L);
        content.setTitle("测试");
        content.setBody("正文");

        PlatformPublishResult result = wechatOfficialPublishAdapter.publish(content, account);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    private Long createAndPublishDraft() throws Exception {
        JsonPath jsonPath = JsonPath.compile("$");
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/content/create")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-正式发布流程",
                                  "contentType": "SHORT_VIDEO",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "creatorUserId": 1003,
                                  "ipGroupId": 9001,
                                  "competitionId": "cmp-001",
                                  "competitionName": "SEED-春季赛事",
                                  "body": "正文",
                                  "aiGenerated": 0
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long contentId = jsonPath.parse(createResult.getResponse().getContentAsString())
                .read("$.data", Long.class);
        submitAndApprove(contentId);
        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/publish-draft")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"platformType": "WECHAT_OFFICIAL", "accountIds": [9001]}
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED_DRAFT"));
        return contentId;
    }

    private void prepareCertifiedWechatAccount() {
        AccountDO account = accountMapper.selectById(9001L);
        account.setUsageStatus("CERTIFIED");
        account.setAppId("wx_it_test_appid");
        account.setAppSecretEncrypted(aesUtil.encrypt("wx_it_test_secret"));
        account.setPublishEnabled(1);
        accountMapper.updateById(account);
    }

    private void submitAndApprove(Long contentId) throws Exception {
        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/submit-review")
                        .header("Authorization", OPERATOR)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/review")
                        .header("Authorization", LEADER)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "APPROVE", "stage": "FIRST_REVIEW", "comment": "ok"}
                                """))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/admin-api/oa/content/" + contentId + "/review")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "APPROVE", "stage": "SECOND_REVIEW", "comment": "ok"}
                                """))
                .andExpect(jsonPath("$.code").value(0));
    }
}
