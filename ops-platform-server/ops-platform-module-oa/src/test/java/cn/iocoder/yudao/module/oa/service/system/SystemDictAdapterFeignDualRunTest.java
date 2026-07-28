package cn.iocoder.yudao.module.oa.service.system;

import cn.iocoder.yudao.framework.common.biz.system.dict.DictDataApi;
import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.oa.dal.dataobject.dict.FootballSystemDictDataDO;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.FootballSystemDictDataMapper;
import cn.iocoder.yudao.module.oa.dal.mysql.dict.FootballSystemDictTypeMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemDictAdapterFeignDualRunTest {

    private static final String DICT_TYPE = "dict_platform_type";
    private static final String DICT_VALUE = "wechat_mp";

    @Mock
    private FootballSystemDictTypeMapper footballSystemDictTypeMapper;
    @Mock
    private FootballSystemDictDataMapper footballSystemDictDataMapper;
    @Mock
    private DictDataApi dictDataApi;

    private SystemDictAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SystemDictAdapter(footballSystemDictTypeMapper, footballSystemDictDataMapper, dictDataApi);
    }

    @Test
    @DisplayName("G-DICT-01 cutover: Feign validate 成功时不查 @DS Mapper")
    void prefersFeignValidateWhenValueValid() {
        when(dictDataApi.validateDictDataList(DICT_TYPE, List.of(DICT_VALUE)))
                .thenReturn(CommonResult.success(true));

        assertTrue(adapter.isValidValue(DICT_TYPE, DICT_VALUE));

        verify(footballSystemDictDataMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("G-DICT-01 cutover: Feign validate 返回 false 时不回退 @DS")
    void acceptsFeignInvalidResultWithoutDsFallback() {
        when(dictDataApi.validateDictDataList(DICT_TYPE, List.of(DICT_VALUE)))
                .thenReturn(CommonResult.success(false));

        assertFalse(adapter.isValidValue(DICT_TYPE, DICT_VALUE));

        verify(footballSystemDictDataMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("G-DICT-01 cutover: Feign validate 失败时 fail-fast")
    void throwsWhenFeignValidateFails() {
        when(dictDataApi.validateDictDataList(DICT_TYPE, List.of(DICT_VALUE)))
                .thenThrow(new RuntimeException("system-server down"));

        assertThrows(ServiceException.class, () -> adapter.isValidValue(DICT_TYPE, DICT_VALUE));

        verify(footballSystemDictDataMapper, never()).selectCount(any(Wrapper.class));
    }

    @Test
    @DisplayName("G-DICT-01 cutover: Feign list 成功时不查 @DS Mapper")
    void prefersFeignDictListWhenAvailable() {
        DictDataRespDTO dto = new DictDataRespDTO();
        dto.setDictType(DICT_TYPE);
        dto.setLabel("微信公众号");
        dto.setValue(DICT_VALUE);
        dto.setStatus(0);
        when(dictDataApi.getDictDataList(DICT_TYPE)).thenReturn(CommonResult.success(List.of(dto)));

        List<FootballSystemDictDataDO> rows = adapter.listEnabledDataByType(DICT_TYPE);

        assertEquals(1, rows.size());
        assertEquals(DICT_VALUE, rows.get(0).getValue());
        assertEquals("微信公众号", rows.get(0).getLabel());
        verify(footballSystemDictDataMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("G-DICT-01 cutover: Feign 返回空列表时不回退 @DS")
    void acceptsEmptyFeignListWithoutDsFallback() {
        when(dictDataApi.getDictDataList(DICT_TYPE)).thenReturn(CommonResult.success(List.of()));

        List<FootballSystemDictDataDO> rows = adapter.listEnabledDataByType(DICT_TYPE);

        assertTrue(rows.isEmpty());
        verify(footballSystemDictDataMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("G-DICT-01 cutover: Feign list 失败时 fail-fast")
    void throwsWhenFeignListFails() {
        when(dictDataApi.getDictDataList(DICT_TYPE)).thenThrow(new RuntimeException("system-server down"));

        assertThrows(ServiceException.class, () -> adapter.listEnabledDataByType(DICT_TYPE));

        verify(footballSystemDictDataMapper, never()).selectList(any(Wrapper.class));
    }
}
