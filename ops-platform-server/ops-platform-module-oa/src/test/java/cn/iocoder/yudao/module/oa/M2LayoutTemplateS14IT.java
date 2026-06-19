package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-14 / S-14b~e: 公推模板库 + 版式套用 merge（ADR-020）
 */
@AutoConfigureMockMvc
class M2LayoutTemplateS14IT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String TPL_BASE = "/admin-api/oa/layout-template";
    private static final String CONTENT_BASE = "/admin-api/oa/content";

    @Autowired
    private MockMvc mockMvc;

    private static final String SAMPLE_SCHEMA = """
            {
              "version": 2,
              "globalStyles": {
                "heading2": { "fontSize": "18px", "fontWeight": "bold", "color": "#1a1a1a" },
                "paragraph": { "fontSize": "16px", "color": "#333333", "lineHeight": "1.75" }
              },
              "blocks": [
                { "type": "heading", "level": 2, "styleRef": "heading2", "slotKind": "heading" },
                { "type": "slot", "slotKind": "paragraph", "styleRef": "paragraph", "repeat": true }
              ]
            }
            """;

    @Test
    @DisplayName("S-14 P0: 模板 CRUD + select-list (layoutSchema)")
    void templateCrud() throws Exception {
        MvcResult create = mockMvc.perform(post(TPL_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "IT-S14-标准版式",
                                  "description": "集成测试",
                                  "status": "ENABLED",
                                  "layoutSchema": %s
                                }
                                """.formatted(SAMPLE_SCHEMA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        Long id = JsonPath.parse(create.getResponse().getContentAsString()).read("$.data", Long.class);

        mockMvc.perform(get(TPL_BASE + "/" + id)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.layoutSchema.version").value(2))
                .andExpect(jsonPath("$.data.previewHtml").exists());

        mockMvc.perform(get(TPL_BASE + "/select-list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("contentType", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.id==" + id + ")]").exists());

        mockMvc.perform(put(TPL_BASE + "/update")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": %d,
                                  "templateName": "IT-S14-已更新",
                                  "status": "ENABLED",
                                  "layoutSchema": %s
                                }
                                """.formatted(id, SAMPLE_SCHEMA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("S-14 P0: import-paste-preview 提取骨架（三步预览流）")
    void importPastePreview() throws Exception {
        MvcResult preview = mockMvc.perform(post(TPL_BASE + "/import-paste-preview")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "IT-S14-粘贴",
                                  "html": "<h2 style=\\"color:#07c160;font-size:22px;\\">粘贴标题</h2><p style=\\"color:#333;font-size:16px;\\">段落内容</p>"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.jobId").exists())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andReturn();
        Long jobId = JsonPath.parse(preview.getResponse().getContentAsString()).read("$.data.jobId", Long.class);

        mockMvc.perform(get(TPL_BASE + "/import-job/" + jobId)
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.previewLayoutSchema.version").value(2))
                .andExpect(jsonPath("$.data.previewLayoutSchema.blocks").isArray())
                .andExpect(jsonPath("$.data.previewHtml").exists())
                .andExpect(jsonPath("$.data.previewHtml").value(org.hamcrest.Matchers.containsString("color:#333")));
    }

    @Test
    @DisplayName("S-14 P0: merge 套用保留 body 文字")
    void applyTemplatePreservesBody() throws Exception {
        Long templateId = createEnabledTemplate();
        Long contentId = createArticleContent();

        mockMvc.perform(post(CONTENT_BASE + "/" + contentId + "/apply-layout-template")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "layoutTemplateId": %d, "overwrite": false }
                                """.formatted(templateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bodyFormat").value("LAYOUT"))
                .andExpect(jsonPath("$.data.body").value("AI生成正文\n\n第二段落"))
                .andExpect(jsonPath("$.data.layoutJson.version").value(2))
                .andExpect(jsonPath("$.data.layoutHtml").exists());

        mockMvc.perform(post(CONTENT_BASE + "/" + contentId + "/apply-layout-template")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "layoutTemplateId": %d, "overwrite": false }
                                """.formatted(templateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2031));
    }

    @Test
    @DisplayName("S-14e P0: 预置模板列表含 PRESET")
    void presetTemplatesSeeded() throws Exception {
        mockMvc.perform(get(TPL_BASE + "/list")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .param("sourceType", "PRESET")
                        .param("status", "ENABLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total", greaterThanOrEqualTo(11)))
                .andExpect(jsonPath("$.data.list[?(@.templateName=='【预置】图文混排·通用')]").exists());
    }

    private Long createEnabledTemplate() throws Exception {
        MvcResult r = mockMvc.perform(post(TPL_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "IT-S14-Apply",
                                  "status": "ENABLED",
                                  "layoutSchema": %s
                                }
                                """.formatted(SAMPLE_SCHEMA)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(r.getResponse().getContentAsString()).read("$.data", Long.class);
    }

    private Long createArticleContent() throws Exception {
        MvcResult r = mockMvc.perform(post(CONTENT_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "IT-S14-文章",
                                  "contentType": "ARTICLE",
                                  "documentType": "POST_MATCH_REVIEW",
                                  "platformType": "WECHAT_OFFICIAL",
                                  "accountId": 9003,
                                  "creatorUserId": 1001,
                                  "body": "AI生成正文\\n\\n第二段落",
                                  "ipGroupId": 9001
                                }
                                """))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(r.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
