package cn.iocoder.yudao.module.oa.service.aicontent;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentAdoptReq;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentAdoptRespVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentContextDTO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentConversationMessageDTO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentConversationSaveReq;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentConversationVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentGenerateReq;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentGenerateRespVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentModelVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentModelsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentPreferenceDimensionVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentPreferenceGenerateReq;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentPreferenceSummaryVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentPreferenceUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentPreferenceUpdateRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.aicontent.AiContentAdoptDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.aicontent.AiContentConversationDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.aicontent.AiContentPreferenceDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.aicontent.AiContentSessionDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AiModelConfigDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AiPromptConfigDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.mysql.aicontent.AiContentAdoptMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.aicontent.AiContentConversationMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.aicontent.AiContentPreferenceMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.aicontent.AiContentSessionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AiModelConfigMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AiPromptConfigMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.content.AiLlmInvokeSupport;
import cn.iocoder.yudao.module.oa.service.content.AiLlmInvokeSupport.AiChatMessage;
import cn.iocoder.yudao.module.oa.service.content.SchemeTypeHelper;
import cn.iocoder.yudao.module.oa.service.dict.DictService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiContentServiceImpl implements AiContentService {

    private static final String SCENE_AI_CONTENT_CHAT = "AI_CONTENT_CHAT";
    private static final int MAX_ROUND_COUNT = 10;
    private static final String DIM_LANGUAGE_STYLE = "language_style";
    private static final String DIM_PREDICTION_DIRECTION = "prediction_direction";
    private static final String DIM_LENGTH_PREFERENCE = "length_preference";
    private static final String DIM_MODIFICATION_TENDENCY = "modification_tendency";
    private static final String DIM_UNSET = "未设定";

    private static final Map<String, String> MODEL_TYPE_ICONS = Map.of(
            "QWEN", "🔵",
            "DEEPSEEK", "🟢",
            "GLM", "🟣",
            "MOONSHOT", "🟠",
            "KIMI", "🟠"
    );

    private final AiModelConfigMapper aiModelConfigMapper;
    private final AiPromptConfigMapper aiPromptConfigMapper;
    private final AiContentSessionMapper aiContentSessionMapper;
    private final AiContentAdoptMapper aiContentAdoptMapper;
    private final AiContentPreferenceMapper aiContentPreferenceMapper;
    private final AiContentConversationMapper aiContentConversationMapper;
    private final AiLlmInvokeSupport aiLlmInvokeSupport;
    private final DictService dictService;

    @Override
    @Transactional
    @AuditLog(module = "M2-ai-content", action = "generate")
    public AiContentGenerateRespVO generate(AiContentGenerateReq req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        AiModelConfigDO model = resolveModelConfig(tenantId, req.getModelId());
        int roundCount = req.getRoundCount() == null ? 1 : req.getRoundCount();
        if (roundCount > MAX_ROUND_COUNT) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "对话轮次已达上限（" + MAX_ROUND_COUNT + "/" + MAX_ROUND_COUNT + "），请采纳当前方案或重新开始");
        }

        AiPromptConfigDO prompt = resolveChatPrompt(tenantId);
        String systemPrompt = buildSystemPrompt(prompt.getPromptContent(), req.getContext(),
                StrUtil.blankToDefault(req.getPreferenceSummary(), ""));

        List<AiChatMessage> messages = new ArrayList<>();
        messages.add(AiChatMessage.system(systemPrompt));
        if (req.getConversationHistory() != null) {
            for (AiContentConversationMessageDTO item : req.getConversationHistory()) {
                if (StrUtil.isBlank(item.getRole()) || StrUtil.isBlank(item.getContent())) {
                    continue;
                }
                String role = item.getRole().toLowerCase(Locale.ROOT);
                if ("assistant".equals(role)) {
                    messages.add(AiChatMessage.assistant(item.getContent()));
                } else {
                    messages.add(AiChatMessage.user(item.getContent()));
                }
            }
        }
        messages.add(AiChatMessage.user(req.getMessage()));

        String content = aiLlmInvokeSupport.chatCompletion(model, messages);
        boolean mock = !"CONNECTED".equals(model.getConnStatus()) || StrUtil.isBlank(model.getApiEndpoint());

        upsertSession(tenantId, userId, req.getSessionId(), model.getId(), roundCount, content, req.getContext());

        AiContentGenerateRespVO resp = new AiContentGenerateRespVO();
        resp.setSessionId(req.getSessionId());
        resp.setContent(content);
        resp.setModelId(model.getId());
        resp.setRoundCount(roundCount);
        resp.setTokensUsed(estimateTokens(content));
        resp.setGeneratedAt(LocalDateTime.now());
        resp.setMock(mock);
        resp.setMessage(mock ? "AI 占位生成（模型未连通）" : "AI 生成完成");
        return resp;
    }

    @Override
    public AiContentModelsRespVO listModels() {
        Long tenantId = requireTenantId();
        List<AiModelConfigDO> allModels = aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfigDO>()
                .eq(AiModelConfigDO::getTenantId, tenantId)
                .eq(AiModelConfigDO::getStatus, "ENABLED")
                .orderByDesc(AiModelConfigDO::getIsDefault)
                .orderByAsc(AiModelConfigDO::getId));

        List<AiContentModelVO> models = new ArrayList<>();
        for (AiModelConfigDO item : allModels) {
            AiContentModelVO vo = new AiContentModelVO();
            vo.setId(item.getId());
            vo.setName(item.getModelName());
            vo.setIcon(resolveModelIcon(item.getModelType()));
            vo.setDescription(StrUtil.blankToDefault(item.getRemark(), item.getModelType()));
            vo.setStatus("CONNECTED".equals(item.getConnStatus()) ? "available" : "unavailable");
            vo.setIsDefault(Objects.equals(item.getIsDefault(), 1));
            models.add(vo);
        }

        AiContentModelsRespVO resp = new AiContentModelsRespVO();
        resp.setModels(models);
        return resp;
    }

    @Override
    public AiContentPreferenceSummaryVO getPreferenceSummary(Long authorId) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        AiContentPreferenceDO pref = aiContentPreferenceMapper.selectOne(
                new LambdaQueryWrapper<AiContentPreferenceDO>()
                        .eq(AiContentPreferenceDO::getTenantId, tenantId)
                        .eq(AiContentPreferenceDO::getUserId, userId)
                        .eq(authorId != null, AiContentPreferenceDO::getAuthorId, authorId)
                        .isNull(authorId == null, AiContentPreferenceDO::getAuthorId)
                        .last("LIMIT 1"));
        if (pref == null || StrUtil.isBlank(pref.getSummaryText())) {
            return null;
        }
        return toPreferenceSummaryVO(pref);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-ai-content", action = "preference-generate")
    public AiContentPreferenceSummaryVO generatePreferenceFromConversation(AiContentPreferenceGenerateReq req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        if (req.getConversationHistory() == null || req.getConversationHistory().isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "对话记录为空，无法生成偏好总结");
        }

        Map<String, AiContentPreferenceDimensionVO> dimensions = extractPreferenceDimensions(req);
        String summaryText = buildPreferenceSummaryText(dimensions);
        if (StrUtil.isBlank(summaryText)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "未能从对话中提取有效偏好");
        }

        AiContentPreferenceDO existing = findPreferenceRecord(tenantId, userId, req.getAuthorId());
        if (existing == null) {
            existing = new AiContentPreferenceDO();
            existing.setTenantId(tenantId);
            existing.setUserId(userId);
            existing.setAuthorId(req.getAuthorId());
            existing.setCreator(TenantContextHolder.getUsername());
            existing.setCreateTime(LocalDateTime.now());
        }
        existing.setSummaryText(summaryText);
        existing.setDimensionsJson(JSONUtil.toJsonStr(dimensions));
        existing.setSourceSessionId(req.getSessionId());
        if (req.getContentId() != null) {
            existing.setContentId(req.getContentId());
        }
        existing.setIsUpdatedByUser(0);
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        if (existing.getId() == null) {
            aiContentPreferenceMapper.insert(existing);
        } else {
            aiContentPreferenceMapper.updateById(existing);
        }
        return toPreferenceSummaryVO(existing);
    }

    @Override
    public AiContentConversationVO getConversationHistory(Long contentId, Long authorId) {
        // 对话历史仅按 contentId 加载；新建内容无 contentId 时不回落 author/global
        if (contentId == null) {
            return null;
        }
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        AiContentConversationDO record = findConversationRecord(tenantId, userId, contentId, authorId);
        if (record == null || StrUtil.isBlank(record.getConversationJson())) {
            return null;
        }
        return toConversationVO(record);
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-ai-content", action = "conversation-save")
    public AiContentConversationVO saveConversationHistory(AiContentConversationSaveReq req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        if (req.getConversationHistory() == null || req.getConversationHistory().isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "对话记录为空，无法保存");
        }
        if (req.getContentId() == null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "内容未保存，无法持久化对话记录");
        }

        List<AiContentConversationMessageDTO> trimmed = trimConversationToMaxRounds(req.getConversationHistory());
        int roundCount = countAssistantRounds(trimmed);
        String scopeKey = buildConversationScopeKey(req.getContentId(), req.getAuthorId());

        AiContentConversationDO existing = findConversationRecord(tenantId, userId, req.getContentId(), req.getAuthorId());
        if (existing == null) {
            existing = new AiContentConversationDO();
            existing.setTenantId(tenantId);
            existing.setUserId(userId);
            existing.setScopeKey(scopeKey);
            existing.setContentId(req.getContentId());
            existing.setAuthorId(req.getAuthorId());
            existing.setCreator(TenantContextHolder.getUsername());
            existing.setCreateTime(LocalDateTime.now());
        }
        existing.setConversationJson(JSONUtil.toJsonStr(trimmed));
        existing.setRoundCount(roundCount);
        existing.setSourceSessionId(req.getSessionId());
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        if (existing.getId() == null) {
            aiContentConversationMapper.insert(existing);
        } else {
            aiContentConversationMapper.updateById(existing);
        }
        return toConversationVO(existing);
    }

    private AiContentConversationVO toConversationVO(AiContentConversationDO record) {
        AiContentConversationVO vo = new AiContentConversationVO();
        if (StrUtil.isNotBlank(record.getConversationJson())) {
            List<AiContentConversationMessageDTO> messages = JSONUtil.toList(
                    record.getConversationJson(), AiContentConversationMessageDTO.class);
            vo.setConversationHistory(trimConversationToMaxRounds(messages));
        }
        vo.setRoundCount(record.getRoundCount());
        vo.setSourceSessionId(record.getSourceSessionId());
        vo.setSavedAt(record.getUpdateTime());
        return vo;
    }

    private AiContentConversationDO findConversationRecord(Long tenantId, Long userId, Long contentId, Long authorId) {
        if (contentId == null) {
            return null;
        }
        String scopeKey = buildConversationScopeKey(contentId, authorId);
        return aiContentConversationMapper.selectOne(
                new LambdaQueryWrapper<AiContentConversationDO>()
                        .eq(AiContentConversationDO::getTenantId, tenantId)
                        .eq(AiContentConversationDO::getUserId, userId)
                        .eq(AiContentConversationDO::getScopeKey, scopeKey)
                        .last("LIMIT 1"));
    }

    private String buildConversationScopeKey(Long contentId, Long authorId) {
        if (contentId != null) {
            return "content:" + contentId;
        }
        // 对话历史仅绑定 contentId，不再使用 author/global 作用域
        return null;
    }

    private List<AiContentConversationMessageDTO> trimConversationToMaxRounds(
            List<AiContentConversationMessageDTO> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<AiContentConversationMessageDTO> valid = history.stream()
                .filter(item -> StrUtil.isNotBlank(item.getRole()) && StrUtil.isNotBlank(item.getContent()))
                .map(item -> {
                    AiContentConversationMessageDTO msg = new AiContentConversationMessageDTO();
                    msg.setRole(item.getRole().toLowerCase(Locale.ROOT));
                    msg.setContent(item.getContent().trim());
                    return msg;
                })
                .collect(Collectors.toList());
        int assistantCount = 0;
        for (AiContentConversationMessageDTO item : valid) {
            if ("assistant".equals(item.getRole())) {
                assistantCount++;
            }
        }
        if (assistantCount <= MAX_ROUND_COUNT) {
            return valid;
        }
        int toDrop = assistantCount - MAX_ROUND_COUNT;
        int dropped = 0;
        int cutIndex = 0;
        for (int i = 0; i < valid.size(); i++) {
            if ("assistant".equals(valid.get(i).getRole())) {
                dropped++;
                if (dropped == toDrop) {
                    cutIndex = i + 1;
                    break;
                }
            }
        }
        while (cutIndex < valid.size() && !"user".equals(valid.get(cutIndex).getRole())) {
            cutIndex++;
        }
        return cutIndex >= valid.size() ? List.of() : new ArrayList<>(valid.subList(cutIndex, valid.size()));
    }

    private int countAssistantRounds(List<AiContentConversationMessageDTO> history) {
        if (history == null) {
            return 0;
        }
        int rounds = 0;
        for (AiContentConversationMessageDTO item : history) {
            if ("assistant".equalsIgnoreCase(item.getRole())) {
                rounds++;
            }
        }
        return Math.min(rounds, MAX_ROUND_COUNT);
    }

    private AiContentPreferenceSummaryVO toPreferenceSummaryVO(AiContentPreferenceDO pref) {
        AiContentPreferenceSummaryVO vo = new AiContentPreferenceSummaryVO();
        vo.setSummaryText(pref.getSummaryText());
        if (StrUtil.isNotBlank(pref.getDimensionsJson())) {
            vo.setDimensions(parseDimensionsJson(pref.getDimensionsJson()));
        }
        vo.setSourceSessionId(pref.getSourceSessionId());
        vo.setGeneratedAt(pref.getUpdateTime());
        vo.setIsUpdatedByUser(pref.getIsUpdatedByUser() != null && pref.getIsUpdatedByUser() == 1);
        return vo;
    }

    private Map<String, AiContentPreferenceDimensionVO> parseDimensionsJson(String dimensionsJson) {
        Map<String, AiContentPreferenceDimensionVO> result = new LinkedHashMap<>();
        JSONObject root = JSONUtil.parseObj(dimensionsJson);
        for (String key : root.keySet()) {
            JSONObject item = root.getJSONObject(key);
            if (item == null) {
                continue;
            }
            AiContentPreferenceDimensionVO dim = new AiContentPreferenceDimensionVO();
            dim.setValue(item.getStr("value"));
            dim.setConfidence(item.getDouble("confidence"));
            dim.setSourceRound(item.getInt("source_round"));
            result.put(key, dim);
        }
        return result;
    }

    private Map<String, AiContentPreferenceDimensionVO> extractPreferenceDimensions(AiContentPreferenceGenerateReq req) {
        Map<String, AiContentPreferenceDimensionVO> llmResult = tryLlmExtractPreference(req);
        if (llmResult != null && !llmResult.isEmpty()) {
            return llmResult;
        }
        return extractPreferenceByRules(req);
    }

    private Map<String, AiContentPreferenceDimensionVO> tryLlmExtractPreference(AiContentPreferenceGenerateReq req) {
        try {
            AiModelConfigDO model = resolveDefaultModel(requireTenantId());
            String prompt = buildPreferenceExtractPrompt(req);
            String raw = aiLlmInvokeSupport.singleUserPrompt(model, prompt);
            return parseLlmPreferenceResponse(raw);
        } catch (Exception ex) {
            log.warn("LLM preference extraction failed, fallback to rules: {}", ex.getMessage());
            return null;
        }
    }

    private String buildPreferenceExtractPrompt(AiContentPreferenceGenerateReq req) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下 AI 内容生成对话记录，提取用户偏好并输出 JSON（仅输出 JSON，不要其他文字）。\n");
        sb.append("四个维度字段名：language_style、prediction_direction、length_preference、modification_tendency。\n");
        sb.append("每个维度对象包含 value（string）、confidence（0~1 浮点）、source_round（int，首次出现的对话轮次）。\n");
        sb.append("未体现的维度 value 填「未设定」。另附 summary_text 字段为完整偏好总结句。\n\n");
        sb.append("【上下文】\n");
        sb.append("赛事：").append(StrUtil.blankToDefault(req.getContext().getMatchName(), "")).append('\n');
        sb.append("作者：").append(StrUtil.blankToDefault(req.getContext().getAuthorName(), "")).append('\n');
        sb.append("方案类型：").append(resolveSchemeTypeLabels(req.getContext().getSchemeTypes())).append('\n');
        sb.append("主播风格：").append(resolveAnchorStyleLabel(req.getContext().getAnchorStyle())).append('\n');
        if (StrUtil.isNotBlank(req.getPreferenceSummary())) {
            sb.append("用户已编辑偏好：").append(req.getPreferenceSummary()).append('\n');
        }
        sb.append("\n【对话记录】\n");
        int round = 0;
        for (AiContentConversationMessageDTO msg : req.getConversationHistory()) {
            if ("user".equalsIgnoreCase(msg.getRole())) {
                round++;
            }
            sb.append('[').append(msg.getRole()).append("] ").append(msg.getContent()).append('\n');
        }
        sb.append("\n示例 summary_text：上次对话中，您偏好简洁直接的语言风格，侧重胜平负分析方向，期望篇幅中等（500-800字），修改时倾向于增加数据对比和风险提示。");
        return sb.toString();
    }

    private Map<String, AiContentPreferenceDimensionVO> parseLlmPreferenceResponse(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String json = raw.trim();
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        json = json.substring(start, end + 1);
        JSONObject root = JSONUtil.parseObj(json);
        Map<String, AiContentPreferenceDimensionVO> dimensions = new LinkedHashMap<>();
        for (String key : List.of(DIM_LANGUAGE_STYLE, DIM_PREDICTION_DIRECTION, DIM_LENGTH_PREFERENCE, DIM_MODIFICATION_TENDENCY)) {
            JSONObject item = root.getJSONObject(key);
            if (item == null) {
                continue;
            }
            AiContentPreferenceDimensionVO dim = new AiContentPreferenceDimensionVO();
            dim.setValue(StrUtil.blankToDefault(item.getStr("value"), DIM_UNSET));
            dim.setConfidence(item.getDouble("confidence"));
            dim.setSourceRound(item.getInt("source_round"));
            dimensions.put(key, dim);
        }
        if (dimensions.isEmpty()) {
            return null;
        }
        for (String key : List.of(DIM_LANGUAGE_STYLE, DIM_PREDICTION_DIRECTION, DIM_LENGTH_PREFERENCE, DIM_MODIFICATION_TENDENCY)) {
            dimensions.putIfAbsent(key, unsetDimension());
        }
        return dimensions;
    }

    private Map<String, AiContentPreferenceDimensionVO> extractPreferenceByRules(AiContentPreferenceGenerateReq req) {
        AiContentContextDTO context = req.getContext();
        List<String> userMessages = collectUserMessages(req.getConversationHistory());
        String allUserText = String.join(" ", userMessages);

        Map<String, AiContentPreferenceDimensionVO> dimensions = new LinkedHashMap<>();
        dimensions.put(DIM_LANGUAGE_STYLE, extractLanguageStyle(context, userMessages, allUserText));
        dimensions.put(DIM_PREDICTION_DIRECTION, extractPredictionDirection(context, allUserText));
        dimensions.put(DIM_LENGTH_PREFERENCE, extractLengthPreference(allUserText));
        dimensions.put(DIM_MODIFICATION_TENDENCY, extractModificationTendency(userMessages));
        return dimensions;
    }

    private AiContentPreferenceDimensionVO extractLanguageStyle(AiContentContextDTO context,
                                                              List<String> userMessages, String allUserText) {
        String anchorLabel = resolveAnchorStyleLabel(context.getAnchorStyle());
        String value = DIM_UNSET;
        int sourceRound = 1;
        if (StrUtil.isNotBlank(anchorLabel)) {
            value = mapAnchorStyleToLanguage(anchorLabel);
        }
        if (containsAny(allUserText, "简洁", "直接", "精炼", "简短")) {
            value = "简洁直接";
            sourceRound = findKeywordRound(userMessages, "简洁", "直接", "精炼", "简短");
        } else if (containsAny(allUserText, "专业", "严谨", "深度")) {
            value = "专业严谨";
            sourceRound = findKeywordRound(userMessages, "专业", "严谨", "深度");
        } else if (containsAny(allUserText, "通俗", "易懂", "接地气")) {
            value = "通俗易懂";
            sourceRound = findKeywordRound(userMessages, "通俗", "易懂", "接地气");
        } else if (containsAny(allUserText, "幽默", "风趣", "有趣")) {
            value = "幽默风趣";
            sourceRound = findKeywordRound(userMessages, "幽默", "风趣", "有趣");
        }
        return dimension(value, value.equals(DIM_UNSET) ? 0.3 : 0.75, sourceRound);
    }

    private AiContentPreferenceDimensionVO extractPredictionDirection(AiContentContextDTO context, String allUserText) {
        String schemeLabel = resolveSchemeTypeLabels(context.getSchemeTypes());
        String value = DIM_UNSET;
        int sourceRound = 1;
        if (StrUtil.isNotBlank(schemeLabel)) {
            value = "偏好" + schemeLabel;
        }
        if (containsAny(allUserText, "胜平负")) {
            value = "偏好胜平负分析";
            sourceRound = 1;
        } else if (containsAny(allUserText, "让球", "盘口")) {
            value = "倾向让球盘口";
        } else if (containsAny(allUserText, "大小球", "大球", "小球")) {
            value = "关注大小球";
        } else if (containsAny(allUserText, "半全场", "半场")) {
            value = "注重半全场";
        }
        return dimension(value, value.equals(DIM_UNSET) ? 0.3 : 0.8, sourceRound);
    }

    private AiContentPreferenceDimensionVO extractLengthPreference(String allUserText) {
        String value = DIM_UNSET;
        if (containsAny(allUserText, "300", "简短", "精炼", "精简", "字数少")) {
            value = "简短精炼（300字内）";
        } else if (containsAny(allUserText, "500", "800", "中等", "适中")) {
            value = "中等篇幅（500-800字）";
        } else if (containsAny(allUserText, "1000", "详细", "深入", "长一点", "多一些")) {
            value = "详细深入（1000字以上）";
        }
        return dimension(value, value.equals(DIM_UNSET) ? 0.3 : 0.7, 1);
    }

    private AiContentPreferenceDimensionVO extractModificationTendency(List<String> userMessages) {
        if (userMessages.size() <= 1) {
            return unsetDimension();
        }
        List<String> modifications = userMessages.subList(1, userMessages.size());
        String modText = String.join(" ", modifications);
        List<String> tendencies = new ArrayList<>();
        if (containsAny(modText, "数据", "对比", "统计")) {
            tendencies.add("喜欢增加数据对比");
        }
        if (containsAny(modText, "激进", "大胆", "冒险")) {
            tendencies.add("要求更激进的角度");
        }
        if (containsAny(modText, "保守", "稳妥", "稳健")) {
            tendencies.add("偏好保守稳妥方案");
        }
        if (containsAny(modText, "风险", "提示", "警示")) {
            tendencies.add("强调风险提示");
        }
        if (containsAny(modText, "简洁", "缩短", "精简")) {
            tendencies.add("希望更简洁");
        }
        String value = tendencies.isEmpty() ? DIM_UNSET : String.join("，", tendencies);
        return dimension(value, tendencies.isEmpty() ? 0.3 : 0.85, Math.min(2, userMessages.size()));
    }

    private String buildPreferenceSummaryText(Map<String, AiContentPreferenceDimensionVO> dimensions) {
        String lang = dimValue(dimensions, DIM_LANGUAGE_STYLE);
        String pred = dimValue(dimensions, DIM_PREDICTION_DIRECTION);
        String len = dimValue(dimensions, DIM_LENGTH_PREFERENCE);
        String mod = dimValue(dimensions, DIM_MODIFICATION_TENDENCY);
        long unsetCount = List.of(lang, pred, len, mod).stream().filter(DIM_UNSET::equals).count();
        if (unsetCount == 4) {
            return "";
        }
        return String.format("上次对话中，您偏好%s的语言风格，侧重%s方向，期望篇幅%s，修改时倾向于%s。",
                lang, pred, len, mod);
    }

    private String dimValue(Map<String, AiContentPreferenceDimensionVO> dimensions, String key) {
        AiContentPreferenceDimensionVO dim = dimensions.get(key);
        if (dim == null || StrUtil.isBlank(dim.getValue())) {
            return DIM_UNSET;
        }
        return dim.getValue();
    }

    private AiContentPreferenceDimensionVO dimension(String value, double confidence, int sourceRound) {
        AiContentPreferenceDimensionVO dim = new AiContentPreferenceDimensionVO();
        dim.setValue(StrUtil.blankToDefault(value, DIM_UNSET));
        dim.setConfidence(confidence);
        dim.setSourceRound(sourceRound);
        return dim;
    }

    private AiContentPreferenceDimensionVO unsetDimension() {
        return dimension(DIM_UNSET, 0.0, 0);
    }

    private List<String> collectUserMessages(List<AiContentConversationMessageDTO> history) {
        List<String> messages = new ArrayList<>();
        if (history == null) {
            return messages;
        }
        for (AiContentConversationMessageDTO item : history) {
            if ("user".equalsIgnoreCase(item.getRole()) && StrUtil.isNotBlank(item.getContent())) {
                messages.add(item.getContent().trim());
            }
        }
        return messages;
    }

    private int findKeywordRound(List<String> userMessages, String... keywords) {
        for (int i = 0; i < userMessages.size(); i++) {
            if (containsAny(userMessages.get(i), keywords)) {
                return i + 1;
            }
        }
        return 1;
    }

    private boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String mapAnchorStyleToLanguage(String anchorLabel) {
        if (anchorLabel.contains("激进")) {
            return "简洁直接";
        }
        if (anchorLabel.contains("稳健") || anchorLabel.contains("保守")) {
            return "专业严谨";
        }
        if (anchorLabel.contains("数据")) {
            return "专业严谨";
        }
        if (anchorLabel.contains("情感")) {
            return "通俗易懂";
        }
        return "综合分析";
    }

    private AiContentPreferenceDO findPreferenceRecord(Long tenantId, Long userId, Long authorId) {
        return aiContentPreferenceMapper.selectOne(
                new LambdaQueryWrapper<AiContentPreferenceDO>()
                        .eq(AiContentPreferenceDO::getTenantId, tenantId)
                        .eq(AiContentPreferenceDO::getUserId, userId)
                        .eq(authorId != null, AiContentPreferenceDO::getAuthorId, authorId)
                        .isNull(authorId == null, AiContentPreferenceDO::getAuthorId)
                        .last("LIMIT 1"));
    }

    private AiModelConfigDO resolveDefaultModel(Long tenantId) {
        AiModelConfigDO model = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigDO>()
                .eq(AiModelConfigDO::getTenantId, tenantId)
                .eq(AiModelConfigDO::getStatus, "ENABLED")
                .orderByDesc(AiModelConfigDO::getIsDefault)
                .orderByAsc(AiModelConfigDO::getId)
                .last("LIMIT 1"));
        if (model == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "未配置可用 AI 模型");
        }
        return model;
    }

    @Override
    @Transactional
    public AiContentPreferenceUpdateRespVO updatePreferenceSummary(AiContentPreferenceUpdateReq req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        if (StrUtil.isBlank(req.getSummaryText())) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "偏好总结不能为空");
        }
        AiContentPreferenceDO existing = findPreferenceRecord(tenantId, userId, null);
        if (existing == null) {
            existing = new AiContentPreferenceDO();
            existing.setTenantId(tenantId);
            existing.setUserId(userId);
            existing.setCreator(TenantContextHolder.getUsername());
            existing.setCreateTime(LocalDateTime.now());
        }
        existing.setSummaryText(req.getSummaryText().trim());
        if (req.getDimensions() != null) {
            existing.setDimensionsJson(JSONUtil.toJsonStr(req.getDimensions()));
        }
        existing.setIsUpdatedByUser(1);
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        if (existing.getId() == null) {
            aiContentPreferenceMapper.insert(existing);
        } else {
            aiContentPreferenceMapper.updateById(existing);
        }
        AiContentPreferenceUpdateRespVO resp = new AiContentPreferenceUpdateRespVO();
        resp.setUpdatedAt(existing.getUpdateTime());
        return resp;
    }

    @Override
    @Transactional
    @AuditLog(module = "M2-ai-content", action = "adopt")
    public AiContentAdoptRespVO adopt(AiContentAdoptReq req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();
        AiContentAdoptDO record = new AiContentAdoptDO();
        record.setTenantId(tenantId);
        record.setSessionId(req.getSessionId());
        record.setUserId(userId);
        record.setContentId(req.getContentId());
        record.setModelKey(String.valueOf(req.getModelId()));
        record.setSchemeType(SchemeTypeHelper.toStored(req.getSchemeTypes()));
        record.setContentLength(StrUtil.length(req.getContent()));
        record.setCreator(TenantContextHolder.getUsername());
        record.setUpdater(TenantContextHolder.getUsername());
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        aiContentAdoptMapper.insert(record);

        AiContentAdoptRespVO resp = new AiContentAdoptRespVO();
        resp.setSessionId(req.getSessionId());
        resp.setContentId(req.getContentId());
        resp.setAdoptedAt(record.getCreateTime());
        return resp;
    }

    private void upsertSession(Long tenantId, Long userId, String sessionId, Long modelId,
                               int roundCount, String content, AiContentContextDTO context) {
        AiContentSessionDO existing = aiContentSessionMapper.selectOne(
                new LambdaQueryWrapper<AiContentSessionDO>()
                        .eq(AiContentSessionDO::getTenantId, tenantId)
                        .eq(AiContentSessionDO::getSessionId, sessionId)
                        .last("LIMIT 1"));
        if (existing == null) {
            existing = new AiContentSessionDO();
            existing.setTenantId(tenantId);
            existing.setSessionId(sessionId);
            existing.setUserId(userId);
            existing.setCreator(TenantContextHolder.getUsername());
            existing.setCreateTime(LocalDateTime.now());
        }
        existing.setModelKey(String.valueOf(modelId));
        existing.setRoundCount(roundCount);
        existing.setLastContent(content);
        existing.setContextJson(JSONUtil.toJsonStr(context));
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        if (existing.getId() == null) {
            aiContentSessionMapper.insert(existing);
        } else {
            aiContentSessionMapper.updateById(existing);
        }
    }

    private AiModelConfigDO resolveModelConfig(Long tenantId, Long modelId) {
        if (modelId == null) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "模型 ID 不能为空");
        }
        AiModelConfigDO model = aiModelConfigMapper.selectById(modelId);
        if (model == null || !Objects.equals(model.getTenantId(), tenantId)) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (!"ENABLED".equals(model.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_DISABLED);
        }
        return model;
    }

    private String resolveModelIcon(String modelType) {
        if (StrUtil.isBlank(modelType)) {
            return "🤖";
        }
        return MODEL_TYPE_ICONS.getOrDefault(modelType.toUpperCase(Locale.ROOT), "🤖");
    }

    private AiPromptConfigDO resolveChatPrompt(Long tenantId) {
        AiPromptConfigDO prompt = aiPromptConfigMapper.selectOne(new LambdaQueryWrapper<AiPromptConfigDO>()
                .eq(AiPromptConfigDO::getTenantId, tenantId)
                .eq(AiPromptConfigDO::getScene, SCENE_AI_CONTENT_CHAT)
                .eq(AiPromptConfigDO::getStatus, "ENABLED")
                .orderByDesc(AiPromptConfigDO::getId)
                .last("LIMIT 1"));
        if (prompt == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "未配置 AI 内容对话提示词");
        }
        return prompt;
    }

    private String buildSystemPrompt(String template, AiContentContextDTO context, String preferenceSummary) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("match_name", StrUtil.blankToDefault(context.getMatchName(), ""));
        vars.put("author_name", StrUtil.blankToDefault(context.getAuthorName(), ""));
        vars.put("scheme_type", resolveSchemeTypeLabels(context.getSchemeTypes()));
        vars.put("history_record", StrUtil.blankToDefault(context.getHistoryRecord(), ""));
        vars.put("anchor_style", resolveAnchorStyleLabel(context.getAnchorStyle()));
        vars.put("product_description", StrUtil.blankToDefault(context.getProductDescription(), ""));
        vars.put("preference_summary", StrUtil.blankToDefault(preferenceSummary, ""));
        return fillPromptPlaceholders(template, vars);
    }

    private String resolveSchemeTypeLabels(List<String> schemeTypes) {
        if (schemeTypes == null || schemeTypes.isEmpty()) {
            return "";
        }
        Map<String, String> labelByValue = dictService.listByType("dict_scheme_type").stream()
                .collect(Collectors.toMap(SysDictDataDO::getDictValue, SysDictDataDO::getLabel, (a, b) -> a));
        return schemeTypes.stream()
                .filter(StrUtil::isNotBlank)
                .map(value -> labelByValue.getOrDefault(value, value))
                .collect(Collectors.joining("、"));
    }

    private String resolveAnchorStyleLabel(String anchorStyle) {
        if (StrUtil.isBlank(anchorStyle)) {
            return "";
        }
        return dictService.listByType("dict_anchor_style").stream()
                .filter(item -> anchorStyle.equals(item.getDictValue()))
                .map(SysDictDataDO::getLabel)
                .findFirst()
                .orElse(anchorStyle);
    }

    private String fillPromptPlaceholders(String template, Map<String, String> vars) {
        String text = StrUtil.blankToDefault(template, "");
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            if (StrUtil.isBlank(entry.getValue())) {
                text = removeOptionalPromptSection(text, entry.getKey());
            }
        }
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String value = StrUtil.blankToDefault(entry.getValue(), "");
            text = text.replace("{{" + entry.getKey() + "}}", value)
                    .replace("{" + entry.getKey() + "}", value);
        }
        return text;
    }

    private String removeOptionalPromptSection(String template, String key) {
        if (StrUtil.isBlank(template) || StrUtil.isBlank(key)) {
            return template;
        }
        Pattern pattern = Pattern.compile("\\{\\{#" + Pattern.quote(key) + "\\}\\}[\\s\\S]*?\\{\\{/" + Pattern.quote(key) + "\\}\\}",
                Pattern.CASE_INSENSITIVE);
        return pattern.matcher(template).replaceAll("");
    }

    private int estimateTokens(String content) {
        return StrUtil.isBlank(content) ? 0 : Math.max(1, content.length() / 2);
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return tenantId;
    }

    private Long requireUserId() {
        Long userId = TenantContextHolder.getUserId();
        if (userId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return userId;
    }
}
