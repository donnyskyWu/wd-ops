package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkTestConnectionRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkAccountDO;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkAccountMapper;
import cn.iocoder.yudao.module.oa.service.collect.wework.WeComAdapter;
import cn.iocoder.yudao.module.oa.service.collect.wework.WeComApiClient;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * M10-WECOM-S-02: 企微 gettoken 探活 + conn_status 落库（MockWebServer）。
 */
@AutoConfigureMockMvc
class M10WecomS02IT extends OaITBase {

    private static final long TENANT_1 = 1L;
    private static final long WEWORK_ACCOUNT_ID = 9001L;
    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";

    private static final AtomicReference<String> tokenResponse = new AtomicReference<>(
            """
            {"errcode":0,"errmsg":"ok","access_token":"mock_token","expires_in":7200}
            """);

    static final MockWebServer server;

    static {
        try {
            server = new MockWebServer();
            server.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    String path = request.getPath() == null ? "" : request.getPath();
                    if (path.contains("/cgi-bin/gettoken")) {
                        return jsonResponse(tokenResponse.get());
                    }
                    return new MockResponse().setResponseCode(404);
                }
            });
            server.start();
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json");
    }

    @Autowired
    private WeComAdapter weComAdapter;

    @Autowired
    private WeComApiClient weComApiClient;

    @Autowired
    private WeworkAccountMapper weworkAccountMapper;

    @Autowired
    private AesUtil aesUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @AfterAll
    static void stopServer() throws IOException {
        server.shutdown();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("oa.wework.base-url", () -> server.url("/").toString().replaceAll("/$", ""));
        registry.add("oa.wework.stub", () -> "false");
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_1);
        TenantContextHolder.setUsername("it-m10-wecom-s02");
        tokenResponse.set("""
                {"errcode":0,"errmsg":"ok","access_token":"mock_token","expires_in":7200}
                """);
        resetConnStatus();
        seedWeworkSecret();
        weComApiClient.clearTokenCacheForTest();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private void resetConnStatus() {
        jdbcTemplate.update(
                "UPDATE oa_wework_account SET conn_status = NULL, last_health_check_at = NULL WHERE id = ?",
                WEWORK_ACCOUNT_ID);
    }

    private void seedWeworkSecret() {
        WeworkAccountDO account = weworkAccountMapper.selectById(WEWORK_ACCOUNT_ID);
        account.setSecretEncrypted(aesUtil.encrypt("stub_wecom_secret"));
        weworkAccountMapper.updateById(account);
    }

    @Test
    @DisplayName("M10-WECOM-S-02: gettoken 成功 → CONNECTED + last_health_check_at")
    void testConnectionSuccessUpdatesConnStatus() {
        WeworkTestConnectionRespVO resp = weComAdapter.testConnection(WEWORK_ACCOUNT_ID);
        assertTrue(resp.isSuccess());
        assertEquals("CONNECTED", resp.getConnStatus());
        assertEquals("连接正常", resp.getMessage());

        WeworkAccountDO account = weworkAccountMapper.selectById(WEWORK_ACCOUNT_ID);
        assertEquals("CONNECTED", account.getConnStatus());
        assertNotNull(account.getLastHealthCheckAt());
    }

    @Test
    @DisplayName("M10-WECOM-S-02: gettoken 失败 → TOKEN_FAIL")
    void testConnectionFailureMapsTokenFail() {
        tokenResponse.set("""
                {"errcode":40001,"errmsg":"invalid secret"}
                """);
        weComApiClient.clearTokenCacheForTest();

        WeworkTestConnectionRespVO resp = weComAdapter.testConnection(WEWORK_ACCOUNT_ID);
        assertFalse(resp.isSuccess());
        assertEquals("TOKEN_FAIL", resp.getConnStatus());

        WeworkAccountDO account = weworkAccountMapper.selectById(WEWORK_ACCOUNT_ID);
        assertEquals("TOKEN_FAIL", account.getConnStatus());
        assertNotNull(account.getLastHealthCheckAt());
    }

    @Test
    @DisplayName("M10-WECOM-S-02: REST POST test-connection")
    void restTestConnectionSuccess() throws Exception {
        mockMvc.perform(post("/admin-api/oa/internal/wework/" + WEWORK_ACCOUNT_ID + "/test-connection")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.connStatus").value("CONNECTED"));
    }
}
