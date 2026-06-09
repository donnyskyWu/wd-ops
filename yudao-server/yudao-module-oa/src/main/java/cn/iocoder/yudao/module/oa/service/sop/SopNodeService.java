package cn.iocoder.yudao.module.oa.service.sop;

import cn.iocoder.yudao.module.oa.api.dto.sop.DagValidateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.DagValidateResp;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopNodeCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopNodeUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.sop.SopNodeVO;

import java.util.List;

public interface SopNodeService {

    List<SopNodeVO> listByTemplateId(Long templateId);

    Long create(SopNodeCreateReq req);

    void update(SopNodeUpdateReq req);

    DagValidateResp validateDag(DagValidateReq req);
}
