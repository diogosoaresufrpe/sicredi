package io.sicredi.spirecorrencia.api.automatico.autorizacao;

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
class ConfirmacaoUsuarioPagadorAutorizacaoPixAutomaticoJobConfig implements SchedulingConfigurer {

    private final AppConfig propertiesLoader;
    private final ConfirmacaoUsuarioPagadorAutorizacaoPixAutomaticoJob job;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        final var cronTrigger = new CronTrigger(
                propertiesLoader.getJobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico().getCronExpression()
        );

        taskRegistrar.addTriggerTask(
                job::executar,
                cronTrigger
        );
    }

}
