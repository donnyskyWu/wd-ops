package cn.iocoder.yudao.module.oa.service.football;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.oa.dal.dataobject.football.AuthorArticleDO;

/**
 * member-server {@code ArticleDO} JacksonTypeHandler 字段写入规范（ADR-054 addendum）。
 * <p>
 * {@code privilege_types}、{@code match_scheme} 等须为合法 JSON 数组或 NULL；
 * DB 列默认 {@code '2'} 为标量，会导致 GET /member/article/get 反序列化 500。
 */
public final class AuthorArticleJsonHelper {

    /** member-server ArticleDO.privilegeTypes = List&lt;Integer&gt; */
    public static final String DEFAULT_PRIVILEGE_TYPES_JSON = "[2]";

    private AuthorArticleJsonHelper() {
    }

    public static void normalizeJsonFields(AuthorArticleDO article) {
        if (article == null) {
            return;
        }
        if (article.getPrivilegeTypes() != null) {
            article.setPrivilegeTypes(normalizePrivilegeTypes(article.getPrivilegeTypes()));
        }
        if (article.getMatchScheme() != null) {
            article.setMatchScheme(normalizeMatchScheme(article.getMatchScheme()));
        }
    }

    /** Insert 时必须写入合法 JSON 默认值，避免 DB 列默认标量 {@code '2'} 导致 member get 500。 */
    public static void normalizeJsonFieldsForInsert(AuthorArticleDO article) {
        if (article == null) {
            return;
        }
        article.setPrivilegeTypes(normalizePrivilegeTypes(article.getPrivilegeTypes()));
        article.setMatchScheme(normalizeMatchScheme(article.getMatchScheme()));
    }

    static String normalizePrivilegeTypes(String raw) {
        if (StrUtil.isBlank(raw)) {
            return DEFAULT_PRIVILEGE_TYPES_JSON;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            return trimmed;
        }
        if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(parts[i].trim());
            }
            sb.append(']');
            return sb.toString();
        }
        return "[" + trimmed + "]";
    }

    /**
     * match_scheme 须为 JSON 数组（List&lt;MatchBaseVO&gt;）或 NULL；空串/标量会导致详情 500。
     */
    static String normalizeMatchScheme(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("[")) {
            return null;
        }
        return trimmed;
    }
}
