package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.AccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.util.AesUtil;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * M10 QR 扫码登录 OA 代理（MockWebServer）。
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class M10ApiCollectorQrLoginHttpIT extends OaITBase {

    private static final long SEED_ACCOUNT_ID = 9001L;
    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String SESSION_ID = "mock_qr_session_wechat_mp";

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
        TenantContextHolder.setUsername("it-m10-qr-login");
        jdbcTemplate.update("DELETE FROM oa_collector_account_bind WHERE oa_account_id = ?", SEED_ACCOUNT_ID);
        jdbcTemplate.update(
                "UPDATE oa_account SET cookie_encrypted = NULL, mp_token_encrypted = NULL WHERE id = ?",
                SEED_ACCOUNT_ID);
    }

    @Test
    @Order(1)
    @DisplayName("QR start 代理 collector /api/v1/auth/qrcode")
    void startQrLoginProxiesCollector() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("""
                        {"code":0,"message":"success","data":{
                          "session_id":"%s",
                          "status":"pending",
                          "qrcode_base64":"iVBORw0KGgo=",
                          "expires_in_seconds":300,
                          "message":"请扫码"
                        }}
                        """.formatted(SESSION_ID))
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(post("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind/qr-login/start")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessionId").value(SESSION_ID))
                .andExpect(jsonPath("$.data.qrcodeBase64").value("iVBORw0KGgo="));

        RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertTrue(req.getPath().contains("/api/v1/auth/qrcode"));
        assertTrue(req.getBody().readUtf8().contains("\"platform\":\"wechat_mp\""));
    }

    @Test
    @Order(2)
    @DisplayName("QR poll confirmed 写入凭证并 bind")
    void pollConfirmedSavesCredentialsAndBind() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("""
                        {"code":0,"message":"success","data":{
                          "status":"confirmed",
                          "message":"登录成功",
                          "account_id":"acc_wechat_mp_qr001",
                          "credential":{"cookie":"bizuin=1; sessionid=abc","token":"mp-token-xyz"}
                        }}
                        """)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(get("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind/qr-login/poll")
                        .param("sessionId", SESSION_ID)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("confirmed"))
                .andExpect(jsonPath("$.data.credentialsSaved").value(true))
                .andExpect(jsonPath("$.data.collectorAccountId").value("acc_wechat_mp_qr001"))
                .andExpect(jsonPath("$.data.bindStatus").value("BOUND"));

        AccountDO saved = accountMapper.selectById(SEED_ACCOUNT_ID);
        assertEquals("bizuin=1; sessionid=abc", aesUtil.decrypt(saved.getCookieEncrypted()));
        assertEquals("mp-token-xyz", aesUtil.decrypt(saved.getMpTokenEncrypted()));

        Integer bindCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oa_collector_account_bind WHERE oa_account_id = ? AND collector_account_id = ?",
                Integer.class, SEED_ACCOUNT_ID, "acc_wechat_mp_qr001");
        assertEquals(1, bindCount);
    }

    @Test
    @Order(3)
    @DisplayName("QR poll pending 不写入凭证")
    void pollPendingDoesNotSaveCredentials() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("""
                        {"code":0,"message":"success","data":{"status":"pending","message":"等待扫码"}}
                        """)
                .addHeader("Content-Type", "application/json"));

        mockMvc.perform(get("/admin-api/oa/account/" + SEED_ACCOUNT_ID + "/collector-bind/qr-login/poll")
                        .param("sessionId", SESSION_ID)
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.credentialsSaved").value(false));

        AccountDO saved = accountMapper.selectById(SEED_ACCOUNT_ID);
        assertTrue(saved.getCookieEncrypted() == null || saved.getCookieEncrypted().isBlank());
    }
}
