package cn.iocoder.yudao.module.oa.service.account;

import cn.iocoder.yudao.framework.common.biz.mp.user.MpUserApi;
import cn.iocoder.yudao.framework.common.biz.mp.user.dto.MpUserDTO;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.account.MpUserDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * G-MP-01 cutover: Feign-only via {@link MpUserApi}（无 mp {@code @DS} 回退）。
 */
@Service
@RequiredArgsConstructor
public class MpUserDataService {

    private static final int SUBSCRIBE_STATUS_ACTIVE = 1;

    private final MpUserApi mpUserApi;

    public Page<MpUserDO> selectPageByAccount(Page<MpUserDO> page, Long tenantId, Long mpAccountId) {
        return loadPageViaFeign(page, tenantId, mpAccountId);
    }

    Page<MpUserDO> loadPageViaFeign(Page<MpUserDO> page, Long tenantId, Long mpAccountId) {
        if (mpUserApi == null || page == null || mpAccountId == null) {
            throw rpcUnavailable();
        }
        try {
            int pageNo = (int) page.getCurrent();
            int pageSize = (int) page.getSize();
            CommonResult<PageResult<MpUserDTO>> result = mpUserApi.getUserPageByAccount(
                    mpAccountId, pageNo, pageSize, SUBSCRIBE_STATUS_ACTIVE);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                throw rpcUnavailable();
            }
            PageResult<MpUserDTO> data = result.getData();
            List<MpUserDO> records = data.getList() == null
                    ? List.of()
                    : data.getList().stream()
                            .map(dto -> toDo(dto, tenantId))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
            Page<MpUserDO> feignPage = new Page<>(pageNo, pageSize);
            feignPage.setRecords(records);
            feignPage.setTotal(data.getTotal() == null ? records.size() : data.getTotal());
            return feignPage;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw rpcUnavailable();
        }
    }

    static MpUserDO toDo(MpUserDTO dto, Long tenantId) {
        if (dto == null || dto.getId() == null) {
            return null;
        }
        MpUserDO user = new MpUserDO();
        user.setId(dto.getId());
        user.setOpenid(dto.getOpenid());
        user.setUnionId(dto.getUnionId());
        user.setSubscribeStatus(dto.getSubscribeStatus());
        user.setSubscribeTime(dto.getSubscribeTime());
        user.setNickname(dto.getNickname());
        user.setHeadImageUrl(dto.getHeadImageUrl());
        user.setAccountId(dto.getAccountId());
        user.setAppId(dto.getAppId());
        user.setUpdateTime(dto.getUpdateTime());
        user.setTenantId(tenantId);
        return user;
    }

    private static ServiceException rpcUnavailable() {
        return new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(),
                "公众号粉丝服务不可用，请确认 mp-server 已启动");
    }
}
