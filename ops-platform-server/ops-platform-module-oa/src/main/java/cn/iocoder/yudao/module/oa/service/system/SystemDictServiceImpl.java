package cn.iocoder.yudao.module.oa.service.system;



import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;

import cn.iocoder.yudao.framework.common.exception.ServiceException;

import cn.iocoder.yudao.framework.common.pojo.PageResult;

import cn.iocoder.yudao.module.oa.api.dto.dict.DictTypeRespVO;

import cn.iocoder.yudao.module.oa.api.dto.system.DictAdminRowVO;

import cn.iocoder.yudao.module.oa.api.dto.system.DictCreateReq;

import cn.iocoder.yudao.module.oa.api.dto.system.DictTypeDetailVO;

import cn.iocoder.yudao.module.oa.api.dto.system.DictUpdateReq;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;



import java.util.List;



/**

 * 字典只读路由（OPS dict merge）：全部读 shenyu-system.system_dict_* via {@link SystemDictAdapter}。

 * CRUD 已迁移至 Football 菜单 105，OPS 写接口返回 410。

 */

@Service

@RequiredArgsConstructor

public class SystemDictServiceImpl implements SystemDictService {



    private final SystemDictAdapter systemDictAdapter;



    @Override

    public List<DictTypeRespVO> typeList() {

        return systemDictAdapter.typeList();

    }



    @Override

    public DictTypeDetailVO getByType(String type) {

        return systemDictAdapter.getByType(type);

    }



    @Override

    public PageResult<DictAdminRowVO> adminList(String dictName, String dictType, String status,

                                                Integer pageNo, Integer pageSize) {

        return systemDictAdapter.adminList(dictName, dictType, status, pageNo, pageSize);

    }



    @Override

    public Long create(DictCreateReq req) {

        throw new ServiceException(OaErrorCodes.DICT_CRUD_DEPRECATED);

    }



    @Override

    public void update(DictUpdateReq req) {

        throw new ServiceException(OaErrorCodes.DICT_CRUD_DEPRECATED);

    }



    @Override

    public void deleteData(Long id) {

        throw new ServiceException(OaErrorCodes.DICT_CRUD_DEPRECATED);

    }

}

