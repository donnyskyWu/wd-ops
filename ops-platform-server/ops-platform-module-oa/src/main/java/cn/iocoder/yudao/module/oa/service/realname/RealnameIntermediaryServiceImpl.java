package cn.iocoder.yudao.module.oa.service.realname;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.realname.IntermediaryCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.realname.IntermediaryRespVO;
import cn.iocoder.yudao.module.oa.api.dto.realname.IntermediaryUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameIntermediaryDO;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameIntermediaryMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUser;
import cn.iocoder.yudao.module.oa.framework.auth.LoginUserContext;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RealnameIntermediaryServiceImpl implements RealnameIntermediaryService {

    private final RealnameIntermediaryMapper intermediaryMapper;
    private final RealnameMapper realnameMapper;
    private final AesUtil aesUtil;

    @Override
    public List<IntermediaryRespVO> listByRealname(Long realnameId) {
        RealnameDO realname = getRealnameInTenant(realnameId);
        Long tenantId = realname.getTenantId();
        List<RealnameIntermediaryDO> list = intermediaryMapper.selectList(
                new LambdaQueryWrapper<RealnameIntermediaryDO>()
                        .eq(RealnameIntermediaryDO::getTenantId, tenantId)
                        .eq(RealnameIntermediaryDO::getRealnameId, realnameId)
                        .orderByDesc(RealnameIntermediaryDO::getId));
        boolean canViewCommission = canViewCommission();
        return list.stream().map(item -> toResp(item, canViewCommission)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-intermediary", action = "create")
    public Long create(Long realnameId, IntermediaryCreateReq req) {
        RealnameDO realname = getRealnameInTenant(realnameId);
        RealnameIntermediaryDO entity = new RealnameIntermediaryDO();
        entity.setTenantId(realname.getTenantId());
        entity.setRealnameId(realnameId);
        entity.setIntermediaryName(req.getIntermediaryName());
        if (StrUtil.isNotBlank(req.getIntermediaryPhone())) {
            entity.setIntermediaryPhoneEncrypted(aesUtil.encrypt(req.getIntermediaryPhone()));
        }
        entity.setIntermediaryWechat(req.getIntermediaryWechat());
        entity.setRelationType(req.getRelationType());
        entity.setCommissionRate(req.getCommissionRate());
        entity.setRemark(req.getRemark());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        intermediaryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-intermediary", action = "update")
    public void update(IntermediaryUpdateReq req) {
        RealnameIntermediaryDO existing = getRequiredInTenant(req.getId());
        if (StrUtil.isNotBlank(req.getIntermediaryName())) {
            existing.setIntermediaryName(req.getIntermediaryName());
        }
        if (StrUtil.isNotBlank(req.getIntermediaryPhone())) {
            existing.setIntermediaryPhoneEncrypted(aesUtil.encrypt(req.getIntermediaryPhone()));
        }
        if (req.getIntermediaryWechat() != null) {
            existing.setIntermediaryWechat(req.getIntermediaryWechat());
        }
        if (StrUtil.isNotBlank(req.getRelationType())) {
            existing.setRelationType(req.getRelationType());
        }
        if (req.getCommissionRate() != null) {
            existing.setCommissionRate(req.getCommissionRate());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        intermediaryMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-intermediary", action = "delete")
    public void delete(Long id) {
        getRequiredInTenant(id);
        intermediaryMapper.deleteById(id);
    }

    private RealnameDO getRealnameInTenant(Long realnameId) {
        RealnameDO entity = realnameMapper.selectById(realnameId);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = requireTenantId();
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private RealnameIntermediaryDO getRequiredInTenant(Long id) {
        RealnameIntermediaryDO entity = intermediaryMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = requireTenantId();
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }

    private boolean canViewCommission() {
        LoginUser user = LoginUserContext.get();
        if (user == null || user.getAuthorities() == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .anyMatch(auth -> "oa:*:*".equals(auth)
                        || "ROLE_OA_ADMIN".equals(auth)
                        || auth.startsWith("oa:realname:"));
    }

    private IntermediaryRespVO toResp(RealnameIntermediaryDO entity, boolean canViewCommission) {
        IntermediaryRespVO vo = new IntermediaryRespVO();
        vo.setId(entity.getId());
        vo.setRealnameId(entity.getRealnameId());
        vo.setIntermediaryName(entity.getIntermediaryName());
        vo.setIntermediaryPhoneMasked(maskPhone(entity.getIntermediaryPhoneEncrypted()));
        vo.setIntermediaryWechat(entity.getIntermediaryWechat());
        vo.setRelationType(entity.getRelationType());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        if (canViewCommission) {
            vo.setCommissionRate(entity.getCommissionRate());
            vo.setCommissionRateDisplay(entity.getCommissionRate() == null
                    ? null
                    : entity.getCommissionRate().stripTrailingZeros().toPlainString() + "%");
        } else {
            vo.setCommissionRate(null);
            vo.setCommissionRateDisplay("****");
        }
        return vo;
    }

    private String maskPhone(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        try {
            String plain = aesUtil.decrypt(encrypted);
            if (plain.length() != 11) {
                return "****";
            }
            return plain.substring(0, 3) + "****" + plain.substring(7);
        } catch (Exception ex) {
            return "****";
        }
    }
}
