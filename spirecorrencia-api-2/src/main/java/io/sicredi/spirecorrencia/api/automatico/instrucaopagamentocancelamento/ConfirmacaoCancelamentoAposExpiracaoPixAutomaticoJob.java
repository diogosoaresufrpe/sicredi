package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import io.micrometer.observation.annotation.Observed;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ConfirmacaoCancelamentoAposExpiracaoPixAutomaticoJob {

    private final AppConfig appConfig;
    private final ObservabilidadeDecorator observabilidadeDecorator;
    private final ConfirmacaoCancelamentoAposExpiracaoPixAutomaticoService confirmacaoCancelamentoAposExpiracaoPixAutomaticoService;
    private final NotificacaoProducer producer;

    @LockProviderToUse("configLockProvider")
    @Scheduled(cron = "${config.jobConfirmacaoCancelamentoAposExpiracaoPixAutomatico.cron-expression}")
    @SchedulerLock(
            name = "#{@appConfig.jobConfirmacaoCancelamentoAposExpiracaoPixAutomatico.nomeJob}",
            lockAtMostFor = "#{@appConfig.jobConfirmacaoCancelamentoAposExpiracaoPixAutomatico.lockAtMostFor}",
            lockAtLeastFor = "#{@appConfig.jobConfirmacaoCancelamentoAposExpiracaoPixAutomatico.lockAtLeastFor}"
    )
    @Observed(name = "#{@appConfig.jobConfirmacaoCancelamentoAposExpiracaoPixAutomatico.nomeJob}")
    public void executar() {
        log.info("Iniciando job de Confirmacao de Cancelamento Apos Expiracao Pix Automatico");

        if (!appConfig.getJobConfirmacaoCancelamentoAposExpiracaoPixAutomatico().isJobHabilitado()) {
            return;
        }

        observabilidadeDecorator.executar(
                Map.of(RecorrenciaMdc.NOME_JOB, appConfig.getJobConfirmacaoCancelamentoAposExpiracaoPixAutomatico().getNomeJob()),
                this::atualizaConfirmacaoCancelamentoPixAutomaticoJobService
                );

    }

    void atualizaConfirmacaoCancelamentoPixAutomaticoJobService (){
        confirmacaoCancelamentoAposExpiracaoPixAutomaticoService.atualizarConfirmacaoCancelamentoPixAutomaticoJobService();
    }

}
