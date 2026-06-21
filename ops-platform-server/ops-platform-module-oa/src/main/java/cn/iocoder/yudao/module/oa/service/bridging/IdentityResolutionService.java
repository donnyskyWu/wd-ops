package cn.iocoder.yudao.module.oa.service.bridging;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.oa.dal.dataobject.bridging.PrivateDomainConversionBridgeDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.collect.AochuangFriendDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.phone.PhoneDO;
import cn.iocoder.yudao.module.oa.dal.mysql.bridging.PrivateDomainConversionBridgeMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.phone.PhoneMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 跨通道身份解析（M10-AO-S-07 骨架 · a1f1ec21）。
 * P0：手机号规则；P2：AI 昵称/头像增强预留。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityResolutionService {

    private static final String IDENTITY_AOCHUANG_FRIEND = "AOCHUANG_FRIEND";
    private static final String IDENTITY_PHONE = "PHONE";
    private static final String MATCH_PHONE = "PHONE";
    private static final String MATCH_AI = "AI";
    private static final String REVIEW_PENDING = "PENDING";
    private static final String REVIEW_APPROVED = "APPROVED";
    private static final BigDecimal AUTO_APPROVE_THRESHOLD = new BigDecimal("85.00");
    private static final BigDecimal PHONE_MATCH_CONFIDENCE = new BigDecimal("95.00");

    private final PrivateDomainConversionBridgeMapper bridgeMapper;
    private final PhoneMapper phoneMapper;
    private final PrivateDomainBridgeService bridgeService;

    /**
     * 好友同步后尝试规则匹配（备注手机号 → oa_phone）。
     */
    public void tryResolveAfterFriendUpsert(AochuangFriendDO friend) {
        if (friend == null || friend.getId() == null || StrUtil.isBlank(friend.getRemark())) {
            return;
        }
        extractPhone(friend.getRemark()).ifPresent(phone -> tryPhoneMatch(friend, phone));
    }

    /**
     * P2 预留：AI 昵称/头像 fuzzy 匹配，当前不实现。
     */
    public void tryAiMatch(AochuangFriendDO friend) {
        log.debug("AI identity match deferred to P2, friendId={}", friend != null ? friend.getId() : null);
    }

    private void tryPhoneMatch(AochuangFriendDO friend, String phoneNumber) {
        Long tenantId = friend.getTenantId();
        String hash = DigestUtil.sha256Hex(phoneNumber);
        PhoneDO phone = phoneMapper.selectOne(new LambdaQueryWrapper<PhoneDO>()
                .eq(PhoneDO::getTenantId, tenantId)
                .eq(PhoneDO::getPhoneNumberHash, hash)
                .last("LIMIT 1"));
        if (phone == null) {
            return;
        }
        if (bridgeService.existsPair(tenantId, IDENTITY_AOCHUANG_FRIEND, friend.getId(),
                IDENTITY_PHONE, phone.getId())) {
            return;
        }
        String reviewStatus = PHONE_MATCH_CONFIDENCE.compareTo(AUTO_APPROVE_THRESHOLD) >= 0
                ? REVIEW_APPROVED : REVIEW_PENDING;
        bridgeService.createBridge(tenantId, IDENTITY_AOCHUANG_FRIEND, friend.getId(),
                IDENTITY_PHONE, phone.getId(), MATCH_PHONE, PHONE_MATCH_CONFIDENCE,
                "{\"phoneExtracted\":\"" + maskPhone(phoneNumber) + "\"}", reviewStatus);
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    static Optional<String> extractPhone(String remark) {
        if (StrUtil.isBlank(remark)) {
            return Optional.empty();
        }
        Matcher matcher = PHONE_PATTERN.matcher(remark);
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
