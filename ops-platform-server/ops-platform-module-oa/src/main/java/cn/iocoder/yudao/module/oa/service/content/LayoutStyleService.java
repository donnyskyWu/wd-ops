package cn.iocoder.yudao.module.oa.service.content;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutStyleCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutStyleUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.content.LayoutStyleVO;

import java.util.List;

public interface LayoutStyleService {

    PageResult<LayoutStyleVO> list(String name, String category, String tags, String status,
                                   Integer pageNum, Integer pageSize);

    List<LayoutStyleVO> listEnabled(String category, String keyword);

    LayoutStyleVO getById(Long id);

    Long create(LayoutStyleCreateReq req);

    void update(LayoutStyleUpdateReq req);

    void delete(Long id);
}
