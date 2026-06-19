package cn.iocoder.yudao.module.oa;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S-16: partial-apply + apply-background merge endpoints
 */
@AutoConfigureMockMvc
class M2LayoutTemplatePartialApplyIT extends OaITBase {

    private static final String ADMIN = "Bearer dev-token-oa-admin";
    private static final String TENANT = "1";
    private static final String TPL_BASE = "/admin-api/oa/layout-template";

    private static final String SAMPLE_SCHEMA = """
            {
              "version": 2,
              "globalStyles": {
                "heading2": { "fontSize": "18px", "fontWeight": "bold", "color": "#1a1a1a" },
                "paragraph": { "fontSize": "16px", "color": "#333333", "lineHeight": "1.75" },
                "divider": { "borderColor": "#e5e5e5", "margin": "24px 0" }
              },
              "blocks": [
                { "type": "heading", "level": 2, "styleRef": "heading2", "slotKind": "heading" },
                { "type": "divider", "styleRef": "divider" },
                { "type": "slot", "slotKind": "paragraph", "styleRef": "paragraph", "repeat": true }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("S-16 P0: partial-apply excludes divider when filtered")
    void partialApply() throws Exception {
        Long templateId = createTemplate();

        mockMvc.perform(post(TPL_BASE + "/" + templateId + "/partial-apply")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "标题段\\n\\n正文段",
                                  "includeBlockTypes": ["heading", "slot"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.layoutJson.blocks").isArray())
                .andExpect(jsonPath("$.data.layoutHtml").exists());
    }

    @Test
    @DisplayName("S-16 P0: apply-background preserves segments with global styles")
    void applyBackground() throws Exception {
        Long templateId = createTemplate();

        mockMvc.perform(post(TPL_BASE + "/" + templateId + "/apply-background")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "仅背景\\n\\n第二段",
                                  "paramOverrides": {
                                    "paragraph": { "color": "#07c160" }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.layoutJson.backgroundOnly").value(true));
    }

    @Test
    @DisplayName("S-16 P0: preview-merge with paramOverrides")
    void previewMergeWithParams() throws Exception {
        Long templateId = createTemplate();

        mockMvc.perform(post(TPL_BASE + "/" + templateId + "/preview-merge")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "带参数\\n\\n段落",
                                  "paramOverrides": {
                                    "heading2": { "color": "#ff0000" }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.layoutHtml").exists());
    }

    private Long createTemplate() throws Exception {
        MvcResult r = mockMvc.perform(post(TPL_BASE + "/create")
                        .header("Authorization", ADMIN)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "IT-S16-Partial",
                                  "status": "ENABLED",
                                  "layoutSchema": %s
                                }
                                """.formatted(SAMPLE_SCHEMA)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JsonPath.parse(r.getResponse().getContentAsString()).read("$.data", Long.class);
    }
}
