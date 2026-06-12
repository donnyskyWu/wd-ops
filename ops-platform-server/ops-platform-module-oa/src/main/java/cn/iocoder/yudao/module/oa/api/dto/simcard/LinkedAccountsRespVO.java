package cn.iocoder.yudao.module.oa.api.dto.simcard;

import lombok.Data;

import java.util.List;

@Data
public class LinkedAccountsRespVO {

    private String phoneNumberMasked;
    private String operator;
    private Integer totalCount;
    private List<LinkedAccountItemVO> accounts;
}
