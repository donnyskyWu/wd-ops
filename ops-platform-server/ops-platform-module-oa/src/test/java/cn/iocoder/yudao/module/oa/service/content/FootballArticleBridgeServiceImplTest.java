package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FootballArticleBridgeServiceImplTest {

    @Test
    void resolvePaidBody_layoutPrefersLayoutHtmlOverPlainPaidBody() {
        ProductionContentDO content = new ProductionContentDO();
        content.setBodyFormat("LAYOUT");
        content.setBody("plain text body");
        content.setPaidBody("plain text body");
        content.setLayoutHtml("<section class=\"layout-article\"><p><strong>bold</strong></p></section>");

        assertEquals(content.getLayoutHtml(), FootballArticleBridgeServiceImpl.resolvePaidBody(content));
    }

    @Test
    void resolvePaidBody_plainUsesPaidBodyHtml() {
        ProductionContentDO content = new ProductionContentDO();
        content.setBodyFormat("PLAIN");
        content.setPaidBody("<p><strong>bold</strong></p>");

        assertEquals("<p><strong>bold</strong></p>", FootballArticleBridgeServiceImpl.resolvePaidBody(content));
    }

    @Test
    void resolvePaidBody_fallsBackToBodyWhenPaidBodyBlank() {
        ProductionContentDO content = new ProductionContentDO();
        content.setBody("fallback");

        assertEquals("fallback", FootballArticleBridgeServiceImpl.resolvePaidBody(content));
    }
}
