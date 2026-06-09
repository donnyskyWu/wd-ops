package cn.iocoder.yudao.module.oa.service.realname;

import cn.iocoder.yudao.module.oa.api.dto.realname.IntermediaryCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.realname.IntermediaryRespVO;
import cn.iocoder.yudao.module.oa.api.dto.realname.IntermediaryUpdateReq;

import java.util.List;

public interface RealnameIntermediaryService {

    List<IntermediaryRespVO> listByRealname(Long realnameId);

    Long create(Long realnameId, IntermediaryCreateReq req);

    void update(IntermediaryUpdateReq req);

    void delete(Long id);
}
