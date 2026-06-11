package cn.iocoder.yudao.module.oa.api.dto.system;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeptTreeVO {

    private Long id;
    private Long parentId;
    private String name;
    private Long dingDeptId;
    private Integer sort;
    private String status;
    private List<DeptTreeVO> children = new ArrayList<>();
}
