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
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.FootballSystemDictDataDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.FootballSystemDictTypeDO;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.FootballSystemDictDataMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.FootballSystemDictTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台字典 Adapter（S2 / ADR-050 D4）：读 shenyu-system.system_dict_*，映射至 Ops VO。
 */
@Service
@RequiredArgsConstructor
public class SystemDictAdapter {

    private final FootballSystemDictTypeMapper footballSystemDictTypeMapper;
    private final FootballSystemDictDataMapper footballSystemDictDataMapper;

    public List<DictTypeRespVO> typeList() {
        return footballSystemDictTypeMapper.selectList(new LambdaQueryWrapper<FootballSystemDictTypeDO>()
                        .orderByAsc(FootballSystemDictTypeDO::getId))
                .stream()
                .map(this::toTypeResp)
                .collect(Collectors.toList());
    }

    public DictTypeDetailVO getByType(String type) {
        FootballSystemDictTypeDO dictType = requireType(type);
        DictTypeDetailVO vo = new DictTypeDetailVO();
        vo.setDictType(dictType.getType());
        vo.setDictName(dictType.getName());
        vo.setStatus(toOpsStatus(dictType.getStatus()));
        vo.setItems(listItems(type));
        return vo;
    }

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

    public Long create(DictCreateReq req) {
        Long typeCount = footballSystemDictTypeMapper.selectCount(new LambdaQueryWrapper<FootballSystemDictTypeDO>()
                .eq(FootballSystemDictTypeDO::getType, req.getDictType()));
        if (typeCount != null && typeCount > 0) {
            throw new ServiceException(OaErrorCodes.DICT_TYPE_DUPLICATE);
        }
        FootballSystemDictTypeDO typeRow = new FootballSystemDictTypeDO();
        typeRow.setType(req.getDictType());
        typeRow.setName(req.getDictName());
        typeRow.setStatus(0);
        footballSystemDictTypeMapper.insert(typeRow);
        insertItems(req.getDictType(), req.getItems());
        return typeRow.getId();
    }

    public void update(DictUpdateReq req) {
        FootballSystemDictTypeDO typeRow = footballSystemDictTypeMapper.selectById(req.getId());
        if (typeRow == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        typeRow.setName(req.getDictName());
        if (StrUtil.isNotBlank(req.getStatus())) {
            typeRow.setStatus(fromOpsStatus(req.getStatus()));
        }
        footballSystemDictTypeMapper.updateById(typeRow);
        if (req.getItems() != null) {
            for (DictDataItemReq item : req.getItems()) {
                if (item.getId() != null) {
                    updateItem(typeRow.getType(), item);
                } else {
                    insertItems(typeRow.getType(), List.of(item));
                }
            }
        }
    }

    public void deleteData(Long id) {
        FootballSystemDictDataDO data = footballSystemDictDataMapper.selectById(id);
        if (data == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        if (data.getStatus() != null && data.getStatus() == 0) {
            throw new ServiceException(OaErrorCodes.ENTITY_ALREADY_BOUND.getCode(), "启用状态的字典项不可删除");
        }
        footballSystemDictDataMapper.deleteById(id);
    }

    public long countActiveDataRows() {
        Long count = footballSystemDictDataMapper.countActiveRows();
        return count == null ? 0L : count;
    }

    public boolean isValidValue(String dictType, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        Long count = footballSystemDictDataMapper.selectCount(new LambdaQueryWrapper<FootballSystemDictDataDO>()
                .eq(FootballSystemDictDataDO::getDictType, dictType)
                .eq(FootballSystemDictDataDO::getValue, value)
                .eq(FootballSystemDictDataDO::getStatus, 0));
        return count != null && count > 0;
    }

    public boolean typeExists(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            return false;
        }
        Long count = footballSystemDictTypeMapper.selectCount(new LambdaQueryWrapper<FootballSystemDictTypeDO>()
                .eq(FootballSystemDictTypeDO::getType, dictType)
                .eq(FootballSystemDictTypeDO::getStatus, 0));
        return count != null && count > 0;
    }

    public List<FootballSystemDictDataDO> listEnabledDataByType(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            return List.of();
        }
        return footballSystemDictDataMapper.selectList(new LambdaQueryWrapper<FootballSystemDictDataDO>()
                .eq(FootballSystemDictDataDO::getDictType, dictType)
                .eq(FootballSystemDictDataDO::getStatus, 0)
                .orderByAsc(FootballSystemDictDataDO::getSort)
                .orderByAsc(FootballSystemDictDataDO::getValue));
    }

    public List<FootballSystemDictTypeDO> listEnabledTypes() {
        return footballSystemDictTypeMapper.selectList(new LambdaQueryWrapper<FootballSystemDictTypeDO>()
                .eq(FootballSystemDictTypeDO::getStatus, 0)
                .orderByAsc(FootballSystemDictTypeDO::getId));
    }

    public static String toOpsStatus(Integer footballStatus) {
        return footballStatus != null && footballStatus == 0 ? "ENABLED" : "DISABLED";
    }

    static Integer fromOpsStatus(String opsStatus) {
        if ("ENABLED".equals(opsStatus)) {
            return 0;
        }
        if ("DISABLED".equals(opsStatus)) {
            return 1;
        }
        throw new ServiceException(OaErrorCodes.DICT_VALUE_INVALID);
    }

    private DictTypeRespVO toTypeResp(FootballSystemDictTypeDO t) {
        DictTypeRespVO vo = new DictTypeRespVO();
        vo.setType(t.getType());
        vo.setName(t.getName());
        vo.setStatus(toOpsStatus(t.getStatus()));
        return vo;
    }

