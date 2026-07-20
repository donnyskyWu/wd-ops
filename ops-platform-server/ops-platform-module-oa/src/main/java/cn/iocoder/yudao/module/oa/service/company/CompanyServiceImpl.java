package cn.iocoder.yudao.module.oa.service.company;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyExpandReq;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyMpStatsRespVO;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyRespVO;
import cn.iocoder.yudao.module.oa.api.dto.company.CompanyUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.company.CompanyDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.company.CompanyExpansionDO;
import cn.iocoder.yudao.module.oa.dal.mysql.company.CompanyExpansionMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.company.CompanyMapper;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import cn.iocoder.yudao.module.oa.util.AesUtil;

import static cn.iocoder.yudao.module.oa.framework.operatelog.OaLogRecordConstants.*;
import cn.iocoder.yudao.module.oa.util.ImageKeyHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyMapper companyMapper;
    private final CompanyExpansionMapper companyExpansionMapper;
    private final AesUtil aesUtil;

    @Override
    public PageResult<CompanyRespVO> list(String companyName, String status, Integer pageNo, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<CompanyDO> wrapper = new LambdaQueryWrapper<CompanyDO>()
                .eq(CompanyDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(companyName), CompanyDO::getCompanyName, companyName)
                .eq(StrUtil.isNotBlank(status), CompanyDO::getStatus, status)
                .orderByDesc(CompanyDO::getId);
        Page<CompanyDO> page = companyMapper.selectPage(new Page<>(pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize), wrapper);
        List<CompanyRespVO> list = page.getRecords().stream().map(this::toResp).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public CompanyRespVO get(Long id) {
        return toResp(getRequiredInTenant(id));
    }

    @Override
    @Transactional
    @LogRecord(type = M4_COMPANY_TYPE, subType = M4_COMPANY_CREATE_SUB_TYPE, bizNo = "{{#company.id}}",
            success = M4_COMPANY_CREATE_SUCCESS)
    public Long create(CompanyCreateReq req) {
        Long tenantId = requireTenantId();
        assertCreditCodeUnique(tenantId, req.getCreditCode(), null);

        CompanyDO entity = new CompanyDO();
        entity.setTenantId(tenantId);
        entity.setCompanyName(req.getCompanyName());
        entity.setCreditCode(req.getCreditCode());
        entity.setIndustry(req.getIndustry());
        entity.setAddress(req.getAddress());
        entity.setLegalName(req.getLegalName());
        if (StrUtil.isNotBlank(req.getLegalIdCard())) {
            entity.setLegalIdCardEncrypted(aesUtil.encrypt(req.getLegalIdCard()));
        }
        entity.setMpCapacityStandard(req.getMpCapacityStandard() == null ? 0 : req.getMpCapacityStandard());
        entity.setMpRegisteredCount(0);
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setBusinessLicenseKeys(serializeLicenseKeys(req.getBusinessLicenseKeys(), tenantId));
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        companyMapper.insert(entity);
        LogRecordContext.putVariable("company", entity);
        return entity.getId();
    }

    @Override
    @Transactional
    @LogRecord(type = M4_COMPANY_TYPE, subType = M4_COMPANY_UPDATE_SUB_TYPE, bizNo = "{{#company.id}}",
            success = M4_COMPANY_UPDATE_SUCCESS)
    public void update(CompanyUpdateReq req) {
        CompanyDO existing = getRequiredInTenant(req.getId());
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, toUpdateReq(existing));
        LogRecordContext.putVariable("company", existing);
        if (StrUtil.isNotBlank(req.getCompanyName())) {
            existing.setCompanyName(req.getCompanyName());
        }
        if (req.getIndustry() != null) {
            existing.setIndustry(req.getIndustry());
        }
        if (req.getAddress() != null) {
            existing.setAddress(req.getAddress());
        }
        if (req.getLegalName() != null) {
            existing.setLegalName(req.getLegalName());
        }
        if (StrUtil.isNotBlank(req.getLegalIdCard())) {
            existing.setLegalIdCardEncrypted(aesUtil.encrypt(req.getLegalIdCard()));
        }
        if (req.getMpCapacityStandard() != null) {
            existing.setMpCapacityStandard(req.getMpCapacityStandard());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        if (req.getBusinessLicenseKeys() != null) {
            existing.setBusinessLicenseKeys(serializeLicenseKeys(req.getBusinessLicenseKeys(), existing.getTenantId()));
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        companyMapper.updateById(existing);
    }

    @Override
    @Transactional
    @LogRecord(type = M4_COMPANY_TYPE, subType = M4_COMPANY_DELETE_SUB_TYPE, bizNo = "{{#company.id}}",
            success = M4_COMPANY_DELETE_SUCCESS)
    public void delete(Long id) {
        CompanyDO existing = getRequiredInTenant(id);
        LogRecordContext.putVariable("company", existing);
        if (existing.getMpRegisteredCount() != null && existing.getMpRegisteredCount() > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND);
        }
        companyMapper.deleteById(id);
    }

    @Override
    @Transactional
    @LogRecord(type = M4_COMPANY_TYPE, subType = M4_COMPANY_EXPAND_SUB_TYPE, bizNo = "{{#company.id}}",
            success = M4_COMPANY_EXPAND_SUCCESS)
    public void expand(Long id, CompanyExpandReq req) {
        CompanyDO existing = getRequiredInTenant(id);
        LogRecordContext.putVariable("company", existing);
        int current = existing.getMpCapacityStandard() == null ? 0 : existing.getMpCapacityStandard();
        if (req.getNewCapacity() <= current) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "新容量必须大于当前容量");
        }
        existing.setMpCapacityStandard(req.getNewCapacity());
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        companyMapper.updateById(existing);

        CompanyExpansionDO expansion = new CompanyExpansionDO();
        expansion.setTenantId(existing.getTenantId());
        expansion.setCompanyId(id);
        expansion.setFromCapacity(current);
        expansion.setToCapacity(req.getNewCapacity());
        expansion.setReason(req.getReason());
        expansion.setOperatorName(TenantContextHolder.getUsername());
        expansion.setCreateTime(LocalDateTime.now());
        companyExpansionMapper.insert(expansion);
    }

    @Override
    public CompanyMpStatsRespVO mpStats(Long id) {
        CompanyDO existing = getRequiredInTenant(id);
        int capacity = existing.getMpCapacityStandard() == null ? 0 : existing.getMpCapacityStandard();
        int registered = existing.getMpRegisteredCount() == null ? 0 : existing.getMpRegisteredCount();
        int remaining = Math.max(capacity - registered, 0);
        CompanyMpStatsRespVO resp = new CompanyMpStatsRespVO();
        resp.setCompanyId(id);
        resp.setCapacity(capacity);
        resp.setRegistered(registered);
        resp.setRemaining(remaining);
        resp.setWarning(remaining <= capacity * 0.2);
        return resp;
    }

    private CompanyDO getRequiredInTenant(Long id) {
        CompanyDO entity = companyMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        Long tenantId = requireTenantId();
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private void assertCreditCodeUnique(Long tenantId, String creditCode, Long excludeId) {
        LambdaQueryWrapper<CompanyDO> wrapper = new LambdaQueryWrapper<CompanyDO>()
                .eq(CompanyDO::getTenantId, tenantId)
                .eq(CompanyDO::getCreditCode, creditCode);
        if (excludeId != null) {
            wrapper.ne(CompanyDO::getId, excludeId);
        }
        if (companyMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(), "统一社会信用代码已存在");
        }
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED);
        }
        return tenantId;
    }

    private CompanyRespVO toResp(CompanyDO entity) {
        CompanyRespVO vo = new CompanyRespVO();
        vo.setId(entity.getId());
        vo.setCompanyName(entity.getCompanyName());
        vo.setCreditCode(entity.getCreditCode());
        vo.setIndustry(entity.getIndustry());
        vo.setAddress(entity.getAddress());
        vo.setLegalName(entity.getLegalName());
        vo.setLegalIdCardMasked(maskIdCard(entity.getLegalIdCardEncrypted()));
        vo.setMpCapacityStandard(entity.getMpCapacityStandard());
        vo.setMpRegisteredCount(entity.getMpRegisteredCount());
        int capacity = entity.getMpCapacityStandard() == null ? 0 : entity.getMpCapacityStandard();
        int registered = entity.getMpRegisteredCount() == null ? 0 : entity.getMpRegisteredCount();
        vo.setMpRemaining(Math.max(capacity - registered, 0));
        vo.setStatus(entity.getStatus());
        List<String> licenseKeys = parseLicenseKeys(entity.getBusinessLicenseKeys());
        vo.setBusinessLicenseKeys(licenseKeys);
        vo.setBusinessLicenseUrls(ImageKeyHelper.toFileViewUrls(licenseKeys));
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private List<String> parseLicenseKeys(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        return JSONUtil.toList(json, String.class);
    }

    private String serializeLicenseKeys(List<String> keys, Long tenantId) {
        if (keys == null) {
            return null;
        }
        List<String> sanitized = ImageKeyHelper.sanitizeImageKeys(keys, tenantId);
        if (sanitized == null || sanitized.isEmpty()) {
            return "[]";
        }
        return JSONUtil.toJsonStr(sanitized);
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
            return plain.substring(0, 4) + "****" + plain.substring(plain.length() - 4);
        } catch (Exception ex) {
            return "****";
        }
    }

    private static CompanyUpdateReq toUpdateReq(CompanyDO entity) {
        CompanyUpdateReq req = new CompanyUpdateReq();
        req.setId(entity.getId());
        req.setCompanyName(entity.getCompanyName());
        req.setIndustry(entity.getIndustry());
        req.setAddress(entity.getAddress());
        req.setLegalName(entity.getLegalName());
        req.setMpCapacityStandard(entity.getMpCapacityStandard());
        req.setStatus(entity.getStatus());
        return req;
    }
}
