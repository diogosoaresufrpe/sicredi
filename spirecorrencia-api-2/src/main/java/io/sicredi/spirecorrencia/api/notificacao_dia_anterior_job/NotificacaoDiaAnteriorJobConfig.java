package io.sicredi.spirecorrencia.api.notificacao_dia_anterior_job;

import io.sicredi.spirecorrencia.api.config.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
class NotificacaoDiaAnteriorJobConfig implements SchedulingConfigurer {

    private final AppConfig propertiesLoader;
    private final NotificacaoDiaAnteriorJob job;


    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        final var cronTrigger = new CronTrigger(
                propertiesLoader.getJobNotificacaoDiaAnterior().getCronExpression()
        );

        taskRegistrar.addTriggerTask(
                job::executar,
                cronTrigger
        );
    }

}
