package cn.iocoder.yudao.module.oa.service.support;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;

import java.util.Locale;
import java.util.regex.Pattern;

public final class SqlSafetySupport {

    private static final Pattern UNSAFE = Pattern.compile(
            "(?i)(\\bDROP\\b|\\bDELETE\\b|\\bUPDATE\\b|\\bINSERT\\b|\\bALTER\\b|\\bTRUNCATE\\b|;|--|/\\*)");

    private SqlSafetySupport() {
    }

    public static void assertSelectOnly(String sqlText) {
        if (sqlText == null || sqlText.isBlank()) {
            throw new ServiceException(OaErrorCodes.FORMULA_SYNTAX_ERROR);
        }
        String normalized = sqlText.trim().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("SELECT")) {
            throw new ServiceException(OaErrorCodes.SQL_INJECTION_RISK);
        }
        if (UNSAFE.matcher(sqlText).find()) {
            throw new ServiceException(OaErrorCodes.SQL_INJECTION_RISK);
        }
    }
}
