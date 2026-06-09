package cn.iocoder.yudao.module.oa.api.dto.company;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class CompanyMpStatsRespVO {

    private Long companyId;
    private Integer capacity;
    private Integer registered;
    private Integer remaining;
    private Boolean warning;
    private List<MpAccountDetail> details = Collections.emptyList();

    @Data
    public static class MpAccountDetail {
        private String accountName;
        private String platformType;
    }
}
