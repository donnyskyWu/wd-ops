package cn.iocoder.yudao.module.oa.service.company;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyExpandReq;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyMpStatsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyRespVO;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyUpdateReq;

public interface CompanyService {

    PageResult<CompanyRespVO> list(String companyName, String status, Integer pageNo, Integer pageSize);

    CompanyRespVO get(Long id);

    Long create(CompanyCreateReq req);

    void update(CompanyUpdateReq req);

    void delete(Long id);

    void expand(Long id, CompanyExpandReq req);

    CompanyMpStatsRespVO mpStats(Long id);
}
