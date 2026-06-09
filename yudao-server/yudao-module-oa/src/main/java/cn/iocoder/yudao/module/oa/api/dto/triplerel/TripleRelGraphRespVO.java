package cn.iocoder.yudao.module.oa.api.dto.triplerel;

import lombok.Data;

import java.util.List;

@Data
public class TripleRelGraphRespVO {

    private Long personalWechatId;
    private PersonalWechatNode personalWechat;
    private List<VideoAccountNode> videoAccounts;
    private WeworkAccountNode weworkAccount;

    @Data
    public static class PersonalWechatNode {
        private Long id;
        private String wechatId;
        private String accountName;
    }

    @Data
    public static class VideoAccountNode {
        private Long id;
        private String accountName;
    }

    @Data
    public static class WeworkAccountNode {
        private Long id;
        private String accountName;
        private String corpId;
    }
}
