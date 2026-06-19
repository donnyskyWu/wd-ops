package cn.iocoder.yudao.module.oa.service.metadata;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataEntityCreateReq;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataEntityUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataEntityVO;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataFieldBatchUpdateReq;
import cn.iocoder.yudao.module.oa.api.dto.metadata.MetadataFieldVO;
import cn.iocoder.yudao.module.oa.api.dto.metadata.TableColumnVO;
import cn.iocoder.yudao.module.oa.api.dto.metadata.UnmappedTableVO;

import java.util.List;

public interface MetadataService {

    PageResult<MetadataEntityVO> list(String entityName, String entityCode, String status,
                                      Integer pageNum, Integer pageSize);

    MetadataEntityVO getById(Long id);

    List<MetadataFieldVO> getFieldsByEntityCode(String entityCode);

    List<UnmappedTableVO> listUnmappedTables();

    List<TableColumnVO> listTableColumns(String tableName);

    Long create(MetadataEntityCreateReq req);

    void update(MetadataEntityUpdateReq req);

    void updateFields(Long entityId, MetadataFieldBatchUpdateReq req);

    void delete(Long id);
}
