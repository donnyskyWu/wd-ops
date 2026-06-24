package cn.iocoder.yudao.module.oa.service.config.aochuang;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.oa.config.AochuangProperties;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AoCreateApiDO;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 奥创 OpenAPI 客户端（Channel-B · ADR-045 · Apifox doc-8348826）。
 * <ul>
 *   <li>GET /api/v1/wechatAccounts — 设备列表（pageIndex 从 0）</li>
 *   <li>GET /api/v1/wechatFriends — 好友瀑布流（lastUpdateTime + maxWechatFriendId）</li>
 *   <li>GET /api/v1/wechatMessages — 消息瀑布流（同上）</li>
 * </ul>
 * 凭证映射：{@code apiUrl} 基址；{@code appId} = Account-Name（主账号登录名）；{@code appSecret} = 签名密钥。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AochuangApiClient {

    private static final String URI_WECHAT_ACCOUNTS = "/api/v1/wechatAccounts";
    private static final String URI_WECHAT_FRIENDS = "/api/v1/wechatFriends";
    private static final String URI_WECHAT_MESSAGES = "/api/v1/wechatMessages";
    private static final String DEFAULT_LAST_UPDATE_TIME = "1970-01-01 00:00:00";
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final AochuangProperties properties;
    private final AesUtil aesUtil;

    public List<AochuangWechatAccountDTO> listWechatAccounts(AoCreateApiDO api, String aochuangAccountId) {
        if (properties.isStub()) {
            return stubDevices(aochuangAccountId);
        }
        requireSigningCredentials(api);
        String baseUrl = normalizeBaseUrl(api.getApiUrl());
        String accountName = api.getAppId();
        String secret = decrypt(api.getAppSecretEncrypted());

        List<AochuangWechatAccountDTO> all = new ArrayList<>();
        int pageIndex = 0;
        while (true) {
            Map<String, String> params = AochuangSignatureHelper.orderedParams();
            if (StrUtil.isNotBlank(aochuangAccountId)) {
                params.put("accountId", aochuangAccountId);
            }
            params.put("pageIndex", String.valueOf(pageIndex));
            params.put("pageSize", String.valueOf(DEFAULT_PAGE_SIZE));

            String body = signedGet(baseUrl, URI_WECHAT_ACCOUNTS, params, accountName, secret);
            List<AochuangWechatAccountDTO> page = parseWechatAccounts(body);
            all.addAll(page);
            if (page.size() < DEFAULT_PAGE_SIZE) {
                break;
            }
            pageIndex++;
        }
        return all;
    }

    public AochuangFriendPageResult listFriends(AoCreateApiDO api, String wechatAccountId,
                                                String cursor, int limit) {
        if (properties.isStub()) {
            return stubFriends(wechatAccountId, cursor, limit);
        }
        requireSigningCredentials(api);
        String baseUrl = normalizeBaseUrl(api.getApiUrl());
        String accountName = api.getAppId();
        String secret = decrypt(api.getAppSecretEncrypted());

        Map<String, String> params = AochuangSignatureHelper.orderedParams();
        params.put("wechatAccountId", wechatAccountId);
        params.put("lastUpdateTime", DEFAULT_LAST_UPDATE_TIME);
        if (StrUtil.isNotBlank(cursor)) {
            params.put("maxWechatFriendId", cursor);
        }

        String body = signedGet(baseUrl, URI_WECHAT_FRIENDS, params, accountName, secret);
        return parseFriendsWaterfall(body, cursor);
    }

    public AochuangMessagePageResult listFriendMessages(AoCreateApiDO api, String wechatAccountId,
                                                        String cursor, int limit) {
        if (properties.isStub()) {
            return stubMessages(wechatAccountId, cursor, limit);
        }
        requireSigningCredentials(api);
        String baseUrl = normalizeBaseUrl(api.getApiUrl());
        String accountName = api.getAppId();
        String secret = decrypt(api.getAppSecretEncrypted());

        Map<String, String> params = AochuangSignatureHelper.orderedParams();
        params.put("wechatAccountId", wechatAccountId);
        params.put("lastUpdateTime", DEFAULT_LAST_UPDATE_TIME);
        if (StrUtil.isNotBlank(cursor)) {
            params.put("maxWechatMessageId", cursor);
        }

        String body = signedGet(baseUrl, URI_WECHAT_MESSAGES, params, accountName, secret);
        return parseMessagesWaterfall(body, cursor);
    }

    private void requireSigningCredentials(AoCreateApiDO api) {
        if (StrUtil.isBlank(api.getAppId())) {
            throw new AochuangApiException("TOKEN_FAIL", "奥创 Account-Name（AppID）未配置");
        }
        if (StrUtil.isBlank(decrypt(api.getAppSecretEncrypted()))) {
            throw new AochuangApiException("TOKEN_FAIL", "奥创签名密钥（AppSecret）未配置");
        }
    }

    private String signedGet(String baseUrl, String requestUri, Map<String, String> params,
                             String accountName, String secret) {
        String queryString = AochuangSignatureHelper.encodeQueryString(params);
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String signature = AochuangSignatureHelper.sign(requestUri, queryString, timestamp, accountName, secret);

        String url = baseUrl + requestUri + (queryString.isEmpty() ? "" : "?" + queryString);
        HttpResponse response = HttpRequest.get(url)
                .timeout(20_000)
                .header("Timestamp", timestamp)
                .header("Account-Name", accountName)
                .header("Signature", signature)
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("Signature-Version", AochuangSignatureHelper.SIGNATURE_VERSION)
                .execute();

        int status = response.getStatus();
        String body = response.body();
        if (status == 401 || status == 403) {
            throw new AochuangApiException(status == 401 ? "TOKEN_FAIL" : "PERMISSION_DENIED",
                    parseErrorMessage(body, "奥创 API 鉴权失败 HTTP " + status));
        }
        if (status < 200 || status >= 300) {
            throw new AochuangApiException("DISCONNECTED",
                    parseErrorMessage(body, "奥创 API 请求失败 HTTP " + status));
        }
        return body;
    }

    private String parseErrorMessage(String body, String fallback) {
        if (StrUtil.isBlank(body)) {
            return fallback;
        }
        try {
            JSONObject root = JSONUtil.parseObj(body);
            String msg = root.getStr("msg");
            if (StrUtil.isNotBlank(msg)) {
                return msg;
            }
        } catch (Exception ignored) {
            // use fallback
        }
        return fallback;
    }

    private AochuangFriendPageResult parseFriendsWaterfall(String body, String previousCursor) {
        List<AochuangFriendDTO> friends = parseFriendItems(body);
        if (friends.isEmpty()) {
            return AochuangFriendPageResult.of(Collections.emptyList(), null, false);
        }
        String lastFriendId = friends.get(friends.size() - 1).getFriendId();
        boolean hasMore = friends.size() >= DEFAULT_PAGE_SIZE
                && StrUtil.isNotBlank(lastFriendId)
                && !StrUtil.equals(lastFriendId, previousCursor);
        return AochuangFriendPageResult.of(friends, hasMore ? lastFriendId : null, hasMore);
    }

    private AochuangMessagePageResult parseMessagesWaterfall(String body, String previousCursor) {
        List<AochuangMessageDTO> messages = parseMessageItems(body);
        if (messages.isEmpty()) {
            return AochuangMessagePageResult.of(Collections.emptyList(), null, false);
        }
        String lastMessageId = messages.get(messages.size() - 1).getMessageId();
        boolean hasMore = messages.size() >= DEFAULT_PAGE_SIZE
                && StrUtil.isNotBlank(lastMessageId)
                && !StrUtil.equals(lastMessageId, previousCursor);
        return AochuangMessagePageResult.of(messages, hasMore ? lastMessageId : null, hasMore);
    }

    // --- stub implementations (unchanged behavior for IT) ---

    private List<AochuangWechatAccountDTO> stubDevices(String aochuangAccountId) {
        if ("invalid-token".equalsIgnoreCase(aochuangAccountId)) {
            throw new AochuangApiException("TOKEN_FAIL", "stub: token invalid");
        }
        if ("no-permission".equalsIgnoreCase(aochuangAccountId)) {
            throw new AochuangApiException("PERMISSION_DENIED", "stub: permission denied");
        }
        if (aochuangAccountId != null && aochuangAccountId.toLowerCase().startsWith("acct-sync-test")) {
            String suffix = aochuangAccountId.substring("acct-sync-test".length());
            AochuangWechatAccountDTO auto = new AochuangWechatAccountDTO();
            auto.setWechatAccountId("stub-device-auto" + suffix);
            auto.setWechatId("wx_auto_match" + suffix);
            auto.setNickname("Auto Match Device");
            auto.setAvatar("https://stub.local/a.png");
            auto.setIsAlive(true);

            AochuangWechatAccountDTO pending = new AochuangWechatAccountDTO();
            pending.setWechatAccountId("stub-device-pending" + suffix);
            pending.setWechatId("wx_pending_only" + suffix);
            pending.setNickname("Pending Device");
            pending.setIsAlive(true);
            return List.of(auto, pending);
        }
        AochuangWechatAccountDTO dto = new AochuangWechatAccountDTO();
        dto.setWechatAccountId("stub-device-1");
        dto.setWechatId("wx_stub_001");
        dto.setNickname("Stub Device");
        dto.setIsAlive(true);
        return List.of(dto);
    }

    private AochuangFriendPageResult stubFriends(String wechatAccountId, String cursor, int limit) {
        if (wechatAccountId != null && wechatAccountId.contains("api-fail")) {
            throw new AochuangApiException("TOKEN_FAIL", "stub: friend sync fail");
        }
        if (wechatAccountId != null && wechatAccountId.contains("no-friends")) {
            return AochuangFriendPageResult.of(Collections.emptyList(), null, false);
        }
        List<AochuangFriendDTO> all = buildStubFriendList(wechatAccountId);
        int pageSize = limit <= 0 ? 2 : limit;
        int pageIndex = 0;
        if (StrUtil.isNotBlank(cursor)) {
            try {
                pageIndex = Integer.parseInt(cursor);
            } catch (NumberFormatException ignored) {
                pageIndex = 0;
            }
        }
        int from = pageIndex * pageSize;
        if (from >= all.size()) {
            return AochuangFriendPageResult.of(Collections.emptyList(), null, false);
        }
        int to = Math.min(from + pageSize, all.size());
        List<AochuangFriendDTO> page = all.subList(from, to);
        boolean hasMore = to < all.size();
        String nextCursor = hasMore ? String.valueOf(pageIndex + 1) : null;
        return AochuangFriendPageResult.of(page, nextCursor, hasMore);
    }

    private AochuangMessagePageResult stubMessages(String wechatAccountId, String cursor, int limit) {
        if (wechatAccountId != null && wechatAccountId.contains("no-messages")) {
            return AochuangMessagePageResult.of(Collections.emptyList(), null, false);
        }
        List<AochuangMessageDTO> all = buildStubMessageList(wechatAccountId);
        int pageSize = limit <= 0 ? 2 : limit;
        int pageIndex = 0;
        if (StrUtil.isNotBlank(cursor)) {
            try {
                pageIndex = Integer.parseInt(cursor);
            } catch (NumberFormatException ignored) {
                pageIndex = 0;
            }
        }
        int from = pageIndex * pageSize;
        if (from >= all.size()) {
            return AochuangMessagePageResult.of(Collections.emptyList(), null, false);
        }
        int to = Math.min(from + pageSize, all.size());
        List<AochuangMessageDTO> page = all.subList(from, to);
        boolean hasMore = to < all.size();
        String nextCursor = hasMore ? String.valueOf(pageIndex + 1) : null;
        return AochuangMessagePageResult.of(page, nextCursor, hasMore);
    }

    private List<AochuangMessageDTO> buildStubMessageList(String wechatAccountId) {
        LocalDateTime today = LocalDate.now().atTime(10, 0);
        if (wechatAccountId != null && wechatAccountId.contains("paged")) {
            return List.of(
                    stubMessage("stub-msg-1", "stub-friend-a", "SENT", "你好", today.minusDays(1)),
                    stubMessage("stub-msg-2", "stub-friend-a", "RECEIVED", "在的", today.minusDays(1).plusHours(1)),
                    stubMessage("stub-msg-3", "stub-friend-b", "SENT", "跟进一下", today),
                    stubMessage("stub-msg-4", "stub-friend-b", "RECEIVED", "好的", today.plusHours(1)));
        }
        return List.of(
                stubMessage("stub-msg-a", "stub-friend-a", "SENT", "Stub hello", today),
                stubMessage("stub-msg-b", "stub-friend-b", "RECEIVED", "Stub reply", today.plusHours(2)));
    }

    private AochuangMessageDTO stubMessage(String messageId, String friendId, String direction,
                                           String content, LocalDateTime messageTime) {
        AochuangMessageDTO dto = new AochuangMessageDTO();
        dto.setMessageId(messageId);
        dto.setFriendId(friendId);
        dto.setDirection(direction);
        dto.setMsgType("TEXT");
        dto.setContent(content);
        dto.setMessageTime(messageTime);
        return dto;
    }

    private List<AochuangFriendDTO> buildStubFriendList(String wechatAccountId) {
        if (wechatAccountId != null && wechatAccountId.contains("paged")) {
            return List.of(
                    stubFriend("stub-friend-1", "wx_friend_001", "好友甲", "remark-stub-friend-1"),
                    stubFriend("stub-friend-2", "wx_friend_002", "好友乙", "remark-stub-friend-2"),
                    stubFriend("stub-friend-3", "wx_friend_003", "好友丙", "remark-stub-friend-3"));
        }
        if (wechatAccountId != null && wechatAccountId.contains("bridge-c")) {
            return List.of(
                    stubFriend("stub-friend-bridge", "wx_friend_bridge", "Bridge Friend",
                            "客户手机13800138707"));
        }
        return List.of(
                stubFriend("stub-friend-a", "wx_friend_a", "Stub Friend A", "remark-stub-friend-a"),
                stubFriend("stub-friend-b", "wx_friend_b", "Stub Friend B", "remark-stub-friend-b"));
    }

    private AochuangFriendDTO stubFriend(String friendId, String wechatId, String nickname, String remark) {
        AochuangFriendDTO dto = new AochuangFriendDTO();
        dto.setFriendId(friendId);
        dto.setWechatId(wechatId);
        dto.setNickname(nickname);
        dto.setAvatar("https://stub.local/f/" + friendId + ".png");
        dto.setRemark(remark);
        return dto;
    }

    private List<AochuangWechatAccountDTO> parseWechatAccounts(String body) {
        JSONArray items = extractArray(body);
        if (items == null) {
            return Collections.emptyList();
        }
        List<AochuangWechatAccountDTO> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            AochuangWechatAccountDTO dto = new AochuangWechatAccountDTO();
            dto.setWechatAccountId(firstNonBlank(item, "wechatAccountId", "id"));
            dto.setWechatId(firstNonBlank(item, "wechatId", "wxId"));
            dto.setAlias(item.getStr("alias"));
            dto.setNickname(item.getStr("nickname"));
            dto.setAvatar(item.getStr("avatar"));
            dto.setIsAlive(item.getBool("isAlive", true));
            result.add(dto);
        }
        return result;
    }

    private List<AochuangFriendDTO> parseFriendItems(String body) {
        JSONArray items = extractArray(body);
        if (items == null) {
            return Collections.emptyList();
        }
        List<AochuangFriendDTO> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            AochuangFriendDTO dto = new AochuangFriendDTO();
            dto.setFriendId(firstNonBlank(item, "wechatFriendId", "friendId", "id"));
            dto.setWechatId(firstNonBlank(item, "wechatId", "wxId"));
            dto.setAlias(item.getStr("alias"));
            dto.setNickname(item.getStr("nickname"));
            dto.setAvatar(item.getStr("avatar"));
            dto.setRemark(item.getStr("remark"));
            result.add(dto);
        }
        return result;
    }

    private List<AochuangMessageDTO> parseMessageItems(String body) {
        JSONArray items = extractArray(body);
        if (items == null) {
            return Collections.emptyList();
        }
        List<AochuangMessageDTO> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            AochuangMessageDTO dto = new AochuangMessageDTO();
            dto.setMessageId(firstNonBlank(item, "wechatMessageId", "messageId", "msgId", "id"));
            dto.setFriendId(firstNonBlank(item, "wechatFriendId", "friendId", "aochuangFriendId"));
            dto.setMsgType(firstNonBlank(item, "msgType", "messageType", "type"));
            dto.setContent(item.getStr("content"));
            String direction = firstNonBlank(item, "direction");
            if (StrUtil.isBlank(direction)) {
                Boolean isSend = item.getBool("isSend");
                if (isSend == null) {
                    isSend = item.getBool("fromMe");
                }
                direction = Boolean.TRUE.equals(isSend) ? "SENT" : "RECEIVED";
            }
            dto.setDirection(direction);
            applyMessageTime(dto, item);
            result.add(dto);
        }
        return result;
    }

    private void applyMessageTime(AochuangMessageDTO dto, JSONObject item) {
        Long ts = item.getLong("messageTime");
        if (ts == null) {
            ts = item.getLong("sendTime");
        }
        if (ts == null) {
            ts = item.getLong("timestamp");
        }
        if (ts != null) {
            if (ts > 1_000_000_000_000L) {
                dto.setMessageTime(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(ts), java.time.ZoneId.systemDefault()));
            } else {
                dto.setMessageTime(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(ts), java.time.ZoneId.systemDefault()));
            }
            return;
        }
        String timeStr = firstNonBlank(item, "messageTime", "sendTime", "time", "createTime");
        if (StrUtil.isNotBlank(timeStr)) {
            try {
                dto.setMessageTime(LocalDateTime.parse(timeStr.replace(" ", "T")));
            } catch (Exception ignored) {
                // leave null
            }
        }
    }

    private JSONArray extractArray(String body) {
        if (StrUtil.isBlank(body)) {
            return null;
        }
        Object parsed = JSONUtil.parse(body);
        if (parsed instanceof JSONArray array) {
            return array;
        }
        JSONObject root = (JSONObject) parsed;
        JSONArray items = root.getJSONArray("data");
        if (items == null) {
            items = root.getJSONArray("list");
        }
        if (items == null) {
            items = root.getJSONArray("records");
        }
        if (items == null && root.containsKey("wechatAccountId")) {
            items = JSONUtil.createArray().set(root);
        }
        return items;
    }

    private static String firstNonBlank(JSONObject item, String... keys) {
        for (String key : keys) {
            String val = item.getStr(key);
            if (StrUtil.isNotBlank(val)) {
                return val;
            }
        }
        return null;
    }

    private String normalizeBaseUrl(String apiUrl) {
        if (StrUtil.isBlank(apiUrl)) {
            throw new AochuangApiException("DISCONNECTED", "奥创接口地址未配置");
        }
        return StrUtil.removeSuffix(apiUrl.trim(), "/");
    }

    private String decrypt(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        try {
            return aesUtil.decrypt(encrypted);
        } catch (Exception ex) {
            log.warn("Failed to decrypt aochuang credential: {}", ex.getMessage());
            return null;
        }
    }
}
