package cn.iocoder.yudao.module.oa.service.file;

import cn.iocoder.yudao.framework.common.biz.infra.file.FileApi;
import cn.iocoder.yudao.framework.common.biz.infra.file.dto.FileCreateReqDTO;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.api.dto.file.FileUploadVO;
import cn.iocoder.yudao.module.oa.api.dto.sop.TaskAttachmentVO;
import cn.iocoder.yudao.module.oa.config.OaFileProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalFileStorageServiceFeignDualRunTest {

    private static final Long TENANT_ID = 1L;
    private static final Long TASK_ID = 100L;
    private static final String INFRA_URL = "https://cdn.example.com/ops/cover.png";
    private static final String PRESIGNED_URL = "https://cdn.example.com/ops/cover.png?sign=abc";

    @Mock
    private OaFileProperties fileProperties;
    @Mock
    private FileApi fileApi;

    @TempDir
    Path tempDir;

    private LocalFileStorageService service;

    @BeforeEach
    void setUp() {
        service = new LocalFileStorageService(fileProperties, fileApi);
        lenient().when(fileProperties.getUploadDir()).thenReturn(tempDir.toString());
        lenient().when(fileProperties.getMaxFileSize()).thenReturn(50L * 1024 * 1024);
    }

    @Test
    @DisplayName("G-INF-01: 内容图片上传 Feign 成功时不写本地盘")
    void prefersFeignForContentImageUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3});
        when(fileApi.createFile(any(FileCreateReqDTO.class))).thenReturn(CommonResult.success(INFRA_URL));

        FileUploadVO vo = service.storeContentImage(file, TENANT_ID);

        assertEquals(INFRA_URL, vo.getUrl());
        assertEquals(INFRA_URL, vo.getKey());
        assertEquals("cover.png", vo.getName());
        assertTrue(Files.list(tempDir).findAny().isEmpty());
    }

    @Test
    @DisplayName("G-INF-01: 内容图片上传 Feign 失败时回退本地盘")
    void fallsBackToLocalDiskForContentImageUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3});
        when(fileApi.createFile(any(FileCreateReqDTO.class))).thenThrow(new RuntimeException("infra-server down"));

        FileUploadVO vo = service.storeContentImage(file, TENANT_ID);

        assertTrue(vo.getUrl().startsWith("/admin-api/oa/file/view?key="));
        assertTrue(vo.getKey().startsWith(TENANT_ID + "/content/"));
        assertTrue(Files.walk(tempDir).anyMatch(path -> path.toString().endsWith(".png")));
    }

    @Test
    @DisplayName("G-INF-01: 任务附件上传 Feign 成功时不写本地盘")
    void prefersFeignForTaskAttachmentUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", new byte[]{9, 8, 7});
        when(fileApi.createFile(any(FileCreateReqDTO.class))).thenReturn(CommonResult.success(INFRA_URL));

        TaskAttachmentVO vo = service.storeTaskAttachment(file, TENANT_ID, TASK_ID);

        assertEquals(INFRA_URL, vo.getUrl());
        assertEquals("plan.pdf", vo.getName());
        ArgumentCaptor<FileCreateReqDTO> captor = ArgumentCaptor.forClass(FileCreateReqDTO.class);
        verify(fileApi).createFile(captor.capture());
        assertEquals(TENANT_ID + "/task/" + TASK_ID, captor.getValue().getDirectory());
        assertTrue(Files.list(tempDir).findAny().isEmpty());
    }

    @Test
    @DisplayName("G-INF-01: 任务附件上传 Feign 失败时回退本地盘")
    void fallsBackToLocalDiskForTaskAttachmentUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "plan.pdf", "application/pdf", new byte[]{9, 8, 7});
        when(fileApi.createFile(any(FileCreateReqDTO.class))).thenReturn(CommonResult.error(500, "fail"));

        TaskAttachmentVO vo = service.storeTaskAttachment(file, TENANT_ID, TASK_ID);

        assertTrue(vo.getUrl().startsWith("/admin-api/oa/file/download?key="));
        assertTrue(Files.walk(tempDir).anyMatch(path -> path.toString().endsWith(".pdf")));
    }

    @Test
    @DisplayName("G-INF-01: 预签名读 Feign 成功时返回临时 URL")
    void prefersFeignPresignedReadUrl() {
        when(fileApi.presignGetUrl(INFRA_URL, 3600)).thenReturn(CommonResult.success(PRESIGNED_URL));

        assertEquals(PRESIGNED_URL, service.resolvePresignedReadUrl(INFRA_URL));
    }

    @Test
    @DisplayName("G-INF-01: 预签名读 Feign 失败时回退原始 URL")
    void fallsBackToRawUrlWhenPresignFails() {
        when(fileApi.presignGetUrl(eq(INFRA_URL), eq(3600))).thenThrow(new RuntimeException("infra-server down"));

        assertEquals(INFRA_URL, service.resolvePresignedReadUrl(INFRA_URL));
    }

    @Test
    @DisplayName("G-INF-01: Feign create 传 tenant 目录与 MIME")
    void createFileRequestCarriesDirectoryAndMime() {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3});
        when(fileApi.createFile(any(FileCreateReqDTO.class))).thenReturn(CommonResult.success(INFRA_URL));

        service.storeContentImage(file, TENANT_ID);

        ArgumentCaptor<FileCreateReqDTO> captor = ArgumentCaptor.forClass(FileCreateReqDTO.class);
        verify(fileApi).createFile(captor.capture());
        FileCreateReqDTO dto = captor.getValue();
        assertEquals("cover.png", dto.getName());
        assertEquals(TENANT_ID + "/content", dto.getDirectory());
        assertEquals("image/png", dto.getType());
        verify(fileApi, never()).presignGetUrl(any(), any());
    }
}
