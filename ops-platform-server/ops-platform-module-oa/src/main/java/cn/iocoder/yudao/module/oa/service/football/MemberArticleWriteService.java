package cn.iocoder.yudao.module.oa.service.football;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.biz.member.article.ArticleApi;
import cn.iocoder.yudao.framework.common.biz.member.article.dto.ArticleSaveDTO;
import cn.iocoder.yudao.framework.common.biz.member.article.dto.ArticleStatusChangeDTO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.football.AuthorArticleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.football.AuthorArticleMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Member DB writes outside master {@code @Transactional} so {@code @DS("member")} routing works (ADR-051/054).
 * G-MEM-03: Feign {@link ArticleApi} dual-run first; @DS fallback for H2 IT / unavailable member-server.
 */
@Service
@RequiredArgsConstructor
public class MemberArticleWriteService {

    private final AuthorArticleMapper authorArticleMapper;
    private final ArticleApi articleApi;

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void insert(AuthorArticleDO article) {
        AuthorArticleJsonHelper.normalizeJsonFieldsForInsert(article);
        if (tryInsertViaFeign(article)) {
            return;
        }
        authorArticleMapper.insert(article);
    }

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthorArticleDO getById(Long id) {
        return authorArticleMapper.selectById(id);
    }

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void updateById(AuthorArticleDO article) {
        AuthorArticleJsonHelper.normalizeJsonFields(article);
        if (tryUpdateViaFeign(article)) {
            return;
        }
        authorArticleMapper.updateById(article);
    }

    boolean tryInsertViaFeign(AuthorArticleDO article) {
        if (articleApi == null) {
            return false;
        }
        try {
            ArticleSaveDTO dto = toSaveDto(article, true);
            CommonResult<Long> result = articleApi.createArticle(dto);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                return false;
            }
            article.setId(result.getData());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    boolean tryUpdateViaFeign(AuthorArticleDO article) {
        if (articleApi == null || article.getId() == null) {
            return false;
        }
        try {
            if (isStatusChangeOnly(article)) {
                ArticleStatusChangeDTO dto = new ArticleStatusChangeDTO();
                dto.setId(article.getId());
                dto.setStatus(article.getStatus());
                CommonResult<Boolean> result = articleApi.statusChange(dto);
                if (result != null && result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                    return true;
                }
                return false;
            }
            ArticleSaveDTO dto = toSaveDto(article, false);
            CommonResult<Boolean> result = articleApi.updateArticle(dto);
            return result != null && result.isSuccess() && Boolean.TRUE.equals(result.getData());
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean isStatusChangeOnly(AuthorArticleDO article) {
        if (article.getStatus() == null || (article.getStatus() != 0 && article.getStatus() != 1)) {
            return false;
        }
        return article.getTitle() == null
                && article.getContent() == null
                && article.getFreeContent() == null
                && article.getPublishTime() == null
                && article.getOrderDeadline() == null;
    }

    static ArticleSaveDTO toSaveDto(AuthorArticleDO article, boolean create) {
        ArticleSaveDTO dto = new ArticleSaveDTO();
        dto.setId(article.getId());
        dto.setAuthorId(article.getAuthorId());
        dto.setTitle(article.getTitle());
        dto.setIntro(article.getIntro());
        dto.setFreeContent(article.getFreeContent());
        dto.setContent(article.getContent());
        dto.setPrice(article.getPrice());
        dto.setRefundType(article.getRefundType());
        dto.setSortNum(article.getSortNum());
        dto.setStatus(article.getStatus());
        dto.setMatchType(article.getMatchType());
        dto.setPublishTime(article.getPublishTime());
        dto.setOrderDeadline(article.getOrderDeadline());
        dto.setPrivilegeTypes(parsePrivilegeTypes(article.getPrivilegeTypes()));
        if (create) {
            dto.setSchedulePublishStatus(0);
        }
        return dto;
    }

    static List<Integer> parsePrivilegeTypes(String raw) {
        String normalized = AuthorArticleJsonHelper.normalizePrivilegeTypes(raw);
        if (StrUtil.isBlank(normalized)) {
            return List.of(2);
        }
        try {
            return JSONUtil.toList(normalized, Integer.class);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
