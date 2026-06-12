package cn.iocoder.yudao.module.oa.service.realname;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.realname.RealnameCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.realname.RealnameRespVO;
import cn.iocoder.yudao.module.oa.api.dto.realname.RealnameUpdateReq;

public interface RealnameService {

    PageResult<RealnameRespVO> list(String realName, Long companyId, String status,
                                    Integer pageNo, Integer pageSize);

    Long create(RealnameCreateReq req);

    void update(RealnameUpdateReq req);

    void delete(Long id);

    RealnameRespVO get(Long id);
}
