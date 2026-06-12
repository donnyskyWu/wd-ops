package cn.iocoder.yudao.module.oa.service.config;

import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateApiReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateApiRespVO;

public interface AoCreateApiService {

    AoCreateApiRespVO get();

    void save(AoCreateApiReq req);
}
