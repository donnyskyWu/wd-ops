package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.module.oa.api.dto.system.PermissionRespVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysPermissionDO;
import cn.iocoder.yudao.module.oa.dal.mysql.auth.SysPermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper sysPermissionMapper;

    @Override
    public List<PermissionRespVO> listAll() {
        return sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermissionDO>()
                        .orderByAsc(SysPermissionDO::getId))
                .stream()
                .map(p -> {
                    PermissionRespVO vo = new PermissionRespVO();
                    vo.setId(p.getId());
                    vo.setCode(p.getCode());
                    vo.setName(p.getName());
                    vo.setModule(p.getModule());
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
