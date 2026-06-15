package cn.iocoder.yudao.module.oa.api.dto.content;

import lombok.Data;

import java.util.List;

@Data
public class ContentPublishOptionsVO {

    private List<PlatformOption> platforms;

    @Data
    public static class PlatformOption {
        private String platformType;
        private String platformName;
        private List<AccountOption> accounts;
    }

    @Data
    public static class AccountOption {
        private Long id;
        private String accountName;
        private String externalAccountId;
        private Boolean publishEnabled;
    }
}
