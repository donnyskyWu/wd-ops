package cn.iocoder.yudao.module.oa.service.realname;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.realname.RealnameCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.realname.RealnameRespVO;
import cn.iocoder.yudao.module.oa.api.dto.realname.RealnameUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.company.CompanyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.realname.RealnameDO;
import cn.iocoder.yudao.module.oa.dal.mysql.company.CompanyMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.realname.RealnameMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.util.AesUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RealnameServiceImpl implements RealnameService {

    private final RealnameMapper realnameMapper;
    private final CompanyMapper companyMapper;
    private final AesUtil aesUtil;

    @Override
    public PageResult<RealnameRespVO> list(String realName, Long companyId, String status,
                                           Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<RealnameDO> wrapper = new LambdaQueryWrapper<RealnameDO>()
                .eq(RealnameDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(realName), RealnameDO::getRealName, realName)
                .eq(companyId != null, RealnameDO::getCompanyId, companyId)
                .eq(StrUtil.isNotBlank(status), RealnameDO::getStatus, status)
                .orderByDesc(RealnameDO::getId);
        Page<RealnameDO> page = realnameMapper.selectPage(
                new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        Map<Long, String> companyNames = loadCompanyNames(page.getRecords());
        List<RealnameRespVO> list = page.getRecords().stream()
                .map(entity -> toResp(entity,
                        entity.getCompanyId() != null ? companyNames.get(entity.getCompanyId()) : null))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-realname", action = "create")
    public Long create(RealnameCreateReq req) {
        Long tenantId = requireTenantId();
        assertCompanyInTenant(req.getCompanyId(), tenantId);

        RealnameDO entity = new RealnameDO();
        entity.setTenantId(tenantId);
        entity.setCompanyId(req.getCompanyId());
        entity.setRealName(req.getRealName());
        entity.setIdType(StrUtil.blankToDefault(req.getIdType(), "ID_CARD"));
        entity.setIdCardEncrypted(aesUtil.encrypt(req.getIdCard()));
        entity.setPhoneEncrypted(aesUtil.encrypt(req.getPhone()));
        entity.setWechat(req.getWechat());
        entity.setGender(req.getGender());
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setAccountBoundCount(0);
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        realnameMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-realname", action = "update")
    public void update(RealnameUpdateReq req) {
        RealnameDO existing = getRequiredInTenant(req.getId());
        if (req.getCompanyId() != null) {
            assertCompanyInTenant(req.getCompanyId(), existing.getTenantId());
            existing.setCompanyId(req.getCompanyId());
        }
        if (StrUtil.isNotBlank(req.getRealName())) {
            existing.setRealName(req.getRealName());
        }
        if (StrUtil.isNotBlank(req.getIdType())) {
            existing.setIdType(req.getIdType());
        }
        if (StrUtil.isNotBlank(req.getIdCard())) {
            existing.setIdCardEncrypted(aesUtil.encrypt(req.getIdCard()));
        }
        if (StrUtil.isNotBlank(req.getPhone())) {
            existing.setPhoneEncrypted(aesUtil.encrypt(req.getPhone()));
        }
        if (req.getWechat() != null) {
            existing.setWechat(req.getWechat());
        }
        if (req.getGender() != null) {
            existing.setGender(req.getGender());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        realnameMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M4-realname", action = "delete")
    public void delete(Long id) {
        RealnameDO existing = getRequiredInTenant(id);
        if (existing.getAccountBoundCount() != null && existing.getAccountBoundCount() > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND);
        }
        realnameMapper.deleteById(id);
    }

    @Override
    public RealnameRespVO get(Long id) {
        RealnameDO entity = getRequiredInTenant(id);
        String companyName = null;
        if (entity.getCompanyId() != null) {
            CompanyDO company = companyMapper.selectById(entity.getCompanyId());
            if (company != null) {
                companyName = company.getCompanyName();
            }
        }
        return toResp(entity, companyName);
    }

    private RealnameDO getRequiredInTenant(Long id) {
        RealnameDO entity = realnameMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = requireTenantId();
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private void assertCompanyInTenant(Long companyId, Long tenantId) {
        if (companyId == null) {
            return;
        }
        CompanyDO company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联公司不存在");
        }
        if (!tenantId.equals(company.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
    }

    private Map<Long, String> loadCompanyNames(List<RealnameDO> records) {
        List<Long> ids = records.stream()
                .map(RealnameDO::getCompanyId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return companyMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CompanyDO::getId, CompanyDO::getCompanyName, (a, b) -> a));
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }

    private RealnameRespVO toResp(RealnameDO entity, String companyName) {
        RealnameRespVO vo = new RealnameRespVO();
        vo.setId(entity.getId());
        vo.setCompanyId(entity.getCompanyId());
        vo.setCompanyName(companyName);
        vo.setRealName(entity.getRealName());
        vo.setIdType(entity.getIdType());
        vo.setIdCardMasked(maskIdCard(entity.getIdCardEncrypted()));
        vo.setPhoneMasked(maskPhone(entity.getPhoneEncrypted()));
        vo.setWechat(entity.getWechat());
        vo.setGender(entity.getGender());
        vo.setStatus(entity.getStatus());
        vo.setAccountBoundCount(entity.getAccountBoundCount());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private String maskIdCard(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        try {
            String plain = aesUtil.decrypt(encrypted);
            if (plain.length() < 8) {
                return "****";
            }
            return plain.substring(0, 6) + "********" + plain.substring(plain.length() - 4);
        } catch (Exception ex) {
            return "****";
        }
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
