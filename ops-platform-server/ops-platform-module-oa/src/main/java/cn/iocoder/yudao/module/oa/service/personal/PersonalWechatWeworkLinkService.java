package cn.iocoder.yudao.module.oa.service.personal;

import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.personal.PersonalWechatRespVO;
import cn.iocoder.yudao.module.oa.api.dto.personal.WeworkEmployeeRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.PersonalWechatAccountDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.personal.WeworkEmployeeDO;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.PersonalWechatAccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.personal.WeworkEmployeeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonalWechatWeworkLinkService {

    private final PersonalWechatAccountMapper personalWechatAccountMapper;
    private final WeworkEmployeeMapper weworkEmployeeMapper;

    @Transactional
    public void syncLink(Long personalWechatId, Long weworkEmployeeId) {
        Long tenantId = requireTenantId();
        if (personalWechatId == null) {
            return;
        }
        PersonalWechatAccountDO personalWechat = getPersonalWechatInTenant(personalWechatId, tenantId);
        if (weworkEmployeeId != null) {
            getWeworkEmployeeInTenant(weworkEmployeeId, tenantId);
            clearEmployeeLinkFromOthers(tenantId, weworkEmployeeId, personalWechatId);
        }
        personalWechat.setLinkedWeworkEmployeeId(weworkEmployeeId);
        personalWechat.setUpdater(TenantContextHolder.getUsername());
        personalWechatAccountMapper.updateById(personalWechat);
    }

    @Transactional
    public void clearLinkByEmployeeId(Long weworkEmployeeId) {
        if (weworkEmployeeId == null) {
            return;
        }
        Long tenantId = requireTenantId();
        personalWechatAccountMapper.update(null, new LambdaUpdateWrapper<PersonalWechatAccountDO>()
                .eq(PersonalWechatAccountDO::getTenantId, tenantId)
                .eq(PersonalWechatAccountDO::getLinkedWeworkEmployeeId, weworkEmployeeId)
                .set(PersonalWechatAccountDO::getLinkedWeworkEmployeeId, null));
    }

    public void enrichPersonalWechat(PersonalWechatRespVO vo, PersonalWechatAccountDO entity) {
        if (entity.getLinkedWeworkEmployeeId() == null) {
            return;
        }
        WeworkEmployeeDO employee = weworkEmployeeMapper.selectById(entity.getLinkedWeworkEmployeeId());
        if (employee == null || !entity.getTenantId().equals(employee.getTenantId())) {
            return;
        }
        vo.setLinkedWeworkEmployeeId(employee.getId());
        vo.setLinkedWeworkEmployeeName(employee.getNickname());
        vo.setLinkedWeworkUserId(employee.getWeworkUserId());
    }

    public void enrichWeworkEmployee(WeworkEmployeeRespVO vo, WeworkEmployeeDO entity) {
        PersonalWechatAccountDO personalWechat = personalWechatAccountMapper.selectOne(
                new LambdaQueryWrapper<PersonalWechatAccountDO>()
                        .eq(PersonalWechatAccountDO::getTenantId, entity.getTenantId())
                        .eq(PersonalWechatAccountDO::getLinkedWeworkEmployeeId, entity.getId())
                        .last("LIMIT 1"));
        if (personalWechat == null) {
            return;
        }
        vo.setLinkedPersonalWechatId(personalWechat.getId());
        vo.setLinkedPersonalWechatName(personalWechat.getAccountName());
        vo.setLinkedWechatId(personalWechat.getWechatId());
    }

    private void clearEmployeeLinkFromOthers(Long tenantId, Long weworkEmployeeId, Long keepPersonalWechatId) {
        personalWechatAccountMapper.update(null, new LambdaUpdateWrapper<PersonalWechatAccountDO>()
                .eq(PersonalWechatAccountDO::getTenantId, tenantId)
                .eq(PersonalWechatAccountDO::getLinkedWeworkEmployeeId, weworkEmployeeId)
                .ne(PersonalWechatAccountDO::getId, keepPersonalWechatId)
                .set(PersonalWechatAccountDO::getLinkedWeworkEmployeeId, null));
    }

    private PersonalWechatAccountDO getPersonalWechatInTenant(Long id, Long tenantId) {
        PersonalWechatAccountDO entity = personalWechatAccountMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联个微账号不存在");
        }
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.TENANT_FORBIDDEN);
        }
        return entity;
    }

    private WeworkEmployeeDO getWeworkEmployeeInTenant(Long id, Long tenantId) {
        WeworkEmployeeDO entity = weworkEmployeeMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS.getCode(), "关联企微员工不存在");
        }
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
}
