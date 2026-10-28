package com.ten.project.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ten.project.mapper.InterfaceInfoMapper;
import com.ten.project.model.entity.InterfaceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 内部接口元数据解析测试。
 *
 * <p>验证上线状态筛选、方法匹配、精确路径优先及未知接口拒绝。</p>
 */
class InnerInterfaceInfoServiceImplTest {
    private InterfaceInfoMapper mapper;
    private InnerInterfaceInfoServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(InterfaceInfoMapper.class);
        service = new InnerInterfaceInfoServiceImpl();
        ReflectionTestUtils.setField(service, "mapper", mapper);
    }

    private InterfaceInfo registration(long id, String path) {
        InterfaceInfo info = new InterfaceInfo();
        info.setId(id); info.setUrl(path); info.setMethod("POST"); info.setStatus(1);
        return info;
    }

    @Test
    @SuppressWarnings("unchecked")
    void resolvesActualIdAndQueriesOnlyOnlineMethodMatches() {
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(registration(99L, "/api/tools/base64")));
        assertEquals(99L, service.resolveOnlineInterfaceId("post", "/api/tools/base64"));
        ArgumentCaptor<QueryWrapper<InterfaceInfo>> query = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(mapper).selectList(query.capture());
        assertTrue(query.getValue().getSqlSegment().contains("status"));
        assertTrue(query.getValue().getParamNameValuePairs().containsValue(1));
        assertTrue(query.getValue().getParamNameValuePairs().containsValue("POST"));
    }

    @Test
    void exactPathsTakePriorityOverTemplates() {
        when(mapper.selectList(any())).thenReturn(Arrays.asList(
                registration(1L, "/api/name/{name}"), registration(2L, "/api/name/object")));
        assertEquals(2L, service.resolveOnlineInterfaceId("POST", "/api/name/object"));
        assertEquals(1L, service.resolveOnlineInterfaceId("POST", "/api/name/ten"));
    }

    @Test
    void unknownOrOfflinePathsHaveNoCallableId() {
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());
        assertNull(service.resolveOnlineInterfaceId("GET", "/api/tools/uuid"));
        assertNull(service.resolveOnlineInterfaceId("GET", "/outside"));
    }
}
