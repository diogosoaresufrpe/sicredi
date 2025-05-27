package io.sicredi.spirecorrencia.api.liquidacao;

import io.sicredi.spirecorrencia.api.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessamentoLiquidacaoJobConfigTest {

    @Mock
    private AppConfig propertiesLoader;

    @Mock
    @SuppressWarnings("unused")
    private ProcessamentoLiquidacaoJob job;

    @Mock
    private ScheduledTaskRegistrar taskRegistrar;

    @Captor
    private ArgumentCaptor<Trigger> triggerCaptor;

    @InjectMocks
    private ProcessamentoLiquidacaoJobConfig processamentoLiquidacaoJobConfig;

    @Test
    void dadoCronExpression_quandoConfigureTask_deveDefinirIntervaloCorretamente() {
        final var cronExpression = "0 0 * * * *";

        AppConfig.JobShedLock jobShedLock = mock(AppConfig.JobShedLock.class);

        when(propertiesLoader.getJobProcessamentoLiquidacao()).thenReturn(jobShedLock);
        when(jobShedLock.getCronExpression()).thenReturn(cronExpression);

        processamentoLiquidacaoJobConfig.configureTasks(taskRegistrar);

        verify(taskRegistrar).addTriggerTask(any(), triggerCaptor.capture());
        final var trigger = triggerCaptor.getValue();
        assertEquals(CronTrigger.class, trigger.getClass());
        final var cronTrigger = (CronTrigger) trigger;
        assertEquals(cronExpression, cronTrigger.getExpression());
    }
}