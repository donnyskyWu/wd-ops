package cn.iocoder.yudao.module.oa.service.content;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.oa.config.AiGenerateProperties;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AiModelConfigDO;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiLlmInvokeSupport {

    private final AesUtil aesUtil;
    private final AiGenerateProperties aiGenerateProperties;

    public String chatCompletion(AiModelConfigDO model, List<AiChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "对话消息不能为空");
        }
        if (!"CONNECTED".equals(model.getConnStatus()) || StrUtil.isBlank(model.getApiEndpoint())) {
            return buildMockContent(model, messages);
        }
        return invokeModelEndpoint(model, messages);
    }

    public String singleUserPrompt(AiModelConfigDO model, String prompt) {
        List<AiChatMessage> messages = new ArrayList<>();
        messages.add(AiChatMessage.user(prompt));
        return chatCompletion(model, messages);
    }

    private String invokeModelEndpoint(AiModelConfigDO model, List<AiChatMessage> messages) {
        String endpoint = resolveChatCompletionsUrl(model.getApiEndpoint());
        int timeoutMs = resolveLlmTimeoutMs(model);
        String apiKey = StrUtil.isNotBlank(model.getApiKeyEncrypted())
                ? aesUtil.decrypt(model.getApiKeyEncrypted()) : "";

        JSONObject body = new JSONObject();
        body.set("model", StrUtil.blankToDefault(model.getModelId(), model.getModelName()));
        body.set("max_tokens", model.getMaxTokens() == null ? 2048 : model.getMaxTokens());
        if (model.getTemperature() != null) {
            body.set("temperature", model.getTemperature());
        }
        if (model.getTopP() != null) {
            body.set("top_p", model.getTopP());
        }
        JSONArray messageArray = new JSONArray();
        for (AiChatMessage message : messages) {
            JSONObject item = new JSONObject();
            item.set("role", message.role());
            item.set("content", message.content());
            messageArray.add(item);
        }
        body.set("messages", messageArray);

        log.info("AI LLM calling model endpoint: model={}, url={}, timeoutMs={}",
                model.getModelName(), endpoint, timeoutMs);
        try {
            HttpRequest request = HttpRequest.post(endpoint)
                    .header("Content-Type", "application/json")
                    .timeout(timeoutMs)
                    .body(body.toString());
            if (StrUtil.isNotBlank(apiKey)) {
                request.header("Authorization", "Bearer " + apiKey);
            }

            HttpResponse response = request.execute();
            if (!response.isOk()) {
                log.warn("AI model HTTP {}: {}", response.getStatus(), response.body());
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                        "AI 模型调用失败（HTTP " + response.getStatus() + "）");
            }

            JSONObject respJson = JSONUtil.parseObj(response.body());
            JSONObject error = respJson.getJSONObject("error");
            if (error != null) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                        "AI 模型返回错误：" + StrUtil.blankToDefault(error.getStr("message"), error.toString()));
            }
            JSONArray choices = respJson.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "AI 模型未返回有效内容");
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message != null ? message.getStr("content") : null;
            if (StrUtil.isBlank(content)) {
                content = choices.getJSONObject(0).getStr("text");
            }
            if (StrUtil.isBlank(content)) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "AI 模型返回空正文");
            }
            return content.trim();
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("AI model call failed: model={}, url={}, timeoutMs={}, msg={}",
                    model.getModelName(), endpoint, timeoutMs, ex.getMessage(), ex);
            String detail = StrUtil.blankToDefault(ex.getMessage(), "外部服务不可用");
            if (detail.toLowerCase().contains("timed out") || detail.toLowerCase().contains("timeout")) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                        "AI 模型调用超时（已等待 " + (timeoutMs / 1000) + " 秒），请稍后重试或调大模型超时配置");
            }
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                    "AI 模型调用失败：" + detail);
        }
    }

    private String buildMockContent(AiModelConfigDO model, List<AiChatMessage> messages) {
        String lastUser = messages.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b)
                .map(AiChatMessage::content)
                .orElse("");
        return "【AI 对话占位 · " + StrUtil.blankToDefault(model.getModelName(), "模型") + "】\n\n"
                + "用户输入：" + StrUtil.blankToDefault(lastUser, "（无）") + "\n\n"
                + "## 赛事方案（占位）\n\n"
                + "### 核心推荐\n\n"
                + "基于您提供的信息，建议关注主队近期状态与历史交锋数据。\n\n"
                + "（模型未连通时将返回占位内容，连通 M8 模型配置后将返回真实生成结果）";
    }

    private int resolveLlmTimeoutMs(AiModelConfigDO model) {
        int floorSec = aiGenerateProperties.getLlmTimeoutSeconds();
        int modelSec = model.getTimeout() != null ? model.getTimeout() : floorSec;
        return Math.max(modelSec, floorSec) * 1000;
    }

    private String resolveChatCompletionsUrl(String apiEndpoint) {
        String url = StrUtil.removeSuffix(StrUtil.trim(apiEndpoint), "/");
        if (url.endsWith("/chat/completions")) {
            return url;
        }
        if (url.endsWith("/v1")) {
            return url + "/chat/completions";
        }
        return url + "/v1/chat/completions";
    }

    public record AiChatMessage(String role, String content) {
        public static AiChatMessage system(String content) {
            return new AiChatMessage("system", content);
        }

        public static AiChatMessage user(String content) {
            return new AiChatMessage("user", content);
        }

        public static AiChatMessage assistant(String content) {
            return new AiChatMessage("assistant", content);
        }
    }
}
