package cn.iocoder.yudao.module.oa.service.aicontent;

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

public interface AiContentService {

    AiContentGenerateRespVO generate(AiContentGenerateReq req);

    AiContentModelsRespVO listModels();

    AiContentPreferenceSummaryVO getPreferenceSummary(Long authorId);

    AiContentPreferenceUpdateRespVO updatePreferenceSummary(AiContentPreferenceUpdateReq req);

    AiContentPreferenceSummaryVO generatePreferenceFromConversation(AiContentPreferenceGenerateReq req);

    AiContentConversationVO getConversationHistory(Long contentId, Long authorId);

    AiContentConversationVO saveConversationHistory(AiContentConversationSaveReq req);

    AiContentAdoptRespVO adopt(AiContentAdoptReq req);
}