    private List<DictAdminRowVO> buildAdminRows(String dictName, String dictType, String status) {
        LambdaQueryWrapper<FootballSystemDictTypeDO> typeWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(dictName)) {
            typeWrapper.like(FootballSystemDictTypeDO::getName, dictName);
        }
        if (StrUtil.isNotBlank(dictType)) {
            typeWrapper.like(FootballSystemDictTypeDO::getType, dictType);
        }
        Map<String, FootballSystemDictTypeDO> typeMap = footballSystemDictTypeMapper.selectList(typeWrapper).stream()
                .collect(Collectors.toMap(FootballSystemDictTypeDO::getType, t -> t, (a, b) -> a));

        LambdaQueryWrapper<FootballSystemDictDataDO> dataWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(dictType)) {
            dataWrapper.like(FootballSystemDictDataDO::getDictType, dictType);
        }
        if (StrUtil.isNotBlank(status)) {
            dataWrapper.eq(FootballSystemDictDataDO::getStatus, fromOpsStatus(status));
        }
        List<FootballSystemDictDataDO> dataRows = footballSystemDictDataMapper.selectList(dataWrapper);

        List<DictAdminRowVO> rows = new ArrayList<>();
        for (FootballSystemDictDataDO d : dataRows) {
            FootballSystemDictTypeDO t = typeMap.get(d.getDictType());
            if (t == null) {
                continue;
            }
            if (StrUtil.isNotBlank(dictName) && !t.getName().contains(dictName)) {
                continue;
            }
            rows.add(toAdminRow(d, t));
        }
        rows.sort(Comparator.comparing(DictAdminRowVO::getDictType).thenComparing(DictAdminRowVO::getSort));
        return rows;
    }

    private DictAdminRowVO toAdminRow(FootballSystemDictDataDO d, FootballSystemDictTypeDO t) {
        DictAdminRowVO vo = new DictAdminRowVO();
        vo.setId(d.getId());
        vo.setTypeId(t.getId());
        vo.setDictName(t.getName());
        vo.setDictType(d.getDictType());
        vo.setDictLabel(d.getLabel());
        vo.setDictValue(d.getValue());
        vo.setSort(d.getSort());
        vo.setStatus(toOpsStatus(d.getStatus()));
        vo.setColorType(d.getColorType());
        vo.setRemark(d.getRemark());
        return vo;
    }

    private void insertItems(String dictType, List<DictDataItemReq> items) {
        for (DictDataItemReq item : items) {
            assertValueUnique(dictType, item.getDictValue(), null);
            FootballSystemDictDataDO row = new FootballSystemDictDataDO();
            row.setDictType(dictType);
            row.setLabel(item.getDictLabel());
            row.setValue(item.getDictValue());
            row.setSort(item.getSort() == null ? 0 : item.getSort());
            row.setStatus(StrUtil.isBlank(item.getStatus()) ? 0 : fromOpsStatus(item.getStatus()));
            row.setColorType(StrUtil.blankToDefault(item.getColorType(), "default"));
            row.setRemark(item.getRemark());
            footballSystemDictDataMapper.insert(row);
        }
    }

    private void updateItem(String dictType, DictDataItemReq item) {
        FootballSystemDictDataDO row = footballSystemDictDataMapper.selectById(item.getId());
        if (row == null || !dictType.equals(row.getDictType())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        assertValueUnique(dictType, item.getDictValue(), item.getId());
        row.setLabel(item.getDictLabel());
        row.setValue(item.getDictValue());
        if (item.getSort() != null) {
            row.setSort(item.getSort());
        }
        if (StrUtil.isNotBlank(item.getStatus())) {
            row.setStatus(fromOpsStatus(item.getStatus()));
        }
        if (item.getColorType() != null) {
            row.setColorType(item.getColorType());
        }
        row.setRemark(item.getRemark());
        footballSystemDictDataMapper.updateById(row);
    }

    private void assertValueUnique(String dictType, String value, Long excludeId) {
        LambdaQueryWrapper<FootballSystemDictDataDO> wrapper = new LambdaQueryWrapper<FootballSystemDictDataDO>()
                .eq(FootballSystemDictDataDO::getDictType, dictType)
                .eq(FootballSystemDictDataDO::getValue, value);
        if (excludeId != null) {
            wrapper.ne(FootballSystemDictDataDO::getId, excludeId);
        }
        Long count = footballSystemDictDataMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException(OaErrorCodes.DICT_VALUE_DUPLICATE);
        }
    }

    private FootballSystemDictTypeDO requireType(String type) {
        FootballSystemDictTypeDO dictType = footballSystemDictTypeMapper.selectOne(
                new LambdaQueryWrapper<FootballSystemDictTypeDO>().eq(FootballSystemDictTypeDO::getType, type));
        if (dictType == null) {
            throw new ServiceException(OaErrorCodes.DICT_TYPE_NOT_FOUND);
        }
        return dictType;
    }

    private List<DictDataItemVO> listItems(String type) {
        return footballSystemDictDataMapper.selectList(new LambdaQueryWrapper<FootballSystemDictDataDO>()
                        .eq(FootballSystemDictDataDO::getDictType, type)
                        .orderByAsc(FootballSystemDictDataDO::getSort)
                        .orderByAsc(FootballSystemDictDataDO::getValue))
                .stream()
                .map(d -> {
                    DictDataItemVO vo = new DictDataItemVO();
                    vo.setId(d.getId());
                    vo.setDictLabel(d.getLabel());
                    vo.setValue(d.getValue());
                    vo.setSort(d.getSort());
                    vo.setStatus(toOpsStatus(d.getStatus()));
                    vo.setColorType(d.getColorType());
                    vo.setRemark(d.getRemark());
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
