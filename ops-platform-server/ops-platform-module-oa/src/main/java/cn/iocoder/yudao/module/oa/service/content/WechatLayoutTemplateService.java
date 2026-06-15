package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.ContentApplyLayoutTemplateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutImportJobCreateResp;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutImportJobVO;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutMergePreviewReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutMergePreviewVO;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutImportPasteReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutImportUrlReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutTemplateCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutTemplateDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutTemplateSelectVO;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutTemplateUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutTemplateVO;
import cn.iocoder.yudao.module.oa.api.dto.content.ProductionContentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface WechatLayoutTemplateService {

    PageResult<LayoutTemplateVO> list(String templateName, String documentType, String status,
                                      String sourceType, Integer pageNum, Integer pageSize);

    LayoutTemplateDetailVO getById(Long id);

    List<LayoutTemplateSelectVO> selectList(String contentType, String documentType);

    Long create(LayoutTemplateCreateReq req);

    void update(LayoutTemplateUpdateReq req);

    void delete(Long id);

    void publish(Long id);

    void disable(Long id);

    void enable(Long id);

    LayoutImportJobCreateResp importUrl(LayoutImportUrlReq req);

    LayoutImportJobCreateResp importDocx(MultipartFile file, String templateName, String documentType);

    LayoutTemplateDetailVO importPaste(LayoutImportPasteReq req);

    LayoutImportJobVO getImportJob(Long jobId);

    ProductionContentVO applyLayoutTemplate(Long contentId, ContentApplyLayoutTemplateReq req);

    LayoutMergePreviewVO previewMerge(Long templateId, LayoutMergePreviewReq req);

    Long copyTemplate(Long id);

    boolean matchesDocumentType(String templateDocumentType, String contentDocumentType);
}
