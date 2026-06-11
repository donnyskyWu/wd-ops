package cn.iocoder.yudao.module.oa.service.system;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.dict.DictTypeRespVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DictAdminRowVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DictCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DictDataItemReq;
import cn.iocoder.yudao.module.oa.api.dto.system.DictDataItemVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DictTypeDetailVO;
import cn.iocoder.yudao.module.oa.api.dto.system.DictUpdateReq;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictDataDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.SysDictTypeDO;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.SysDictDataMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.SysDictTypeMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import cn.iocoder.yudao.module.oa.service.dict.DictService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemDictServiceImpl implements SystemDictService {

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictDataMapper sysDictDataMapper;
    private final DictService dictService;

    @Override
    public List<DictTypeRespVO> typeList() {
        return sysDictTypeMapper.selectList(new LambdaQueryWrapper<SysDictTypeDO>()
                        .orderByAsc(SysDictTypeDO::getId))
                .stream()
                .map(t -> {
                    DictTypeRespVO vo = new DictTypeRespVO();
                    vo.setType(t.getType());
                    vo.setName(t.getName());
                    vo.setStatus(t.getStatus());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public DictTypeDetailVO getByType(String type) {
        SysDictTypeDO dictType = requireType(type);
        DictTypeDetailVO vo = new DictTypeDetailVO();
        vo.setDictType(dictType.getType());
        vo.setDictName(dictType.getName());
        vo.setStatus(dictType.getStatus());
        vo.setItems(listItems(type));
        return vo;
    }

    @Override
    public PageResult<DictAdminRowVO> adminList(String dictName, String dictType, String status,
                                                Integer pageNo, Integer pageSize) {
        List<DictAdminRowVO> all = buildAdminRows(dictName, dictType, status);
        int pn = pageNo == null ? 1 : pageNo;
        int ps = pageSize == null ? 10 : pageSize;
        int from = Math.max(0, (pn - 1) * ps);
        int to = Math.min(all.size(), from + ps);
        List<DictAdminRowVO> page = from >= all.size() ? List.of() : all.subList(from, to);
        return new PageResult<>(page, (long) all.size());
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-dict", action = "create")
    public Long create(DictCreateReq req) {
        Long typeCount = sysDictTypeMapper.selectCount(new LambdaQueryWrapper<SysDictTypeDO>()
                .eq(SysDictTypeDO::getType, req.getDictType()));
        if (typeCount != null && typeCount > 0) {
            throw new ServiceException(OaErrorCodes.DICT_TYPE_DUPLICATE);
        }
        SysDictTypeDO typeRow = new SysDictTypeDO();
        typeRow.setType(req.getDictType());
        typeRow.setName(req.getDictName());
        typeRow.setStatus("ENABLED");
        sysDictTypeMapper.insert(typeRow);
        insertItems(req.getDictType(), req.getItems());
        dictService.evictCache();
        return typeRow.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-dict", action = "update")
    public void update(DictUpdateReq req) {
        SysDictTypeDO typeRow = sysDictTypeMapper.selectById(req.getId());
        if (typeRow == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        typeRow.setName(req.getDictName());
        if (StrUtil.isNotBlank(req.getStatus())) {
            assertStatus(req.getStatus());
            typeRow.setStatus(req.getStatus());
        }
        sysDictTypeMapper.updateById(typeRow);
        if (req.getItems() != null) {
            for (DictDataItemReq item : req.getItems()) {
                if (item.getId() != null) {
                    updateItem(typeRow.getType(), item);
                } else {
                    insertItems(typeRow.getType(), List.of(item));
                }
            }
        }
        dictService.evictCache();
    }

    @Override
    @Transactional
    @AuditLog(module = "M9-dict", action = "delete")
    public void deleteData(Long id) {
        SysDictDataDO data = sysDictDataMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if ("ENABLED".equals(data.getStatus())) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(), "启用状态的字典项不可删除");
        }
        sysDictDataMapper.deleteById(id);
        dictService.evictCache();
    }

    private List<DictAdminRowVO> buildAdminRows(String dictName, String dictType, String status) {
        LambdaQueryWrapper<SysDictTypeDO> typeWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(dictName)) {
            typeWrapper.like(SysDictTypeDO::getName, dictName);
        }
        if (StrUtil.isNotBlank(dictType)) {
            typeWrapper.like(SysDictTypeDO::getType, dictType);
        }
        Map<String, SysDictTypeDO> typeMap = sysDictTypeMapper.selectList(typeWrapper).stream()
                .collect(Collectors.toMap(SysDictTypeDO::getType, t -> t, (a, b) -> a));

        LambdaQueryWrapper<SysDictDataDO> dataWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(dictType)) {
            dataWrapper.like(SysDictDataDO::getDictType, dictType);
        }
        if (StrUtil.isNotBlank(status)) {
            dataWrapper.eq(SysDictDataDO::getStatus, status);
        }
        List<SysDictDataDO> dataRows = sysDictDataMapper.selectList(dataWrapper);

        List<DictAdminRowVO> rows = new ArrayList<>();
        for (SysDictDataDO d : dataRows) {
            SysDictTypeDO t = typeMap.get(d.getDictType());
            if (t == null) {
                continue;
            }
            if (StrUtil.isNotBlank(dictName) && !t.getName().contains(dictName)) {
                continue;
            }
            DictAdminRowVO vo = new DictAdminRowVO();
            vo.setId(d.getId());
            vo.setTypeId(t.getId());
            vo.setDictName(t.getName());
            vo.setDictType(d.getDictType());
            vo.setDictLabel(d.getLabel());
            vo.setDictValue(d.getDictValue());
            vo.setSort(d.getSort());
            vo.setStatus(d.getStatus());
            vo.setColorType(d.getColorType());
            vo.setRemark(d.getRemark());
            rows.add(vo);
        }
        rows.sort(Comparator.comparing(DictAdminRowVO::getDictType).thenComparing(DictAdminRowVO::getSort));
        return rows;
    }

    private void insertItems(String dictType, List<DictDataItemReq> items) {
        for (DictDataItemReq item : items) {
            assertValueUnique(dictType, item.getDictValue(), null);
            SysDictDataDO row = new SysDictDataDO();
            row.setDictType(dictType);
            row.setLabel(item.getDictLabel());
            row.setDictValue(item.getDictValue());
            row.setSort(item.getSort() == null ? 0 : item.getSort());
            row.setStatus(StrUtil.blankToDefault(item.getStatus(), "ENABLED"));
            assertStatus(row.getStatus());
            row.setColorType(StrUtil.blankToDefault(item.getColorType(), "default"));
            row.setRemark(item.getRemark());
            sysDictDataMapper.insert(row);
        }
    }

    private void updateItem(String dictType, DictDataItemReq item) {
        SysDictDataDO row = sysDictDataMapper.selectById(item.getId());
        if (row == null || !dictType.equals(row.getDictType())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        assertValueUnique(dictType, item.getDictValue(), item.getId());
        row.setLabel(item.getDictLabel());
        row.setDictValue(item.getDictValue());
        if (item.getSort() != null) {
            row.setSort(item.getSort());
        }
        if (StrUtil.isNotBlank(item.getStatus())) {
            assertStatus(item.getStatus());
            row.setStatus(item.getStatus());
        }
        if (item.getColorType() != null) {
            row.setColorType(item.getColorType());
        }
        row.setRemark(item.getRemark());
        sysDictDataMapper.updateById(row);
    }

    private void assertValueUnique(String dictType, String value, Long excludeId) {
        LambdaQueryWrapper<SysDictDataDO> wrapper = new LambdaQueryWrapper<SysDictDataDO>()
                .eq(SysDictDataDO::getDictType, dictType)
                .eq(SysDictDataDO::getDictValue, value);
        if (excludeId != null) {
            wrapper.ne(SysDictDataDO::getId, excludeId);
        }
        Long count = sysDictDataMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_DUPLICATE);
        }
    }

    private SysDictTypeDO requireType(String type) {
        SysDictTypeDO dictType = sysDictTypeMapper.selectOne(new LambdaQueryWrapper<SysDictTypeDO>()
                .eq(SysDictTypeDO::getType, type));
        if (dictType == null) {
            throw new ServiceException(OaErrorCodes.DICT_TYPE_NOT_FOUND);
        }
        return dictType;
    }

    private List<DictDataItemVO> listItems(String type) {
        return sysDictDataMapper.selectList(new LambdaQueryWrapper<SysDictDataDO>()
                        .eq(SysDictDataDO::getDictType, type)
                        .orderByAsc(SysDictDataDO::getSort)
                        .orderByAsc(SysDictDataDO::getDictValue))
                .stream()
                .map(d -> {
                    DictDataItemVO vo = new DictDataItemVO();
                    vo.setId(d.getId());
                    vo.setDictLabel(d.getLabel());
                    vo.setValue(d.getDictValue());
                    vo.setSort(d.getSort());
                    vo.setStatus(d.getStatus());
                    vo.setColorType(d.getColorType());
                    vo.setRemark(d.getRemark());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private void assertStatus(String status) {
        if (!"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID);
        }
    }
}
