package com.ten.project.dubbo;

import com.ten.project.mapper.UserInterfaceInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InnerUserInterfaceInfoServiceImplTest {

    @Mock
    private UserInterfaceInfoMapper userInterfaceInfoMapper;

    @InjectMocks
    private InnerUserInterfaceInfoServiceImpl service;

    @Test
    void shouldConsumeQuotaWithSingleAtomicStatement() {
        when(userInterfaceInfoMapper.consumeInvokeCount(1L, 2L, 100)).thenReturn(1);

        assertTrue(service.invokeCount(1L, 2L));
        verify(userInterfaceInfoMapper).consumeInvokeCount(1L, 2L, 100);
    }

    @Test
    void shouldRejectWhenQuotaCannotBeConsumed() {
        when(userInterfaceInfoMapper.consumeInvokeCount(1L, 2L, 100)).thenReturn(0);

        assertFalse(service.invokeCount(1L, 2L));
    }
}
