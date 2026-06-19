package cn.iocoder.yudao.module.oa.service.metadata;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.OaErrorCodes;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataEntityCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataEntityUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataEntityVO;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataFieldBatchUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataFieldUpdateItem;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataFieldVO;
import cn.iocoder.yudao.module.oa.api.dto.metadata.TableColumnVO;
import cn.iocoder.yudao.module.oa.api.dto.metadata.UnmappedTableVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.metadata.MetadataEntityDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.metadata.MetadataFieldDO;
import cn.iocoder.yudao.module.oa.dal.mysql.metadata.MetadataEntityMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.metadata.MetadataFieldMapper;
import cn.iocoder.yudao.module.oa.framework.audit.AuditLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetadataServiceImpl implements MetadataService {

    private static final Set<String> EXCLUDED_TABLE_PREFIXES = Set.of("sys_", "flyway_");

    private final MetadataEntityMapper metadataEntityMapper;
    private final MetadataFieldMapper metadataFieldMapper;
    private final JdbcTemplate jdbcTemplate;
    private final MetadataAuthSupport metadataAuthSupport;

    @Override
    public PageResult<MetadataEntityVO> list(String entityName, String entityCode, String status,
                                             Integer pageNum, Integer pageSize) {
        Long tenantId = requireTenantId();
        LambdaQueryWrapper<MetadataEntityDO> wrapper = new LambdaQueryWrapper<MetadataEntityDO>()
                .eq(MetadataEntityDO::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(entityName), MetadataEntityDO::getEntityName, entityName)
                .like(StrUtil.isNotBlank(entityCode), MetadataEntityDO::getEntityCode, entityCode)
                .eq(StrUtil.isNotBlank(status), MetadataEntityDO::getStatus, status)
                .orderByDesc(MetadataEntityDO::getUpdateTime);
        Page<MetadataEntityDO> page = metadataEntityMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize), wrapper);
        List<MetadataEntityVO> list = page.getRecords().stream()
                .map(this::toEntityVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public MetadataEntityVO getById(Long id) {
        MetadataEntityVO vo = toEntityVO(requireEntity(id));
        vo.setFields(listFields(id));
        return vo;
    }

    @Override
    public List<MetadataFieldVO> getFieldsByEntityCode(String entityCode) {
        MetadataEntityDO entity = requireEntityByCode(entityCode);
        return listFields(entity.getId());
    }

    @Override
    public List<UnmappedTableVO> listUnmappedTables() {
        Long tenantId = requireTenantId();
        Set<String> mapped = metadataEntityMapper.selectList(new LambdaQueryWrapper<MetadataEntityDO>()
                        .eq(MetadataEntityDO::getTenantId, tenantId)
                        .select(MetadataEntityDO::getPhysicalTable))
                .stream()
                .map(MetadataEntityDO::getPhysicalTable)
                .collect(Collectors.toSet());

        List<String> allTables = queryAllPhysicalTables();
        List<UnmappedTableVO> result = new ArrayList<>();
        for (String tableName : allTables) {
            if (mapped.contains(tableName)) {
                continue;
            }
            result.add(new UnmappedTableVO(tableName, suggestEntityCode(tableName), suggestEntityName(tableName)));
        }
        return result;
    }

    @Override
    public List<TableColumnVO> listTableColumns(String tableName) {
        requirePhysicalTableExists(tableName);
        return queryTableColumns(tableName).stream()
                .map(col -> new TableColumnVO(
                        col.columnName(),
                        col.dataType(),
                        toFieldCode(col.columnName()),
                        inferQueryConditionType(col.columnName(), col.dataType()).type(),
                        inferQueryConditionType(col.columnName(), col.dataType()).dictType()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-metadata", action = "create")
    public Long create(MetadataEntityCreateReq req) {
        Long tenantId = requireTenantId();
        String physicalTable = req.getPhysicalTable().trim().toLowerCase(Locale.ROOT);
        requirePhysicalTableExists(physicalTable);
        assertNotMapped(tenantId, physicalTable, null);

        if (metadataEntityMapper.selectCount(new LambdaQueryWrapper<MetadataEntityDO>()
                .eq(MetadataEntityDO::getTenantId, tenantId)
                .eq(MetadataEntityDO::getEntityCode, req.getEntityCode().trim())) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY);
        }

        MetadataEntityDO entity = new MetadataEntityDO();
        entity.setTenantId(tenantId);
        entity.setEntityCode(req.getEntityCode().trim());
        entity.setEntityName(req.getEntityName().trim());
        entity.setPhysicalTable(physicalTable);
        entity.setStatus(StrUtil.blankToDefault(req.getStatus(), "ENABLED"));
        entity.setRemark(req.getRemark());
        entity.setCreator(TenantContextHolder.getUsername());
        entity.setUpdater(TenantContextHolder.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        metadataEntityMapper.insert(entity);

        importFieldsFromTable(entity, physicalTable);
        return entity.getId();
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-metadata", action = "update")
    public void update(MetadataEntityUpdateReq req) {
        MetadataEntityDO existing = requireEntity(req.getId());
        if (StrUtil.isNotBlank(req.getEntityName())) {
            existing.setEntityName(req.getEntityName().trim());
        }
        if (StrUtil.isNotBlank(req.getStatus())) {
            existing.setStatus(req.getStatus());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        existing.setUpdater(TenantContextHolder.getUsername());
        existing.setUpdateTime(LocalDateTime.now());
        metadataEntityMapper.updateById(existing);
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-metadata", action = "update-fields")
    public void updateFields(Long entityId, MetadataFieldBatchUpdateReq req) {
        MetadataEntityDO entity = requireEntity(entityId);
        Long tenantId = requireTenantId();
        for (MetadataFieldUpdateItem item : req.getFields()) {
            if (item.getId() == null) {
                throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "字段 ID 不能为空");
            }
            MetadataFieldDO field = metadataFieldMapper.selectById(item.getId());
            if (field == null || !tenantId.equals(field.getTenantId())
                    || !entity.getId().equals(field.getEntityId())) {
                throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
            }
            validateQueryConditionType(item.getQueryConditionType(), item.getDictType());
            field.setFieldName(item.getFieldName().trim());
            field.setQueryConditionType(item.getQueryConditionType());
            field.setDictType(item.getDictType());
            field.setSelectorConfig(item.getSelectorConfig() == null
                    ? null
                    : JSONUtil.toJsonStr(item.getSelectorConfig()));
            field.setSort(item.getSort());
            field.setUpdater(TenantContextHolder.getUsername());
            field.setUpdateTime(LocalDateTime.now());
            metadataFieldMapper.updateById(field);
        }
    }

    @Override
    @Transactional
    @AuditLog(module = "M8-metadata", action = "delete")
    public void delete(Long id) {
        metadataAuthSupport.requireSuperAdmin();
        MetadataEntityDO entity = requireEntity(id);
        metadataFieldMapper.delete(new LambdaQueryWrapper<MetadataFieldDO>()
                .eq(MetadataFieldDO::getTenantId, entity.getTenantId())
                .eq(MetadataFieldDO::getEntityId, entity.getId()));
        metadataEntityMapper.deleteById(entity.getId());
    }

    private void importFieldsFromTable(MetadataEntityDO entity, String physicalTable) {
        List<ColumnMeta> columns = queryTableColumns(physicalTable);
        int sort = 10;
        for (ColumnMeta col : columns) {
            if (isSkippedColumn(col.columnName())) {
                continue;
            }
            QueryConditionInference inference = inferQueryConditionType(col.columnName(), col.dataType());
            MetadataFieldDO field = new MetadataFieldDO();
            field.setTenantId(entity.getTenantId());
            field.setEntityId(entity.getId());
            field.setFieldCode(toFieldCode(col.columnName()));
            field.setFieldName(humanizeColumn(col.columnName()));
            field.setColumnName(col.columnName());
            field.setDataType(normalizeDataType(col.dataType()));
            field.setQueryConditionType(inference.type());
            field.setDictType(inference.dictType());
            field.setSort(sort);
            field.setCreator(TenantContextHolder.getUsername());
            field.setUpdater(TenantContextHolder.getUsername());
            field.setCreateTime(LocalDateTime.now());
            field.setUpdateTime(LocalDateTime.now());
            metadataFieldMapper.insert(field);
            sort += 10;
        }
    }

    private List<MetadataFieldVO> listFields(Long entityId) {
        return metadataFieldMapper.selectList(new LambdaQueryWrapper<MetadataFieldDO>()
                        .eq(MetadataFieldDO::getTenantId, requireTenantId())
                        .eq(MetadataFieldDO::getEntityId, entityId)
                        .orderByAsc(MetadataFieldDO::getSort)
                        .orderByAsc(MetadataFieldDO::getId))
                .stream()
                .map(this::toFieldVO)
                .collect(Collectors.toList());
    }

    private MetadataEntityDO requireEntity(Long id) {
        MetadataEntityDO entity = metadataEntityMapper.selectById(id);
        if (entity == null || !requireTenantId().equals(entity.getTenantId())) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return entity;
    }

    private MetadataEntityDO requireEntityByCode(String entityCode) {
        MetadataEntityDO entity = metadataEntityMapper.selectOne(new LambdaQueryWrapper<MetadataEntityDO>()
                .eq(MetadataEntityDO::getTenantId, requireTenantId())
                .eq(MetadataEntityDO::getEntityCode, entityCode));
        if (entity == null) {
            throw new ServiceException(OaErrorCodes.ENTITY_NOT_EXISTS);
        }
        return entity;
    }

    private void assertNotMapped(Long tenantId, String physicalTable, Long excludeId) {
        LambdaQueryWrapper<MetadataEntityDO> wrapper = new LambdaQueryWrapper<MetadataEntityDO>()
                .eq(MetadataEntityDO::getTenantId, tenantId)
                .eq(MetadataEntityDO::getPhysicalTable, physicalTable);
        if (excludeId != null) {
            wrapper.ne(MetadataEntityDO::getId, excludeId);
        }
        if (metadataEntityMapper.selectCount(wrapper) > 0) {
            throw new ServiceException(OaErrorCodes.DUPLICATE_ENTITY.getCode(), "物理表已映射");
        }
    }

    private void requirePhysicalTableExists(String tableName) {
        List<String> tables = queryAllPhysicalTables();
        if (!tables.contains(tableName.toLowerCase(Locale.ROOT))) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "物理表不存在");
        }
    }

    private List<String> queryAllPhysicalTables() {
        String schema = resolveSchema();
        List<String> tables = jdbcTemplate.queryForList(
                """
                        SELECT LOWER(TABLE_NAME) AS TABLE_NAME
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_SCHEMA = ?
                          AND TABLE_TYPE IN ('BASE TABLE', 'TABLE')
                        ORDER BY TABLE_NAME
                        """,
                String.class,
                schema);
        return tables.stream()
                .filter(t -> EXCLUDED_TABLE_PREFIXES.stream().noneMatch(t::startsWith))
                .collect(Collectors.toList());
    }

    private List<ColumnMeta> queryTableColumns(String tableName) {
        String schema = resolveSchema();
        return jdbcTemplate.query(
                """
                        SELECT COLUMN_NAME, DATA_TYPE
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = ?
                          AND TABLE_NAME = ?
                        ORDER BY ORDINAL_POSITION
                        """,
                (rs, rowNum) -> new ColumnMeta(
                        rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT),
                        rs.getString("DATA_TYPE")),
                schema,
                tableName.toLowerCase(Locale.ROOT));
    }

    private String resolveSchema() {
        String schema = jdbcTemplate.queryForObject("SELECT SCHEMA()", String.class);
        if (StrUtil.isBlank(schema)) {
            schema = jdbcTemplate.queryForObject("SELECT CURRENT_SCHEMA()", String.class);
        }
        return schema;
    }

    private void validateQueryConditionType(String queryConditionType, String dictType) {
        if ("DICT".equals(queryConditionType) && StrUtil.isBlank(dictType)) {
            throw new ServiceException(OaErrorCodes.BAD_REQUEST.getCode(), "DICT 类型须指定 dict_type");
        }
    }

    private boolean isSkippedColumn(String columnName) {
        return Set.of("id", "tenant_id", "creator", "create_time", "updater", "update_time", "deleted")
                .contains(columnName.toLowerCase(Locale.ROOT));
    }

    private String suggestEntityCode(String tableName) {
        String code = tableName.toLowerCase(Locale.ROOT);
        if (code.startsWith("oa_")) {
            code = code.substring(3);
        }
        return code;
    }

    private String suggestEntityName(String tableName) {
        return humanizeColumn(suggestEntityCode(tableName));
    }

    private String toFieldCode(String columnName) {
        return columnName.toLowerCase(Locale.ROOT);
    }

    private String humanizeColumn(String columnName) {
        return columnName.replace('_', ' ');
    }

    private String normalizeDataType(String dataType) {
        if (dataType == null) {
            return "VARCHAR";
        }
        return dataType.toUpperCase(Locale.ROOT);
    }

    private QueryConditionInference inferQueryConditionType(String columnName, String dataType) {
        String col = columnName.toLowerCase(Locale.ROOT);
        String type = dataType == null ? "" : dataType.toLowerCase(Locale.ROOT);

        if (col.equals("ip_group_id")) {
            return new QueryConditionInference("IP_GROUP_SELECT", null);
        }
        if (col.equals("account_id") || col.endsWith("_account_id")) {
            return new QueryConditionInference("ACCOUNT_SELECT", null);
        }
        if (col.equals("author_id") || col.equals("user_id") || col.endsWith("_user_id")) {
            return new QueryConditionInference("USER_SELECT", null);
        }
        if (col.equals("platform_type") || col.endsWith("_platform_type")) {
            return new QueryConditionInference("PLATFORM_SELECT", "dict_platform_type");
        }
        if (col.contains("competition")) {
            return new QueryConditionInference("COMPETITION_SELECT", null);
        }
        if (type.contains("date") || type.contains("time") || col.endsWith("_date") || col.endsWith("_time")) {
            return new QueryConditionInference("DATE", null);
        }
        if (type.contains("int") || type.contains("decimal") || type.contains("numeric") || type.contains("double")
                || type.contains("float") || type.contains("number")) {
            return new QueryConditionInference("NUMBER", null);
        }
        if (col.equals("status") || col.endsWith("_status") || col.endsWith("_type")) {
            return new QueryConditionInference("DICT", null);
        }
        return new QueryConditionInference("TEXT", null);
    }

    private MetadataEntityVO toEntityVO(MetadataEntityDO entity) {
        MetadataEntityVO vo = new MetadataEntityVO();
        vo.setId(entity.getId());
        vo.setEntityCode(entity.getEntityCode());
        vo.setEntityName(entity.getEntityName());
        vo.setPhysicalTable(entity.getPhysicalTable());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private MetadataFieldVO toFieldVO(MetadataFieldDO field) {
        MetadataFieldVO vo = new MetadataFieldVO();
        vo.setId(field.getId());
        vo.setEntityId(field.getEntityId());
        vo.setFieldCode(field.getFieldCode());
        vo.setFieldName(field.getFieldName());
        vo.setColumnName(field.getColumnName());
        vo.setDataType(field.getDataType());
        vo.setQueryConditionType(field.getQueryConditionType());
        vo.setDictType(field.getDictType());
        if (StrUtil.isNotBlank(field.getSelectorConfig())) {
            vo.setSelectorConfig(JSONUtil.parse(field.getSelectorConfig()));
        }
        vo.setSort(field.getSort());
        return vo;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(OaErrorCodes.UNAUTHORIZED.getCode(), "缺少租户上下文");
        }
        return tenantId;
    }

    private record ColumnMeta(String columnName, String dataType) {
    }

    private record QueryConditionInference(String type, String dictType) {
    }
}
