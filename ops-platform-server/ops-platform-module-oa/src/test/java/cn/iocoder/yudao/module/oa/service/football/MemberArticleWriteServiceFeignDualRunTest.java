package cn.iocoder.yudao.module.oa.service.football;

import cn.iocoder.yudao.framework.common.biz.member.article.ArticleApi;
import cn.iocoder.yudao.framework.common.biz.member.article.dto.ArticleSaveDTO;
import cn.iocoder.yudao.framework.common.biz.member.article.dto.ArticleStatusChangeDTO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.football.AuthorArticleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.football.AuthorArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberArticleWriteServiceFeignDualRunTest {

    @Mock
    private AuthorArticleMapper authorArticleMapper;
    @Mock
    private ArticleApi articleApi;

    private MemberArticleWriteService service;

    @BeforeEach
    void setUp() {
        service = new MemberArticleWriteService(authorArticleMapper, articleApi);
    }

    @Test
    @DisplayName("G-MEM-03: insert 优先 createArticle Feign")
    void insertPrefersFeignCreate() {
        AuthorArticleDO article = sampleArticle();
        when(articleApi.createArticle(any(ArticleSaveDTO.class))).thenReturn(CommonResult.success(90001L));

        service.insert(article);

        assertEquals(90001L, article.getId());
        verify(authorArticleMapper, never()).insert(any(AuthorArticleDO.class));
    }

    @Test
    @DisplayName("G-MEM-03: update 仅 status 时走 statusChange Feign")
    void updateStatusOnlyUsesStatusChange() {
        AuthorArticleDO patch = new AuthorArticleDO();
        patch.setId(90001L);
        patch.setStatus(1);
        when(articleApi.statusChange(any(ArticleStatusChangeDTO.class))).thenReturn(CommonResult.success(true));

        service.updateById(patch);

        verify(articleApi).statusChange(any(ArticleStatusChangeDTO.class));
        verify(articleApi, never()).updateArticle(any());
        verify(authorArticleMapper, never()).updateById(any(AuthorArticleDO.class));
    }

    @Test
    @DisplayName("G-MEM-03: update 含 title 时走 updateArticle Feign")
    void updateWithTitleUsesUpdateArticle() {
        AuthorArticleDO patch = new AuthorArticleDO();
        patch.setId(90001L);
        patch.setTitle("新标题");
        when(articleApi.updateArticle(any(ArticleSaveDTO.class))).thenReturn(CommonResult.success(true));

        service.updateById(patch);

        verify(articleApi).updateArticle(any(ArticleSaveDTO.class));
        verify(authorArticleMapper, never()).updateById(any(AuthorArticleDO.class));
    }

    @Test
    @DisplayName("G-MEM-03: Feign 失败时 insert 回退 @DS")
    void insertFallsBackToDsWhenFeignFails() {
        AuthorArticleDO article = sampleArticle();
        when(articleApi.createArticle(any())).thenThrow(new RuntimeException("member-server down"));

        service.insert(article);

        verify(authorArticleMapper).insert(article);
    }

    @Test
    @DisplayName("G-MEM-03: isStatusChangeOnly 识别纯上下架 patch")
    void detectsStatusChangeOnlyPatch() {
        AuthorArticleDO patch = new AuthorArticleDO();
        patch.setId(1L);
        patch.setStatus(0);
        assertTrue(MemberArticleWriteService.isStatusChangeOnly(patch));
    }

    private static AuthorArticleDO sampleArticle() {
        AuthorArticleDO article = new AuthorArticleDO();
        article.setAuthorId(1001L);
        article.setTitle("周末竞足方案");
        article.setContent("<p>正文</p>");
        article.setStatus(-1);
        article.setPrice(new BigDecimal("88.00"));
        article.setPrivilegeTypes("[2]");
        article.setRefundType(0);
        article.setSortNum(0);
        article.setMatchType(1);
        return article;
    }
}
