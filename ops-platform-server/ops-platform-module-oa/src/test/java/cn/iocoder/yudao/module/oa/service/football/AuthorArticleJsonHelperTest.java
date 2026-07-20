package cn.iocoder.yudao.module.oa.service.football;

import cn.iocoder.yudao.module.oa.dal.dataobject.football.AuthorArticleDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthorArticleJsonHelperTest {

    @Test
    void normalizePrivilegeTypes_scalarToArray() {
        assertEquals("[2]", AuthorArticleJsonHelper.normalizePrivilegeTypes("2"));
        assertEquals("[2]", AuthorArticleJsonHelper.normalizePrivilegeTypes("[2]"));
        assertEquals("[0,1]", AuthorArticleJsonHelper.normalizePrivilegeTypes("0,1"));
        assertEquals("[2]", AuthorArticleJsonHelper.normalizePrivilegeTypes(null));
    }

    @Test
    void normalizeMatchScheme_invalidToNull() {
        assertNull(AuthorArticleJsonHelper.normalizeMatchScheme(null));
        assertNull(AuthorArticleJsonHelper.normalizeMatchScheme(""));
        assertNull(AuthorArticleJsonHelper.normalizeMatchScheme("2"));
        assertEquals("[{}]", AuthorArticleJsonHelper.normalizeMatchScheme("[{}]"));
    }

    @Test
    void normalizeJsonFields_onArticle() {
        AuthorArticleDO article = new AuthorArticleDO();
        article.setPrivilegeTypes("2");
        article.setMatchScheme("");
        AuthorArticleJsonHelper.normalizeJsonFieldsForInsert(article);
        assertEquals("[2]", article.getPrivilegeTypes());
        assertNull(article.getMatchScheme());
    }

    @Test
    void normalizeJsonFields_partialUpdateSkipsNull() {
        AuthorArticleDO patch = new AuthorArticleDO();
        patch.setTitle("x");
        AuthorArticleJsonHelper.normalizeJsonFields(patch);
        assertNull(patch.getPrivilegeTypes());
    }
}
