package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import io.micrometer.observation.annotation.Observed;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
class ConfirmacaoUsuarioPagadorAutorizacaoPixAutomaticoJob {

    private final AppConfig appConfig;
    private final ObservabilidadeDecorator observabilidadeDecorator;
    private final ProcessarConfirmacaoAutorizacaoExpiradasService service;

    @LockProviderToUse("configLockProvider")
    @SchedulerLock(
            name = "#{@appConfig.jobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico.nomeJob}",
            lockAtMostFor = "#{@appConfig.jobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico.lockAtMostFor}",
            lockAtLeastFor = "#{@appConfig.jobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico.lockAtLeastFor}"
    )
    @Observed(name = "#{@appConfig.jobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico.nomeJob}")
    public void executar() {

        if (!appConfig.getJobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico().isJobHabilitado()) {
            return;
        }

        var atributos = Map.of(
                RecorrenciaMdc.NOME_JOB, appConfig.getJobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico().getNomeJob()
        );

        observabilidadeDecorator.executar(atributos, () -> {

            int tamanhoDaConsulta = appConfig.getJobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico().getTamanhoDaConsulta();
            int minutosExpiracao = appConfig.getRegras().getProcessamento().getMinutosExpiracao();

            log.info("Executando Job responsável por confirmar a autorização do usuário pagador do pix automáticos, há: {} minutos.",
                    minutosExpiracao);

            service.processarAutorizacoesExpiradas(tamanhoDaConsulta, minutosExpiracao);

            log.info("Job responsável por confirmar a autorização do usuário pagador do pix automáticos foi concluído com sucesso");
        });
    }
}