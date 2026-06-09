package cn.iocoder.yudao.module.oa.service.author;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorDashboardVO;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.author.AuthorVO;
import cn.iocoder.yudao.module.oa.api.dto.author.OpsUserVO;

import java.util.List;

public interface AuthorService {

    PageResult<AuthorVO> list(Long ipGroupId, String keyword, Integer status, Integer pageNo, Integer pageSize);

    Long create(AuthorCreateReq req);

    void update(AuthorUpdateReq req);

    void delete(Long id);

    AuthorDashboardVO dashboard(Long id);

    List<OpsUserVO> opsList(Long id);
}
