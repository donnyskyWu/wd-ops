package cn.iocoder.yudao.module.oa.service.config;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateApiReq;
import cn.iocoder.yudao.module.oa.api.dto.config.AoCreateApiRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.config.AoCreateApiDO;
import cn.iocoder.yudao.module.oa.dal.mysql.config.AoCreateApiMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AoCreateApiServiceImpl implements AoCreateApiService {

    private static final String MASK = "****";

    private final AoCreateApiMapper aoCreateApiMapper;
    private final AesUtil aesUtil;

    @Override
    public AoCreateApiRespVO get() {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        AoCreateApiDO entity = aoCreateApiMapper.selectOne(new LambdaQueryWrapper<AoCreateApiDO>()
                .eq(AoCreateApiDO::getTenantId, tenantId)
                .last("LIMIT 1"));
        if (entity == null) {
            return null;
        }
        return toResp(entity);
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-aocreate", action = "save")
    public void save(AoCreateApiReq req) {
        Long tenantId = ConfigTenantSupport.requireTenantId();
        AoCreateApiDO entity = aoCreateApiMapper.selectOne(new LambdaQueryWrapper<AoCreateApiDO>()
                .eq(AoCreateApiDO::getTenantId, tenantId)
                .last("LIMIT 1"));
        if (entity == null) {
            entity = new AoCreateApiDO();
            entity.setApiUrl(req.getApiUrl());
            entity.setAppId(req.getAppId());
            if (StrUtil.isNotBlank(req.getAppSecret())) {
                entity.setAppSecretEncrypted(aesUtil.encrypt(req.getAppSecret()));
            }
            if (StrUtil.isNotBlank(req.getToken())) {
                entity.setTokenEncrypted(aesUtil.encrypt(req.getToken()));
            }
            entity.setStatus("ENABLED");
            entity.setDailyQuota(10000);
            entity.setCurrentUsage(0);
            ConfigTenantSupport.fillCreate(entity);
            aoCreateApiMapper.insert(entity);
            return;
        }
        entity.setApiUrl(req.getApiUrl());
        entity.setAppId(req.getAppId());
        if (StrUtil.isNotBlank(req.getAppSecret())) {
            entity.setAppSecretEncrypted(aesUtil.encrypt(req.getAppSecret()));
        }
        if (StrUtil.isNotBlank(req.getToken())) {
            entity.setTokenEncrypted(aesUtil.encrypt(req.getToken()));
        }
        ConfigTenantSupport.fillUpdate(entity);
        aoCreateApiMapper.updateById(entity);
    }

    private AoCreateApiRespVO toResp(AoCreateApiDO entity) {
        AoCreateApiRespVO vo = new AoCreateApiRespVO();
        vo.setId(entity.getId());
        vo.setApiUrl(entity.getApiUrl());
        vo.setAppId(entity.getAppId());
        vo.setAppSecretMasked(StrUtil.isNotBlank(entity.getAppSecretEncrypted()) ? MASK : null);
        vo.setTokenMasked(StrUtil.isNotBlank(entity.getTokenEncrypted()) ? MASK : null);
        vo.setStatus(entity.getStatus());
        vo.setDailyQuota(entity.getDailyQuota());
        vo.setCurrentUsage(entity.getCurrentUsage());
        return vo;
    }
}
