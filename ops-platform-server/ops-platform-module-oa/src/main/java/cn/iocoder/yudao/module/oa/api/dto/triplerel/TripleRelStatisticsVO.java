package cn.iocoder.yudao.module.oa.api.dto.triplerel;

import lombok.Data;

@Data
public class TripleRelStatisticsVO {

    private long totalBound;
    private long fullTriple;
    private long wechatVideo;
    private long wechatWework;
    private long videoWework;
    private long unbound;
}
