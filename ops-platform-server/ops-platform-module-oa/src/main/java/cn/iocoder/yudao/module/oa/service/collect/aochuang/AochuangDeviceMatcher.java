package cn.iocoder.yudao.module.oa.service.collect.aochuang;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.service.config.aochuang.AochuangWechatAccountDTO;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 奥创设备 ↔ OA 个微档案匹配（ADR-045 §3）。
 */
@Component
public class AochuangDeviceMatcher {

    private static final double AUTO_BIND_THRESHOLD = 0.85;
    private static final double SUGGEST_THRESHOLD = 0.60;

    public Optional<PersonalWechatAccountDO> findAutoMatch(AochuangWechatAccountDTO device,
                                                           List<PersonalWechatAccountDO> candidates) {
        if (device == null || candidates == null) {
            return Optional.empty();
        }
        for (PersonalWechatAccountDO row : candidates) {
            if (isAlreadyBoundToOtherDevice(row, device.getWechatAccountId())) {
                continue;
            }
            if (matchesWechatId(device, row)) {
                return Optional.of(row);
            }
            if (matchesAlias(device, row)) {
                return Optional.of(row);
            }
            if (fuzzyScore(device, row) >= AUTO_BIND_THRESHOLD) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    public Optional<MatchSuggestion> findBestSuggestion(AochuangWechatAccountDTO device,
                                                        List<PersonalWechatAccountDO> candidates) {
        if (device == null || candidates == null) {
            return Optional.empty();
        }
        MatchSuggestion best = null;
        for (PersonalWechatAccountDO row : candidates) {
            if (isAlreadyBoundToOtherDevice(row, device.getWechatAccountId())) {
                continue;
            }
            double score = fuzzyScore(device, row);
            if (score < SUGGEST_THRESHOLD) {
                continue;
            }
            if (best == null || score > best.getScore()) {
                best = new MatchSuggestion(row.getId(), score);
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean isAlreadyBoundToOtherDevice(PersonalWechatAccountDO row, String deviceId) {
        return StrUtil.isNotBlank(row.getAochuangWechatAccountId())
                && !row.getAochuangWechatAccountId().equals(deviceId);
    }

    private boolean matchesWechatId(AochuangWechatAccountDTO device, PersonalWechatAccountDO row) {
        if (StrUtil.isBlank(device.getWechatId()) || StrUtil.isBlank(row.getWechatId())) {
            return false;
        }
        return device.getWechatId().equalsIgnoreCase(row.getWechatId());
    }

    private boolean matchesAlias(AochuangWechatAccountDTO device, PersonalWechatAccountDO row) {
        if (StrUtil.isBlank(device.getAlias()) || StrUtil.isBlank(row.getWechatId())) {
            return false;
        }
        return device.getAlias().equalsIgnoreCase(row.getWechatId());
    }

    double fuzzyScore(AochuangWechatAccountDTO device, PersonalWechatAccountDO row) {
        double nicknameSim = nicknameSimilarity(device.getNickname(), row.getAccountName());
        double avatarSim = avatarSimilarity(device.getAvatar(), row.getAochuangAvatar());
        return 0.4 * nicknameSim + 0.6 * avatarSim;
    }

    private double nicknameSimilarity(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        if (StrUtil.isBlank(a) || StrUtil.isBlank(b)) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 1.0;
        }
        return 1.0 - (double) levenshteinDistance(a, b) / maxLen;
    }

    private double avatarSimilarity(String deviceAvatar, String rowAvatar) {
        if (StrUtil.isBlank(deviceAvatar)) {
            return 0.0;
        }
        if (StrUtil.isNotBlank(rowAvatar) && deviceAvatar.equals(rowAvatar)) {
            return 1.0;
        }
        return 0.0;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    @Value
    public static class MatchSuggestion {
        Long personalWechatId;
        double score;
    }
}
