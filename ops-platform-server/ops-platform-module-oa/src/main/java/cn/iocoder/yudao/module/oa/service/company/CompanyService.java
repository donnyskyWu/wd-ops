package cn.iocoder.yudao.module.oa.service.company;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyExpandReq;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyMpStatsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyRespVO;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyUpdateReq;

/**
 * 公司管理服务接口
 * <p>
 * 提供公司信息的查询、创建、更新、删除、扩容和公众号统计等业务功能。
 * </p>
 *
 * @author system
 */

public interface CompanyService {

    PageResult<CompanyRespVO> list(String companyName, String status, Integer pageNo, Integer pageSize);

    CompanyRespVO get(Long id);

    Long create(CompanyCreateReq req);

    void update(CompanyUpdateReq req);

    void delete(Long id);

    void expand(Long id, CompanyExpandReq req);

    CompanyMpStatsRespVO mpStats(Long id);
}
