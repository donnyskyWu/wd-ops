package cn.iocoder.yudao.module.oa.dal.dataobject.company;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oa_company_expansion")
public class CompanyExpansionDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long companyId;
    private Integer fromCapacity;
    private Integer toCapacity;
    private String reason;
    private String operatorName;
    private LocalDateTime createTime;
}
