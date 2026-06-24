package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.jayway.jsonpath.JsonPath;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * M10-API-S-02: MockWebServer 验证 UnifiedCollectorApiClient HTTP 路径。
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class M10ApiCollectorBindS02HttpIT extends OaITBase {

    private static final long SEED_ACCOUNT_ID = 9001L;
    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    static final MockWebServer server;

    static {
        try {
            server = new MockWebServer();
            server.start();
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AesUtil aesUtil;

    @AfterAll
    static void stopServer() throws IOException {
        server.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("oa.unified-collector.base-url",
                () -> server.url("/").toString().replaceAll("/$", ""));
        registry.add("oa.unified-collector.api-token", () -> "test-key-2026");
        registry.add("oa.unified-collector.stub", () -> "false");
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        TenantContextHolder.setUsername("it-m10-api-s02-http");
        jdbcTemplate.update("DELETE FROM oa_collector_account_bind WHERE oa_account_id = ?", SEED_ACCOUNT_ID);
        AccountDO account = accountMapper.selectById(SEED_ACCOUNT_ID);
        account.setQualificationType("ENTERPRISE");
        account.setCookieEncrypted(aesUtil.encrypt("bizuin=123; data_bizuin=123"));
        account.setMpTokenEncrypted(aesUtil.encrypt("1234567890"));
        accountMapper.updateById(account);
    }

    @Test
    @Order(1)
    @DisplayName("M10-API-S-02: MockWebServer import 请求体含 cookie+token")
    void bindUsesMockCollectorImport() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("""
                        {"code":0,"message":"success","data":{"account_id":"acc_wechat_mp_mock001","platform":"wechat_mp","status":"active"}}
                        """)
                .addHeader("Content-Type", "application/json"));

        var result = mockMvc.perform(post("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.collectorAccountId").value("acc_wechat_mp_mock001"))
                .andReturn();

        RecordedRequest importReq = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(importReq);
        assertEquals("POST", importReq.getMethod());
        assertTrue(importReq.getPath().contains("/api/v1/accounts/import"));
        assertEquals("Bearer test-key-2026", importReq.getHeader("Authorization"));

        String body = importReq.getBody().readUtf8();
        assertTrue(body.contains("\"platform\":\"wechat_mp\""));
        assertTrue(body.contains("\"cookie\""));
        assertTrue(body.contains("\"token\""));
        assertNotNull(JsonPath.parse(result.getResponse().getContentAsString()).read("$.data.id"));
    }

    @Test
    @Order(2)
    @DisplayName("M10-API-S-02: Collector 鉴权失败 → 2022")
    void collectorAuthFailureReturns2022() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("""
                        {"code":40002,"message":"invalid token","data":null}
                        """)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(post("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(2022));
    }

    @Test
    @Order(3)
    @DisplayName("M10-API-S-02: MockWebServer health 探活")
    void testConnectionUsesMockHealth() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("""
                        {"code":0,"message":"success","data":{"account_id":"acc_wechat_mp_mock001","platform":"wechat_mp","status":"active"}}
                        """)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(post("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0));

        server.enqueue(new MockResponse()
                .setBody("""
                        {"code":0,"message":"success","data":{"account_id":"acc_wechat_mp_mock001","platform":"wechat_mp","status":"active"}}
                        """)
                .addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
                .setBody("""
                        {"code":0,"message":"success","data":{"summary":{},"accounts":[{"account_id":"acc_wechat_mp_mock001","platform":"wechat_mp","status":"active","alive":true}]}}
                        """)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(post("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind/test-connection")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @Order(4)
    @DisplayName("M10-API-S-02: 采集 Tab 保存长 Cookie + MP Token")
    void updateLongWechatOfficialCredentialsViaApi() throws Exception {
        String longCookie = "bizuin=1234567890; data_bizuin=1234567890; slave_sid="
                + "A".repeat(2048);
        String mpToken = "mp-token-" + "B".repeat(512);

        mockMvc.perform(put("/admin-api/oa/account/update")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "id": %d,
                                  "cookie": "%s",
                                  "mpToken": "%s"
                                }
                                """, SEED_ACCOUNT_ID, longCookie, mpToken)))
                .andExpect(jsonPath("$.code").value(0));

        AccountDO saved = accountMapper.selectById(SEED_ACCOUNT_ID);
        assertEquals(longCookie, aesUtil.decrypt(saved.getCookieEncrypted()));
        assertEquals(mpToken, aesUtil.decrypt(saved.getMpTokenEncrypted()));
    }
}
