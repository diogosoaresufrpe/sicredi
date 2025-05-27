package io.sicredi.spirecorrencia.api.automatico.expiracao;

import io.micrometer.observation.annotation.Observed;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import io.sicredi.spirecorrencia.api.utils.NotificadorProcessadorPaginacaoUtil;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spirecorrencia.api.utils.SystemDateUtil;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static br.com.sicredi.canaisdigitais.enums.OrigemEnum.FISITAL;
import static br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum.MOBI;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.*;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ExpiracaoAutorizacaoPixAutomaticoJob {

    private final AppConfig appConfig;
    private final ObservabilidadeDecorator observabilidadeDecorator;
    private final NotificadorProcessadorPaginacaoUtil processadorPaginas;
    private final ConsultaSolicitacaoExpiracaoPixAutomaticoService consultaSolicitacaoExpiracaoPixAutomaticoService;
    private final NotificacaoProducer producer;

    @LockProviderToUse("configLockProvider")
    @Scheduled(cron = "${config.jobNotificaoExpiracaoPixAutomatico.cron-expression}")
    @SchedulerLock(
            name = "#{@appConfig.jobNotificaoExpiracaoPixAutomatico.nomeJob}",
            lockAtMostFor = "#{@appConfig.jobNotificaoExpiracaoPixAutomatico.lockAtMostFor}",
            lockAtLeastFor = "#{@appConfig.jobNotificaoExpiracaoPixAutomatico.lockAtLeastFor}"
    )
    @Observed(name = "#{@appConfig.jobNotificaoExpiracaoPixAutomatico.nomeJob}")
    public void executar() {
        log.info("Iniciando job de expiracao de autorizacao pix");

        if (!appConfig.getJobNotificaoExpiracaoPixAutomatico().isJobHabilitado()) {
            return;
        }

        observabilidadeDecorator.executar(
                Map.of(RecorrenciaMdc.NOME_JOB, appConfig.getJobNotificaoExpiracaoPixAutomatico().getNomeJob()),
                () -> processadorPaginas.processaPaginacoesEnviaNotificacao(
                        buscaPaginadaDeSolicitacoes(),
                        this::converteSolicitacaoEmNotificacao,
                        producer::enviarNotificacao
                )
        );
    }

    protected Page<SolicitacaoAutorizacaoRecorrencia> buscaPaginadaDeSolicitacoes() {
        Pageable pageable = PageRequest.of(0, appConfig.getJobNotificaoExpiracaoPixAutomatico().getTamanhoDaConsulta());
        return consultaSolicitacaoExpiracaoPixAutomaticoService.buscarSolicitacoesExpiradas(pageable);
    }

    protected NotificacaoDTO converteSolicitacaoEmNotificacao(SolicitacaoAutorizacaoRecorrencia solicitacao) {
        var canal = FISITAL.equals(solicitacao.getTipoSistemaPagador()) ? "SICREDI_APP" : MOBI.getTipoCanalPix().name();
        return NotificacaoDTO.builder()
                .conta(solicitacao.getContaPagador())
                .agencia(solicitacao.getAgenciaPagador())
                .operacao(NotificacaoDTO.TipoTemplate.AUTOMATICO_AUTORIZACAO_PENDENTE_DE_APROVACAO)
                .canal(canal)
                .informacoesAdicionais(List.of(
                        NotificacaoDTO.InformacaoAdicional.of(NOME_RECEBEDOR, solicitacao.getNomeRecebedor()),
                        NotificacaoDTO.InformacaoAdicional.of(DATA_EXPIRACAO_AUTORIZACAO, SystemDateUtil.formatarData(solicitacao.getDataExpiracaoConfirmacaoSolicitacao())),
                        NotificacaoDTO.InformacaoAdicional.of(VALOR, String.valueOf(solicitacao.getValor())),
                        NotificacaoDTO.InformacaoAdicional.of(DOCUMENTO_PAGADOR, solicitacao.getCpfCnpjPagador())
                ))
                .build();
    }
}
