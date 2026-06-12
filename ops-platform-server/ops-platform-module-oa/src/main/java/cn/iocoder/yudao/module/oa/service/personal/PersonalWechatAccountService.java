package cn.iocoder.yudao.module.oa.service.personal;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatApiConfigReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatUpdateReq;

public interface PersonalWechatAccountService {

    PageResult<PersonalWechatRespVO> list(String accountName, String wechatId, String status,
                                          Integer pageNo, Integer pageSize);

    PersonalWechatRespVO get(Long id);

    Long create(PersonalWechatCreateReq req);

    void update(PersonalWechatUpdateReq req);

    void delete(Long id);

    void saveApiConfig(PersonalWechatApiConfigReq req);

    PersonalWechatRespVO getApiConfig(Long id);
}
