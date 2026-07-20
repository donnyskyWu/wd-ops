package cn.iocoder.yudao.module.oa.controller.aicontent;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentAdoptReq;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentAdoptRespVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentConversationSaveReq;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentConversationVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentGenerateReq;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentGenerateRespVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentModelsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentPreferenceGenerateReq;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentPreferenceSummaryVO;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentPreferenceUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.aicontent.AiContentPreferenceUpdateRespVO;
import cn.iocoder.yudao.module.oa.service.aicontent.AiContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oa/ai-content")
@Validated
@RequiredArgsConstructor
public class AiContentController {

    private final AiContentService aiContentService;

    @PostMapping("/generate")
    public CommonResult<AiContentGenerateRespVO> generate(@Valid @RequestBody AiContentGenerateReq req) {
        return CommonResult.success(aiContentService.generate(req));
    }

    @GetMapping("/models")
    public CommonResult<AiContentModelsRespVO> listModels() {
        return CommonResult.success(aiContentService.listModels());
    }

    @GetMapping("/preference-summary")
    public CommonResult<AiContentPreferenceSummaryVO> getPreferenceSummary(
            @RequestParam(required = false) Long authorId) {
        return CommonResult.success(aiContentService.getPreferenceSummary(authorId));
    }

    @PutMapping("/preference-summary")
    public CommonResult<AiContentPreferenceUpdateRespVO> updatePreferenceSummary(
            @Valid @RequestBody AiContentPreferenceUpdateReq req) {
        return CommonResult.success(aiContentService.updatePreferenceSummary(req));
    }

    @PostMapping("/preference-summary/generate")
    public CommonResult<AiContentPreferenceSummaryVO> generatePreferenceSummary(
            @Valid @RequestBody AiContentPreferenceGenerateReq req) {
        return CommonResult.success(aiContentService.generatePreferenceFromConversation(req));
    }

    @GetMapping("/conversation")
    public CommonResult<AiContentConversationVO> getConversation(
            @RequestParam(required = false) Long contentId,
            @RequestParam(required = false) Long authorId) {
        return CommonResult.success(aiContentService.getConversationHistory(contentId, authorId));
    }

    @PutMapping("/conversation")
    public CommonResult<AiContentConversationVO> saveConversation(
            @Valid @RequestBody AiContentConversationSaveReq req) {
        return CommonResult.success(aiContentService.saveConversationHistory(req));
    }

    @PostMapping("/adopt")
    public CommonResult<AiContentAdoptRespVO> adopt(@Valid @RequestBody AiContentAdoptReq req) {
        return CommonResult.success(aiContentService.adopt(req));
    }
}
