package cn.iocoder.yudao.module.oa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-17: Typesetting rules + content typeset (ADR-020 body preservation)
 */
@AutoConfigureMockMvc
class M2TypesettingIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String RULE_BASE = "/admin-api/oa/typesetting-rule";
    private static final String CONTENT_BASE = "/admin-api/oa/content";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-17 P0: seeded rules + typeset preserves plain text")
    void typesetPreservesText() throws Exception {
        mockMvc.perform(get(RULE_BASE + "/enabled-list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(5)));

        mockMvc.perform(post(CONTENT_BASE + "/typeset")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "html": "<h1>大标题</h1><p>正文内容</p><img src=\\"x.jpg\\"/>"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.plainTextBefore").value("大标题正文内容"))
                .andExpect(jsonPath("$.data.plainTextAfter").value("大标题正文内容"))
                .andExpect(jsonPath("$.data.rulesApplied", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.html").exists());
    }
}
