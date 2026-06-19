package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.TypesettingRuleCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.TypesettingRuleUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.TypesettingRuleVO;

import java.util.List;

public interface TypesettingRuleService {

    PageResult<TypesettingRuleVO> list(String name, String status, Integer pageNum, Integer pageSize);

    List<TypesettingRuleVO> listEnabled();

    TypesettingRuleVO getById(Long id);

    Long create(TypesettingRuleCreateReq req);

    void update(TypesettingRuleUpdateReq req);

    void delete(Long id);
}
