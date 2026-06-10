package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentImportVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentTrendDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ImportContentDataReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.ImportReviewReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.InternalContentVO;

import java.time.LocalDate;

public interface InternalContentService {

    /**
     * 内部内容列表（spec: FR-M1-006 / PRD-M1-运营管理 §4.6）
     * S-R7-Bug4 修复：原签名只收 platformType/dataSource/pageNo/pageSize，4 筛选项（ipGroupId/keyword/startDate/endDate）被 Spring 忽略。
     * 改 7 参数 + page/size（S-R3 风格统一）。
     *
     * @param platformType 平台类型（dict_platform_type，可空）
     * @param dataSource   数据来源（dict_data_source，可空）
     * @param ipGroupId    IP 组 ID（通过 oa_account 关联，可空）
     * @param keyword      关键词（模糊匹配 oa_content.title，可空）
     * @param startDate    发布起始日期（publishTime >= startDate，可空）
     * @param endDate      发布结束日期（publishTime <= endDate，可空）
     * @param page         页码（从 1 开始）
     * @param size         每页条数
     */
    PageResult<InternalContentVO> list(String platformType, String dataSource,
                                       Long ipGroupId, String keyword,
                                       LocalDate startDate, LocalDate endDate,
                                       Integer page, Integer size);

    Long submitImport(ImportContentDataReq req);

    PageResult<ContentImportVO> importList(Integer reviewStatus, Integer pageNo, Integer pageSize);

    void reviewImport(Long id, ImportReviewReq req);

    /**
     * 内容趋势详情（按日，从 oa_content_daily 取）
     *
     * @param contentId 内容 ID
     * @return 趋势详情（contentId/title/series）
     */
    ContentTrendDetailVO trend(Long contentId);
}
