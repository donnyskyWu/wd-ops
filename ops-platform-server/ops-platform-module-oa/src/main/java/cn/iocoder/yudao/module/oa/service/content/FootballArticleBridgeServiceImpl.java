package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.content.FootballSchemeVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentExtDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.football.AuthorArticleDO;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentExtMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.content.ProductionContentMapper;
import cn.iocoder.yudao.module.oa.service.football.AuthorArticleJsonHelper;
import cn.iocoder.yudao.module.oa.service.football.MemberArticleWriteService;
import cn.iocoder.yudao.module.oa.util.LayoutJsonHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * OPS → Football author_article 桥接（ADR-054 §9）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FootballArticleBridgeServiceImpl implements FootballArticleBridgeService {

    private static final int STATUS_DRAFT = -1;
    private static final int STATUS_OFF = 0;
    private static final int STATUS_ON = 1;
    private static final int TITLE_MAX_LEN = 35;
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("88.00");
    /** member-server ArticleDO.privilegeTypes 为 Jackson List&lt;Integer&gt;，须存 JSON 数组如 [2]（ADR-054 addendum） */
    private static final String DEFAULT_PRIVILEGE_TYPES = AuthorArticleJsonHelper.DEFAULT_PRIVILEGE_TYPES_JSON;
    private static final int DEFAULT_REFUND_TYPE = 0;
    private static final int DEFAULT_SORT_NUM = 0;
    private static final Integer DEFAULT_MATCH_TYPE = 1;
    private static final String SOURCE_OPS = "OPS";

    private final ProductionContentMapper productionContentMapper;
    private final ProductionContentExtMapper productionContentExtMapper;
    private final MemberArticleWriteService memberArticleWriteService;

    @Override
    public void syncDraftOnCreate(ProductionContentDO content) {
        try {
            ProductionContentExtDO ext = requireOrCreateExt(content);
            if (ext.getAuthorArticleId() != null) {
                doSyncUpdate(content, ext);
                return;
            }
            AuthorArticleDO article = buildArticleFromContent(content, true);
            memberArticleWriteService.insert(article);
            markSyncSuccess(ext, article.getId());
        } catch (Exception ex) {
            log.warn("Football draft sync failed on create, contentId={}: {}", content.getId(), ex.getMessage(), ex);
            recordSyncError(content.getId(), ex);
        }
    }

    @Override
    public void syncOnUpdate(ProductionContentDO content) {
        try {
            ProductionContentExtDO ext = requireOrCreateExt(content);
            if (ext.getAuthorArticleId() == null) {
                syncDraftOnCreate(content);
                return;
            }
            doSyncUpdate(content, ext);
        } catch (Exception ex) {
            log.warn("Football sync failed on update, contentId={}: {}", content.getId(), ex.getMessage(), ex);
            recordSyncError(content.getId(), ex);
        }
    }

    @Override
    @Transactional
    public FootballSchemeVO retrySync(Long productionContentId) {
        ProductionContentDO content = requireContent(productionContentId);
        ProductionContentExtDO ext = requireOrCreateExt(content);
        try {
            if (ext.getAuthorArticleId() == null) {
                AuthorArticleDO article = buildArticleFromContent(content, true);
                memberArticleWriteService.insert(article);
                markSyncSuccess(ext, article.getId());
            } else {
                doSyncUpdate(content, ext);
            }
        } catch (Exception ex) {
            recordSyncError(productionContentId, ex);
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "Football 方案同步失败：" + truncateError(ex.getMessage()));
        }
        return buildSchemeVO(content.getId(), requireExtByContentId(content.getId()));
    }

    @Override
    public FootballSchemeVO getFootballScheme(Long productionContentId) {
        ProductionContentDO content = requireContent(productionContentId);
        ProductionContentExtDO ext = productionContentExtMapper.selectOne(new LambdaQueryWrapper<ProductionContentExtDO>()
                .eq(ProductionContentExtDO::getTenantId, content.getTenantId())
                .eq(ProductionContentExtDO::getProductionContentId, productionContentId)
                .last("LIMIT 1"));
        return buildSchemeVO(productionContentId, ext);
    }

    @Override
    @Transactional
    public FootballSchemeVO shelfOn(Long productionContentId) {
        ProductionContentDO content = requireContent(productionContentId);
        ProductionContentExtDO ext = requireExtWithArticle(content);
        AuthorArticleDO patch = new AuthorArticleDO();
        patch.setId(ext.getAuthorArticleId());
        patch.setStatus(STATUS_ON);
        LocalDateTime now = LocalDateTime.now();
        AuthorArticleDO existing = memberArticleWriteService.getById(ext.getAuthorArticleId());
        if (existing != null && existing.getPublishTime() == null) {
            patch.setPublishTime(now);
        }
        if (existing != null && existing.getOrderDeadline() == null) {
            patch.setOrderDeadline(now.plusHours(2));
        }
        patch.setUpdater(TenantContextHolder.getUsername());
        patch.setUpdateTime(now);
        memberArticleWriteService.updateById(patch);
        return buildSchemeVO(productionContentId, ext);
    }

    @Override
    @Transactional
    public FootballSchemeVO shelfOff(Long productionContentId) {
        ProductionContentDO content = requireContent(productionContentId);
        ProductionContentExtDO ext = requireExtWithArticle(content);
        AuthorArticleDO patch = new AuthorArticleDO();
        patch.setId(ext.getAuthorArticleId());
        patch.setStatus(STATUS_OFF);
        patch.setUpdater(TenantContextHolder.getUsername());
        patch.setUpdateTime(LocalDateTime.now());
        memberArticleWriteService.updateById(patch);
        return buildSchemeVO(productionContentId, ext);
    }

    private void doSyncUpdate(ProductionContentDO content, ProductionContentExtDO ext) {
        AuthorArticleDO patch = new AuthorArticleDO();
        patch.setId(ext.getAuthorArticleId());
        patch.setTitle(sanitizeTitle(content.getTitle()));
        patch.setContent(sanitizeBody(resolvePaidBody(content)));
        // ADR-054 §6.3：OPS free_body 为 null 表示未改免费栏，不覆盖 author_article.free_content
        if (content.getFreeBody() != null) {
            patch.setFreeContent(sanitizeOptionalBody(content.getFreeBody()));
        }
        patch.setUpdater(TenantContextHolder.getUsername());
        patch.setUpdateTime(LocalDateTime.now());
        memberArticleWriteService.updateById(patch);
        markSyncSuccess(ext, ext.getAuthorArticleId());
    }

    private AuthorArticleDO buildArticleFromContent(ProductionContentDO content, boolean draft) {
        if (content.getAuthorId() == null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "同步 Football 方案须指定作者");
        }
        AuthorArticleDO article = new AuthorArticleDO();
        article.setAuthorId(content.getAuthorId());
        article.setTitle(sanitizeTitle(content.getTitle()));
        article.setContent(sanitizeBody(resolvePaidBody(content)));
        article.setFreeContent(sanitizeOptionalBody(content.getFreeBody()));
        article.setStatus(draft ? STATUS_DRAFT : STATUS_OFF);
        article.setPrice(DEFAULT_PRICE);
        article.setPrivilegeTypes(DEFAULT_PRIVILEGE_TYPES);
        article.setRefundType(DEFAULT_REFUND_TYPE);
        article.setSortNum(DEFAULT_SORT_NUM);
        article.setMatchType(DEFAULT_MATCH_TYPE);
        article.setMatchScheme(null);
        AuthorArticleJsonHelper.normalizeJsonFieldsForInsert(article);
        article.setTenantId(content.getTenantId());
        String username = TenantContextHolder.getUsername();
        LocalDateTime now = LocalDateTime.now();
        article.setCreator(username);
        article.setUpdater(username);
        article.setCreateTime(now);
        article.setUpdateTime(now);
        article.setDeleted(0);
        return article;
    }

    private ProductionContentExtDO requireOrCreateExt(ProductionContentDO content) {
        ProductionContentExtDO ext = productionContentExtMapper.selectOne(new LambdaQueryWrapper<ProductionContentExtDO>()
                .eq(ProductionContentExtDO::getTenantId, content.getTenantId())
                .eq(ProductionContentExtDO::getProductionContentId, content.getId())
                .last("LIMIT 1"));
        if (ext != null) {
            refreshExtMetadata(ext, content);
            return ext;
        }
        ext = new ProductionContentExtDO();
        ext.setTenantId(content.getTenantId());
        ext.setProductionContentId(content.getId());
        ext.setIpGroupId(content.getIpGroupId());
        ext.setTaskId(content.getTaskId());
        ext.setSchemeTypes(content.getSchemeType());
        ext.setCompetitionId(content.getCompetitionId());
        ext.setCompetitionName(content.getCompetitionName());
        ext.setSource(SOURCE_OPS);
        ext.setCreator(TenantContextHolder.getUsername());
        ext.setUpdater(TenantContextHolder.getUsername());
        ext.setCreateTime(LocalDateTime.now());
        ext.setUpdateTime(LocalDateTime.now());
        productionContentExtMapper.insert(ext);
        return ext;
    }

    private void refreshExtMetadata(ProductionContentExtDO ext, ProductionContentDO content) {
        boolean changed = false;
        if (!Objects.equals(ext.getIpGroupId(), content.getIpGroupId())) {
            ext.setIpGroupId(content.getIpGroupId());
            changed = true;
        }
        if (!Objects.equals(ext.getTaskId(), content.getTaskId())) {
            ext.setTaskId(content.getTaskId());
            changed = true;
        }
        if (!Objects.equals(ext.getSchemeTypes(), content.getSchemeType())) {
            ext.setSchemeTypes(content.getSchemeType());
            changed = true;
        }
        if (!Objects.equals(ext.getCompetitionId(), content.getCompetitionId())) {
            ext.setCompetitionId(content.getCompetitionId());
            changed = true;
        }
        if (!Objects.equals(ext.getCompetitionName(), content.getCompetitionName())) {
            ext.setCompetitionName(content.getCompetitionName());
            changed = true;
        }
        if (changed) {
            ext.setUpdater(TenantContextHolder.getUsername());
            ext.setUpdateTime(LocalDateTime.now());
            productionContentExtMapper.updateById(ext);
        }
    }

    private void markSyncSuccess(ProductionContentExtDO ext, Long authorArticleId) {
        ext.setAuthorArticleId(authorArticleId);
        ext.setSyncFootballAt(LocalDateTime.now());
        ext.setFootballSyncError(null);
        ext.setUpdater(TenantContextHolder.getUsername());
        ext.setUpdateTime(LocalDateTime.now());
        productionContentExtMapper.updateById(ext);
    }

    private void recordSyncError(Long productionContentId, Exception ex) {
        ProductionContentExtDO ext = productionContentExtMapper.selectOne(new LambdaQueryWrapper<ProductionContentExtDO>()
                .eq(ProductionContentExtDO::getProductionContentId, productionContentId)
                .last("LIMIT 1"));
        if (ext == null) {
            return;
        }
        ext.setFootballSyncError(truncateError(ex.getMessage()));
        ext.setUpdater(TenantContextHolder.getUsername());
        ext.setUpdateTime(LocalDateTime.now());
        productionContentExtMapper.updateById(ext);
    }

    private FootballSchemeVO buildSchemeVO(Long productionContentId, ProductionContentExtDO ext) {
        FootballSchemeVO vo = new FootballSchemeVO();
        vo.setProductionContentId(productionContentId);
        if (ext == null) {
            return vo;
        }
        vo.setAuthorArticleId(ext.getAuthorArticleId());
        vo.setFootballSyncError(ext.getFootballSyncError());
        vo.setSyncFootballAt(ext.getSyncFootballAt());
        if (ext.getAuthorArticleId() != null) {
            AuthorArticleDO article = memberArticleWriteService.getById(ext.getAuthorArticleId());
            if (article != null) {
                vo.setShelfStatus(article.getStatus());
            }
        }
        return vo;
    }

    private ProductionContentExtDO requireExtByContentId(Long productionContentId) {
        return productionContentExtMapper.selectOne(new LambdaQueryWrapper<ProductionContentExtDO>()
                .eq(ProductionContentExtDO::getProductionContentId, productionContentId)
                .last("LIMIT 1"));
    }

    private ProductionContentExtDO requireExtWithArticle(ProductionContentDO content) {
        ProductionContentExtDO ext = requireExtByContentId(content.getId());
        if (ext == null || ext.getAuthorArticleId() == null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "Football 方案尚未同步，请先执行 sync-football-scheme");
        }
        return ext;
    }

    private ProductionContentDO requireContent(Long id) {
        ProductionContentDO entity = productionContentMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !Objects.equals(entity.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    /**
     * LAYOUT 正文 SSOT 为 layout_html（富文本 HTML）；paid_body 在 OPS 编辑流中常回填为 body 纯文本。
     * Football sync 须优先 layout_html，否则 author_article.content 丢失格式（ADR-054 §6.2）。
     */
    static String resolvePaidBody(ProductionContentDO content) {
        if ("LAYOUT".equals(content.getBodyFormat()) && StrUtil.isNotBlank(content.getLayoutHtml())) {
            return content.getLayoutHtml();
        }
        if (StrUtil.isNotBlank(content.getPaidBody())) {
            return content.getPaidBody();
        }
        return StrUtil.blankToDefault(content.getBody(), "");
    }

    private static String sanitizeTitle(String title) {
        String normalized = StrUtil.blankToDefault(title, "").trim();
        return normalized.length() <= TITLE_MAX_LEN ? normalized : normalized.substring(0, TITLE_MAX_LEN);
    }

    private static String sanitizeBody(String body) {
        return LayoutJsonHelper.sanitizeHtml(StrUtil.blankToDefault(body, ""));
    }

    private static String sanitizeOptionalBody(String body) {
        if (StrUtil.isBlank(body)) {
            return null;
        }
        return LayoutJsonHelper.sanitizeHtml(body);
    }

    private static String truncateError(String message) {
        String text = StrUtil.blankToDefault(message, "unknown error");
        return text.length() <= 512 ? text : text.substring(0, 512);
    }
}
