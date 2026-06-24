package cn.iocoder.yudao.module.oa;

import cn.iocoder.yudao.module.oa.dal.dataobject.collect.CollectLogDO;
import cn.iocoder.yudao.module.oa.dal.mysql.collect.CollectLogMapper;
import com.jayway.jsonpath.JsonPath;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class M10ColCollectLogDetailIT extends OaITBase {

    private static final String AUTH = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String TENANT_B = "2";
    private static final String TENANT_B_AUTH = "Bearer dev-token-oa-tenantb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollectLogMapper collectLogMapper;

    @Test
    @DisplayName("M10-COL: 采集日志详情返回 result 摘要")
    void logDetailReturnsResultPayload() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/admin-api/oa/collect/task/create")
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "日志详情测试任务",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9001,
                                  "method": "INTERNAL",
                                  "source": "WECHAT_MP_API",
                                  "frequency": "DAILY",
                                  "cron": "0 0 2 * * ?",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long taskId = JsonPath.parse(createResult.getResponse().getContentAsString()).read("$.data", Long.class);

        CollectLogDO log = new CollectLogDO();
        log.setTenantId(Long.parseLong(TENANT));
        log.setTaskId(taskId);
        log.setStatus("SUCCESS");
        log.setStartAt(LocalDateTime.now().minusMinutes(1));
        log.setEndAt(LocalDateTime.now());
        log.setDurationMs(1200L);
        log.setRecordCount(2);
        log.setRetryCount(0);
        log.setCreator("it-test");
        log.setUpdater("it-test");
        log.setCreateTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());
        log.setResultJson("""
                {
                  "summary": "同步粉丝 2 条",
                  "dataType": "MP_FOLLOWER_LIST",
                  "targetTable": "oa_wechat_mp_follower",
                  "targetHint": "数据已写入公众号粉丝表",
                  "recordCount": 2,
                  "samples": [
                    {"openid": "o-test-1", "nickname": "测试粉丝A"},
                    {"openid": "o-test-2", "nickname": "测试粉丝B"}
                  ]
                }
                """);
        collectLogMapper.insert(log);

        // 详情与列表鉴权一致：均无 @PreAuthorize，dev-token 均可 200（非 403）
        mockMvc.perform(get("/admin-api/oa/collect/log/page")
                        .param("taskId", String.valueOf(taskId))
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin-api/oa/collect/log/" + log.getId())
                        .header("Authorization", AUTH)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.taskName").value("日志详情测试任务"))
                .andExpect(jsonPath("$.data.platformType").value("WECHAT_OFFICIAL"))
                .andExpect(jsonPath("$.data.result.summary").value("同步粉丝 2 条"))
                .andExpect(jsonPath("$.data.result.samples[0].openid").value("o-test-1"))
                .andExpect(jsonPath("$.data.result.samples[1].nickname").value("测试粉丝B"));

        mockMvc.perform(get("/admin-api/oa/collect/log/" + log.getId())
                        .header("Authorization", TENANT_B_AUTH)
                        .header("X-Tenant-Id", TENANT_B))
                .andExpect(jsonPath("$.code").value(1504));
    }
}
