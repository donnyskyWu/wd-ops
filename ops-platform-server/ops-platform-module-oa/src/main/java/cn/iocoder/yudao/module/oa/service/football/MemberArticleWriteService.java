package cn.iocoder.yudao.module.oa.service.football;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.biz.member.article.ArticleApi;
import cn.iocoder.yudao.framework.common.biz.member.article.dto.ArticleSaveDTO;
import cn.iocoder.yudao.framework.common.biz.member.article.dto.ArticleStatusChangeDTO;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
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
 * G-MEM-03 cutover: create/update Feign-only via {@link ArticleApi}.
 * {@link #getById} 仍 @DS member（无 Feign 读路径；待 G-MEM read RPC 或移除调用方）。
 */
@Service
@RequiredArgsConstructor
public class MemberArticleWriteService {

    private final AuthorArticleMapper authorArticleMapper;
    private final ArticleApi articleApi;

    public void insert(AuthorArticleDO article) {
        AuthorArticleJsonHelper.normalizeJsonFieldsForInsert(article);
        insertViaFeign(article);
    }

    @DS("member")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AuthorArticleDO getById(Long id) {
        return authorArticleMapper.selectById(id);
    }

    public void updateById(AuthorArticleDO article) {
        AuthorArticleJsonHelper.normalizeJsonFields(article);
        updateViaFeign(article);
    }

    void insertViaFeign(AuthorArticleDO article) {
        if (articleApi == null) {
            throw rpcUnavailable();
        }
        try {
            ArticleSaveDTO dto = toSaveDto(article, true);
            CommonResult<Long> result = articleApi.createArticle(dto);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                throw rpcUnavailable();
            }
            article.setId(result.getData());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    void updateViaFeign(AuthorArticleDO article) {
        if (articleApi == null || article.getId() == null) {
            throw rpcUnavailable();
        }
        try {
            if (isStatusChangeOnly(article)) {
                ArticleStatusChangeDTO dto = new ArticleStatusChangeDTO();
                dto.setId(article.getId());
                dto.setStatus(article.getStatus());
                CommonResult<Boolean> result = articleApi.statusChange(dto);
                if (result == null || !result.isSuccess() || !Boolean.TRUE.equals(result.getData())) {
                    throw rpcUnavailable();
                }
                return;
            }
            ArticleSaveDTO dto = toSaveDto(article, false);
            CommonResult<Boolean> result = articleApi.updateArticle(dto);
            if (result == null || !result.isSuccess() || !Boolean.TRUE.equals(result.getData())) {
                throw rpcUnavailable();
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
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

    private static ServiceException rpcUnavailable() {
        return new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                "文章写入服务不可用，请确认 member-server 已启动");
    }
}
