package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentAiGenerateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentAiGenerateResultVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentAiPromptOptionVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentGenerateResultVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewConfigVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentReviewReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentScriptRefVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentVO;

import java.util.List;

public interface ProductionContentService {

    PageResult<ProductionContentVO> list(String title, String platformType, String contentType,
                                          Long accountId, String status, Integer aiGenerated,
                                          Integer pageNum, Integer pageSize);

    Long create(ProductionContentCreateReq req);

    void update(ProductionContentUpdateReq req);

    void submitReview(Long id);

    void review(Long id, ContentReviewReq req);

    void delete(Long id);

    ProductionContentVO getByTaskId(Long taskId);

    ProductionContentVO getById(Long id);

    ContentScriptRefVO getScriptRef(String competitionId, String documentType);

    void confirm(Long id);

    ContentGenerateResultVO generate(Long id);

    List<ContentAiPromptOptionVO> listAiPromptOptions(String contentType, String documentType);

    ContentAiGenerateResultVO aiGenerate(ContentAiGenerateReq req);

    ContentReviewConfigVO getReviewConfig();
}
