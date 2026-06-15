package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.module.oa.api.dto.account.WechatCertRenewalCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.account.WechatCertRenewalRespVO;

import java.util.List;

public interface WechatOfficialCertRenewalService {

    List<WechatCertRenewalRespVO> listByAccount(Long accountId);

    Long create(WechatCertRenewalCreateReq req);

    void delete(Long id);
}
