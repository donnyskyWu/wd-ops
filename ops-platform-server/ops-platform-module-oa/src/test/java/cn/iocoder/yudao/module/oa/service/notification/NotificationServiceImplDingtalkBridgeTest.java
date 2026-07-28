package cn.iocoder.yudao.module.oa.service.notification;

import cn.iocoder.yudao.framework.common.biz.system.user.AdminUserApi;
import cn.iocoder.yudao.framework.common.biz.system.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.auth.SysUserDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.content.ProductionContentDO;
import cn.iocoder.yudao.module.oa.dal.dataobject.system.SysMessageDO;
import cn.iocoder.yudao.module.oa.dal.mysql.account.AccountMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.ipgroup.IpGroupMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.SopNodeMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.sop.TaskMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.SysMessageMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.system.SysNotificationEventMapper;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkRobotClient;
import cn.iocoder.yudao.module.oa.framework.dingtalk.DingTalkWorkNotifyClient;
import cn.iocoder.yudao.module.oa.framework.notification.NotificationProperties;
import cn.iocoder.yudao.module.oa.service.content.ContentReviewConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplDingtalkBridgeTest {

    private static final Long FOOTBALL_USER_ID = 1024L;

    @Mock
    private SysMessageMapper sysMessageMapper;
    @Mock
    private SysNotificationEventMapper notificationEventMapper;
    @Mock
    private IpGroupMapper ipGroupMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private SopNodeMapper sopNodeMapper;
    @Mock
    private DingTalkRobotClient dingTalkRobotClient;
    @Mock
    private DingTalkWorkNotifyClient dingTalkWorkNotifyClient;
    @Mock
    private ContentReviewConfigService contentReviewConfigService;
    @Mock
    private NotificationProperties notificationProperties;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private AdminUserApi adminUserApi;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                sysMessageMapper,
                notificationEventMapper,
                ipGroupMapper,
                taskMapper,
                sopNodeMapper,
                dingTalkRobotClient,
                dingTalkWorkNotifyClient,
                contentReviewConfigService,
                notificationProperties,
                accountMapper,
                adminUserApi);
    }

    @Test
    @DisplayName("C-WP6: Feign getUser 返回 dingtalkUserId 时优先使用")
    void prefersFeignDingtalkUserId() {
        AdminUserRespDTO dto = new AdminUserRespDTO();
        dto.setId(FOOTBALL_USER_ID);
        dto.setDingtalkUserId("ding-feign-001");
        when(adminUserApi.getUser(FOOTBALL_USER_ID)).thenReturn(CommonResult.success(dto));

        SysUserDO legacyUser = new SysUserDO();
        legacyUser.setDingUserId("ding-legacy-001");

        assertEquals("ding-feign-001",
                notificationService.resolveDingtalkUserId(FOOTBALL_USER_ID, legacyUser));
    }

    @Test
    @DisplayName("C-WP6: Feign 不可用或 dingtalkUserId 为空时降级 legacy sys_user.ding_user_id")
    void fallsBackToLegacyDingUserIdWhenFeignBlank() {
        AdminUserRespDTO dto = new AdminUserRespDTO();
        dto.setId(FOOTBALL_USER_ID);
        dto.setDingtalkUserId("");
        when(adminUserApi.getUser(FOOTBALL_USER_ID)).thenReturn(CommonResult.success(dto));

        SysUserDO legacyUser = new SysUserDO();
        legacyUser.setDingUserId("ding-legacy-002");

        assertEquals("ding-legacy-002",
                notificationService.resolveDingtalkUserId(FOOTBALL_USER_ID, legacyUser));
    }

    @Test
    @DisplayName("C-WP6: Feign 异常时降级 legacy sys_user.ding_user_id")
    void fallsBackToLegacyDingUserIdWhenFeignThrows() {
        when(adminUserApi.getUser(FOOTBALL_USER_ID)).thenThrow(new RuntimeException("system-server down"));

        SysUserDO legacyUser = new SysUserDO();
        legacyUser.setDingUserId("ding-legacy-003");

        assertEquals("ding-legacy-003",
                notificationService.resolveDingtalkUserId(FOOTBALL_USER_ID, legacyUser));
    }

    @Test
    @DisplayName("C-WP6: 双源均无 dingtalk id 时返回 null")
    void returnsNullWhenBothSourcesBlank() {
        AdminUserRespDTO dto = new AdminUserRespDTO();
        dto.setId(FOOTBALL_USER_ID);
        when(adminUserApi.getUser(FOOTBALL_USER_ID)).thenReturn(CommonResult.success(dto));

        SysUserDO legacyUser = new SysUserDO();

        assertNull(notificationService.resolveDingtalkUserId(FOOTBALL_USER_ID, legacyUser));
    }

    @Test
    @DisplayName("ADR-056: Football userId 无 legacy sys_user 行时仍发送站内信")
    void sendProceedsWithoutLegacySysUserRow() {
        ProductionContentDO content = new ProductionContentDO();
        content.setTenantId(1L);
        content.setCreatorUserId(FOOTBALL_USER_ID);
        content.setTitle("测试内容");

        notificationService.notifyContentReviewApproved(content);

        verify(sysMessageMapper).insert(any(SysMessageDO.class));
    }

    @Test
    @DisplayName("ADR-056: userId 为 null 时跳过通知")
    void sendSkipsWhenUserIdNull() {
        ProductionContentDO content = new ProductionContentDO();
        content.setTenantId(1L);
        content.setCreatorUserId(null);
        content.setTitle("测试内容");

        notificationService.notifyContentReviewApproved(content);

        verify(sysMessageMapper, never()).insert(any(SysMessageDO.class));
    }
}
