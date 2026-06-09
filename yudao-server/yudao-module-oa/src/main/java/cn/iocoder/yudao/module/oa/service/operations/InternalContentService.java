package cn.iocoder.yudao.module.oa.service.operations;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.operations.ContentImportVO;
import cn.iocoder.yudao.module.oa.api.dto.operations.ImportContentDataReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.ImportReviewReq;
import cn.iocoder.yudao.module.oa.api.dto.operations.InternalContentVO;

public interface InternalContentService {

    PageResult<InternalContentVO> list(String platformType, String dataSource, Integer pageNo, Integer pageSize);

    Long submitImport(ImportContentDataReq req);

    PageResult<ContentImportVO> importList(Integer reviewStatus, Integer pageNo, Integer pageSize);

    void reviewImport(Long id, ImportReviewReq req);
}
