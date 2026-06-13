package cn.iocoder.yudao.module.oa.service.match;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.match.MatchLeagueVO;
import cn.iocoder.yudao.module.oa.api.dto.match.MatchVO;
import cn.iocoder.yudao.module.oa.config.OaMatchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchProxyService {

    private static final DateTimeFormatter MATCH_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_PAGE_SIZE = 200;
    private static final long LEAGUE_CACHE_TTL_MS = 30 * 60 * 1000L;

    private final OaMatchProperties matchProperties;

    private final Map<String, String> competitionCache = new ConcurrentHashMap<>();
    private volatile long competitionCacheAt = 0L;

    public PageResult<MatchVO> listMatches(String date, Integer pageNo, Integer pageSize,
                                           String leagueId, String teamKeyword, String lotteryType) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, MAX_PAGE_SIZE);
        String queryDate = StrUtil.blankToDefault(date, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", safePageNo);
        params.put("pageSize", safePageSize);
        params.put("date", queryDate);
        if (StrUtil.isNotBlank(leagueId)) {
            params.put("sclassId", leagueId);
        }

        JSONObject payload = getExternalJson("/list", params);
        JSONObject data = payload.getJSONObject("data");
        JSONArray rawList = data != null ? data.getJSONArray("list") : null;
        if (rawList == null && payload.get("data") instanceof JSONArray array) {
            rawList = array;
        }
        long total = data != null
                ? data.getLong("total", rawList == null ? 0L : (long) rawList.size())
                : 0L;

        Map<String, String> leagueMap = loadCompetitionNameMap();
        List<MatchVO> matches = new ArrayList<>();
        if (rawList != null) {
            for (Object item : rawList) {
                if (!(item instanceof JSONObject matchJson)) {
                    continue;
                }
                MatchVO vo = toMatchVO(matchJson, leagueMap);
                if (StrUtil.isNotBlank(teamKeyword)) {
                    String kw = teamKeyword.toLowerCase();
                    if (!containsTeamKeyword(vo, kw)) {
                        continue;
                    }
                }
                if (StrUtil.isNotBlank(lotteryType) && !lotteryType.equals(vo.getLotteryType())) {
                    continue;
                }
                matches.add(vo);
            }
        }

        if (StrUtil.isNotBlank(teamKeyword) || StrUtil.isNotBlank(lotteryType)) {
            total = matches.size();
        }
        return new PageResult<>(matches, total);
    }

    public List<MatchLeagueVO> listLeagues() {
        JSONObject payload = getExternalJson("/filter/competitions/flat", Map.of());
        Object data = payload.get("data");
        JSONArray competitions = data instanceof JSONArray ? (JSONArray) data : new JSONArray();
        List<MatchLeagueVO> list = new ArrayList<>();
        for (Object item : competitions) {
            if (!(item instanceof JSONObject comp)) {
                continue;
            }
            MatchLeagueVO vo = new MatchLeagueVO();
            vo.setId(String.valueOf(comp.get("sclassId")));
            vo.setName(comp.getStr("nameZh"));
            vo.setNameEn(comp.getStr("nameEn"));
            vo.setShortName(comp.getStr("shortName"));
            list.add(vo);
        }
        return list;
    }

    private MatchVO toMatchVO(JSONObject match, Map<String, String> leagueMap) {
        String sclassId = match.getStr("sclassId");
        String sclassName = leagueMap.getOrDefault(sclassId, match.getStr("sClassName"));
        String matchTime = formatTimestamp(match.get("matchTime"));
        String home = match.getStr("homeTeamName", "");
        String guest = match.getStr("guestTeamName", "");
        String displayName = sclassName + "-" + home + " VS " + guest + "-" + matchTime;

        MatchVO vo = new MatchVO();
        vo.setScheduleId(String.valueOf(match.get("scheduleId")));
        vo.setDisplayName(displayName);
        vo.setSClassId(sclassId);
        vo.setSClassName(sclassName);
        vo.setHomeTeamName(home);
        vo.setGuestTeamName(guest);
        vo.setMatchTime(matchTime);
        vo.setMatchTimeRaw(match.getLong("matchTime"));
        vo.setLotteryType(determineLotteryType(match));
        return vo;
    }

    private boolean containsTeamKeyword(MatchVO vo, String keyword) {
        return StrUtil.containsIgnoreCase(vo.getHomeTeamName(), keyword)
                || StrUtil.containsIgnoreCase(vo.getGuestTeamName(), keyword);
    }

    private String determineLotteryType(JSONObject match) {
        String type = match.getStr("lotteryType");
        return StrUtil.blankToDefault(type, "jc");
    }

    private String formatTimestamp(Object timestampMs) {
        if (timestampMs == null) {
            return "";
        }
        try {
            long ms = Long.parseLong(String.valueOf(timestampMs));
            if (ms <= 0) {
                return "";
            }
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
            return dt.format(MATCH_TIME_FMT);
        } catch (Exception ex) {
            return "";
        }
    }

    private Map<String, String> loadCompetitionNameMap() {
        long now = System.currentTimeMillis();
        if (!competitionCache.isEmpty() && now - competitionCacheAt < LEAGUE_CACHE_TTL_MS) {
            return competitionCache;
        }
        synchronized (this) {
            if (!competitionCache.isEmpty() && now - competitionCacheAt < LEAGUE_CACHE_TTL_MS) {
                return competitionCache;
            }
            try {
                JSONObject payload = getExternalJson("/filter/competitions/flat", Map.of());
                Object data = payload.get("data");
                JSONArray competitions = data instanceof JSONArray ? (JSONArray) data : new JSONArray();
                competitionCache.clear();
                for (Object item : competitions) {
                    if (item instanceof JSONObject comp) {
                        competitionCache.put(String.valueOf(comp.get("sclassId")), comp.getStr("nameZh"));
                    }
                }
                competitionCacheAt = System.currentTimeMillis();
            } catch (Exception ex) {
                log.warn("加载联赛名称映射失败: {}", ex.getMessage());
            }
            return competitionCache;
        }
    }

    private JSONObject getExternalJson(String path, Map<String, Object> params) {
        String baseUrl = StrUtil.removeSuffix(matchProperties.getApiBaseUrl(), "/");
        String url = baseUrl + path;
        HttpRequest request = HttpRequest.get(url).timeout(10_000);
        if (matchProperties.getHeaders() != null) {
            matchProperties.getHeaders().forEach(request::header);
        }
        Long tenantId = TenantContextHolder.getTenantId();
        request.header("tenant-id", tenantId != null ? String.valueOf(tenantId) : "1");
        if (params != null && !params.isEmpty()) {
            params.forEach((key, value) -> {
                if (value != null && StrUtil.isNotBlank(String.valueOf(value))) {
                    request.form(key, value);
                }
            });
        }
        HttpResponse response = request.execute();
        if (!response.isOk()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "赛事 API 请求失败：" + response.getStatus());
        }
        JSONObject body = JSONUtil.parseObj(response.body());
        if (body.getInt("code", -1) != 0) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    StrUtil.blankToDefault(body.getStr("msg"), "获取赛事数据失败"));
        }
        return body;
    }
}
