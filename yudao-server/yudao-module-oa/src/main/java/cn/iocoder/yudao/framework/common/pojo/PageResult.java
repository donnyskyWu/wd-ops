package cn.iocoder.yudao.framework.common.pojo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class PageResult<T> {

    private List<T> list;
    private Long total;

    public PageResult() {
    }

    public PageResult(List<T> list, Long total) {
        this.list = list;
        this.total = total;
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0L);
    }
}
