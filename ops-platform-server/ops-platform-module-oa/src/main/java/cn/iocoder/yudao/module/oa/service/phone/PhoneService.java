package cn.iocoder.yudao.module.oa.service.phone;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.phone.PhoneCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.phone.PhoneRespVO;
import cn.iocoder.yudao.module.oa.api.dto.phone.PhoneUpdateReq;

public interface PhoneService {

    PageResult<PhoneRespVO> list(String phoneNumber, Long realnameId, String status,
                                 Integer pageNo, Integer pageSize);

    Long create(PhoneCreateReq req);

    void update(PhoneUpdateReq req);

    void delete(Long id);
}
