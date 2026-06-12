package cn.iocoder.yudao.framework.common.pojo;

import lombok.Data;

@Data
public class PageParam {

    private Integer pageNo = 1;
    private Integer pageSize = 10;

    public int getOffset() {
        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return (no - 1) * size;
    }

    public int getLimit() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
